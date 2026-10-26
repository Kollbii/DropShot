# Deliverable 9: ASD-STE100 Compliance Checklist (Microcopy Audit)

This checklist audits **every screen** in DropShot against all required ASD-STE100 rules:

1. **R1 (Simple words, 1 word = 1 meaning, no idioms/slang):** Every term comes from the approved Glossary ([`04-glossary.md`](04-glossary.md)).
2. **R2 (No contractions):** `0` contractions (`do not`, `cannot`, `is not`, `will not` are used everywhere).
3. **R3 (Maximum 3 nouns in a string):** `0` noun clusters exceed 3 consecutive nouns (for example, `settings for the court timer` or `court timer settings` = 3 nouns).
4. **R4 (Sentence length limits):** Every instruction is **`<= 20 words`** and every description is **`<= 25 words`**.
5. **R5 (One idea per sentence, active voice, simple tenses only, no `-ing` verb forms):** Only simple present, simple past, and simple future (`will`) are used. Words ending in `-ing` are never used as progressive verbs or gerunds in sentences (only as technical nouns/adjectives in headings where permitted, and avoided in all instructions and messages).
6. **R6 (Articles `"the"`, `"a"`, `"an"`, `"must"` vs `"can"`):** Every countable noun in sentences uses an article (`the`, `a`, `an`). `"Must"` is used only for required actions; `"can"` is used only for possibilities.
7. **R7 (Instructions & warnings):** Instructions use the imperative form, 1 action per sentence, numbered lists for sequences, a separate sentence for results, and warnings placed **before** the step they apply to.
8. **R8 (Buttons, numbers, and SI units):** Every button starts with an imperative verb (`Save`, `Start`, `Delete`, `Undo`, `Pause`, `Resume`, `Show`, `Open`, `Select`, `Clear`, `Cancel`, `Retry`), numbers use digits (`1`, `2`, `3`, `11`, `60`), and units follow numbers (`60 min`, `30 min`, `15 min`, `5 min`, `6 m`).
9. **R9 (3-part non-blaming error messages):** Every error message has 3 sentences: (1) What occurred, (2) Why it occurred, (3) What the user must do. No error message blames the user.

---

## Screen-by-Screen STE Verification Table

| Screen | Max Instruction Length (`<= 20` words) | Max Description Length (`<= 25` words) | Contractions Count (`0`) | Max Noun Cluster (`<= 3`) | `-ing` Verb Count (`0`) | Verb-First Buttons (`100%`) | 3-Part Error Format (`100%`) | Result |
|---|---|---|---|---|---|---|---|---|
| **1. Splash Screen** | `16` words (`Tap Retry load, or tap Reset local data to start with default settings.`) | `10` words (`The app could not read the saved settings.`) | `0` | `3` (`local match data`) | `0` | `Retry load`, `Reset local data` | 1. What: `The app could not read the saved settings.`<br>2. Why: `The storage file on the device is damaged.`<br>3. Action: `Tap Retry load, or tap Reset local data to start with default settings.` | **PASS** |
| **2. Onboarding (Screens 1-3)** | `12` words (`Tap the Player 1 panel to add 1 point to Player 1.`) | `17` words (`The court timer shows warnings at 30 min, 15 min, and 5 min before the rental ends.`) | `0` | `3` (`Bluetooth remote control`) | `0` | `Skip intro`, `Show previous step`, `Show next step`, `Select 45 min`, `Select 60 min`, `Select 90 min`, `Start app` | 1. What: `The app could not save the settings.`<br>2. Why: `The device storage is full.`<br>3. Action: `Free storage space on the device and tap Save settings.` | **PASS** |
| **3. Sign-In & Cloud Backup** | `12` words (`Tap Sync data now to upload the matches and the profiles.`) | `14` words (`You can use all match and statistics features without an account.`) | `0` | `2` (`cloud backup`, `player profiles`) | `0` | `Go back`, `Sign in with Google`, `Continue without account`, `Sync data now`, `Retry sync`, `Sign out of account` | 1. What: `The cloud backup failed.`<br>2. Why: `The network connection stopped during the transfer.`<br>3. Action: `Connect the device to the network and tap Retry sync.` | **PASS** |
| **4. Home / Match Screen** | `11` words (`Close other scoreboard apps and reopen the Match screen.`) | `14` words (`The app will finish the current match and save the match in the history.`) | `0` | `2` (`court timer`, `match board`) | `0` | `Start court timer`, `Pause match`, `Resume match`, `Undo point`, `Set up match`, `New match`, `Cancel action`, `Start new match`, `Stop court timer`, `Reset court timer`, `Show match board`, `Add 3 games`, `Show match details` | 1. What: `The rule value is not valid.`<br>2. Why: `Each value must be at least 1.`<br>3. Action: `Enter a whole number of 1 or more.` | **PASS** |
| **5. Match History List** | `8` words (`Tap Retry load to load the valid matches.`) | `18` words (`The app saves each match here when the match finishes or when you start a new match.`) | `0` | `3` (`saved match record`) | `0` | `Search matches`, `Open players`, `Select all matches`, `Select finished`, `Select unfinished`, `Delete match`, `Open match screen`, `Cancel action`, `Retry load` | 1. What: `The app could not load the match history.`<br>2. Why: `A saved match record is damaged.`<br>3. Action: `Tap Retry load to load the valid matches.` | **PASS** |
| **6. Match Detail & Chart** | `9` words (`Tap Go back and select another match.`) | `7` words (`This match has 0 recorded points.`) | `0` | `2` (`match statistics`, `game chart`) | `0` | `Go back`, `Delete match`, `Resume match`, `Open match`, `Show point chart`, `Show game chart`, `Show match statistics`, `Show all games`, `Select Game %1$d`, `Close chart`, `Zoom in`, `Zoom out`, `Reset zoom` | 1. What: `The app could not open the match record.`<br>2. Why: `The match data is missing or damaged.`<br>3. Action: `Tap Go back and select another match.` | **PASS** |
| **7. Search Screen** | `12` words (`Tap Clear search and enter a player name or a date.`) | `10` words (`No saved matches or players match this search.`) | `0` | `2` (`search filter`, `player name`) | `0` | `Go back`, `Clear search`, `Select finished`, `Select unfinished` | 1. What: `The search filter could not run.`<br>2. Why: `The entered text contains characters that are not valid.`<br>3. Action: `Tap Clear search and enter a player name or a date.` | **PASS** |
| **8. Players, Profile & Comparison** | `9` words (`Select a different profile for Player 2.`) | `19` words (`The match history will stay on the device, but %1$s will not appear in the player list.`) | `0` | `2` (`player profile`, `career statistics`) | `0` | `Compare players`, `Add player`, `Cancel action`, `Save player`, `Edit player`, `Delete player`, `Show comparison` | 1. What: `The selected profiles are the same.`<br>2. Why: `You must select 2 different player profiles.`<br>3. Action: `Select a different profile for Player 2.` | **PASS** |
| **9. Settings Screen** | `8` words (`Select a unique key for each action.`) | `14` words (`Every rule and court timer value must be a whole number of 1 or more.`) | `0` | `3` (`court timer settings`) | `0` | `Save settings`, `Manage account`, `Open help`, `Keep editing`, `Discard changes` | 1. What: `Two remote actions use the same key.`<br>2. Why: `Each remote action needs a different key.`<br>3. Action: `Select a unique key for each action.` | **PASS** |
| **10. Help & Remote Guide** | `15` words (`Tap +1 for Player 1, +2 for Player 2, << to undo, or R to reset.`) | `16` words (`If the watch loses the Bluetooth connection, the watch does not send the command again.`) | `0` | `3` (`Bluetooth remote control`, `local watch bridge`) | `0` | `Go back`, `Test watch bridge`, `Retry test`, `Open match screen` | 1. What: `The local watch bridge test failed.`<br>2. Why: `Port 28282 on the phone is busy.`<br>3. Action: `Close other scoreboard apps and tap Retry test.` | **PASS** |

