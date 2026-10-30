---
name: release-version-bump
description: Bump the DropShot version, build the APK, tag the release, and publish the GitHub Release with the APK asset attached.
---

# Release and Version Bump Procedure

> [!IMPORTANT]
> Run this procedure only when the feature is complete and no new code will be added to the release.

## Steps

1. **Bump the Android version** in `Android/app/build.gradle`:
   - Increase `versionCode` by `1`.
   - Increase `versionName` by `0.1` (for example, `'1.2'` to `'1.3'`).
2. **Build and verify the APK**:
   ```bash
   /opt/homebrew/bin/gradle -p Android -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home --no-daemon testDebugUnitTest assembleDebug
   ```
   Verify that `Android/app/build/outputs/apk/debug/app-debug.apk` exists and is up to date.
3. **Commit and merge to `master`**:
   - Stage all modified source files and exclude `*.apk`.
   - Commit the changes and merge the branch into `master`.
4. **Create the annotated Git tag**:
   ```bash
   git tag -a 0.1.3 -m "Release 0.1.3"
   ```
5. **Push `master` and the tag to `origin`**:
   ```bash
   git push origin master
   git push origin 0.1.3
   ```
6. **Publish the GitHub Release with the APK asset**:
   - Create a GitHub Release for tag `0.1.3` with title `0.1.3`.
   - Upload `Android/app/build/outputs/apk/debug/app-debug.apk` (`Punkty-Squash.apk`) as a binary release asset.
