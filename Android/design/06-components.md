# Deliverable 6: Component Specifications (Material Design 3)

Every component specification defines:
- **Dimensions and layout rules** (`8 dp` grid and `48 dp × 48 dp` minimum touch target)
- **Color roles** (Light theme and Dark theme tokens from Deliverable 3)
- **All 5 interactive states:** `Enabled`, `Pressed`, `Focused`, `Disabled`, and `Error`

### Universal State Layer and Focus Ring Rule
Across all interactive Material 3 components in DropShot:
- **Pressed state:** Adds a state layer overlay using the component's content color (`onPrimary`, `onSurface`, or `primary`) at **10% opacity** (`0x1A` alpha).
- **Focused state (TalkBack, Switch Access, Bluetooth keyboard / remote D-pad):** Adds a **10% opacity** state layer **and** a high-visibility **`2 dp` solid focus ring** in `primary` (`#AF3000` in Light, `#FFB59A` in Dark, `>= 5.54:1` contrast against surfaces) with a `2 dp` offset.
- **Disabled state:** Container uses `onSurface` at **12% opacity** (`0x1F` alpha), and text/icons use `onSurface` at **38% opacity** (`0x61` alpha). Touch and key events are blocked.
- **Error state:** Uses `error` (`#BF0312` Light / `#FFB4A5` Dark) or `errorContainer` (`#FFDAD3` Light / `#93000B` Dark) paired with a leading warning/error icon and an explicit STE text label or message.

---

## 1. Top App Bar (`TopAppBar` / Match Header Bar)

### 1.1 Sizes and Layout
- **Standard screens (`SmallTopAppBar`):** Height `64 dp` (expands vertically if title wraps at `200%` font scale), horizontal padding `16 dp`, gap between navigation icon and title `16 dp`, gap between trailing action icons `8 dp`.
- **Match screen compact header:** Height `56 dp` in landscape, `64 dp` in portrait; contains the app logo (`36 dp × 36 dp`), live status text (`clock` / `titleSmall`), court timer button (`48 dp` height), and 4 action buttons (`48 dp × 48 dp` each).
- **Touch targets:** Navigation back button and all trailing action buttons are `48 dp × 48 dp` minimum.

### 1.2 Colors and States
| State | Container Color | Title / Icon Color | Border / Indicator |
|---|---|---|---|
| **Enabled (At top)** | `surface` (`#FDF9F2` L / `#16130C` D) | `onSurface` (`#1E1B16` L / `#E6E2DB` D) | Elevation `level0` (`0 dp`) |
| **Enabled (Scrolled)** | `surfaceContainer` (`#F1EDE6` L / `#221F1A` D) | `onSurface` (`#1E1B16` L / `#E6E2DB` D) | Elevation `level2` (`3 dp`) |
| **Pressed (Action button)** | `surface` + `onSurface` at `10%` opacity | `onSurface` | Circular `40 dp` ripple inside `48 dp` target |
| **Focused (Action button)** | `surface` + `onSurface` at `10%` opacity | `onSurface` | `2 dp` solid `primary` focus ring (`8 dp` radius) |
| **Disabled (e.g. Undo at 0:0)** | Transparent | `onSurface` at `38%` opacity | None |
| **Error (Court timer END)** | `errorContainer` (`#FFDAD3` L / `#93000B` D) on court badge | `onErrorContainer` (`#380C00` L / `#FFDAD3` D) | `2 dp` solid `error` border + alert icon + `END` text |

---

## 2. Bottom Navigation Bar (`NavigationBar` and Landscape `NavigationRail`)

### 2.1 Sizes and Layout
- **Maximum items:** **4 items** (`Match`, `History`, `Players`, `Settings`) — strictly within the 5-item maximum.
- **Portrait (`NavigationBar`):** Height `80 dp` (plus bottom system bar inset), each item takes `25%` of screen width (`>= 80 dp × 80 dp` touch target). Active pill indicator size: `64 dp × 32 dp`, corner radius `full` (`16 dp`), icon size `24 dp × 24 dp`, label `labelMedium` (`14 sp`, scales up to `200%`).
- **Landscape / Medium / Expanded (`NavigationRail`):** Width `80 dp` on the start edge, item touch target `64 dp × 56 dp`, vertical spacing `12 dp`.

