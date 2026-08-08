# Quest Targets + UX Tickets — Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Make Today's Quest completable and legible (show three quest lessons, route the CTA to them, count progress) and fix the remaining adversarial UX regressions (sort/MCQ affordance, sort wording, module labels, filler copy, auth affordance).

**Architecture:** One plan, staged: Phase A (P1 quest) then Phase B (remaining tickets). Quest is a single vertical slice: persist deterministic daily targets → enrich view-model → surface targets on the home card → route the CTA correctly → mark completion in the lesson repository. Follow-on fixes are renderer/content/auth small PRs. All changes behind existing unit/connected gates; no new permissions or migrations.

**Tech Stack:** Kotlin, Jetpack Compose, Room v9, Hilt, Coroutines Flow, ModuleCatalog/ContentLessonLoader, PlayroomHome*, LessonPlayerViewModel/LessonCompletionRepository.

---

## Context and current behavior (derived from repo)

- **Quest pick:** `DailyQuestManager.ensureToday()` creates one `DailyQuestSetEntity(childId, dayKey)` per day holding JSON `assignedQuestIds` (3 ids) via `DailyQuestPlanner.selectQuestIds(childId:dayKey, completed, available)`. `available` = all catalog lessons across `DAILY_QUEST_SUBJECTS` (mathematics, english, science, filipino, araling-panlipunan, makabansa, gmrc) with unfinished prioritized. Progress is `completedCount = getCompletedQuestIds ∩ assigned`, synthesized back via `lesson_completions` (`observeDistinctLessonIds`). Source: `DailyQuestManager.kt:28-97`, `PlayroomHomeViewModel.kt:57-79`.
- **Quest UI today:** `QuestUi(task:String, pawPrintsCompleted:Int, pawPrintTotal:Int, recommendedSubjectId:String?, buttonLabel:String, buttonAction:QuestAction)` with task = `"Complete N learning adventures today."`, three paw glyphs, button `Start|Continue|Choose a subject|Open Field Guide`. No per-lesson list is surfaced. `recommendedSubjectId = subjects.firstAvailable` — so the CTA opens `subject_modules/{childId}/{subject}` for e.g. `mathematics` even when the three quest ids are e.g. `english-…`. `PlayroomHomeUiState.kt`, `PlayroomHomeViewModel.kt:147-176`, `MaxinesNavGraph.kt:154-185`.
- **Module routing:** `SubjectModulesScreen` lists modules for one subject; `ModuleLessonsScreen` lists lessons for one module (with `nextLessonId` resume). There is **no** direct `lesson_player/{lessonId}` quest deep-link today; `MAXINES_NAV` routes are `child_home`, `subject_modules`, `module_lessons`, `lesson_player`. `MaxinesNavGraph.kt`.
- **Sort wording defect:** Lesson `mathematics-g3-m01-d01` sort instruction = `"Sort each card: is the statement about 4,352 or 2,406 true?"` but bucket labels were shown as `Fits / Does not fit` (English) after the category rewrite, so the card phrasing + bucket label produce the reported `"… true? Fits?"` confusion and one distractor (`"10,000 is ten thousands"` — note the trailing *s*) is arguably mis-keyed. The typed model now parses `content.fits/doesNotFit/categories` and `sortInstructionWithCategories()` replaces `true/false` with bucket labels; the legacy `shows the skill` mapping is unrelated. `LessonPlayerViewModel.kt:317-457`, `math-g3-m01-d01.json:73-99`, `ActivityStepConversionTest`.
- **MCQ/sort affordance:** `MultipleChoiceRenderer` disables Submit when no selection and keeps it that way on retry; sort's `Submit` currently requires all cards placed (`placedCount == items.size`). Sort state machine has a dedicated submit bar that doubles as retry/success navigation. `MultipleChoiceRenderer.kt:186-224`, `SortAndClassifyRenderer.kt:54-186`.
- **Week labels:** SLM module keys are `qN-wMM` and `ModuleIdRules.moduleTitle(qN-wMM)` yields `"Quarter N · Week M"` (e.g. `q2-w04 → Quarter 2 · Week 4`), which is the reported `"Quarter 2 · Week 3/4"` sequence. This is not a bug in rendering — it's a consecutive pair of weekly modules; the reporter expects the week counter to reset per quarter (it doesn't; it is global weekly sequencing within the term). `ModuleCatalog.kt:181-217`, `ModuleIdRulesTest`.

