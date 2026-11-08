package pl.maciek.squashscoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * [MASVS-CODE] [ASVS V4.2 / V5.1]
 * Immutable, schema-validated match record entity with O(1) precomputed point-state lookup.
 * - Bounds-checks all rule parameters (`pointsToWinSet >= 1`, `setsToWinMatch >= 1`) to prevent
 *   division-by-zero or infinite loops when rendering charts from untrusted JSON / Intent extras.
 * - Caps point history arrays (`MAX_RECORDED_POINTS = 5000`) to prevent OOM/DoS attacks.
 * - Precomputes cumulative game/point scores and per-rally server/box metadata in a single O(N) pass.
 */
public final class MatchRecord {
    public final long id, finishedAt, durationMs;
    public final String playerOneName, playerTwoName, playerOneProfileId, playerTwoProfileId;
    public final int playerOneSets, playerTwoSets, playerOnePoints, playerTwoPoints, pointsToWinSet, setsToWinMatch, playerOneColor, playerTwoColor;
    public final int initialServer;
    public final boolean finished, paused;
    public final ArrayList<Integer> pointWinners;
    /** Normalized 'R'/'L' service box string for every point in pointWinners. */
    public final String pointSides;
    /** Elapsed active match time at every scored point; old records can have an empty list. */
    public final ArrayList<Long> pointTimesMs;
    public final ArrayList<Long> setDurationsMs;

    // Precomputed O(1) lookup tables
    private final int computedSetsOne;
    private final int computedSetsTwo;
    private final short[] prefixPointsOne;
    private final short[] prefixPointsTwo;
    private final byte[] prefixSetsOne;
    private final byte[] prefixSetsTwo;
    private final byte[] prefixSetNumber;
    // Bit 0: server (0 = P1, 1 = P2), Bit 1: side (0 = 'R', 1 = 'L'), Bit 2: sidePicked (1 = picked, 0 = alternating)
    private final byte[] prefixServeMeta;
    private volatile Analytics cachedAnalytics;