### 2.2 Colors and States
| State | Bar Container | Active Pill Indicator | Icon & Label Color | Focus / Error Treatment |
|---|---|---|---|---|
| **Enabled (Selected)** | `surfaceContainer` (`#F1EDE6` L / `#221F1A` D) | `secondaryContainer` (`#FFDCC5` L / `#5B412F` D) | Icon: `onSecondaryContainer` (`#2C1700` L / `#FFDCC5` D); Label: `onSurface` | Filled icon variant |
| **Enabled (Unselected)** | `surfaceContainer` | Transparent | `onSurfaceVariant` (`#4C4639` L / `#CEC6B6` D) | Outlined icon variant |
| **Pressed** | `surfaceContainer` | `secondaryContainer` + `10%` `onSurface` | `onSecondaryContainer` | Ripple clipped to pill |
| **Focused** | `surfaceContainer` | `secondaryContainer` + `10%` `onSurface` | `onSecondaryContainer` | `2 dp` solid `primary` ring around item |
| **Disabled** | `surfaceContainer` | Transparent | `onSurface` at `38%` opacity | Item not clickable |
| **Error (Sync badge on Settings)** | `surfaceContainer` | `error` badge (`16 dp` circle with `!`) on Settings icon | Badge text: `onError` (`#FFFFFF` L / `#690000` D) | Announces `"Settings, 1 error"` in TalkBack |

---

## 3. Button (`Button`, `FilledTonalButton`, `OutlinedButton`, `IconButton`, and Score Panel Button)

### 3.1 Sizes and Layout
- **Standard buttons (`Button`, `FilledTonalButton`, `OutlinedButton`):** Minimum height `48 dp`, minimum width `64 dp`, horizontal padding `24 dp` (`16 dp` when leading `18 dp` icon is present), icon-to-label gap `8 dp`, corner radius `full` (`CircleShape`), label `labelLarge` (`16 sp`, Medium `500`).
- **Danger button (`Delete match`, `Delete player`):** Same dimensions as `FilledTonalButton`, colored with `error` / `errorContainer`.
- **Icon button (`IconButton`):** Touch target `48 dp × 48 dp`, visible container `40 dp × 40 dp`, icon `24 dp × 24 dp`, corner radius `full`.
- **Score Panel button (Player 1 and Player 2 panels):** Fills `50%` of the scoreboard width (`>= 240 dp × 200 dp` in landscape), corner radius `large` (`16 dp`), internal padding `16 dp`.

### 3.2 Colors and States (Filled Primary Button)
| State | Container Color | Label & Icon Color | Border / Elevation |
|---|---|---|---|
| **Enabled** | `primary` (`#AF3000` L / `#FFB59A` D) | `onPrimary` (`#FFFFFF` L / `#5B1B00` D) | Elevation `level0` (`0 dp`) |
| **Pressed** | `primary` + `10%` `onPrimary` overlay | `onPrimary` | Elevation `level0`, `100 ms` press transition |
| **Focused** | `primary` + `10%` `onPrimary` overlay | `onPrimary` | `2 dp` solid `onSurface` / `primary` outer focus ring |
| **Disabled** | `onSurface` at `12%` opacity | `onSurface` at `38%` opacity | Elevation `level0` |
| **Error (Destructive / Invalid)** | `error` (`#BF0312` L / `#FFB4A5` D) | `onError` (`#FFFFFF` L / `#690000` D) | Leading warning/delete icon required |

---

## 4. Text Field (`OutlinedTextField` and `ExposedDropdownMenuBox`)

### 4.1 Sizes and Layout
- **Minimum height:** `56 dp` for the input box + `20 dp` supporting/error text line below (`76 dp` total height when supporting text is visible).
- **Corner radius:** `extraSmall` (`4 dp`) on the outlined container.
- **Internal padding:** `16 dp` start and end, `12 dp` top and bottom.
- **Typography:** Input text uses `bodyLarge` (`18 sp`); floating label uses `bodySmall` (`16 sp` when resting, `14 sp` when floated); supporting and error text use `bodySmall` (`16 sp`).
- **Touch target:** Full field width (`>= 200 dp`), height `56 dp` (`>= 48 dp`). Trailing clear or dropdown icon button has a `48 dp × 48 dp` touch target.

