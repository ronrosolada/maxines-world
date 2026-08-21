# Task 2.2 Report — Child-home streak data path

## STATUS

COMPLETE

## Implementation

- Added `ProgressEventDao.observeTimestampsByChild(childId)`, selecting only `timestamp` from the existing `progress_events` table. No entity change, schema change, or migration was added.
- Added the child-home presentation adapter `streakDaysFromTimestamps(...)`. It converts epoch timestamps with the supplied zone and delegates live-streak calculation to the shared `currentLearningStreak(...)` function.
- Added stable `streakDays: Int = 0` to `PlayroomHomeUiState.Content`.
- Added a separately guarded streak collector to `PlayroomHomeViewModel`; streak read failures reset to zero without replacing otherwise usable home content.
- Preserved the existing `baseContent` plus single final publisher architecture. The final publisher now combines streak state alongside video assets/progress and copies only `streakDays` into the final content snapshot.
- Reset/restarted the streak collector with the existing retry path, so process recreation and retry do not retain stale in-memory streak data.
- Did not modify child-home UI/card rendering, text lesson content, legacy renderers, navigation, quest loading, or video progress behavior.

## Tests and verification

- `:feature-child-home:testDebugUnitTest` — 38 tests, 0 failures, 0 errors, 0 skipped.
- `PlayroomHomeViewModelTest` — 20 tests, 0 failures, 0 errors, 0 skipped; includes live today+yesterday streak, stale-zero, timestamp-adapter deterministic coverage, empty/missing-child, and streak database-error paths.
- `:feature-child-home:lintDebug` — passed.
- `:feature-child-home:connectedDebugAndroidTest` — 26/26 passed, 0 failures, 0 skipped.
- `:app:assembleDebug` — passed.
- `git diff --check` — passed.

## Commit

- Implementation: `1dade0f8` (`feat: add child-home streak data path`)

## Concerns

- The current connected child-home suite contains 26 tests; the prior Task 1.3 report referenced 24. All 26 current tests passed. No implementation blocker is known.

## Fix round 1

### Review findings addressed

- Missing child/profile: streak publication now combines `childProfileDao.observeById(childId)` with timestamp data. A null profile always publishes `streakDays = 0`, including when today's timestamp rows exist, while the rest of the home content remains usable.
- Local midnight rollover: child-home streak collection now combines its data flows with a lifecycle-scoped local-date trigger. The trigger emits immediately, schedules the next local midnight using `atStartOfDay(zone)`, handles DST-length days, and is cancellation-safe without a busy loop. Timestamp conversion remains in `ZoneId.systemDefault()` and calculation remains delegated to `localLearningDates` / `currentLearningStreak`.
- Added deterministic coverage for missing profile + today's timestamp, the same timestamps across successive local dates, trigger advancement without sleeping, and spring/fall DST midnight delays.
- Preserved the existing base-content/final-publisher architecture, retry reset, parent mode, database-error handling, video progress behavior, and the no-legacy-content/no-schema-change constraint.

### Tests and verification

- `./gradlew :feature-child-home:testDebugUnitTest --tests com.maxinesworld.featurechildhome.PlayroomHomeViewModelTest` — BUILD SUCCESSFUL; 24 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew :feature-child-home:testDebugUnitTest` — BUILD SUCCESSFUL; 42 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew :feature-child-home:lintDebug` — BUILD SUCCESSFUL.
- `adb devices` — emulator `emulator-5554` available.
- `./gradlew :feature-child-home:connectedDebugAndroidTest` — BUILD SUCCESSFUL; 26/26 tests passed, 0 skipped, 0 failed.
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL.
- `git diff --check` — passed.
