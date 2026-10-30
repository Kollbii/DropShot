# DropShot — Squash Scoreboard

DropShot is a squash scoreboard app for Android phones and tablets.
It works with a touch screen, a Bluetooth remote control, and an Amazfit (Zepp OS) watch.

---

## 1. Purpose of the app

Players put a phone or a tablet on a stand outside the squash court.
The app shows the current score in large digits that are easy to read through the court glass.
Players can add points from the court with a Bluetooth remote control or a watch, and check their match history and statistics after play.

---

## 2. General use cases

| Use case | How DropShot helps |
|---|---|
| **Score a match from inside the court** | Press a key on a Bluetooth remote control or tap the Amazfit watch to add 1 point, undo 1 point, or start a new match. |
| **Score a match on the screen** | Put the phone or tablet outside the court. Tap the left or right player panel to add 1 point. |
| **Track the court rental time** | Start the court timer on the Match screen. The border changes color and the phone vibrates at 30 min, 15 min, and 5 min before the rental ends. |
| **Review past matches and charts** | Open the History screen to see finished and unfinished matches, point timelines, and game progress charts. |
| **Compare 2 players** | Open the Players screen to see career win rates, point averages, head-to-head scores, and special point counts (`6-7` and `6-9`). |
| **Customize player colors** | Open the Players screen to roll the squash lootbox and equip solid, gradient, or animated colors for each profile. |

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
   │ • Start new      │ │ • Resume     │ │ • Open lootbox   │ └───────────────────┘
   │   match          │ │   match      │ │ • Player detail  │
   └──────────────────┘ │ • Match      │ │   & color editor │
                        │   charts     │ └──────────────────┘
                        └──────────────┘
```

### Feature location guide

| Feature | Screen | How to open it |
|---|---|---|
| **Add 1 point** | Match | Tap the panel for Player 1 or Player 2, press the remote key, or tap `+1` / `+2` on the watch. |
| **Undo the last point** | Match | Tap `↶` in the top bar, press the Undo key on the remote control, or tap `<<` on the watch. |
| **Pause or resume the match** | Match | Tap `Ⅱ` / `▶` in the top bar, or tap the pause overlay. |
| **Start a new match** | Match | Tap `↺` in the top bar, or tap `R` on the watch. |
| **Start or stop the court timer** | Match | Tap **Start court timer** in the top-left corner. |
| **Change the court timer warnings** | Settings → Court & help | Hold **Start court timer** on the Match screen, or open **Settings** and tap **Court & help**. |
| **Select player profiles and rules** | Settings → Players & rules | Hold a player panel on the Match screen, or open **Settings** from the navigation bar. |
| **Switch dark theme or high contrast** | Settings → Appearance | Open **Settings** and tap **Appearance**. |
| **Change Bluetooth remote keys** | Settings → Remote keys | Open **Settings** and tap **Remote keys**. |
| **Read the remote and watch guide** | Settings → Court & help | Open **Settings**, tap **Court & help**, and tap **Open help and remote guide**. |
| **View or resume saved matches** | History | Tap `⌃` at the bottom of the screen and tap **History**. |
| **View match charts and statistics** | History → Match detail | Tap any match row in the **History** list. Tap a chart to open it in fullscreen. |
| **Add a player profile** | Players | Open **Players** from the navigation bar and tap **Add player**. |
| **Compare 2 players (head-to-head)** | Players | Open **Players** and tap **Compare players**. |
| **Edit a player name or color** | Players → Player detail | Tap a player in **Players**, then tap **Edit player**. |
| **Unlock new player colors** | Players → Lootbox | Open **Players**, tap **Open lootbox prototype**, and select a player. |

---

## 4. How the main features work

### 4.1 Match score and squash rules

- By default, a player wins a game at **11 points** with a **2-point lead**.
- By default, a player wins the match at **3 games**.
- You can change both numbers in **Settings → Players & rules**.
- When 1 more point wins a game or the match, the top bar shows **GAME BALL** or **MATCH BALL** and the player panel border pulses.
- When the score reaches `6-7`, `7-6`, `6-9`, or `9-6`, the screen shows a fireworks effect.
- When a match finishes, you can start a new match, add 3 more games to continue the match, or open the match details.

### 4.2 Court timer

- Tap **Start court timer** on the Match screen. The app sets the start time to the current full hour.
- Default rental duration is **60 min**.
- Default warnings appear at:
  - **30 min** before the end (green border, 1 vibration pulse).
  - **15 min** before the end (gold border, 2 vibration pulses).
  - **5 min** before the end (red border, 3 vibration pulses).

### 4.3 Bluetooth remote control

> [!WARNING]
> The volume keys change the score only while the Match screen is open.

1. Pair the Bluetooth remote control in the Android settings.
2. Open the Match screen in DropShot.
3. Use the default keys (or change them in **Settings → Remote keys**):

| Remote key | Default action |
|---|---|
| `Volume +` | Add 1 point to Player 1 |
| `Volume -` | Add 1 point to Player 2 |
| `Pause` | Undo the last point |
| `Play` | Start a new match |

### 4.4 Amazfit watch (Zepp OS)

> [!WARNING]
> If the watch loses the Bluetooth connection, the watch does not send the command again. This rule prevents a late point after reconnection.

DropShot includes 2 watch apps in `ZeppOS/`:
- **Punkty Squash** (`ZeppOS/`): A standalone 2 × 2 button remote control for the watch.
- **Punkty Squash Trening** (`ZeppOS/workout-extension/`): A workout data page that you can add inside the Squash workout mode on the watch.

| Watch button | Action |
|---|---|
| `+1` (blue) | Add 1 point to Player 1 |
| `+2` (red) | Add 1 point to Player 2 |
| `<<` (gray) | Undo the last point |
| `R` (green) | Start a new match |

### 4.5 Match history and charts

The app saves up to **100 matches** automatically.
- You can search matches by player name, date, or score.
- You can filter the list by **All**, **Finished**, or **Unfinished**.
- Tap **Resume match** on any unfinished match to continue play immediately.
- Tap a match row to see 4 detail pages:
  1. **Point timeline chart**: Shows the point difference across the match.
  2. **Game progress chart**: Shows point-by-point progress inside each game.
  3. **Games summary chart**: Shows the points and duration of each game.
  4. **Match statistics**: Shows streaks, game balls, durations, and point shares.

### 4.6 Player profiles, statistics, and cosmetics

- Each profile tracks total matches, wins, win rate, total points, average points per match, average points per game, and special `6-7` / `6-9` points.
- Every player starts with 4 colors: **Blue**, **Red**, **Green**, and **Gold**.
- Players can open lootboxes to unlock extra solid colors (Green and Gray tiers), gradients (Purple tier), and the animated **Living Rainbow** (Gold tier).

---

## 5. Install and run

### Option 1: Install the release APK on an Android device

1. Open the [Releases page](https://github.com/Kollbii/DropShot/releases) on GitHub.
2. Download the `.apk` file from the latest release.
3. Open the `.apk` file on your Android phone or tablet (Android 8.0 / API 26 or newer) to install DropShot.

### Option 2: Build from source

Use the `Makefile` in the repository root:

| Task | Command |
|---|---|
| Build the debug APK | `make build` |
| Install and start on a connected device | `make run` |
| Start the Android emulator and run the app | `make start` |
| Check SDK, JDK, and device status | `make status` |
| Stop the emulator and background daemons | `make stop` |
