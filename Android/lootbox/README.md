# Lootbox module

This Android library contains the placeholder lootbox screen and its drop logic. The host app only includes `:lootbox`, registers `LootboxActivity`, and opens it from `UiTheme` navigation. Other app features do not depend on lootbox internals.

`LootTier` lists tiers in rarest-first order with absolute percentage weights. `TierRoller` checks each tier in that order. The conditional chance for a tier is `tier.weight / sum(weights of remaining tiers)`, so the final distribution stays 1% gold, 4% purple, 25% green, 70% gray. To add a tier, insert it into `LootTier` and adjust weights to total 100. The reel's filler squares are only decoration; the selected result is rolled once before animation and placed at the reel's stopping point.

The ball cracks, then the reel slows to the selected square. With Android's animation scale set to zero, the result appears immediately. This is a local visual prototype: no inventory, persistence, payments, or server-side rewards are included.

Run `:lootbox:testDebugUnitTest` for roll tests and `:app:assembleDebug` for the integrated app.
