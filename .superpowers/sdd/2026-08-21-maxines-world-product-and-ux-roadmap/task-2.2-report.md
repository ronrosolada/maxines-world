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
