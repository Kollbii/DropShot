package pl.maciek.squashscoreboard;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
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
            SupabaseSyncClient.syncProfileInBackground(c, profile);
        }
    }

    public static void remove(Context c, String id) {
        if (!InputValidator.isValidProfileId(id)) return;
        ensureLoaded(c);
        boolean removed = CACHE.removeIf(p -> p.id.equals(id));
        if (removed) {
            persistAsync(c, new ArrayList<>(CACHE));
            RewardInventory.forgetProfile(c, id);
            CloudConfigStore.unlinkProfile(c, id);
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

            CACHE.clear();
            CACHE.addAll(loaded);
            initialized = true;

            if (migrate) {
                persistAsync(appContext, new ArrayList<>(CACHE));
            }
        }
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
