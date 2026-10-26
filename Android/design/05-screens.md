# Deliverable 5: Screen Specifications

Every screen specification includes:
1. **Purpose of the screen**
2. **Text wireframe**
3. **List of Material 3 components**
4. **All 6 states:** `Default`, `Loading`, `Empty`, `Error`, `Offline`, and `Success`
5. **TalkBack reading order**
6. **Exact ASD-STE100 text** for every label, button, instruction, and message

---

## Screen 1: Splash Screen (`SplashActivity` / Android 12+ `SplashScreen` API)

### 1.1 Purpose
Show the DropShot brand icon while the app reads the local preferences (`AppPreferences`), player profiles (`ProfileStore`), and match history (`MatchHistoryStore`), then route the user to either the Onboarding screen (first launch) or the Match screen.

### 1.2 Text Wireframe
```text
+-------------------------------------------------------------------+
|                                                                   |
|                                                                   |
|                         +---------------+                         |
|                         |   [DropShot   |                         |
|                         |  Squash Ball  |                         |
|                         |     Logo]     |                         |
|                         +---------------+                         |
|                                                                   |
|                             DropShot                              |
|                     Squash match scoreboard                       |
|                                                                   |
|                    ( o ) Circular progress                        |
|                    Loading local match data.                      |
|                                                                   |
+-------------------------------------------------------------------+
```

### 1.3 Components
- System `SplashScreen` window background (`surface` `#16130C` in dark theme, `#FDF9F2` in light theme).
- Centered adaptive icon (`R.drawable.squash_logo`, `108 dp × 108 dp`).
- Brand title (`headlineMedium`, `28 sp`) and subtitle (`bodyMedium`, `16 sp`).
- Material 3 `CircularProgressIndicator` (`48 dp × 48 dp`) shown only if storage initialization takes longer than `300 ms`.
- Error `AlertDialog` if local storage fails to open.

### 1.4 States
- **Default:** The logo and the app name appear for up to `250 ms` while preferences load.
- **Loading:** If migration of legacy profiles takes more than `300 ms`, the circular progress indicator and the text `Loading local match data.` appear.
- **Empty:** If no preferences exist yet (`firstRun == true`), the screen routes to **Screen 2: Onboarding**.
- **Error:** If `SharedPreferences` throws a read error, an error dialog appears with the action **Reset local data** or **Retry load**.
- **Offline:** The app works 100% offline. No network call runs during splash, so the screen proceeds normally without a network connection.
- **Success:** Preferences and profiles load, and the app opens **Screen 4: Match Screen (Home)**.

### 1.5 TalkBack Reading Order
1. Image: `"DropShot logo."`
2. Heading: `"DropShot. Squash match scoreboard."`
3. Status (only in Loading state): `"Loading local match data."`
4. Error dialog (only in Error state): Dialog title -> Error message -> `"Retry load"` button -> `"Reset local data"` button.

### 1.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `splash_title` | Title | `DropShot` |
| `splash_subtitle` | Description | `Squash match scoreboard` |
| `splash_loading` | Status label | `Loading local match data.` |
| `splash_err_title` | Dialog title | `Cannot read the local data` |
| `splash_err_body` | Error message (3-part) | `The app could not read the saved settings. The storage file on the device is damaged. Tap Retry load, or tap Reset local data to start with default settings.` |
| `splash_btn_retry` | Button | `Retry load` |
| `splash_btn_reset` | Button | `Reset local data` |

---

## Screen 2: Onboarding Flow (Maximum 3 Screens)

### 2.1 Purpose
Introduce new users to the 3 core features of DropShot (screen scoring, Bluetooth remote / Zepp OS watch scoring, and the court timer) in a maximum of 3 screens, and let the user set the default court rental duration before their first match.

### 2.2 Text Wireframe (Screens 1, 2, and 3)
```text
+-------------------------------------------------------------------+
| DropShot                                       [ Skip intro ]     |
+-------------------------------------------------------------------+
|  Step 1 of 3  [==== active ====] [----] [----]                    |
|                                                                   |
|  +-------------------------------------------------------------+  |
|  |  [Illustration: Two score panels with 11 : 9 and Undo icon] |  |
|  +-------------------------------------------------------------+  |
|                                                                   |
|  Score every point on the court                                   |
|  1. Tap the Player 1 panel to add 1 point to Player 1.            |
|  2. Tap the Player 2 panel to add 1 point to Player 2.            |
|  3. Tap Undo point to remove the last point.                      |
|                                                                   |
|  (On Step 3 only: Court duration chips: [45 min] [60 min] [90 min])|
+-------------------------------------------------------------------+
|  [ Go back ]                                  [ Show next step ]  |
|                                       (Step 3: [ Start app ])     |
+-------------------------------------------------------------------+
```

### 2.3 Components
- Top app bar (`SmallTopAppBar`) with brand title and a text button **Skip intro** (`48 dp` min height).
- Horizontal step progress indicator (`LinearProgressIndicator` + step label `Step 1 of 3`, `Step 2 of 3`, `Step 3 of 3`).
- `HorizontalPager` with 3 cards (`OutlinedCard`, corner radius `16 dp`).
- On Step 3: 3 Material 3 `FilterChip` components (`45 min`, `60 min`, `90 min`) to pick the default court rental duration.
- Bottom bar with an outlined button **Show previous step** (on Steps 2 and 3) and a filled primary button **Show next step** (Steps 1 and 2) or **Start app** (Step 3).

### 2.4 States
- **Default:** Step 1 of 3 is visible with `60 min` pre-selected for Step 3.
- **Loading:** When the user taps **Start app**, the button shows an inline spinner for `< 100 ms` while saving `onboardingCompleted = true`.
- **Empty:** Not applicable; static illustrations and default `60 min` chip are always populated.
- **Error:** If saving the preference fails, a snackbar shows the 3-part error message and a **Save settings** action.
- **Offline:** Works offline without changes. Step 2 notes that the watch bridge uses `127.0.0.1:28282` on the phone and does not need a Wi-Fi network.
- **Success:** Tapping **Start app** or **Skip intro** saves the default settings and opens the Match screen with the snackbar `The default settings are saved.`

### 2.5 TalkBack Reading Order
1. Top bar heading: `"DropShotonboarding, Step 1 of 3"` (updates per step).
2. Top bar button: `"Skip intro. Button."`
3. Illustration image: Content description of the current step illustration.
4. Step title heading (`headlineSmall`).
5. Numbered instructions (`1.`, `2.`, `3.`).
6. (Step 3 only) Filter chips: `"Select 45 min"`, `"Select 60 min, selected"`, `"Select 90 min"`.
7. Bottom navigation buttons: `"Show previous step. Button"` (Steps 2-3) -> `"Show next step. Button"` (Steps 1-2) or `"Start app. Button"` (Step 3).

### 2.6 Exact Screen Text (ASD-STE100)

#### Step 1 of 3
| Element ID | Element Type | Exact Text |
|---|---|---|
| `onb_skip` | Button | `Skip intro` |
| `onb_step_1` | Label | `Step 1 of 3` |
| `onb_1_title` | Heading | `Score every point on the court` |
| `onb_1_desc` | Description | `Put the phone or the tablet in landscape mode behind the court wall.` |
| `onb_1_inst` | Numbered instructions | `1. Tap the Player 1 panel to add 1 point to Player 1.`<br>`2. Tap the Player 2 panel to add 1 point to Player 2.`<br>`3. Tap Undo point to remove the last point.`<br>`The app saves each finished game automatically.` |
| `onb_next` | Button | `Show next step` |

