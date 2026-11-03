# DropShot — Android Module (`Android/`)

This directory contains the native Android application (`:app`) and the cosmetic lootbox and encryption library (`:lootbox`).

---

## 1. Modules

| Module | Path | Package | Description |
|---|---|---|---|
| `:app` | `Android/app` | `pl.maciek.squashscoreboard` | Squash scoreboard, Single Hub for History and Players, charts, Bluetooth RFCOMM watch server, and Supabase cloud sync. |
| `:lootbox` | `Android/lootbox` | `pl.maciek.squashscoreboard.lootbox` | Reward catalog, `SecureRandom` tier roller, HMAC-signed ticket validation, `RewardInventory`, and `SecureStorageEngine` (AES-256-GCM). |

---

## 2. Default Bluetooth Remote Controls

> [!WARNING]
> Volume keys change the score only while the Match screen (`MainActivity`) is open.

| Remote Key | Action |
|---|---|
| `Volume +` or `Next Track` | Add 1 point to Player 1 |
| `Volume -` or `Previous Track` | Add 1 point to Player 2 |
| `Pause` | Undo the last point |
| `Play` / `Play-Pause` | Start a new match (asks for confirmation if points exist) |

You can customize these keys in **Settings → Remote keys**.

---

## 3. Build and Test

Run all commands from the repository root (`DropShot/`) or with `gradle -p Android` and JDK 17:

```bash
# Build and run all unit tests
gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
```

The compiled debug APK is written to `Android/app/build/outputs/apk/debug/app-debug.apk`.

---

## 4. Agent Documentation

Read these guides before you modify code in `Android/`:

* [Root AGENTS.md](../AGENTS.md)
* [Android/AGENTS.md](AGENTS.md)
* [Android/lootbox/AGENTS.md](lootbox/AGENTS.md)
* [docs/agents/architecture.md](../docs/agents/architecture.md)
* [docs/agents/development-guide.md](../docs/agents/development-guide.md)
