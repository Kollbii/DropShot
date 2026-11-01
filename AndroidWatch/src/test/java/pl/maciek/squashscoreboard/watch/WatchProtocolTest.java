package pl.maciek.squashscoreboard.watch;

import static org.junit.Assert.assertArrayEquals;
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

    @Test
    public void providesGradientStopsForRainbowAndPurpleRewards() {
        assertArrayEquals(WatchProtocol.RAINBOW_STOPS, WatchProtocol.getGradientStops(0, true));
        assertArrayEquals(WatchProtocol.RAINBOW_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_RAINBOW, false));
        assertArrayEquals(WatchProtocol.SUNSET_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_SUNSET, false));
        assertArrayEquals(WatchProtocol.OCEAN_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_OCEAN, false));
        assertArrayEquals(WatchProtocol.AURORA_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_AURORA, false));
        assertArrayEquals(WatchProtocol.ORCHID_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_ORCHID, false));
        assertArrayEquals(WatchProtocol.EMBER_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_EMBER, false));

        assertArrayEquals(WatchProtocol.ORCHID_STOPS, WatchProtocol.getGradientStops(WatchProtocol.KEY_ORCHID_DARKENED, false));

        assertNull(WatchProtocol.getGradientStops(0xFF3184FF, false));
        assertNull(WatchProtocol.getGradientStops(0xFFF45C5C, false));
    }

    @Test
    public void detectsEdgeSwipeExitAccurately() {
        assertTrue(WatchProtocol.isEdgeSwipeExit(20f, 95f, 50f, 55f, 36f, 70f));
        assertFalse(WatchProtocol.isEdgeSwipeExit(40f, 120f, 50f, 55f, 36f, 70f));
        assertFalse(WatchProtocol.isEdgeSwipeExit(20f, 70f, 50f, 55f, 36f, 70f));
        assertFalse(WatchProtocol.isEdgeSwipeExit(20f, 95f, 50f, 110f, 36f, 70f));
    }

    @Test
    public void detectsTouchMovementExceededAccurately() {
        assertFalse(WatchProtocol.isTouchMovementExceeded(10f, 12f, 20f, 22f, 8f));
        assertTrue(WatchProtocol.isTouchMovementExceeded(10f, 20f, 20f, 20f, 8f));
    }

    @Test
    public void detectsReactionScoresAccurately() {
        assertTrue(WatchProtocol.isReaction67Score(6, 7));
        assertTrue(WatchProtocol.isReaction67Score(7, 6));
        assertFalse(WatchProtocol.isReaction67Score(6, 6));
        assertFalse(WatchProtocol.isReaction67Score(7, 7));
        assertFalse(WatchProtocol.isReaction67Score(6, 8));

        assertTrue(WatchProtocol.isReaction69Score(6, 9));
        assertTrue(WatchProtocol.isReaction69Score(9, 6));
        assertFalse(WatchProtocol.isReaction69Score(6, 8));
        assertFalse(WatchProtocol.isReaction69Score(9, 9));
        assertFalse(WatchProtocol.isReaction69Score(7, 9));
    }
}