#### Step 2 of 3
| Element ID | Element Type | Exact Text |
|---|---|---|
| `onb_step_2` | Label | `Step 2 of 3` |
| `onb_2_title` | Heading | `Use a remote control or a watch` |
| `onb_2_desc` | Description | `You can add points from a Bluetooth remote control or from a Zepp OS watch.` |
| `onb_2_inst` | Numbered instructions | `1. Pair the Bluetooth remote control or the watch with the phone.`<br>`2. Press Volume + to add 1 point to Player 1.`<br>`3. Press Volume - to add 1 point to Player 2.`<br>`The score updates on the screen immediately.` |
| `onb_prev` | Button | `Show previous step` |
| `onb_next` | Button | `Show next step` |

#### Step 3 of 3
| Element ID | Element Type | Exact Text |
|---|---|---|
| `onb_step_3` | Label | `Step 3 of 3` |
| `onb_3_title` | Heading | `Track court time and statistics` |
| `onb_3_desc` | Description | `The court timer shows warnings at 30 min, 15 min, and 5 min before the rental ends.` |
| `onb_3_inst` | Instruction | `Select the default duration for the court timer.` |
| `onb_3_chip_45` | Filter chip | `Select 45 min` |
| `onb_3_chip_60` | Filter chip | `Select 60 min` |
| `onb_3_chip_90` | Filter chip | `Select 90 min` |
| `onb_start` | Button | `Start app` |
| `onb_success_msg` | Snackbar | `The default settings are saved.` |
| `onb_err_msg` | Error snackbar (3-part) | `The app could not save the settings. The device storage is full. Free storage space on the device and tap Save settings.` |

---

## Screen 3: Sign-In and Cloud Backup Screen (`AccountActivity` / Route `settings/account`)

### 3.1 Purpose
Let the user optionally sign in with a Google account to back up and sync their match history and player profiles across devices. All core scoring, history, and statistics features work without an account.

### 3.2 Text Wireframe
```text
+-------------------------------------------------------------------+
| [<- Go back]     Account and cloud backup                         |
+-------------------------------------------------------------------+
|  +-------------------------------------------------------------+  |
|  |  [Cloud Icon]   Status: Not signed in (Local data only)     |  |
|  |  Local records: 14 matches · 4 player profiles              |  |
|  |  You can use all match and statistics features without      |  |
|  |  an account. Sign in to save a backup of the data.          |  |
|  +-------------------------------------------------------------+  |
|                                                                   |
|  How to back up the data:                                         |
|  1. Connect the device to the network.                            |
|  2. Tap Sign in with Google and select an account.                |
|  3. Tap Sync data now to upload the matches and the profiles.     |
|  The app keeps a local copy of all data on the device.            |
|                                                                   |
|  [G] [ Sign in with Google ]        [ Continue without account ]  |
+-------------------------------------------------------------------+
```

### 3.3 Components
- Top app bar with back button (`Go back`) and title `Account and cloud backup`.
- Status `ElevatedCard` showing current account status, count of local matches and profiles, and last backup time.
- Primary filled button **Sign in with Google** (or **Sync data now** when signed in).
- Secondary outlined button **Continue without account** (or **Sign out of account** when signed in).
- `LinearProgressIndicator` during sign-in or cloud sync.

### 3.4 States
- **Default (Signed out):** Shows the local record count, the instructions, **Sign in with Google**, and **Continue without account**.
- **Default (Signed in):** Shows `Signed in as: [email]`, `Last backup: [dd.MM.yyyy HH:mm]`, **Sync data now**, and **Sign out of account**.
- **Loading:** Shows the linear progress bar and the status `Syncing match data with the cloud backup.` while buttons are disabled.
- **Empty:** If the user has `0 matches` and `0 player profiles`, the card displays `Local records: 0 matches · 0 player profiles. Play a match or add a player first.`
- **Error:** If Google sign-in or cloud sync fails, an inline error banner with an error icon shows the 3-part error message and the button **Retry sync**.
- **Offline:** When the device has no network connection, an offline banner (`Info` container with Wi-Fi-off icon) states: `The device is offline. Local matches are saved on the device. Connect to the network to sync the backup.` The **Sign in with Google** and **Sync data now** buttons are disabled.
- **Success:** After a successful sync, a success banner (`Success` container with check icon) states: `The cloud backup is complete. All matches and player profiles are saved.`

### 3.5 TalkBack Reading Order
1. Back button: `"Go back. Button."`
2. Screen title: `"Account and cloud backup. Heading."`
3. Status card: Account status, local record counts, and last backup timestamp.
4. State banner (Offline, Error, or Success, if visible).
5. Numbered instructions (`1.`, `2.`, `3.`).
6. Primary button: `"Sign in with Google. Button"` (or `"Sync data now. Button"`).
7. Secondary button: `"Continue without account. Button"` (or `"Sign out of account. Button"`).

### 3.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `acc_title` | Screen title | `Account and cloud backup` |
| `acc_status_out` | Status label | `Status: Not signed in (local data only)` |
| `acc_status_in` | Status label | `Status: Signed in as %1$s` |
| `acc_counts` | Body text | `Local records: %1$d matches · %2$d player profiles` |
| `acc_empty` | Empty text | `Local records: 0 matches · 0 player profiles. Play a match or add a player first.` |
| `acc_last_sync` | Body text | `Last backup: %1$s` |
| `acc_desc` | Description | `You can use all match and statistics features without an account. Sign in to save a backup of the data.` |
| `acc_inst` | Numbered instructions | `1. Connect the device to the network.`<br>`2. Tap Sign in with Google and select an account.`<br>`3. Tap Sync data now to upload the matches and the profiles.`<br>`The app keeps a local copy of all data on the device.` |
| `acc_loading` | Progress label | `Syncing match data with the cloud backup.` |
| `acc_offline` | Offline message | `The device is offline. Local matches are saved on the device. Connect to the network to sync the backup.` |
| `acc_err_auth` | Error message (3-part) | `The sign-in action failed. The Google service did not return a valid token. Check the network connection and tap Sign in with Google again.` |
| `acc_err_sync` | Error message (3-part) | `The cloud backup failed. The network connection stopped during the transfer. Connect the device to the network and tap Retry sync.` |
| `acc_success` | Success message | `The cloud backup is complete. All matches and player profiles are saved.` |
| `acc_btn_signin` | Button | `Sign in with Google` |
| `acc_btn_continue` | Button | `Continue without account` |
| `acc_btn_sync` | Button | `Sync data now` |
| `acc_btn_retry` | Button | `Retry sync` |
| `acc_btn_signout` | Button | `Sign out of account` |

---

## Screen 4: Home / Match Screen (`MainActivity` - Main Task 1 and Task 2)

### 4.1 Purpose
Keep the live score of a squash match (points and games), run the match clock and the court rental countdown timer, accept commands from the screen, a Bluetooth remote control, or a Zepp OS watch, and let the user set up players and rules in 3 taps or fewer.

### 4.2 Text Wireframe (Landscape Orientation - Primary Court View)
```text
+-------------------------------------------------------------------+
| [Logo] 14:08 · GAME TO 11 | MATCH TO 3   [Court: 19:00 · 0:32 OK] |
|        [II Pause match] [Undo point] [Set up match] [New match]   |
+-------------------------------------------------------------------+
| +-------------------------------+ +-----------------------------+ |
| |           PLAYER 1            | |          PLAYER 2           | |
| |           GAMES 2             | |          GAMES 1            | |
| |                               | |                             | |
| |              10               | |              8              | |
| |      [Ball] GAME BALL         | |                             | |
| |   TAP PANEL TO ADD 1 POINT    | |  TAP PANEL TO ADD 1 POINT   | |
| +-------------------------------+ +-----------------------------+ |
+-------------------------------------------------------------------+
| [Match (active)]    [History]         [Players]        [Settings] |
+-------------------------------------------------------------------+
```

