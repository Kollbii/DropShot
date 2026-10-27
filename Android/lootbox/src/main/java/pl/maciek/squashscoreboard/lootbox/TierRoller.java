package pl.maciek.squashscoreboard.lootbox;

import java.security.SecureRandom;
import java.util.Random;

/** Rolls rarest first. Each tier's conditional chance preserves its absolute weight. */
public final class TierRoller {
    private final Random random;

    public TierRoller() {
        this(new SecureRandom());
    }

    public TierRoller(Random random) {
        if (random == null) throw new IllegalArgumentException("random is required");
        this.random = random;
    }

    public LootTier roll() {
        LootTier[] tiers = LootTier.values();
        int remaining = 0;
        for (LootTier tier : tiers) {
            if (tier.weight <= 0) throw new IllegalStateException("Tier weights must be positive");
            remaining += tier.weight;
        }
        for (LootTier tier : tiers) {
            // Last tier is guaranteed once all higher-tier rolls fail.
            if (random.nextDouble() * remaining < tier.weight) return tier;
            remaining -= tier.weight;
        }
        throw new IllegalStateException("No tier selected");
    }
}