### 4.2 Colors and States
| State | Container Fill | Border Stroke | Label & Supporting Text | Input Text & Cursor |
|---|---|---|---|---|
| **Enabled (Unfocused)** | `surface` | `1 dp` solid `outline` (`#7E7668` L / `#989081` D) | `onSurfaceVariant` (`#4C4639` L / `#CEC6B6` D) | `onSurface` (`#1E1B16` L / `#E6E2DB` D) |
| **Pressed** | `surface` + `4%` `onSurface` | `1 dp` solid `onSurface` | `onSurfaceVariant` | `onSurface` |
| **Focused** | `surface` | `2 dp` solid `primary` (`#AF3000` L / `#FFB59A` D) | `primary` | `onSurface`, cursor `primary` |
| **Disabled** | `onSurface` at `4%` opacity | `1 dp` solid `onSurface` at `12%` opacity | `onSurface` at `38%` opacity | `onSurface` at `38%` opacity |
| **Error** | `surface` | `2 dp` solid `error` (`#BF0312` L / `#FFB4A5` D) | `error` + trailing error icon (`!`) | `onSurface`, cursor `error`, 3-part STE error message below |

---

## 5. Card (`ElevatedCard` and `OutlinedCard`)

### 5.1 Sizes and Layout
- **Corner radius:** `medium` (`12 dp`) for list cards and settings groups; `large` (`16 dp`) for score panels and chart containers.
- **Internal padding:** `16 dp` on Compact screens, `24 dp` on Medium/Expanded screens.
- **Minimum height:** `72 dp` for summary cards, `88 dp` for match history cards (expands automatically with font scale).
- **Vertical spacing between cards:** `8 dp` in lists, `16 dp` between sections.

### 5.2 Colors and States
| State | Container Color | Content Color | Border & Elevation |
|---|---|---|---|
| **Enabled (`ElevatedCard`)** | `surfaceContainerLow` (`#F7F3EC` L / `#1E1B16` D) | `onSurface` / `onSurfaceVariant` | Elevation `level1` (`1 dp`), no border |
| **Enabled (`OutlinedCard`)** | `surface` (`#FDF9F2` L / `#16130C` D) | `onSurface` / `onSurfaceVariant` | `1 dp` solid `outlineVariant`, elevation `level0` |
| **Pressed (Interactive card)** | Container + `10%` `onSurface` overlay | `onSurface` | Elevation `level2` (`3 dp`) |
| **Focused** | Container + `10%` `onSurface` overlay | `onSurface` | `2 dp` solid `primary` focus border |
| **Disabled** | `surfaceVariant` at `38%` opacity | `onSurface` at `38%` opacity | Elevation `level0` |
| **Error (Corrupted record card)** | `errorContainer` (`#FFDAD3` L / `#93000B` D) | `onErrorContainer` (`#380C00` L / `#FFDAD3` D) | `2 dp` solid `error` border + error icon |

---

## 6. List Item (`ListItem` / Player Table Row / History Row)

### 6.1 Sizes and Layout
- **1-line item:** Minimum height `56 dp`.
- **2-line item (Player row / Match row):** Minimum height `72 dp` (`88 dp` when a 3rd metadata line is present).
- **Padding:** `16 dp` horizontal, `12 dp` vertical, `16 dp` gap between leading badge/avatar (`40 dp × 40 dp`) and text column.
- **Trailing action:** Minimum `48 dp × 48 dp` touch target separated by at least `8 dp` from the main row content.

### 6.2 Colors and States
| State | Container Color | Headline & Supporting Text | Leading / Trailing Icons |
|---|---|---|---|
| **Enabled** | `surface` or `surfaceContainerLow` | Headline: `onSurface` (`18 sp`); Supporting: `onSurfaceVariant` (`16 sp`) | `onSurfaceVariant` |
| **Pressed** | Container + `10%` `onSurface` overlay | `onSurface` | `onSurface` |
| **Focused** | Container + `10%` `onSurface` overlay | `onSurface` + `2 dp` `primary` focus outline | `primary` |
| **Disabled** | Transparent | `onSurface` at `38%` opacity | `onSurface` at `38%` opacity |
| **Error** | `errorContainer` | `onErrorContainer` | `error` warning icon + error description |

---

## 7. Dialog (`AlertDialog` and Full-Screen Match Finished Overlay)

