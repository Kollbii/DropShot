# UI and UX standard

The source of truth for UI is [Android/design](../../Android/design/README.md).
This file gives the short rules. Read the full design file for each screen that you change.

| Reference | Use |
|---|---|
| Material Design 3 | Components, color roles, shape, motion, type scale |
| WCAG 2.2 level AA | Contrast, target size, focus, text alternatives, motion |
| Android accessibility guidelines | TalkBack, `contentDescription`, live regions, font scale |
| ASD-STE100 | All UI text (see [ste-writing.md](ste-writing.md)) |

## 1. Context of use

1. The user puts the phone or tablet on a stand outside the court. The distance is about 2 m to 3 m.
2. The user taps with sweaty hands, or uses a Bluetooth remote control or a watch.
3. The Match screen is the main screen. It must show the score with very large digits.
4. Both orientations must work. Landscape is the main orientation on a tablet.

## 2. Layout and touch targets

1. Make each touch target 48 dp × 48 dp or larger (WCAG 2.5.8 and Material).
2. Put 8 dp or more between targets.
3. Use the 8 dp grid for padding and margins (4 dp for small adjustments).
4. Do not put a destructive action next to a frequent action without space or a confirmation.
5. Keep the bottom navigation with 4 tabs: Match, History, Players, Settings. Do not add a 5th tab.
6. The score panels fill the screen. A tap on a panel adds 1 point. A long press opens Settings.

## 3. Color and contrast

1. Use the `UiTheme` color roles. Do not hardcode new colors in a screen.
2. Text contrast must be 4.5:1 or more. Large text (24 sp or more, or 18.66 sp bold) must be 3:1 or more.
3. Non-text controls and focus outlines must be 3:1 or more against the background.
4. Do not use color as the only signal. Add a symbol or a text (`UiTheme.playerSymbol`, `✓ FINISHED`).
5. Support the light theme, the dark theme, and the high-contrast mode.
6. For player colors, use `playerPanelFill` (white text on top) and `playerAccentOnSurface` (text on the surface).

## 4. Typography

1. Use `sp` for text sizes. Do not use `dp` or `px` for text.
2. Use tabular numbers for scores, times, and statistics (`UiTheme.applyTabularNumbers`).
3. Check the screen at 200% font scale. Text must not overlap. Use `setSingleLine` and ellipsis only for names.

## 5. Accessibility

1. Give each icon-only control a `contentDescription`. Write it in STE (example: `Undo point.`).
2. Give a score panel a full description: player, name, games, points, and the action.
3. Use `ACCESSIBILITY_LIVE_REGION_POLITE` for status text that changes (clock, result).
4. Mark decorative views as not important for accessibility (`CourtAlertView`, `FireworksView`).
5. Keep a logical focus order. Each control must show a visible focus state.
6. Respect the reduce-motion setting (`UiTheme.isReduceMotionEnabled`). Show the final state at once.
7. Do not flash content more than 3 times in 1 second (WCAG 2.3.1).
8. Check each changed screen with TalkBack.

## 6. Interaction and feedback

1. Give feedback for each action in 100 ms or less.
2. Ask for confirmation before a destructive action (delete a match, delete a player, start a new match with points).
3. Show a "Discard unsaved changes?" dialog when the user leaves a changed form.
4. Do not block the score with a dialog during play, except for the confirmations above.
5. Show an empty state with a title, a description, and 1 primary action.
6. Write error messages in 3 parts (what, why, action). Do not blame the user.

## 7. Motion

1. Use short motion: 150 ms to 300 ms for UI changes.
2. Use `AccelerateDecelerateInterpolator` or Material easing.
3. Stop all animations in `onPause` and when the view detaches.
4. Do not use motion as the only signal for a state (game ball has a border and a status text).

## 8. UI text

1. Write all text in STE. Use the glossary terms.
2. Start each button with a verb. Use 1 to 3 words.
3. Write the title of a dialog as a question or a short noun phrase.
4. Put new strings in `res/values/strings.xml` when you create a new screen. The current screens use hardcoded strings (see caveats).

## 9. UI review checklist

- [ ] Light, dark, and high-contrast modes are correct.
- [ ] Portrait and landscape are correct. No text overlap at 200% font scale.
- [ ] All targets are 48 dp or larger.
- [ ] Contrast is 4.5:1 (text) and 3:1 (controls) or more.
- [ ] All icon-only controls have an STE `contentDescription`.
- [ ] Reduce motion stops the animations.
- [ ] Destructive actions ask for confirmation.
- [ ] Text obeys STE and the glossary.

