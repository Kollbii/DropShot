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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bluetooth RFCOMM client for the LG G Watch (API 20-22).
 * - Connects to the paired phone running DropShot (pl.maciek.squashscoreboard).
 * - Keeps a single persistent RFCOMM socket open during the session with a blocking reader loop
 *   (no polling loop while connected).
 * - Uses exponential backoff (3 s, 5 s, 10 s) when disconnected, or reconnects immediately on screen touch.
 * - Never retries, queues, or replays a score command after a failure (prevents late points).
 */
public final class BluetoothWatchClient {
    private static final String TAG = "DropShotWatchBT";
    public static final long[] BACKOFF_DELAYS_MS = new long[]{3000L, 5000L, 10000L};

    public interface ClientListener {
        void onConnectionChanged(boolean connected);
        void onStateReceived(WatchProtocol.WatchState state);
        void onMatchEnded(WatchProtocol.MatchEndedEvent event);
        void onCommandAck(boolean accepted, boolean gameOrMatchBall);
    }

    private final Handler mainHandler;
    private final ClientListener listener;
    private final Object writeLock = new Object();
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger backoffAttempt = new AtomicInteger(0);

    private volatile boolean running;
    private volatile boolean connected;
    private volatile BluetoothSocket activeSocket;
    private volatile OutputStream activeOutput;
    private Thread connectionThread;

    public BluetoothWatchClient(Handler mainHandler, ClientListener listener) {
        this.mainHandler = mainHandler;
        this.listener = listener;
    }

    public static long getBackoffDelayMs(int attemptIndex) {
        if (attemptIndex <= 0) return BACKOFF_DELAYS_MS[0];
        if (attemptIndex >= BACKOFF_DELAYS_MS.length) {
            return BACKOFF_DELAYS_MS[BACKOFF_DELAYS_MS.length - 1];
        }
        return BACKOFF_DELAYS_MS[attemptIndex];
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        backoffAttempt.set(0);
        connectionThread = new Thread(this::connectionLoop, "watch-bt-session-loop");
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
     * Resets exponential backoff and triggers an immediate reconnect attempt if disconnected.
     */
    public void requestReconnectNow() {
        backoffAttempt.set(0);
        Thread t = connectionThread;
        if (t != null && !connected) {
            t.interrupt();
        }
    }

    /**
     * Sends a single watch command over the persistent RFCOMM socket.
     * Obeys AGENTS.md Rule 5.1.1: never queues or retries a failed command.
     */
    public void sendCommand(final String command) {
        if (!WatchProtocol.isAllowedCommand(command)) {
            dispatchAck(false, false);
            return;
        }
        commandExecutor.execute(() -> {
            final byte[] payload = WatchProtocol.encodeCommandLine(command).getBytes(StandardCharsets.UTF_8);
            boolean sent = false;
            synchronized (writeLock) {
                OutputStream out = activeOutput;
                if (connected && out != null) {
                    try {
                        out.write(payload);
                        out.flush();
                        sent = true;
                    } catch (IOException e) {
                        Log.w(TAG, "Command write failed; closing socket without retry", e);
                        closeActiveSocket();
                        requestReconnectNow();
                    }
                }
            }
            if (!sent) {
                dispatchAck(false, false);
                requestReconnectNow();
            }
        });
    }

    private void connectionLoop() {
        while (running) {
            BluetoothSocket socket = tryConnectToPairedPhone();
            if (socket != null) {
                backoffAttempt.set(0);
                runPersistentReaderLoop(socket);
                closeActiveSocket();
            }
            if (!running) break;

            int attempt = backoffAttempt.getAndIncrement();
            long sleepMs = getBackoffDelayMs(attempt);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ignored) {
                // Woken up for immediate reconnect or stop()
            }
        }
    }

    private BluetoothSocket tryConnectToPairedPhone() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            updateConnectionState(false);
            return null;
        }

        Set<BluetoothDevice> bondedDevices;
        try {
            bondedDevices = adapter.getBondedDevices();
        } catch (SecurityException e) {
            Log.w(TAG, "Bluetooth permission denied", e);
            updateConnectionState(false);
            return null;
        }

        if (bondedDevices == null || bondedDevices.isEmpty()) {
            updateConnectionState(false);
            return null;
        }

        try {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
        } catch (SecurityException ignored) { }

        for (BluetoothDevice device : bondedDevices) {
            if (!running) return null;
            BluetoothSocket socket = openRfcommSocket(device);
            if (socket != null) {
                synchronized (writeLock) {
                    try {
                        activeSocket = socket;
                        activeOutput = socket.getOutputStream();
                        updateConnectionState(true);
                        return socket;
                    } catch (IOException e) {
                        closeQuietly(socket);
                    }
                }
            }
        }
        updateConnectionState(false);
        return null;
    }

    private void runPersistentReaderLoop(BluetoothSocket socket) {
        try {
            InputStream input = new BufferedInputStream(socket.getInputStream());
            while (running && connected) {
                String line = readBoundedLine(input);
                if (line == null) {
                    break;
                }
                handleIncomingLine(line);
            }
        } catch (IOException e) {
            if (running) {
                Log.w(TAG, "Persistent Bluetooth reader ended", e);
            }
        }
    }

    private void handleIncomingLine(String line) {
        String type = WatchProtocol.parseMessageType(line);
        if (WatchProtocol.MSG_STATE.equals(type)) {
            WatchProtocol.WatchState state = WatchProtocol.parseWatchState(line);
            if (state != null && mainHandler != null && listener != null) {
                mainHandler.post(() -> listener.onStateReceived(state));
            }
        } else if (WatchProtocol.MSG_MATCH_ENDED.equals(type)) {
            WatchProtocol.MatchEndedEvent event = WatchProtocol.parseMatchEnded(line);
            if (event != null && mainHandler != null && listener != null) {
                mainHandler.post(() -> listener.onMatchEnded(event));
            }
        } else if (WatchProtocol.MSG_ACK.equals(type)) {
            boolean accepted = WatchProtocol.parseAcceptedResponse(line);
            boolean ball = WatchProtocol.parseAckBall(line);
            dispatchAck(accepted, ball);
        }
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
        ByteArrayOutputStream out = new ByteArrayOutputStream(128);
        int b;
        while ((b = input.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') out.write(b);
            if (out.size() > 1024) {
                throw new IOException("Incoming line exceeded 1024 bytes");
            }
        }
        if (b == -1 && out.size() == 0) return null;
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private void closeActiveSocket() {
        synchronized (writeLock) {
            closeQuietly(activeSocket);
            activeSocket = null;
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
        if (mainHandler != null && listener != null) {
            mainHandler.post(() -> listener.onConnectionChanged(nextConnected));
        }
    }

    private void dispatchAck(final boolean accepted, final boolean gameOrMatchBall) {
        if (mainHandler != null && listener != null) {
            mainHandler.post(() -> listener.onCommandAck(accepted, gameOrMatchBall));
        }
    }
}