    public MatchRecord(
        long id,
        String p1,
        String p2,
        String p1Id,
        String p2Id,
        int s1,
        int s2,
        int p1Points,
        int p2Points,
        int points,
        int sets,
        boolean finished,
        long timestamp,
        List<Integer> winners,
        long duration,
        List<Long> setDurations,
        List<Long> pointTimes,
        boolean paused,
        int color1,
        int color2,
        int initialServer,
        String rawSides
    ) {
        this.id = id > 0 ? id : InputValidator.generateSecureMatchId();
        this.playerOneName = InputValidator.sanitizePlayerName(p1, "PLAYER 1");
        this.playerTwoName = InputValidator.sanitizePlayerName(p2, "PLAYER 2");
        this.playerOneProfileId = InputValidator.sanitizeProfileIdOrNull(p1Id);
        this.playerTwoProfileId = InputValidator.sanitizeProfileIdOrNull(p2Id);
        this.pointsToWinSet = InputValidator.clampPointsPerSet(points);
        this.setsToWinMatch = InputValidator.clampSetsPerMatch(sets);
        this.playerOneSets = InputValidator.clampNonNegativeScore(s1, 100);
        this.playerTwoSets = InputValidator.clampNonNegativeScore(s2, 100);
        this.playerOnePoints = InputValidator.clampNonNegativeScore(p1Points, 500);
        this.playerTwoPoints = InputValidator.clampNonNegativeScore(p2Points, 500);
        this.finished = finished;
        this.finishedAt = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.durationMs = InputValidator.clampDurationMs(duration);
        this.paused = paused;
        this.playerOneColor = color1;
        this.playerTwoColor = color2;
        this.initialServer = (initialServer == 2) ? 2 : 1;

        this.pointWinners = new ArrayList<>();
        if (winners != null) {
            int limit = Math.min(winners.size(), InputValidator.MAX_RECORDED_POINTS);
            for (int i = 0; i < limit; i++) {
                Integer w = winners.get(i);
                if (w != null && (w == 1 || w == 2)) {
                    this.pointWinners.add(w);
                }
            }
        }

        this.setDurationsMs = new ArrayList<>();
        if (setDurations != null) {
            int limit = Math.min(setDurations.size(), InputValidator.MAX_SETS_PER_MATCH * 4);
            for (int i = 0; i < limit; i++) {
                Long d = setDurations.get(i);
                if (d != null) {
                    this.setDurationsMs.add(InputValidator.clampDurationMs(d));
                }
            }
        }

        this.pointTimesMs = new ArrayList<>();
        if (pointTimes != null) {
            int limit = Math.min(pointTimes.size(), this.pointWinners.size());
            for (int i = 0; i < limit; i++) {
                Long t = pointTimes.get(i);
                if (t != null) {
                    this.pointTimesMs.add(InputValidator.clampDurationMs(t));
                }
            }
        }

        int n = this.pointWinners.size();
        this.prefixPointsOne = new short[n];
        this.prefixPointsTwo = new short[n];
        this.prefixSetsOne = new byte[n];
        this.prefixSetsTwo = new byte[n];
        this.prefixSetNumber = new byte[n];
        this.prefixServeMeta = new byte[n];

        StringBuilder normalizedSides = new StringBuilder(n);
        int curS1 = 0, curS2 = 0, curP1 = 0, curP2 = 0, curSetNum = 1;
        int prevSrv = this.initialServer;
        boolean completedSet = false;
        boolean runningRightBox = true;

        for (int i = 0; i < n; i++) {
            int srv = (i == 0) ? this.initialServer : this.pointWinners.get(i - 1);
            boolean picked = (i == 0) || completedSet || (srv != prevSrv);
            if (completedSet) {
                curP1 = 0;
                curP2 = 0;
                curSetNum++;
                completedSet = false;
            }
            char sideChar = 0;
            if (rawSides != null && i < rawSides.length()) {
                char candidate = rawSides.charAt(i);
                if (candidate == 'R' || candidate == 'L') {
                    sideChar = candidate;
                }
            }
            if (sideChar == 'R' || sideChar == 'L') {
                runningRightBox = (sideChar == 'R');
            } else {
                if (picked) {
                    runningRightBox = true;
                } else {
                    runningRightBox = !runningRightBox;
                }
                sideChar = runningRightBox ? 'R' : 'L';
            }
            normalizedSides.append(sideChar);

            int winner = this.pointWinners.get(i);
            if (winner == 1) curP1++; else curP2++;
            if (Math.max(curP1, curP2) >= this.pointsToWinSet && Math.abs(curP1 - curP2) >= 2) {
                if (curP1 > curP2) curS1++; else curS2++;
                completedSet = true;
            }

            this.prefixPointsOne[i] = (short) curP1;
            this.prefixPointsTwo[i] = (short) curP2;
            this.prefixSetsOne[i] = (byte) Math.min(127, curS1);
            this.prefixSetsTwo[i] = (byte) Math.min(127, curS2);
            this.prefixSetNumber[i] = (byte) Math.min(127, curSetNum);
            int meta = (srv == 2 ? 1 : 0)
                | (sideChar == 'L' ? 2 : 0)
                | (picked ? 4 : 0);
            this.prefixServeMeta[i] = (byte) meta;
            prevSrv = srv;
        }

        this.pointSides = normalizedSides.toString();
        if (n == 0) {
            this.computedSetsOne = this.playerOneSets;
            this.computedSetsTwo = this.playerTwoSets;
        } else {
            this.computedSetsOne = curS1;
            this.computedSetsTwo = curS2;
        }
    }

    public MatchRecord(long id, String p1, String p2, String p1Id, String p2Id, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners, long duration, List<Long> setDurations, List<Long> pointTimes, boolean paused, int color1, int color2) {
        this(id, p1, p2, p1Id, p2Id, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, duration, setDurations, pointTimes, paused, color1, color2, 1, null);
    }

    public MatchRecord(long id, String p1, String p2, String p1Id, String p2Id, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners, long duration, List<Long> setDurations, List<Long> pointTimes, boolean paused) {
        this(id, p1, p2, p1Id, p2Id, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, duration, setDurations, pointTimes, paused, AppPreferences.BLUE, AppPreferences.RED, 1, null);
    }

    public MatchRecord(long id, String p1, String p2, String p1Id, String p2Id, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners, long duration, List<Long> setDurations, boolean paused) {
        this(id, p1, p2, p1Id, p2Id, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, duration, setDurations, Collections.<Long>emptyList(), paused);
    }

