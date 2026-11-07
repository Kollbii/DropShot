# AGENTS.md — Lootbox module

These rules apply to `Android/lootbox/`. They add to [../AGENTS.md](../AGENTS.md) and the root [AGENTS.md](../../AGENTS.md).

## 1. Purpose

The module gives cosmetic player colors and bundled name fonts. It contains:

| Class | Responsibility |
|---|---|
| `RewardCatalog` | The single source of truth for all rewards (ID, name, tier, kind, colors, font file) with $O(1)$ precomputed lookup tables. |
| `RewardFonts` | Lock-free process-wide `Typeface`, optical size-scale, and tile glyph size cache for bundled `.ttf` fonts. |
| `LootTier` | Tiers and absolute drop weights (total 100). |
| `TierRoller` | Rolls a tier. Checks the rarest tier first. |
| `LootboxAuthoritativeService` | `SecureRandom` roll, single-use HMAC-signed tickets, profile binding. |
| `RewardInventory` | Encrypted, profile-scoped unlocks (`available` for colors, `availableAll` for all kinds, `restoreFromCloud` for sync). |
| `SecureStorageEngine` | AES-256-GCM with AndroidKeyStore. AAD binds each value to its file and key. |
| `LootboxActivity`, `RewardTilePainter`, `ReelTiming`, `ReelLanding` | UI and 120 Hz zero-allocation animation. |

See [REWARDS.md](REWARDS.md) for the catalog rules.

## 2. Rules

1. The module must not import classes from `:app`. The dependency goes only from `:app` to `:lootbox`.
2. Keep each reward `id` and its first color stable. Saved profiles and matches use the first color as the key.
3. Each reward must have a unique `id` and a unique first color.
4. The weights in `LootTier` must total 100. Update `TierRollerTest` if you change them.
5. Do not unlock a reward without a redeemed ticket, except in the documented `TEMPORARY` seed in `ProfileStore`.
6. Redeem a ticket only once. Check the profile, the expiry, and the signature (constant-time compare).
7. Use `SecureRandom` for all rolls and reel positions.
8. Do not add a "developer" or "unlock all" bypass. `RewardInventory.isDeveloper` must return `false`.
9. Do not change `SecureStorageEngine.KEY_ALIAS` or `PREFIX`. A change makes all saved data unreadable.
10. Do not allocate objects in `onDraw` or in animator update listeners. Reuse `Path`, `Paint`, and arrays.

## 3. Caveats

1. `LootboxAuthoritativeService` runs in the app process. The HMAC secret is in memory. This is not server-side authority.
   A rooted device or a modified APK can unlock rewards. The design accepts this risk because rewards are cosmetic only.
2. Tickets are in memory. A ticket is lost if the activity is destroyed during the spin.
3. `RewardTilePainter.draw` reuses a static `FLOW_PATH` and caches shaders and font glyph sizes on the UI thread. Do not call `RewardTilePainter.draw` from background threads.
4. `RewardCatalog.configure` indexes `% 6` for animated rewards. An animated reward must have 7 colors (6 plus the first color again).
