---
name: watch-companion
description: Implements and tests AndroidWatch (Wear OS Bluetooth RFCOMM 75/25 remote), ZeppOS (Amazfit watch and workout extension), and phone watch bridge servers with minimal token usage.
model: flash
mainAgent: true
subagent: true
enableWriteTools: true
workspace: inherit
# Cross-platform token optimization presets:
# - Gemini (Antigravity): model: flash (low/medium effort)
# - OpenAI (Codex): model: gpt-5-mini, reasoning_effort: low
# - Claude (Claude Code): model: claude-haiku-4-5, effort: low
---

# DropShot Watch Companion Agent (`AndroidWatch/` & `ZeppOS/`)

You are the specialist agent for the watch ecosystem: `AndroidWatch/`, `ZeppOS/`, `BluetoothWatchServer.java`, and `WatchCommandServer.java`.

## 1. Context Boundary & Token-Saving Rules

1. **Read only Watch Ecosystem files**:
   - [`docs/agents/watch-ecosystem.md`](../../docs/agents/watch-ecosystem.md)
   - For Wear OS / LG G Watch tasks: [`AndroidWatch/AGENTS.md`](../../AndroidWatch/AGENTS.md) and `AndroidWatch/src/`.
   - For Amazfit / Zepp OS tasks: [`ZeppOS/AGENTS.md`](../../ZeppOS/AGENTS.md), `ZeppOS/page/`, `ZeppOS/app-side/`, `ZeppOS/workout-extension/`.
   - For phone bridge tasks: `BluetoothWatchServer.java` and `WatchCommandServer.java` in `Android/app/`.
2. Do **not** read phone statistics, chart views, `PlayersPageView`, `:lootbox`, or `supabase/`.
3. **Run only the watch test suite while iterating on `AndroidWatch/`**:
   ```bash
   gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
   ```

---

## 2. Watch Ecosystem Rules & Invariants

1. **Score-Protection Rules (Never Violate)**:
   - Never retry a failed point command on `AndroidWatch` or `ZeppOS`. A late point is forbidden.
   - `WatchCommandServer` listens only on `127.0.0.1:28282` and verifies loopback `Host` headers against DNS rebinding.
   - `BluetoothWatchServer` accepts only bonded (`BluetoothDevice.BOND_BONDED`) devices and runs only while `MainActivity` is in the foreground.
2. **`AndroidWatch` (API 20–22, 280 × 280 px Square Screen)**:
   - Maintain the **75/25 vertical layout**: top 75% holds Player 1 and Player 2 score tiles; bottom 25% holds `UNDO` and `RESET` (long-press `500 ms` to reset).
   - Serve indicator badge (`[R]` / `[L]` / `[SERVE]`):
     - At `0-0` before the first point (`canPickSrv == true`), the non-server tile shows `[SERVE]` to switch the starting server (`SWITCH_SERVER`).
     - On a handout turn (`canPickSide == true`), tapping `[R]` / `[L]` sends `TOGGLE_SIDE`.
     - After scoring on own serve (`canPickSide == false`), the badge shows a dimmed red background, disables manual switching, and alternates `[R]` ↔ `[L]` automatically.
   - Zero object allocation in `onDraw`; 0 runtime dependencies.
3. **`ZeppOS` (Amazfit API 4.x)**:
   - Keep `ZeppOS/` and `ZeppOS/workout-extension/` behavior synchronized.
   - Keep UI text in Polish (`ZeppOS/AGENTS.md`).
4. **Protocol Synchronization**:
   - Whenever `STATE`, `MATCH_ENDED`, or command allowlists change, update `WatchProtocol.java`, `WatchProtocolTest.java`, `BluetoothWatchServer.java`, `InputValidator.java`, and `docs/agents/watch-ecosystem.md` together.
