# Agent Development Guide

This guide gives step-by-step instructions for AI agents developing DropShot.

---

## 1. Pre-Task Checklist

Complete these steps before you modify code:

1. **Read the Documentation**:
   * Read [`AGENTS.md`](file:///Users/kollbi/repos/DropShot/AGENTS.md) and [`docs/agents/caveats.md`](file:///Users/kollbi/repos/DropShot/docs/agents/caveats.md).
   * Read the specialized guide in `docs/agents/` for your task:
     * UI changes: [`docs/agents/ui-ux.md`](file:///Users/kollbi/repos/DropShot/docs/agents/ui-ux.md) and [`docs/agents/architecture.md`](file:///Users/kollbi/repos/DropShot/docs/agents/architecture.md).
     * Cloud and network changes: [`docs/agents/cloud-sync.md`](file:///Users/kollbi/repos/DropShot/docs/agents/cloud-sync.md) and [`docs/agents/security.md`](file:///Users/kollbi/repos/DropShot/docs/agents/security.md).
     * Watch changes: [`docs/agents/watch-ecosystem.md`](file:///Users/kollbi/repos/DropShot/docs/agents/watch-ecosystem.md).
     * General logic: [`docs/agents/coding-standards.md`](file:///Users/kollbi/repos/DropShot/docs/agents/coding-standards.md).
2. **Inspect Existing Code**:
   * Read the code that you will change.
   * Read the callers of that code. Do not guess an API.
3. **Check the Build Environment**:
   * Use Homebrew `gradle` 9.x with AGP 8.7.3 and Eclipse Temurin JDK 17.
   * Do not use JDK 21+ or newer Gradle wrappers.

---

## 2. Sandbox Execution Rules

1. **Standard Sandbox First**: Attempt commands in standard sandbox mode first.
2. **Gradle and Git Daemon Limitations**:
   * Inside the sandbox, Gradle cannot open daemon sockets.
   * Inside the sandbox, Git cannot write `.git/index.lock`.
   * Use `BypassSandbox: true` for Gradle builds and Git commit/push commands after user confirmation.
3. **Consistent Tool Summary**:
   * When re-running a command with bypass, keep the `toolSummary` identical.
   * Example: `"Build Android app and run unit tests"`.

---

## 3. UI and Screen Development Rules

1. **Prevent Screen Flicker**:
   * Never switch between content tabs with `activity.finish()`.
   * Use the Single Hub Activity pattern in [`HistoryActivity.java`](file:///Users/kollbi/repos/DropShot/Android/app/src/main/java/pl/maciek/squashscoreboard/HistoryActivity.java). Toggle `View.VISIBLE` and `View.GONE` in memory.
2. **Touch Targets**:
   * Minimum touch target size is 48 dp × 48 dp (WCAG 2.2 AA).
   * Set `setMinHeight(dp(48))` and `setMinimumHeight(dp(48))` on clickable elements.
3. **Tabular Numbers**:
   * Call `UiTheme.applyTabularNumbers(textView)` on all score and clock numbers to prevent horizontal jitter.
4. **Writing Style**:
   * Use ASD-STE100 Simplified Technical English.
   * 20 words or fewer in instructions. 25 words or fewer in descriptions.
   * Active voice. Simple tenses. No contractions.

---

## 4. Storage and Data Rules

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

## 5. Verification and Definition of Done

Before completing any task:

1. **Compile and Run Tests**:
   ```bash
   gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest assembleDebug
   ```
2. **Verify Watch Tests**:
   ```bash
   gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home testDebugUnitTest
   ```
3. **Check the Checklist**:
   * [ ] 0 test failures.
   * [ ] No main-thread disk, crypto, or network I/O.
   * [ ] All new UI text follows ASD-STE100.
   * [ ] Touch targets are 48 dp or larger.
   * [ ] Rules protecting the score in Section 5.1 of `AGENTS.md` are unchanged.
   * [ ] Do not commit `*.apk` files.
