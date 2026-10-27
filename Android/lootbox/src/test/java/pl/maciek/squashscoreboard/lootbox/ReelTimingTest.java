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
        float previousStep = Float.MAX_VALUE;
        for (int ms = 100; ms <= 6500; ms += 100) {
            float current = ReelTiming.progressAt(ms);
            assertTrue("Reel must move forward", current >= previous);
            float step = current - previous;
            assertTrue("Reel must slow smoothly", step <= previousStep + .0001f);
            previousStep = step;
            previous = current;
        }
        assertEquals("Speed is continuous at 1.5 s",
            ReelTiming.progressAt(1500) - ReelTiming.progressAt(1490),
            ReelTiming.progressAt(1510) - ReelTiming.progressAt(1500), .0001f);
        assertEquals("Speed is continuous at 4.5 s",
            ReelTiming.progressAt(4500) - ReelTiming.progressAt(4490),
            ReelTiming.progressAt(4510) - ReelTiming.progressAt(4500), .0001f);
        assertTrue("Reel must ease to a stop",
            ReelTiming.progressAt(6500) - ReelTiming.progressAt(6400) < .001f);
        assertTrue("Final two seconds should travel less than two squares",
            (ReelTiming.progressAt(6500) - ReelTiming.progressAt(4500)) * 42 < 2);
    }
}
