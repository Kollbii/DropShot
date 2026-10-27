# AGENTS.md — DropShot

This file gives the rules for all AI agents in this repository.
Rules in a subdirectory `AGENTS.md` add to these rules. They do not replace them.

All agent text (rules, commit messages, code comments, UI text) uses ASD-STE100 style.
See [Writing style](#4-writing-style-asd-ste100).

---

## 1. Project summary

DropShot is a squash scoreboard. It has 2 parts:

| Part | Path | Language | Purpose |
|---|---|---|---|
| Android app | `Android/app` | Java 11, Android SDK (API 26 to 35) | Keeps the score. Saves the match history, player profiles, and statistics. |
| Lootbox module | `Android/lootbox` | Java 11, Android library | Cosmetic player colors, lootbox roll, reward inventory, encrypted storage engine. |
| Watch remote | `ZeppOS/` | JavaScript (Zepp OS API 4.x, ZML) | Amazfit watch app. Sends `POINT_1`, `POINT_2`, `UNDO`, `RESET` to the phone. |
| Watch workout page | `ZeppOS/workout-extension` | JavaScript | Same commands as a workout data page. |
| Design specification | `Android/design` | Markdown, JSON, Kotlin (reference only) | Material Design 3 tokens, screens, glossary, accessibility and STE checklists. |

The user count is small (2 players). Do not add cloud services, accounts, or analytics without a request.

## 2. Repository map

```
DropShot/
├── AGENTS.md                  ← this file (global rules)
├── Makefile                   ← build, install, emulator tasks
├── docs/agents/               ← detailed standards (read on demand)
│   ├── security.md            ← OWASP Top 10, OWASP MASVS / MASTG, ASVS
│   ├── coding-standards.md    ← Java, architecture, performance, tests
│   ├── ui-ux.md               ← Material Design 3, WCAG 2.2 AA, UI text
│   ├── ste-writing.md         ← ASD-STE100 rules for all text
│   └── caveats.md             ← known problems and traps (READ THIS)
├── .agents/skills/            ← step-by-step procedures for agents
├── Android/
│   ├── AGENTS.md              ← Android rules
│   ├── app/                   ← application module
│   ├── lootbox/               ← library module (has its own AGENTS.md)
│   └── design/                ← UI/UX specification (source of truth for UI)
└── ZeppOS/                    ← watch apps (has its own AGENTS.md)
```

## 3. Commands

Use the `Makefile`. It finds the Android SDK and JDK 17.

| Task | Command |
|---|---|
| Build the debug APK | `make build` |
| Install and start on a device | `make run` |
| Start the emulator, then install | `make start` |
| Show the tool paths | `make status` |
| Run all unit tests | `gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest` |
| Build and test in 1 step | add `assembleDebug` to the test command |

> [!WARNING]
> There is no Gradle wrapper (`gradlew`). The build uses the Homebrew `gradle` (9.x) with AGP 8.7.3 and JDK 17.
> Do not use a newer JDK. The build can fail.

> [!NOTE]
> In the agent sandbox, Gradle cannot open its daemon socket and `git` cannot write `.git/index.lock`.
> Run these commands with the sandbox bypass. Ask the user first.

## 4. Writing style (ASD-STE100)

Use ASD-STE100 Simplified Technical English for all text that you write:
UI strings, code comments, Javadoc, commit messages, documents, and replies to the user.

The short rules (full rules: [docs/agents/ste-writing.md](docs/agents/ste-writing.md)):

1. Use 1 word for 1 meaning. Use the terms in [Android/design/04-glossary.md](Android/design/04-glossary.md).
2. Write 20 words or fewer in an instruction. Write 25 words or fewer in a description.
3. Write 1 instruction in 1 sentence. Use the imperative form.
4. Use the active voice. Use simple tenses.
5. Do not use contractions. Do not use idioms or slang.
6. Do not use more than 3 nouns in a noun cluster.
7. Put a warning or a caution before the step that it applies to.
8. Write an error message in 3 parts: what occurred, why, and what the user must do. Do not blame the user.
9. Start each button label with a verb (`Save`, `Start`, `Delete`, `Undo`).
10. Write numbers as digits. Put the unit after the number (`60 min`).

The UI uses the squash term "game". Code names such as `pointsToWinSet` keep the word "set". Do not rename them.

## 5. Mandatory engineering standards

Each task must obey these standards. Read the linked file before you change the related code.

| Area | Standard | File |
|---|---|---|
| Security | OWASP Top 10 (2021), OWASP Mobile Top 10 (2024), OWASP MASVS v2 and MASTG, OWASP ASVS 4.0.3 | [docs/agents/security.md](docs/agents/security.md) |
| Code and logic | Clean code, SOLID, fail-safe defaults, deterministic state, no main-thread I/O | [docs/agents/coding-standards.md](docs/agents/coding-standards.md) |
| Performance | 60/120 Hz frame budget, no allocation in `onDraw`, cache reads, async writes | [docs/agents/coding-standards.md](docs/agents/coding-standards.md#5-performance) |
| UI and UX | Material Design 3, WCAG 2.2 AA, 48 dp touch targets, design tokens | [docs/agents/ui-ux.md](docs/agents/ui-ux.md) |
| Text | ASD-STE100 | [docs/agents/ste-writing.md](docs/agents/ste-writing.md) |

### 5.1 Rules that protect the score (do not change)

These behaviors keep the score correct. Do not change them without an explicit request.

1. The watch and the App Side Service do not send a command again after a failure. A late point is not permitted.
2. The watch bridge listens only on `127.0.0.1:28282`. It listens only while the Match screen is open.
3. The volume keys change the score only while the Match screen is open.
4. "Start new match" asks for confirmation when the current match has points.
5. The app saves the current match in the history before it starts a new match.
6. When the user deletes a player, the matches of that player stay in the history.
7. The undo of a point that completed a game restores the previous game and its duration.

### 5.2 Security rules (summary)

1. Treat all external input as untrusted: Intent extras, JSON from storage, bridge requests, user text.
   Validate it with `InputValidator` before use.
2. Store persistent data only through `SecureStorageEngine` (AES-256-GCM, AndroidKeyStore).
   Do not write new plain `SharedPreferences` for personal or game data.
3. Do not add hardcoded secrets, keys, tokens, back doors, or "developer" accounts.
4. Do not add permissions, exported components, or network hosts without a request.
5. Use `SecureRandom` for IDs, tokens, and rolls. Do not use `java.util.Random` for these.
6. Log only through `SafeLog`. Do not log personal data, tokens, or full payloads.
7. Keep `android:allowBackup="false"` and the cleartext block (loopback is the only exception).

## 6. Workflow for each task

1. Read this file, the subdirectory `AGENTS.md`, and [docs/agents/caveats.md](docs/agents/caveats.md).
2. Read the code that you will change, and the code that calls it. Do not guess an API.
3. Make the smallest change that completes the task. Keep the current code style.
4. Keep the existing comments and Javadoc. Update them if the behavior changes.
5. Add or update a unit test for each change to logic, validation, or security.
6. Build and test: `testDebugUnitTest assembleDebug`. The build must pass with 0 test failures.
7. Report the changed files, the test result, and all open risks to the user.

### 6.1 Definition of done

- [ ] The build passes. All unit tests pass.
- [ ] New logic has unit tests (JVM tests, no device necessary).
- [ ] No new main-thread disk, network, or crypto work.
- [ ] No new lint, security, or accessibility regression.
- [ ] All new UI text obeys ASD-STE100 and the glossary.
- [ ] Touch targets are 48 dp or larger. Text contrast is 4.5:1 or more.
- [ ] The rules in section 5.1 are not changed.
- [ ] Temporary code has a `TEMPORARY:` comment with the removal condition.

## 7. Version control and release

1. Do not commit `*.apk` files. `.gitignore` excludes them.
2. Commit only when the user asks. Use 1 commit for 1 batch unless the user asks for more.
3. Write the commit subject in the imperative form, 72 characters or fewer.
4. Do not push, force-push, rebase, or delete branches without a request.
5. A version bump changes `Android/app/build.gradle`:
   `versionCode` increases by 1. `versionName` increases by `0.1` (example: `1.3` → `1.4`).
6. The ZeppOS apps have separate versions in their `app.json`. Change them only for watch changes.

See the skill [.agents/skills/release-version-bump](.agents/skills/release-version-bump/SKILL.md).

## 8. Known caveats (summary)

Read [docs/agents/caveats.md](docs/agents/caveats.md) for the full list. The most important items:

1. `ProfileStore` contains `TEMPORARY` seed code. It creates "Kacper" and "Łukasz" with the rainbow color at each cold start.
2. The watch bridge token check is off. `setExpectedToken` is never called, and the watch sends no token.
3. The lootbox "authoritative" service runs on the device. It is not a server. A rooted device can change rewards.
4. If the AndroidKeyStore key is lost, the encrypted data cannot be read. The app then starts with empty data.
5. UI text is hardcoded in Java. It is not in `strings.xml`. The design specification expects `strings.xml`.
6. `Android/gradle.properties` contains stray text from an old patch (`*** Add File: ...`).
7. `Android/Punkty-Squash.apk` is still tracked by git, but `.gitignore` excludes `*.apk`.
8. The README files are in Polish. The UI and the design documents are in English.

