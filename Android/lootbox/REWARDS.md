# Reward catalog

The single source of truth is [`RewardCatalog.java`](src/main/java/pl/maciek/squashscoreboard/lootbox/RewardCatalog.java), in the `ENTRIES` array. Each reward has a stable ID, display name, tier and one or more ARGB colors. One color makes a solid; multiple colors make a gradient. The animated rainbow uses `animated: true` in its constructor.

| Tier | Rewards | Drop chance |
| --- | ---: | ---: |
| Gold | 1 animated rainbow | 1% |
| Purple | 5 gradients | 4% |
| Green | 5 selected solid colors | 25% |
| Gray | 14 solid colors | 70% |

To add a reward, add a `solid(...)` or `gradient(...)` entry with a unique ID and unique first color (the first color is the key stored in profiles and match history). To change its tier, edit only its `LootTier` value. To remove a reward, delete its entry. Keep IDs and first colors stable for rewards already distributed; changing either breaks existing unlocks or saved player colors. The four starter colors (`blue`, `red`, `green`, `gold`) are always available and retain their historical color keys.

Lootbox tier probabilities live in [`LootTier.java`](src/main/java/pl/maciek/squashscoreboard/lootbox/LootTier.java). After a tier is drawn, one reward in that tier is picked uniformly. Unlocks are stored by reward ID and player profile ID in `RewardInventory`. The four starter colors remain available to everyone; each other reward is available only to the player who received it. The `deweloper` profile can use every reward. Repeated drops remain in the pool and are labeled as already owned.