    /** Kept for source compatibility with old callers. */
    public MatchRecord(long id, String p1, String p2, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners) {
        this(id, p1, p2, null, null, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, 0, Collections.<Long>emptyList(), false);
    }

    /** O(1) lookup of completed sets so older early-ended records do not count invented sets. */
    public int[] playedSets() {
        return new int[]{computedSetsOne, computedSetsTwo};
    }

    public int statisticalWinner() {
        if (computedSetsOne != computedSetsTwo) return computedSetsOne > computedSetsTwo ? 1 : 2;
        if (playerOnePoints != playerTwoPoints) return playerOnePoints > playerTwoPoints ? 1 : 2;
        return 0;
    }

    public static final class PointScore {
        public final int setNumber, setsOne, setsTwo, pointsOne, pointsTwo;
        public final int server;
        public final char serviceSide;
        public final boolean sidePicked;

        private PointScore(int number, int s1, int s2, int p1, int p2, int srv, char side, boolean picked) {
            setNumber = number;
            setsOne = s1;
            setsTwo = s2;
            pointsOne = p1;
            pointsTwo = p2;
            server = srv;
            serviceSide = side;
            sidePicked = picked;
        }
    }

    /** O(1) lookup of the score and serve metadata at the indexed point. */
    public PointScore scoreAtPoint(int index) {
        int n = pointWinners.size();
        if (n == 0 || index < 0) {
            return new PointScore(1, 0, 0, 0, 0, initialServer, 'R', true);
        }
        int clamped = Math.min(index, n - 1);
        int meta = prefixServeMeta[clamped] & 0xFF;
        int srv = (meta & 1) != 0 ? 2 : 1;
        char side = (meta & 2) != 0 ? 'L' : 'R';
        boolean picked = (meta & 4) != 0;
        return new PointScore(
            prefixSetNumber[clamped] & 0xFF,
            prefixSetsOne[clamped] & 0xFF,
            prefixSetsTwo[clamped] & 0xFF,
            prefixPointsOne[clamped],
            prefixPointsTwo[clamped],
            srv,
            side,
            picked
        );
    }

    /** O(1) lookup of who served at the indexed point (1 or 2). */
    public int serverAtPoint(int index) {
        if (index <= 0 || index >= prefixServeMeta.length) return initialServer;
        return (prefixServeMeta[index] & 1) != 0 ? 2 : 1;
    }

    /** O(1) lookup of which box ('R' or 'L') was served from at the indexed point. */
    public char sideAtPoint(int index) {
        if (index < 0 || index >= prefixServeMeta.length) return 'R';
        return (prefixServeMeta[index] & 2) != 0 ? 'L' : 'R';
    }

    /** O(1) lookup of whether the server could freely pick the side at the indexed point. */
    public boolean isSidePickedAtPoint(int index) {
        if (index < 0 || index >= prefixServeMeta.length) return true;
        return (prefixServeMeta[index] & 4) != 0;
    }

    /**
     * [PERF-120HZ] Single-pass O(N) per-match analytics and game-segment breakdown.
     * Shared across {@link MatchFinishedView}, {@link MatchStatsView}, {@link SetProgressChartView},
     * {@link SetsChartView}, and {@link HistoryActivity} to eliminate duplicated loops.
     */
    public static final class Analytics {
        public final int[] sets;
        public final int[] points = new int[2];
        public final int[] longestStreak = new int[2];
        public final int[] servesTotal = new int[2];
        public final int[] servesWon = new int[2];
        public final int[] receivesTotal = new int[2];
        public final int[] receivesWon = new int[2];
        public final int[] rightPicks = new int[2];
        public final int[] leftPicks = new int[2];
        public final int[] rightServesTotal = new int[2];
        public final int[] rightServesWon = new int[2];
        public final int[] leftServesTotal = new int[2];
        public final int[] leftServesWon = new int[2];
        public final int[] pickedServesTotal = new int[2];
        public final int[] pickedServesWon = new int[2];
        public final int[] altServesTotal = new int[2];
        public final int[] altServesWon = new int[2];
        public final int[] receivesVsRightTotal = new int[2];
        public final int[] receivesVsRightWon = new int[2];
        public final int[] receivesVsLeftTotal = new int[2];
        public final int[] receivesVsLeftWon = new int[2];
        /** Each entry is `{startPointIndex, endPointIndexExclusive, pointsOne, pointsTwo}`. */
        public final List<int[]> gameSegments;

