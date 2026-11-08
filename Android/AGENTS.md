# AGENTS.md — Android

These rules apply to `Android/` and its subdirectories. They add to the root [AGENTS.md](../AGENTS.md).

## 1. Build configuration

| Item | Value |
|---|---|
| Modules | `:app` (application), `:lootbox` (library) |
| Package | `pl.maciek.squashscoreboard` (lootbox: `pl.maciek.squashscoreboard.lootbox`) |
| SDK | `minSdk 26`, `targetSdk 35`, `compileSdk 35` |
| Java | Source and target 11 |
| AGP | 8.7.3 (root `build.gradle`) |
| Dependencies | None at runtime. JUnit 4.13.2 for tests only. |
| UI toolkit | Android framework views, built in Java code. No XML layouts, no AndroidX, no Compose. |
| Release | R8 minify and resource shrink are on. See `app/proguard-rules.pro`. |

> [!IMPORTANT]
> Do not add a dependency (AndroidX, Compose, Kotlin, Room, Retrofit, and others) without a request.
> The app has 0 runtime dependencies. This keeps the attack surface and the APK size small.

`design/07-compose-theme/*.kt` is a reference only. It is not compiled.

## 2. Architecture

The app uses a simple layered structure. Keep the layers separate.

| Layer | Classes | Rules |
|---|---|---|
| Screens (UI) | `MainActivity`, `HistoryActivity` (Single Hub), `PlayersPageView`, `SettingsDialog`, `MatchDetailActivity`, `PlayerStatsActivity` (forwarder), `PlayerDetailActivity`, `PlayerComparisonActivity`, `PlayerCustomizationActivity`, `ChartFullscreenActivity` | Build views in code. Read data from the stores. Do not put score logic or storage logic here. |
| Custom views | `ScoreChartView`, `SetProgressChartView`, `SetsChartView`, `CourtSideDiagramView`, `StatComparisonRowView`, `MatchStatsView`, `MatchFinishedView`, `CourtAlertView`, `FireworksView`, `ChartPager` | No allocation and no I/O in `onDraw`. Respect the reduce-motion setting. Use $O(\log N)$ `ChartUtils` binary search for point inspection and $O(1)$ `MatchRecord` prefix lookups. |
| Theme | `UiTheme` | The only source of colors, sizes, bottom navigation bar (`BottomNavBar`), chart legends (`createChartLegend`), and shared controls. Do not hardcode new colors in screens. |
| Domain logic | `MatchState`, `PlayerStatistics`, `MatchRecord`, `ChartUtils` | Pure Java. No Android types. Deterministic. Unit-tested. `MatchRecord` precomputes $O(1)$ point/set/serve/side prefix tables and cached single-pass `MatchRecord.Analytics`; `PlayerStatistics.calculateAll` aggregates in $O(P + M \cdot N)$ time. |
| Validation | `InputValidator` | All untrusted input goes through this class. |
| Repositories | `ProfileStore`, `MatchHistoryStore`, `CloudConfigStore`, `AppPreferences` | In-memory cache for reads. Encrypted, asynchronous writes for personal and game data. |
| Infrastructure | `AsyncRepositoryExecutor`, `SafeLog`, `WatchCommandServer`, `BluetoothWatchServer`, `SupabaseSyncClient` | Background I/O, safe logs, loopback Zepp bridge, Bluetooth RFCOMM watch bridge, HTTPS PostgREST cloud sync (see `../supabase/schema.sql` golden standard). |
| Lootbox module | see [lootbox/AGENTS.md](lootbox/AGENTS.md) | The app depends on the module. The module must not depend on the app. |

### 2.1 Data flow

1. An input event (tap, remote key, Zepp bridge, or Bluetooth watch command) calls `MainActivity.addPoint`.
2. `MatchState` changes the score. `MainActivity` records the point time and the game duration.
3. `refreshScreen()` updates the views and broadcasts the latest `STATE` JSON line asynchronously through `BluetoothWatchServer`.
4. At the end of a match, or at "Start new match", `archiveCurrentMatch()` writes a `MatchRecord` to `MatchHistoryStore`, notifies `BluetoothWatchServer` (`MATCH_ENDED`), and triggers `SupabaseSyncClient.syncAllAsync`.
5. The store updates its cache at once. It writes the encrypted JSON on the `dropshot-repo-io` thread.

### 2.2 Persistent data

| Data | Store | File (`SharedPreferences`) | Encrypted |
|---|---|---|---|
| Player profiles | `ProfileStore` | `profiles` / `items` | Yes |
| Match history (max. 100) | `MatchHistoryStore` | `match_history` / `records` | Yes |
| Cloud config and tokens | `CloudConfigStore` | `cloud_config` / `supabase_url`, `supabase_anon_key`, `token_<profileId>` | Yes |
| Reward unlocks | `RewardInventory` | `reward_inventory` / `encrypted_unlocks_v2` | Yes |
| Settings (theme, rules, keys, court timer) | `AppPreferences` | `settings` | No (no personal data) |

