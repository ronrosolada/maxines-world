# Task 2.3 Report — Child-visible streak card

- **STATUS:** PASS
- **Commit hash(es):** `3f007c3b` (implementation commit)
- **Implementation:** Added an informational child-home learning-day card driven only by the existing `Content.streakDays` UI-state field. It includes zero-state and pluralized positive copy, a flame icon with merged number-and-meaning TalkBack content description, and a child-friendly static explanation dialog. The `home_streak` tag now targets the card; the Today’s Quest paw row no longer owns it.
- **Reduced motion:** The optional positive-streak celebration pop is gated by `LocalAnimationsDisabled`; reduced-motion unit and Compose coverage were added.
- **Tests:** `:feature-child-home:testDebugUnitTest`, `:feature-child-home:lintDebug`, `:feature-child-home:connectedDebugAndroidTest` (30/30), and `:app:assembleDebug` all passed.
- **Concerns:** None for Task 2.3. Existing `docs/superpowers/` untracked workspace content was left untouched; Gradle emitted the repository’s existing `SanctuaryScene.kt` warnings.

## Fix round 1

- **Status:** PASS
- **Findings addressed:** Updated `home_streak` documentation to describe the learning-day streak card; made the streak input nullable so Loading/Error headers do not fabricate a zero-state card; converted dialog copy to a singular-aware plural resource; and moved `No learning days yet.` into a string resource for the TalkBack label.
- **Regression coverage:** Added Loading and Error assertions that no streak card is rendered, plus singular dialog-copy coverage for one learning day. Existing zero-state, positive-state, tap, and reduced-motion coverage remains green.
- **Verification:**
  - `./gradlew :feature-child-home:testDebugUnitTest` — **BUILD SUCCESSFUL**
  - `./gradlew :feature-child-home:lintDebug` — **BUILD SUCCESSFUL**
  - `./gradlew :feature-child-home:connectedDebugAndroidTest` — **BUILD SUCCESSFUL**, 32/32 tests passed, 0 failed
  - `./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL**
- **Note:** The first connected-test compile caught a missing `onAllNodesWithTag` import in the new assertions; the import was added and the full connected suite was rerun successfully. The standalone `adb` command was not on PATH, but Gradle’s managed AVD executed the connected suite.

## Final fix

- **Status:** PASS
- **Finding addressed:** Removed the child-visible learning-day streak card from `PlayroomHeader` and placed it immediately after Today’s Quest inside the Content-only `ContentLayout` flow. Header balance, keepsakes, offline presentation, and all existing streak behavior remain unchanged.
- **Regression coverage:** Added deterministic Compose semantics-bound coverage asserting Today’s Quest precedes `home_streak`; existing zero-state, positive-state, tap-dialog, reduced-motion, and Loading/Error coverage remains in place.
- **Verification environment:** JDK 17 via `JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem`.
- **Exact verification commands/results:**
  - `export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem; export PATH="$JAVA_HOME/bin:$PATH"; ./gradlew :feature-child-home:testDebugUnitTest` — **BUILD SUCCESSFUL**
  - `export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem; export PATH="$JAVA_HOME/bin:$PATH"; ./gradlew :feature-child-home:lintDebug` — **BUILD SUCCESSFUL**
  - `export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem; export PATH="$JAVA_HOME/bin:$PATH"; ./gradlew :feature-child-home:connectedDebugAndroidTest` — **BUILD SUCCESSFUL**, **34/34 tests passed**, 0 skipped, 0 failed
  - `export JAVA_HOME=/home/ron/.sdkman/candidates/java/17.0.16-tem; export PATH="$JAVA_HOME/bin:$PATH"; ./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL**
  - `git diff --check` — **PASS**
- **Concerns:** None for this final fix wave. Existing untracked `docs/superpowers/` workspace content was left untouched.
