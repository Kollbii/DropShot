package pl.maciek.squashscoreboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Automated verification suite for `:app`:
 * - Input sanitization, BiDi/control character stripping, and bounds clamping
 * - Non-enumerable CSPRNG IDs
 * - Localhost bridge (`WatchCommandServer`) DNS rebinding & constant-time token verification
 * - Deterministic `MatchState` transitions and undo integrity
 * - Supabase cloud endpoint validation and SHA-256 token hashing (`CloudConfigStore`)
 */
public final class SecurityAndConcurrencyTest {

    @Test
    public void sanitizesUntrustedPlayerNamesAndProfileIds() {
        // Strip control characters and Unicode BiDi override characters
        String malicious = "Alice\u0000\u0007\u202E<script>alert(1)</script>12345678901234567890";
        String cleaned = InputValidator.sanitizePlayerName(malicious, "PLAYER 1");
        assertFalse(cleaned.contains("\u0000"));
        assertFalse(cleaned.contains("\u202E"));
        assertEquals(InputValidator.MAX_NAME_LENGTH, cleaned.length());

        // Reject path traversal & injection profile IDs
        assertFalse(InputValidator.isValidProfileId("../profiles"));
        assertFalse(InputValidator.isValidProfileId("id:admin"));
        assertTrue(InputValidator.isValidProfileId(InputValidator.generateProfileUuid()));

        // Clamp negative and excessive match rules (prevents divide-by-zero in chart views)
        assertEquals(1, InputValidator.clampPointsPerSet(-10));
        assertEquals(1, InputValidator.clampPointsPerSet(0));
        assertEquals(InputValidator.MAX_POINTS_PER_SET, InputValidator.clampPointsPerSet(5000));
        assertEquals(1, InputValidator.clampSetsPerMatch(0));
    }

    @Test
    public void generatesNonEnumerableMatchIds() {
        long id1 = InputValidator.generateSecureMatchId();
        long id2 = InputValidator.generateSecureMatchId();
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertNotEquals(id1, id2);
        // Should not be sequential (+1)
        assertTrue(Math.abs(id1 - id2) > 1000L);
    }

