package pl.maciek.squashscoreboard;

import android.content.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [PERF-120HZ] Shared, profile-aware statistics calculation.
 * Supports single-pass batch calculation (`calculateAll`) across all profiles using the
 * in-memory `MatchHistoryStore` cache to eliminate O(P * M) repeated JSON deserialization.
 */
public final class PlayerStatistics {
    public static final class Summary {
        public int matches, wins, points, wonSets, totalSets, score67, score69;
    }

    private PlayerStatistics() { }

    public static Summary calculate(Context context, PlayerProfile profile) {
        Summary s = new Summary();
        if (profile == null) return s;
        for (MatchRecord r : MatchHistoryStore.load(context)) {
            boolean one = belongs(r, true, profile), two = belongs(r, false, profile);
            if (!one && !two) continue;
            if (one) addAppearance(s, r, 1);
            if (two) addAppearance(s, r, 2);
        }
        return s;
    }

    /**
     * Computes `Summary` for every profile in `profiles` in a single pass over the match history.
     */
    public static Map<String, Summary> calculateAll(Context context, List<PlayerProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return Collections.emptyMap();
        Map<String, Summary> result = new HashMap<>(profiles.size() * 2);
        for (PlayerProfile p : profiles) {
            result.put(p.id, new Summary());
        }
        List<MatchRecord> records = MatchHistoryStore.load(context);
        for (MatchRecord r : records) {
            for (PlayerProfile p : profiles) {
                Summary s = result.get(p.id);
                if (s == null) continue;
                if (belongs(r, true, p)) addAppearance(s, r, 1);
                if (belongs(r, false, p)) addAppearance(s, r, 2);
            }
        }
        return result;
    }

    public static boolean belongs(MatchRecord r, boolean one, PlayerProfile p) {
        if (r == null || p == null) return false;
        String id = one ? r.playerOneProfileId : r.playerTwoProfileId;
        String name = one ? r.playerOneName : r.playerTwoName;
        return p.id.equals(id) || (id == null && p.name.equalsIgnoreCase(name));
    }

    private static void addAppearance(Summary s, MatchRecord r, int player) {
        int[] sets = r.playedSets();
        s.matches++;
        s.wonSets += sets[player - 1];
        s.totalSets += sets[0] + sets[1];
        if (r.statisticalWinner() == player) s.wins++;
        int a = 0, b = 0;
        for (int winner : r.pointWinners) {
            if (winner == 1) a++; else b++;
            if (winner == player) s.points++;
            boolean special67 = (a == 6 && b == 7) || (a == 7 && b == 6);
            boolean special69 = (a == 6 && b == 9) || (a == 9 && b == 6);
            if (winner == player && special67) s.score67++;
            if (winner == player && special69) s.score69++;
            if (Math.max(a, b) >= r.pointsToWinSet && Math.abs(a - b) >= 2) {
                a = 0;
                b = 0;
            }
        }
    }
}
