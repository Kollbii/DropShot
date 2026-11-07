package pl.maciek.squashscoreboard.lootbox;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * [ASVS V4.1 / V4.2] [MASVS-CRYPTO] [MASVS-RESILIENCE]
 * Authoritative domain service for lootbox rolls and cosmetic entitlement grants.
 * - Eliminates client-UI RNG manipulation (`java.util.Random` replaced with `SecureRandom`).
 * - Prevents IDOR / BOLA cross-profile reward unlocking via cryptographic single-use redemption tickets
 *   bound to the authenticated/selected `profileId` and `rewardId`.
 */
public final class LootboxAuthoritativeService {
    private static final Pattern SAFE_PROFILE_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final long TICKET_TTL_MS = 30_000L;

    private final SecureRandom csprng;
    private final TierRoller tierRoller;
    private final byte[] hmacSecret = new byte[32];
    private final Map<String, PendingRoll> activeTickets = new ConcurrentHashMap<>();

    public static final class SpinOutcome {
        public final String ticketId;
        public final String signature;
        public final String profileId;
        public final LootTier tier;
        public final RewardCatalog.Reward reward;
        public final RewardCatalog.Reward[] reelStrip;
        public final float landingFraction;
        public final int startIndex;

        private SpinOutcome(
            String ticketId,
            String signature,
            String profileId,
            LootTier tier,
            RewardCatalog.Reward reward,
            RewardCatalog.Reward[] reelStrip,
            float landingFraction,
            int startIndex
        ) {
            this.ticketId = ticketId;
            this.signature = signature;
            this.profileId = profileId;
            this.tier = tier;
            this.reward = reward;
            this.reelStrip = reelStrip;
            this.landingFraction = landingFraction;
            this.startIndex = startIndex;
        }
    }

    private static final class PendingRoll {
        final String profileId;
        final RewardCatalog.Reward reward;
        final long expiresAtMs;
        final String expectedSignature;

        PendingRoll(String profileId, RewardCatalog.Reward reward, long expiresAtMs, String expectedSignature) {
            this.profileId = profileId;
            this.reward = reward;
            this.expiresAtMs = expiresAtMs;
            this.expectedSignature = expectedSignature;
        }
    }

    public LootboxAuthoritativeService() {
        this(new SecureRandom());
    }

    public LootboxAuthoritativeService(SecureRandom csprng) {
        if (csprng == null) throw new IllegalArgumentException("CSPRNG is required");
        this.csprng = csprng;
        this.tierRoller = new TierRoller(csprng);
        this.csprng.nextBytes(this.hmacSecret);
    }

    public static boolean isValidProfileId(String profileId) {
        return profileId != null && SAFE_PROFILE_ID.matcher(profileId).matches();
    }

    /**
     * Executes an authoritative CSPRNG lootbox roll and issues a single-use signed redemption ticket
     * bound to the requesting `profileId`.
     */
    public SpinOutcome initiateSpin(String profileId, int stripLength, int targetIndex) {
        if (!isValidProfileId(profileId)) {
            throw new SecurityException("Unauthorized or invalid profile identifier");
        }
        if (stripLength <= 0 || targetIndex < 0 || targetIndex >= stripLength) {
            throw new IllegalArgumentException("Invalid reel strip configuration");
        }
        purgeExpiredTickets();

        LootTier winningTier = tierRoller.roll();
        RewardCatalog.Reward winningReward = RewardCatalog.pick(winningTier, csprng);
        RewardCatalog.Reward[] strip = new RewardCatalog.Reward[stripLength];
        for (int i = 0; i < stripLength; i++) {
            strip[i] = RewardCatalog.pick(tierRoller.roll(), csprng);
        }
        strip[targetIndex] = winningReward;

        float landingFraction = ReelLanding.pickOffsetFraction(csprng);
        int startIndex = 10 + csprng.nextInt(6);

        String ticketId = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + TICKET_TTL_MS;
        String signature = signTicket(ticketId, profileId, winningReward.id, expiresAt);

        activeTickets.put(ticketId, new PendingRoll(profileId, winningReward, expiresAt, signature));
        return new SpinOutcome(ticketId, signature, profileId, winningTier, winningReward, strip, landingFraction, startIndex);
    }

    /**
     * Atomically redeems a single-use spin ticket, verifying profile ownership (anti-IDOR/BOLA),
     * reward match, expiration, and HMAC signature in constant time before unlocking.
     */
    public RewardCatalog.Reward redeemSpin(Context context, String requestingProfileId, String ticketId, String signature) {
        if (!isValidProfileId(requestingProfileId) || ticketId == null || signature == null) {
            throw new SecurityException("Invalid redemption request");
        }
        PendingRoll pending = activeTickets.remove(ticketId);
        if (pending == null) {
            throw new SecurityException("Spin ticket not found or already redeemed");
        }
        if (System.currentTimeMillis() > pending.expiresAtMs) {
            throw new SecurityException("Spin ticket has expired");
        }
        // [ASVS V4.2] Anti-IDOR / BOLA check: requesting profile MUST match the ticket's owner profile
        if (!pending.profileId.equals(requestingProfileId)) {
            throw new SecurityException("BOLA violation: profileId mismatch on ticket redemption");
        }
        byte[] expectedBytes = pending.expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = signature.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw new SecurityException("Invalid ticket signature");
        }
        if (context != null) {
            RewardInventory.unlock(context, pending.profileId, pending.reward);
        }
        return pending.reward;
    }

    private String signTicket(String ticketId, String profileId, String rewardId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            String payload = ticketId + ":" + profileId + ":" + rewardId + ":" + expiresAt;
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private void purgeExpiredTickets() {
        long now = System.currentTimeMillis();
        activeTickets.entrySet().removeIf(entry -> now > entry.getValue().expiresAtMs);
    }
}

