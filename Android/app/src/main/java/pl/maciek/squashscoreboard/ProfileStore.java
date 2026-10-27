package pl.maciek.squashscoreboard;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.maciek.squashscoreboard.lootbox.RewardCatalog;
import pl.maciek.squashscoreboard.lootbox.RewardInventory;
import pl.maciek.squashscoreboard.lootbox.SecureStorageEngine;

/**
 * [MASVS-STORAGE] [ASVS V4.1 / V4.2] [PERF-120HZ]
 * Encrypted player profile repository with lock-free in-memory read caching and
 * background write-behind persistence.
 * - Eliminates the hardcoded `"developer-profile"` / `"deweloper"` auto-creation backdoor.
 * - Encrypts profile records at rest with AES-256-GCM (`SecureStorageEngine`).
 * - Executes all JSON serialization and disk writes on `AsyncRepositoryExecutor`.
 */
public final class ProfileStore {
    private static final String PREFS_FILE = "profiles";
    private static final String KEY_ITEMS = "items";

    // TEMPORARY: Initial storage configuration that seeds two players ("Kacper" & "Łukasz")
    // with the default "rainbow" ("Living Rainbow") color drop unlocked for both.
    // Remove this temporary configuration once initial testing/onboarding seeding is no longer needed.
    static final String[] TEMP_INITIAL_PLAYERS = {"Kacper", "Łukasz"};
    static final String TEMP_DEFAULT_DROP_ID = "rainbow";

    private static final CopyOnWriteArrayList<PlayerProfile> CACHE = new CopyOnWriteArrayList<>();
    private static volatile boolean initialized;

    private ProfileStore() { }

    public static List<PlayerProfile> load(Context c) {
        ensureLoaded(c);
        return Collections.unmodifiableList(new ArrayList<>(CACHE));
    }

    public static void save(Context c, List<PlayerProfile> profiles) {
        if (profiles == null) return;
        ensureLoaded(c);
        List<PlayerProfile> sanitized = new ArrayList<>(profiles.size());
        for (PlayerProfile p : profiles) {
            if (p != null) sanitized.add(p);
        }
        CACHE.clear();
        CACHE.addAll(sanitized);
        persistAsync(c, sanitized);
    }

    public static void add(Context c, PlayerProfile p) {
        if (p == null) return;
        ensureLoaded(c);
        CACHE.add(p);
        persistAsync(c, new ArrayList<>(CACHE));
    }

    public static PlayerProfile find(Context c, String id) {
        if (!InputValidator.isValidProfileId(id)) return null;
        ensureLoaded(c);
        for (PlayerProfile p : CACHE) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    public static PlayerProfile findByName(Context c, String name) {
        if (name == null) return null;
        String clean = name.trim();
        if (clean.isEmpty()) return null;
        ensureLoaded(c);
        for (PlayerProfile p : CACHE) {
            if (p.name.equalsIgnoreCase(clean)) return p;
        }
        return null;
    }

    public static void update(Context c, PlayerProfile profile) {
        if (profile == null || !InputValidator.isValidProfileId(profile.id)) return;
        ensureLoaded(c);
        boolean updated = false;
        for (int i = 0; i < CACHE.size(); i++) {
            if (CACHE.get(i).id.equals(profile.id)) {
                CACHE.set(i, profile);
                updated = true;
                break;
            }
        }
        if (updated) {
            persistAsync(c, new ArrayList<>(CACHE));
        }
    }

    public static void remove(Context c, String id) {
        if (!InputValidator.isValidProfileId(id)) return;
        ensureLoaded(c);
        boolean removed = CACHE.removeIf(p -> p.id.equals(id));
        if (removed) {
            persistAsync(c, new ArrayList<>(CACHE));
            RewardInventory.forgetProfile(c, id);
        }
    }

    private static void ensureLoaded(Context c) {
        if (initialized || c == null) return;
        synchronized (ProfileStore.class) {
            if (initialized) return;
            Context appContext = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            ArrayList<PlayerProfile> loaded = new ArrayList<>();
            boolean migrate = false;
            try {
                String raw = SecureStorageEngine.getEncryptedString(appContext, PREFS_FILE, KEY_ITEMS, "[]");
                JSONArray a = new JSONArray(raw);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    String rawId = o.optString("id", "");
                    String rawName = o.optString("name", "");
                    // Drop the legacy auto-injected "developer-profile" backdoor account if empty/synthetic
                    if ("developer-profile".equals(rawId) && "deweloper".equalsIgnoreCase(rawName)) {
                        migrate = true;
                        continue;
                    }
                    if (!o.has("id") || rawId.startsWith("legacy-")) {
                        migrate = true;
                    }
                    loaded.add(PlayerProfile.from(o));
                }
            } catch (Exception ignored) { }

            List<String> ids = new ArrayList<>(loaded.size());
            for (PlayerProfile p : loaded) ids.add(p.id);
            RewardInventory.migrateLegacy(appContext, ids);

            // TEMPORARY: Seed initial players "Kacper" and "Łukasz" and grant both the default "rainbow" drop.
            if (seedTemporaryInitialPlayers(appContext, loaded)) {
                migrate = true;
            }

            CACHE.clear();
            CACHE.addAll(loaded);
            initialized = true;

            if (migrate) {
                persistAsync(appContext, new ArrayList<>(CACHE));
            }
        }
    }

    /**
     * TEMPORARY: Ensures "Kacper" and "Łukasz" exist in initial profile storage and unlocks
     * the default "rainbow" ("Living Rainbow") reward drop for both players.
     */
    static boolean seedTemporaryInitialPlayers(Context appContext, List<PlayerProfile> profiles) {
        RewardCatalog.Reward rainbow = RewardCatalog.byId(TEMP_DEFAULT_DROP_ID);
        int defaultColor = rainbow != null ? rainbow.key() : AppPreferences.BLUE;
        boolean modified = false;
        for (String playerName : TEMP_INITIAL_PLAYERS) {
            PlayerProfile target = null;
            for (PlayerProfile existing : profiles) {
                if (existing.name.equalsIgnoreCase(playerName)) {
                    target = existing;
                    break;
                }
            }
            if (target == null) {
                target = new PlayerProfile(playerName, defaultColor);
                profiles.add(target);
                modified = true;
            }
            if (rainbow != null && appContext != null) {
                RewardInventory.unlock(appContext, target.id, rainbow);
            }
        }
        return modified;
    }

    private static void persistAsync(Context c, List<PlayerProfile> snapshot) {
        if (c == null) return;
        final Context appContext = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        AsyncRepositoryExecutor.runIo(() -> {
            JSONArray a = new JSONArray();
            try {
                for (PlayerProfile p : snapshot) a.put(p.json());
            } catch (Exception e) {
                return;
            }
            SecureStorageEngine.putEncryptedString(appContext, PREFS_FILE, KEY_ITEMS, a.toString());
        });
    }
}
