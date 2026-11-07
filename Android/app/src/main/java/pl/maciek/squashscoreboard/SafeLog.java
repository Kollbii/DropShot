package pl.maciek.squashscoreboard;

import android.util.Log;

/**
 * [MASVS-CODE] [MASVS-PRIVACY]
 * Production-safe logging wrapper that gates verbose/info output behind `Log.isLoggable`
 * and scrubs tokens, headers, or sensitive identifiers before writing to logcat.
 */
public final class SafeLog {
    private SafeLog() {}

    public static void i(String tag, String message) {
        try {
            if (Log.isLoggable(tag, Log.INFO)) {
                Log.i(tag, scrub(message));
            }
        } catch (Throwable ignored) {
            // Safe no-op in pure JVM unit tests where android.util.Log is stubbed
        }
    }

    public static void w(String tag, String message, Throwable error) {
        try {
            if (Log.isLoggable(tag, Log.WARN)) {
                Log.w(tag, scrub(message), error != null ? new SecurityException(scrub(error.getMessage())) : null);
            }
        } catch (Throwable ignored) {
            // Safe no-op in pure JVM unit tests
        }
    }

    public static String scrub(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("(?i)(bearer|token|signature|secret|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]")
            .replaceAll("[\\r\\n]", " ");
    }
}

