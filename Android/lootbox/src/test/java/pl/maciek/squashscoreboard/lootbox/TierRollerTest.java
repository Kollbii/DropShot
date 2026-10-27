package pl.maciek.squashscoreboard.lootbox;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import org.junit.Test;

public final class TierRollerTest {
    @Test public void checksRareTiersInOrderAndFallsBackToGray() {
        assertEquals(LootTier.GOLD, new TierRoller(fixed(0.009)).roll());
        assertEquals(LootTier.PURPLE, new TierRoller(sequence(0.5, 0.03)).roll());
        assertEquals(LootTier.GREEN, new TierRoller(sequence(0.5, 0.5, 0.2)).roll());
        assertEquals(LootTier.GRAY, new TierRoller(sequence(0.5, 0.5, 0.5, 0.5)).roll());
    }

    @Test public void absoluteDistributionMatchesConfiguredWeights() {
        TierRoller roller = new TierRoller(new Random(104));
        int[] counts = new int[LootTier.values().length];
        int samples = 200_000;
        for (int i = 0; i < samples; i++) counts[roller.roll().ordinal()]++;
        for (LootTier tier : LootTier.values()) {
            double observed = counts[tier.ordinal()] * 100.0 / samples;
            org.junit.Assert.assertTrue(tier.label + " observed " + observed,
                Math.abs(observed - tier.weight) < 0.5);
        }
    }

    private static Random fixed(double value) { return sequence(value); }

    private static Random sequence(double... values) {
        return new Random() {
            private int next;
            @Override public double nextDouble() {
                return values[Math.min(next++, values.length - 1)];
            }
        };
    }
}
