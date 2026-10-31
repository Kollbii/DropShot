package pl.maciek.squashscoreboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [MASVS-NETWORK] [MASVS-PLATFORM] [MASVS-CODE]
 * Classic Bluetooth RFCOMM listener for the standalone AndroidWatch remote control.
 * - Listens only while the Match screen (`MainActivity`) is open.
 * - Accepts connections only from paired (`BOND_BONDED`) Bluetooth devices.
 * - Uses the same JSON command schema (`{"command":"POINT_1"}`) and strict allowlist
 *   (`InputValidator.isAllowedWatchCommand`) as `WatchCommandServer`.
 */
final class BluetoothWatchServer {
    static final String SERVICE_NAME = "DropShotWatchBridge";
    static final UUID SERVICE_UUID = UUID.fromString("d8095407-4b6d-4765-9b3f-282820000001");
    private static final String TAG = "BluetoothWatchServer";
    private static final long MIN_COMMAND_INTERVAL_NANOS = 40_000_000L; // Max 25 req/sec

    private final Context appContext;
    private final Handler mainHandler;
    private final WatchCommandServer.CommandListener listener;
    private final AtomicLong lastCommandNanos = new AtomicLong(0L);

    private volatile boolean stopping;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket activeClientSocket;
    private Thread acceptThread;

    BluetoothWatchServer(
        Context context,
        Handler mainHandler,
        WatchCommandServer.CommandListener listener
    ) {
        this.appContext = context != null && context.getApplicationContext() != null
            ? context.getApplicationContext()
            : context;
        this.mainHandler = mainHandler;
        this.listener = listener;
    }

    static boolean hasBluetoothConnectPermission(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= 31) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    synchronized void start() {
        if (acceptThread != null && acceptThread.isAlive()) return;
        if (!hasBluetoothConnectPermission(appContext)) return;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) return;

        BluetoothServerSocket opened = openServerSocket(adapter);
        if (opened == null) return;

        serverSocket = opened;
        stopping = false;
        acceptThread = new Thread(() -> acceptLoop(opened), "android-watch-rfcomm-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    synchronized void stop() {
        stopping = true;
        closeClientSocket();
        closeServerSocket();
        acceptThread = null;
    }

    private BluetoothServerSocket openServerSocket(BluetoothAdapter adapter) {
        try {
            return adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
        } catch (Exception secureErr) {
            try {
                return adapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
            } catch (Exception insecureErr) {
                SafeLog.w(TAG, "Cannot start Bluetooth watch RFCOMM server", insecureErr);
                return null;
            }
        }
    }

    private void acceptLoop(BluetoothServerSocket listeningSocket) {
        while (!stopping && serverSocket == listeningSocket) {
            BluetoothSocket socket;
            try {
                socket = listeningSocket.accept();
            } catch (IOException e) {
                if (!stopping) SafeLog.w(TAG, "Bluetooth watch accept stopped", e);
                break;
            }
            if (socket == null) continue;

            if (!isBondedDevice(socket.getRemoteDevice())) {
                closeQuietly(socket);
                continue;
            }

            closeClientSocket();
            synchronized (this) {
                activeClientSocket = socket;
            }
            handleClient(socket);
        }
    }

    private boolean isBondedDevice(BluetoothDevice device) {
        if (device == null) return false;
        try {
            return device.getBondState() == BluetoothDevice.BOND_BONDED;
        } catch (SecurityException e) {
            return false;
        }
    }

    private void handleClient(BluetoothSocket socket) {
        try {
            InputStream input = new BufferedInputStream(socket.getInputStream());
            OutputStream output = socket.getOutputStream();
            String line;
            while (!stopping && (line = readBoundedLine(input)) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                boolean accepted = processCommandPayload(trimmed);
                byte[] reply = ("{\"accepted\":" + accepted + "}\n").getBytes(StandardCharsets.UTF_8);
                output.write(reply);
                output.flush();
            }
        } catch (IOException e) {
            if (!stopping) SafeLog.w(TAG, "Bluetooth watch client disconnected", e);
        } finally {
            closeQuietly(socket);
        }
    }

    boolean processCommandPayload(String rawJson) {
        String command = parseWatchCommandJson(rawJson);
        if (command == null || !InputValidator.isAllowedWatchCommand(command)) {
            return false;
        }

        long nowNanos = System.nanoTime();
        long prevNanos = lastCommandNanos.get();
        if (nowNanos - prevNanos < MIN_COMMAND_INTERVAL_NANOS
            || !lastCommandNanos.compareAndSet(prevNanos, nowNanos)) {
            return false;
        }

        if (mainHandler == null || listener == null) {
            return false;
        }

        final boolean[] accepted = {false};
        CountDownLatch completed = new CountDownLatch(1);
        mainHandler.post(() -> {
            try {
                accepted[0] = listener.onCommand(command);
            } finally {
                completed.countDown();
            }
        });
        try {
            if (!completed.await(2, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return accepted[0];
    }

    static String parseWatchCommandJson(String rawJson) {
        if (rawJson == null) return null;
        String trimmed = rawJson.trim();
        if (trimmed.length() < 2 || trimmed.length() > 256) return null;
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null;
        int keyIdx = trimmed.indexOf("\"command\"");
        if (keyIdx < 0) return null;
        int colonIdx = trimmed.indexOf(':', keyIdx + 9);
        if (colonIdx < 0) return null;
        int firstQuote = trimmed.indexOf('"', colonIdx + 1);
        if (firstQuote < 0) return null;
        int secondQuote = trimmed.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        String cmd = trimmed.substring(firstQuote + 1, secondQuote);
        return InputValidator.isAllowedWatchCommand(cmd) ? cmd : null;
    }

    private static String readBoundedLine(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64);
        int b;
        while ((b = input.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') out.write(b);
            if (out.size() > 256) {
                throw new IOException("Bluetooth command line exceeded 256 bytes");
            }
        }
        if (b == -1 && out.size() == 0) return null;
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private synchronized void closeClientSocket() {
        closeQuietly(activeClientSocket);
        activeClientSocket = null;
    }

    private synchronized void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) { }
            serverSocket = null;
        }
    }

    private static void closeQuietly(BluetoothSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) { }
        }
    }
}
