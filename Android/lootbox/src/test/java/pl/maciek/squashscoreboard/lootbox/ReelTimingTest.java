package pl.maciek.squashscoreboard.lootbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ReelTimingTest {
    @Test public void phaseBoundariesAndFinalSlowSection() {
        assertEquals(0f, ReelTiming.progressAt(0), .0001f);
        assertEquals(.70f, ReelTiming.progressAt(1500), .0001f);
        assertEquals(.96f, ReelTiming.progressAt(4500), .0001f);
        assertEquals(1f, ReelTiming.progressAt(6500), .0001f);
        assertEquals(1f, ReelTiming.progressAt(9000), .0001f);
        float previous = 0f;
        for (int ms = 100; ms <= 6500; ms += 100) {
            float current = ReelTiming.progressAt(ms);
            assertTrue("Reel must move forward", current >= previous);
            previous = current;
        }
        assertTrue("Final two seconds should travel less than two squares",
            (ReelTiming.progressAt(6500) - ReelTiming.progressAt(4500)) * 42 < 2);
    }
}
