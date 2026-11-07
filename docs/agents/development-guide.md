# Agent Development Guide

This guide gives step-by-step instructions for AI agents developing DropShot.

---

## 1. Pre-Task Checklist

Complete these steps before you modify code:

1. **Read the Documentation**:
   * Read [`AGENTS.md`](file:///Users/kollbi/repos/DropShot/AGENTS.md) and [`docs/agents/caveats.md`](file:///Users/kollbi/repos/DropShot/docs/agents/caveats.md).
   * Read the module `AGENTS.md` ([`Android/AGENTS.md`](file:///Users/kollbi/repos/DropShot/Android/AGENTS.md), [`Android/lootbox/AGENTS.md`](file:///Users/kollbi/repos/DropShot/Android/lootbox/AGENTS.md), or [`AndroidWatch/AGENTS.md`](file:///Users/kollbi/repos/DropShot/AndroidWatch/AGENTS.md)).
   * Read the specialized guide in `docs/agents/` for your task:
     * UI changes: [`docs/agents/ui-ux.md`](file:///Users/kollbi/repos/DropShot/docs/agents/ui-ux.md) and [`docs/agents/architecture.md`](file:///Users/kollbi/repos/DropShot/docs/agents/architecture.md).
     * Cloud and network changes: [`docs/agents/cloud-sync.md`](file:///Users/kollbi/repos/DropShot/docs/agents/cloud-sync.md) and [`docs/agents/security.md`](file:///Users/kollbi/repos/DropShot/docs/agents/security.md).
     * Watch changes: [`docs/agents/watch-ecosystem.md`](file:///Users/kollbi/repos/DropShot/docs/agents/watch-ecosystem.md).
     * General logic: [`docs/agents/coding-standards.md`](file:///Users/kollbi/repos/DropShot/docs/agents/coding-standards.md).
2. **Create a Dedicated Branch**:
   * Never make changes or commits directly on `master` (or `main`).
   * Create and switch to a branch named `release/n.n.n/feature-name` (for example, `release/0.1.8/fix-score-sync`).
3. **Inspect Existing Code**:
   * Read the code that you will change.
   * Read the callers of that code. Do not guess an API.
4. **Check the Build Environment**:
   * Use Homebrew `gradle` 9.x with AGP 8.7.3 and Eclipse Temurin JDK 17.
   * Do not use JDK 21+ or newer Gradle wrappers.

---

## 2. Branching, Documentation, and Merge Workflow

1. **Branch Naming Rule**:
   * Every fix, feature, or documentation task must run in its own branch separate from `master`.
   * Use the naming pattern `release/n.n.n/feature-name`:
     ```bash
     git checkout -b release/0.1.8/your-feature-name
     ```
2. **Continuous Documentation Sync**:
   * Always keep [`README.md`](file:///Users/kollbi/repos/DropShot/README.md), [`AGENTS.md`](file:///Users/kollbi/repos/DropShot/AGENTS.md), module `AGENTS.md` files, and `docs/agents/` up to date in the same branch.
   * Update the documentation whenever a screen, command, protocol, build step, or caveat changes so future agents read accurate rules.
3. **Approval and Merge Suggestion**:
   * Build and run unit tests locally first.
   * Report the results and wait for user approval before committing or pushing.
   * After implementation and user approval, suggest merging the `release/n.n.n/feature-name` branch into `master` (`main`).

---

## 3. Sandbox Execution Rules

1. **Standard Sandbox First**: Attempt commands in standard sandbox mode first.
2. **Gradle and Git Daemon Limitations**:
   * Inside the sandbox, Gradle cannot open daemon sockets.
   * Inside the sandbox, Git cannot write `.git/index.lock`.
   * Use `BypassSandbox: true` for Gradle builds and Git branch/commit/push commands after user confirmation.
3. **Consistent Tool Summary**:
   * When re-running a command with bypass, keep the `toolSummary` identical.
   * Example: `"Build Android app and run unit tests"`.

---

## 4. UI and Screen Development Rules

1. **Prevent Screen Flicker**:
   * Never switch between content tabs with `activity.finish()`.
   * Use the Single Hub Activity pattern in [`HistoryActivity.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/HistoryActivity.java). Toggle `View.VISIBLE` and `View.GONE` in memory.
2. **Modal Settings**:
   * Open settings in place with [`SettingsDialog.show`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/SettingsDialog.java). Never launch `MainActivity` to show settings.
3. **Touch Targets**:
   * Minimum touch target size is 48 dp × 48 dp (WCAG 2.2 AA).
   * Set `setMinHeight(dp(48))` and `setMinimumHeight(dp(48))` on clickable elements.
4. **Tabular Numbers**:
   * Call `UiTheme.applyTabularNumbers(textView)` on all score and clock numbers to prevent horizontal jitter.
5. **Writing Style**:
   * Use ASD-STE100 Simplified Technical English.
   * 20 words or fewer in instructions. 25 words or fewer in descriptions.
   * Active voice. Simple tenses. No contractions.

---

## 5. Storage and Data Rules

1. **Encrypted Storage Only**:
   * Store personal data and game records only via [`SecureStorageEngine`](file:///Users/kollbi/repos/DropShot/Android/lootbox/src/main/java/pl/maciek/squashscoreboard/lootbox/SecureStorageEngine.java) (AES-256-GCM + AndroidKeyStore).
   * Do not write plain `SharedPreferences` for player or match records.
2. **Lock-Free Memory Cache**:
   * Use `CopyOnWriteArrayList` for repository in-memory caches.
   * Read from memory. Do not read from disk on frame draws.
3. **Asynchronous Writes**:
   * Perform all serialization and file writes on [`AsyncRepositoryExecutor.runIo`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/AsyncRepositoryExecutor.java).
   * Never perform disk I/O on the main thread.

---

## 6. Verification and Definition of Done

Before completing any task:

1. **Compile and Run Phone Tests**:
   ```bash
   gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
   ```
2. **Compile and Run Watch Tests**:
   ```bash
   gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
   ```
3. **Check the Checklist**:
   * [ ] Work is on a dedicated `release/n.n.n/feature-name` branch (not `master`).
   * [ ] 0 test failures in `Android` and `AndroidWatch`.
   * [ ] `README.md`, `AGENTS.md`, module `AGENTS.md` files, and `docs/agents/` are updated.
   * [ ] No main-thread disk, crypto, or network I/O.
   * [ ] All new UI text follows ASD-STE100.
   * [ ] Touch targets are 48 dp or larger.
   * [ ] Rules protecting the score in Section 5.1 of `AGENTS.md` are unchanged.
   * [ ] Do not commit `*.apk` files.
   * [ ] Suggest merging into `master` after user approval.
