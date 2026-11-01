# AndroidWatch UI/UX Design Ideas and Evaluation

This document records the user interface and user experience design ideas for the `AndroidWatch` application.
Use this document to evaluate and select future improvements.

---

## 1. Hardware Constraints and Context

* **Target device**: LG G Watch (model LG-W100).
* **Display**: Square 280 x 280 pixels, IPS LCD backlight.
* **Android version**: API level 20 (Android 4.4W).
* **Environment**: Fast squash match. The player has sweaty hands, rapid movements, and short glance times (< 1 s).
* **Accessibility standard**: WCAG 2.2 AA (minimum 48 dp touch target).

---

## 2. Bluetooth Connection Indicator Ideas

The table summarizes the 4 design options for showing connection status:

| Idea | Concept | Status | Touch Target |
|---|---|---|---|
| **Idea 1** | Micro Dot in Top Notch | **Implemented** | 48 x 40 dp (hit box) |
| **Idea 2** | 70/30 Divider Hairline | Proposed | Full-width line |
| **Idea 3** | Bottom Edge Status Stripe | Proposed | Bottom edge |
| **Idea 4** | Bezel Glow on Disconnect | Proposed | None (automatic) |

### 2.1 Idea 1: Micro Dot in Top Notch (Implemented)

* **Description**:
  A compact 8 dp circular dot sits in the top center notch between the player score tiles.
* **Visual states**:
  * Connected: Solid emerald green dot (`#39B87F`).
  * Reconnecting: Solid gold dot (`#FFD54F`).
  * Disconnected: Solid crimson red dot (`#F1636E`) with a white rim.
* **Touch behavior**:
  An invisible 48 x 40 dp touch container surrounds the dot.
  Touching the container triggers a 35 ms vibration and starts reconnection.
* **Advantages**:
  * Saves 60 dp of horizontal space compared to a text pill.
  * Initials and set dots do not truncate.
  * Meets the WCAG 2.2 touch target size requirement (48 dp).
* **Disadvantages**:
  * The user must know that the dot is touchable to reconnect manually.

---

### 2.2 Idea 2: 70/30 Divider Hairline (Proposed)

* **Description**:
  A 2 dp horizontal line separates the top score tiles (70%) from the bottom buttons (30%).
* **Visual states**:
  * Connected: Dark slate or muted green line.
  * Disconnected: Bright crimson red line with a small centered `[OFFLINE]` badge.
* **Touch behavior**:
  Tapping the line triggers reconnection.
* **Advantages**:
  * Gives a clear visual boundary between scoring and controls.
  * Does not use space in the player header area.
* **Disadvantages**:
  * Can reduce touch precision between top tiles and bottom buttons.
  * Increases visual clutter in the screen center.

---

### 2.3 Idea 3: Bottom Edge Status Stripe (Proposed)

* **Description**:
  A 3 dp colored stripe runs along the bottom screen edge below `UNDO` and `STOP`.
* **Visual states**:
  * Connected: Invisible (0% opacity).
  * Disconnected: Red glowing bar with a short "TAP TO RECONNECT" label.
* **Touch behavior**:
  Tapping the bottom stripe triggers reconnection.
* **Advantages**:
  * Follows the "Status by Exception" rule. The screen stays clean during normal play.
  * Keeps the game score as the main visual element.
* **Disadvantages**:
  * The bottom edge of square watch cases can hide thin lines.
  * Tapping near the bottom edge can cause accidental palm touches.

---

### 2.4 Idea 4: Bezel Glow on Disconnect (Proposed)

* **Description**:
  The watch shows no connection badge during normal play.
  When the connection drops, a 3 dp red border frames the screen and the watch vibrates once.
* **Visual states**:
  * Connected: Standard screen layout with zero connection icons.
  * Disconnected: Red outer border around all 4 edges.
* **Touch behavior**:
  Tapping anywhere on the screen while disconnected initiates reconnection.
* **Advantages**:
  * Zero screen clutter during active play.
  * The player notices disconnection immediately through peripheral vision.
