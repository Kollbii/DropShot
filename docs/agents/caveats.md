# Known caveats

This file lists the known problems, risks, and traps in this repository.
Read it before each task. Add a new item when you find a new caveat. Remove an item when you fix it.

Severity: **High** = can lose data or allow unwanted changes. **Medium** = can cause wrong behavior. **Low** = cleanup.

## 1. Temporary code

There is no active temporary code.

## 2. Security

| # | Description | Severity |
|---|---|---|
| S1 | `WatchCommandServer.setExpectedToken` is never called. The watch sends no token. Any local app can send a score command while the Match screen is open. | Medium |
| S2 | `WatchCommandServer.isLoopbackHost` accepts an empty or missing `Host` header. | Low |
| S3 | `LootboxAuthoritativeService` runs on the device. A rooted device or a modified APK can unlock rewards. Rewards are cosmetic only. | Low |
| S4 | `AppPreferences` (theme, rules, keys, court timer, last player colors) is not encrypted. It contains no personal data now. Do not add personal data to it. | Low |
| S5 | The release build has no signing configuration in `build.gradle`. The debug key signs the APK copies. | Medium |

## 3. Data and storage

| # | Description | Severity |
|---|---|---|
| D1 | If the AndroidKeyStore key is lost (device reset, OS bug, app data clear), the encrypted data cannot be read. `getEncryptedString` returns the default value. The app starts with empty profiles and history. There is no user message. | High |
| D2 | `getEncryptedString` migrates old plaintext values to encrypted values when it reads them. A downgrade to an old app version cannot read the new data. | Medium |
| D3 | Writes are asynchronous. If the process stops at once after a change, the change can be lost. | Medium |
| D4 | The stores use static caches. They load once for each process. | Low |
| D5 | The history keeps a maximum of 100 matches. The oldest match is deleted without a message. | Low |
| D6 | A player color is the first ARGB color of a reward. A change to the first color of an existing reward breaks saved profiles and matches. | High |
| D7 | Old match records link players by name when `p1profile`/`p2profile` is missing. A rename uses `MatchHistoryStore.renameProfile` to keep the link. | Low |

## 4. Build and tooling

| # | Description | Severity |
|---|---|---|
| B1 | There is no Gradle wrapper. The build uses Homebrew `gradle` 9.x with AGP 8.7.3. Gradle prints deprecation warnings. A future Gradle can break the build. | Medium |
| B2 | The build needs JDK 17 (`/Library/Java/JavaVirtualMachines/temurin-17.jdk`). The `Makefile` sets it. A direct `gradle` call must set `-Dorg.gradle.java.home`. | Medium |
| B3 | `Android/gradle.properties` contains stray text from an old patch (`*** Add File: squash-scoreboard/app/build.gradle` and a copy of an old `build.gradle`). Gradle ignores it, but it is wrong. | Low |
| B4 | In the agent sandbox, Gradle cannot connect to its daemon, and `git` cannot write `.git/index.lock`. These commands need the sandbox bypass. | Low |
| B5 | `Android/Punkty-Squash.apk` is still tracked by git. `.gitignore` excludes `*.apk`, so local changes to the file still show as modified. To stop tracking it: `git rm --cached Android/Punkty-Squash.apk` (ask the user first). | Low |
| B6 | `ZeppOS/dist/*.zab` packages are tracked by git. | Low |
| B7 | The SDK tools print "SDK XML version 4" warnings. They are not errors. | Low |

## 5. Code structure

| # | Description | Severity |
|---|---|---|
| C1 | `MainActivity` has more than 1,400 lines (score, clock, court timer, settings dialog, remote keys, watch commands). Extract new features into separate classes. | Medium |
| C2 | All UI text is hardcoded in Java. The design specification expects `strings.xml`. Translation is not possible now. | Medium |
| C3 | `RewardTilePainter.draw` allocates a `Path` for each call (in `onDraw`). | Low |
| C4 | Animated rewards must have exactly 7 colors. `RewardCatalog.configure` uses `% 6`. | Low |
| C5 | The Settings color list shows only the 4 starter colors. A guest player cannot use an unlocked color. `colorIndex()` returns Blue for an unknown color. | Low |
| C6 | The user requested a local `deweloper` test profile with all cosmetics unlocked. Preserve it during profile and inventory migration. | Low |

## 6. Documentation

| # | Description | Severity |
|---|---|---|
| X1 | `README.md`, `Android/README.md`, and `ZeppOS/README.md` are in Polish. The UI and the design documents are in English. | Low |
| X2 | `Android/README.md` says the volume keys and Play key do some actions. The current defaults are in `MainActivity.loadStyle` and the Settings screen. Check the code, not the README. | Low |
| X3 | `Android/lootbox/README.md` says "no inventory, persistence". This is old. Inventory and encrypted persistence exist. | Low |
| X5 | `Android/design/README.md` describes features that do not exist yet (Google sign-in, cloud backup, onboarding, splash). Do not implement them without a request. | Low |

