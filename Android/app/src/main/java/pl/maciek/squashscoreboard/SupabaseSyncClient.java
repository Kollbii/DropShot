package pl.maciek.squashscoreboard;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 *
 * <p><b>Database Golden Standard for AI Agents</b>:
 * Read `supabase/schema.sql`, `supabase/migrations/`, and `docs/agents/cloud-sync.md`
 * before you change any query, payload column, or synchronization rule in this class.
 * `supabase/schema.sql` is the single source of truth for how the Supabase PostgreSQL
 * database is and will be configured.
 *
 * <ul>
 *   <li>Uses only Android framework `HttpsURLConnection` and `org.json` (100% GrapheneOS
 *       compatible, zero Google Play Services dependencies).</li>
 *   <li>Enforces HTTPS-only connections, strict connect/read timeouts, and response size bounds.</li>
 *   <li>Authenticates player requests via `x-player-token-hash` (SHA-256 hex digest) evaluated by
 *       PostgreSQL Row Level Security (RLS).</li>
 *   <li>Writes universal JSONB expansion columns (`equipped_cosmetics`, `stats_ext`,
 *       `schema_version`) and falls back to base v1 columns automatically if the remote
 *       Supabase project has not applied `002_universal_cosmetics_and_stats.sql` yet.</li>
 *   <li>Runs all network I/O on a background thread and dispatches UI callbacks on the main thread.</li>
 * </ul>
 */
public final class SupabaseSyncClient {
    private static final String TAG = "DropShotCloud";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_RESPONSE_BYTES = 512 * 1024; // 512 KB cap
    public static final int CURRENT_SCHEMA_VERSION = 2;

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
     * Pulls a cloud player profile, equipped cosmetics, unlocked rewards, and match history
     * from Supabase using the SHA-256 hash of `rawSecretToken` in `x-player-token-hash`.
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

                // 1. Fetch player row filtered by token_hash and RLS on x-player-token-hash
                String playersEndpoint = baseUrl
                    + "/rest/v1/cloud_players?token_hash=eq." + tokenHash + "&select=*&limit=1";
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

                JSONObject equippedObj = optJsonObjectFlexible(playerObj, "equipped_cosmetics");
                boolean hasCloudFontField = equippedObj != null && equippedObj.has("font");
                String cloudFontId = hasCloudFontField
                    ? InputValidator.sanitizeCosmeticIdOrNull(equippedObj.optString("font", null))
                    : null;
                if (equippedObj != null && equippedObj.has("color")) {
                    cloudColor = equippedObj.optInt("color", cloudColor);
                }

