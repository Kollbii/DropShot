package pl.maciek.squashscoreboard.watch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WatchProtocolTest {

    @Test
    public void validatesAllowedCommandsAndEncodesJsonLine() {
        assertTrue(WatchProtocol.isAllowedCommand("POINT_1"));
        assertTrue(WatchProtocol.isAllowedCommand("POINT_2"));
        assertTrue(WatchProtocol.isAllowedCommand("UNDO"));
        assertTrue(WatchProtocol.isAllowedCommand("RESET"));
        assertFalse(WatchProtocol.isAllowedCommand("INVALID"));
        assertFalse(WatchProtocol.isAllowedCommand(null));

        assertEquals("{\"command\":\"POINT_1\"}\n", WatchProtocol.encodeCommandLine("POINT_1"));
        assertEquals("{\"command\":\"RESET\"}\n", WatchProtocol.encodeCommandLine("RESET"));
    }

    @Test
    public void parsesAcceptedJsonResponseSafely() {
        assertTrue(WatchProtocol.parseAcceptedResponse("{\"accepted\":true}"));
        assertFalse(WatchProtocol.parseAcceptedResponse("{\"accepted\":false}"));
        assertFalse(WatchProtocol.parseAcceptedResponse("{}"));
        assertFalse(WatchProtocol.parseAcceptedResponse("invalid"));
        assertFalse(WatchProtocol.parseAcceptedResponse(null));
    }
}

