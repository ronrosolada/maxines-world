# Task 4 report — mixed video + Assessment Arena Daily Mission

Date: 2026-08-22
Branch: `feat/video-daily-mission`
Base: `a962afe5`

## Outcome

Implemented the user-approved mixed Daily Mission composition without changing the Room schema, Assessment Arena internals, bundled lesson JSON/renderers, sticker policy, or `app/proguard-rules.pro`.

- One deterministic frontier video is always the first mission slot.
- Up to two unpassed Grade-3 Assessment Arena packs are appended in bundled catalog order.
- When fewer than two packs remain, additional deterministic frontier videos fill the remaining slots.
- Arena IDs are persisted as `arena:<packId>`; video IDs remain opaque media IDs in the existing `daily_quest_sets` payload.
- Arena completion is credited from existing `assessment_arena_passed:<packId>` reward metadata rows.
- `DailyQuestRewardWriter` remains the only daily mission reward minter and now recognizes arena completion rows while preserving video-ledger validation.
- Quest targets are typed `VIDEO` / `ARENA`; arena targets resolve offline from the bundled pack catalog, render the pack title plus `Quiz`, and route to Assessment Arena. Video targets retain subject video-library routing.
- Unresolved mixed targets remain `Unavailable` + `Retry`; no legacy lesson fallback was added.
- `VideoQuestPlanner.MAX_SECONDS` is 3000 seconds (50 minutes), with updated KDoc and test wording.

## Files changed

Production:

- `android/core-model/src/main/java/com/maxinesworld/coremodel/VideoQuestPlanner.kt`
- `android/core-database/src/main/java/com/maxinesworld/coredatabase/Daos.kt`
- `android/feature-rewards/src/main/java/com/maxinesworld/featurerewards/DailyQuestRewardWriter.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/DailyQuestManager.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/QuestTargetResolver.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModel.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/TodayQuestCard.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeScreen.kt`
- `android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt`

Tests:

- `android/core-model/src/test/java/com/maxinesworld/coremodel/VideoQuestPlannerTest.kt`
- `android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/QuestTargetResolverTest.kt`
- `android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModelTest.kt`
- `android/feature-child-home/src/androidTest/java/com/maxinesworld/featurechildhome/DailyQuestManagerTest.kt`
- `android/feature-child-home/src/androidTest/java/com/maxinesworld/featurechildhome/PlayroomHomeInteractionContractTest.kt`

## Exact verification commands and results

All Gradle commands below ran from `/home/ron/projects/maxines-world/android` with JDK 17:

```bash
export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Result: OpenJDK 17.0.16 (`17.0.16`, Temurin).

### Initial compile probe

```bash
./gradlew :core-model:testDebugUnitTest :feature-child-home:testDebugUnitTest :feature-child-home:compileDebugAndroidTestKotlin :app:compileDebugKotlin
```

Result: first iteration failed only because the changed `onQuestTargetClick` callback type had two stale Android-test fixture declarations. Those fixtures were updated; no production failure remained.

### Corrected compile and JVM tests

```bash
./gradlew :feature-child-home:testDebugUnitTest :feature-child-home:compileDebugAndroidTestKotlin :app:compileDebugKotlin
```

Result: `BUILD SUCCESSFUL in 6s`; `306 actionable tasks: 10 executed, 1 from cache, 295 up-to-date`.

### Connected test iteration

```bash
export PATH="$JAVA_HOME/bin:/home/ron/android-sdk/platform-tools:$PATH"
./gradlew :feature-child-home:connectedDebugAndroidTest
```

Result: first mixed-behavior iteration failed 4/38 because pre-amendment expectations assumed two video-only assignments and two recovery slots. Tests were corrected to assert mixed composition and the real one-pack/two-video fallback.

### Connected Android gate after behavior corrections

```bash
./gradlew :feature-child-home:connectedDebugAndroidTest
```

Result: `BUILD SUCCESSFUL in 1m 18s`; 38/38 tests completed, 0 skipped, 0 failed.

### Required unit, lint, and debug assemble gates

```bash
./gradlew :core-model:testDebugUnitTest :feature-child-home:testDebugUnitTest :feature-child-home:lintDebug :app:assembleDebug
```

Result: `BUILD SUCCESSFUL in 23s`; `580 actionable tasks: 182 executed, 15 from cache, 383 up-to-date`.

### Final connected Android gate after the arena UI contract test

```bash
export PATH="$JAVA_HOME/bin:/home/ron/android-sdk/platform-tools:$PATH"
./gradlew :feature-child-home:connectedDebugAndroidTest
```

Result: `BUILD SUCCESSFUL in 1m 21s`; 39/39 tests completed, 0 skipped, 0 failed.

### Final whitespace gate

```bash
cd /home/ron/projects/maxines-world
git diff --check
```

Result: exit 0; no output.

## Coverage specifically added or verified

- Bundled catalog titles selected for arena slots begin with `Grade 3`; catalog order is stable.
- Existing arena reward metadata rows exclude passed packs and complete assigned `arena:<packId>` slots.
- One unpassed pack produces one arena target plus two frontier videos; this is an instrumentation test with two 20-minute frontier videos, exercising the raised 3000-second planner ceiling.
- Arena targets resolve without a video catalog and preserve pack identity, title, typed state, and completion state.
- Arena UI exposes a `Quiz` indicator and sends the typed target/pack identity through the target callback; video rows retain subject/duration behavior.
- Missing arena/video metadata remains retryable and never falls back to lesson IDs.

## Concerns

- The initial connected run was an expected test-adjustment failure after changing the contract from video-only to mixed targets; the final gate is green at 39/39.
- Assessment Arena's existing route API is subject-based, so mixed target navigation enters the existing Assessment Arena route for the target pack's subject without modifying Arena internals.
- Pre-existing untracked `docs/superpowers/` and `android/tools/mark_media_released.py` were not staged by this task.

## Task 4 fix-round evidence

All commands below ran from `/home/ron/projects/maxines-world/android` with JDK 17. The report is intentionally append-only for this fix round.

```bash
export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Result: OpenJDK 17.0.16 (`17.0.16`, Temurin).

