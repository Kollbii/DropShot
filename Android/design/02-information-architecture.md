# Deliverable 2: Information Architecture

## 1. Navigation Architecture and Bottom Navigation Bar

DropShot uses **4 top-level destinations** (the maximum allowed is 5 items):

| Index | Label | Icon (Unselected / Selected) | Destination Screen | Purpose |
|---|---|---|---|---|
| 1 | **Match** | Outlined / Filled scoreboard icon | Match Screen (Home) | Keep the score of the current squash match and run the court timer. |
| 2 | **History** | Outlined / Filled history clock icon | Match History List Screen | View, search, filter, delete, or resume saved matches (up to 100 records). |
| 3 | **Players** | Outlined / Filled group icon | Players and Statistics Screen | Manage player profiles, view win rates, and compare 2 players. |
| 4 | **Settings** | Outlined / Filled gear icon | Settings Screen | Configure match rules, remote keys, court timer warnings, theme, account backup, and help. |

### Adaptive Navigation Behavior across Window Size Classes
- **Compact width in Portrait (`< 600 dp`):** A Material 3 `NavigationBar` (bottom navigation bar, height `80 dp`) stays at the bottom of the screen.
- **Landscape during an active match (Compact height `< 480 dp` or Medium/Expanded width):** To give the two score panels maximum height and prevent accidental navigation taps from across the court, the 4 destinations are placed in a compact `NavigationRail` (`80 dp` wide) on the start edge, or opened from the top bar when the match is paused.
- **Medium and Expanded width outside an active match (`>= 600 dp`):** A Material 3 `NavigationRail` on the start edge shows the same 4 items with their text labels.

---

## 2. Information Architecture Text Tree

```text
DropShot (Android App)
├── 0.0 Launch & First-Run Flow
│   ├── 0.1 Splash Screen (System SplashScreen API)
│   └── 0.2 Onboarding Flow (First launch only, max 3 screens)
│       ├── 0.2.1 Screen 1: Score Every Point on the Court
│       ├── 0.2.2 Screen 2: Use a Remote Control or a Watch
│       └── 0.2.3 Screen 3: Track Court Time and Statistics
│
├── 1.0 Match (Tab 1 - Home / Main Task 1 & Task 2)
│   ├── 1.1 Active Scoreboard Canvas
│   │   ├── 1.1.1 Top Status Bar (Match Clock, Game/Match Ball Status, Court Timer Chip)
│   │   ├── 1.1.2 Top Action Bar (Pause/Resume, Undo Point, Set Up Match, New Match)
│   │   ├── 1.1.3 Player 1 Score Panel (Name, Games Won, Current Game Points, Tap to Add Point)
│   │   ├── 1.1.4 Player 2 Score Panel (Name, Games Won, Current Game Points, Tap to Add Point)
│   │   └── 1.1.5 Court Warning Frame Overlay (Green / Amber / Red border + text badge)
│   ├── 1.2 Match Setup Sheet / Dialog (Main Task 2)
│   │   ├── 1.2.1 Player 1 Profile Selector & Color Picker
│   │   ├── 1.2.2 Player 2 Profile Selector & Color Picker
│   │   ├── 1.2.3 Match Rules Selector (Points to Win Game, Games to Win Match)
│   │   └── 1.2.4 Action: Start New Match
│   ├── 1.3 Court Timer Dialog
│   │   ├── 1.3.1 Action: Reset Court Timer to Current Full Hour
│   │   ├── 1.3.2 Action: Stop Court Timer
│   │   └── 1.3.3 Link: Open Court Timer Settings
│   ├── 1.4 New Match Confirmation Dialog
│   └── 1.5 Match Finished Overlay / Dialog
│       ├── 1.5.1 Action: Show Match Board (Return to final score)
│       ├── 1.5.2 Action: Start New Match
│       ├── 1.5.3 Action: Add 3 Games (Continue match)
│       └── 1.5.4 Action: Show Match Details
│
├── 2.0 History (Tab 2 - Main Task 3: Match List, Detail, and Search)
│   ├── 2.1 Match History List Screen
│   │   ├── 2.1.1 Search Bar Trigger ("Search matches")
│   │   ├── 2.1.2 Status Filter Chips (All matches, Finished, Unfinished)
│   │   ├── 2.1.3 Match Record Cards (Players, Game Score, Status, Duration, Date)
│   │   └── 2.1.4 Delete Match Confirmation Dialog
│   ├── 2.2 Search Screen (Full-screen M3 Search View)
│   │   ├── 2.2.1 Query Input Field (Search by player name, score, or date)
│   │   ├── 2.2.2 Filter Chips (Finished, Unfinished, Player filter)
│   │   └── 2.2.3 Matching Match Cards & Matching Player Profiles
│   └── 2.3 Match Detail Screen
│       ├── 2.3.1 Match Summary Card (Winner, Game Scores, Match Duration, Game Durations)
│       ├── 2.3.2 Primary Action: Resume Match (if unfinished) or Open Match on Board (if finished)
│       ├── 2.3.3 Chart Pager
│       │   ├── Page 1: Point Timeline Chart (Points over rally count / time)
│       │   ├── Page 2: Game Progress Chart (Per-game point progression, filterable by game)
│       │   └── Page 3: Match Statistics Table (Points won, Point share %, Games won, Longest streak)
│       └── 2.4 Fullscreen Chart Screen (Pinch to zoom, drag to pan, tap point for timestamp)
│
├── 3.0 Players (Tab 3 - Main Task 3: Profiles, Statistics, and Comparison)
│   ├── 3.1 Players and Statistics List Screen
│   │   ├── 3.1.1 Player Summary Table (Player, Matches, Wins, Win Rate %, Points, Average pts/match)
│   │   ├── 3.1.2 Action: Add Player (Opens Add Player Dialog)
│   │   └── 3.1.3 Action: Compare Players (Opens Compare Players Dialog)
│   ├── 3.2 Player Detail Screen (Profile Screen)
│   │   ├── 3.2.1 Player Identity Header (Name, Color Badge, Color Label)
│   │   ├── 3.2.2 Career Statistics Cards (Matches, Wins, Win Rate, Total Points, Averages, 6-7 / 6-9 points)
│   │   ├── 3.2.3 Action: Edit Player (Opens Edit Player Dialog)
│   │   └── 3.2.4 Action: Delete Player (Opens Delete Player Confirmation Dialog)
│   └── 3.3 Player Comparison Screen
│       ├── 3.3.1 Side-by-Side Comparison Table (Player 1 vs Player 2 metrics)
│       └── 3.3.2 Head-to-Head Summary Banner (Direct wins and matches played)
│
└── 4.0 Settings (Tab 4 - Configuration, Sign-In, and Help)
    ├── 4.1 Settings Screen
    │   ├── 4.1.1 Default Match Rules Section (Points per game, Games to win match)
    │   ├── 4.1.2 Bluetooth Remote Control Section (Player 1 key, Player 2 key, Undo key, Reset key)
    │   ├── 4.1.3 Court Timer Section (Rental duration in min, Green/Yellow/Red warning min, Vibration switch)
    │   ├── 4.1.4 Appearance Section (Dark theme switch, High-contrast mode switch)
    │   ├── 4.1.5 Account and Cloud Backup Section -> Opens 4.2 Sign-In Screen
    │   └── 4.1.6 Support Section -> Opens 4.3 Help Screen
    ├── 4.2 Sign-In and Cloud Backup Screen
    │   ├── 4.2.1 Signed-Out State (Sign in with Google, Continue without account)
    │   └── 4.2.2 Signed-In State (Sync data now, Last backup timestamp, Sign out)
    └── 4.3 Help and Remote Setup Guide Screen
        ├── 4.3.1 Section 1: How to Score on the Screen
        ├── 4.3.2 Section 2: How to Use a Bluetooth Remote Control
        ├── 4.3.3 Section 3: How to Connect the Zepp OS Watch (`127.0.0.1:28282`)
        └── 4.3.4 Section 4: How the Court Timer Works
```