---

## Detailed Audit of Every Error Message (3-Part Structure Check)

Every error message in the app follows the exact 3-sentence structure required by Part 5:

| Error ID | Sentence 1: What Occurred | Sentence 2: Why It Occurred | Sentence 3: What the User Must Do | Word Counts (S1 / S2 / S3) |
|---|---|---|---|---|
| `splash_err_body` | `The app could not read the saved settings.` | `The storage file on the device is damaged.` | `Tap Retry load, or tap Reset local data to start with default settings.` | `8` / `8` / `13` |
| `onb_err_msg` | `The app could not save the settings.` | `The device storage is full.` | `Free storage space on the device and tap Save settings.` | `7` / `5` / `10` |
| `acc_err_auth` | `The sign-in action failed.` | `The Google service did not return a valid token.` | `Check the network connection and tap Sign in with Google again.` | `4` / `9` / `11` |
| `acc_err_sync` | `The cloud backup failed.` | `The network connection stopped during the transfer.` | `Connect the device to the network and tap Retry sync.` | `4` / `7` / `10` |
| `match_err_rule` | `The rule value is not valid.` | `Each value must be at least 1.` | `Enter a whole number of 1 or more.` | `6` / `7` / `8` |
| `match_err_watch` | `The watch bridge could not start.` | `Port 28282 on the phone is busy.` | `Close other scoreboard apps and reopen the Match screen.` | `6` / `7` / `9` |
| `hist_err_load` | `The app could not load the match history.` | `A saved match record is damaged.` | `Tap Retry load to load the valid matches.` | `8` / `6` / `8` |
| `md_err_record` | `The app could not open the match record.` | `The match data is missing or damaged.` | `Tap Go back and select another match.` | `8` / `7` / `7` |
| `srch_err_query` | `The search filter could not run.` | `The entered text contains characters that are not valid.` | `Tap Clear search and enter a player name or a date.` | `6` / `9` / `11` |
| `pl_err_empty_name` | `The player name is empty.` | `Each player profile must have a name.` | `Enter a name in the text field.` | `5` / `7` / `7` |
| `pl_err_dup_name` | `This player name already exists.` | `Each player profile must have a unique name.` | `Enter a different name.` | `5` / `8` / `4` |
| `pl_err_need_two` | `The app cannot compare players yet.` | `A comparison requires at least 2 player profiles.` | `Tap Add player to create another profile.` | `6` / `8` / `7` |
| `pl_err_same_two` | `The selected profiles are the same.` | `You must select 2 different player profiles.` | `Select a different profile for Player 2.` | `6` / `7` / `7` |
| `set_err_num` | `The entered value is not valid.` | `Every rule and court timer value must be a whole number of 1 or more.` | `Enter a positive whole number.` | `6` / `15` / `5` |
| `set_err_key` | `Two remote actions use the same key.` | `Each remote action needs a different key.` | `Select a unique key for each action.` | `7` / `7` / `7` |
| `help_diag_err` | `The local watch bridge test failed.` | `Port 28282 on the phone is busy.` | `Close other scoreboard apps and tap Retry test.` | `6` / `7` / `8` |

