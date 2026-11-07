# Reward catalog

The single source of truth is [`RewardCatalog.java`](src/main/java/pl/maciek/squashscoreboard/lootbox/RewardCatalog.java) (`ENTRIES` array). Each reward has a stable ID, display name, tier, and kind (`COLOR` or `FONT`). Color rewards hold 1 or more ARGB colors; font rewards reference a bundled `.ttf` file in `Android/app/src/main/assets/fonts/`. The animated rainbow uses `animated: true`.

| Tier | Rewards | Drop chance |
| --- | ---: | ---: |
| Gold | 1 animated rainbow | 1% |
| Purple | 5 gradients | 4% |
| Green | 5 selected solid colors + 5 fonts | 25% |
| Gray | 14 solid colors + 10 fonts | 70% |

To add a color, add a `solid(...)` or `gradient(...)` entry with a unique ID and unique first color (the first color is the key stored in profiles and match history). To add a font, add a `font(...)` entry with a unique ID and a matching `.ttf` file in `app/src/main/assets/fonts/`. To change a tier, edit its `LootTier` value. Keep IDs and first colors stable for rewards already distributed. The 4 starter colors (`blue`, `red`, `green`, `gold`) and the built-in default font (`font-default`) are always available.

Lootbox tier probabilities live in [`LootTier.java`](src/main/java/pl/maciek/squashscoreboard/lootbox/LootTier.java). After a tier is rolled, 1 color or font in that tier is selected uniformly from the precomputed tier pool. Unlocks are stored per player profile ID in encrypted `RewardInventory` storage (`available` returns owned colors; `availableAll` returns owned colors and fonts for Supabase cloud sync). Developer bypasses are permanently disabled (`RewardInventory.isDeveloper` always returns `false`). Repeated drops stay in the pool and show as already owned.

## Bundled fonts

- **Gray**: Rajdhani, Oxanium, Chakra Petch, Teko, Barlow Condensed, Exo 2, Russo One, Audiowide, Righteous, Space Grotesk.
- **Green**: Cinzel Decorative, Pirata One, New Rocker, Black Ops One, Eagle Lake.

Font binaries come from the [Google Fonts repository](https://github.com/google/fonts) (`ofl/` directory). Family-specific SIL Open Font License texts are stored in `Android/font-licenses/`. Every bundled font supports the Polish sample `Zażółć gęślą jaźń Łukasz`.