        private Analytics(MatchRecord r) {
            this.sets = r.playedSets();
            ArrayList<int[]> segments = new ArrayList<>();
            int n = r.pointWinners.size();
            int prevWinner = 0, curStreak = 0;
            int segStart = 0, gOne = 0, gTwo = 0;

            for (int i = 0; i < n; i++) {
                int winner = r.pointWinners.get(i);
                if (winner != 1 && winner != 2) continue;
                int wIdx = winner - 1;
                points[wIdx]++;
                curStreak = (winner == prevWinner) ? curStreak + 1 : 1;
                if (curStreak > longestStreak[wIdx]) longestStreak[wIdx] = curStreak;
                prevWinner = winner;

                int srv = r.serverAtPoint(i);
                int sIdx = (srv == 2) ? 1 : 0;
                int rIdx = 1 - sIdx;
                char side = r.sideAtPoint(i);
                boolean picked = r.isSidePickedAtPoint(i);
                boolean serverScored = (winner == srv);

                servesTotal[sIdx]++;
                receivesTotal[rIdx]++;
                if (serverScored) {
                    servesWon[sIdx]++;
                } else {
                    receivesWon[rIdx]++;
                }

                if (picked) {
                    if (side == 'L') leftPicks[sIdx]++; else rightPicks[sIdx]++;
                    pickedServesTotal[sIdx]++;
                    if (serverScored) pickedServesWon[sIdx]++;
                } else {
                    altServesTotal[sIdx]++;
                    if (serverScored) altServesWon[sIdx]++;
                }

                if (side == 'L') {
                    leftServesTotal[sIdx]++;
                    if (serverScored) leftServesWon[sIdx]++;
                    receivesVsLeftTotal[rIdx]++;
                    if (!serverScored) receivesVsLeftWon[rIdx]++;
                } else {
                    rightServesTotal[sIdx]++;
                    if (serverScored) rightServesWon[sIdx]++;
                    receivesVsRightTotal[rIdx]++;
                    if (!serverScored) receivesVsRightWon[rIdx]++;
                }

                if (winner == 1) gOne++; else gTwo++;
                if (Math.max(gOne, gTwo) >= r.pointsToWinSet && Math.abs(gOne - gTwo) >= 2) {
                    segments.add(new int[]{segStart, i + 1, gOne, gTwo});
                    segStart = i + 1;
                    gOne = 0;
                    gTwo = 0;
                }
            }
            if (segStart < n) {
                segments.add(new int[]{segStart, n, gOne, gTwo});
            } else if (n == 0 && (r.playerOnePoints > 0 || r.playerTwoPoints > 0)) {
                segments.add(new int[]{0, 0, r.playerOnePoints, r.playerTwoPoints});
                points[0] = r.playerOnePoints;
                points[1] = r.playerTwoPoints;
            }
            this.gameSegments = Collections.unmodifiableList(segments);
        }

        public int totalPoints() {
            return points[0] + points[1];
        }

        public double serveHoldRate(int playerIndex) {
            return servesTotal[playerIndex] <= 0 ? 0.0 : (double) servesWon[playerIndex] / servesTotal[playerIndex];
        }

        public double sideOutRate(int playerIndex) {
            return receivesTotal[playerIndex] <= 0 ? 0.0 : (double) receivesWon[playerIndex] / receivesTotal[playerIndex];
        }

        public double rightServeHoldRate(int playerIndex) {
            return rightServesTotal[playerIndex] <= 0 ? 0.0 : (double) rightServesWon[playerIndex] / rightServesTotal[playerIndex];
        }

        public double leftServeHoldRate(int playerIndex) {
            return leftServesTotal[playerIndex] <= 0 ? 0.0 : (double) leftServesWon[playerIndex] / leftServesTotal[playerIndex];
        }

        public double markovServeShare(int playerIndex) {
            if (servesTotal[playerIndex] <= 0 && receivesTotal[playerIndex] <= 0) return 0.5;
            double pS = servesTotal[playerIndex] <= 0 ? 0.5 : (double) servesWon[playerIndex] / servesTotal[playerIndex];
            double pR = receivesTotal[playerIndex] <= 0 ? 0.5 : (double) receivesWon[playerIndex] / receivesTotal[playerIndex];
            double denom = (1.0 - pS) + pR;
            return denom <= 1e-9 ? 0.5 : pR / denom;
        }
    }

