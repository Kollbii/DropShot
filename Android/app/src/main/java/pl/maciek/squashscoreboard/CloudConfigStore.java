package pl.maciek.squashscoreboard;

import android.content.Context;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import pl.maciek.squashscoreboard.lootbox.SecureStorageEngine;

/**
 * [MASVS-STORAGE] [MASVS-CRYPTO] [MASVS-AUTH]
 * Encrypted repository for runtime Supabase cloud configuration and per-profile secret token hashes.
 * - Zero hardcoded keys, URLs, or player tokens in code, resources, or build configuration.
 * - Encrypts the Supabase project URL, public key, and profile token hashes at rest via
 *   `SecureStorageEngine` (AES-256-GCM + AndroidKeyStore + AAD).
 * - Hashes each player secret token with SHA-256 so only the 64-character hex digest is stored
 *   and sent in the `x-player-token-hash` HTTP header.
 */
public final class CloudConfigStore {
    private static final String PREFS_FILE = "cloud_sync";
    private static final String KEY_CONFIG = "config_v1";

    private static volatile boolean initialized;
    private static volatile String projectUrl = "";
    private static volatile String anonKey = "";
    private static final Map<String, String> PROFILE_TOKEN_HASHES = new ConcurrentHashMap<>();

    private CloudConfigStore() {}

    public static String getProjectUrl(Context context) {
        ensureLoaded(context);
        return projectUrl;
    }

    public static String getAnonKey(Context context) {
        ensureLoaded(context);
        return anonKey;
    }

    public static boolean isServerConfigured(Context context) {
        ensureLoaded(context);
        return isValidHttpsEndpoint(projectUrl) && !anonKey.isEmpty();
    }

    public static void saveServerConfig(Context context, String rawUrl, String rawKey) {
        ensureLoaded(context);
        String cleanUrl = normalizeHttpsEndpoint(rawUrl);
        String cleanKey = sanitizeCredential(rawKey);
        if (!cleanUrl.isEmpty() && !isValidHttpsEndpoint(cleanUrl)) {
            throw new IllegalArgumentException("The Supabase URL must use https://.");
        }
        projectUrl = cleanUrl;
        anonKey = cleanKey;
        persistAsync(context);
    }

    public static boolean isCloudLinked(Context context, String profileId) {
        if (!InputValidator.isValidProfileId(profileId)) return false;
        ensureLoaded(context);
        String hash = PROFILE_TOKEN_HASHES.get(profileId);
        return isValidTokenHash(hash);
    }

    public static String getTokenHash(Context context, String profileId) {
        if (!InputValidator.isValidProfileId(profileId)) return null;
        ensureLoaded(context);
        return PROFILE_TOKEN_HASHES.get(profileId);
    }

    public static void linkProfileToken(Context context, String profileId, String rawSecretToken) {
        if (!InputValidator.isValidProfileId(profileId)) {
            throw new IllegalArgumentException("Valid profile ID is required.");
        }
        String hash = sha256Hex(rawSecretToken);
        linkProfileTokenHash(context, profileId, hash);
    }

    public static void linkProfileTokenHash(Context context, String profileId, String tokenHash) {
        if (!InputValidator.isValidProfileId(profileId) || !isValidTokenHash(tokenHash)) {
            throw new IllegalArgumentException("Valid profile ID and 64-char SHA-256 hash are required.");
        }
        ensureLoaded(context);
        PROFILE_TOKEN_HASHES.put(profileId, tokenHash.toLowerCase(Locale.US));
        persistAsync(context);
    }

    public static void unlinkProfile(Context context, String profileId) {
        if (!InputValidator.isValidProfileId(profileId)) return;
        ensureLoaded(context);
        if (PROFILE_TOKEN_HASHES.remove(profileId) != null) {
            persistAsync(context);
        }
    }

    /**
     * Computes the lowercase 64-character SHA-256 hex digest of a player secret token.
     * If the user enters an already-hashed 64-character hex string, normalizes and returns it.
     */
    public static String sha256Hex(String rawSecretToken) {
        String trimmed = sanitizeCredential(rawSecretToken);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("The player secret token is empty.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(trimmed.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format(Locale.US, "%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static boolean isValidTokenHash(String hash) {
        if (hash == null || hash.length() != 64) return false;
        for (int i = 0; i < 64; i++) {
            char c = hash.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) return false;
        }
        return true;
    }

    public static String normalizeHttpsEndpoint(String rawUrl) {
        if (rawUrl == null) return "";
        String clean = rawUrl.trim().replaceAll("[\\p{Cntrl}\\s]", "");
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    public static boolean isValidHttpsEndpoint(String rawUrl) {
        String clean = normalizeHttpsEndpoint(rawUrl);
        if (clean.isEmpty() || clean.length() > 256) return false;
        try {
            URI uri = new URI(clean);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return "https".equalsIgnoreCase(scheme)
                && host != null
                && !host.trim().isEmpty()
                && !host.contains("..")
                && uri.getUserInfo() == null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String sanitizeCredential(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("[\\p{Cntrl}]", "");
    }

    private static void ensureLoaded(Context context) {
        if (initialized || context == null) return;
        synchronized (CloudConfigStore.class) {
            if (initialized) return;
            Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            try {
                String raw = SecureStorageEngine.getEncryptedString(appContext, PREFS_FILE, KEY_CONFIG, "{}");
                JSONObject json = new JSONObject(raw);
                String loadedUrl = normalizeHttpsEndpoint(json.optString("url", ""));
                String loadedKey = sanitizeCredential(json.optString("anonKey", ""));
                if (isValidHttpsEndpoint(loadedUrl)) {
                    projectUrl = loadedUrl;
                }
                anonKey = loadedKey;
                JSONObject tokens = json.optJSONObject("tokens");
                PROFILE_TOKEN_HASHES.clear();
                if (tokens != null) {
                    Iterator<String> keys = tokens.keys();
                    while (keys.hasNext()) {
                        String profileId = keys.next();
                        String hash = tokens.optString(profileId, "");
                        if (InputValidator.isValidProfileId(profileId) && isValidTokenHash(hash)) {
                            PROFILE_TOKEN_HASHES.put(profileId, hash.toLowerCase(Locale.US));
                        }
                    }
                }
            } catch (Exception ignored) {
                // Corrupt or unreadable state defaults to empty configuration
            }
            initialized = true;
        }
    }

    private static void persistAsync(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final String snapshotUrl = projectUrl;
        final String snapshotKey = anonKey;
        final Map<String, String> snapshotTokens = new ConcurrentHashMap<>(PROFILE_TOKEN_HASHES);
        AsyncRepositoryExecutor.runIo(() -> {
            try {
                JSONObject root = new JSONObject();
                root.put("url", snapshotUrl);
                root.put("anonKey", snapshotKey);
                JSONObject tokens = new JSONObject();
                for (Map.Entry<String, String> entry : snapshotTokens.entrySet()) {
                    tokens.put(entry.getKey(), entry.getValue());
                }
                root.put("tokens", tokens);
                SecureStorageEngine.putEncryptedString(appContext, PREFS_FILE, KEY_CONFIG, root.toString());
            } catch (Exception ignored) {
                // Ignore JSON serialization error
            }
        });
    }
}

