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
        assertFalse(InputValidator.isAllowedWatchCommand("DROP_ALL"));
        assertFalse(InputValidator.isAllowedWatchCommand(""));

        // Constant-time token verification
        WatchCommandServer server = new WatchCommandServer(null, cmd -> true);
        server.setExpectedToken("secret-session-token-123");
        assertFalse(server.verifyTokenIfConfigured(null));
        assertFalse(server.verifyTokenIfConfigured("wrong-token"));
        assertTrue(server.verifyTokenIfConfigured("secret-session-token-123"));
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

        // Undo across set boundary restores 10-9 in set 1
        state.undo();
        assertEquals(0, state.getPlayerOneSets());
        assertEquals(10, state.getPlayerOnePoints());
        assertEquals(9, state.getPlayerTwoPoints());
    }

    @Test
    public void hashesCloudTokensWithSha256AndValidatesHttpsEndpoints() {
        String digest = CloudConfigStore.sha256Hex("player-secret-token");
        assertEquals(64, digest.length());
        assertTrue(CloudConfigStore.isValidTokenHash(digest));
        // Deterministic SHA-256 output
        assertEquals(digest, CloudConfigStore.sha256Hex("  player-secret-token\n"));
        assertNotEquals(digest, CloudConfigStore.sha256Hex("different-token"));

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