* **Disadvantages**:
  * Shows no visual confirmation while the app is in the connected state.

---

## 3. Squash Service Side Selection Ideas

Squash rules (rules 5.1 to 5.3) permit the server to choose the service box (`R` or `L`) at match start, after a handout, and at the start of a new game.
The table compares the 5 interaction ideas:

| Idea | Interaction | Status | Eyes-Free Usability |
|---|---|---|---|
| **Idea A** | Pre-Match Setup Modal | Proposed | Low (requires screen reading) |
| **Idea B** | Serve Toggle Icon | Proposed | Medium (small touch target) |
| **Idea C** | Double-Tap Service Side | Proposed | Medium (can conflict with fast taps) |
| **Idea D** | 1-Second Hold on Server Tile | **Implemented** | High (distinct double haptic) |
| **Idea E** | Horizontal Swipe Gesture | Proposed | Medium (accidental swipe risk) |

### 3.1 Idea A: Pre-Match Setup Modal (Proposed)

* **Description**:
  Before the match starts (at 0-0), a full-screen dialog asks the user to pick the first server and side (`P1-L`, `P1-R`, `P2-L`, `P2-R`).
* **Advantages**:
  * Makes the initial serve side explicit before play starts.
* **Disadvantages**:
  * Blocks immediate play.
  * Does not solve mid-game handouts when the server switches.

---

### 3.2 Idea B: Serve Toggle Icon (Proposed)

* **Description**:
  A small icon button (`⇄` or `<>`) displays next to the `[R]` / `[L]` text on the server tile.
* **Advantages**:
  * Clear visual affordance.
* **Disadvantages**:
  * The touch target is smaller than 48 dp on a 136 dp wide tile.
  * High risk of accidental point taps.

---

### 3.3 Idea C: Double-Tap Service Side (Proposed)

* **Description**:
  Double-tapping the active server tile within 300 ms switches between `[R]` and `[L]`.
* **Advantages**:
  * Faster than a 1-second hold.
* **Disadvantages**:
  * Introduces a 300 ms tap delay for single point taps.
  * Can misinterpret rapid point taps during referee testing.

---

### 3.4 Idea D: 1-Second Hold on Active Server Tile (Implemented)

* **Description**:
  Pressing and holding the active server tile for 1.0 second toggles the service side between `[R]` and `[L]`.
  A `[CHANGING...]` gold text indicator displays after 200 ms.
  When 1.0 second elapses, a double haptic pulse confirms the toggle.
  Releasing before 1.0 second registers a regular point tap.
* **Grace period protection**:
  A 1.5-second touch grace period activates upon side toggle and on finger lift.
  The grace period absorbs finger bounce and prevents accidental point registration when releasing or tapping immediately after a switch.
* **Advantages**:
  * Completely eyes-free operation.
  * No extra buttons or clutter.
  * Normal single taps stay instant (no tap-detection delay).
  * Only applies to the active server tile.
  * Grace period eliminates phantom points upon finger release.
* **Disadvantages**:
  * The player must wait 1.0 second to change the side.

---

### 3.5 Idea E: Horizontal Swipe Gesture (Proposed)

* **Description**:
  Swiping left or right across the active server tile toggles between `[L]` and `[R]`.
* **Advantages**:
  * Very fast gesture.
* **Disadvantages**:
  * Wet or sweaty fingers cause gesture recognition errors.
  * Can conflict with watch system swipe-to-dismiss gestures.

---

## 4. Evaluation Summary and Next Steps

1. **Connection indicator**:
   Idea 1 (Micro Dot) provides the best balance between clean layout and WCAG touch target compliance.
   If players prefer zero visual indicators during play, evaluate Idea 4 (Bezel Glow on Disconnect).

2. **Service side selection**:
   Idea D (1-Second Hold) is currently active and protects against accidental taps.
   Field testing during active squash matches will verify if the 1.0 second duration is optimal or if 0.8 seconds is faster and safe.

