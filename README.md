# DropShot — Squash Scoreboard

DropShot is a squash scoreboard app for Android phones and tablets.
It works with a touch screen, a Bluetooth remote control, an Android Watch, and an Amazfit watch.

---

## 1. Purpose of the app

Players put a phone or a tablet on a stand outside the squash court.
The app shows the score in large digits that are easy to read through the court glass.
Players can add points from the court with a Bluetooth remote control or a watch.
After the match, players can review their history, charts, and statistics.

---

## 2. General use cases

| Use case | How DropShot helps |
|---|---|
| **Score a match from inside the court** | Press a key on a Bluetooth remote control, or tap an Android Watch or Amazfit watch to score. |
| **Score a match on the screen** | Put the phone outside the court. Tap the left or right player panel to add 1 point. |
| **Track the court rental time** | Start the court timer on the Match screen. The border changes color and vibrates at 30 min, 15 min, and 5 min. |
| **Review past matches and charts** | Open the History screen to see finished matches, timelines, and game progress charts. |
| **Compare 2 players** | Open the Players screen to see career win rates, averages, head-to-head records, and `6-7` / `6-9` counts. |
| **Customize and sync player colors** | Roll the squash lootbox to unlock solid, gradient, and animated colors. Sync rewards with the cloud. |

---

## 3. Screen map and where to find each feature

Tap the `⌃` button at the bottom of the screen to open the navigation bar.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                             NAVIGATION BAR                              │
│         [ Match ]        [ History ]        [ Players ]    [ Settings ] │
└────────────┬──────────────────┬──────────────────┬──────────────┬───────┘
             │                  │                  │              │
             ▼                  ▼                  ▼              ▼
   ┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐ ┌───────────────────┐
   │   Match screen   │ │History screen│ │  Players screen  │ │  Settings dialog  │
   ├──────────────────┤ ├──────────────┤ ├──────────────────┤ ├───────────────────┤
   │ • Score panels   │ │ • Search box │ │ • Player list    │ │ • Players & rules │
   │ • Court timer    │ │ • All /      │ │ • Add player     │ │ • Appearance      │
   │ • Pause / Resume │ │   Finished / │ │ • Compare        │ │ • Remote keys     │
   │ • Undo point     │ │   Unfinished │ │   players        │ │ • Court & help    │
   │ • Serve side     │ │ • Resume     │ │ • Open lootbox   │ └───────────────────┘
   │ • Start new      │ │   match      │ │ • Sync cloud     │
   │   match          │ │ • Match      │ │ • Color editor   │
   └──────────────────┘ │   charts     │ └──────────────────┘
                        └──────────────┘
