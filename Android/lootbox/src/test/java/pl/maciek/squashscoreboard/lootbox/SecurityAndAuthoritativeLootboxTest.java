package pl.maciek.squashscoreboard.lootbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Automated security and concurrency verification suite for `:lootbox`:
 * - Anti-IDOR / BOLA cross-profile ticket redemption rejection
 * - Single-use ticket replay & HMAC signature tampering rejection
 * - AES-256-GCM encryption + AAD binding verification (`SecureStorageEngine`)
 * - Multi-threaded concurrency determinism
 */
public final class SecurityAndAuthoritativeLootboxTest {

    @Test
    public void rejectsBolaCrossProfileRedemptionAndTicketReplay() {
        LootboxAuthoritativeService service = new LootboxAuthoritativeService();
        String ownerProfileId = UUID.randomUUID().toString();
        String attackerProfileId = UUID.randomUUID().toString();

        LootboxAuthoritativeService.SpinOutcome spin = service.initiateSpin(ownerProfileId, 50, 42);
        assertNotNull(spin.ticketId);
        assertNotNull(spin.signature);
        assertEquals(spin.reward, spin.reelStrip[42]);

        // 1. BOLA attack: Attacker attempts to redeem Owner's ticket for Attacker's profileId
        try {
            service.redeemSpin(null, attackerProfileId, spin.ticketId, spin.signature);
            fail("Expected SecurityException on cross-profile BOLA redemption");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("BOLA"));
        }

        // 2. Ticket is consumed/invalidated on failed tampering attempt (single-use atomic removal)
        try {
            service.redeemSpin(null, ownerProfileId, spin.ticketId, spin.signature);
            fail("Expected SecurityException on consumed ticket replay");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("not found or already redeemed"));
        }
    }

    @Test
    public void rejectsTamperedHmacSignatureAndInvalidProfileIds() {
        LootboxAuthoritativeService service = new LootboxAuthoritativeService();
        String validProfile = UUID.randomUUID().toString();

        // Reject SQLi / path traversal / empty profile IDs
        assertFalse(LootboxAuthoritativeService.isValidProfileId(null));
        assertFalse(LootboxAuthoritativeService.isValidProfileId(""));
        assertFalse(LootboxAuthoritativeService.isValidProfileId("../etc/passwd"));
        assertFalse(LootboxAuthoritativeService.isValidProfileId("user' OR '1'='1"));

        LootboxAuthoritativeService.SpinOutcome spin = service.initiateSpin(validProfile, 50, 42);
        String tamperedSig = spin.signature.substring(0, spin.signature.length() - 2) + "ff";
        if (tamperedSig.equals(spin.signature)) {
            tamperedSig = spin.signature.substring(0, spin.signature.length() - 2) + "00";
        }

        try {
            service.redeemSpin(null, validProfile, spin.ticketId, tamperedSig);
            fail("Expected SecurityException for forged HMAC signature");
        } catch (SecurityException expected) {
            assertTrue(expected.getMessage().contains("Invalid ticket signature"));
        }
    }

    @Test
    public void developerLookupRequiresContext() {
        assertNull(RewardInventory.developerId(null));
        assertFalse(RewardInventory.isDeveloper(null, "developer-profile"));
        assertFalse(RewardInventory.isDeveloper(null, "deweloper"));
    }

    @Test
    public void aes256GcmEncryptsWithRandomNoncesAndRejectsAadSubstitution() {
        byte[] aadProfileA = SecureStorageEngine.buildAad("reward_inventory", "profileA:rainbow");
        byte[] aadProfileB = SecureStorageEngine.buildAad("reward_inventory", "profileB:rainbow");

        String c1 = SecureStorageEngine.encrypt("true", aadProfileA);
        String c2 = SecureStorageEngine.encrypt("true", aadProfileA);
        assertNotNull(c1);
        assertNotNull(c2);
        // CSPRNG 96-bit IV guarantees distinct ciphertexts for identical plaintexts
        assertNotEquals(c1, c2);

        // Decryption succeeds with matching AAD
        assertEquals("true", SecureStorageEngine.decrypt(c1, aadProfileA));

        // Cross-profile AAD substitution (copying profileA's ciphertext into profileB's slot) MUST fail GCM tag check
        assertNull(SecureStorageEngine.decrypt(c1, aadProfileB));

        // Bit-flipping ciphertext MUST fail GCM authentication
        char lastChar = c1.charAt(c1.length() - 2);
        char flipped = lastChar == 'A' ? 'B' : 'A';
        String corrupted = c1.substring(0, c1.length() - 2) + flipped + c1.substring(c1.length() - 1);
        assertNull(SecureStorageEngine.decrypt(corrupted, aadProfileA));
    }

    @Test
    public void concurrentTicketRedemptionAllowsExactlyOneWinner() throws Exception {
        LootboxAuthoritativeService service = new LootboxAuthoritativeService();
        String profileId = UUID.randomUUID().toString();
        LootboxAuthoritativeService.SpinOutcome spin = service.initiateSpin(profileId, 50, 42);

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger rejections = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                try {
                    RewardCatalog.Reward r = service.redeemSpin(null, profileId, spin.ticketId, spin.signature);
                    if (r != null) successes.incrementAndGet();
                } catch (SecurityException e) {
                    rejections.incrementAndGet();
                }
                return null;
            });
        }

        for (Future<Void> f : pool.invokeAll(tasks)) {
            f.get();
        }
        pool.shutdownNow();

        assertEquals("Single-use ticket must succeed exactly once under concurrency", 1, successes.get());
        assertEquals(threads - 1, rejections.get());
    }
}

