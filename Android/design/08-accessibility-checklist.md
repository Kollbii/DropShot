# Deliverable 8: Accessibility Checklist (WCAG 2.2 AA & Android Accessibility)

## 1. WCAG 2.2 AA Verification Table

| WCAG 2.2 Criterion | Requirement | DropShot Implementation | Status |
|---|---|---|---|
| **1.1.1 Non-text Content (A)** | Provide text alternatives for all non-text content. | Every icon button, logo, status icon, and chart view has an explicit ASD-STE100 `contentDescription`. Purely decorative overlays (`CourtAlertView`, `FireworksView`) set `IMPORTANT_FOR_ACCESSIBILITY_NO`. | **PASS** |
| **1.3.1 Info and Relationships (A)** | Convey structure and relationships programmatically. | Screen titles use `semantics { heading() }`. Table rows in Players and Match Statistics merge child cells (`mergeDescendants = true`) so TalkBack reads each row as a coherent pair. | **PASS** |
| **1.3.4 Orientation (AA)** | Do not restrict view and operation to a single display orientation. | All screens support both **Portrait** and **Landscape** across `Compact`, `Medium`, and `Expanded` window size classes. | **PASS** |
| **1.4.1 Use of Color (A)** | Do not use color as the only visual means of conveying information. | Player colors are paired with text names (`Blue`, `Red`, `Green`, `Gold`) and distinct chart line styles (`Solid + circle` vs `Dashed + diamond`). Court timer alerts pair border color with icons and text (`OK`, `WARN`, `END`). | **PASS** |
| **1.4.3 Contrast (Minimum) (AA)** | Normal text `>= 4.5:1`; large text `>= 3:1`. | All 200 color pairs across Light, Dark, Light High-Contrast, and Dark High-Contrast themes pass. Normal text pairs range from `5.54:1` to `21.00:1`. | **PASS** |
| **1.4.4 Resize Text (AA)** | Text can be resized up to `200%` without loss of content or functionality. | Minimum body font size is `16 sp`. `DropShotTheme` supports `fontScale` up to `2.0f` (`200%`) and containers use `heightIn(min = ...)` so text wraps instead of clipping. | **PASS** |
| **1.4.10 Reflow (AA)** | Content reflows without two-dimensional scrolling. | At `200%` font scale in Portrait, side-by-side cards and dialog buttons stack into a single vertical column. | **PASS** |
| **1.4.11 Non-text Contrast (AA)** | UI components and graphical objects have at least `3:1` contrast. | `outline` on `surface` is `4.28:1` (Light) and `5.84:1` (Dark); `outline` on `surfaceContainerHighest` is `3.48:1` (Light) and `4.14:1` (Dark). | **PASS** |
| **2.1.1 Keyboard / Switch Access (A)** | All functionality is operable through a keyboard interface. | Every interactive control is focusable via Switch Access, Bluetooth keyboards, and media remote keys. Fullscreen chart zoom and pan also provide `+`, `-`, and `Reset zoom` buttons. | **PASS** |
| **2.2.1 Timing Adjustable (A)** | Do not use a time limit that the user cannot change. | Match scoring has no time limit. The court timer duration (`60 min` default) and warnings (`30 min`, `15 min`, `5 min`) are user-editable in Settings and never lock or close the match when reaching `0:00`. Error/action snackbars do not auto-dismiss while an action button is present. | **PASS** |
| **2.3.3 Animation from Interactions (AAA / System Requirement)** | Respect the system setting "Remove animations". | `rememberReduceMotion()` reads `Settings.Global.ANIMATOR_DURATION_SCALE`. When `0f`, all motion durations become `0 ms`, `FireworksView` is disabled, and the game-ball pulse becomes a static border. | **PASS** |
| **2.4.3 Focus Order (A)** | Focusable components receive focus in an order that preserves meaning. | Focus moves top-to-bottom, start-to-end: Top App Bar -> Status / Filter Bar -> Primary Content (Player 1 -> Player 2) -> Bottom Navigation Bar. | **PASS** |
| **2.4.7 Focus Visible & 2.4.11 Focus Appearance (AA)** | Show a clearly visible focus indicator. | Every focused control renders a `2 dp` solid `primary` ring (`#AF3000` Light / `#FFB59A` Dark, contrast `>= 5.54:1`) with a `2 dp` offset. | **PASS** |
| **2.5.1 Pointer Gestures (A)** | Multipoint or path-based gestures have single-pointer alternatives. | Fullscreen chart pinch-to-zoom is supplemented with single-tap **Zoom in**, **Zoom out**, and **Reset zoom** buttons (`48 dp × 48 dp`). | **PASS** |
| **2.5.3 Label in Name (A)** | For Voice Access, the accessible name starts with the visible text label. | Every button's `contentDescription` matches or starts with its visible STE text (for example, `"Undo point"`, `"Start new match"`, `"Delete match"`). | **PASS** |
| **2.5.8 Target Size (Minimum) & Android Touch Target (AA)** | Touch targets are at least `48 dp × 48 dp`. | Every button, icon button, filter chip, switch, dropdown, and table row enforces a minimum touch target of `48 dp × 48 dp`. | **PASS** |

---

## 2. Content Descriptions for Every Icon and Image

All content descriptions are written in ASD-STE100:

