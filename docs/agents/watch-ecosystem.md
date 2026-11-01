# Watch Ecosystem Guide

This document describes the wearable companion applications for DropShot.

---

## 1. Supported Wearables

DropShot supports 2 watch platforms:

| Platform | Path | Communication | Target Device |
|---|---|---|---|
| **Zepp OS** | `ZeppOS/` | Localhost HTTP bridge (`127.0.0.1:28282`) | Amazfit watches (Zepp OS API 4.x) |
| **AndroidWatch** | `AndroidWatch/` | Classic Bluetooth RFCOMM socket | LG G Watch / Wear OS (API 20) |

---

## 2. Command Protocol

The watch sends commands to the phone. The phone processes commands only while [`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) is open.

### 2.1 Permitted Commands
* `POINT_1`: Adds 1 point to Player 1.
* `POINT_2`: Adds 1 point to Player 2.
* `UNDO`: Removes the last point.
* `RESET`: Starts a new match.
* `TOGGLE_SIDE`: Toggles serve side between Left `[L]` and Right `[R]`.
* `TOGGLE_PAUSE`: Pauses or resumes the match clock.

### 2.2 Live State Push
The phone pushes JSON lines over Bluetooth to the watch:
```json
{"p1Score":8,"p2Score":6,"p1Sets":1,"p2Sets":0,"server":1,"side":"R","gameBall":false,"matchBall":false}
```

---

## 3. Score Safety Rules

Follow these rules to protect the match score:

1. **No Retries**: The watch and the companion service must not retry failed commands. A late point is not permitted.
2. **Listen Only on Match**: Socket servers listen only while [`MainActivity`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/MainActivity.java) is in the foreground.
3. **Loopback Only**: The Zepp bridge binds to `127.0.0.1`. It never binds to external interfaces.
4. **Bonded Devices Only**: The Bluetooth RFCOMM server accepts connections only from paired devices (`BOND_BONDED`).
5. **Rate Limiting**: Commands faster than 25 requests per second are dropped.

---

## 4. Watch UI and Interaction Standards

1. **75 / 25 Layout Split**:
   * The top 75% of the watch screen displays player score tiles.
   * The bottom 25% displays secondary actions (serve side toggle, undo, reset).
2. **Touch Lockout Protection**:
   * After the user holds a tile to switch serve sides, touch input is locked for 400 ms.
   * This lockout prevents accidental point additions when the finger lifts.
3. **Score Reaction Badges**:
   * When the score reaches 6-7 or 7-6, the watch displays the cat badge (`67cat.png`).
   * When the score reaches 6-9 or 9-6, the watch displays the shrek badge (`69shrek.png`).
   * The badge appears immediately and fades out after 3 to 5 seconds.