---

## Decisions to lock before Phase B

- **D1 — Quest surface.** Minimal honest surface: **list the three quest lessons directly on the Today's Quest card** (one row per lesson: `subject icon + friendly lesson title + check/done`), drawn from a new `QuestTargetUi` list on the home state. Keep the existing paw row. CTA becomes **Go to quest** that scrolls/focuses the list when needed (or a per-row `Play` shortcut — see Task 5 variant). This avoids adding a new top-level route.
- **D2 — CTA targeting.** Make the CTA quest-aware: if there is an uncompleted quest lesson, deep-link straight to its `lesson_player/{lessonId}`; otherwise open the subject's module list for the next uncompleted quest lesson's module. Fallback remains `subject_modules/{availableFirst}` when no target is resolvable. Requires lesson-id → `(subject, moduleKey)` resolution via `ModuleCatalog` (pure ID parsing via `ModuleIdRules` is sufficient for routing).
- **D3 — Quest completion signal.** Count a quest lesson as done **when `LessonCompletionRepository.complete()` commits**, not on view. This already drives `observeDistinctLessonIds` → `ensureToday` reconciliation, so no new table is added — but we standardize on the post-commit flow (Task 8).
- **D4 — Module label expectation.** Close as **by design — no code change** for the week-number reset. Add a one-line doc note in `ModuleCatalog.kt` header clarifying SLM week sequencing so the ticket's expectation is addressed without renaming real modules.
- **D5 — Remaining tickets scope.** One PR group (Phase B): sort wording fix (instruction + bucket copy), sort `Submit` affordance guard, MCQ silent-no-op guard, filler assessment explanation sweep, optional auth affordance. Nothing that widens the reward/quest surface beyond the card.

---

## Phase A — Quest targets (P1) — make the quest completable, legible, and honest

### Task A1: Add `QuestTargetUi` and extend `QuestUi` to carry the three targets

**Objective:** Home state can carry the three quest lessons (with done/pending) without breaking existing tests.

**Files:**
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt:37-48, 101-134`
- Modify: `android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModelTest.kt:157-206` (add coverage)

**Step 1: Write failing test**

```kotlin
// PlayroomHomeViewModelTest.kt — new test
@Test fun `quest carries three lesson targets with checkmarks`() = runTest(dispatcher) {
  val vm = buildViewModel(completedLessons = listOf("mathematics-g3-m01-d01"))
  advanceUntilIdle()
  val quest = content(vm).quest
  assertEquals(3, quest.targets.size)
  assertTrue(quest.targets.any { it.isCompleted })
  quest.targets.forEach { assertTrue(it.title.isNotBlank()) }
}
```

**Step 2: Run test to verify failure**

Run: `./gradlew :feature-child-home:test --tests "*PlayroomHomeViewModelTest*quest*targets*" -q`
Expected: FAIL — `Unresolved reference: targets`

**Step 3: Write minimal implementation**

```kotlin
// PlayroomHomeUiState.kt — new types
data class QuestTargetUi(
  val lessonId: String,
  val title: String,            // friendlyLessonTitleOf(...)
  val subject: String,          // pack subject e.g. "english"
  val displaySubject: String,   // subjectDisplayName(subject)
  val moduleKey: String?,       // ModuleIdRules.moduleKeyFor(lessonId)
  val isCompleted: Boolean,
)

data class QuestUi(
  val task: String,
  val pawPrintsCompleted: Int,
  val pawPrintTotal: Int,
  val isComplete: Boolean = false,
  val recommendedSubjectId: String? = null,
  val buttonLabel: String = "",
  val buttonAction: QuestAction = QuestAction.Continue,
  val targets: List<QuestTargetUi> = emptyList(), // NEW — default keeps old tests green
)
```

Keep existing fields; `targets` defaults to `emptyList()` so every current producer compiles.

**Step 4: Run test to verify pass**

Run: `./gradlew :feature-child-home:test --tests "*PlayroomHomeViewModelTest*" -q`
Expected: PASS (new test green, old tests still green because `targets` defaults)

**Step 5: Commit**

```bash
git add android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt \
        android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModelTest.kt