```

### Feature location guide

| Feature | Screen | How to open it |
|---|---|---|
| **Add 1 point** | Match | Tap Player 1 or Player 2, press the remote key, or tap the watch tile. |
| **Undo the last point** | Match | Tap `↶` in the top bar, press Undo on the remote, or tap `UNDO` on the watch. |
| **Toggle serve side** | Match / Watch | Tap the server panel on the phone, or tap `[R]` / `[L]` on the watch. |
| **Pause or resume match** | Match | Tap `Ⅱ` / `▶` in the top bar, or tap the pause overlay. |
| **Start a new match** | Match | Tap `↺` in the top bar, or hold `RESET` on the watch. |
| **Start or stop court timer** | Match | Tap **Start court timer** in the top-left corner. |
| **Change court timer warnings** | Settings → Court & help | Hold **Start court timer** on the Match screen, or open **Settings → Court & help**. |
| **Select profiles and rules** | Settings → Players & rules | Hold a player panel on the Match screen, or open **Settings → Players & rules**. |
| **Switch dark theme or contrast** | Settings → Appearance | Open **Settings** and tap **Appearance**. |
| **Change Bluetooth remote keys** | Settings → Remote keys | Open **Settings** and tap **Remote keys**. |
| **View or resume saved matches** | History | Open the navigation bar and tap **History**. |
| **View match charts and stats** | History → Match detail | Tap any match row in the **History** list. |
| **Add a player profile** | Players | Open **Players** and tap **Add player**. |
| **Compare 2 players** | Players | Open **Players** and tap **Compare players**. |
| **Unlock new player colors** | Players → Lootbox | Open **Players** and tap **Open lootbox prototype**. |
| **Sync cosmetics to cloud** | Players | Open **Players** and tap **Sync cloud**. |

---

## 4. How the main features work

### 4.1 Match score and squash rules

- By default, a player wins a game at **11 points** with a **2-point lead**.
- By default, a player wins the match at **3 games**.
- You can change both numbers in **Settings → Players & rules**.
- The active server shows a highlight border and serve side (`[R]` or `[L]`).
- When 1 point wins a game or the match, the top bar shows **GAME BALL** or **MATCH BALL**.
- When the score reaches `6-7`, `7-6`, `6-9`, or `9-6`, the screen shows animations.

### 4.2 Court timer

- Tap **Start court timer** on the Match screen.
- Default duration is **60 min**.
- Warnings show at **30 min** (green), **15 min** (gold), and **5 min** (red) before rental ends.

### 4.3 Bluetooth remote control

> [!WARNING]
> Volume keys change the score only while the Match screen is open.

1. Pair the Bluetooth remote control in Android settings.
2. Open the Match screen in DropShot.
3. Default keys:
   - `Volume +`: Add 1 point to Player 1
   - `Volume -`: Add 1 point to Player 2
   - `Pause`: Undo last point
   - `Play`: Start new match

### 4.4 Watch apps

#### 4.4.1 Android Watch (Wear OS / LG G Watch)

The `AndroidWatch/` module provides a standalone remote for Android Wear watches (API 20+):
- **75/25 screen split**: Top 75% displays the two player tiles. Bottom 25% holds the `UNDO` and `RESET` buttons.
- **Score tiles**: Tap a tile to add 1 point.
- **Serve side badge (`[R]` / `[L]`)**: Tap directly to toggle the serve side. It never triggers a point.
- **Cosmetics**: Displays unlocked player colors and gradients (Purple and Living Rainbow) on the score.
- **Reactions**: Shows quick graphic animations when the score reaches `6-7` (Cat) or `6-9` (Shrek).
- **Swipe to exit**: Swipe right from the left screen edge to exit smoothly to the watch face.
- **Status dot**: A micro dot in the top notch shows Bluetooth status. Tap to reconnect.

#### 4.4.2 Amazfit watch (Zepp OS)

The `ZeppOS/` directory provides 2 watch apps:
- **Punkty Squash** (`ZeppOS/`): Standalone 2 × 2 button remote control.
- **Punkty Squash Trening** (`ZeppOS/workout-extension/`): Workout page inside the squash workout mode.

| Watch button | Action |
|---|---|
| `+1` (blue) | Add 1 point to Player 1 |
| `+2` (red) | Add 1 point to Player 2 |
| `<<` (gray) | Undo the last point |
| `R` (green) | Start a new match |

### 4.5 Match history and charts

- Saves up to **100 matches** automatically.
- Filter by **All**, **Finished**, or **Unfinished**.
- Tap **Resume match** on an unfinished match to continue play.
- Match details include 4 tabs: point timeline, game progress, games summary, and match statistics.

### 4.6 Player profiles, statistics, and cosmetics

- Tracks career matches, wins, win rates, averages, and special score counts.
- Unlock extra colors through the squash lootbox: Green, Gray, Purple gradients, and Living Rainbow.
- Tap **Sync cloud** in the Players screen to sync unlocked cosmetics through Supabase.

---

## 5. Install and run

### Option 1: Install the release APKs

1. Open the [Releases page](https://github.com/Kollbii/DropShot/releases) on GitHub.
2. Download the APK files:
   - `DropShot-X.Y.Z.apk`: Install on your Android phone or tablet (Android 8.0+ / API 26+).
   - `DropShot-Watch-X.Y.Z.apk`: Install on your Android Watch (Android 4.4W+ / API 20+).

### Option 2: Build from source

Use the `Makefile` in the repository root:

| Task | Command |
|---|---|
| Build phone debug APK | `make build` |
| Install and run on phone | `make run` |
| Build watch debug APK | `gradle -p AndroidWatch assembleDebug` |
| Run all unit tests | `gradle -p Android testDebugUnitTest && gradle -p AndroidWatch testDebugUnitTest` |
