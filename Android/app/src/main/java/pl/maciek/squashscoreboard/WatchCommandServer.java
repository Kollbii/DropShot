package pl.maciek.squashscoreboard;

import android.os.Handler;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;

/**
 * [MASVS-NETWORK] [MASVS-PLATFORM] [MASVS-CODE] [ASVS V2 / V4]
 * Hardened localhost-only HTTP bridge for the Zepp App Side Service (`127.0.0.1:28282`).
 * Security controls enforced:
 * 1. Explicit IPv4 loopback binding (`127.0.0.1`) + peer loopback address verification.
 * 2. DNS Rebinding & Cross-Origin Browser CSRF defense:
 *    - Validates `Host` header against loopback allowlist.
 *    - Rejects any request carrying browser `Origin`, `Referer`, or `Sec-Fetch-*` headers (`403`).
 * 3. Constant-time token verification (`MessageDigest.isEqual`) when a bridge token is configured.
 * 4. Strict request rate-limiting, header count caps, payload size bounds (`2..256` bytes),
 *    and strict command allowlist (`InputValidator.isAllowedWatchCommand`).
 */
final class WatchCommandServer {
    static final int PORT = 28282;
    private static final String PATH = "/zepp-command";
    private static final String TAG = "WatchCommandServer";
    private static final int MAX_HEADER_LINES = 24;
    private static final long MIN_COMMAND_INTERVAL_NANOS = 40_000_000L; // Max 25 req/sec

    interface CommandListener { boolean onCommand(String command); }

    private final Handler mainHandler;
    private final CommandListener listener;
    private final AtomicLong lastCommandNanos = new AtomicLong(0L);
    private volatile String expectedToken;
    private volatile boolean stopping;
    private ServerSocket serverSocket;
    private Thread worker;

    WatchCommandServer(Handler mainHandler, CommandListener listener) {
        this.mainHandler = mainHandler;
        this.listener = listener;
    }

    void setExpectedToken(String token) {
        this.expectedToken = (token != null && !token.trim().isEmpty()) ? token.trim() : null;
    }