git commit -m "feat(home): carry quest lesson targets on QuestUi"
```

---

### Task A2: Populate `targets` in `PlayroomHomeViewModel.buildContent()` via `ModuleCatalog` + `DailyQuestProgress`

**Objective:** Targets are real (titles + subject + moduleKey + done) using only on-device data; no new DAO added.

**Files:**
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModel.kt:117-199`
- Create helper: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/QuestTargetResolver.kt` (pure, unit-testable)

**Step 1: Write failing test**

```kotlin
// New file: QuestTargetResolverTest.kt
@Test fun `resolver maps lessonIds to friendly titles and completed flags`() {
  val catalog = catalogWithLessons(mapOf(
    "mathematics-g3-m01-d01" to "Building Numbers to 10,000",
    "english-g3-q1-w01-d02" to "Picture Detective"
  ))
  val targets = QuestTargetResolver.resolve(
    assigned = listOf("mathematics-g3-m01-d01", "english-g3-q1-w01-d02", "science-g3-m01-d03"),
    completed = setOf("mathematics-g3-m01-d01"),
    catalog = catalog
  )
  assertEquals("Building Numbers to 10,000", targets[0].title)
  assertTrue(targets[0].isCompleted); assertFalse(targets[1].isCompleted)
  assertEquals("m01", targets[0].moduleKey); assertEquals("q1-w01", targets[1].moduleKey)
}
```

**Step 2: Run test to verify failure**

Run: `./gradlew :feature-child-home:test --tests "*QuestTargetResolverTest*" -q`
Expected: FAIL — `Unresolved reference: QuestTargetResolver`

**Step 3: Write minimal implementation**

```kotlin
// QuestTargetResolver.kt
object QuestTargetResolver {
  suspend fun resolve(
    assigned: List<String>, completed: Set<String>, catalog: ModuleCatalog
  ): List<QuestTargetUi> = assigned.map { id ->
    val title = lookupFriendlyTitle(id, catalog) // catalog.modulesFor(subject) scan; cache friendly
    val subject = subjectForLessonId(id) // ModuleIdRules + subject prefix parse; reuse subjectForPack
    QuestTargetUi(
      lessonId = id, title = title ?: friendlyLessonTitleOf(id),
      subject = subject, displaySubject = subjectDisplayName(subject),
      moduleKey = ModuleIdRules.moduleKeyFor(id),
      isCompleted = id in completed
    )
  }
  // lookupFriendlyTitle: iterate ModuleCatalog.modulesFor(subjectForLessonId(id)) to find lesson
}
```

In `PlayroomHomeViewModel.buildContent()`, after computing `questTotal/completedCount`, call `QuestTargetResolver.resolve(dailyQuest.assignedQuestIds, completed, catalog)` and pass `targets` into `QuestUi(...)`. Provider for `subjectForLessonId` is: prefix before `-g3-` (e.g. `mathematics`), normalized via `subjectForPack` with `araling-panlipunan → makabansa` mapping already present.

**Step 4: Run tests**

Run: `./gradlew :feature-child-home:test -q`
Expected: PASS — resolver test green, ViewModel tests adapt (update `buildViewModel` to provide a catalog stub that covers resolver's lookup so titles don't fall back to id string if you want exact assertions; or assert only non-empty + completed flag).

**Step 5: Commit**

```bash
git add android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/QuestTargetResolver.kt \
        android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModel.kt \
        android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/QuestTargetResolverTest.kt
git commit -m "feat(home): resolve quest lesson titles and completion for the card"
```

---

### Task A3: Render the three targets on `TodayQuestCard` (honest list + checkmarks, no new route yet)

**Objective:** The Today card shows three tappable-adjacent rows so a child/parent can read which lessons count; existing button and paws remain.

**Files:**
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeScreen.kt:753-861` (`TodayQuestCard`)
- Modify: `android/feature-child-home/src/main/res/values/mw_option3_strings.xml` (add `home_quest_target_done`, `home_quest_target_pending` a11y strings)
- Test: `android/feature-child-home/src/androidTest/java/com/maxinesworld/featurechildhome/PlayroomHomeScreenTest.kt` (add compose test for card with 3 targets)