    @Test
    public void watchCommandServerBlocksDnsRebindingAndVerifiesTokensConstantTime() {
        // Loopback host allowlist check against DNS rebinding
        assertTrue(WatchCommandServer.isLoopbackHost("127.0.0.1:28282"));
        assertTrue(WatchCommandServer.isLoopbackHost("localhost:28282"));
        assertFalse(WatchCommandServer.isLoopbackHost("evil-rebind.attacker.com:28282"));
        assertFalse(WatchCommandServer.isLoopbackHost("192.168.1.50:28282"));

        // Command allowlist
        assertTrue(InputValidator.isAllowedWatchCommand("POINT_1"));
        assertTrue(InputValidator.isAllowedWatchCommand("POINT_2"));
        assertTrue(InputValidator.isAllowedWatchCommand("UNDO"));
        assertTrue(InputValidator.isAllowedWatchCommand("RESET"));
        assertTrue(InputValidator.isAllowedWatchCommand("TOGGLE_PAUSE"));
        assertTrue(InputValidator.isAllowedWatchCommand("TOGGLE_SIDE"));
        assertFalse(InputValidator.isAllowedWatchCommand("DROP_ALL"));
        assertFalse(InputValidator.isAllowedWatchCommand(""));

        // Constant-time token verification
        WatchCommandServer server = new WatchCommandServer(null, cmd -> true);
        server.setExpectedToken("secret-session-token-123");
        assertFalse(server.verifyTokenIfConfigured(null));
        assertFalse(server.verifyTokenIfConfigured("wrong-token"));
        assertTrue(server.verifyTokenIfConfigured("secret-session-token-123"));

        // Bluetooth watch RFCOMM JSON command validation
        assertEquals("POINT_1", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"POINT_1\"}"));
        assertEquals("POINT_2", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"POINT_2\"}"));
        assertEquals("UNDO", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"UNDO\"}"));
        assertEquals("RESET", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"RESET\"}"));
        assertEquals("TOGGLE_PAUSE", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"TOGGLE_PAUSE\"}"));
        assertEquals("TOGGLE_SIDE", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"TOGGLE_SIDE\"}"));
        assertEquals(null, BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"EXEC\"}"));
        assertEquals(null, BluetoothWatchServer.parseWatchCommandJson("not-json"));
    }

    @Test
    public void matchStateExecutesDeterministicallyWithReversibleUndo() {
        MatchState state = new MatchState();
        state.configure(11, 2);
        for (int i = 0; i < 10; i++) state.addPoint(1);
        for (int i = 0; i < 9; i++) state.addPoint(2);
        assertEquals(10, state.getPlayerOnePoints());
        assertEquals(9, state.getPlayerTwoPoints());

        // Win set 1 (11-9)
        state.addPoint(1);
        assertEquals(1, state.getPlayerOneSets());
        assertEquals(0, state.getPlayerOnePoints());

        // Undo across set boundary restores 10-9 in set 1 (where Player 2 scored the 9th point)
        state.undo();
        assertEquals(0, state.getPlayerOneSets());
        assertEquals(10, state.getPlayerOnePoints());
        assertEquals(9, state.getPlayerTwoPoints());
        assertTrue(state.isGameOrMatchBall());
        assertEquals(2, state.getActiveServer());
        assertEquals("R", state.getServiceSide());

        // Verify server alternation and initials/JSON builders for watch state
        state.addPoint(2); // 10-10 consecutive point for Player 2 -> toggles to L
        assertEquals(2, state.getActiveServer());
        assertEquals("L", state.getServiceSide());
        assertFalse(state.isGameOrMatchBall());

        state.addPoint(2); // 10-11 consecutive point for Player 2 -> toggles to R, game ball
        assertEquals(2, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertTrue(state.isGameOrMatchBall());

        state.addPoint(1); // 11-11 handout to Player 1 -> starts fresh on R
        assertEquals(1, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertFalse(state.isGameOrMatchBall());

        // Player 1 chooses to toggle service side to L
        state.toggleServiceSide();
        assertEquals("L", state.getServiceSide());
        state.toggleServiceSide();
        assertEquals("R", state.getServiceSide());

        assertEquals("KAC", BluetoothWatchServer.extractInitials("Kacper", "P1"));
        assertEquals("ŁUK", BluetoothWatchServer.extractInitials("Łukasz", "P2"));
        String stateLine = BluetoothWatchServer.buildStateJsonLine(
            "Kacper", "Łukasz", 10, 11, 1, 0, 2, 2, "L",
            0xFF2F7CF6, false, BluetoothWatchServer.RAINBOW_COLOR_KEY, true,
            "STOP", true, false, false
        );
        assertTrue(stateLine.contains("\"type\":\"STATE\""));
        assertTrue(stateLine.contains("\"n1\":\"KAC\""));
        assertTrue(stateLine.contains("\"r2\":true"));
        assertTrue(stateLine.contains("\"side\":\"L\""));

        String matchEndedLine = BluetoothWatchServer.buildMatchEndedJsonLine("Kacper", "2 - 1");
        assertTrue(matchEndedLine.contains("\"type\":\"MATCH_ENDED\""));
        assertTrue(matchEndedLine.contains("\"winner\":\"Kacper\""));
        assertTrue(matchEndedLine.contains("\"score\":\"2 - 1\""));
    }

    @Test
    public void hashesCloudTokensWithSha256AndValidatesHttpsEndpoints() {
        String digest = CloudConfigStore.sha256Hex("player-secret-token");
        assertEquals(64, digest.length());
        assertTrue(CloudConfigStore.isValidTokenHash(digest));
        // Deterministic SHA-256 output and pass-through normalization of existing 64-char hex hashes
        assertEquals(digest, CloudConfigStore.sha256Hex("  player-secret-token\n"));
        assertEquals(digest, CloudConfigStore.sha256Hex(digest.toUpperCase()));
        assertNotEquals(digest, CloudConfigStore.sha256Hex("different-token"));

        // Distinguish legacy JWT anon keys (eyJ...) from publishable keys (sb_publishable_...)
        assertTrue(SupabaseSyncClient.isJwtKey("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiJ9.sig"));
        assertFalse(SupabaseSyncClient.isJwtKey("sb_publishable_abc123def456"));
        assertFalse(SupabaseSyncClient.isJwtKey(""));
        assertFalse(SupabaseSyncClient.isJwtKey(null));

        // Reject invalid hashes
        assertFalse(CloudConfigStore.isValidTokenHash(null));
        assertFalse(CloudConfigStore.isValidTokenHash("short"));
        assertFalse(CloudConfigStore.isValidTokenHash("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));

        // Validate HTTPS-only endpoints
        assertTrue(CloudConfigStore.isValidHttpsEndpoint("https://example-project.supabase.co/"));
        assertEquals("https://example-project.supabase.co", CloudConfigStore.normalizeHttpsEndpoint("https://example-project.supabase.co///"));
        assertFalse(CloudConfigStore.isValidHttpsEndpoint("http://example-project.supabase.co"));
        assertFalse(CloudConfigStore.isValidHttpsEndpoint("javascript:alert(1)"));
        assertFalse(CloudConfigStore.isValidHttpsEndpoint(""));
    }
}
