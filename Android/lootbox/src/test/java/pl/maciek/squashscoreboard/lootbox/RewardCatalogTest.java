package pl.maciek.squashscoreboard.lootbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public final class RewardCatalogTest {
    @Test public void colorAndFontRewardsShareTiersWithoutKeyCollisions() {
        Set<String> ids = new HashSet<>();
        Set<Integer> colorKeys = new HashSet<>();
        int grayFonts = 0, greenFonts = 0, colors = 0;
        for (RewardCatalog.Reward reward : RewardCatalog.all()) {
            assertTrue("duplicate reward ID: " + reward.id, ids.add(reward.id));
            if (reward.kind == RewardCatalog.Kind.FONT) {
                assertNotNull(reward.fontFile);
                if (reward.tier == LootTier.GRAY) grayFonts++;
                if (reward.tier == LootTier.GREEN) greenFonts++;
            } else {
                assertTrue("duplicate color key: " + reward.id, colorKeys.add(reward.key()));
                colors++;
            }
        }
        assertEquals(25, colors);
        assertEquals(10, grayFonts);
        assertEquals(5, greenFonts);
        assertEquals(40, ids.size());
        assertEquals(16, RewardCatalog.fonts().length); // 15 drops + free default
    }
}