**Step 1: Write failing test**

```kotlin
// PlayroomHomeScreenTest.kt
@Test fun todayQuest_showsThreeTargetsWithDoneMarker() {
  composeTestRule.setContent { TodayQuestCard(quest = QuestUi(targets = listOf(
    QuestTargetUi("a","Building Numbers to 10,000","mathematics","Mathematics","m01",true),
    QuestTargetUi("b","Picture Detective","english","English","q1-w01",false),
    QuestTargetUi("c","Plant Quest","science","Science","m01",false)
  ), onQuestAction = {}) }
  onNodeWithText("Building Numbers to 10,000").assertExists()
  onNodeWithContentDescription("Quest target done: Building Numbers to 10,000").assertExists()
}
```

**Step 2: Run test to verify failure**

Run: `./gradlew :feature-child-home:connectedDebugAndroidTest -q` (or `:feature-child-home:connectedAndroidTest`)
Expected: FAIL — target rows not found.

**Step 3: Write minimal implementation**

Inside `TodayQuestCard`, after the paw row and before the CTA `Surface`, add:

```kotlin
if (quest.targets.isNotEmpty()) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    quest.targets.forEach { t ->
      Row(Modifier.fillMaxWidth().semantics { contentDescription =
        if (t.isCompleted) "Quest target done: ${t.title}" else "Quest target: ${t.displaySubject}: ${t.title}"
      }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape)
          .background(if (t.isCompleted) PlaySunshine else PlayMuted.copy(alpha=0.14f)),
          contentAlignment = Alignment.Center) {
          if (t.isCompleted) Text("✓", fontWeight=FontWeight.Black, color=PlayInkDark)
          else Text(t.displaySubject.first().toString(), fontWeight=FontWeight.Bold, color=PlayMuted)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
          Text(t.displaySubject, fontSize=12.sp, fontWeight=FontWeight.Black, color=PlayMuted)
          Text(t.title, fontSize=15.sp, fontWeight=FontWeight.Bold, color=PlayInk, maxLines=2)
        }
      }
    }
  }
  Spacer(Modifier.height(14.dp))
}
```

No click handling on the rows in this task — legibility first, routing in the next task.

**Step 4: Run tests + screenshot check**

Run: `./gradlew :feature-child-home:connectedDebugAndroidTest -q`
Expected: PASS. Verify at font scale 2.0 titles wrap (maxLines=2) and don't ellipsize the subject/target row count.

**Step 5: Commit**

```bash
git add android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeScreen.kt \
        android/feature-child-home/src/main/res/values/mw_option3_strings.xml
git commit -m "feat(home): show the three quest lessons on the Today card"
```

---

### Task A4: Wire `DailyQuestManager` so the quest targets are deterministic and never hidden (no-ops already present)

**Objective:** Confirm no change needed to selection (it already prefers unfinished, stable per `childId:dayKey`). This task is a regression test, not a code change, unless the reviewer wants `qm01` vs SLM weighting — deferred.

**Files:** none (test only)
- Modify: `android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/DailyQuestPlannerTest.kt` (strengthen)

**Step 1: Write failing test**

```kotlin
@Test fun `unfinished lessons are prioritized over finished`() {
  val available = listOf("a","b","c","d","e")
  val completed = setOf("a","b")
  val picked = DailyQuestPlanner.selectQuestIds("child", "2026-08-08", completed, available, 3)
  assertFalse(picked.contains("a")); assertFalse(picked.contains("b"))
}
```

**Step 2/3:** Run, then keep implementation as-is (no production change; the assertion already holds by the `unfinished + finished` prioritization on line 37-38 of `DailyQuestManager.kt`).

**Step 4:** `./gradlew :feature-child-home:test -q` PASS.

**Step 5:** No commit unless test was added — if added, commit it.

---

### Task A5: Make the CTA (and per-row Play) open the quest lesson directly — `lesson_player/{childId}/{lessonId}`

