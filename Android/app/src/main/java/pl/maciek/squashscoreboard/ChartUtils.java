package pl.maciek.squashscoreboard;

import java.util.Locale;

/**
 * Shared pure-Java time formatting, O(log N) point time lookup, and rally inspection label helpers
 * used across {@link ScoreChartView} and {@link SetProgressChartView}.
 */
final class ChartUtils {
    private ChartUtils() { }

    static boolean hasRecordedTimes(MatchRecord record) {
        return record != null
            && !record.pointTimesMs.isEmpty()
            && record.pointTimesMs.size() == record.pointWinners.size();
    }

    static long axisDurationMs(MatchRecord record) {
        if (record == null) return 1L;
        long last = 0L;
        for (long t : record.pointTimesMs) {
            if (t > last) last = t;
        }
        return Math.max(1L, Math.max(record.durationMs, last));
    }

    static long pointTimeMs(MatchRecord record, int index, long axisDurationMs) {
        if (record == null || record.pointWinners.isEmpty()) return 0L;
        if (hasRecordedTimes(record)) {
            return Math.min(axisDurationMs, Math.max(0L, record.pointTimesMs.get(index)));
        }
        return axisDurationMs * (index + 1L) / Math.max(1, record.pointWinners.size());
    }

    static String formatClock(long ms) {
        long seconds = Math.max(0L, ms) / 1000L;
        return seconds >= 3600L
            ? String.format(Locale.US, "%d:%02d:%02d", seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
            : String.format(Locale.US, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    static String formatPrecise(long ms) {
        long clamped = Math.max(0L, ms);
        return String.format(Locale.US, "%d:%02d.%03d", clamped / 60000L, (clamped / 1000L) % 60L, clamped % 1000L);
    }

    static String formatPointInspection(MatchRecord record, int pointIndex, long axisDurationMs) {
        if (record == null || pointIndex < 0 || pointIndex >= record.pointWinners.size()) {
            return "";
        }
        MatchRecord.PointScore score = record.scoreAtPoint(pointIndex);
        int winner = record.pointWinners.get(pointIndex);
        String srvName = score.server == 2 ? record.playerTwoName : record.playerOneName;
        String outcome = (winner == score.server) ? "HOLD" : "SIDE-OUT";
        long timeMs = pointTimeMs(record, pointIndex, axisDurationMs);
        return "G" + score.setNumber + " " + score.pointsOne + ":" + score.pointsTwo
            + " · SRV: " + srvName + " [" + score.serviceSide + "] ("
            + (score.sidePicked ? "PICK" : "ALT") + " · " + outcome + ") · "
            + formatPrecise(timeMs);
    }

    static int binarySearchNearestPoint(
        MatchRecord record,
        int fromInclusive,
        int toExclusive,
        long axisDurationMs,
        double wantedTimeMs
    ) {
        if (record == null || fromInclusive >= toExclusive) return -1;
        int lo = fromInclusive;
        int hi = toExclusive - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (pointTimeMs(record, mid, axisDurationMs) < wantedTimeMs) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        int bestIndex = lo;
        if (lo > fromInclusive) {
            double diffPrev = Math.abs(pointTimeMs(record, lo - 1, axisDurationMs) - wantedTimeMs);
            double diffCurr = Math.abs(pointTimeMs(record, lo, axisDurationMs) - wantedTimeMs);
            if (diffPrev <= diffCurr) bestIndex = lo - 1;
        }
        return bestIndex;
    }
}
