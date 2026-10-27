package pl.maciek.squashscoreboard.lootbox;

/** Position on the reel for the three requested speed phases (total 6.5 seconds). */
final class ReelTiming {
    private ReelTiming() {}

    static float progressAt(long elapsedMs) {
        if (elapsedMs <= 0) return 0f;
        if (elapsedMs >= 6500) return 1f;
        if (elapsedMs <= 1500) {
            return .70f * elapsedMs / 1500f;
        }
        if (elapsedMs <= 4500) {
            float t = (elapsedMs - 1500) / 3000f;
            // Ease from fast to slow over three seconds.
            return .70f + .26f * (t + .75f * t * (1f - t));
        }
        // The final two seconds cross fewer than two squares at nearly constant speed.
        return .96f + .04f * (elapsedMs - 4500) / 2000f;
    }
}
