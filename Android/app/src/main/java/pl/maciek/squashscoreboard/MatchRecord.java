package pl.maciek.squashscoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * [MASVS-CODE] [ASVS V4.2 / V5.1]
 * Immutable, schema-validated match record entity.
 * - Bounds-checks all rule parameters (`pointsToWinSet >= 1`, `setsToWinMatch >= 1`) to prevent
 *   division-by-zero or infinite loops when rendering charts from untrusted JSON / Intent extras.
 * - Caps point history arrays (`MAX_RECORDED_POINTS = 5000`) to prevent OOM/DoS attacks.
 */
public final class MatchRecord {
    public final long id, finishedAt, durationMs;
    public final String playerOneName, playerTwoName, playerOneProfileId, playerTwoProfileId;
    public final int playerOneSets, playerTwoSets, playerOnePoints, playerTwoPoints, pointsToWinSet, setsToWinMatch, playerOneColor, playerTwoColor;
    public final boolean finished, paused;
    public final ArrayList<Integer> pointWinners;
    /** Elapsed active match time at every scored point; old records can have an empty list. */
    public final ArrayList<Long> pointTimesMs;
    public final ArrayList<Long> setDurationsMs;

    public MatchRecord(long id, String p1, String p2, String p1Id, String p2Id, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners, long duration, List<Long> setDurations, List<Long> pointTimes, boolean paused, int color1, int color2) {
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
    }

    public MatchRecord(long id, String p1, String p2, String p1Id, String p2Id, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners, long duration, List<Long> setDurations, List<Long> pointTimes, boolean paused) {
        this(id, p1, p2, p1Id, p2Id, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, duration, setDurations, pointTimes, paused, AppPreferences.BLUE, AppPreferences.RED);
    }

    public MatchRecord(long id, String p1, String p2, String p1Id, String p2Id, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners, long duration, List<Long> setDurations, boolean paused) {
        this(id, p1, p2, p1Id, p2Id, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, duration, setDurations, Collections.<Long>emptyList(), paused);
    }

    /** Kept for source compatibility with old callers. */
    public MatchRecord(long id, String p1, String p2, int s1, int s2, int p1Points, int p2Points, int points, int sets, boolean finished, long timestamp, List<Integer> winners) {
        this(id, p1, p2, null, null, s1, s2, p1Points, p2Points, points, sets, finished, timestamp, winners, 0, Collections.<Long>emptyList(), false);
    }

    /** Reconstruct completed sets so older early-ended records do not count invented sets. */
    public int[] playedSets() {
        if (pointWinners.isEmpty()) return new int[]{playerOneSets, playerTwoSets};
        int[] sets = {0, 0};
        int a = 0, b = 0;
        for (int winner : pointWinners) {
            if (winner == 1) a++;
            else if (winner == 2) b++;
            if (Math.max(a, b) >= pointsToWinSet && Math.abs(a - b) >= 2) {
                sets[a > b ? 0 : 1]++;
                a = 0;
                b = 0;
            }
        }
        return sets;
    }

    public int statisticalWinner() {
        int[] sets = playedSets();
        if (sets[0] != sets[1]) return sets[0] > sets[1] ? 1 : 2;
        if (playerOnePoints != playerTwoPoints) return playerOnePoints > playerTwoPoints ? 1 : 2;
        return 0;
    }

    public static final class PointScore {
        public final int setNumber, setsOne, setsTwo, pointsOne, pointsTwo;
        private PointScore(int number, int s1, int s2, int p1, int p2) {
            setNumber = number; setsOne = s1; setsTwo = s2; pointsOne = p1; pointsTwo = p2;
        }
    }

    /** Score immediately after the indexed point, including a just-completed set. */
    public PointScore scoreAtPoint(int index) {
        int s1 = 0, s2 = 0, p1 = 0, p2 = 0, setNumber = 1;
        boolean completed = false;
        for (int i = 0; i <= index && i < pointWinners.size(); i++) {
            if (completed) { p1 = 0; p2 = 0; setNumber++; completed = false; }
            if (pointWinners.get(i) == 1) p1++; else p2++;
            if (Math.max(p1, p2) >= pointsToWinSet && Math.abs(p1 - p2) >= 2) {
                if (p1 > p2) s1++; else s2++;
                completed = true;
            }
        }
        return new PointScore(setNumber, s1, s2, p1, p2);
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
            j.optInt("color2", AppPreferences.RED)
        );
    }
}
