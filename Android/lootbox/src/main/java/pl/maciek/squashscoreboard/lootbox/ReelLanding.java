package pl.maciek.squashscoreboard.lootbox;

import java.util.Random;

/** Varies the stop position without allowing the marker to land on a gap. */
final class ReelLanding {
    static final float MAX_OFFSET_FRACTION = .28f;

    private ReelLanding() {}

    static float pickOffsetFraction(Random random) {
        return (random.nextFloat() * 2f - 1f) * MAX_OFFSET_FRACTION;
    }
}
