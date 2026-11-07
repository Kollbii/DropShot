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
 * - Eliminates the hardcoded developer privilege-escalation bypass.
 * - Encrypts unlocked entitlement state via `SecureStorageEngine` (AES-256-GCM + AAD).
 * - Guarantees zero disk I/O during 120 Hz `onDraw()` rendering passes.
 */
public final class RewardInventory {
    public interface OnUnlockListener {
        void onRewardUnlocked(Context context, String profileId, RewardCatalog.Reward reward);
    }

    private static final String FILE = "reward_inventory";
    private static final String ENCRYPTED_STATE_KEY = "encrypted_unlocks_v2";
    private static final Set<String> UNLOCKED_CACHE = ConcurrentHashMap.newKeySet();
    private static volatile boolean cacheLoaded;
    private static volatile OnUnlockListener unlockListener;
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "dropshot-inventory-io");
        t.setDaemon(true);
        return t;
    });

    private RewardInventory() {}

    public static void setOnUnlockListener(OnUnlockListener listener) {
        unlockListener = listener;
    }

    public static boolean owns(Context context, String profileId, RewardCatalog.Reward reward) {
        if (reward == null) return false;
        if (isStarter(reward.id)) return true;
        if (!LootboxAuthoritativeService.isValidProfileId(profileId)) return false;
        ensureLoaded(context);
        return UNLOCKED_CACHE.contains(cacheKey(profileId, reward.id));
    }

    public static void unlock(Context context, String profileId, RewardCatalog.Reward reward) {
        if (recordUnlock(context, profileId, reward)) {
            OnUnlockListener listener = unlockListener;
            if (listener != null && context != null) {
                listener.onRewardUnlocked(context, profileId, reward);
            }
        }
    }

    /** Restores an entitlement pulled from cloud storage without triggering an echo cloud push. */
    public static void restoreFromCloud(Context context, String profileId, RewardCatalog.Reward reward) {
        recordUnlock(context, profileId, reward);
    }

    private static boolean recordUnlock(Context context, String profileId, RewardCatalog.Reward reward) {
        if (!LootboxAuthoritativeService.isValidProfileId(profileId)) {
            throw new IllegalArgumentException("A valid player profile ID is required to unlock a reward");
        }
        if (reward == null) {
            throw new IllegalArgumentException("Reward is required");
        }
        ensureLoaded(context);
        if (UNLOCKED_CACHE.add(cacheKey(profileId, reward.id))) {
            persistAsync(context);
            return true;
        }
        return false;
    }

    /** Returns owned color rewards (compatible with callers expecting `Reward.key()`). */
    public static RewardCatalog.Reward[] available(Context context, String profileId) {
        List<RewardCatalog.Reward> result = new ArrayList<>();
        for (RewardCatalog.Reward reward : RewardCatalog.colors()) {
            if (owns(context, profileId, reward)) result.add(reward);
        }
        return result.toArray(new RewardCatalog.Reward[0]);
    }

    /** Returns all owned rewards across every category (colors, fonts, and future kinds) for cloud sync. */
    public static RewardCatalog.Reward[] availableAll(Context context, String profileId) {
        List<RewardCatalog.Reward> result = new ArrayList<>();
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            if (owns(context, profileId, reward)) result.add(reward);
        }
        return result.toArray(new RewardCatalog.Reward[0]);
    }

    /**
     * [ASVS V4.1] Hardcoded developer backdoor removed.
     * Kept as a constant false/null stub for binary/source compatibility without privilege escalation.
     */
    public static String developerId(Context context) {
        return null;
    }

    public static void setDeveloper(Context context, String profileId) {
        // Intentional no-op: client-side privilege escalation is prohibited by OWASP ASVS V4.1.
    }

    public static boolean isDeveloper(Context context, String profileId) {
        return false;
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
