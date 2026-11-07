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
        assertTrue(InputValidator.isAllowedWatchCommand("SWITCH_SERVER"));
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
        assertEquals("SWITCH_SERVER", BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"SWITCH_SERVER\"}"));
        assertEquals(null, BluetoothWatchServer.parseWatchCommandJson("{\"command\":\"EXEC\"}"));
        assertEquals(null, BluetoothWatchServer.parseWatchCommandJson("not-json"));
    }

    @Test
    public void matchStateExecutesDeterministicallyWithReversibleUndo() {
        MatchState state = new MatchState();
        state.configure(11, 2);
        assertTrue(state.canSelectInitialServer());
        assertTrue(state.canChooseServiceSide());

        for (int i = 0; i < 10; i++) state.addPoint(1);
        for (int i = 0; i < 9; i++) state.addPoint(2);
        assertEquals(10, state.getPlayerOnePoints());
        assertEquals(9, state.getPlayerTwoPoints());
        assertFalse(state.canSelectInitialServer());
        assertFalse(state.canChooseServiceSide());

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
        assertFalse(state.canChooseServiceSide());

        // Verify server alternation and initials/JSON builders for watch state
        state.addPoint(2); // 10-10 consecutive point for Player 2 -> toggles to L
        assertEquals(2, state.getActiveServer());
        assertEquals("L", state.getServiceSide());
        assertFalse(state.canChooseServiceSide());
        assertFalse(state.isGameOrMatchBall());

        state.addPoint(2); // 10-11 consecutive point for Player 2 -> toggles to R, game ball
        assertEquals(2, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertTrue(state.isGameOrMatchBall());

        state.addPoint(1); // 11-11 handout to Player 1 -> starts fresh on R and unlocks side choice
        assertEquals(1, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertTrue(state.canChooseServiceSide());
        assertFalse(state.isGameOrMatchBall());

        // Player 1 chooses to toggle service side to L and back to R as many times as desired
        assertTrue(state.toggleServiceSide());
        assertEquals("L", state.getServiceSide());
        assertTrue(state.toggleServiceSide());
        assertEquals("R", state.getServiceSide());

        assertEquals("KAC", BluetoothWatchServer.extractInitials("Kacper", "P1"));
        assertEquals("ŁUK", BluetoothWatchServer.extractInitials("Łukasz", "P2"));
        String stateLine = BluetoothWatchServer.buildStateJsonLine(
            "Kacper", "Łukasz", 10, 11, 1, 0, 2, 2, "L", false, false,
            0xFF2F7CF6, false, BluetoothWatchServer.RAINBOW_COLOR_KEY, true,
            "STOP", true, false, false
        );
        assertTrue(stateLine.contains("\"type\":\"STATE\""));
        assertTrue(stateLine.contains("\"n1\":\"KAC\""));
        assertTrue(stateLine.contains("\"r2\":true"));
        assertTrue(stateLine.contains("\"side\":\"L\""));
        assertTrue(stateLine.contains("\"canPickSide\":false"));
        assertTrue(stateLine.contains("\"canPickSrv\":false"));

        String matchEndedLine = BluetoothWatchServer.buildMatchEndedJsonLine("Kacper", "2 - 1");
        assertTrue(matchEndedLine.contains("\"type\":\"MATCH_ENDED\""));
        assertTrue(matchEndedLine.contains("\"winner\":\"Kacper\""));
        assertTrue(matchEndedLine.contains("\"score\":\"2 - 1\""));
    }

    @Test
    public void enforcesInitialServerSelectionAndStreakSideLockingRules() {
        MatchState state = new MatchState();
        state.configure(11, 2);

        // Before the first point of the match (0-0, 0-0), both initial server and side can be switched freely
        assertTrue(state.canSelectInitialServer());
        assertTrue(state.canChooseServiceSide());
        assertEquals(1, state.getActiveServer());
        assertEquals("R", state.getServiceSide());

        assertTrue(state.switchInitialServer());
        assertEquals(2, state.getActiveServer());
        assertEquals(2, state.getInitialServer());
        assertTrue(state.toggleServiceSide());
        assertEquals("L", state.getServiceSide());
        assertTrue(state.toggleServiceSide());
        assertEquals("R", state.getServiceSide());
        assertTrue(state.toggleServiceSide());
        assertEquals("L", state.getServiceSide());

        // Player 2 (starting server) wins the 1st point from L -> streak continues, side alternates to R and LOCKS
        state.addPoint(2);
        assertFalse(state.canSelectInitialServer());
        assertFalse(state.switchInitialServer());
        assertEquals(2, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertFalse(state.canChooseServiceSide());
        assertFalse(state.toggleServiceSide()); // Must reject side toggle during streak

        // Player 2 wins 2nd consecutive point -> alternates to L, still locked
        state.addPoint(2);
        assertEquals(2, state.getActiveServer());
        assertEquals("L", state.getServiceSide());
        assertFalse(state.canChooseServiceSide());
        assertFalse(state.toggleServiceSide());

        // Player 1 wins point (handout!) -> Player 1 becomes server and side picker UNLOCKS
        state.addPoint(1);
        assertEquals(1, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertTrue(state.canChooseServiceSide());
        assertTrue(state.toggleServiceSide());
        assertEquals("L", state.getServiceSide());
        assertTrue(state.toggleServiceSide());
        assertEquals("R", state.getServiceSide());
        assertTrue(state.toggleServiceSide());
        assertEquals("L", state.getServiceSide());

        // Player 1 wins consecutive point on own serve -> alternates to R and LOCKS
        state.addPoint(1);
        assertEquals(1, state.getActiveServer());
        assertEquals("R", state.getServiceSide());
        assertFalse(state.canChooseServiceSide());
        assertFalse(state.toggleServiceSide());

        // Check encoded point sides ("LRL" + "L" -> "LRLL")
        assertEquals("LRLL", state.getEncodedPointSides());

        // Undo restores the unlocked handout state with side L
        state.undo();
        assertEquals(1, state.getActiveServer());
        assertTrue(state.canChooseServiceSide());
        assertEquals("LRL", state.getEncodedPointSides());
    }

    @Test
    public void computesConstantTimePointMetadataAndScalableStatistics() {
        java.util.List<Integer> winners = java.util.Arrays.asList(2, 2, 1, 1);
        java.util.List<Long> setDurations = java.util.Collections.emptyList();
        java.util.List<Long> pointTimes = java.util.Arrays.asList(1000L, 2000L, 3000L, 4000L);
        MatchRecord restored = new MatchRecord(
            12345L, "Alice", "Bob", "p1", "p2",
            0, 0, 2, 2, 11, 2, false, 1700000000000L,
            winners, 4000L, setDurations, pointTimes, false,
            AppPreferences.BLUE, AppPreferences.RED,
            2, "LRLL"
        );

        assertEquals(2, restored.initialServer);
        assertEquals("LRLL", restored.pointSides);

        // Point 0: Server 2 (initial), Side L (picked), Winner 2 -> score 0:1
        MatchRecord.PointScore p0 = restored.scoreAtPoint(0);
        assertEquals(2, p0.server);
        assertEquals('L', p0.serviceSide);
        assertTrue(p0.sidePicked);
        assertEquals(0, p0.pointsOne);
        assertEquals(1, p0.pointsTwo);

        // Point 1: Server 2 (streak), Side R (alternated), Winner 2 -> score 0:2
        MatchRecord.PointScore p1 = restored.scoreAtPoint(1);
        assertEquals(2, p1.server);
        assertEquals('R', p1.serviceSide);
        assertFalse(p1.sidePicked);

        // Point 2: Server 2 (streak), Side L (alternated), Winner 1 (handout) -> score 1:2
        MatchRecord.PointScore p2 = restored.scoreAtPoint(2);
        assertEquals(2, p2.server);
        assertEquals('L', p2.serviceSide);
        assertFalse(p2.sidePicked);

        // Point 3: Server 1 (handout!), Side L (picked), Winner 1 -> score 2:2
        MatchRecord.PointScore p3 = restored.scoreAtPoint(3);
        assertEquals(1, p3.server);
        assertEquals('L', p3.serviceSide);
        assertTrue(p3.sidePicked);

        // Verify single-pass O(P + M * N) PlayerStatistics aggregation
        PlayerProfile alice = new PlayerProfile("p1", "Alice", AppPreferences.BLUE);
        PlayerProfile bob = new PlayerProfile("p2", "Bob", AppPreferences.RED);
        java.util.Map<String, PlayerStatistics.Summary> stats = PlayerStatistics.calculateAllFromRecords(
            java.util.Collections.singletonList(restored),
            java.util.Arrays.asList(alice, bob)
        );
        PlayerStatistics.Summary sAlice = stats.get("p1");
        PlayerStatistics.Summary sBob = stats.get("p2");

        // Bob served 3 times (won 2, lost 1) and received 1 time (won 0)
        assertEquals(3, sBob.servesTotal);
        assertEquals(2, sBob.servesWon);
        assertEquals(1, sBob.receivesTotal);
        assertEquals(0, sBob.receivesWon);
        assertEquals(1, sBob.leftPicks);
        assertEquals(0, sBob.rightPicks);
        assertEquals(2, sBob.leftServesTotal);
        assertEquals(1, sBob.leftServesWon);
        assertEquals(1, sBob.rightServesTotal);
        assertEquals(1, sBob.rightServesWon);

        // Alice served 1 time (from L, won 1) and received 3 times (won 1)
        assertEquals(1, sAlice.servesTotal);
        assertEquals(1, sAlice.servesWon);
        assertEquals(3, sAlice.receivesTotal);
        assertEquals(1, sAlice.receivesWon);
        assertEquals(1, sAlice.leftPicks);
        assertEquals(0, sAlice.rightPicks);
        assertTrue(sAlice.markovServeShare() > 0.9);
        assertEquals(1.0 / 3.0, sAlice.laplaceWinProbability(), 1e-6);
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

    @Test
    public void sanitizesCosmeticFontIdsAndHandlesLegacySchemaFallback() {
        assertEquals("font-anton", InputValidator.sanitizeCosmeticIdOrNull("  FONT-ANTON  "));
        assertEquals("font-bebas-neue", InputValidator.sanitizeCosmeticIdOrNull("font-bebas-neue"));
        assertEquals(null, InputValidator.sanitizeCosmeticIdOrNull(null));
        assertEquals(null, InputValidator.sanitizeCosmeticIdOrNull(""));
        assertEquals(null, InputValidator.sanitizeCosmeticIdOrNull("null"));
        assertEquals(null, InputValidator.sanitizeCosmeticIdOrNull("font-default"));
        assertEquals(null, InputValidator.sanitizeCosmeticIdOrNull("../fonts/evil.ttf"));
        assertEquals(null, InputValidator.sanitizeCosmeticIdOrNull("font'; DROP TABLE cloud_players;--"));

        PlayerProfile profileWithFont = new PlayerProfile(
            "11111111-2222-4333-8444-555555555555",
            "Kacper",
            AppPreferences.BLUE,
            "  FONT-TEKO "
        );
        assertEquals("11111111-2222-4333-8444-555555555555", profileWithFont.id);
        assertEquals("Kacper", profileWithFont.name);
        assertEquals("font-teko", profileWithFont.fontId);

        PlayerProfile defaultFontProfile = new PlayerProfile(
            "developer-profile",
            "Deweloper",
            AppPreferences.RED,
            "font-default"
        );
        assertNotEquals("developer-profile", defaultFontProfile.id);
        assertEquals(null, defaultFontProfile.fontId);

        assertTrue(SupabaseSyncClient.shouldFallbackToLegacySchema(
            400,
            "{\"code\":\"PGRST204\",\"message\":\"Could not find the 'equipped_cosmetics' column of 'cloud_players' in the schema cache\"}"
        ));
        assertFalse(SupabaseSyncClient.shouldFallbackToLegacySchema(401, "Unauthorized"));
        assertFalse(SupabaseSyncClient.shouldFallbackToLegacySchema(403, "Forbidden"));
        assertFalse(SupabaseSyncClient.shouldFallbackToLegacySchema(201, ""));
    }
}