### 4.3 Components
- **Top Status and Action Bar:**
  - App logo (`36 dp × 36 dp`).
  - Live match status banner (`clock` + `titleSmall`): shows elapsed time (`14:08`) and either the rule target (`GAME TO 11 | MATCH TO 3`), `PAUSED · 14:08`, `GAME BALL: [Player name]`, `MATCH BALL: [Player name]`, or `MATCH WINNER: [Player name]`.
  - Court Timer button (`OutlinedButton` with status icon and dynamic border width `2 dp` / `5 dp` / `6 dp`).
  - 4 action buttons (`48 dp × 48 dp` minimum touch targets with visible icon + tooltip/label): **Pause match** (or **Resume match**), **Undo point**, **Set up match**, and **New match**.
- **Scoreboard Canvas (2 equal-weight `ElevatedCard` panels, corner radius `16 dp`, gap `12 dp`):**
  - **Player 1 Panel (Left in landscape, Top in portrait):** Filled with the selected Player 1 color (`playerBlue.fill` `#005CBC` by default, text `#FFFFFF`, contrast `6.45:1`). Shows Player 1 name (`headlineMedium`), games won (`GAMES 2`), current game points (`scoreDisplay`, `96 sp` to `240 sp` bold tabular numerals), game/match ball badge (squash ball icon + label), and instruction footer `TAP PANEL TO ADD 1 POINT`.
  - **Player 2 Panel (Right in landscape, Bottom in portrait):** Filled with the selected Player 2 color (`playerRed.fill` `#BB152C` by default, text `#FFFFFF`, contrast `6.45:1`).
- **Court Warning Frame (`CourtAlertView`):** Translucent perimeter glow around the entire screen when the court timer reaches the green (`30 min`), yellow (`15 min`), or red (`5 min` / `0 min`) threshold.
- **Pause Banner Overlay:** Centered card shown when `paused == true`: `MATCH PAUSED · TAP RESUME MATCH TO CONTINUE`.
- **Match Setup Bottom Sheet / Dialog (Task 2):** Lets the user select Player 1 profile, Player 2 profile, player colors (`Blue`, `Red`, `Green`, `Gold`), points per game (`11`, `15`, or custom), and games to win match (`1`, `2`, `3`, `4`, `5`), and tap **Start new match**.
- **Match Finished Overlay (`MatchFinishedView`):** Full-screen high-contrast overlay shown when a player reaches the target games, with 4 clear action buttons: **Show match board**, **Start new match**, **Add 3 games**, and **Show match details**.

### 4.4 States
- **Default (Active match):** Both player panels accept taps, remote keys, and watch commands. The match clock ticks every `1 s`.
- **Loading:** Restoring a match from History shows a brief skeleton state (`< 50 ms`) while loading the point array.
- **Empty (New match at `0 : 0`):** Points are `0` and `0`, games are `0` and `0`, the match clock is at `00:00` and waits for the first point or tap, and the **Undo point** button is in the `Disabled` state (`38%` opacity) because no points exist yet.
- **Error:**
  - If the user enters invalid rules in the Match Setup dialog (for example, `0` points), an inline error appears on the text field: `The rule value is not valid. Each value must be at least 1. Enter a whole number of 1 or more.`
  - If the local watch port `127.0.0.1:28282` is busy, the app logs the warning and shows a non-blocking snackbar: `The watch bridge could not start. Port 28282 on the phone is busy. Close other scoreboard apps and reopen the Match screen.` Scoring on the screen and with the Bluetooth remote control continues to work.
- **Offline:** Scoring, Bluetooth remote control keys, and the local Zepp OS watch bridge (`127.0.0.1:28282`) work completely offline without internet access.
- **Success (Match finished):** When a player wins the required number of games, the app saves the completed match to `MatchHistoryStore` and opens the Match Finished overlay (`MATCH FINISHED · WINNER: [Player name]`).

### 4.5 TalkBack Reading Order
1. Live status text (`polite` live region): `"Match time 14 minutes 8 seconds. Game to 11, match to 3. Game ball for Player 1."`
2. Court timer button: `"Court timer active. Start time 19:00. 32 minutes remaining. Status OK. Double-tap to manage the court timer."` (or `"Start court timer. Court timer is off. Button."`).
3. Action bar buttons:
   - `"Pause match. Button."` (or `"Resume match. Button."`)
   - `"Undo point. Button."`
   - `"Set up match. Button."`
   - `"New match. Button."`
4. Player 1 panel: `"Player 1, [Name]. 2 games won, 10 points, Game ball. Button. Double-tap to add 1 point to [Name]."`
5. Player 2 panel: `"Player 2, [Name]. 1 game won, 8 points. Button. Double-tap to add 1 point to [Name]."`
6. Bottom navigation bar / rail: `"Match, selected, tab 1 of 4"`, `"History, tab 2 of 4"`, `"Players, tab 3 of 4"`, `"Settings, tab 4 of 4"`.

### 4.6 Exact Screen Text (ASD-STE100)

#### Main Scoreboard and Status Bar
| Element ID | Element Type | Exact Text |
|---|---|---|
| `match_status_default` | Status text | `%1$s · GAME TO %2$d | MATCH TO %3$d` |
| `match_status_paused` | Status text | `PAUSED · %1$s · %2$s` |
| `match_status_game_ball` | Status text | `%1$s · GAME BALL: %2$s` |
| `match_status_match_ball` | Status text | `%1$s · MATCH BALL: %2$s` |
| `match_status_finished` | Status text | `MATCH WINNER: %1$s · %2$s` |
| `match_court_off` | Button | `Start court timer` |
| `match_court_wait` | Button | `COURT ON · START %1$s IN %2$d:%3$02d` |
| `match_court_ok` | Button | `COURT ON · %1$s · %2$d:%3$02d · OK` |
| `match_court_warn` | Button | `COURT ON · %1$s · %2$d:%3$02d · WARN` |
| `match_court_end` | Button | `COURT ON · %1$s · END` |
| `match_btn_pause` | Button | `Pause match` |
| `match_btn_resume` | Button | `Resume match` |
| `match_btn_undo` | Button | `Undo point` |
| `match_btn_setup` | Button | `Set up match` |
| `match_btn_new` | Button | `New match` |
| `match_p1_default` | Label | `PLAYER 1` |
| `match_p2_default` | Label | `PLAYER 2` |
| `match_games_label` | Label | `GAMES %1$d` |
| `match_tap_hint` | Instruction | `TAP PANEL TO ADD 1 POINT` |
| `match_pause_banner` | Overlay label | `MATCH PAUSED · TAP RESUME MATCH TO CONTINUE` |

