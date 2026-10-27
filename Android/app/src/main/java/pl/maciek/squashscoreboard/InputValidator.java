package pl.maciek.squashscoreboard;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * [MASVS-CODE] [MASVS-PLATFORM] [ASVS V4.2 / V5.1]
 * Centralized runtime schema and bounds validator for untrusted inputs
 * (Intent extras, JSON records, localhost bridge payloads, and user-entered text).
 */
public final class InputValidator {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int MAX_POINTS_PER_SET = 99;
    public static final int MAX_SETS_PER_MATCH = 25;
    public static final int MAX_RECORDED_POINTS = 5000;
    public static final long MAX_MATCH_DURATION_MS = 24L * 60L * 60L * 1000L; // 24 hours

    private static final Pattern SAFE_PROFILE_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Pattern CONTROL_AND_BIDI = Pattern.compile("[\\p{Cntrl}\\u202A-\\u202E\\u2066-\\u2069]");
    private static final SecureRandom CSPRNG = new SecureRandom();

    private InputValidator() {}

    /**
     * Generates a cryptographically random positive 63-bit identifier (replacing sequential timestamps).
     */
    public static long generateSecureMatchId() {
        long v;
        do {
            v = CSPRNG.nextLong() & Long.MAX_VALUE;
        } while (v == 0L);
        return v;
    }

    /**
     * Generates a cryptographically random UUIDv4 profile identifier.
     */
    public static String generateProfileUuid() {
        return UUID.randomUUID().toString();
    }

    public static boolean isValidProfileId(String id) {
        return id != null && SAFE_PROFILE_ID.matcher(id).matches();
    }

    public static String sanitizeProfileIdOrNull(String id) {
        return isValidProfileId(id) ? id : null;
    }

    /**
     * Strips control characters and Unicode BiDi override characters, trims whitespace,
     * and enforces a strict 32-character cap.
     */
    public static String sanitizePlayerName(String raw, String fallback) {
        String safeFallback = (fallback == null || fallback.isEmpty()) ? "PLAYER" : fallback;
        if (raw == null) return safeFallback;
        String cleaned = CONTROL_AND_BIDI.matcher(raw).replaceAll("").trim();
        if (cleaned.isEmpty()) return safeFallback;
        return cleaned.length() > MAX_NAME_LENGTH ? cleaned.substring(0, MAX_NAME_LENGTH) : cleaned;
    }

    public static String sanitizeUpperPlayerName(String raw, String fallback) {
        return sanitizePlayerName(raw, fallback).toUpperCase(Locale.ROOT);
    }

    public static int clampPointsPerSet(int value) {
        return Math.max(1, Math.min(MAX_POINTS_PER_SET, value));
    }

    public static int clampSetsPerMatch(int value) {
        return Math.max(1, Math.min(MAX_SETS_PER_MATCH, value));
    }

    public static int clampNonNegativeScore(int value, int maxAllowed) {
        return Math.max(0, Math.min(maxAllowed, value));
    }

    public static long clampDurationMs(long durationMs) {
        return Math.max(0L, Math.min(MAX_MATCH_DURATION_MS, durationMs));
    }

    public static boolean isAllowedWatchCommand(String command) {
        return "POINT_1".equals(command)
            || "POINT_2".equals(command)
            || "UNDO".equals(command)
            || "RESET".equals(command);
    }
}