    /** Lazily computes and caches the single-pass {@link Analytics} for this match record. */
    public Analytics analytics() {
        Analytics a = cachedAnalytics;
        if (a == null) {
            a = new Analytics(this);
            cachedAnalytics = a;
        }
        return a;
    }

    /** Formats per-game scores as a compact string such as `"11-8, 9-11, 11-7"`. */
    public String formattedGameScores() {
        List<int[]> games = analytics().gameSegments;
        if (games.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(games.size() * 7);
        for (int i = 0; i < games.size(); i++) {
            if (i > 0) sb.append(", ");
            int[] g = games.get(i);
            sb.append(g[2]).append('-').append(g[3]);
        }
        return sb.toString();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("p1", playerOneName);
        j.put("p2", playerTwoName);
        if (playerOneProfileId != null) j.put("p1profile", playerOneProfileId);
        if (playerTwoProfileId != null) j.put("p2profile", playerTwoProfileId);
        j.put("color1", playerOneColor);
        j.put("color2", playerTwoColor);
        j.put("s1", playerOneSets);
        j.put("s2", playerTwoSets);
        j.put("time", finishedAt);
        j.put("p1points", playerOnePoints);
        j.put("p2points", playerTwoPoints);
        j.put("pointsPerSet", pointsToWinSet);
        j.put("setsPerMatch", setsToWinMatch);
        j.put("finished", finished);
        j.put("duration", durationMs);
        j.put("paused", paused);
        j.put("initSrv", initialServer);
        if (!pointSides.isEmpty()) {
            j.put("sides", pointSides);
        }
        JSONArray a = new JSONArray();
        for (int p : pointWinners) a.put(p);
        j.put("points", a);
        JSONArray d = new JSONArray();
        for (long v : setDurationsMs) d.put(v);
        j.put("setDurations", d);
        JSONArray pt = new JSONArray();
        for (long v : pointTimesMs) pt.put(v);
        j.put("pointTimes", pt);
        return j;
    }

    public static MatchRecord fromJson(JSONObject j) throws JSONException {
        if (j == null) throw new JSONException("Null MatchRecord JSON");
        ArrayList<Integer> w = new ArrayList<>();
        JSONArray a = j.optJSONArray("points");
        if (a != null) {
            int limit = Math.min(a.length(), InputValidator.MAX_RECORDED_POINTS);
            for (int i = 0; i < limit; i++) {
                int val = a.optInt(i, 0);
                if (val == 1 || val == 2) w.add(val);
            }
        }
        ArrayList<Long> d = new ArrayList<>();
        JSONArray sd = j.optJSONArray("setDurations");
        if (sd != null) {
            int limit = Math.min(sd.length(), InputValidator.MAX_SETS_PER_MATCH * 4);
            for (int i = 0; i < limit; i++) d.add(InputValidator.clampDurationMs(sd.optLong(i)));
        }
        ArrayList<Long> pt = new ArrayList<>();
        JSONArray times = j.optJSONArray("pointTimes");
        if (times != null) {
            int limit = Math.min(times.length(), w.size());
            for (int i = 0; i < limit; i++) pt.add(InputValidator.clampDurationMs(times.optLong(i)));
        }
        long time = j.optLong("time", System.currentTimeMillis());
        long rawId = j.optLong("id", 0L);
        return new MatchRecord(
            rawId > 0 ? rawId : InputValidator.generateSecureMatchId(),
            j.optString("p1", "PLAYER 1"),
            j.optString("p2", "PLAYER 2"),
            j.optString("p1profile", null),
            j.optString("p2profile", null),
            j.optInt("s1", 0),
            j.optInt("s2", 0),
            j.optInt("p1points", 0),
            j.optInt("p2points", 0),
            j.optInt("pointsPerSet", 11),
            j.optInt("setsPerMatch", 3),
            j.optBoolean("finished", false),
            time,
            w,
            j.optLong("duration", 0),
            d,
            pt,
            j.optBoolean("paused", false),
            j.optInt("color1", AppPreferences.BLUE),
            j.optInt("color2", AppPreferences.RED),
            j.optInt("initSrv", 1),
            j.optString("sides", null)
        );
    }
}
