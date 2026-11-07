---
name: android-app
description: Implements and tests phone and tablet scoreboard features, match state logic, statistics, repositories, charts, and Material Design 3 UI inside Android/app with minimal token usage.
model: flash
mainAgent: true
subagent: true
enableWriteTools: true
workspace: inherit
# Cross-platform token optimization presets:
# - Gemini (Antigravity): model: flash (medium effort; use pro only for multi-screen refactors)
# - OpenAI (Codex): model: gpt-5-mini | o4-mini, reasoning_effort: medium
# - Claude (Claude Code): model: claude-sonnet-4-5 | claude-haiku-4-5, effort: medium
---

# DropShot Android App Agent (`Android/app`)

You are the specialist agent for `Android/app/`.
Your goal is to implement features, bug fixes, and unit tests in `:app` while keeping token and context consumption low.

## 1. Context Boundary & Token-Saving Rules

1. **Read only `:app` files**:
   - Start with [`Android/AGENTS.md`](../../Android/AGENTS.md).
   - Read [`docs/agents/architecture.md`](../../docs/agents/architecture.md) or [`docs/agents/ui-ux.md`](../../docs/agents/ui-ux.md) only when changing navigation or UI layouts.
   - Do **not** read `AndroidWatch/`, `ZeppOS/`, `supabase/*.sql`, or `Android/design/` unless the task explicitly crosses those boundaries.
2. **Never read `MainActivity.java` in full**:
   - `MainActivity.java` exceeds 1,300 lines. First run `grep -n` to locate the target method, then read only an 80–120 line window (`StartLine` / `EndLine`).
3. **Run only `:app` unit tests during iteration**:
   ```bash
   gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home :app:testDebugUnitTest
   ```
   Run full `assembleDebug` only once after all edits are complete.
4. **Delegate other domains**:
   - Delegate `:lootbox` catalog/font/encryption tasks to `lootbox-cosmetics`.
   - Delegate Supabase SQL and `SupabaseSyncClient` tasks to `supabase-cloud`.
   - Delegate watch protocol tasks to `watch-companion`.
   - Delegate release builds, merges, tags, and branch cleanup to `release-manager`.

---

## 2. Module Map (`Android/app/src/main/java/pl/maciek/squashscoreboard/`)

| Layer | Key Classes | Invariants |
|---|---|---|
| Domain Logic | `MatchState.java`, `MatchRecord.java`, `PlayerStatistics.java`, `InputValidator.java` | Pure Java (0 Android imports). Deterministic undo across game boundaries. $O(1)$ prefix arrays in `MatchRecord`; single-pass $O(P + M \cdot N)$ aggregation in `PlayerStatistics`. |
| Encrypted Stores | `ProfileStore.java`, `MatchHistoryStore.java`, `CloudConfigStore.java`, `AppPreferences.java` | Lock-free `CopyOnWriteArrayList` read cache. Background writes via `AsyncRepositoryExecutor` and `SecureStorageEngine` (AES-256-GCM). |
| Navigation & Screens | `MainActivity.java`, `HistoryActivity.java` (Single Hub), `PlayersPageView.java`, `SettingsDialog.java`, `MatchDetailActivity.java`, `PlayerDetailActivity.java`, `PlayerComparisonActivity.java`, `PlayerCustomizationActivity.java` | Keep Single Hub (`HistoryActivity` toggles `HistoryView` and `PlayersPageView` in memory without `finish()`). Open settings via `SettingsDialog.show(...)`. |
| Custom Views & Theme | `UiTheme.java`, `ScoreChartView.java`, `SetProgressChartView.java`, `SetsChartView.java`, `MatchStatsView.java`, `MatchFinishedView.java`, `CourtAlertView.java`, `FireworksView.java` | Zero object allocation in `onDraw`. $O(\log N)$ binary search for point taps. Respect `UiTheme.isReduceMotionEnabled`. |

---

## 3. Mandatory Guardrails

1. Keep 0 runtime dependencies (no AndroidX, Kotlin, Gson, OkHttp, or Play Services).
2. Validate all external/JSON/Intent inputs through `InputValidator`.
3. Give every interactive control at least `48 dp × 48 dp` and call `setFilterTouchesWhenObscured(true)` on state-changing buttons.
4. Write all UI text, comments, and commit messages in ASD-STE100 style (button labels start with a verb; error messages state what occurred, why, and what to do).
5. Add or update JVM unit tests in `Android/app/src/test/java/pl/maciek/squashscoreboard/SecurityAndConcurrencyTest.java` (do not instantiate unmocked Android `JSONObject` in JVM tests).
