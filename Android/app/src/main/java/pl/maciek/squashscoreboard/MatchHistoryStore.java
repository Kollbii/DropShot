package pl.maciek.squashscoreboard;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import pl.maciek.squashscoreboard.lootbox.SecureStorageEngine;

/**
 * [MASVS-STORAGE] [PERF-120HZ]
 * Encrypted match history repository with lock-free in-memory caching and
 * background write-behind persistence.
 * - Eliminates main-thread JSON deserialization on every screen draw and statistics calculation.
 * - Encrypts match records at rest via `SecureStorageEngine` (AES-256-GCM + AAD).
 */
public final class MatchHistoryStore {
    private static final String PREFS = "match_history";
    private static final String KEY = "records";
    private static final int MAX_RECORDS = 100;

    private static final CopyOnWriteArrayList<MatchRecord> CACHE = new CopyOnWriteArrayList<>();
    private static volatile boolean initialized;

    private MatchHistoryStore() { }

    public static List<MatchRecord> load(Context context) {
        ensureLoaded(context);
        return new ArrayList<>(CACHE);
    }

    public static void add(Context context, MatchRecord record) {
        if (record == null) return;
        ensureLoaded(context);
        synchronized (MatchHistoryStore.class) {
            boolean replaced = false;
            for (int i = 0; i < CACHE.size(); i++) {
                if (CACHE.get(i).id == record.id) {
                    CACHE.set(i, record);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                CACHE.add(0, record);
            }
            while (CACHE.size() > MAX_RECORDS) {
                CACHE.remove(CACHE.size() - 1);
            }
            persistAsync(context, new ArrayList<>(CACHE));
        }
    }

    public static void remove(Context context, long id) {
        ensureLoaded(context);
        synchronized (MatchHistoryStore.class) {
            if (CACHE.removeIf(r -> r.id == id)) {
                persistAsync(context, new ArrayList<>(CACHE));
            }
        }
    }

    public static void removePlayer(Context context, String player) {
        if (player == null) return;
        ensureLoaded(context);
        synchronized (MatchHistoryStore.class) {
            if (CACHE.removeIf(r -> r.playerOneName.equals(player) || r.playerTwoName.equals(player))) {
                persistAsync(context, new ArrayList<>(CACHE));
            }
        }
    }

    public static void renameProfile(Context context, String profileId, String oldName, String newName) {
        if (!InputValidator.isValidProfileId(profileId)) return;
        String safeNewName = InputValidator.sanitizePlayerName(newName, oldName);
        ensureLoaded(context);
        synchronized (MatchHistoryStore.class) {
            boolean changed = false;
            for (int i = 0; i < CACHE.size(); i++) {
                MatchRecord r = CACHE.get(i);
                boolean one = profileId.equals(r.playerOneProfileId) || (r.playerOneProfileId == null && oldName != null && oldName.equalsIgnoreCase(r.playerOneName));
                boolean two = profileId.equals(r.playerTwoProfileId) || (r.playerTwoProfileId == null && oldName != null && oldName.equalsIgnoreCase(r.playerTwoName));
                if (one || two) {
                    CACHE.set(i, new MatchRecord(
                        r.id,
                        one ? safeNewName : r.playerOneName,
                        two ? safeNewName : r.playerTwoName,
                        one ? profileId : r.playerOneProfileId,
                        two ? profileId : r.playerTwoProfileId,
                        r.playerOneSets,
                        r.playerTwoSets,
                        r.playerOnePoints,
                        r.playerTwoPoints,
                        r.pointsToWinSet,
                        r.setsToWinMatch,
                        r.finished,
                        r.finishedAt,
                        r.pointWinners,
                        r.durationMs,
                        r.setDurationsMs,
                        r.pointTimesMs,
                        r.paused,
                        r.playerOneColor,
                        r.playerTwoColor
                    ));
                    changed = true;
                }
            }
            if (changed) {
                persistAsync(context, new ArrayList<>(CACHE));
            }
        }
    }

    private static void ensureLoaded(Context context) {
        if (initialized || context == null) return;
        synchronized (MatchHistoryStore.class) {
            if (initialized) return;
            Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            ArrayList<MatchRecord> loaded = new ArrayList<>();
            try {
                String source = SecureStorageEngine.getEncryptedString(appContext, PREFS, KEY, "[]");
                JSONArray values = new JSONArray(source);
                int limit = Math.min(values.length(), MAX_RECORDS);
                for (int i = 0; i < limit; i++) {
                    loaded.add(MatchRecord.fromJson(values.getJSONObject(i)));
                }
            } catch (Exception ignored) { }
            CACHE.clear();
            CACHE.addAll(loaded);
            initialized = true;
        }
    }

    private static void persistAsync(Context context, List<MatchRecord> snapshot) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        AsyncRepositoryExecutor.runIo(() -> {
            JSONArray values = new JSONArray();
            try {
                for (MatchRecord value : snapshot) {
                    values.put(value.toJson());
                }
            } catch (Exception ignored) {
                return;
            }
            SecureStorageEngine.putEncryptedString(appContext, PREFS, KEY, values.toString());
        });
    }
}