**Objective:** Quest progress becomes completable in one tap; the button takes the child to the actual quest content, not the first available subject.

**Files:**
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt` (add `questActionTargets: List<String>` or reuse `targets` + a `nextUncompletedLessonId` derived prop on `QuestUi`)
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModel.kt:147-176` (compute `nextUncompleted = targets.firstOrNull { !it.isCompleted }`)
- Modify: `android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt:136-186` (handle new `QuestAction.OpenLesson(lessonId)` + `QuestAction.OpenModule(subject, moduleKey)` — or fold into existing `QuestAction.Continue` with payload)

**Step 1: Write failing test**

```kotlin
@Test fun `quest CTA picks next uncompleted quest lesson`() = runTest(dispatcher) {
  val vm = buildViewModel(completedLessons = listOf("mathematics-g3-m01-d01"))
  advanceUntilIdle()
  val quest = content(vm).quest
  assertEquals("english-g3-q1-w01-d02", quest.nextLessonId) // second pick
}
```

**Step 2:** `./gradlew :feature-child-home:test -q` FAIL — `nextLessonId` missing.

**Step 3: Implementation**

```kotlin
// PlayroomHomeUiState.kt
data class QuestUi(
  // ... existing
  val targets: List<QuestTargetUi> = emptyList(),
  val nextLessonId: String? = null, // first uncompleted in assigned order
)
enum class QuestAction { Continue, ChooseSubject, ViewReward, OpenLesson, OpenModule }
// or keep 3 values and overload Continue's payload — prefer explicit new value.

// PlayroomHomeViewModel.kt — compute nextLessonId
val nextLessonId = targets.firstOrNull { !it.isCompleted }?.lessonId
  ?: targets.firstOrNull()?.lessonId // all done → replay first
val questUi = if (dailyQuest.isComplete) { /* ViewReward unchanged but keep targets */ }
  else QuestUi(..., targets=targets, nextLessonId=nextLessonId,
    buttonLabel = if (nextLessonId==null) "Choose a subject" else if (completedCount==0) "Start quest" else "Continue quest",
    buttonAction = if (nextLessonId==null) QuestAction.ChooseSubject else QuestAction.OpenLesson
  )

// MaxinesNavGraph.kt — route
QuestAction.OpenLesson -> {
  val lid = (homeState as? PlayroomHomeUiState.Content)?.quest?.nextLessonId
  if (lid != null) navController.navigate(Routes.lessonPlayer(childId, lid))
}
```

Also make each target row a `clickable(role=Button)` that navigates to its `lesson_player/{lessonId}` so a child can pick which of the three to play — this is the "one obvious action + two visible alternates" pattern, still quest-led.

**Step 4:** `./gradlew :feature-child-home:test :app:connectedDebugAndroidTest -q` PASS. Manual check: Playroom → Start quest → should land in lesson_player for the uncompleted quest lesson (not subject_modules).

**Step 5: Commit**

```bash
git add android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt \
        android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModel.kt \
        android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt
git commit -m "feat(quest): route CTA and rows to the quest lessons"
```

---

### Task A6: Confirm quest completion is driven by `LessonCompletionRepository.complete()` (no new table)

**Objective:** No duplicate completion path. The existing `observeDistinctLessonIds → ensureToday` flip is already correct; this task just pins it with a regression and fixes the one risky path (if a lesson id never appears in `DailyQuestProgress` because of legacy ids, ensure it still flips).

**Files:**
- Modify: `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonCompletionRepository.kt` — no change expected; if the lesson id is not in `DAILY_QUEST_SUBJECTS`, it still lands in `dailyQuestCompletions` because `ensureToday`'s catch-all `insertIgnoring` covers `assignedQuestIds ∩ completedLessonIds` regardless of subject list. Add a test if missing.
- Test: `android/feature-child-home/src/androidTest/java/com/maxinesworld/featurechildhome/DailyQuestManagerTest.kt`

**Step 1: Write failing test**