---

## 3. Tap-Count Verification Table (Maximum 3 Taps Rule)

| User Task | Path from App Launch / Active Screen | Tap Count |
|---|---|---|
| **Add a point to Player 1 or Player 2** | Tap the Player 1 or Player 2 score panel (or press 1 remote/watch button). | **1 tap** |
| **Undo the last point** | Tap **Undo point** in the top app bar (or press 1 remote/watch button). | **1 tap** |
| **Pause or resume the match** | Tap **Pause match** / **Resume match** in the top app bar. | **1 tap** |
| **Start the court timer (from Off)** | Tap **Start court timer** in the top app bar. | **1 tap** |
| **Reset the court timer (when running)** | Tap the court timer button (1) -> Tap **Reset court timer** (2). | **2 taps** |
| **Start a new match with the same players** | Tap **New match** (1) -> Tap **Start new match** in the dialog (2). | **2 taps** |
| **Change a player or rule and start a match** | Tap **Set up match** (1) -> Tap the profile/rule chip (2) -> Tap **Start new match** (3). | **3 taps** |
| **Open match details and charts from history** | Tap **History** tab (1) -> Tap a match card (2). | **2 taps** |
| **Resume an unfinished match from history** | Tap **History** tab (1) -> Tap the match card (2) -> Tap **Resume match** (3). | **3 taps** |
| **Search matches by player name** | Tap **History** tab (1) -> Tap **Search matches** bar (2) -> Type or tap a filter chip (3). | **3 taps** |
| **View a player's career statistics** | Tap **Players** tab (1) -> Tap a player row (2). | **2 taps** |
| **Compare 2 players** | Tap **Players** tab (1) -> Tap **Compare players** (2) -> Tap **Show comparison** (3). | **3 taps** |
| **Open Help or Sign-In** | Tap **Settings** tab (1) -> Tap **Open help** or **Manage account** (2). | **2 taps** |