### 7.1 Sizes and Layout
- **Standard `AlertDialog`:** Width `min(calc(100vw - 32 dp), 560 dp)` (minimum `280 dp`, maximum `560 dp`), corner radius `extraLarge` (`28 dp`), internal padding `24 dp`.
- **Spacing:** Title to body `16 dp`; body to action buttons `24 dp`; gap between action buttons `8 dp`. Action buttons stack vertically when text scale is `>= 150%` so labels never truncate.
- **Touch targets:** Every dialog button (`Cancel action`, `Start new match`, `Delete match`, `Save player`) has a minimum height of `48 dp`.

### 7.2 Colors and States
| State | Dialog Surface | Scrim Behind Dialog | Title & Body Color | Action Buttons |
|---|---|---|---|---|
| **Enabled** | `surfaceContainerHigh` (`#ECE8E0` L / `#2C2A24` D), elevation `level3` (`6 dp`) | `scrim` (`#000000`) at `50%` opacity | Title: `onSurface` (`headlineSmall` `24 sp`); Body: `onSurfaceVariant` (`bodyMedium` `16 sp`) | Confirm: `primary` filled button; Cancel: `primary` text button |
| **Pressed (Button)** | Same | Same | Same | `10%` state layer on pressed button |
| **Focused (Button / Field)** | Same | Same | Same | `2 dp` solid `primary` focus ring on focused control; initial focus lands on the safe non-destructive button or first input field |
| **Disabled (Confirm button)** | Same | Same | Same | Confirm button uses `12%` container / `38%` text when inputs are invalid |
| **Error (Validation inside dialog)** | Same | Same | Inline error box in `errorContainer` with `onErrorContainer` text and error icon | Confirm button remains disabled until the input is valid |

---

## 8. Snackbar (`Snackbar`)

### 8.1 Sizes and Layout
- **Height:** `48 dp` minimum (single line) to `68 dp` (2 lines, or 3 lines at `200%` text scale).
- **Margins:** `16 dp` from start, end, and above the bottom navigation bar (`Max width 560 dp` on tablets).
- **Corner radius:** `small` (`8 dp`), elevation `level3` (`6 dp`).
- **Action button:** Minimum `48 dp × 48 dp` touch target on the trailing edge.
- **No forced timeout with action:** When a snackbar contains an action button (such as **Retry sync** or **Save settings**), it remains visible until the user taps the action or dismisses it, satisfying WCAG 2.2 AA (No Timing).

### 8.2 Colors and States
| State | Container Color | Message Text Color | Action Button Color |
|---|---|---|---|
| **Enabled (Default / Success)** | `inverseSurface` (`#33302B` L / `#E6E2DB` D) | `inverseOnSurface` (`#F4F0E9` L / `#33302B` D, contrast `11.57:1`) | `inversePrimary` (`#FFB59A` L / `#AF3000` D) |
| **Pressed (Action button)** | `inverseSurface` | `inverseOnSurface` | `inversePrimary` + `10%` state layer |
| **Focused (Action button)** | `inverseSurface` | `inverseOnSurface` | `2 dp` solid `inverseOnSurface` focus border |
| **Disabled** | Not applicable (snackbars are not shown in a disabled state) | — | — |
| **Error (Error snackbar)** | `errorContainer` (`#FFDAD3` L / `#93000B` D) with leading error icon | `onErrorContainer` (`#380C00` L / `#FFDAD3` D) | `onErrorContainer` bold action label |

---

## 9. Chip (`FilterChip` and `AssistChip`)

### 9.1 Sizes and Layout
- **Touch target height:** `48 dp` (`36 dp` visible chip box centered inside a `48 dp` vertical touch target).
- **Horizontal padding:** `16 dp` (`12 dp` on the leading side when the `18 dp` checkmark icon is visible).
- **Corner radius:** `small` (`8 dp`).
- **Typography:** `labelLarge` (`16 sp`, Medium `500`).
- **Non-color signal:** Selected filter chips always show a leading `18 dp` checkmark icon in addition to the container color change.

