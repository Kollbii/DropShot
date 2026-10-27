# Lootbox module

This Android library contains the placeholder lootbox screen and its drop logic. The host app only includes `:lootbox`, registers `LootboxActivity`, and opens it from `UiTheme` navigation. Other app features do not depend on lootbox internals.

`LootTier` lists tiers in rarest-first order with absolute percentage weights. `TierRoller` checks each tier in that order. The conditional chance for a tier is `tier.weight / sum(weights of remaining tiers)`, so the final distribution stays 1% gold, 4% purple, 25% green, 70% gray. To add a tier, insert it into `LootTier` and adjust weights to total 100. The reel's filler squares are only decoration; the selected result is rolled once before animation and placed at the reel's stopping point.

The ball shakes for one second, cracks into two jagged halves over 1.2 seconds, and the reel appears in the same centered stage. The reel travels quickly for 1.5 seconds, slows for 3 seconds, then moves very slowly for the final 2 seconds. A smooth speed curve joins those sections and ends at zero speed. The stop point varies inside the winning square, with a margin from its edges. A centered reward card celebrates the result; Continue returns to a fresh ball. The landscape layout fits without scrolling. With Android's animation scale set to zero, the result appears immediately. This is a local visual prototype: no inventory, persistence, payments, or server-side rewards are included.

Run `:lootbox:testDebugUnitTest` for roll tests and `:app:assembleDebug` for the integrated app.