    synchronized void start() {
        if (worker != null && worker.isAlive()) return;
        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 8);
            serverSocket = socket;
            stopping = false;
            worker = new Thread(() -> serve(socket), "zepp-watch-command-server");
            worker.setDaemon(true);
            worker.start();
        } catch (IOException e) {
            SafeLog.w(TAG, "Cannot start local Zepp bridge", e);
            closeServerSocket();
        }
    }

    synchronized void stop() {
        stopping = true;
        closeServerSocket();
        worker = null;
    }

    private void serve(ServerSocket listeningSocket) {
        while (!stopping && serverSocket == listeningSocket) {
            try (Socket socket = listeningSocket.accept()) {
                socket.setSoTimeout(2500);
                InetAddress remoteAddr = socket.getInetAddress();
                if (remoteAddr == null || !remoteAddr.isLoopbackAddress()) {
                    writeJson(socket, 403, "{\"accepted\":false}");
                    continue;
                }
                handle(socket);
            } catch (SocketException e) {
                if (!stopping) SafeLog.w(TAG, "Zepp bridge socket error", e);
            } catch (IOException e) {
                if (!stopping) SafeLog.w(TAG, "Zepp bridge request error", e);
            }
        }
    }

    private void handle(Socket socket) throws IOException {
        BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
        String requestLine = readLine(input);
        if (requestLine == null) return;
        String[] request = requestLine.split(" ");
        int contentLength = 0;
        String hostHeader = null;
        String tokenHeader = null;
        boolean browserHeaderDetected = false;

        String line;
        int headerCount = 0;
        while ((line = readLine(input)) != null && !line.isEmpty()) {
            if (++headerCount > MAX_HEADER_LINES) {
                writeJson(socket, 400, "{\"accepted\":false}");
                return;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim().toLowerCase(Locale.US);
                String value = line.substring(colon + 1).trim();
                switch (name) {
                    case "content-length":
                        try {
                            contentLength = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {
                            contentLength = -1;
                        }
                        break;
                    case "host":
                        hostHeader = value.toLowerCase(Locale.US);
                        break;
                    case "x-dropshot-token":
                        tokenHeader = value;
                        break;
                    case "origin":
                    case "referer":
                    case "sec-fetch-site":
                    case "sec-fetch-mode":
                        browserHeaderDetected = true;
                        break;
                    default:
                        break;
                }
            }
        }

        // [MASVS-NETWORK] Reject cross-origin browser requests and DNS rebinding attempts
        if (browserHeaderDetected || !isLoopbackHost(hostHeader)) {
            writeJson(socket, 403, "{\"accepted\":false}");
            return;
        }
        if (request.length < 2 || !"POST".equals(request[0]) || !PATH.equals(request[1])) {
            writeJson(socket, 404, "{\"accepted\":false}");
            return;
        }
        if (contentLength < 2 || contentLength > 256) {
            writeJson(socket, 400, "{\"accepted\":false}");
            return;
        }

        byte[] body = readExactly(input, contentLength);
        String command;
        String bodyToken;
        try {
            JSONObject payload = new JSONObject(new String(body, StandardCharsets.UTF_8));
            command = payload.optString("command", "");
            bodyToken = payload.optString("token", null);
        } catch (Exception e) {
            writeJson(socket, 400, "{\"accepted\":false}");
            return;
        }

        if (!verifyTokenIfConfigured(tokenHeader != null ? tokenHeader : bodyToken)) {
            writeJson(socket, 401, "{\"accepted\":false}");
            return;
        }

        if (!InputValidator.isAllowedWatchCommand(command)) {
            writeJson(socket, 400, "{\"accepted\":false}");
            return;
        }

        long nowNanos = System.nanoTime();
        long prevNanos = lastCommandNanos.get();
        if (nowNanos - prevNanos < MIN_COMMAND_INTERVAL_NANOS
            || !lastCommandNanos.compareAndSet(prevNanos, nowNanos)) {
            writeJson(socket, 429, "{\"accepted\":false}");
            return;
        }

        SafeLog.i(TAG, "Received command from Zepp: " + command);

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
                writeJson(socket, 503, "{\"accepted\":false}");
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeJson(socket, 503, "{\"accepted\":false}");
            return;
        }
        SafeLog.i(TAG, "Processed Zepp command " + command + ", accepted=" + accepted[0]);
        writeJson(socket, 200, "{\"accepted\":" + accepted[0] + "}");
    }

    static boolean isLoopbackHost(String host) {
        if (host == null || host.isEmpty()) return true;
        return "127.0.0.1:28282".equals(host)
            || "127.0.0.1".equals(host)
            || "localhost:28282".equals(host)
            || "localhost".equals(host);
    }

    boolean verifyTokenIfConfigured(String providedToken) {
        String configured = this.expectedToken;
        if (configured == null) return true;
        if (providedToken == null) return false;
        byte[] expectedBytes = configured.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = providedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') break;
            if (value != '\r') line.write(value);
            if (line.size() > 1024) throw new IOException("HTTP header too large");
        }
        return value == -1 && line.size() == 0 ? null : line.toString(StandardCharsets.US_ASCII.name());
    }

    private static byte[] readExactly(InputStream input, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(data, offset, length - offset);
            if (count < 0) throw new EOFException();
            offset += count;
        }
        return data;
    }

    private static void writeJson(Socket socket, int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        String reason = status == 200 ? "OK" : (status == 400 ? "Bad Request" : (status == 401 ? "Unauthorized" : (status == 403 ? "Forbidden" : (status == 404 ? "Not Found" : (status == 429 ? "Too Many Requests" : "Service Unavailable")))));
        OutputStream output = socket.getOutputStream();
        output.write(("HTTP/1.1 " + status + " " + reason + "\r\nContent-Type: application/json\r\nX-Content-Type-Options: nosniff\r\nCache-Control: no-store\r\nContent-Length: " + data.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        output.write(data);
        output.flush();
    }

    private synchronized void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) { }
            serverSocket = null;
        }
    }
}
