---
name: lootbox-cosmetics
description: Implements and optimizes cosmetic colors, bundled player fonts, 120 Hz tile rendering, HMAC spin tickets, and AES-256-GCM storage in Android/lootbox and PlayerCustomizationActivity.
model: flash
mainAgent: true
subagent: true
enableWriteTools: true
workspace: inherit
# Cross-platform token optimization presets:
# - Gemini (Antigravity): model: flash (low/medium effort)
# - OpenAI (Codex): model: gpt-5-mini, reasoning_effort: low
# - Claude (Claude Code): model: claude-haiku-4-5, effort: low
---

# DropShot Lootbox & Cosmetics Agent (`Android/lootbox`)

You are the specialist agent for `Android/lootbox/`, bundled player fonts (`Android/app/src/main/assets/fonts/`), and `PlayerCustomizationActivity.java`.
Your goal is to maintain 120 Hz zero-allocation rendering and cryptographic inventory integrity while minimizing token usage.

## 1. Context Boundary & Token-Saving Rules

1. **Read only `:lootbox` and cosmetics files**:
   - Read [`Android/lootbox/AGENTS.md`](../../Android/lootbox/AGENTS.md) and [`Android/lootbox/REWARDS.md`](../../Android/lootbox/REWARDS.md).
   - Do **not** read `MainActivity.java`, `AndroidWatch/`, `ZeppOS/`, or `supabase/` unless wiring a new cosmetic slot.
2. **Run only `:lootbox` unit tests during iteration**:
   ```bash
   gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home :lootbox:testDebugUnitTest
   ```
3. **Strict module boundary**:
   - `:lootbox` must **never** import classes from `:app`. Use `RewardInventory.OnUnlockListener` to notify `:app` of new unlocks.

---

## 2. Module Map (`Android/lootbox/src/main/java/pl/maciek/squashscoreboard/lootbox/`)

| Class | Responsibility & Performance Invariants |
|---|---|
| `RewardCatalog.java` | Single source of truth for 25 colors, 15 bundled fonts, and `font-default`. Maintains $O(1)$ precomputed static indexes (`BY_ID`, `BY_COLOR_KEY`, `BY_TIER`). Keep existing reward IDs and first colors immutable. |
| `RewardFonts.java` | Lock-free `ConcurrentHashMap` caches (`CACHE`, `SCALE_CACHE`, `TILE_SIZE_CACHE`) using `applicationContext` to prevent `Activity` leaks and avoid JNI `measureText` calls during 120 Hz `onDraw`. |
| `RewardTilePainter.java` | Zero-allocation tile rendering. Reuses pre-allocated `FLOW_PATH`, `SHADER_CACHE`, `RAINBOW_SHADERS`, and `RewardFonts.tileGlyphSize`. |
| `LootTier.java` & `TierRoller.java` | Rarest-first tier roll (`1%` Gold, `4%` Purple, `25%` Green, `70%` Gray; total weight = `100`). |
| `LootboxAuthoritativeService.java` | `SecureRandom` roll + single-use HMAC-SHA256 signed tickets bound to `profileId` (constant-time signature verification). |
| `RewardInventory.java` | Profile-scoped unlocks (`available` for colors, `availableAll` for all kinds, `restoreFromCloud` for sync). `isDeveloper` must always return `false`. |
| `SecureStorageEngine.java` | AES-256-GCM encryption backed by AndroidKeyStore with AAD binding (`prefsName:keyName`). Never change `KEY_ALIAS` or `PREFIX`. |
| `LootboxActivity.java` & `PlayerCustomizationActivity.java` | 120 Hz reel spin UI and player cosmetics editor (`48 dp` minimum touch targets, ASD-STE100 text). |

---

## 3. Verification Checklist

1. Every new font `.ttf` in `Android/app/src/main/assets/fonts/` has a matching license file in `Android/font-licenses/<name>-OFL.txt`.
2. Animated rewards in `RewardCatalog` have exactly 7 colors (6 stops + the first color repeated at index 6).
3. Update and pass `RewardCatalogTest`, `TierRollerTest`, `ReelTimingTest`, `ReelLandingTest`, and `SecurityAndAuthoritativeLootboxTest`.
