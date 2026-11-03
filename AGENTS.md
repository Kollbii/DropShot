# AGENTS.md — DropShot

This file gives the rules for all AI agents in this repository.
Rules in a subdirectory `AGENTS.md` add to these rules. They do not replace them.

All agent text (rules, commit messages, code comments, UI text) uses ASD-STE100 style.
See [Writing style](#4-writing-style-asd-ste100).

---

## 1. Project summary

DropShot is a squash scoreboard system. It has these parts:

| Part | Path | Language | Purpose |
|---|---|---|---|
| Android app | `Android/app` | Java 11, Android SDK (API 26 to 35) | Keeps the score. Saves the match history, player profiles, statistics, and cloud sync. |
| Lootbox module | `Android/lootbox` | Java 11, Android library | Cosmetic player colors, lootbox roll, reward inventory, encrypted storage engine. |
| Android Watch app | `AndroidWatch/` | Java 11, Android SDK (API 20 to 22, compileSdk 35) | Standalone Bluetooth RFCOMM watch remote. Shows live 75/25 score tiles, serve-side badge, and reactions. |
| Watch remote | `ZeppOS/` | JavaScript (Zepp OS API 4.x, ZML) | Amazfit watch app. Sends `POINT_1`, `POINT_2`, `UNDO`, `RESET` to the phone. |
| Watch workout page | `ZeppOS/workout-extension` | JavaScript | Same commands as a workout data page. |
| Design specification | `Android/design` | Markdown, JSON, Kotlin (reference only) | Material Design 3 tokens, screens, glossary, accessibility and STE checklists. |

The user count is small (2 players). Do not add new cloud services, accounts, or analytics without a request.

## 2. Repository map

```
DropShot/
├── AGENTS.md                  ← this file (global rules)
├── README.md                  ← main project documentation (keep up to date)
├── Makefile                   ← build, install, emulator tasks
├── docs/agents/               ← detailed standards (read before changing code)
│   ├── architecture.md        ← activity hierarchy, Single Hub pattern, navigation bar
│   ├── caveats.md             ← known problems and traps (READ THIS FIRST)
│   ├── cloud-sync.md          ← Supabase PostgREST sync, tokens, database schema
│   ├── coding-standards.md    ← Java, architecture, performance, tests
│   ├── development-guide.md   ← step-by-step developer guide for agents
│   ├── security.md            ← OWASP Top 10, OWASP MASVS / MASTG, ASVS
│   ├── ste-writing.md         ← ASD-STE100 rules for all text
│   ├── ui-ux.md               ← Material Design 3, WCAG 2.2 AA, UI text
│   └── watch-ecosystem.md     ← Zepp OS and AndroidWatch companions, protocol
├── .agents/skills/            ← step-by-step procedures for agents
├── Android/
│   ├── AGENTS.md              ← Android rules
│   ├── README.md              ← Android module guide
│   ├── app/                   ← phone/tablet application module
│   ├── lootbox/               ← reward and encryption library module (has its own AGENTS.md)
│   └── design/                ← UI/UX specification (source of truth for UI)
├── AndroidWatch/
│   ├── AGENTS.md              ← AndroidWatch rules
│   ├── README.md              ← AndroidWatch module guide
│   ├── DESIGN_IDEAS.md        ← watch UI/UX evaluation and implemented designs
│   └── src/                   ← standalone Wear OS / LG G Watch app and unit tests
└── ZeppOS/                    ← Amazfit watch apps (has its own AGENTS.md)
```

## 3. Commands

Use the `Makefile`. It finds the Android SDK and JDK 17.

| Task | Command |
|---|---|
| Build the phone debug APK | `make build` |
| Install and start on a device | `make run` |
| Start the emulator, then install | `make start` |
| Show the tool paths | `make status` |
| Run Android unit tests and build | `gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug` |
| Run AndroidWatch unit tests and build | `gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug` |

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

Each task must obey these standards.

> [!IMPORTANT]
> You MUST read the relevant files in `docs/agents/` before you design or change code.

| Area | Standard | File |
|---|---|---|
| Development guide | Branching, environment, sandbox rules, UI rules, definition of done | [docs/agents/development-guide.md](docs/agents/development-guide.md) |
| Architecture | Single Hub pattern, activity hierarchy, navigation bar, modal settings | [docs/agents/architecture.md](docs/agents/architecture.md) |
| Cloud sync | Supabase REST sync, token auth, background push | [docs/agents/cloud-sync.md](docs/agents/cloud-sync.md) |
| Watch ecosystem | Zepp OS and AndroidWatch remotes, RFCOMM protocol, 75/25 layout | [docs/agents/watch-ecosystem.md](docs/agents/watch-ecosystem.md) |
| Security | OWASP Top 10 (2021), OWASP Mobile Top 10 (2024), MASVS v2 | [docs/agents/security.md](docs/agents/security.md) |
| Code and logic | Clean code, SOLID, fail-safe defaults, deterministic state | [docs/agents/coding-standards.md](docs/agents/coding-standards.md) |
| Performance | 60/120 Hz frame budget, no allocation in `onDraw`, cache reads | [docs/agents/coding-standards.md](docs/agents/coding-standards.md#5-performance) |
| UI and UX | Material Design 3, WCAG 2.2 AA, 48 dp touch targets | [docs/agents/ui-ux.md](docs/agents/ui-ux.md) |
| Text | ASD-STE100 | [docs/agents/ste-writing.md](docs/agents/ste-writing.md) |

### 5.1 Rules that protect the score (do not change)

These behaviors keep the score correct. Do not change them without an explicit request.

1. The watch and the App Side Service do not send a command again after a failure. A late point is not permitted.
2. The loopback watch bridge (`WatchCommandServer`) listens only on `127.0.0.1:28282` and the Bluetooth watch bridge (`BluetoothWatchServer`) accepts only bonded (`BOND_BONDED`) devices. Both listen only while the Match screen (`MainActivity`) is open.
3. The volume keys change the score only while the Match screen is open.
4. "Start new match" asks for confirmation when the current match has points.
5. The app saves the current match in the history before it starts a new match.
6. When the user deletes a player, the matches of that player stay in the history.
7. The undo of a point that completed a game restores the previous game and its duration.

### 5.2 Security rules (summary)

1. Treat all external input as untrusted: Intent extras, JSON from storage, bridge requests, user text.
   Validate it with `InputValidator` or `WatchProtocol` before use.
2. Store persistent data only through `SecureStorageEngine` (AES-256-GCM, AndroidKeyStore).
   Do not write new plain `SharedPreferences` for personal or game data.
3. Do not add hardcoded secrets, keys, tokens, back doors, or "developer" accounts.
4. Do not add permissions, exported components, or network hosts without a request.
5. Use `SecureRandom` for IDs, tokens, and rolls. Do not use `java.util.Random` for these.
6. Log only through `SafeLog`. Do not log personal data, tokens, or full payloads.
7. Keep `android:allowBackup="false"` and the cleartext block (loopback is the only exception).

## 6. Workflow for each task

1. Read this file, the subdirectory `AGENTS.md`, and the relevant files in `docs/agents/` (especially `caveats.md` and `development-guide.md`).
2. Create and switch to a dedicated branch before you change any file. Never make fixes or changes directly on `master` (or `main`). Name the branch `release/n.n.n/feature-name` (for example, `release/0.1.8/fix-score-sync`).
3. Read the code that you will change, and the code that calls it. Do not guess an API.
4. Make the smallest change that completes the task. Keep the current code style.
5. Keep the existing comments and Javadoc. Update them if the behavior changes.
6. Update all affected documentation (`README.md`, `AGENTS.md`, module `AGENTS.md` files, and `docs/agents/`) in the same branch so that documentation is always accurate for other agents.
7. Add or update a unit test for each change to logic, validation, protocol, or security.
8. Build and test both `Android` and `AndroidWatch` (`testDebugUnitTest assembleDebug`). Both builds must pass with 0 test failures.
9. Report the changed files, the test result, and all open risks to the user. After implementation and user approval, suggest merging the branch into `master` (`main`).

### 6.1 Definition of done

- [ ] Changes are made on a separate branch named `release/n.n.n/feature-name` (not on `master`).
- [ ] The `Android` and `AndroidWatch` builds pass. All unit tests pass with 0 failures.
- [ ] New logic has unit tests (JVM tests, no device necessary).
- [ ] `README.md`, `AGENTS.md`, module `AGENTS.md` files, and `docs/agents/` are updated and synchronized with the code.
- [ ] No new main-thread disk, network, or crypto work.
- [ ] No new lint, security, or accessibility regression.
- [ ] All new UI text obeys ASD-STE100 and the glossary.
- [ ] Touch targets are 48 dp or larger. Text contrast is 4.5:1 or more.
- [ ] The rules in section 5.1 are not changed.
- [ ] Temporary code has a `TEMPORARY:` comment with the removal condition.
- [ ] After user approval, the agent suggests merging the feature branch into `master`.

## 7. Version control, branching, and release

1. **Mandatory feature branch**: Make every fix, feature, or documentation update in a separate branch created from `master`. Never commit directly on `master`.
2. **Branch naming**: Use the format `release/n.n.n/feature-name` (for example, `release/0.1.8/watch-haptics` or `release/0.1.8/agent-rules-and-docs`), where `n.n.n` is the target release version.
3. **Documentation upkeep**: Always keep `README.md`, `AGENTS.md`, subdirectory `AGENTS.md` files, and `docs/agents/` up to date in the same branch as your code changes.
4. **Commit discipline**: Do not commit `*.apk` files (`.gitignore` excludes them). Commit only when the user asks. Use 1 commit for 1 batch unless the user asks for more. Write the commit subject in the imperative form, 72 characters or fewer.
5. **Approval and merge**: Do not push, force-push, rebase, merge, or delete branches without a request. After successful implementation, verification, and user approval, suggest merging the `release/n.n.n/feature-name` branch into `master`.
6. **Version numbers**:
   - Phone app (`Android/app/build.gradle`): `versionCode` increases by `1`, `versionName` increases by `0.1` (example: `'1.7'` → `'1.8'`).
   - Android Watch app (`AndroidWatch/build.gradle`): increase `versionCode` by `1` and `versionName` by `0.1` when watch code changes.
   - Zepp OS apps (`ZeppOS/app.json`, `ZeppOS/workout-extension/app.json`): change only for Zepp OS changes.
7. **Release tag format**: Use annotated tags in the format `git tag -a 0.1.8 -m "Release 0.1.8"`.
8. **Final release procedure** (run only when the user requests a release and no more code will be added):
   1. Update `versionCode` and `versionName` in `Android/app/build.gradle` (and `AndroidWatch/build.gradle` if changed).
   2. Verify that `README.md`, `AGENTS.md`, and `docs/agents/` reflect all changes in the release.
   3. Build and test both modules:
      - `gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug`
      - `gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug`
   4. Commit the changes on `release/n.n.n/feature-name` (without `*.apk`) and merge the branch into `master`.
   5. Tag the release commit on `master`: `git tag -a n.n.n -m "Release n.n.n"`.
   6. Push `master` and the tag to `origin`, and delete the merged `release/n.n.n/*` branch locally and on `origin`.
   7. Publish the GitHub Release with title `n.n.n` and attach `DropShot-n.n.n.apk` (from `Android/app/build/outputs/apk/debug/app-debug.apk`) and `DropShot-Watch-n.n.n.apk` (from `AndroidWatch/build/outputs/apk/debug/AndroidWatch-debug.apk`).

See the skill [.agents/skills/release-version-bump](.agents/skills/release-version-bump/SKILL.md).

## 8. Known caveats (summary)

Read [docs/agents/caveats.md](docs/agents/caveats.md) for the full list. The most important items:

1. Player secret token hashing uses unsalted SHA-256. Weak tokens can be cracked offline.
2. The loopback watch bridge token check is off. `setExpectedToken` is never called, and the Zepp OS watch sends no token.
3. The lootbox "authoritative" service runs on the device. It is not a server. A rooted device can change rewards.
4. If the AndroidKeyStore key is lost, the encrypted data cannot be read. The app then starts with empty data.
5. UI text is hardcoded in Java. It is not in `strings.xml`. The design specification expects `strings.xml`.
6. `Android/gradle.properties` contains stray text from an old patch (`*** Add File: ...`).
7. `Android/Punkty-Squash.apk` is still tracked by git, but `.gitignore` excludes `*.apk`.
8. `README.md`, `Android/README.md`, `AndroidWatch/README.md`, and `Android/lootbox/README.md` are in English. `ZeppOS/README.md` is in Polish.