### 9.2 Colors and States
| State | Unselected Container & Border | Selected Container & Border | Label & Icon Color |
|---|---|---|---|
| **Enabled** | Transparent fill, `1 dp` `outline` border (`#7E7668` L / `#989081` D) | `secondaryContainer` (`#FFDCC5` L / `#5B412F` D), `0 dp` border + checkmark icon | Unselected: `onSurfaceVariant`; Selected: `onSecondaryContainer` |
| **Pressed** | `10%` `onSurfaceVariant` overlay | `secondaryContainer` + `10%` `onSecondaryContainer` overlay | Same |
| **Focused** | `2 dp` solid `primary` border | `secondaryContainer` + `2 dp` solid `primary` border | Same |
| **Disabled** | Transparent fill, `1 dp` `onSurface` (`12%`) border | `onSurface` (`12%`) fill | `onSurface` at `38%` opacity |
| **Error** | `errorContainer` fill, `1 dp` `error` border | `errorContainer` fill, `2 dp` `error` border | `onErrorContainer` + leading error icon |

---

## 10. Switch (`Switch`)

### 10.1 Sizes and Layout
- **Track dimensions:** `52 dp` wide × `32 dp` high, corner radius `full` (`16 dp`).
- **Touch target:** `48 dp × 48 dp` minimum around the switch; in Settings rows, the entire row (`height >= 56 dp`) toggles the switch when tapped.
- **Thumb dimensions:** `16 dp × 16 dp` when unchecked (or `24 dp × 24 dp` when an `×` icon is shown inside the unchecked thumb), `24 dp × 24 dp` with a `16 dp` checkmark icon when checked, expanding to `28 dp × 28 dp` when pressed.
- **Non-color signal:** The checked thumb always contains a checkmark (`✓`) icon and the unchecked thumb is smaller and sits on the start side.

### 10.2 Colors and States
| State | Track Color | Thumb Color | Thumb Icon & Border |
|---|---|---|---|
| **Enabled (Checked / On)** | `primary` (`#AF3000` L / `#FFB59A` D) | `onPrimary` (`#FFFFFF` L / `#5B1B00` D) | Checkmark icon in `onPrimaryContainer`, no track border |
| **Enabled (Unchecked / Off)** | `surfaceContainerHighest` (`#E6E2DB` L / `#37342F` D) | `outline` (`#7E7668` L / `#989081` D) | `2 dp` solid `outline` track border |
| **Pressed** | Track + `10%` state halo (`40 dp` circle around thumb) | Thumb expands to `28 dp × 28 dp` | Same |
| **Focused** | Track + `2 dp` solid `primary` outer focus ring | Same | Same |
| **Disabled** | `surfaceVariant` at `12%` opacity | `onSurface` at `38%` opacity | Border `onSurface` at `12%` opacity |
| **Error** | `errorContainer` | `error` | Warning `!` icon inside thumb |

---

## 11. Progress Indicator (`CircularProgressIndicator` and `LinearProgressIndicator`)

### 11.1 Sizes and Layout
- **`CircularProgressIndicator`:** Diameter `48 dp × 48 dp` (full-screen loading) or `24 dp × 24 dp` (inline inside a button), stroke width `4 dp`, rounded stroke caps.
- **`LinearProgressIndicator`:** Full container width, height `4 dp` (`8 dp` in high-contrast mode), corner radius `full` (`2 dp`), `4 dp` stop indicator gap between active bar and track.
- **Accompanying text:** Every progress indicator is paired with a visible status label (for example, `Loading local match data.` or `Syncing match data with the cloud backup.`).

### 11.2 Colors and States
| State | Active Indicator Color | Track Color | Behavior under "Remove animations" |
|---|---|---|---|
| **Enabled (Indeterminate / Determinate)** | `primary` (`#AF3000` L / `#FFB59A` D) | `secondaryContainer` (`#FFDCC5` L / `#5B412F` D) | When `ANIMATOR_DURATION_SCALE == 0`, indeterminate rotation stops and shows a static `60%` arc + status text |
| **Pressed** | Not interactive | — | — |
| **Focused (When focusable for TalkBack)** | `primary` | `secondaryContainer` + `2 dp` focus outline | Announces current step or status text |
| **Disabled / Paused** | `onSurface` at `38%` opacity | `onSurface` at `12%` opacity | Static bar |
| **Error (Failed transfer)** | `error` (`#BF0312` L / `#FFB4A5` D) | `errorContainer` (`#FFDAD3` L / `#93000B` D) | Stops animation and shows the error icon and 3-part STE error message |

