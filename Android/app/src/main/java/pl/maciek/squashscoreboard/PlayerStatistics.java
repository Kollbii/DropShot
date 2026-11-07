package pl.maciek.squashscoreboard;

import android.content.Context;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [PERF-120HZ] Shared, profile-aware statistics engine.
 * Uses O(P + M * N) hash-indexed single-pass aggregation over precomputed O(1) MatchRecord tables,
 * plus Markov chain steady-state serve dominance and Bayesian (Laplace / Wilson) win estimators.
 */
public final class PlayerStatistics {
    public static final class Summary {
        public int matches, wins, points, wonSets, totalSets, score67, score69;
        public int servesTotal, servesWon, receivesTotal, receivesWon;
        public int rightPicks, leftPicks, rightServesTotal, rightServesWon, leftServesTotal, leftServesWon;

        /** Bayesian Beta(1, 1) Laplace-smoothed match win probability: (W + 1) / (M + 2). */
        public double laplaceWinProbability() {
            return (wins + 1.0) / (matches + 2.0);
        }

        /** Lower bound of the 95% Wilson score interval (z = 1.96) for scalable leaderboard ranking. */
        public double wilsonLowerBound95() {
            if (matches <= 0) return 0.0;
            double n = matches;
            double p = (double) wins / n;
            double z = 1.96;
            double z2 = z * z;
            double denom = 1.0 + z2 / n;
            double center = p + z2 / (2.0 * n);
            double rad = z * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n);
            return Math.max(0.0, (center - rad) / denom);
        }

        /** Conditional probability of winning a rally when serving: P(win | serve). */
        public double serveHoldRate() {
            return servesTotal <= 0 ? 0.0 : (double) servesWon / servesTotal;
        }

        /** Conditional probability of winning a rally when receiving (handout): P(win | receive). */
        public double sideOutRate() {
            return receivesTotal <= 0 ? 0.0 : (double) receivesWon / receivesTotal;
        }

        /**
         * Stationary distribution of the 2-state squash serve Markov chain:
         * pi_serve = p_R / ((1 - p_S) + p_R).
         */
        public double markovServeShare() {
            if (servesTotal <= 0 && receivesTotal <= 0) return 0.5;
            double pS = servesTotal <= 0 ? 0.5 : (double) servesWon / servesTotal;
            double pR = receivesTotal <= 0 ? 0.5 : (double) receivesWon / receivesTotal;
            double denom = (1.0 - pS) + pR;
            return denom <= 1e-9 ? 0.5 : pR / denom;
        }

        /** Asymptotic point win probability implied by the 2-state serve Markov chain. */
        public double expectedRallyWinRate() {
            double pi = markovServeShare();
            return pi * serveHoldRate() + (1.0 - pi) * sideOutRate();
        }
    }

    private PlayerStatistics() { }

    public static Summary calculate(Context context, PlayerProfile profile) {
        if (profile == null) return new Summary();
        return calculateFromRecords(MatchHistoryStore.load(context), profile);
    }

    public static Summary calculateFromRecords(List<MatchRecord> records, PlayerProfile profile) {
        Summary s = new Summary();
        if (profile == null || records == null) return s;
        for (MatchRecord r : records) {
            boolean one = belongs(r, true, profile);
            boolean two = belongs(r, false, profile);
            if (!one && !two) continue;
            accumulateMatch(one ? s : null, two ? s : null, r);
        }
        return s;
    }

    /**
     * Computes `Summary` for every profile in `profiles` in O(P + M * N) time using hash lookup.
     */
    public static Map<String, Summary> calculateAll(Context context, List<PlayerProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return Collections.emptyMap();
        return calculateAllFromRecords(MatchHistoryStore.load(context), profiles);
    }

    public static Map<String, Summary> calculateAllFromRecords(List<MatchRecord> records, List<PlayerProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return Collections.emptyMap();
        int cap = Math.max(16, profiles.size() * 2);
        Map<String, Summary> result = new HashMap<>(cap);
        Map<String, Summary> byName = new HashMap<>(cap);
        for (PlayerProfile p : profiles) {
            if (p == null || p.id == null) continue;
            Summary s = new Summary();
            result.put(p.id, s);
            if (p.name != null) {
                byName.putIfAbsent(p.name.toLowerCase(Locale.ROOT), s);
            }
        }
        if (records == null) return result;
        for (MatchRecord r : records) {
            if (r == null) continue;
            Summary s1 = resolve(r.playerOneProfileId, r.playerOneName, result, byName);
            Summary s2 = resolve(r.playerTwoProfileId, r.playerTwoName, result, byName);
            if (s1 != null || s2 != null) {
                accumulateMatch(s1, s2, r);
            }
        }
        return result;
    }

    private static Summary resolve(
        String profileId,
        String playerName,
        Map<String, Summary> byId,
        Map<String, Summary> byName
    ) {
        if (profileId != null) {
            return byId.get(profileId);
        }
        if (playerName != null) {
            return byName.get(playerName.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    public static boolean belongs(MatchRecord r, boolean one, PlayerProfile p) {
        if (r == null || p == null) return false;
        String id = one ? r.playerOneProfileId : r.playerTwoProfileId;
        String name = one ? r.playerOneName : r.playerTwoName;
        return p.id.equals(id) || (id == null && p.name.equalsIgnoreCase(name));
    }

    private static void accumulateMatch(Summary s1, Summary s2, MatchRecord r) {
        int[] sets = r.playedSets();
        int totalPlayedSets = sets[0] + sets[1];
        int statWinner = r.statisticalWinner();

        if (s1 != null) {
            s1.matches++;
            s1.wonSets += sets[0];
            s1.totalSets += totalPlayedSets;
            if (statWinner == 1) s1.wins++;
        }
        if (s2 != null) {
            s2.matches++;
            s2.wonSets += sets[1];
            s2.totalSets += totalPlayedSets;
            if (statWinner == 2) s2.wins++;
        }

        int n = r.pointWinners.size();
        for (int i = 0; i < n; i++) {
            int winner = r.pointWinners.get(i);
            MatchRecord.PointScore ps = r.scoreAtPoint(i);
            int a = ps.pointsOne;
            int b = ps.pointsTwo;
            boolean special67 = (a == 6 && b == 7) || (a == 7 && b == 6);
            boolean special69 = (a == 6 && b == 9) || (a == 9 && b == 6);

            if (s1 != null && winner == 1) {
                s1.points++;
                if (special67) s1.score67++;
                if (special69) s1.score69++;
            }
            if (s2 != null && winner == 2) {
                s2.points++;
                if (special67) s2.score67++;
                if (special69) s2.score69++;
            }

            Summary srvSummary = ps.server == 2 ? s2 : s1;
            Summary rcvSummary = ps.server == 2 ? s1 : s2;
            boolean serverWon = (winner == ps.server);

            if (srvSummary != null) {
                srvSummary.servesTotal++;
                if (serverWon) srvSummary.servesWon++;
                if (ps.sidePicked) {
                    if (ps.serviceSide == 'L') srvSummary.leftPicks++;
                    else srvSummary.rightPicks++;
                }
                if (ps.serviceSide == 'L') {
                    srvSummary.leftServesTotal++;
                    if (serverWon) srvSummary.leftServesWon++;
                } else {
                    srvSummary.rightServesTotal++;
                    if (serverWon) srvSummary.rightServesWon++;
                }
            }
            if (rcvSummary != null) {
                rcvSummary.receivesTotal++;
                if (!serverWon) rcvSummary.receivesWon++;
            }
        }
    }
}
