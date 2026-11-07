---
name: release-version-bump
description: Bump the DropShot version, verify documentation, build phone and watch APKs, merge the release/n.n.n/feature-name branch into master, tag the release, and publish the GitHub Release with APK assets attached.
---

# Release and Version Bump Procedure

> [!TIP]
> **Token Optimization**: Delegate this release workflow to the lightweight Flash custom agent [`.agents/agents/release-manager.md`](../../agents/release-manager.md) via `invoke_subagent` (`TypeName: "release-manager"`, `Model: "flash"`) or select `release-manager` directly in Antigravity so high-tier reasoning tokens are not spent on mechanical build, merge, tag, and release commands.

> [!IMPORTANT]
> Run this procedure only when the feature is complete on a `release/n.n.n/feature-name` branch, user approval is granted, and no new code will be added to the release.

## Steps

1. **Verify the feature branch and documentation**:
   - Confirm that work is on a branch named `release/n.n.n/feature-name` (never directly on `master`).
   - Confirm that `README.md`, `AGENTS.md`, module `AGENTS.md` files, and `docs/agents/` are updated and match the new features.
2. **Bump the Android version** in `Android/app/build.gradle` (and `AndroidWatch/build.gradle` if watch code changed):
   - Increase `versionCode` by `1`.
   - Increase `versionName` by `0.1` (for example, `'1.7'` to `'1.8'`).
3. **Build and verify both APKs and unit tests**:
   ```bash
   /opt/homebrew/bin/gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home --no-daemon testDebugUnitTest assembleDebug
   /opt/homebrew/bin/gradle -p AndroidWatch -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home --no-daemon testDebugUnitTest assembleDebug
   ```
   Verify that `Android/app/build/outputs/apk/debug/app-debug.apk` and `AndroidWatch/build/outputs/apk/debug/AndroidWatch-debug.apk` exist and are up to date.
4. **Commit on the feature branch and merge into `master`**:
   - Stage all modified source and documentation files and exclude `*.apk`.
   - Commit the changes on `release/n.n.n/feature-name` and merge the branch into `master`.
5. **Create the annotated Git tag on the merge commit**:
   - If changes (such as version bump or documentation) are added on the merge commit, stage them and run `git commit --amend`.
   - Create the annotated Git tag:
   ```bash
   git tag -a <n.n.n> -m "Release <n.n.n>"
   ```
6. **Push `master` and the tag to `origin`, and clean up the release branch**:
   ```bash
   git push origin master
   git push origin <n.n.n>
   git branch -d release/<n.n.n>/feature-name
   git push origin --delete release/<n.n.n>/feature-name
   ```
7. **Publish the GitHub Release with the APK assets**:
   - Create or update the GitHub Release for tag `<n.n.n>` with title `<n.n.n>`.
   - Upload `Android/app/build/outputs/apk/debug/app-debug.apk` as `DropShot-<n.n.n>.apk` and `AndroidWatch/build/outputs/apk/debug/AndroidWatch-debug.apk` as `DropShot-Watch-<n.n.n>.apk` using `gh release upload`.