```kotlin
@Test fun questCountsLessonCompletedViaRepository() = runBlocking {
  val mgr = manager(ctx); val day="2026-08-08"
  val set = mgr.ensureToday("child-1", day, emptyList(), listOf("math-1","eng-1","sci-1"))
  // simulate a lesson completion that matches one of the assigned ids
  lessonCompletionDao.insertIgnoring(LessonCompletionEntity("id","child-1", set.assignedQuestIds[0], "a", 0.9))
  val after = mgr.ensureToday("child-1", day, null, listOf("math-1","eng-1","sci-1"))
  assertEquals(1, after.completedCount)
}
```

**Step 2/3:** Run; already passes given current `DailyQuestManager.ensureToday:64-75`. No prod change.

**Step 4/5:** Commit only if a new test was added.

---

## Phase B — Remaining tickets (small, independent — do after Phase A is green)

### Task B1: Sort — fix instruction + bucket copy so the card is honest

**Objective:** Remove the `"… true? Fits?"` phrasing and keep bucket labels aligned with the instruction's `true/false` → bucket mapping.

**Files:**
- Modify: `android/app/src/main/assets/content-pack/month-01/lessons/mathematics-g3-m01-d01.json:73-99` (and any sibling where `fits/doesNotFit` phrasing is used — audit via `tools/content_quality_audit.py` search)
- Modify: `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonPlayerViewModel.kt:317-373, 450-457` (`toActivityStep` sort branch + `sortInstructionWithCategories`)
- Fix grammar: `"10,000 is ten thousands"` → `"10,000 is ten thousand"` in `fits` (both `content.fits` and any other json occurrences surfaced by the earlier grep).
- Test: `android/feature-lesson-player/src/test/java/com/maxinesworld/featurelessonplayer/ActivityStepConversionTest.kt`

**Step 1: Failing test**

```kotlin
@Test fun `sort instruction keeps bucket labels consistent`() {
  val step = toActivityStep(sortActivity(
    instruction="Sort each card: is the statement about 4,352 or 2,406 true?",
    categories=listOf("True","Not true"), fits=listOf("4,000+300+50+2"), doesNotFit=listOf("4,352 means 4 hundreds")
  ), "en")
  assertTrue(step.question.contains("True") && step.question.contains("Not true"))
  assertEquals(listOf("True","Not true"), step.sortCategories)
}
```

