package pl.maciek.squashscoreboard.lootbox;

/** Smooth, continuously decelerating reel motion across three timed phases. */
final class ReelTiming {
    private ReelTiming() {}

    static float progressAt(long elapsedMs) {
        if (elapsedMs <= 0) return 0f;
        if (elapsedMs >= 6500) return 1f;
        if (elapsedMs <= 1500) {
            return hermite(0f, .70f, .70f, .18f, 1.5f, elapsedMs / 1500f);
        }
        if (elapsedMs <= 4500) {
            return hermite(.70f, .96f, .18f, .04f, 3f, (elapsedMs - 1500) / 3000f);
        }
        // The last two seconds cover fewer than two squares and ease to zero speed.
        return hermite(.96f, 1f, .04f, 0f, 2f, (elapsedMs - 4500) / 2000f);
    }

    private static float hermite(float start, float end, float startSpeed,
                                 float endSpeed, float seconds, float t) {
        float t2 = t * t, t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * start
            + (t3 - 2 * t2 + t) * startSpeed * seconds
            + (-2 * t3 + 3 * t2) * end
            + (t3 - t2) * endSpeed * seconds;
    }
}
