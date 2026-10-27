package pl.maciek.squashscoreboard.lootbox;

import static org.junit.Assert.assertTrue;

import java.util.Random;
import org.junit.Test;

public final class ReelLandingTest {
    @Test public void markerAlwaysStopsInsideTheWinningSquare() {
        Random random = new Random(2048);
        boolean sawLeft = false, sawRight = false;
        for (int i = 0; i < 10000; i++) {
            float offset = ReelLanding.pickOffsetFraction(random);
            assertTrue("Marker must stay away from square edges", Math.abs(offset) < .5f);
            sawLeft |= offset < 0;
            sawRight |= offset > 0;
        }
        assertTrue("Landing varies on either side of the center", sawLeft && sawRight);
    }
}
