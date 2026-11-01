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
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [MASVS-NETWORK] [MASVS-PLATFORM] [MASVS-CODE]
 * Bidirectional classic Bluetooth RFCOMM bridge for the standalone AndroidWatch remote control.
 * - Listens only while the Match screen (`MainActivity`) is open.
 * - Accepts connections only from paired (`BOND_BONDED`) Bluetooth devices.
 * - Keeps a single persistent RFCOMM socket open and pushes live `STATE` and `MATCH_ENDED`
 *   messages without polling.
 */
final class BluetoothWatchServer {
    static final String SERVICE_NAME = "DropShotWatchBridge";
    static final UUID SERVICE_UUID = UUID.fromString("d8095407-4b6d-4765-9b3f-282820000001");
    static final int RAINBOW_COLOR_KEY = 0xFFFF5059;
    private static final String TAG = "BluetoothWatchServer";
    private static final long MIN_COMMAND_INTERVAL_NANOS = 40_000_000L; // Max 25 req/sec

    interface StateProvider {
        String buildStateJsonLine();
        boolean isGameOrMatchBall();
    }

    private final Context appContext;
    private final Handler mainHandler;
    private final WatchCommandServer.CommandListener listener;
    private final StateProvider stateProvider;
    private final AtomicLong lastCommandNanos = new AtomicLong(0L);
    private final Object writeLock = new Object();

    private volatile boolean stopping;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket activeClientSocket;
    private OutputStream activeClientOutput;
    private Thread acceptThread;

    BluetoothWatchServer(
        Context context,
        Handler mainHandler,
        WatchCommandServer.CommandListener listener,
        StateProvider stateProvider
    ) {
        this.appContext = context != null && context.getApplicationContext() != null
            ? context.getApplicationContext()
            : context;
        this.mainHandler = mainHandler;
        this.listener = listener;
        this.stateProvider = stateProvider;
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

    /**
     * Asynchronously pushes the latest match and player state to the connected watch if present.
     */
    void broadcastStateAsync(final String stateJsonLine) {
        if (stateJsonLine == null || stateJsonLine.isEmpty()) return;
        AsyncRepositoryExecutor.runIo(() -> sendLineToActiveClient(stateJsonLine));
    }

    /**
     * Asynchronously pushes a MATCH_ENDED event to the connected watch when a match finishes.
     */
    void broadcastMatchEndedAsync(final String winnerName, final String finalScore) {
        final String line = buildMatchEndedJsonLine(winnerName, finalScore);
        AsyncRepositoryExecutor.runIo(() -> sendLineToActiveClient(line));
    }

    private void sendLineToActiveClient(String jsonLine) {
        synchronized (writeLock) {
            if (activeClientOutput == null) return;
            try {
                String normalized = jsonLine.endsWith("\n") ? jsonLine : (jsonLine + "\n");
                activeClientOutput.write(normalized.getBytes(StandardCharsets.UTF_8));
                activeClientOutput.flush();
            } catch (IOException e) {
                closeClientSocket();
            }
        }
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
            synchronized (writeLock) {
                activeClientSocket = socket;
                activeClientOutput = output;
            }

            // Immediately push full match & player color state upon connection
            pushSnapshotFromMainThread();

            String line;
            while (!stopping && (line = readBoundedLine(input)) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                CommandOutcome outcome = processCommandPayload(trimmed);
                String ackLine = "{\"type\":\"ACK\",\"accepted\":" + outcome.accepted
                    + ",\"ball\":" + outcome.gameOrMatchBall + "}\n";
                sendLineToActiveClient(ackLine);
                if (outcome.stateLine != null && !outcome.stateLine.isEmpty()) {
                    sendLineToActiveClient(outcome.stateLine);
                }
            }
        } catch (IOException e) {
            if (!stopping) SafeLog.w(TAG, "Bluetooth watch client disconnected", e);
        } finally {
            closeClientSocket();
            closeQuietly(socket);
        }
    }

    private void pushSnapshotFromMainThread() {
        if (mainHandler == null || stateProvider == null) return;
        mainHandler.post(() -> {
            String stateLine = stateProvider.buildStateJsonLine();
            broadcastStateAsync(stateLine);
        });
    }

    static final class CommandOutcome {
        final boolean accepted;
        final boolean gameOrMatchBall;
        final String stateLine;

        CommandOutcome(boolean accepted, boolean gameOrMatchBall, String stateLine) {
            this.accepted = accepted;
            this.gameOrMatchBall = gameOrMatchBall;
            this.stateLine = stateLine;
        }
    }