### Regression test written first (RED)

```bash
./gradlew :feature-child-home:testDebugUnitTest --tests com.maxinesworld.featurechildhome.PlayroomHomeViewModelTest --no-daemon
```

Result: expected failure in the new `arena reward metadata emission refreshes daily mission inputs` test (1 failed); this reproduced the missing reactive RewardDao observation before the production fix.

### Corrected JVM/unit and route-contract gates

```bash
./gradlew :core-model:testDebugUnitTest :feature-child-home:testDebugUnitTest :app:testDebugUnitTest --no-daemon
```

Result: `BUILD SUCCESSFUL in 17s`; core-model, feature-child-home, and app unit tests passed.

### Android-test compilation gate

```bash
export PATH="$JAVA_HOME/bin:/home/ron/android-sdk/platform-tools:$PATH"
./gradlew :feature-child-home:compileDebugAndroidTestKotlin --no-daemon
```

Result: `BUILD SUCCESSFUL in 17s`.

### Required connected Android gate

```bash
./gradlew :feature-child-home:connectedDebugAndroidTest --no-daemon
```

Result: `BUILD SUCCESSFUL in 1m 49s`; 42/42 tests completed, 0 skipped, 0 failed.

### Required unit, lint, and debug assemble gates

```bash
./gradlew :core-model:testDebugUnitTest :feature-child-home:testDebugUnitTest :feature-child-home:lintDebug :app:assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL in 1m`; all requested tasks passed, including `:feature-child-home:lintDebug` and `:app:assembleDebug`.

### Required whitespace gate

```bash
cd /home/ron/projects/maxines-world
git diff --check
```

Result: exit 0; no output.

### Fix-round coverage

- `PlayroomHomeViewModel` now combines `RewardDao.observeByChild(childId)` and passes the observed Arena pack IDs into daily-quest reconciliation, so an asynchronous `assessment_arena_passed:<packId>` reward refreshes the home mission.
- Arena route construction carries nullable `packId`; both mission CTA and target-row actions preserve `target.arenaPackId`, while null pack IDs retain normal subject browsing. The route starts the assigned pack through the existing Arena ViewModel API without changing Arena internals.
- Sparse planner rejection now composes one deterministic valid frontier video when Arena slots exist; multi-video planner fallback remains ordered and deterministic.
- Added coverage for planner-one-video ordered fallback, zero unpassed Arena packs, sparse one-video plus two-Arena composition, reactive Arena reward refresh, and distinct video/Arena route contracts.
- Updated stale 40-minute planner comments to 50 minutes. No schema, content, renderer, sticker-policy, or R8 keep-rule changes were made.
