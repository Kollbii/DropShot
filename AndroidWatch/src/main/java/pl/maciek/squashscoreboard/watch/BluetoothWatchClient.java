package pl.maciek.squashscoreboard.watch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bluetooth RFCOMM client for the LG G Watch (API 20-22).
 * - Connects to the paired phone running DropShot (pl.maciek.squashscoreboard).
 * - Automatically reconnects in the background when the Bluetooth connection drops.
 * - Never retries, queues, or replays a score command after a failure (prevents late points).
 */
public final class BluetoothWatchClient {
    private static final String TAG = "DropShotWatchBT";
    private static final long RECONNECT_DELAY_MS = 2500L;

    public interface ConnectionListener {
        void onConnectionChanged(boolean connected);
    }

    public interface CommandCallback {
        void onResult(boolean accepted);
    }

    private final Handler mainHandler;
    private final ConnectionListener connectionListener;
    private final Object socketLock = new Object();
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean running;
    private volatile boolean connected;
    private BluetoothSocket activeSocket;
    private InputStream activeInput;
    private OutputStream activeOutput;
    private Thread connectionThread;

    public BluetoothWatchClient(Handler mainHandler, ConnectionListener connectionListener) {
        this.mainHandler = mainHandler;
        this.connectionListener = connectionListener;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        connectionThread = new Thread(this::connectionLoop, "watch-bt-reconnect-loop");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public synchronized void stop() {
        running = false;
        closeActiveSocket();
        if (connectionThread != null) {
            connectionThread.interrupt();
            connectionThread = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Triggers an immediate reconnect attempt if currently disconnected.
     */
    public void requestReconnectNow() {
        Thread t = connectionThread;
        if (t != null && !connected) {
            t.interrupt();
        }
    }

    /**
     * Sends a single watch command over the active RFCOMM socket.
     * Obeys AGENTS.md Rule 5.1.1: never queues or retries a failed command.
     */
    public void sendCommand(final String command, final CommandCallback callback) {
        if (!WatchProtocol.isAllowedCommand(command)) {
            dispatchResult(callback, false);
            return;
        }
        commandExecutor.execute(() -> {
            final byte[] payload = WatchProtocol.encodeCommandLine(command).getBytes(StandardCharsets.UTF_8);
            boolean accepted = false;
            synchronized (socketLock) {
                if (activeSocket != null && activeOutput != null && activeInput != null) {
                    try {
                        activeOutput.write(payload);
                        activeOutput.flush();
                        String responseLine = readBoundedLine(activeInput);
                        if (responseLine == null) {
                            throw new IOException("Bluetooth stream closed by peer");
                        }
                        accepted = WatchProtocol.parseAcceptedResponse(responseLine);
                    } catch (IOException e) {
                        Log.w(TAG, "Command send failed; closing socket without retry", e);
                        closeActiveSocket();
                        requestReconnectNow();
                    }
                }
            }
            dispatchResult(callback, accepted);
        });
    }

    private void connectionLoop() {
        while (running) {
            if (!connected) {
                tryConnectToPairedPhone();
            }
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
            } catch (InterruptedException ignored) {
                // Woken up for immediate reconnect or stop()
            }
        }
    }

    private void tryConnectToPairedPhone() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            updateConnectionState(false);
            return;
        }

        Set<BluetoothDevice> bondedDevices;
        try {
            bondedDevices = adapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission denied", e);
            updateConnectionState(false);
            return;
        }

        if (bondedDevices == null || bondedDevices.isEmpty()) {
            updateConnectionState(false);
            return;
        }

        try {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
        } catch (SecurityException ignored) { }

        for (BluetoothDevice device : bondedDevices) {
            if (!running) return;
            BluetoothSocket socket = openRfcommSocket(device);
            if (socket != null) {
                synchronized (socketLock) {
                    try {
                        activeSocket = socket;
                        activeInput = new BufferedInputStream(socket.getInputStream());
                        activeOutput = socket.getOutputStream();
                        updateConnectionState(true);
                        return;
                    } catch (IOException e) {
                        closeQuietly(socket);
                    }
                }
            }
        }
        updateConnectionState(false);
    }

    private BluetoothSocket openRfcommSocket(BluetoothDevice device) {
        // 1. Try secure RFCOMM channel first
        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(WatchProtocol.SERVICE_UUID);
            socket.connect();
            return socket;
        } catch (Exception firstError) {
            closeQuietly(socket);
        }

        // 2. Fallback to insecure RFCOMM channel for older Android Wear 5.0/5.1 stacks
        socket = null;
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(WatchProtocol.SERVICE_UUID);
            socket.connect();
            return socket;
        } catch (Exception secondError) {
            closeQuietly(socket);
            return null;
        }
    }

    private static String readBoundedLine(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        int b;
        while ((b = input.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') out.write(b);
            if (out.size() > 256) {
                throw new IOException("Response line exceeded 256 bytes");
            }
        }
        if (b == -1 && out.size() == 0) return null;
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private void closeActiveSocket() {
        synchronized (socketLock) {
            closeQuietly(activeSocket);
            activeSocket = null;
            activeInput = null;
            activeOutput = null;
        }
        updateConnectionState(false);
    }

    private static void closeQuietly(BluetoothSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) { }
        }
    }

    private void updateConnectionState(final boolean nextConnected) {
        if (this.connected == nextConnected) return;
        this.connected = nextConnected;
        if (mainHandler != null && connectionListener != null) {
            mainHandler.post(() -> connectionListener.onConnectionChanged(nextConnected));
        }
    }

    private void dispatchResult(final CommandCallback callback, final boolean accepted) {
        if (callback == null || mainHandler == null) return;
        mainHandler.post(() -> callback.onResult(accepted));
    }
}

