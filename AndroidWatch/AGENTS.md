# AGENTS.md — AndroidWatch

These rules apply to `AndroidWatch/`. They add to the root [AGENTS.md](../AGENTS.md).

## 1. Build configuration

| Item | Value |
|---|---|
| Module | Standalone application (`AndroidWatch/`) |
| Package | `pl.maciek.squashscoreboard.watch` |
| SDK | `minSdk 20` (Android 4.4W), `targetSdk 22`, `compileSdk 35` |
| Target hardware | LG G Watch (model LG-W100, 280 × 280 square display) and Android Wear 1.x / 2.x devices |
| Java | Source and target 11 |
| AGP | 8.7.3 |
| Dependencies | None at runtime. JUnit 4.13.2 for unit tests only. |
| UI toolkit | Standard Android framework views built in Java code (`Activity`, `LinearLayout`, `FrameLayout`, `GradientDrawable`). |

> [!IMPORTANT]
> Do not add AndroidX, Wearable Support libraries, or Google Play Services dependencies without a request.
> Keep `minSdk 20` compatibility so the APK installs and runs on Android 4.4W watches.

## 2. Architecture

| Class | Responsibility |
|---|---|
| `MainActivity` | Fullscreen watch UI, 75/25 split layout, touch slop and left-edge swipe handling, haptics, reaction animations, and brightness control. |
| `BluetoothWatchClient` | Persistent Bluetooth RFCOMM client socket, exponential reconnect backoff (`3 s`, `5 s`, `10 s`), single-shot command dispatch without retry. |
| `WatchProtocol` | Pure Java command validation, JSON line encoding/parsing, game-dot formatting, gradient stop lookup, and gesture math. |
| `WatchProtocolTest` | JVM unit tests for all protocol parsing, gradient stops, backoff delays, edge-swipe detection, touch slop, and reaction scores. |

## 3. Bluetooth RFCOMM protocol rules

1. **Service UUID**: `d8095407-4b6d-4765-9b3f-282820000001` (`DropShotWatchBridge`).
2. **No retries**: `BluetoothWatchClient.sendCommand` must never queue, replay, or retry a failed command. A late point is forbidden.
3. **Permitted commands** (watch → phone):
   `POINT_1`, `POINT_2`, `UNDO`, `RESET`, `TOGGLE_PAUSE`, `TOGGLE_SIDE`, `SWITCH_SERVER` (encoded as `{"command":"<CMD>"}\n`).
4. **Incoming messages** (phone → watch):
   - `STATE`: Live score, initials (`n1`, `n2`), points (`p1`, `p2`), games (`s1`, `s2`, `targetSets`), server (`srv`), serve side (`side`), side-choice unlocked (`canPickSide`), starting-server choice unlocked (`canPickSrv`), colors (`c1`, `r1`, `c2`, `r2`), action label (`act`), game/match ball (`ball`), finished (`fin`), and paused (`paused`).
   - `ACK`: Command acknowledgement (`{"type":"ACK","accepted":true,"ball":false}`).
   - `MATCH_ENDED`: Winner overlay trigger (`{"type":"MATCH_ENDED","winner":"...","score":"2 - 0"}`).
5. **Bounded reads**: Keep the 1,024-byte line cap in `readBoundedLine` to prevent out-of-memory errors.
6. **Synchronized writes**: Keep `WatchProtocol` constants and JSON keys synchronized with `BluetoothWatchServer.java` in `Android/app`.

## 4. UI and interaction rules

1. **75 / 25 vertical screen split**:
   - Top 75% (`layout_weight="75"`): Player 1 and Player 2 score tiles.
   - Bottom 25% (`layout_weight="25"`): `UNDO` button and `START` / `STOP` / `NEXT` action button.
2. **Starting server selection (`[SERVE]`) and streak-locked serve-side badge (`[R]` / `[L]`)**:
   - Each player tile has a dedicated full-width × `30 dp` service badge container at the bottom.
   - At `0-0` points and `0-0` games (`canPickSrv: true`), the starting server's tile shows `[R]` or `[L]` and the non-server's tile shows `[SERVE]`. Tapping `[SERVE]` sends `SWITCH_SERVER` to switch the starting server.
   - When `canPickSide: true` (at `0-0` or immediately after winning a handout), tapping the active server's `[R]` / `[L]` badge container sends `TOGGLE_SIDE` and vibrates a double-haptic pulse (`0, 30, 45, 30 ms`).
   - When `canPickSide: false` (after the server scores a point on their own serve), the `[R]` / `[L]` badge shows a dimmed red background (`0x66B71C1C`), disables manual clicking/switching, and alternates automatically (`[R]` ↔ `[L]`) on each consecutive point won.
3. **Action button hold for reset**:
   - Tapping the bottom-right button (`START` / `STOP` / `NEXT`) sends `TOGGLE_PAUSE`.
   - Holding the button shows `HOLD 2S` after `400 ms` and sends `RESET` at `2,000 ms`.
4. **Left-edge swipe-to-exit and touch slop**:
   - A touch starting at `x <= 36 dp` and moving right by `dx >= 70 dp` (`dx > 1.5 * dy`) dispatches `ACTION_CANCEL` to child views and calls `finish()` on release.
   - Every tile and button touch listener checks `WatchProtocol.isTouchMovementExceeded` (`touchSlop >= 8 dp`) and cancels the tap if the finger drags.
5. **Score reaction animations**:
   - Score `6-7` or `7-6` displays `R.drawable.reaction_cat_67`.
   - Score `6-9` or `9-6` displays `R.drawable.reaction_shrek_69`.
   - Scales from `0.2` to `1.0` in `220 ms` (`OvershootInterpolator`), holds for `2,600 ms`, and fades out over `600 ms`. Tapping dismisses immediately.
6. **Connection status dot**:
   - Keep the `8 dp` status dot inside a `48 × 40 dp` touch hit box at the top center notch (`#39B87F` connected, `#FFD54F` reconnecting, `#F1636E` disconnected). Tapping triggers `requestReconnectNow()`.
7. **Battery and brightness**:
   - Keep screen brightness at `0.2f` during active play and boost to `0.85f` for `3,000 ms` when the match-end overlay appears.

See [DESIGN_IDEAS.md](DESIGN_IDEAS.md) and [../docs/agents/watch-ecosystem.md](../docs/agents/watch-ecosystem.md) for full details.

## 5. Tests

Run JVM unit tests before completing any change in `AndroidWatch/`:

```bash
gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
```

1. Put pure logic and gesture/protocol helpers in `WatchProtocol` so they are unit-tested in `WatchProtocolTest` without an Android device.
2. Keep 0 test failures.

