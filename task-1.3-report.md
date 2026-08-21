# Task 1.3 Report

## Round 1 fix

- Refactored `PlayroomHomeViewModel` to keep the main home snapshot in `baseContent` and publish child-facing `Content` from one `combine(baseContent, videoAssets, passedVideoIds)` collector.
- Video catalog and passed-media observers now update only their own flows; an older home snapshot cannot overwrite newer video counts.
- Added `withVideoProgress()` as the deterministic derived-publication helper. It changes only subject video counts and preserves quest, navigation, balances, rewards, and other home state.
- Retry clears `baseContent`, resets navigation selection, publishes `Loading`, and restarts the existing collectors. Main-load exceptions still publish the existing retryable `Error`; optional video-catalog failures remain unavailable progress rather than errors.
- Added deterministic unit coverage for the helper and for latest passed IDs winning while an older base-content build is suspended. Tests use coroutine test controls and deferred gates; no sleeps.
- No text lesson files, text renderers, database migrations, or schema files were changed.

## Verification

- `./gradlew :feature-child-home:testDebugUnitTest` — 15 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew :feature-child-home:lintDebug` — passed.
- `./gradlew :feature-child-home:connectedDebugAndroidTest` — 24 tests, 0 failures, 0 skipped.
- `./gradlew :app:assembleDebug` — passed.
