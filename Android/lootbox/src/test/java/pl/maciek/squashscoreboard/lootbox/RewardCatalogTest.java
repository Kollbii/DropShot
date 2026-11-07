package pl.maciek.squashscoreboard.lootbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class RewardCatalogTest {
    @Test
    public void colorAndFontRewardsShareTiersWithoutKeyCollisions() {
        Set<String> ids = new HashSet<>();
        Set<Integer> colorKeys = new HashSet<>();
        int grayFonts = 0, greenFonts = 0, colors = 0;
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            assertTrue("duplicate reward ID: " + reward.id, ids.add(reward.id));
            assertSame(reward, RewardCatalog.byId(reward.id));
            assertSame(reward, RewardCatalog.byId(reward.name));
            if (reward.kind == RewardCatalog.Kind.FONT) {
                assertNotNull(reward.fontFile);
                assertSame(reward, RewardCatalog.fontOrDefault(reward.id));
                if (reward.tier == LootTier.GRAY) grayFonts++;
                if (reward.tier == LootTier.GREEN) greenFonts++;
            } else {
                assertTrue("duplicate color key: " + reward.id, colorKeys.add(reward.key()));
                assertSame(reward, RewardCatalog.byKey(reward.key()));
                colors++;
            }
        }
        assertEquals(25, colors);
        assertEquals(10, grayFonts);
        assertEquals(5, greenFonts);
        assertEquals(40, ids.size());
        assertEquals(16, RewardCatalog.fonts().length); // 15 drops + free default
    }

    @Test
    public void resolvesDefaultFontAndNullLookupSafely() {
        assertNull(RewardCatalog.byId(null));
        assertNull(RewardCatalog.byId(""));
        assertNull(RewardCatalog.byId("unknown-cosmetic-id"));

        RewardCatalog.Reward def = RewardCatalog.defaultFont();
        assertNotNull(def);
        assertEquals("font-default", def.id);
        assertEquals(RewardCatalog.Kind.FONT, def.kind);
        assertSame(def, RewardCatalog.byId("font-default"));
        assertSame(def, RewardCatalog.fontOrDefault(null));
        assertSame(def, RewardCatalog.fontOrDefault(""));
        assertSame(def, RewardCatalog.fontOrDefault("orchid")); // color ID falls back to default font
        assertSame(def, RewardCatalog.fontOrDefault("unknown-font"));
    }

    @Test
    public void picksTierMatchedRewardsDeterministically() {
        SecureRandom random = new SecureRandom();
        for (LootTier tier : LootTier.values()) {
            for (int i = 0; i < 20; i++) {
                RewardCatalog.Reward picked = RewardCatalog.pick(tier, random);
                assertNotNull(picked);
                assertEquals(tier, picked.tier);
            }
        }
    }
}
