# Task 2 implementation report — video-based Daily Mission

Date: 2026-08-22
Branch base: `cb6cb71d` (`fix: retire duplicate video daily mission path`)

## Scope implemented

- Updated the child-home quest handler in `MaxinesNavGraph.kt`:
  - `QuestAction.OpenVideoQuest` now resolves `quest.nextMediaId` to the matching `QuestTargetUi` and navigates to `Routes.videoLibrary(childId, target.subjectId)`.
  - Removed mission-handler subject fallback behavior. The handler no longer falls back to `recommendedSubjectId`, `Routes.lessonPlayer`, or subject-module navigation.
  - `QuestAction.Continue` now always navigates to `Routes.videoLibrary(childId)` without a subject filter.
  - Existing legitimate module-lesson navigation and lesson-player routes remain unchanged.
- Updated `TodayQuestCard.kt`:
  - Each target renders its actual catalog title.
  - Each target renders its catalog duration as a visible `mm:ss` chip.
  - Target accessibility descriptions include the title and formatted duration.
  - Preserved the merged clickable card semantics, nested target click actions, and one visible primary CTA.
- Updated `PlayroomHomeInteractionContractTest.kt` to assert the new target accessibility label and duration chip.
- Preserved the normal `VideoLibraryViewModel` and `VideoLibraryScreen` browsing, playback, download, assessment, and progress paths; no duplicate quest/banner/bonus path was reintroduced.
- Mission copy in `mw_option3_strings.xml` already used “learning adventures” / “video mission” rather than calling mission items lessons, so no unrelated copy was changed.
- Pre-existing untracked `docs/superpowers/` content was left untouched.

## Files modified

- `android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt`
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/TodayQuestCard.kt`
- `android/feature-child-home/src/androidTest/java/com/maxinesworld/featurechildhome/PlayroomHomeInteractionContractTest.kt`
- `.superpowers/sdd/2026-08-22-maxines-world-video-daily-mission/task-2-report.md` (this report)

## Verification environment

JDK 17 was selected explicitly for every Gradle command:

```text
JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
```

Working directory for Gradle commands: `/home/ron/projects/maxines-world/android`.

## Exact verification commands and output

### Unit tests

Command:

```bash
export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :feature-child-home:testDebugUnitTest
```

Output:

```text
> Task :feature-child-home:testDebugUnitTest

BUILD SUCCESSFUL in 3s
97 actionable tasks: 4 executed, 93 up-to-date
```

### Lint

Command:

```bash
export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :feature-child-home:lintDebug
```

Output:

```text
> Task :feature-child-home:lintReportDebug
Wrote HTML report to file:///home/ron/projects/maxines-world/android/feature-child-home/build/reports/lint-results-debug.html

> Task :feature-child-home:lintDebug

BUILD SUCCESSFUL in 9s
265 actionable tasks: 12 executed, 253 up-to-date
```

### Connected Android tests

The first run exposed a stale contract assertion after the accessibility label was intentionally expanded to include duration:

```text
java.lang.AssertionError: Failed to assert the following: (OnClick is defined)
Reason: Expected exactly '1' node but could not find any node that satisfies: (ContentDescription = 'Quest target: English: Word Roots · 01:00' (ignoreCase: false))
...
> Task :feature-child-home:connectedDebugAndroidTest FAILED
```

The target content description was then updated to include the formatted duration. The required command was rerun successfully.

Command:

```bash
export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :feature-child-home:connectedDebugAndroidTest
```

Output:

```text
> Task :feature-child-home:connectedDebugAndroidTest
Starting 36 tests on MaxinesWorld(AVD) - 15

MaxinesWorld(AVD) - 15 Tests 32/36 completed. (0 skipped) (0 failed)
Finished 36 tests on MaxinesWorld(AVD) - 15

BUILD SUCCESSFUL in 1m 17s
184 actionable tasks: 12 executed, 172 up-to-date
```

### Debug APK assembly

Command:

```bash
export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug
```

Output:

```text
> Task :app:assembleDebug

BUILD SUCCESSFUL in 4s
422 actionable tasks: 15 executed, 407 up-to-date
```

### Whitespace check

Command:

```bash
git diff --check
```

Output:

```text
(no output; exit code 0)
```

## Final diff scope

Before committing, `git diff --name-status` showed only the three intended implementation/test files as modified. The pre-existing untracked `docs/superpowers/` directory remained outside the diff and was not staged.

The implementation is committed with the required `feat:` message; the finalized commit hash is returned in the task completion response.

## Concerns

- No implementation concerns remain after the corrected connected-test run.
- The initial connected-test failure was a test expectation mismatch caused by the intentional duration-inclusive accessibility label; the rerun passed all 36 tests.

## Fix round 1 — subject-aware target-row routing

The scoped review finding was fixed without changing the enum-based primary CTA. Target rows now use an explicit `onQuestTargetClick(subjectId)` callback, wired through `TodayQuestCard` → `PlayroomHomeScreen` → `MaxinesNavGraph`, where the callback navigates only to `Routes.videoLibrary(childId, subjectId)`. `QuestAction.OpenVideoQuest` remains unchanged for the primary CTA and still resolves the current `nextMediaId`; `Continue` remains the unfiltered video-library route.

The interaction contract now renders English and Science target rows, taps the non-next Science row first, and asserts the callback receives `science` followed by `english` when the English row is tapped. It also asserts neither target-row tap dispatches `QuestAction.OpenVideoQuest` and that the primary Continue action remains unchanged. This proves a row cannot fall back to the next target's subject.

Fresh JDK 17 verification for this fix round:

```text
:feature-child-home:testDebugUnitTest       BUILD SUCCESSFUL (97 actionable tasks)
:feature-child-home:lintDebug               BUILD SUCCESSFUL (265 actionable tasks)
:feature-child-home:connectedDebugAndroidTest
                                             BUILD SUCCESSFUL (36 tests, 0 skipped, 0 failed)
:app:assembleDebug                          BUILD SUCCESSFUL (422 actionable tasks)
git diff --check                            exit 0; no output
```

No lesson JSON, renderer, schema, migration, duplicate Video Library quest, or legitimate non-mission lesson flow was modified. The pre-existing untracked `docs/superpowers/` content remains untouched and unstaged.

## Fix round 2 — required target callback seam

The remaining review correction removes the silent no-op default from the public `PlayroomHomeScreen` callback seam. `ContentLayout` and `TodayQuestCard` already required `onQuestTargetClick`; all production and test callers now pass an explicit callback, using `{}` only where a caller intentionally does not exercise target-row navigation. The existing route wiring remains unchanged: target rows emit their own `subjectId`, and `MaxinesNavGraph` routes that value to `Routes.videoLibrary(childId, subjectId)`.

The interaction contract continues to prove both required behaviors: tapping the non-next Science target records `science` (and the English target records `english`), while target-row taps leave the primary CTA action list unchanged at `Continue`; tapping the primary card and visible Continue CTA records only the expected Continue actions.

Fresh JDK 17 verification for this fix round:

```text
:feature-child-home:testDebugUnitTest       BUILD SUCCESSFUL (97 actionable tasks)
:feature-child-home:lintDebug               BUILD SUCCESSFUL (265 actionable tasks)
:feature-child-home:connectedDebugAndroidTest
                                             BUILD SUCCESSFUL (36 tests, 0 skipped, 0 failed)
:app:assembleDebug                          BUILD SUCCESSFUL (422 actionable tasks)
git diff --check                            exit 0; no output
```

Only the required callback signature/call-site test updates and this evidence section were changed in this fix round. The pre-existing untracked `docs/superpowers/` content remains untouched and unstaged.