**Step 2:** FAIL until `sortInstructionWithCategories` mapping is corrected to replace the `true` token in the instruction with the bucket label (already does `replace("true", categories[0]) / replace("false", categories[1])` — so just ensure the instruction's trailing `true?` is rewritten to `"<bucket>?"` and not duplicated).

**Step 3:** Content fix: change the authored `instruction` to `"Sort each card into True or Not true."` or keep it and let `sortInstructionWithCategories` render as `"… is the statement about 4,352 or 2,406 True or Not true?"` — pick the one that reads best with the card mock. Prefer the latter: the method change is safer than editing the question stem's meaning.

**Step 4:** `./gradlew :feature-lesson-player:test :core-content:test -q` PASS; `python3 tools/content_quality_audit.py --check` clean.

**Step 5:** Commit lesson JSON + ViewModel + test.

---

### Task B2: Sort — Submit affordance: hidden submit + silent no-op guard

**Objective:** Submit is never "invisible then silently disabled" on small heights.

**Files:**
- Modify: `android/engine-activity/src/main/java/com/maxinesworld/engineactivity/renderers/SortAndClassifyRenderer.kt:54-186`
- Test: `android/app/src/androidTest/java/com/maxinesworld/app/SortAndClassifyRendererTest.kt`, `LessonRendererScrollCompositionTest.kt`

**Step 1: Failing test**

```kotlin
@Test fun sort_showsStickySubmitAndGuidesWhenNotAllPlaced() { /* assert scroll-to-submit or sticky CTA exists */ }
@Test fun sort_tappingDisabledSubmitShowsGuidance() { /* no crash, shows "Place all N cards first" nudged */ }
```

**Step 2:** FAIL (no sticky CTA).

**Step 3:** Make the submit bar sticky (pinned to bottom of the scrollable column via `BoxWithConstraints` / `stickyHeader`-like scaffold) and add a Toast/semantics announcement when `placedCount < items.size` and submit is tapped: `"Place ${remaining} more to check"`. Keep the existing `progressCopy` and `progressDescription` semantics; add `semantics { role=Role.Button }` to the CTA.

**Step 4:** `./gradlew connectedDebugAndroidTest -q` PASS at 1.0 and 2.0 font scales.

**Step 5:** Commit.

---

### Task B3: MCQ — silent no-op on empty Submit + retry affordance

**Objective:** Tapping Submit with no selection is never silent.

**Files:**
- Modify: `android/engine-activity/src/main/java/com/maxinesworld/engineactivity/renderers/MultipleChoiceRenderer.kt:186-224`
- Test: `android/engine-activity/src/test/java/com/maxinesworld/engineactivity/renderers/MultipleChoiceRendererTest.kt`

**Step 1: Failing test**

```kotlin
@Test fun `submit with no selection nudges the learner`() { /* tapping Submit when selectedIndex==-1 shows guidance */ }
```

**Step 2:** FAIL.

**Step 3:** When `selectedIndex==-1` and Submit is tapped, set a transient `showNoSelectionHint=true` and render a `Text("Pick one answer to check", color=ReviewText)` under the options for 2s (or until a selection is made). Do not call `onResult`. On retry, keep `hintsUsed` semantics as-is.

**Step 4:** `:engine-activity:test` PASS.

**Step 5:** Commit.

---

### Task B4: Filler assessment explanations + copy sweep

**Objective:** Remove generic retry strings that read as filler.

**Files:**
- Modify: `android/app/src/main/assets/content-pack/month-01/lessons/**/*.json` — sweep for retry/correct strings equal to `"Nice work. Continue to the next step."` etc. when the activity already has a richer `question` context; replace with the specific rule (e.g. `"Check each place: thousands → hundreds → tens → ones."`).
- Gate: `python3 tools/content_quality_audit.py --check` and `tools/content_pack_validation.py --strict`

**Step 1:** Run audit, capture the filler list (expect ≤ 5 files here; the sweep otherwise is non-blocking).

**Step 2:** Edit only filler lines, keep educator meaning.

**Step 3:** Re-run gates PASS.

**Step 4:** Commit as `content(ux): replace filler retry copy with specific guidance`.

---

### Task B5: Auth affordance — fingerprint label + name-field helper (optional if not desired)

**Objective:** The fingerprint icon reads as a button to a non-technical caregiver.

**Files:**
- Modify: `android/feature-auth/src/main/java/com/maxinesworld/featureauth/AuthScreen.kt:146`
- Modify: `android/feature-auth/src/main/res/values/strings.xml` (add `auth_fingerprint_label = "Use fingerprint (optional)"`)

**Step 1:** Add `contentDescription = stringResource(R.string.auth_fingerprint_label)` and a `Text` label under the icon; add `"Parent or guardian name (optional) — Maxine can call you this in the app"` helper under the name field.

**Step 2:** `./gradlew :feature-auth:connectedDebugAndroidTest -q` PASS.

**Step 3:** Commit as `fix(auth): label the biometric affordance`.

---

## Verification (must pass before tagging)

```bash
./gradlew check --stacktrace
./gradlew assembleRelease --stacktrace
python3 tools/content_quality_audit.py --check
python3 tools/dedupe_lesson_titles.py --check
python3 tools/content_pack_validation.py --strict
python3 tools/test_content_review.py
# on emulator:
./gradlew connectedDebugAndroidTest
# fresh-install smoke: PIN setup → child creation → Playroom shows 3 quest rows →
# Start quest → lesson_player for quest lesson → complete → Playroom paw + row checkmark advances → Field Guide reachable.
```

## Risks and mitigations

- Quest targets are daily and per-child; rotation uses `ModuleCatalog` which is IO — keep resolution off the main thread (`withContext(Dispatchers.IO)` in resolver or inside `buildContent`'s existing IO path).
- Titles are asset-derived; never trust them to be non-empty — fallback to `friendlyLessonTitleOf(lessonId)` preserves legibility even if the pack is pruned for one subject.
- No schema migration needed. If `assignedQuestIds` ever stores legacy `araling-panlipunan-*` ids, resolver normalizes to `makabansa` for display while the DAO row remains the source of truth.
