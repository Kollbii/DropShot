# Reward catalog

The single source of truth is [`RewardCatalog.java`](src/main/java/pl/maciek/squashscoreboard/lootbox/RewardCatalog.java), in the `ENTRIES` array. Each reward has a stable ID, display name, tier and kind. Color rewards contain one or more ARGB colors; font rewards name a bundled `.ttf` file. The animated rainbow uses `animated: true` in its constructor.

| Tier | Rewards | Drop chance |
| --- | ---: | ---: |
| Gold | 1 animated rainbow | 1% |
| Purple | 5 gradients | 4% |
| Green | 5 selected solid colors + 5 fonts | 25% |
| Gray | 14 solid colors + 10 fonts | 70% |

To add a color, add a `solid(...)` or `gradient(...)` entry with a unique ID and unique first color (the first color is the key stored in profiles and match history). To add a font, add a `font(...)` entry with a unique ID and a corresponding file in `app/src/main/assets/fonts/`. To change its tier, edit its `LootTier` value. To remove a reward, delete its entry. Keep IDs and first colors stable for rewards already distributed; changing either breaks existing unlocks or saved player colors. The four starter colors (`blue`, `red`, `green`, `gold`) and built-in default font are always available.

Lootbox tier probabilities live in [`LootTier.java`](src/main/java/pl/maciek/squashscoreboard/lootbox/LootTier.java). After a tier is drawn, one color or font in that tier is picked uniformly from the same pool. Unlocks are stored by reward ID and player profile ID in `RewardInventory`. Each reward is available only to the player who received it; the `deweloper` profile can use every reward. Repeated drops remain in the pool and are labeled as already owned.

## Bundled fonts

Gray: Rajdhani, Oxanium, Chakra Petch, Teko, Barlow Condensed, Exo 2, Russo One, Audiowide, Righteous, Space Grotesk.

Green: Cinzel Decorative, Pirata One, New Rocker, Black Ops One, Eagle Lake.

Font binaries came from the [official Google Fonts repository](https://github.com/google/fonts), under each family's `ofl/` directory. Family-specific SIL Open Font License texts are copied to `Android/font-licenses/`. Every bundled font was checked for the Polish sample `Zażółć gęślą jaźń Łukasz`.
