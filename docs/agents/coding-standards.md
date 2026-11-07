# Coding standard

This standard applies to the Java code in `Android/` and the JavaScript code in `ZeppOS/`.

## 1. General principles

1. Write correct code first. Then write clear code. Then optimize with measurements.
2. Make the smallest change that completes the task. Do not refactor code that the task does not need.
3. Use 1 responsibility for each class and each method (SOLID: single responsibility).
4. Prefer composition to inheritance. Keep inheritance for Android framework types.
5. Do not repeat logic. Extract a helper when the same logic occurs 3 times (DRY, rule of 3).
6. Do not add code for a future need (YAGNI).
7. Fail safe: on invalid input, keep the current state and return a safe default.
8. Make state changes deterministic. The same inputs must give the same score.

## 2. Java style

| Item | Rule |
|---|---|
| Indentation | 4 spaces. No tabs. |
| Line length | 120 characters or fewer. |
| Names | `UpperCamelCase` classes, `lowerCamelCase` methods and fields, `UPPER_SNAKE_CASE` constants. |
| Visibility | Use the smallest visibility. Use package-private for test hooks (example: `seedTemporaryInitialPlayers`). |
| Immutability | Use `final` fields. Copy mutable inputs (`clone()`, `new ArrayList<>(…)`). |
| Null | Check `null` at public boundaries. Do not return `null` collections. Return an empty list. |
| Exceptions | Throw `IllegalArgumentException` for invalid arguments, `SecurityException` for failed security checks. |
| Magic numbers | Use a named constant for each rule value, limit, or timeout. |
| Comments | Explain why, not what. Use STE. Keep existing control tags such as `[MASVS-STORAGE]`. |
| Language level | Java 11. Do not use records, `var` in public APIs, or text blocks (Java 15+). |

## 3. Domain logic

1. Keep score logic in `MatchState`. Keep statistics in `PlayerStatistics`. Keep both free of Android types.
2. A game ends when 1 player has `pointsToWinSet` points or more and a lead of 2 points or more.
3. The UI calls the game a "game". The code calls it a "set". Do not mix the 2 terms in 1 identifier.
4. Store each point winner (`1` or `2`) in order. Derive the sets, the statistics, and the charts from this list.
5. Keep `MatchRecord.playedSets()` and `scoreAtPoint()` consistent with `MatchState`.
6. Old records can have empty `pointTimesMs` or `setDurationsMs`. Handle empty lists.

## 4. Repositories and concurrency

1. Read from the in-memory cache. Do not read the disk on each screen draw.
2. Write to the cache first. Then persist on `AsyncRepositoryExecutor.runIo` (1 sequential thread).
3. Pass an immutable snapshot (`new ArrayList<>(CACHE)`) to the background task.
4. Use `CopyOnWriteArrayList` or `synchronized` blocks for shared state. Use `volatile` for flags.
5. Do not block the main thread with disk, crypto, or socket work.
6. Do not start a new thread for each task. Use the existing executors.
7. Post UI changes to the main thread (`Handler(Looper.getMainLooper())` or `runOnMain`).

## 5. Performance

Target: 60 Hz on old devices, 120 Hz on new devices. The frame budget is 16.6 ms (60 Hz) or 8.3 ms (120 Hz).

1. Do not allocate objects in `onDraw`, `onMeasure`, `onLayout`, or animator update listeners.
   Allocate `Paint`, `Path`, `RectF`, and arrays as fields. Reuse them.
2. Do not use `ArgbEvaluator` in hot paths. It boxes `Integer` values. Use primitive channel math (`RewardCatalog.blend`).
3. Cache shaders (`LinearGradient`) when the inputs do not change.
4. Cancel animators when the view detaches. Do not keep references to a destroyed activity (memory leak).
5. Calculate statistics for all profiles in 1 pass (`PlayerStatistics.calculateAll`). Do not loop history × profiles with I/O.
6. Keep the history limit (100 matches) and the point limit (5,000) unless the user asks for a change.
7. Measure before you optimize. Use Android Studio Profiler or `adb shell dumpsys gfxinfo`.

## 6. Lifecycle

1. `onResume`: start the clock ticker, the media session, and the watch bridge. Sync the selected profiles.
2. `onPause`: pause the match, stop the bridge, stop the pulse animations, hide the fireworks.
3. `onStop`: archive the current match.
4. `onDestroy`: remove all `Handler` callbacks. Release the media session.
5. `onConfigurationChanged`: rebuild the view. Keep the match state.

## 7. Tests

1. Write a unit test for each change to domain logic, validation, storage format, or security.
2. Test the normal case, the edge case, and the invalid case.
3. Make tests deterministic. Inject `Random` or `SecureRandom` where a roll occurs.
4. Do not depend on wall-clock time in assertions. Use tolerances or injected values.
5. Keep tests fast. A JVM unit test must not need a device.

## 8. JavaScript (Zepp OS)

1. Use ES modules and `const`/`let`. Do not use `var`.
2. Use only `@zos/*` and `@zeppos/zml` APIs. Check the API level in `app.json` before you use a new API.
3. Catch all errors from `request` and `fetch`. Do not retry score commands.
4. Keep the watch code small. Watch memory and battery are limited.
5. Use 2-space indentation and no semicolons, as the current files do.

## 9. Temporary code

1. Mark temporary code with a `TEMPORARY:` comment.
2. In the comment, write what the code does and when to remove it.
3. Keep temporary code in 1 place. Make it easy to delete.
4. List all temporary code in `docs/agents/caveats.md`.