#### Dialogs on the Match Screen
| Dialog / Element | Element Type | Exact Text |
|---|---|---|
| **New Match Dialog** | Title | `Start a new match?` |
| `dlg_new_body` | Message | `The app will finish the current match and save the match in the history.` |
| `dlg_new_cancel` | Button | `Cancel action` |
| `dlg_new_confirm` | Button | `Start new match` |
| **Court Timer Dialog** | Title | `Court timer` |
| `dlg_court_body` | Message | `Do you want to reset the start time to the current full hour, or stop the court timer?` |
| `dlg_court_cancel` | Button | `Cancel action` |
| `dlg_court_stop` | Button | `Stop court timer` |
| `dlg_court_reset` | Button | `Reset court timer` |
| **Match Setup Dialog** | Title | `Set up match` |
| `dlg_setup_note` | Description | `A change to the players or the rules starts a new match.` |
| `dlg_setup_p1_prof` | Field label | `Profile for Player 1` |
| `dlg_setup_p1_name` | Field label | `Name of Player 1` |
| `dlg_setup_p1_color` | Field label | `Color for Player 1` |
| `dlg_setup_p2_prof` | Field label | `Profile for Player 2` |
| `dlg_setup_p2_name` | Field label | `Name of Player 2` |
| `dlg_setup_p2_color` | Field label | `Color for Player 2` |
| `dlg_setup_guest` | Dropdown item | `Guest` |
| `dlg_setup_pts` | Field label | `Points to win a game` |
| `dlg_setup_games` | Field label | `Games to win the match` |
| `dlg_setup_cancel` | Button | `Cancel action` |
| `dlg_setup_save` | Button | `Start new match` |
| **Match Finished Overlay** | Title | `MATCH FINISHED` |
| `fin_subtitle` | Subtitle | `WINNER: %1$s` |
| `fin_btn_board` | Button | `Show match board [Previous track]` |
| `fin_btn_reset` | Button | `Start new match [Play / Pause]` |
| `fin_btn_add3` | Button | `Add 3 games [Next track]` |
| `fin_btn_details` | Button | `Show match details` |

---

## Screen 5: Match History List Screen (`HistoryActivity` - Main Task 3: List)

### 5.1 Purpose
Show the chronological list of saved matches (up to 100 matches), let the user filter by match status (`All matches`, `Finished`, `Unfinished`), open the Search screen, delete a match record, or tap a match card to view its detailed charts or resume play.

### 5.2 Text Wireframe
```text
+-------------------------------------------------------------------+
| Match history                     [Search matches] [Compare players]|
+-------------------------------------------------------------------+
| Filters: [x All matches (14)]  [ Finished (12) ]  [ Unfinished (2)]|
+-------------------------------------------------------------------+
| +---------------------------------------------------------------+ |
| | MAREK  vs  ANNA                               [Delete match]  | |
| | 3 : 1  |  FINISHED  |  42:15  |  26.10.2026 19:45             | |
| | Games: 11:8, 9:11, 11:6, 11:7                                 | |
| +---------------------------------------------------------------+ |
| +---------------------------------------------------------------+ |
| | TOMASZ  vs  MAREK                             [Delete match]  | |
| | 1 : 1  |  UNFINISHED  |  24:10  |  24.10.2026 18:30           | |
| | Games: 11:9, 7:11 · Current game: 6:4                         | |
| +---------------------------------------------------------------+ |
+-------------------------------------------------------------------+
| [Match]          [History (active)]      [Players]     [Settings] |
+-------------------------------------------------------------------+
```

### 5.3 Components
- Top app bar (`SmallTopAppBar`) with title `Match history`, action icon button **Search matches**, and action button **Open players**.
- Horizontal row of 3 Material 3 `FilterChip` components: **Select all matches**, **Select finished matches**, and **Select unfinished matches**.
- `LazyColumn` of Material 3 `ElevatedCard` items (`12 dp` corner radius, `8 dp` vertical gap, minimum height `88 dp`).
- Each match card contains:
  - Player pairing headline (`titleLarge`, `22 sp`): `[Player 1] vs [Player 2]`.
  - Metadata line (`bodyMedium`, `16 sp`): `[Games 1] : [Games 2] | [FINISHED / UNFINISHED] | [Duration] | [Date]`.
  - Status icon + badge (`Check icon + FINISHED` or `Clock icon + UNFINISHED`).
  - Trailing outlined danger button **Delete match** (`48 dp × 48 dp` minimum touch target).
- Delete confirmation `AlertDialog`.

### 5.4 States
- **Default:** Shows the list of saved match cards ordered from newest to oldest.
- **Loading:** Shows 4 shimmer placeholder cards and `Loading match history.` if reading large history files takes `> 100 ms`.
- **Empty:** Shows an empty-state illustration icon, the heading `No saved matches`, the description `The app saves each match here when the match finishes or when you start a new match.`, and a primary button **Open match screen**.
- **Error:** If a record in `match_history` is corrupted, an error banner shows the 3-part error message with **Retry load**.
- **Offline:** Works completely from local storage.
- **Success:** When the user deletes a match, a snackbar states `The match is deleted.`

### 5.5 TalkBack Reading Order
1. Screen title: `"Match history. Heading."`
2. Top bar actions: `"Search matches. Button."` -> `"Open players. Button."`
3. Filter chips: `"Select all matches, selected, 14 matches"` -> `"Select finished matches, 12 matches"` -> `"Select unfinished matches, 2 matches"`.
4. For each match card:
   - Card main action: `"Marek versus Anna. 3 to 1. Status: Finished. Duration 42 minutes 15 seconds. Date 26.10.2026 19:45. Double-tap to open match details."`
   - Trailing action: `"Delete match Marek versus Anna. Button."`
5. Navigation bar / rail.

### 5.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `hist_title` | Screen title | `Match history` |
| `hist_btn_search` | Button | `Search matches` |
| `hist_btn_players` | Button | `Open players` |
| `hist_chip_all` | Filter chip | `Select all matches (%1$d)` |
| `hist_chip_fin` | Filter chip | `Select finished (%1$d)` |
| `hist_chip_unfin` | Filter chip | `Select unfinished (%1$d)` |
| `hist_row_vs` | Card title | `%1$s vs %2$s` |
| `hist_state_fin` | Status badge | `FINISHED` |
| `hist_state_unfin` | Status badge | `UNFINISHED` |
| `hist_row_meta` | Card body | `%1$d : %2$d | %3$s | %4$s | %5$s` |
| `hist_btn_delete` | Button | `Delete match` |
| `hist_empty_title` | Heading | `No saved matches` |
| `hist_empty_body` | Description | `The app saves each match here when the match finishes or when you start a new match.` |
| `hist_empty_btn` | Button | `Open match screen` |
| `hist_dlg_del_title` | Dialog title | `Delete this match?` |
| `hist_dlg_del_body` | Dialog message | `The app will delete the match %1$s vs %2$s permanently.` |
| `hist_dlg_cancel` | Button | `Cancel action` |
| `hist_dlg_confirm` | Button | `Delete match` |
| `hist_snack_deleted` | Snackbar | `The match is deleted.` |
| `hist_err_load` | Error message (3-part) | `The app could not load the match history. A saved match record is damaged. Tap Retry load to load the valid matches.` |

---

## Screen 6: Match Detail and Fullscreen Chart Screen (`MatchDetailActivity` and `ChartFullscreenActivity` - Main Task 3: Detail)

### 6.1 Purpose
Show the complete breakdown of a selected match: the winner, individual game scores, total match duration, per-game durations, a 3-page interactive chart pager (Point Timeline Chart, Game Progress Chart, and Match Statistics Table), a fullscreen zoomable chart view, and a primary button to resume an unfinished match or open a finished match on the scoreboard.