                // 2. Fetch player's cloud match records filtered by player_id
                String matchesEndpoint = baseUrl
                    + "/rest/v1/cloud_player_matches?player_id=eq." + cloudId
                    + "&select=record_json&order=finished_at.desc&limit=100";
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
                            } catch (Exception ignored) {
                                // Skip malformed remote match JSON without aborting pull
                            }
                        }
                    }
                }

                // 3. Merge profile locally (reuse existing profile if ID or name matches)
                PlayerProfile existingById = ProfileStore.find(appContext, cloudId);
                PlayerProfile existingByName = ProfileStore.findByName(appContext, cloudName);
                PlayerProfile resolvedProfile;
                if (existingById != null) {
                    String oldName = existingById.name;
                    String mergedFontId = hasCloudFontField ? cloudFontId : existingById.fontId;
                    resolvedProfile = new PlayerProfile(cloudId, cloudName, cloudColor, mergedFontId);
                    ProfileStore.update(appContext, resolvedProfile);
                    if (!oldName.equals(cloudName)) {
                        MatchHistoryStore.renameProfile(appContext, cloudId, oldName, cloudName);
                    }
                } else if (existingByName != null) {
                    // Replace local profile ID with cloud profile ID so future matches link cleanly
                    List<PlayerProfile> current = new ArrayList<>(ProfileStore.load(appContext));
                    String mergedFontId = hasCloudFontField ? cloudFontId : existingByName.fontId;
                    resolvedProfile = new PlayerProfile(cloudId, cloudName, cloudColor, mergedFontId);
                    for (int i = 0; i < current.size(); i++) {
                        if (current.get(i).id.equals(existingByName.id)) {
                            current.set(i, resolvedProfile);
                            break;
                        }
                    }
                    ProfileStore.save(appContext, current);
                    MatchHistoryStore.renameProfile(appContext, cloudId, existingByName.name, cloudName);
                } else {
                    resolvedProfile = new PlayerProfile(cloudId, cloudName, cloudColor, cloudFontId);
                    ProfileStore.add(appContext, resolvedProfile);
                }

                // 4. Restore unlocked rewards (colors + fonts + future kinds) from cloud
                JSONArray unlockedRewards = playerObj.optJSONArray("unlocked_rewards");
                if (unlockedRewards == null) {
                    String raw = playerObj.optString("unlocked_rewards", null);
                    if (raw != null) {
                        String trimmed = raw.trim();
                        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                            try { unlockedRewards = new JSONArray(trimmed); } catch (Exception ignored) {}
                        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                            String inner = trimmed.substring(1, trimmed.length() - 1);
                            unlockedRewards = new JSONArray();
                            for (String part : inner.split(",")) {
                                String clean = part.replace("\"", "").trim();
                                if (!clean.isEmpty()) unlockedRewards.put(clean);
                            }
                        }
                    }
                }
                if (unlockedRewards != null) {
                    for (int i = 0; i < unlockedRewards.length(); i++) {
                        String rewardId = unlockedRewards.optString(i, null);
                        RewardCatalog.Reward reward = RewardCatalog.byId(rewardId);
                        if (reward != null) {
                            RewardInventory.restoreFromCloud(appContext, resolvedProfile.id, reward);
                        }
                    }
                }
                RewardCatalog.Reward equippedColorReward = RewardCatalog.byKey(cloudColor);
                if (equippedColorReward != null) {
                    RewardInventory.restoreFromCloud(appContext, resolvedProfile.id, equippedColorReward);
                }
                if (resolvedProfile.fontId != null) {
                    RewardCatalog.Reward equippedFontReward = RewardCatalog.byId(resolvedProfile.fontId);
                    if (equippedFontReward != null) {
                        RewardInventory.restoreFromCloud(appContext, resolvedProfile.id, equippedFontReward);
                    }
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
     * uploading their current profile, equipped cosmetics, unlocked rewards, career statistics,
     * and match history.
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

                int status = pushPlayerStateSync(appContext, profile, tokenHash, null);
                if (status < 200 || status >= 300) {
                    if (status == 401 || status == 403) {
                        dispatchError(
                            callback,
                            formatHttpError("The app could not upload the player to Supabase.", status)
                        );
                    } else {
                        dispatchError(
                            callback,
                            "The app could not upload the player to Supabase (HTTP " + status + "). Another player may already use this token, or RLS rejected the request. Check the secret token and try again."
                        );
                    }
                    return;
                }
                CloudConfigStore.linkProfileTokenHash(appContext, profile.id, tokenHash);
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
     * Pushes a player's latest profile (name, equipped cosmetics, unlocks) and statistics in the background.
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

    /**
     * Looks up `profileId` and syncs the profile in the background when a new lootbox reward unlocks.
     */
    public static void syncProfileByIdInBackground(Context context, String profileId) {
        if (context == null || !InputValidator.isValidProfileId(profileId)) return;
        PlayerProfile profile = ProfileStore.find(context, profileId);
        if (profile != null) {
            syncProfileInBackground(context, profile);
        }
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

    private static int pushPlayerStateSync(
        Context appContext,
        PlayerProfile profile,
        String tokenHash,
        MatchRecord latestMatch
    ) throws Exception {
        String baseUrl = CloudConfigStore.getProjectUrl(appContext);
        String anonKey = CloudConfigStore.getAnonKey(appContext);
        if (!CloudConfigStore.isValidHttpsEndpoint(baseUrl) || anonKey.isEmpty()) {
            return 0;
        }

        // 1. Check if cloud_players row already exists for this token_hash
        String getEndpoint = baseUrl
            + "/rest/v1/cloud_players?token_hash=eq." + tokenHash + "&select=id&limit=1";
        HttpResponse existingResp = executeRequest("GET", getEndpoint, anonKey, tokenHash, null, null);
        if (existingResp.statusCode == 401 || existingResp.statusCode == 403) {
            return existingResp.statusCode;
        }
        String targetCloudPlayerId = profile.id;
        if (existingResp.statusCode >= 200 && existingResp.statusCode < 300) {
            JSONArray arr = new JSONArray(existingResp.body);
            if (arr.length() > 0) {
                String existingId = arr.getJSONObject(0).optString("id", profile.id);
                if (InputValidator.isValidProfileId(existingId)) {
                    targetCloudPlayerId = existingId;
                }
            }
        }

        PlayerStatistics.Summary summary = PlayerStatistics.calculate(appContext, profile);
        JSONArray unlockedJson = new JSONArray();
        for (RewardCatalog.Reward r : RewardInventory.availableAll(appContext, profile.id)) {
            unlockedJson.put(r.id);
        }

        JSONObject equippedCosmetics = new JSONObject();
        equippedCosmetics.put("color", profile.color);
        equippedCosmetics.put("font", profile.fontId != null ? profile.fontId : "font-default");

        JSONObject statsExt = new JSONObject();
        statsExt.put("serves_total", summary.servesTotal);
        statsExt.put("serves_won", summary.servesWon);
        statsExt.put("receives_total", summary.receivesTotal);
        statsExt.put("receives_won", summary.receivesWon);
        statsExt.put("right_picks", summary.rightPicks);
        statsExt.put("left_picks", summary.leftPicks);
        statsExt.put("right_serves_total", summary.rightServesTotal);
        statsExt.put("right_serves_won", summary.rightServesWon);
        statsExt.put("left_serves_total", summary.leftServesTotal);
        statsExt.put("left_serves_won", summary.leftServesWon);
        statsExt.put("laplace_win_prob", summary.laplaceWinProbability());
        statsExt.put("wilson_lower_95", summary.wilsonLowerBound95());
        statsExt.put("markov_serve_share", summary.markovServeShare());

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
        playerPayload.put("equipped_cosmetics", equippedCosmetics);
        playerPayload.put("stats_ext", statsExt);
        playerPayload.put("schema_version", CURRENT_SCHEMA_VERSION);

        String postEndpoint = baseUrl + "/rest/v1/cloud_players?on_conflict=token_hash";
        HttpResponse upsertPlayerResp = executeRequest(
            "POST",
            postEndpoint,
            anonKey,
            tokenHash,
            "resolution=merge-duplicates,return=minimal",
            playerPayload.toString()
        );

        // Graceful backward compatibility if the Supabase DB has not applied v2 columns yet
        if (shouldFallbackToLegacySchema(upsertPlayerResp.statusCode, upsertPlayerResp.body)) {
            playerPayload.remove("equipped_cosmetics");
            playerPayload.remove("stats_ext");
            playerPayload.remove("schema_version");
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
            return upsertPlayerResp.statusCode;
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
        return upsertPlayerResp.statusCode;
    }

    static boolean shouldFallbackToLegacySchema(int statusCode, String errorBody) {
        if (statusCode != 400 || errorBody == null || errorBody.isEmpty()) {
            return false;
        }
        return errorBody.contains("PGRST204")
            || errorBody.contains("equipped_cosmetics")
            || errorBody.contains("stats_ext")
            || errorBody.contains("schema_version");
    }

    private static JSONObject optJsonObjectFlexible(JSONObject parent, String key) {
        if (parent == null || !parent.has(key) || parent.isNull(key)) return null;
        JSONObject direct = parent.optJSONObject(key);
        if (direct != null) return direct;
        String raw = parent.optString(key, null);
        if (raw != null) {
            String trimmed = raw.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    return new JSONObject(trimmed);
                } catch (Exception ignored) {
                    // Ignore malformed JSON string
                }
            }
        }
        return null;
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
            if (isJwtKey(anonKey)) {
                conn.setRequestProperty("Authorization", "Bearer " + anonKey);
            }
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

    static boolean isJwtKey(String key) {
        return key != null && key.trim().startsWith("eyJ");
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
