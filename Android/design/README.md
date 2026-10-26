# DropShot: UI and UX design specification (Android)

Material Design 3 · WCAG 2.2 AA · Simplified Technical English (ASD-STE100) for all in-app text.

## Deliverables (in the requested order)

| # | Deliverable | File |
|---|---|---|
| 1 | Personas and user flows | [01-personas-and-user-flows.md](01-personas-and-user-flows.md) |
| 2 | Information architecture | [02-information-architecture.md](02-information-architecture.md) |
| 3 | Design tokens (table) | [03-design-tokens.md](03-design-tokens.md) |
| 3 | Design tokens (JSON) and the generator script | [03-tokens/design-tokens.json](03-tokens/design-tokens.json), [03-tokens/generate_palette.py](03-tokens/generate_palette.py) |
| 4 | Glossary of approved terms | [04-glossary.md](04-glossary.md) |
| 5 | Screen specifications with the full text | [05-screens.md](05-screens.md) |
| 6 | Component specifications | [06-components.md](06-components.md) |
| 7 | Jetpack Compose theme code | [07-compose-theme/Color.kt](07-compose-theme/Color.kt), [Type.kt](07-compose-theme/Type.kt), [Theme.kt](07-compose-theme/Theme.kt), [README](07-compose-theme/README.md) |
| 8 | Accessibility checklist | [08-accessibility-checklist.md](08-accessibility-checklist.md) |
| 9 | STE checklist for each screen | [09-ste-checklist.md](09-ste-checklist.md) |

## App data and assumptions

The prompt had placeholders. I filled them from the code in this repository (`Android/`, `ZeppOS/`, `assets/logos/`). Each assumption is one sentence.

| Item | Value | Assumption / source |
|---|---|---|
| App name | **DropShot** | I used the repository name and the README title, and it replaces the current launcher label "Punkty Squash". |
| App purpose | Keep the score of a squash match with the screen, a Bluetooth remote control, or a Zepp OS watch, and record the matches and the player statistics. | Taken from `MainActivity`, `WatchCommandServer`, `MatchHistoryStore`, and `PlayerStatistics`. |
| Target users | Recreational and club squash players, 18 to 65 years old, low to medium technical skill, who put a phone or a tablet on a stand outside the court. | I assume this from the README ("user_count: 2") and from the landscape, large-digit design of the current app. |
| Task 1 | Keep the score of a match. | Main loop of `MainActivity` (tap, remote key, watch command, undo, pause). |
| Task 2 | Set up and start a match (players, colors, rules). | `ProfileStore`, `MatchState.configure`. |
| Task 3 | Look at the match history and the player statistics. | `HistoryActivity`, `MatchDetailActivity`, `PlayerStatsActivity`, `PlayerComparisonActivity`. |
| Secondary task | Use the court timer (time of the court rental). | `AppPreferences.court*`, `CourtAlertView`. |
| Minimum Android version | Android 8.0 (API 26) | Given; the current `minSdk 23` must change to `26`. |
| Sign-in | Optional Google sign-in, only for backup. | The current code has no account, so I assume an optional backup feature and every other function works without an account. |
| Squash term | "game" (not "set") | The World Squash rules use "game", so the UI uses "game"; code names such as `pointsToWinSet` do not change. |
| Default rules | 11 points to win a game, a 2-point lead, 3 games to win the match. | Taken from `MatchState`. |
| History limit | 100 matches | Taken from `MatchHistoryStore.add` (the oldest match is deleted after 100). |
| Language | English (STE). | The current UI is Polish; translation is out of scope, and all strings go to `strings.xml`. |

## Safety and accuracy rules that the design keeps from the current code

These behaviors protect the score. The design must not change them.

1. The watch does not send a command again after a lost connection. The app must not add a late point.
2. The local watch bridge listens only on `127.0.0.1:28282`, and only while the match screen is open.
3. The volume keys change the score only while the match screen is open.
4. "Start a new match" always asks for confirmation when the current match has points.
5. The app saves the current match in the match history before it starts a new match.
6. When a player is deleted, the matches of that player stay in the match history (unless the user selects the extra option).

## Important note about STE validation

> The ASD-STE100 dictionary is a licensed document. I wrote the strings with the STE rules and I mark each glossary term as an approved word, a technical name (TN), or a technical verb (TV). Before release, check the final `strings.xml` with the official dictionary or an STE checker tool.