### 6.2 Text Wireframe
```text
+-------------------------------------------------------------------+
| [<- Go back]     MAREK vs ANNA (3 : 1)         [ Delete match ]   |
+-------------------------------------------------------------------+
|  MATCH WINNER: MAREK · Game scores: 11:8, 9:11, 11:6, 11:7        |
|  MATCH DURATION: 42:15  |  GAME DURATIONS: G1 09:40 · G2 11:20... |
|  [■ MAREK (Blue, solid line)]     [◆ ANNA (Red, dashed line)]     |
|  [                 Open match on board / Resume match           ] |
+-------------------------------------------------------------------+
|  Tabs: [1. Show point chart] [2. Show game chart] [3. Show stats] |
|  +-------------------------------------------------------------+  |
|  |  POINT TIMELINE · TAP CHART TO OPEN FULLSCREEN              |  |
|  |   11 |          o---o                                       |  |
|  |    6 |    o--o         ◆---◆                                |  |
|  |    0 +----------------------------------------------------  |  |
|  |      00:00                  21:00                     42:15 |  |
|  +-------------------------------------------------------------+  |
+-------------------------------------------------------------------+
```

### 6.3 Components
- Top app bar with back button (`Go back`), match pairing title, and **Delete match** icon button.
- Summary `OutlinedCard`:
  - Winner / status line (`titleMedium`, `18 sp`).
  - Game scores line (`bodyMedium`, `16 sp`).
  - Match duration and per-game durations (`bodySmall`, `16 sp` tabular digits). If the record is a legacy entry without timestamps, shows `ESTIMATED DURATION FOR OLD RECORD`.
  - Accessible player legend showing player color swatch, player name, and line marker style (`Solid line, circle marker` vs `Dashed line, diamond marker`).
  - Primary filled button **Resume match** (if `finished == false`) or **Open match** (if `finished == true`).
- `PrimaryTabRow` + `HorizontalPager` (`ChartPager`) with 3 pages:
  - **Page 1 (`ScoreChartView`):** Point timeline across the match. Tapping the chart opens **Fullscreen Chart (`ChartFullscreenActivity`)**, where the user can pinch to zoom, drag to pan, and tap any point to inspect the exact timestamp and score.
  - **Page 2 (`SetProgressChartView`):** Point progression within each game. Tapping a game chip (`Select Game 1`, `Select Game 2`, ... or `Show all games`) isolates that game.
  - **Page 3 (`MatchStatsView`):** 4 horizontal comparison bars: **POINTS WON**, **POINT SHARE**, **GAMES WON**, and **LONGEST STREAK**.

### 6.4 States
- **Default:** Summary card and Page 1 (Point Timeline Chart) are visible.
- **Loading:** Shows a circular progress indicator while rendering a match with `> 200` points.
- **Empty (Match with `0` points):** The chart container displays `This match has 0 recorded points. Tap Resume match to add points.`
- **Error:** If the match JSON extra is missing or invalid, the screen displays the 3-part error message and a button **Go back**.
- **Offline:** Works 100% offline.
- **Success:** Tapping **Resume match** or **Open match** loads the match into the Match screen and restores the exact point history and timers.

### 6.5 TalkBack Reading Order
1. Back button: `"Go back. Button."`
2. Screen title: `"Marek versus Anna, 3 to 1. Heading."`
3. Delete button: `"Delete match. Button."`
4. Summary card: Winner status, game scores, match duration, game durations, and player legend.
5. Primary button: `"Resume match. Button."` (or `"Open match. Button."`).
6. Chart tabs: `"Show point chart, tab 1 of 3"`, `"Show game chart, tab 2 of 3"`, `"Show match statistics, tab 3 of 3"`.
7. Chart canvas or statistics rows (each statistics metric is read as a single grouped row: `"Points won: Marek 42, Anna 32"`).

### 6.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `md_winner` | Summary label | `MATCH WINNER: %1$s` |
| `md_unfinished` | Summary label | `STATUS: UNFINISHED · CURRENT SCORE %1$d:%2$d (%3$d:%4$d)` |
| `md_game_scores` | Summary label | `GAME SCORES: %1$s` |
| `md_duration` | Summary label | `MATCH DURATION: %1$s` |
| `md_duration_none` | Summary label | `MATCH DURATION: no data` |
| `md_duration_est` | Summary label | `ESTIMATED DURATION FOR OLD RECORD` |
| `md_game_durations` | Summary label | `GAME DURATIONS: %1$s` |
| `md_game_dur_none` | Summary label | `GAME DURATIONS: no data` |
| `md_btn_resume` | Button | `Resume match` |
| `md_btn_open` | Button | `Open match` |
| `md_tab_points` | Tab label | `Show point chart` |
| `md_tab_games` | Tab label | `Show game chart` |
| `md_tab_stats` | Tab label | `Show match statistics` |
| `md_chart1_title` | Chart header | `POINT TIMELINE · TAP CHART TO OPEN FULLSCREEN` |
| `md_chart1_full` | Fullscreen header | `MATCH TIME · PINCH, DRAG, OR TAP A POINT` |
| `md_chart1_close` | Button | `Close chart` |
| `md_chart2_title` | Chart header | `GAME PROGRESS · TAP A GAME TO FILTER` |
| `md_chart2_all` | Button / Chip | `Show all games` |
| `md_chart2_game` | Chip | `Select Game %1$d` |
| `md_stat_title` | Section header | `MATCH STATISTICS` |
| `md_stat_pts` | Metric label | `POINTS WON` |
| `md_stat_share` | Metric label | `POINT SHARE` |
| `md_stat_games` | Metric label | `GAMES WON` |
| `md_stat_streak` | Metric label | `LONGEST STREAK` |
| `md_empty_pts` | Empty message | `This match has 0 recorded points. Tap Resume match to add points.` |
| `md_err_record` | Error message (3-part) | `The app could not open the match record. The match data is missing or damaged. Tap Go back and select another match.` |

---

## Screen 7: Search Screen (`SearchView` on History & Players)

### 7.1 Purpose
Let the user quickly find saved matches or player profiles by typing a player name, a score (such as `3:2`), or a date (`dd.MM.yyyy`), or by tapping status filter chips.

### 7.2 Text Wireframe
```text
+-------------------------------------------------------------------+
| [<- Go back]  [ Search by player name or date... ] [Clear search] |
+-------------------------------------------------------------------+
| Quick filters: [ Select finished ] [ Select unfinished ]          |
|                [ Select Marek ]    [ Select Anna ]                |
+-------------------------------------------------------------------+
| MATCHING PLAYERS (1)                                              |
| +---------------------------------------------------------------+ |
| | [Blue Circle] Marek · 12 matches · 8 wins (67%)  [Open player]| |
| +---------------------------------------------------------------+ |
| MATCHING MATCHES (3)                                              |
| +---------------------------------------------------------------+ |
| | MAREK vs ANNA · 3 : 1 · FINISHED · 42:15 · 26.10.2026 19:45   | |
| +---------------------------------------------------------------+ |
+-------------------------------------------------------------------+
```

### 7.3 Components
- Material 3 `SearchBar` / full-screen `SearchView` with leading back button (`Go back`), text input (`16 sp`), and trailing **Clear search** icon button (`48 dp × 48 dp`).
- Horizontal `LazyRow` of `FilterChip` components (status filters + saved player profile chips).
- Categorized `LazyColumn` showing **MATCHING PLAYERS** and **MATCHING MATCHES**.

### 7.4 States
- **Default (Before typing):** Shows recent player chips and all saved matches below the filter bar.
- **Loading:** Filters local in-memory records in `< 16 ms`; shows a linear progress bar if a large history is being indexed.
- **Empty (No matches for query):** Shows search-off icon, `No results for "%1$s"`, `Check the spelling of the player name or clear the active filters.`, and the button **Clear search**.
- **Error:** If a date filter format is invalid, shows the 3-part error message.
- **Offline:** Searches local data 100% offline.
- **Success:** Tapping a player row opens **Player Detail**; tapping a match card opens **Match Detail**.

