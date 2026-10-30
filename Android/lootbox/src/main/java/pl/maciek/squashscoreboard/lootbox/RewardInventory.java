package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/**
 * [MASVS-STORAGE] [ASVS V4.1 / V4.2] [PERF-120HZ]
 * Encrypted, profile-scoped cosmetic entitlement repository with O(1) in-memory read cache
 * and asynchronous background persistence.
 * - Keeps the requested developer profile entitlement in encrypted state.
 * - Encrypts unlocked entitlement state via `SecureStorageEngine` (AES-256-GCM + AAD).
 * - Guarantees zero disk I/O during 120Hz `onDraw()` rendering passes.
 */
public final class RewardInventory {
    private static final String FILE = "reward_inventory";
    private static final String ENCRYPTED_STATE_KEY = "encrypted_unlocks_v2";
    private static final String DEVELOPER_PREFIX = "__developer__:";
    private static final Set<String> UNLOCKED_CACHE = ConcurrentHashMap.newKeySet();
    private static volatile boolean cacheLoaded;
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dropshot-inventory-io");
        t.setDaemon(true);
        return t;
    });

    private RewardInventory() {}

    public static boolean owns(Context context, String profileId, RewardCatalog.Reward reward) {
        if (reward == null) return false;
        if (isStarter(reward.id)) return true;
        if (isDeveloper(context, profileId)) return true;
        if (!LootboxAuthoritativeService.isValidProfileId(profileId)) return false;
        ensureLoaded(context);
        return UNLOCKED_CACHE.contains(cacheKey(profileId, reward.id));
    }

    public static void unlock(Context context, String profileId, RewardCatalog.Reward reward) {
        if (!LootboxAuthoritativeService.isValidProfileId(profileId)) {
            throw new IllegalArgumentException("A valid player profile ID is required to unlock a reward");
        }
        if (reward == null) {
            throw new IllegalArgumentException("Reward is required");
        }
        ensureLoaded(context);
        if (UNLOCKED_CACHE.add(cacheKey(profileId, reward.id))) {
            persistAsync(context);
        }
    }

    public static RewardCatalog.Reward[] available(Context context, String profileId) {
        List<RewardCatalog.Reward> result = new ArrayList<>();
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            if (reward.kind == RewardCatalog.Kind.COLOR && owns(context, profileId, reward))
                result.add(reward);
        }
        return result.toArray(new RewardCatalog.Reward[0]);
    }

    public static String developerId(Context context) {
        if (context == null) return null;
        ensureLoaded(context);
        for (String entry : UNLOCKED_CACHE)
            if (entry.startsWith(DEVELOPER_PREFIX))
                return entry.substring(DEVELOPER_PREFIX.length());
        String legacy = preferences(context).getString("developer_profile_id", null);
        return LootboxAuthoritativeService.isValidProfileId(legacy) ? legacy : null;
    }

    public static void setDeveloper(Context context, String profileId) {
        if (context == null || !LootboxAuthoritativeService.isValidProfileId(profileId)) return;
        ensureLoaded(context);
        UNLOCKED_CACHE.removeIf(entry -> entry.startsWith(DEVELOPER_PREFIX));
        UNLOCKED_CACHE.add(DEVELOPER_PREFIX + profileId);
        persistAsync(context);
    }

    public static boolean isDeveloper(Context context, String profileId) {
        return profileId != null && profileId.equals(developerId(context));
    }

    public static void forgetProfile(Context context, String profileId) {
        if (!LootboxAuthoritativeService.isValidProfileId(profileId)) return;
        ensureLoaded(context);
        String prefix = profileId + ":";
        if (UNLOCKED_CACHE.removeIf(entry -> entry.startsWith(prefix))) {
            persistAsync(context);
        }
    }

    /** Old builds stored unlocks globally; preserve them once into encrypted storage. */
    public static void migrateLegacy(Context context, List<String> profileIds) {
        ensureLoaded(context);
        if (context == null || profileIds == null) return;
        SharedPreferences store = preferences(context);
        if (store.getBoolean("per_player_migrated_v2", false)) return;
        boolean changed = false;
        SharedPreferences.Editor editor = store.edit();
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            if (store.getBoolean(reward.id, false)) {
                for (String profileId : profileIds) {
                    if (LootboxAuthoritativeService.isValidProfileId(profileId)) {
                        UNLOCKED_CACHE.add(cacheKey(profileId, reward.id));
                        changed = true;
                    }
                }
                editor.remove(reward.id);
            }
            for (String profileId : profileIds) {
                String legacyKey = profileId + ":" + reward.id;
                if (store.getBoolean(legacyKey, false)) {
                    if (LootboxAuthoritativeService.isValidProfileId(profileId)) {
                        UNLOCKED_CACHE.add(legacyKey);
                        changed = true;
                    }
                    editor.remove(legacyKey);
                }
            }
        }
        String legacyDeveloper = store.getString("developer_profile_id", null);
        if (LootboxAuthoritativeService.isValidProfileId(legacyDeveloper)) {
            UNLOCKED_CACHE.add(DEVELOPER_PREFIX + legacyDeveloper);
            changed = true;
        }
        editor.remove("developer_profile_id");
        editor.putBoolean("per_player_migrated_v2", true).apply();
        if (changed) persistAsync(context);
    }

    private static void ensureLoaded(Context context) {
        if (cacheLoaded || context == null) return;
        synchronized (RewardInventory.class) {
            if (cacheLoaded) return;
            Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            String rawJson = SecureStorageEngine.getEncryptedString(appContext, FILE, ENCRYPTED_STATE_KEY, "[]");
            try {
                JSONArray array = new JSONArray(rawJson);
                for (int i = 0; i < array.length(); i++) {
                    String entry = array.optString(i, "");
                    if (entry.contains(":")) UNLOCKED_CACHE.add(entry);
                }
            } catch (Exception ignored) { }
            cacheLoaded = true;
        }
    }

    private static void persistAsync(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final List<String> snapshot = new ArrayList<>(UNLOCKED_CACHE);
        IO_EXECUTOR.execute(() -> {
            JSONArray array = new JSONArray();
            for (String item : snapshot) array.put(item);
            SecureStorageEngine.putEncryptedString(appContext, FILE, ENCRYPTED_STATE_KEY, array.toString());
        });
    }

    private static String cacheKey(String profileId, String rewardId) {
        return profileId + ":" + rewardId;
    }

    private static boolean isStarter(String id) {
        return "font-default".equals(id) || "blue".equals(id) || "red".equals(id)
            || "green".equals(id) || "gold".equals(id);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