    CommandOutcome processCommandPayload(String rawJson) {
        String command = parseWatchCommandJson(rawJson);
        if (command == null || !InputValidator.isAllowedWatchCommand(command)) {
            return new CommandOutcome(false, false, null);
        }

        long nowNanos = System.nanoTime();
        long prevNanos = lastCommandNanos.get();
        if (nowNanos - prevNanos < MIN_COMMAND_INTERVAL_NANOS
            || !lastCommandNanos.compareAndSet(prevNanos, nowNanos)) {
            return new CommandOutcome(false, false, null);
        }

        if (mainHandler == null || listener == null) {
            return new CommandOutcome(false, false, null);
        }

        final boolean[] accepted = {false};
        final boolean[] ball = {false};
        final String[] state = {null};
        CountDownLatch completed = new CountDownLatch(1);
        mainHandler.post(() -> {
            try {
                accepted[0] = listener.onCommand(command);
                if (stateProvider != null) {
                    ball[0] = stateProvider.isGameOrMatchBall();
                    state[0] = stateProvider.buildStateJsonLine();
                }
            } finally {
                completed.countDown();
            }
        });
        try {
            if (!completed.await(2, TimeUnit.SECONDS)) {
                return new CommandOutcome(false, false, null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandOutcome(false, false, null);
        }
        return new CommandOutcome(accepted[0], ball[0], state[0]);
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

    /**
     * Extracts a 3-letter uppercase initial from a player name (for example "Kacper" -> "KAC",
     * "PLAYER 1" -> "PL1").
     */
    static String extractInitials(String rawName, String fallback) {
        if (rawName == null) return fallback;
        String upper = rawName.trim().toUpperCase(Locale.getDefault());
        if (upper.isEmpty()) return fallback;
        if (upper.startsWith("PLAYER ") && upper.length() >= 8) {
            char suffix = upper.charAt(7);
            if (Character.isDigit(suffix)) {
                return "PL" + suffix;
            }
        }
        StringBuilder sb = new StringBuilder(3);
        for (int i = 0; i < upper.length() && sb.length() < 3; i++) {
            char c = upper.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isISOControl(c)) {
                sb.append(c);
            }
        }
        return sb.length() > 0 ? sb.toString() : fallback;
    }

    static String buildStateJsonLine(
        String playerOneName,
        String playerTwoName,
        int p1Points,
        int p2Points,
        int p1Sets,
        int p2Sets,
        int setsToWinMatch,
        int activeServer,
        String serviceSide,
        int p1Color,
        boolean p1Rainbow,
        int p2Color,
        boolean p2Rainbow,
        String actionLabel,
        boolean gameOrMatchBall,
        boolean matchFinished,
        boolean paused
    ) {
        String n1 = escapeJson(extractInitials(playerOneName, "PL1"));
        String n2 = escapeJson(extractInitials(playerTwoName, "PL2"));
        String side = "L".equals(serviceSide) ? "L" : "R";
        String act = ("START".equals(actionLabel) || "NEXT".equals(actionLabel)) ? actionLabel : "STOP";
        return "{\"type\":\"STATE\""
            + ",\"n1\":\"" + n1 + "\""
            + ",\"n2\":\"" + n2 + "\""
            + ",\"p1\":" + p1Points
            + ",\"p2\":" + p2Points
            + ",\"s1\":" + p1Sets
            + ",\"s2\":" + p2Sets
            + ",\"targetSets\":" + Math.max(1, setsToWinMatch)
            + ",\"srv\":" + (activeServer == 2 ? 2 : 1)
            + ",\"side\":\"" + side + "\""
            + ",\"c1\":" + p1Color
            + ",\"r1\":" + p1Rainbow
            + ",\"c2\":" + p2Color
            + ",\"r2\":" + p2Rainbow
            + ",\"act\":\"" + act + "\""
            + ",\"action\":\"" + act + "\""
            + ",\"ball\":" + gameOrMatchBall
            + ",\"fin\":" + matchFinished
            + ",\"paused\":" + paused + "}\n";
    }

    static String buildMatchEndedJsonLine(String winnerName, String finalScore) {
        String safeWinner = escapeJson(winnerName != null && !winnerName.trim().isEmpty() ? winnerName.trim() : "PLAYER");
        String safeScore = escapeJson(finalScore != null ? finalScore.trim() : "");
        return "{\"type\":\"MATCH_ENDED\",\"winner\":\"" + safeWinner + "\",\"score\":\"" + safeScore + "\"}\n";
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length() + 4);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString();
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

    private void closeClientSocket() {
        BluetoothSocket sock;
        synchronized (writeLock) {
            sock = activeClientSocket;
            activeClientSocket = null;
            activeClientOutput = null;
        }
        closeQuietly(sock);
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