### 7.5 TalkBack Reading Order
1. Back button: `"Go back. Button."`
2. Search input field: `"Search by player name or date. Edit box."`
3. Clear button (when text is not empty): `"Clear search. Button."`
4. Quick filter chips: `"Select finished, filter chip"`, `"Select unfinished, filter chip"`, player filter chips.
5. Section heading `"Matching players"` -> Player items.
6. Section heading `"Matching matches"` -> Match cards.

### 7.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `srch_hint` | Text field placeholder | `Search by player name or date` |
| `srch_btn_clear` | Button | `Clear search` |
| `srch_sec_players` | Section header | `MATCHING PLAYERS (%1$d)` |
| `srch_sec_matches` | Section header | `MATCHING MATCHES (%1$d)` |
| `srch_empty_title` | Heading | `No results for "%1$s"` |
| `srch_empty_body` | Description | `No saved matches or players match this search. Check the spelling or clear the filters.` |
| `srch_err_query` | Error message (3-part) | `The search filter could not run. The entered text contains characters that are not valid. Tap Clear search and enter a player name or a date.` |

---

## Screen 8: Players and Statistics Screen (`PlayerStatsActivity`, `PlayerDetailActivity`, `PlayerComparisonActivity` - Profile & Task 3)

### 8.1 Purpose
Manage saved player profiles (each with a stable UUID, name, and color), show the career statistics table for all players, open an individual **Player Detail (Profile)** screen (`PlayerDetailActivity`), and open the **Player Comparison** screen (`PlayerComparisonActivity`) to compare 2 players side by side.

### 8.2 Text Wireframe (Players List, Player Detail, and Player Comparison)
```text
[8A: PLAYERS AND STATISTICS LIST SCREEN]
+-------------------------------------------------------------------+
| Players and statistics          [Compare players]  [+ Add player] |
+-------------------------------------------------------------------+
| Player          | Matches | Wins | Win rate | Points | Avg pts/m  |
|-------------------------------------------------------------------|
| [■ Blue] Marek  |   12    |  8   |   67%    |  486   |   40.5     |
| [◆ Red]  Anna   |   10    |  6   |   60%    |  392   |   39.2     |
| [▲ Grn]  Tomasz |    6    |  2   |   33%    |  210   |   35.0     |
+-------------------------------------------------------------------+

[8B: PLAYER DETAIL (PROFILE) SCREEN]
+-------------------------------------------------------------------+
| [<- Go back]     Player profile: Marek                            |
+-------------------------------------------------------------------+
|  [■ Blue swatch]  Marek (Color: Blue)                             |
|  +-----------------------------+ +------------------------------+ |
|  | Matches: 12                 | | Wins: 8 (Win rate: 67%)      | |
|  | Points won: 486             | | Average points / match: 40.5 | |
|  | Average points / game: 10.1 | | Average games won / m: 2.3   | |
|  | Points to 6-7 or 7-6: 5     | | Points to 6-9 or 9-6: 3      | |
|  +-----------------------------+ +------------------------------+ |
|  [ Edit player ]                                [ Delete player ] |
+-------------------------------------------------------------------+

[8C: PLAYER COMPARISON SCREEN]
+-------------------------------------------------------------------+
| [<- Go back]     Player comparison                                |
+-------------------------------------------------------------------+
|        MAREK (Blue)       |     Metric      |     ANNA (Red)      |
|---------------------------+-----------------+---------------------|
|            12             |     Matches     |         10          |
|             8             |      Wins       |          6          |
|            67%            |    Win rate     |         60%         |
|           486             |     Points      |        392          |
|           40.5            |  Avg pts/match  |        39.2         |
|             5             |   6-7 / 7-6     |          4          |
|             3             |   6-9 / 9-6     |          2          |
+-------------------------------------------------------------------+
|  HEAD-TO-HEAD SCORE: MAREK 5 : 3 ANNA  (8 matches)                |
+-------------------------------------------------------------------+
```

### 8.3 Components
- **8A (List):** Top app bar with buttons **Compare players** and **Add player**; scrollable table with sticky header and `56 dp` minimum row height; **Add Player Dialog** and **Compare Players Dialog**.
- **8B (Profile Detail):** Player identity header with color badge + text color name; 2-column grid of `OutlinedCard` statistics; **Edit player** filled tonal button and **Delete player** error button; **Edit Player Dialog** and **Delete Player Confirmation Dialog**.
- **8C (Comparison):** 3-column comparison table (`Player 1 | Metric | Player 2`) with tabular numerals and a bottom `SecondaryContainer` banner showing the direct head-to-head record.

### 8.4 States
- **Default:** Lists all saved player profiles with live calculated statistics from `PlayerStatistics.calculate()`.
- **Loading:** Shows a progress bar if recalculating statistics across 100 matches.
- **Empty:** When `ProfileStore` has `0` profiles, displays `No saved player profiles. Add a player profile to track career statistics.` and a primary button **Add player**.
- **Error:**
  - Empty name in Add/Edit dialog: `The player name is empty. Each player profile must have a name. Enter a name in the text field.`
  - Duplicate name in Add/Edit dialog: `This player name already exists. Each player profile must have a unique name. Enter a different name.`
  - Fewer than 2 profiles when tapping **Compare players**: `The app cannot compare players yet. A comparison requires at least 2 player profiles. Tap Add player to create another profile.`
  - Same profile selected twice in Compare dialog: `The selected profiles are the same. You must select 2 different player profiles. Select a different profile for Player 2.`
- **Offline:** All profile storage and statistics calculations run locally on the device.
- **Success:** Adding, editing, or deleting a profile shows a confirmation snackbar (`The player profile is saved.` or `The player profile is deleted.`). When a player name changes, `MatchHistoryStore.renameProfile()` updates all linked matches automatically.

### 8.5 TalkBack Reading Order
- **8A (List):** Screen title -> `"Compare players. Button."` -> `"Add player. Button."` -> Each player row read as a complete summary: `"Marek, Color Blue. 12 matches, 8 wins, win rate 67%, 486 points, average 40.5 points per match. Double-tap to open player profile."`
- **8B (Profile Detail):** `"Go back. Button."` -> `"Player profile: Marek, Color Blue. Heading."` -> Statistics cards in reading order -> `"Edit player. Button."` -> `"Delete player. Button."`
- **8C (Comparison):** `"Go back. Button."` -> `"Player comparison: Marek versus Anna. Heading."` -> Each comparison row read as `"Matches: Marek 12, Anna 10"` -> `"Head-to-head score: Marek 5, Anna 3, in 8 matches."`