| Screen / Component | Visual Icon or Image | Exact `contentDescription` (ASD-STE100) |
|---|---|---|
| **Splash & Match Top Bar** | DropShot squash ball logo (`R.drawable.squash_logo`) | `DropShot logo.` |
| **Onboarding Step 1** | Scoreboard panels illustration | `Two score panels with the score 11 to 9 and the Undo point button.` |
| **Onboarding Step 2** | Bluetooth remote and Zepp OS watch illustration | `A Bluetooth remote control and a Zepp OS watch with 4 score buttons.` |
| **Onboarding Step 3** | Court timer border and chart illustration | `A court timer warning border and a match statistics chart.` |
| **Top App Bar (All subscreens)** | Left arrow icon (`<-`) | `Go back.` |
| **Match Top Bar** | Pause icon (`Ⅱ`) | `Pause match.` |
| **Match Top Bar** | Play triangle icon (`▶`, when paused) | `Resume match.` |
| **Match Top Bar** | Counter-clockwise undo arrow (`↶`) | `Undo point.` |
| **Match Top Bar** | Sliders / tune icon | `Set up match.` |
| **Match Top Bar** | Circular reset arrow (`↺`) | `New match.` |
| **Match Top Bar (Court Timer Off)** | Clock outline icon | `Start court timer. Court timer is off.` |
| **Match Top Bar (Court Timer Active)** | Clock / warning / alert icon | `Court timer active. Start time %1$s. %2$d min remaining. Status %3$s.` |
| **Match Score Panel (Player 1 / 2)** | Squash ball badge next to points | `Game ball for %1$s.` (or `Match ball for %1$s.`) |
| **Match Score Panel (Player 1 / 2)** | Full interactive score panel | `Player %1$d, %2$s. %3$d games won, %4$d points. Tap panel to add 1 point.` |
| **Match Decorative Overlays** | `CourtAlertView` and `FireworksView` | `null` (`IMPORTANT_FOR_ACCESSIBILITY_NO`; status is announced by the top bar text) |
| **History & Search Top Bar** | Magnifying glass icon | `Search matches.` |
| **Search Input Field** | Trailing `×` icon | `Clear search.` |
| **History Card & Match Detail** | Checkmark circle icon (`FINISHED`) | `Status: Finished.` |
| **History Card & Match Detail** | Clock icon (`UNFINISHED`) | `Status: Unfinished.` |
| **History Card & Player Detail** | Trash can icon | `Delete match.` (or `Delete player.`) |
| **Match Detail Chart Pager** | Point Timeline Chart canvas (`ScoreChartView`) | `Point timeline chart for %1$s versus %2$s. Final game score %3$d to %4$d. Tap chart to open fullscreen.` |
| **Match Detail Chart Pager** | Game Progress Chart canvas (`SetProgressChartView`) | `Game progress chart for %1$s versus %2$s. Tap a game chip to filter.` |
| **Fullscreen Chart** | Close `×` icon | `Close chart.` |
| **Fullscreen Chart** | `+` / `-` / `1:1` zoom icons | `Zoom in.`, `Zoom out.`, `Reset zoom.` |
| **Players List & Detail** | Player color swatch (`■` / `◆` / `▲` / `●`) | `Player color: %1$s.` |
| **Account Screen** | Cloud check / cloud off icon | `Cloud backup status: %1$s.` |
| **Error Banner / Text Field** | Exclamation mark in triangle / circle (`!`) | `Error.` |
| **Bottom Navigation Bar** | Scoreboard / Clock / Group / Gear icons | `Match`, `History`, `Players`, `Settings` |

---

## 3. Assistive Technology Support Matrix

### 3.1 TalkBack (Screen Reader)
- **Live score announcements:** The top status TextView / Composable on the Match screen uses `accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE` so TalkBack announces `"10 to 8. Game ball for Marek"` whenever a point is added via screen tap, Bluetooth remote control, or Zepp OS watch.
- **Grouped table rows:** Each row in the Players table, Player Comparison table, and Match Statistics view uses `Modifier.semantics(mergeDescendants = true) {}` so a single swipe reads the complete metric for both players.
- **Modal focus trapping:** All `AlertDialog` components and the `MatchFinishedView` overlay trap TalkBack focus inside the modal until the user selects an action or cancels.

### 3.2 Switch Access and Bluetooth Keyboard / Remote Navigation
- **Deterministic 2D focus order:** Every screen defines explicit focus traversal groups (`Modifier.focusGroup()`).
- **Single-switch operation:** No task requires a multi-finger gesture, a long-press-and-drag, or a timed double tap. Fullscreen chart panning and zooming provide explicit on-screen buttons (`Zoom in`, `Zoom out`, `Reset zoom`).
- **Visible focus ring:** Focused elements draw a `2 dp` solid `primary` border (`>= 5.54:1` contrast) so Switch Access users always see the active scan target.

### 3.3 Voice Access
- **Exact label match:** Every interactive control displays its verb-first STE label visibly on screen (or exposes the exact visible text at the start of its accessibility label). Voice Access users can say:
  - `"Tap Player 1"` or `"Tap Player 2"`
  - `"Tap Undo point"`
  - `"Tap Pause match"` or `"Tap Resume match"`
  - `"Tap Start court timer"`
  - `"Tap Set up match"`
  - `"Tap New match"`
  - `"Tap History"`, `"Tap Players"`, or `"Tap Settings"`
- **No duplicate ambiguous labels:** In the Match History list, each card's delete button includes the player pairing in its accessible name (`"Delete match Marek versus Anna"`) while still matching `"Tap Delete match"`.

