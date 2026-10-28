package pl.maciek.squashscoreboard;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.maciek.squashscoreboard.lootbox.RewardCatalog;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;

/**
 * [MASVS-NETWORK] [MASVS-AUTH] [PERF-120HZ]
 * Zero-dependency PostgREST HTTP client for Supabase synchronization.
 * - Uses only Android framework `HttpsURLConnection` and `org.json` (100% GrapheneOS compatible,
 *   zero Google Play Services dependencies).
 * - Enforces HTTPS-only connections, strict connect/read timeouts, and response size bounds.
 * - Authenticates player requests via `x-player-token-hash` (SHA-256 hex digest) evaluated by
 *   PostgreSQL Row Level Security (RLS).
 * - Runs all network I/O on a background thread and dispatches UI callbacks on the main thread.
 */
public final class SupabaseSyncClient {
    private static final String TAG = "DropShotCloud";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_RESPONSE_BYTES = 512 * 1024; // 512 KB cap

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String userMessage);
    }

    public static final class PulledPlayer {
        public final PlayerProfile profile;
        public final int syncedMatchesCount;

        public PulledPlayer(PlayerProfile profile, int syncedMatchesCount) {
            this.profile = profile;
            this.syncedMatchesCount = syncedMatchesCount;
        }
    }

    private SupabaseSyncClient() {}

    /**
     * Pulls a cloud player profile, unlocked rewards, and match history from Supabase using
     * the SHA-256 hash of `rawSecretToken` in `x-player-token-hash`.
     */
    public static void pullPlayerAsync(
        Context context,
        String rawSecretToken,
        Callback<PulledPlayer> callback
    ) {
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        AsyncRepositoryExecutor.runIo(() -> {
            try {
                String baseUrl = CloudConfigStore.getProjectUrl(appContext);
                String anonKey = CloudConfigStore.getAnonKey(appContext);
                if (!CloudConfigStore.isValidHttpsEndpoint(baseUrl) || anonKey.isEmpty()) {
                    dispatchError(callback, "Cloud settings are missing. The app needs the Supabase URL and key. Open Cloud settings and enter both values.");
                    return;
                }
                String tokenHash = CloudConfigStore.sha256Hex(rawSecretToken);

                // 1. Fetch player row filtered by RLS on x-player-token-hash
                String playersEndpoint = baseUrl + "/rest/v1/cloud_players?select=*&limit=1";
                HttpResponse playerResp = executeRequest("GET", playersEndpoint, anonKey, tokenHash, null, null);
                if (playerResp.statusCode < 200 || playerResp.statusCode >= 300) {
                    dispatchError(callback, formatHttpError("The app could not pull the player from the cloud.", playerResp.statusCode));
                    return;
                }

                JSONArray playersArray = new JSONArray(playerResp.body);
                if (playersArray.length() == 0) {
                    dispatchError(
                        callback,
                        "No cloud player matched this token. The secret token is not registered in Supabase. Check the token or create a cloud player."
                    );
                    return;
                }

                JSONObject playerObj = playersArray.getJSONObject(0);
                String cloudId = InputValidator.sanitizeProfileIdOrNull(playerObj.optString("id", null));
                String cloudName = InputValidator.sanitizePlayerName(playerObj.optString("name", "Player"), "Player");
                int cloudColor = playerObj.optInt("color", AppPreferences.BLUE);
                if (cloudId == null) {
                    cloudId = InputValidator.generateProfileUuid();
                }

                // 2. Fetch player's cloud match records
                String matchesEndpoint = baseUrl
                    + "/rest/v1/cloud_player_matches?select=record_json&order=finished_at.desc&limit=100";
                HttpResponse matchesResp = executeRequest("GET", matchesEndpoint, anonKey, tokenHash, null, null);
                List<MatchRecord> cloudMatches = new ArrayList<>();
                if (matchesResp.statusCode >= 200 && matchesResp.statusCode < 300 && !matchesResp.body.isEmpty()) {
                    JSONArray matchesArr = new JSONArray(matchesResp.body);
                    for (int i = 0; i < matchesArr.length(); i++) {
                        JSONObject row = matchesArr.optJSONObject(i);
                        if (row == null) continue;
                        JSONObject recJson = row.optJSONObject("record_json");
                        if (recJson != null) {
                            try {
                                cloudMatches.add(MatchRecord.fromJson(recJson));
                            } catch (Exception ignored) { }
                        }
                    }
                }

                // 3. Merge profile locally (reuse existing profile if ID or name matches)
                PlayerProfile existingById = ProfileStore.find(appContext, cloudId);
                PlayerProfile existingByName = ProfileStore.findByName(appContext, cloudName);
                PlayerProfile resolvedProfile;
                if (existingById != null) {
                    String oldName = existingById.name;
                    resolvedProfile = new PlayerProfile(cloudId, cloudName, cloudColor);
                    ProfileStore.update(appContext, resolvedProfile);
                    if (!oldName.equals(cloudName)) {
                        MatchHistoryStore.renameProfile(appContext, cloudId, oldName, cloudName);
                    }
                } else if (existingByName != null) {
                    // Replace local profile ID with cloud profile ID so future matches link cleanly
                    List<PlayerProfile> current = new ArrayList<>(ProfileStore.load(appContext));
                    resolvedProfile = new PlayerProfile(cloudId, cloudName, cloudColor);
                    for (int i = 0; i < current.size(); i++) {
                        if (current.get(i).id.equals(existingByName.id)) {
                            current.set(i, resolvedProfile);
                            break;
                        }
                    }
                    ProfileStore.save(appContext, current);
                    MatchHistoryStore.renameProfile(appContext, cloudId, existingByName.name, cloudName);
                } else {
                    resolvedProfile = new PlayerProfile(cloudId, cloudName, cloudColor);
                    ProfileStore.add(appContext, resolvedProfile);
                }

                // 4. Restore unlocked rewards from cloud
                JSONArray unlockedRewards = playerObj.optJSONArray("unlocked_rewards");
                if (unlockedRewards != null) {
                    for (int i = 0; i < unlockedRewards.length(); i++) {
                        String rewardId = unlockedRewards.optString(i, null);
                        RewardCatalog.Reward reward = RewardCatalog.byId(rewardId);
                        if (reward != null) {
                            RewardInventory.unlock(appContext, resolvedProfile.id, reward);
                        }
                    }
                }
                RewardCatalog.Reward equippedReward = RewardCatalog.byKey(cloudColor);
                if (equippedReward != null) {
                    RewardInventory.unlock(appContext, resolvedProfile.id, equippedReward);
                }

                // 5. Merge cloud match history into local MatchHistoryStore
                for (int i = cloudMatches.size() - 1; i >= 0; i--) {
                    MatchHistoryStore.add(appContext, cloudMatches.get(i));
                }

                // 6. Save token hash for automatic background sync
                CloudConfigStore.linkProfileTokenHash(appContext, resolvedProfile.id, tokenHash);

                final PulledPlayer result = new PulledPlayer(resolvedProfile, cloudMatches.size());
                AsyncRepositoryExecutor.runOnMain(() -> {
                    if (callback != null) callback.onSuccess(result);
                });
            } catch (IllegalArgumentException e) {
                dispatchError(callback, e.getMessage());
            } catch (Exception e) {
                SafeLog.w(TAG, "Cloud pull failed", e);
                dispatchError(
                    callback,
                    "The cloud connection failed. The network or the Supabase URL did not respond. Check your internet connection and Cloud settings."
                );
            }
        });
    }

    /**
     * Registers or links a local player profile to Supabase using the given secret token,
     * uploading their current profile, unlocked rewards, career statistics, and match history.
     */
    public static void registerOrPushPlayerAsync(
        Context context,
        PlayerProfile profile,
        String rawSecretToken,
        Callback<Void> callback
    ) {
        if (profile == null) {
            dispatchError(callback, "The player profile is missing.");
            return;
        }
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        AsyncRepositoryExecutor.runIo(() -> {
            try {
                String baseUrl = CloudConfigStore.getProjectUrl(appContext);
                String anonKey = CloudConfigStore.getAnonKey(appContext);
                if (!CloudConfigStore.isValidHttpsEndpoint(baseUrl) || anonKey.isEmpty()) {
                    dispatchError(callback, "Cloud settings are missing. The app needs the Supabase URL and key. Open Cloud settings and enter both values.");
                    return;
                }
                String tokenHash = CloudConfigStore.sha256Hex(rawSecretToken);
                CloudConfigStore.linkProfileTokenHash(appContext, profile.id, tokenHash);

                boolean ok = pushPlayerStateSync(appContext, profile, tokenHash, null);
                if (!ok) {
                    dispatchError(
                        callback,
                        "The app could not upload the player to Supabase. Another player may already use this token, or RLS rejected the request. Check the secret token and try again."
                    );
                    return;
                }
                AsyncRepositoryExecutor.runOnMain(() -> {
                    if (callback != null) callback.onSuccess(null);
                });
            } catch (IllegalArgumentException e) {
                dispatchError(callback, e.getMessage());
            } catch (Exception e) {
                SafeLog.w(TAG, "Cloud registration failed", e);
                dispatchError(
                    callback,
                    "The cloud connection failed. The network or the Supabase URL did not respond. Check your internet connection and Cloud settings."
                );
            }
        });
    }

    /**
     * Automatically pushes updated statistics and the current match record in the background
     * for any player in `record` that is linked to a cloud token.
     * Never blocks the UI thread and fails silently when offline.
     */
    public static void syncMatchInBackground(Context context, MatchRecord record) {
        if (context == null || record == null) return;
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        if (!CloudConfigStore.isServerConfigured(appContext)) return;

        AsyncRepositoryExecutor.runIo(() -> {
            syncParticipantIfLinked(appContext, record.playerOneProfileId, record.playerOneName, record);
            syncParticipantIfLinked(appContext, record.playerTwoProfileId, record.playerTwoName, record);
        });
    }

    /**
     * Pushes a player's latest profile (name/color/unlocks) and statistics in the background.
     */
    public static void syncProfileInBackground(Context context, PlayerProfile profile) {
        if (context == null || profile == null) return;
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        if (!CloudConfigStore.isServerConfigured(appContext)) return;
        final String tokenHash = CloudConfigStore.getTokenHash(appContext, profile.id);
        if (!CloudConfigStore.isValidTokenHash(tokenHash)) return;

        AsyncRepositoryExecutor.runIo(() -> {
            try {
                pushPlayerStateSync(appContext, profile, tokenHash, null);
            } catch (Exception e) {
                SafeLog.w(TAG, "Background profile sync skipped", e);
            }
        });
    }

    private static void syncParticipantIfLinked(
        Context appContext,
        String profileId,
        String fallbackName,
        MatchRecord latestMatch
    ) {
        PlayerProfile profile = ProfileStore.find(appContext, profileId);
        if (profile == null && fallbackName != null) {
            profile = ProfileStore.findByName(appContext, fallbackName);
        }
        if (profile == null) return;
        String tokenHash = CloudConfigStore.getTokenHash(appContext, profile.id);
        if (!CloudConfigStore.isValidTokenHash(tokenHash)) return;
        try {
            pushPlayerStateSync(appContext, profile, tokenHash, latestMatch);
        } catch (Exception e) {
            SafeLog.w(TAG, "Background match sync skipped", e);
        }
    }

    private static boolean pushPlayerStateSync(
        Context appContext,
        PlayerProfile profile,
        String tokenHash,
        MatchRecord latestMatch
    ) throws Exception {
        String baseUrl = CloudConfigStore.getProjectUrl(appContext);
        String anonKey = CloudConfigStore.getAnonKey(appContext);
        if (!CloudConfigStore.isValidHttpsEndpoint(baseUrl) || anonKey.isEmpty()) {
            return false;
        }

        // 1. Check if cloud_players row already exists for this token_hash
        String getEndpoint = baseUrl + "/rest/v1/cloud_players?select=id&limit=1";
        HttpResponse existingResp = executeRequest("GET", getEndpoint, anonKey, tokenHash, null, null);
        String targetCloudPlayerId = profile.id;
        boolean existsInCloud = false;
        if (existingResp.statusCode >= 200 && existingResp.statusCode < 300) {
            JSONArray arr = new JSONArray(existingResp.body);
            if (arr.length() > 0) {
                existsInCloud = true;
                String existingId = arr.getJSONObject(0).optString("id", profile.id);
                if (InputValidator.isValidProfileId(existingId)) {
                    targetCloudPlayerId = existingId;
                }
            }
        }

        PlayerStatistics.Summary summary = PlayerStatistics.calculate(appContext, profile);
        JSONArray unlockedJson = new JSONArray();
        for (RewardCatalog.Reward r : RewardInventory.available(appContext, profile.id)) {
            unlockedJson.put(r.id);
        }

        JSONObject playerPayload = new JSONObject();
        playerPayload.put("id", targetCloudPlayerId);
        playerPayload.put("token_hash", tokenHash);
        playerPayload.put("name", profile.name);
        playerPayload.put("color", profile.color);
        playerPayload.put("unlocked_rewards", unlockedJson);
        playerPayload.put("matches", summary.matches);
        playerPayload.put("wins", summary.wins);
        playerPayload.put("points", summary.points);
        playerPayload.put("won_sets", summary.wonSets);
        playerPayload.put("total_sets", summary.totalSets);
        playerPayload.put("score_67", summary.score67);
        playerPayload.put("score_69", summary.score69);

        HttpResponse upsertPlayerResp;
        if (existsInCloud) {
            String patchEndpoint = baseUrl + "/rest/v1/cloud_players?token_hash=eq." + tokenHash;
            JSONObject updatePayload = new JSONObject(playerPayload.toString());
            updatePayload.remove("id");
            upsertPlayerResp = executeRequest(
                "PATCH",
                patchEndpoint,
                anonKey,
                tokenHash,
                "return=minimal",
                updatePayload.toString()
            );
        } else {
            String postEndpoint = baseUrl + "/rest/v1/cloud_players?on_conflict=token_hash";
            upsertPlayerResp = executeRequest(
                "POST",
                postEndpoint,
                anonKey,
                tokenHash,
                "resolution=merge-duplicates,return=minimal",
                playerPayload.toString()
            );
        }

        if (upsertPlayerResp.statusCode < 200 || upsertPlayerResp.statusCode >= 300) {
            return false;
        }

        // 2. Upsert match record(s)
        if (latestMatch != null) {
            upsertSingleMatch(baseUrl, anonKey, tokenHash, targetCloudPlayerId, latestMatch);
        } else {
            // Upload all local matches belonging to this player
            List<MatchRecord> allMatches = MatchHistoryStore.load(appContext);
            for (MatchRecord r : allMatches) {
                if (PlayerStatistics.belongs(r, true, profile) || PlayerStatistics.belongs(r, false, profile)) {
                    upsertSingleMatch(baseUrl, anonKey, tokenHash, targetCloudPlayerId, r);
                }
            }
        }
        return true;
    }

    private static void upsertSingleMatch(
        String baseUrl,
        String anonKey,
        String tokenHash,
        String cloudPlayerId,
        MatchRecord record
    ) throws Exception {
        JSONObject matchPayload = new JSONObject();
        matchPayload.put("player_id", cloudPlayerId);
        matchPayload.put("match_id", record.id);
        matchPayload.put("finished", record.finished);
        matchPayload.put("finished_at", record.finishedAt);
        matchPayload.put("record_json", record.toJson());

        String endpoint = baseUrl + "/rest/v1/cloud_player_matches?on_conflict=player_id,match_id";
        executeRequest(
            "POST",
            endpoint,
            anonKey,
            tokenHash,
            "resolution=merge-duplicates,return=minimal",
            matchPayload.toString()
        );
    }

    private static HttpResponse executeRequest(
        String method,
        String endpointUrl,
        String anonKey,
        String tokenHash,
        String preferHeader,
        String jsonBody
    ) throws IOException {
        URL url = new URL(endpointUrl);
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Refusing non-HTTPS Supabase connection");
        }
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(false);

            if ("PATCH".equals(method)) {
                // HttpURLConnection on some Android versions does not allow direct "PATCH" without override
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            } else {
                conn.setRequestMethod(method);
            }

            conn.setRequestProperty("apikey", anonKey);
            conn.setRequestProperty("Authorization", "Bearer " + anonKey);
            conn.setRequestProperty("x-player-token-hash", tokenHash);
            conn.setRequestProperty("Accept", "application/json");

            if (preferHeader != null && !preferHeader.isEmpty()) {
                conn.setRequestProperty("Prefer", preferHeader);
            }

            if (jsonBody != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payloadBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(payloadBytes.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payloadBytes);
                    os.flush();
                }
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 200 && status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = readBoundedStream(stream);
            return new HttpResponse(status, responseBody);
        } finally {
            conn.disconnect();
        }
    }

    private static String readBoundedStream(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_RESPONSE_BYTES) {
                    throw new IOException("Response exceeded size cap");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String formatHttpError(String whatOccurred, int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return whatOccurred
                + " Supabase rejected the API key or RLS policy (HTTP " + statusCode + ")."
                + " Check the Supabase public key in Cloud settings.";
        }
        return whatOccurred
            + " The server returned HTTP " + statusCode + "."
            + " Check the Supabase URL and SQL tables.";
    }

    private static <T> void dispatchError(Callback<T> callback, String message) {
        if (callback == null) return;
        AsyncRepositoryExecutor.runOnMain(() -> callback.onError(message));
    }

    private static final class HttpResponse {
        final int statusCode;
        final String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body != null ? body : "";
        }
    }
}