### 8.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `pl_title` | Screen title | `Players and statistics` |
| `pl_btn_compare` | Button | `Compare players` |
| `pl_btn_add` | Button | `Add player` |
| `pl_col_player` | Table header | `Player` |
| `pl_col_matches` | Table header | `Matches` |
| `pl_col_wins` | Table header | `Wins` |
| `pl_col_rate` | Table header | `Win rate` |
| `pl_col_points` | Table header | `Points` |
| `pl_col_avg` | Table header | `Avg pts/match` |
| `pl_empty` | Empty message | `No saved player profiles. Add a player profile to track career statistics.` |
| **Add / Edit Dialog** | Title (Add) | `Add player` |
| `pl_dlg_edit_title` | Title (Edit) | `Edit player` |
| `pl_dlg_name_lbl` | Field label | `Player name` |
| `pl_dlg_color_lbl` | Field label | `Player color` |
| `pl_color_blue` | Option | `Blue` |
| `pl_color_red` | Option | `Red` |
| `pl_color_green` | Option | `Green` |
| `pl_color_gold` | Option | `Gold` |
| `pl_dlg_cancel` | Button | `Cancel action` |
| `pl_dlg_save` | Button | `Save player` |
| **Player Detail (Profile)** | Screen title | `Player profile` |
| `pd_matches` | Stat label | `Matches: %1$d` |
| `pd_wins` | Stat label | `Wins: %1$d (%2$s%%)` |
| `pd_points` | Stat label | `Points won: %1$d` |
| `pd_avg_match` | Stat label | `Average points per match: %1$s` |
| `pd_avg_game` | Stat label | `Average points per game: %1$s` |
| `pd_avg_games_won` | Stat label | `Average games won per match: %1$s` |
| `pd_score_67` | Stat label | `Points to 6-7 or 7-6: %1$d` |
| `pd_score_69` | Stat label | `Points to 6-9 or 9-6: %1$d` |
| `pd_btn_edit` | Button | `Edit player` |
| `pd_btn_delete` | Button | `Delete player` |
| **Delete Player Dialog** | Dialog title | `Delete this player profile?` |
| `pd_del_body` | Dialog message | `The match history will stay on the device, but %1$s will not appear in the player list.` |
| `pd_del_confirm` | Button | `Delete player` |
| **Compare Players** | Dialog title | `Compare 2 players` |
| `pc_btn_show` | Button | `Show comparison` |
| `pc_title` | Screen title | `Player comparison` |
| `pc_h2h` | Banner text | `HEAD-TO-HEAD SCORE: %1$s %2$d : %3$d %4$s (%5$d matches)` |
| `pl_err_empty_name` | Error message (3-part) | `The player name is empty. Each player profile must have a name. Enter a name in the text field.` |
| `pl_err_dup_name` | Error message (3-part) | `This player name already exists. Each player profile must have a unique name. Enter a different name.` |
| `pl_err_need_two` | Error message (3-part) | `The app cannot compare players yet. A comparison requires at least 2 player profiles. Tap Add player to create another profile.` |
| `pl_err_same_two` | Error message (3-part) | `The selected profiles are the same. You must select 2 different player profiles. Select a different profile for Player 2.` |

---

## Screen 9: Settings Screen (`SettingsScreen` / Dialogs in `MainActivity`)

### 9.1 Purpose
Configure default match rules, Bluetooth remote control key mappings, court rental timer duration and warning thresholds, vibration alerts, dark theme and high-contrast mode, and provide direct links to **Account and cloud backup** and **Help and remote guide**.

### 9.2 Text Wireframe
```text
+-------------------------------------------------------------------+
| Settings                                      [ Save settings ]   |
+-------------------------------------------------------------------+
|  DEFAULT MATCH RULES                                              |
|  Points to win a game: [ 11 ]     Games to win the match: [ 3 ]   |
|                                                                   |
|  BLUETOOTH REMOTE CONTROL KEYS                                    |
|  Remote key for Player 1: [ Volume +       v ]                    |
|  Remote key for Player 2: [ Volume -       v ]                    |
|  Remote key for Undo:     [ Previous track v ]                    |
|  Remote key for Reset:    [ Next track     v ]                    |
|                                                                   |
|  COURT TIMER SETTINGS                                             |
|  Rental duration (min): [ 60 ]    Green warning (min):  [ 30 ]    |
|  Yellow warning (min):  [ 15 ]    Red warning (min):    [ 5  ]    |
|  [x] Vibrate at warnings (1, 2, and 3 pulses)                     |
|                                                                   |
|  APPEARANCE                                                       |
|  [x] Use dark theme               [ ] Use high-contrast mode      |
|                                                                   |
|  ACCOUNT AND SUPPORT                                              |
|  [ Manage account ]               [ Open help ]                   |
+-------------------------------------------------------------------+
```

### 9.3 Components
- Top app bar with title `Settings` and primary action button **Save settings**.
- Grouped `OutlinedCard` sections:
  - **Default match rules:** 2 numeric `OutlinedTextField` inputs (`Points to win a game`, `Games to win the match`).
  - **Bluetooth remote control keys:** 4 `ExposedDropdownMenuBox` selectors with options `Volume +`, `Volume -`, `Next track`, and `Previous track`.
  - **Court timer settings:** 4 numeric `OutlinedTextField` inputs (`Rental duration (min)`, `Green warning (min before end)`, `Yellow warning (min before end)`, `Red warning (min before end)`) and 1 Material 3 `Switch` (`Vibrate at warnings (1, 2, and 3 pulses)`).
  - **Appearance:** 2 Material 3 `Switch` rows (`Use dark theme`, `Use high-contrast mode`).
  - **Account and support:** 2 navigation list items / buttons (**Manage account** -> Screen 3; **Open help** -> Screen 10).
- **Discard Changes Confirmation Dialog** when the user presses Back with unsaved edits (`Discard unsaved changes?` -> **Keep editing** / **Discard changes**).

### 9.4 States
- **Default:** Shows current saved values from `AppPreferences`.
- **Loading:** Applies theme and remote changes in `< 50 ms`.
- **Empty:** If a user clears a numeric text field, the field highlights in the `Error` state.
- **Error:**
  - If any numeric field is `<= 0` or not a number: `The entered value is not valid. Every rule and court timer value must be a whole number of 1 or more. Enter a positive whole number.`
  - If two remote actions use the same key: `Two remote actions use the same key. Each remote action needs a different key. Select a unique key for each action.`
- **Offline:** All settings are stored in local `SharedPreferences` and work offline.
- **Success:** Tapping **Save settings** stores all preferences and shows the snackbar `The settings are saved.`

### 9.5 TalkBack Reading Order
1. Screen title: `"Settings. Heading."`
2. Save button: `"Save settings. Button."`
3. Section `"Default match rules"` -> `"Points to win a game, 11, edit box"` -> `"Games to win the match, 3, edit box"`.
4. Section `"Bluetooth remote control keys"` -> 4 dropdown selectors.
5. Section `"Court timer settings"` -> 4 numeric fields -> `"Vibrate at warnings, 1, 2, and 3 pulses. Switch, on."`
6. Section `"Appearance"` -> `"Use dark theme. Switch, on."` -> `"Use high-contrast mode. Switch, off."`
7. Section `"Account and support"` -> `"Manage account. Button."` -> `"Open help. Button."`

