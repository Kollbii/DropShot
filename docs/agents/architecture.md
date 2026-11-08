# Architecture Guide

This document describes the application architecture of DropShot for AI agents.

---

## 1. System Overview

DropShot is a squash scoreboard application. It contains 3 primary sub-systems:

| Sub-system | Path | Purpose |
|---|---|---|
| Scoreboard Core | `Android/app` | Immersive match scoreboard, match history, player statistics, and cloud sync. |
| Lootbox Library | `Android/lootbox` | Reward catalog, random reward engine, and encrypted storage engine. |
| Watch Companions | `ZeppOS/` and `AndroidWatch/` | Wearable remotes for wrist-based score input during play. |

---

## 2. Activity and Screen Hierarchy

```
[MainActivity] (Scoreboard, Immersive Fullscreen)
   │
   ├── Tapping History or Players opens ──► [HistoryActivity] (Single Hub Activity)
   │                                           ├── HistoryPageView (Match List)
   │                                           └── PlayersPageView (Player Profiles)
   │
   ├── [MatchDetailActivity] (Unified 4-page swipeable ChartPager + top-bar view spinner for Point chart, Game chart, Game scores, and MatchStatsView; ChartFullscreenActivity is a compatibility subclass)
   ├── [PlayerDetailActivity] (Career stats with single-player StatComparisonRowView progress bars, CourtSideDiagramView [L]/[R] map, cloud link, customization link)
   ├── [PlayerComparisonActivity] (Side-by-side dual-player CourtSideDiagramView + head-to-head StatComparisonRowView bars)
   ├── [PlayerCustomizationActivity] (Cosmetic player color and bundled font selection)
   └── [LootboxActivity] (Interactive reel roll for cosmetic rewards)
```

---

## 3. The Single Hub Activity Pattern

[`HistoryActivity.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/HistoryActivity.java) is the single Hub Activity for content browsing.

### 3.1 Why the Hub Pattern Exists
1. [`MainActivity.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) uses immersive sticky full-screen mode. It hides system bars.
2. Browsing activities show system bars.
3. If an agent switches activities between History and Players with `finish()`, Android briefly exposes [`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java).
4. This causes visible screen flicker and layout jumps.
5. In the Hub pattern, [`HistoryActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/HistoryActivity.java) hosts both `HistoryView` and [`PlayersPageView`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/PlayersPageView.java).
6. Tapping tabs toggles `View.VISIBLE` and `View.GONE` in memory.
7. Zero activities finish or start during tab switches. The switch takes 0 ms.

### 3.2 Compatibility Forwarder
[`PlayerStatsActivity.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/PlayerStatsActivity.java) forwards external intents to [`HistoryActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/HistoryActivity.java) with `EXTRA_ACTIVE_TAB = TAB_PLAYERS`.
Do not delete [`PlayerStatsActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/PlayerStatsActivity.java). It keeps backward compatibility with external intents.

---

## 4. Navigation Bar Architecture

[`UiTheme.BottomNavBar`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/UiTheme.java#L450-L545) provides 4 destinations:
1. `Match` (Tab 0)
2. `History` (Tab 1)
3. `Players` (Tab 2)
4. `Settings` (Tab 3)

### 4.1 Navigation Modes
* **Scoreboard Mode ([`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java))**:
  * During live play, the navigation bar is hidden. This prevents accidental taps.
  * When paused or before starting, the bar is visible.
* **Hub Mode ([`HistoryActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/HistoryActivity.java))**:
  * The navigation bar is permanently visible.
  * Uses a [`BottomNavListener`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/UiTheme.java#L440-L443) to swap views in place.
  * `setActiveTab(tab)` updates the pill highlight dynamically.

---

## 5. Modal Settings Architecture

[`SettingsDialog.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SettingsDialog.java) manages all application settings in a shared dialog:
* Tab 0: Players and Rules (player selection, points per game, games per match).
* Tab 1: Court Timer (rental duration, color warning thresholds).
* Tab 2: Remote and Theme (watch keys, dark theme, high contrast).
* Tab 3: Help Guide (rules and controls).

Show the settings dialog locally in any activity:
```java
SettingsDialog.show(activity, initialTab, onSaveCallback);
```
Do not launch [`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) to show settings. Keep the user on their current screen.
