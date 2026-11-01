package pl.maciek.squashscoreboard.watch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WatchProtocolTest {

    @Test
    public void validatesAllowedCommandsAndEncodesJsonLine() {
        assertTrue(WatchProtocol.isAllowedCommand("POINT_1"));
        assertTrue(WatchProtocol.isAllowedCommand("POINT_2"));
        assertTrue(WatchProtocol.isAllowedCommand("UNDO"));
        assertTrue(WatchProtocol.isAllowedCommand("RESET"));
        assertTrue(WatchProtocol.isAllowedCommand("TOGGLE_PAUSE"));
        assertTrue(WatchProtocol.isAllowedCommand("TOGGLE_SIDE"));
        assertFalse(WatchProtocol.isAllowedCommand("INVALID"));
        assertFalse(WatchProtocol.isAllowedCommand(null));

        assertEquals("{\"command\":\"POINT_1\"}\n", WatchProtocol.encodeCommandLine("POINT_1"));
        assertEquals("{\"command\":\"RESET\"}\n", WatchProtocol.encodeCommandLine("RESET"));
        assertEquals("{\"command\":\"TOGGLE_PAUSE\"}\n", WatchProtocol.encodeCommandLine("TOGGLE_PAUSE"));
        assertEquals("{\"command\":\"TOGGLE_SIDE\"}\n", WatchProtocol.encodeCommandLine("TOGGLE_SIDE"));
    }

    @Test
    public void parsesAcceptedJsonResponseSafely() {
        assertTrue(WatchProtocol.parseAcceptedResponse("{\"accepted\":true}"));
        assertFalse(WatchProtocol.parseAcceptedResponse("{\"accepted\":false}"));
        assertFalse(WatchProtocol.parseAcceptedResponse("{}"));
        assertFalse(WatchProtocol.parseAcceptedResponse("invalid"));
        assertFalse(WatchProtocol.parseAcceptedResponse(null));

        assertTrue(WatchProtocol.parseAckBall("{\"type\":\"ACK\",\"accepted\":true,\"ball\":true}"));
        assertFalse(WatchProtocol.parseAckBall("{\"type\":\"ACK\",\"accepted\":true,\"ball\":false}"));
        assertEquals("ACK", WatchProtocol.parseMessageType("{\"type\":\"ACK\",\"accepted\":true}"));
        assertEquals("ACK", WatchProtocol.parseMessageType("{\"accepted\":true}"));
    }

    @Test
    public void parsesWatchStateJsonSafely() {
        String json = "{\"type\":\"STATE\",\"n1\":\"KAC\",\"n2\":\"ŁUK\",\"p1\":9,\"p2\":8,\"s1\":1,\"s2\":0,"
            + "\"targetSets\":2,\"srv\":2,\"side\":\"L\",\"c1\":-13665034,\"r1\":false,\"c2\":-44967,\"r2\":true,"
            + "\"act\":\"STOP\",\"ball\":true,\"fin\":false,\"paused\":false}";

        WatchProtocol.WatchState state = WatchProtocol.parseWatchState(json);
        assertNotNull(state);
        assertEquals("KAC", state.playerOneInitials);
        assertEquals("ŁUK", state.playerTwoInitials);
        assertEquals(9, state.playerOnePoints);
        assertEquals(8, state.playerTwoPoints);
        assertEquals(1, state.playerOneSets);
        assertEquals(0, state.playerTwoSets);
        assertEquals(2, state.targetSets);
        assertEquals(2, state.activeServer);
        assertEquals("L", state.serviceSide);
        assertEquals(-13665034, state.playerOneColor);
        assertFalse(state.playerOneRainbow);
        assertEquals(-44967, state.playerTwoColor);
        assertTrue(state.playerTwoRainbow);
        assertEquals("STOP", state.actionLabel);
        assertTrue(state.gameOrMatchBall);
        assertFalse(state.matchFinished);
        assertFalse(state.paused);

        assertNull(WatchProtocol.parseWatchState("{\"type\":\"OTHER\"}"));
        assertNull(WatchProtocol.parseWatchState("invalid"));
        assertNull(WatchProtocol.parseWatchState(null));
    }

    @Test
    public void parsesMatchEndedJsonSafely() {
        String json = "{\"type\":\"MATCH_ENDED\",\"winner\":\"Łukasz\",\"score\":\"2 - 1\"}";
        WatchProtocol.MatchEndedEvent event = WatchProtocol.parseMatchEnded(json);
        assertNotNull(event);
        assertEquals("Łukasz", event.winnerName);
        assertEquals("2 - 1", event.finalScore);

        assertNull(WatchProtocol.parseMatchEnded("{\"type\":\"STATE\"}"));
        assertNull(WatchProtocol.parseMatchEnded("invalid"));
        assertNull(WatchProtocol.parseMatchEnded(null));
    }

    @Test
    public void formatsSetsIndicatorCorrectly() {
        assertEquals("●○", WatchProtocol.formatSetsIndicator(1, 2));
        assertEquals("●●○", WatchProtocol.formatSetsIndicator(2, 3));
        assertEquals("○○○", WatchProtocol.formatSetsIndicator(0, 3));
        assertEquals("●●", WatchProtocol.formatSetsIndicator(2, 2));
    }

    @Test
    public void calculatesExponentialBackoffDelays() {
        assertEquals(3000L, BluetoothWatchClient.getBackoffDelayMs(0));
        assertEquals(5000L, BluetoothWatchClient.getBackoffDelayMs(1));
        assertEquals(10000L, BluetoothWatchClient.getBackoffDelayMs(2));
        assertEquals(10000L, BluetoothWatchClient.getBackoffDelayMs(3));
        assertEquals(10000L, BluetoothWatchClient.getBackoffDelayMs(100));
    }
}