### 9.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `set_title` | Screen title | `Settings` |
| `set_btn_save` | Button | `Save settings` |
| `set_note` | Description | `Changes to the remote control and the theme apply immediately.` |
| `set_sec_rules` | Section header | `DEFAULT MATCH RULES` |
| `set_pts_game` | Field label | `Points to win a game` |
| `set_games_match` | Field label | `Games to win the match` |
| `set_sec_remote` | Section header | `BLUETOOTH REMOTE CONTROL KEYS` |
| `set_rem_p1` | Field label | `Remote key for Player 1` |
| `set_rem_p2` | Field label | `Remote key for Player 2` |
| `set_rem_undo` | Field label | `Remote key for Undo` |
| `set_rem_reset` | Field label | `Remote key for Reset` |
| `set_key_vol_up` | Dropdown option | `Volume +` |
| `set_key_vol_down` | Dropdown option | `Volume -` |
| `set_key_next` | Dropdown option | `Next track` |
| `set_key_prev` | Dropdown option | `Previous track` |
| `set_sec_court` | Section header | `COURT TIMER SETTINGS` |
| `set_court_dur` | Field label | `Rental duration (min)` |
| `set_court_green` | Field label | `Green warning (min before end)` |
| `set_court_yellow` | Field label | `Yellow warning (min before end)` |
| `set_court_red` | Field label | `Red warning (min before end)` |
| `set_court_vib` | Switch label | `Vibrate at warnings (1, 2, and 3 pulses)` |
| `set_sec_theme` | Section header | `APPEARANCE` |
| `set_sw_dark` | Switch label | `Use dark theme` |
| `set_sw_hc` | Switch label | `Use high-contrast mode` |
| `set_sec_more` | Section header | `ACCOUNT AND SUPPORT` |
| `set_btn_account` | Button | `Manage account` |
| `set_btn_help` | Button | `Open help` |
| **Discard Dialog** | Dialog title | `Discard unsaved changes?` |
| `set_disc_body` | Dialog message | `You changed the settings. If you leave now, the app will not save the changes.` |
| `set_disc_keep` | Button | `Keep editing` |
| `set_disc_discard` | Button | `Discard changes` |
| `set_err_num` | Error message (3-part) | `The entered value is not valid. Every rule and court timer value must be a whole number of 1 or more. Enter a positive whole number.` |
| `set_err_key` | Error message (3-part) | `Two remote actions use the same key. Each remote action needs a different key. Select a unique key for each action.` |
| `set_snack_saved` | Snackbar | `The settings are saved.` |

---

## Screen 10: Help and Remote Setup Guide Screen (`HelpActivity` / Route `settings/help`)

### 10.1 Purpose
Provide clear, step-by-step ASD-STE100 instructions for scoring on the screen, pairing a Bluetooth remote control, connecting the Amazfit Zepp OS watch (`127.0.0.1:28282`), and using the court timer.

### 10.2 Text Wireframe
```text
+-------------------------------------------------------------------+
| [<- Go back]     Help and remote control guide                    |
+-------------------------------------------------------------------+
|  [Watch bridge status: Ready on 127.0.0.1:28282] [Test watch link]|
|                                                                   |
|  1. SCORE ON THE SCREEN                                           |
|  1. Tap the Player 1 panel to add 1 point to Player 1.            |
|  2. Tap the Player 2 panel to add 1 point to Player 2.            |
|  3. Tap Undo point to remove the last point.                      |
|                                                                   |
|  2. USE A BLUETOOTH REMOTE CONTROL                                |
|  1. Pair the Bluetooth remote control in the Android settings.    |
|  2. Open the Match screen in DropShot.                            |
|  3. Press Volume + for Player 1 or Volume - for Player 2.         |
|  4. Press Pause to undo 1 point, or Play to start a new match.    |
|                                                                   |
|  3. CONNECT THE ZEPP OS WATCH                                     |
|  WARNING: Do not tap a watch button repeatedly if the watch is    |
|  disconnected. The watch does not queue commands, so a late point |
|  never changes the score after reconnection.                      |
|  1. Pair the Amazfit watch with the Zepp app on this phone.       |
|  2. Keep the Match screen open in DropShot.                       |
|  3. Open Punkty Squash on the watch and tap +1, +2, <<, or R.     |
+-------------------------------------------------------------------+
```

### 10.3 Components
- Top app bar with back button (`Go back`) and title `Help and remote control guide`.
- Diagnostic status card showing the local loopback watch bridge state (`127.0.0.1:28282`) and a button **Test watch bridge**.
- 4 expandable or stacked `OutlinedCard` guide sections with numbered STE instructions and safety warnings placed **before** the steps they apply to.
- Bottom button **Open match screen**.

### 10.4 States
- **Default:** All 4 help sections and the watch diagnostic card are visible.
- **Loading:** When the user taps **Test watch bridge**, a circular indicator shows `Checking local port 28282.`
- **Empty:** Not applicable; static help content is always present on the device.
- **Error:** If the local loopback check fails, the diagnostic card shows the 3-part error message and **Retry test**.
- **Offline:** All help articles and the `127.0.0.1:28282` loopback test work offline without an internet connection.
- **Success:** When the loopback check succeeds, the status card shows `The local watch bridge on 127.0.0.1:28282 is ready.`

### 10.5 TalkBack Reading Order
1. Back button: `"Go back. Button."`
2. Screen title: `"Help and remote control guide. Heading."`
3. Diagnostic card: Status text -> `"Test watch bridge. Button."`
4. Section 1 heading and numbered steps.
5. Section 2 heading, warning, and numbered steps.
6. Section 3 heading, warning, and numbered steps.
7. Section 4 heading and numbered steps.
8. Bottom button: `"Open match screen. Button."`

### 10.6 Exact Screen Text (ASD-STE100)
| Element ID | Element Type | Exact Text |
|---|---|---|
| `help_title` | Screen title | `Help and remote control guide` |
| `help_diag_title` | Card title | `Local watch bridge status` |
| `help_diag_desc` | Description | `The app listens on 127.0.0.1:28282 while the Match screen is open.` |
| `help_btn_test` | Button | `Test watch bridge` |
| `help_btn_retry` | Button | `Retry test` |
| `help_diag_loading` | Status text | `Checking local port 28282.` |
| `help_diag_ok` | Success message | `The local watch bridge on 127.0.0.1:28282 is ready.` |
| `help_diag_err` | Error message (3-part) | `The local watch bridge test failed. Port 28282 on the phone is busy. Close other scoreboard apps and tap Retry test.` |
| `help_sec1_title` | Section header | `1. Score on the screen` |
| `help_sec1_steps` | Numbered instructions | `1. Tap the Player 1 panel to add 1 point to Player 1.`<br>`2. Tap the Player 2 panel to add 1 point to Player 2.`<br>`3. Tap Undo point to remove the last point.`<br>`The app awards a game at 11 points with a 2-point lead.` |
| `help_sec2_title` | Section header | `2. Use a Bluetooth remote control` |
| `help_sec2_warn` | Warning (before steps) | `WARNING: The volume keys change the score only while the Match screen is open.` |
| `help_sec2_steps` | Numbered instructions | `1. Pair the Bluetooth remote control in the Android settings.`<br>`2. Open the Match screen in DropShot.`<br>`3. Press Volume + to add 1 point to Player 1.`<br>`4. Press Volume - to add 1 point to Player 2.`<br>`5. Press Pause to undo 1 point, or press Play to start a new match.` |
| `help_sec3_title` | Section header | `3. Connect the Zepp OS watch` |
| `help_sec3_warn` | Warning (before steps) | `WARNING: If the watch loses the Bluetooth connection, the watch does not send the command again. This rule prevents a late point after reconnection.` |
| `help_sec3_steps` | Numbered instructions | `1. Pair the Amazfit watch with the Zepp app on the phone.`<br>`2. Open the Match screen in DropShot.`<br>`3. Open Punkty Squash on the watch.`<br>`4. Tap +1 for Player 1, +2 for Player 2, << to undo, or R to reset.` |
| `help_sec4_title` | Section header | `4. Use the court timer` |
| `help_sec4_steps` | Numbered instructions | `1. Tap Start court timer on the Match screen.`<br>`2. The app sets the start time to the current full hour.`<br>`3. Check the border color and the status text at 30 min, 15 min, and 5 min.`<br>`The device vibrates at each warning if vibration is on.` |
| `help_btn_match` | Button | `Open match screen` |

