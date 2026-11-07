# Watch Ecosystem Guide

This document describes the wearable companion applications for DropShot.

---

## 1. Supported Wearables

DropShot supports 2 watch platforms:

| Platform | Path | Communication | Target Device |
|---|---|---|---|
| **AndroidWatch** | [`AndroidWatch/`](file:///Users/kollbi/repos/DropShot/AndroidWatch) | Classic Bluetooth RFCOMM socket (`d8095407-4b6d-4765-9b3f-282820000001`) | LG G Watch / Wear OS (API 20 to 22) |
| **Zepp OS** | [`ZeppOS/`](file:///Users/kollbi/repos/DropShot/ZeppOS) | Localhost HTTP bridge (`127.0.0.1:28282`) | Amazfit watches (Zepp OS API 4.x) |

---

## 2. Command Protocol

The watch sends commands to the phone. The phone processes commands only while [`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) is open.

### 2.1 Permitted Commands (Watch → Phone)
Encoded as JSON lines (`{"command":"<CMD>"}\n` on RFCOMM, or HTTP `POST /command` on loopback):
* `POINT_1`: Adds 1 point to Player 1.
* `POINT_2`: Adds 1 point to Player 2.
* `UNDO`: Removes the last point.
* `RESET`: Starts a new match or advances to the next game.
* `TOGGLE_SIDE`: Toggles serve side between Left `[L]` and Right `[R]` (accepted only when `canChooseServiceSide()` is `true`: at `0-0` or immediately after winning a handout).
* `SWITCH_SERVER`: Switches the starting server between Player 1 and Player 2 (accepted only before the first point of the match when `canSelectInitialServer()` is `true`).
* `TOGGLE_PAUSE`: Pauses or resumes the match clock.

### 2.2 Live State Push (Phone → AndroidWatch)
[`BluetoothWatchServer.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/BluetoothWatchServer.java) pushes JSON lines over the persistent RFCOMM socket to [`WatchProtocol.java`](file:///Users/kollbi/repos/DropShot/AndroidWatch/src/main/java/pl/maciek/squashscoreboard/watch/WatchProtocol.java):

1. **Live State (`STATE`)**:
   ```json
   {"type":"STATE","n1":"KAC","n2":"LUK","p1":8,"p2":6,"s1":1,"s2":0,"targetSets":2,"srv":1,"side":"R","canPickSide":false,"canPickSrv":false,"c1":-15435311,"r1":false,"c2":-2604465,"r2":false,"act":"STOP","action":"STOP","ball":false,"fin":false,"paused":false}
   ```
2. **Command Acknowledgement (`ACK`)**:
   ```json
   {"type":"ACK","accepted":true,"ball":false}
   ```
3. **Match Ended (`MATCH_ENDED`)**:
   ```json
   {"type":"MATCH_ENDED","winner":"Kacper","score":"2 - 0"}
   ```

---

## 3. Score Safety Rules

Follow these rules to protect the match score:

1. **No Retries**: The watch ([`BluetoothWatchClient`](file:///Users/kollbi/repos/DropShot/AndroidWatch/src/main/java/pl/maciek/squashscoreboard/watch/BluetoothWatchClient.java)) and the Zepp companion service must never queue, replay, or retry failed commands. A late point is forbidden.
2. **Listen Only on Match**: Socket servers ([`WatchCommandServer`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/WatchCommandServer.java) and [`BluetoothWatchServer`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/BluetoothWatchServer.java)) listen only while [`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) is in the foreground.
3. **Loopback Only**: The Zepp bridge binds only to `127.0.0.1:28282`. It never binds to external interfaces.
4. **Bonded Devices Only**: The Bluetooth RFCOMM server accepts connections only from paired devices (`BOND_BONDED`).
5. **Rate Limiting**: Commands faster than 25 requests per second (`40 ms` minimum interval) are dropped.

---

## 4. AndroidWatch UI and Interaction Standards

See [`AndroidWatch/AGENTS.md`](file:///Users/kollbi/repos/DropShot/AndroidWatch/AGENTS.md) and [`AndroidWatch/DESIGN_IDEAS.md`](file:///Users/kollbi/repos/DropShot/AndroidWatch/DESIGN_IDEAS.md) for full details:

1. **75 / 25 Layout Split**:
   * The top 75% (`layout_weight="75"`) displays Player 1 and Player 2 score tiles (3-letter initials, game dots `●○`, points, `4 dp` gold active server border, and solid/gradient colors).
   * The bottom 25% (`layout_weight="25"`) displays the `UNDO` button and the `START` / `STOP` / `NEXT` action button (hold `2,000 ms` to send `RESET`).
2. **Starting Server Selection (`[SERVE]`) and Streak-Locked Serve-Side Badge (`[R]` / `[L]`)**:
   * A dedicated full-width × `30 dp` service badge container sits at the bottom of each player tile on the watch (and `36 dp` on the phone when enabled in Settings → Appearance).
   * **Before the first point of the match (`0-0` points, `0-0` games)**: The active starting server shows `[R]` or `[L]`, and the other player's tile shows `[SERVE]`. Tapping `[SERVE]` sends `SWITCH_SERVER` (`selectInitialServer` on the phone) and transfers the starting serve immediately.
   * **Handout / First Point of Streak (`canPickSide: true`)**: At `0-0` or immediately after a player wins a point on the opponent's serve (handout), the active server can tap `[R]` / `[L]` as many times as desired (`TOGGLE_SIDE`) with double-pulse haptics (`0, 30, 45, 30 ms`).
   * **During a Serve Streak (`canPickSide: false`)**: Once the server scores a point on their own serve, the `[R]` / `[L]` badge switches to a dimmed red background (`0x66B71C1C` fill, `0x88EF5350` stroke), disables manual clicking/switching, and alternates automatically (`[R]` ↔ `[L]`) after each consecutive point won until the opponent scores a handout.
3. **Left-Edge Swipe-to-Exit and Touch Slop**:
   * Swiping right from the left screen edge (`startX <= 36 dp`, `dx >= 70 dp`, `dx > 1.5 * dy`) dispatches `ACTION_CANCEL` to child views and exits the app (`finish()`).
   * Touch slop filtering (`>= 8 dp`) on all tiles and buttons prevents accidental points or undo commands during finger drags.
4. **Score Reaction Badges**:
   * When the score reaches `6-7` or `7-6`, the watch displays the Cat badge (`reaction_cat_67.png`).
   * When the score reaches `6-9` or `9-6`, the watch displays the Shrek badge (`reaction_shrek_69.png`).
   * Scales from `0.2` to `1.0` in `220 ms` (`OvershootInterpolator`), holds for `2,600 ms`, and fades out over `600 ms` (`3.4 s` total). Tapping dismisses immediately.