Rules for persistent data:

1. Keep the JSON keys stable. Old keys must still load (backward compatibility).
2. Use `opt*` methods with safe defaults when you read JSON.
3. Clamp all numbers with `InputValidator` when you build a `MatchRecord`.
4. A player color is stored as an ARGB `int`. It is the first color of a `RewardCatalog.Reward`. Do not change existing keys.
5. Do not add personal data or secret tokens to `AppPreferences`. Use `SecureStorageEngine`.

## 3. Rules for Java code

1. Make classes `final` and constructors `private` for utility classes.
2. Make entity classes immutable (`public final` fields, defensive copies of lists and arrays).
3. Check arguments at public boundaries. Return a safe default, or throw `IllegalArgumentException`.
4. Do not catch `Exception` without a reason. If you ignore an exception, write why in a comment.
5. Do not use `System.currentTimeMillis()` for IDs. Use `InputValidator.generateSecureMatchId()` or `generateProfileUuid()`.
6. Use `SystemClock.elapsedRealtime()` for durations. Use `System.currentTimeMillis()` only for wall-clock dates.
7. Use `Locale.US` for machine formats. Use `Locale.getDefault()` only for text that the user reads.
8. Stop each `Handler` callback, `ValueAnimator`, and server in the matching lifecycle method (`onPause`, `onDestroy`).
9. Set `setFilterTouchesWhenObscured(true)` on each new button that changes data.

## 4. Rules for UI changes

1. Read the screen specification in [design/05-screens.md](design/05-screens.md) and [../docs/agents/architecture.md](../docs/agents/architecture.md) before you change a screen.
2. Use the Single Hub pattern in `HistoryActivity`: toggle `HistoryView` and `PlayersPageView` in memory with `View.VISIBLE` and `View.GONE`. Never call `finish()` to switch between History and Players.
3. Open settings with `SettingsDialog.show(activity, initialTab, onSaveCallback)` on the current activity. Do not launch `MainActivity` to show settings.
4. Use `UiTheme` controls: `primaryButton`, `tonalButton`, `outlinedButton`, `dangerButton`, `editField`, `styledSpinner`.
5. Use the theme colors (`theme.onSurface`, `theme.primary`, …). For a player color, use `playerPanelFill` and `playerAccentOnSurface`.
6. Give each interactive view a minimum size of 48 dp × 48 dp.
7. Give each icon-only control a `contentDescription` in STE.
8. Check the light theme, the dark theme, and the high-contrast mode.
9. Check portrait and landscape. Activities handle `orientation|screenSize` themselves and rebuild the view.
10. Use `UiTheme.isReduceMotionEnabled` before you start an animation.
11. Keep `README.md`, `Android/README.md`, `Android/AGENTS.md`, and `../docs/agents/` updated whenever screens, stores, or flows change.

Full rules: [../docs/agents/ui-ux.md](../docs/agents/ui-ux.md).

## 5. Tests

| Module | Test class | Scope |
|---|---|---|
| `:app` | `SecurityAndConcurrencyTest` | Input validation, cosmetic font ID sanitization, IDs, bridge host and token checks, `MatchState`, scalable stats, cloud schema fallback |
| `:lootbox` | `RewardCatalogTest`, `TierRollerTest`, `ReelTimingTest`, `ReelLandingTest`, `SecurityAndAuthoritativeLootboxTest` | Catalog integrity and $O(1)$ lookups, roll weights, reel timing, ticket security, AES-GCM |

1. Write JVM unit tests (`src/test/java`). There are no instrumented tests.
2. In JVM tests, `android.*` classes are stubs. `Context` is `null`. Design logic so that it can run without Android.
3. `SecureStorageEngine` uses an ephemeral AES key in JVM tests.
4. Name a test for the behavior: `rejectsTamperedHmacSignature`, not `test1`.

## 6. Android caveats

1. `MainActivity.java` has more than 1,000 lines. Do not add more responsibilities. Extract new logic into a separate class (like `SettingsDialog` and `PlayersPageView`).
2. `PlayerStatsActivity.java` is a compatibility forwarder to `HistoryActivity` (`EXTRA_ACTIVE_TAB = TAB_PLAYERS`). Do not delete it.
3. The stores use static caches with `volatile boolean initialized`. Data that another process writes is not reloaded.
4. An asynchronous write can be lost if the process stops at once after the change.
5. The Settings color list (`UiTheme.playerColors`) shows only the 4 starter colors (profile `null`). Profile colors come from `PlayerCustomizationActivity`.
6. `colorIndex()` returns `0` (Blue) for a color that is not in the list. This can change a guest color silently.
7. `MatchRecord` uses `AppPreferences.BLUE` and `RED` as default colors for old records.

More caveats: [../docs/agents/caveats.md](../docs/agents/caveats.md).
