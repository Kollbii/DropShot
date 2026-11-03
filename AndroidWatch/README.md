# DropShot — Android Watch Remote (`AndroidWatch/`)

This module provides a standalone Bluetooth RFCOMM remote control app for Android Wear watches (API 20+, such as the LG G Watch LG-W100).

---

## 1. Features and Controls

* **75/25 Split Layout**:
  * **Top 75%**: Live score tiles for Player 1 and Player 2 with 3-letter initials, game dots (`●○`), points, active server gold border (`4 dp`), and cosmetic colors/gradients.
  * **Bottom 25%**: `UNDO` button (left) and `START` / `STOP` / `NEXT` button (right).
* **Add 1 Point**: Tap the Player 1 or Player 2 score area (`35 ms` haptic pulse).
* **Toggle Serve Side (`[R]` / `[L]`)**: Tap the `30 dp` `[R]` / `[L]` badge container at the bottom of the active server tile (double-pulse haptics). Never adds a point.
* **Undo Last Point**: Tap `UNDO` (`80 ms` haptic pulse).
* **Pause / Resume / Next Game**: Tap `START`, `STOP`, or `NEXT` (`100 ms` haptic pulse).
* **Reset Match**: Hold `STOP` / `START` / `NEXT` for `2 seconds` (`HOLD 2S` indicator).
* **Reconnect Bluetooth**: Tap the top-center status dot (`8 dp` dot with `48 × 40 dp` hit box) or touch the screen while disconnected.
* **Exit to Watch Face**: Swipe right from the left screen edge (`start <= 36 dp`, `distance >= 70 dp`).
* **Score Reactions**: Displays celebratory badges at `6-7` / `7-6` (Cat) and `6-9` / `9-6` (Shrek).

---

## 2. Build and Test

```bash
gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
```

The debug APK is written to `AndroidWatch/build/outputs/apk/debug/AndroidWatch-debug.apk`.

---

## 3. Agent Documentation

* [AndroidWatch/AGENTS.md](AGENTS.md)
* [AndroidWatch/DESIGN_IDEAS.md](DESIGN_IDEAS.md)
* [docs/agents/watch-ecosystem.md](../docs/agents/watch-ecosystem.md)
* [Root AGENTS.md](../AGENTS.md)

