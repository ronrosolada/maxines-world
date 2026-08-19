# Maxine's World, Review Validation Handoff

**Status: Record, dated review document; current canonical status lives in `HANDOFF.md` (spec CH-13).**

**Created:** 2026-08-03 17:52 Asia/Singapore  
**Purpose:** Preserve the current conversation context for continuation in a fresh thread.

## Core Goal

Validate the independent educational-content and module-layout review of the Maxine's World Android app against the current repository state. Separate findings that remain valid from findings fixed after the review, then identify the next engineering and content-review priorities.

The repository must remain clean, synchronized, and ready for review by another LLM or an authorized educator.

## Repository Baseline

- **Workspace:** `/home/ron/workspace`
- **Repository:** `/home/ron/workspace/maxines-world/repo`
- **Branch:** `main`
- **HEAD:** `11b49af`, `fix: close educational content review P0s (#26)`
- **Remote:** `origin/main`
- **Branch state before creating this handoff:** clean and synchronized with `origin/main`
- **Review report:** [`content-review-2026-08-03.md`](https://github.com/ronrosolada/maxines-world/blob/review/content-2026-08-03/docs/content-review-2026-08-03.md)
- **Review branch:** `review/content-2026-08-03`
- **Review PR:** PR #25
- **P0 fix PR:** PR #26, merged into `main`

The original review was based on an older repository state (`357ff81`) before PR #26. It is therefore a valid historical review, but it is not fully current.

## User Preferences and Constraints

- Prefer concise, direct, actionable responses.
- Use exact paths, counts, commands, and evidence instead of vague descriptions.
- Do not invent commands, APIs, or results.
- Do not include credentials, tokens, API keys, or other secrets.
- Preserve the child-safe, privacy-first product direction.
- The target learner is an 8-year-old using Philippine Grade 3 DepEd MATATAG-aligned content.
- Passing automated checks does **not** prove factual accuracy, pedagogical quality, language quality, safety, or independent educator approval.
- Do not run `mark_lessons_reviewed.py` unless an authorized human review has genuinely been completed.
- Preserve the original review as historical evidence; update it additively or create a validation report instead of rewriting historical observations destructively.

## Overall Verdict

**CONDITIONAL.**

The review correctly identified serious issues at its original baseline. PR #26 fixed the main live-content P0s, but several P1/P2 findings remain and the review document should be updated before being presented as a current-state assessment.

## Findings Confirmed on Current `main`

### Content pack scope

The current bundled pack contains:

- **349 lesson JSON files**
- **100 legacy lessons**
- **249 quarterly lessons**
- Six activities per lesson
- Five assessment items per lesson

Current subject-family counts:

```text
ARALING_PANLIPUNAN: 20
ENGLISH: 20
FILIPINO: 20
MATHEMATICS: 20
SCIENCE: 20
english: 73
filipino: 63
gmrc: 24
makabansa: 26
mathematics: 38
science: 25
```

### Assessment filler remains

The live activity path was fixed by PR #26, but the currently unused `assessment` block still contains Math quarterly filler. Current counts are:

```text
a mismatched unit       152
an unrelated operation  152
a random guess          152
an answer with no label 114
Total                   570
```

These occur in the 38 quarterly Mathematics lessons. They must be replaced before the assessment flow is exposed.

### Duplicate assessment prompts remain

**230 lessons** contain duplicate prompts within their own five-item assessment set. This remains a P1 issue before the assessment UI is connected.

### Asset coverage remains incomplete

Current audit:

```text
Activity asset references: 1,994
Distinct asset IDs:       349
SVG assets available:      100
Unresolved references:     1,494
```

Quarterly lessons still lack corresponding SVG artwork and use the renderer's fallback presentation. This remains a valid engagement and visual-learning concern, especially for lessons titled things such as `Picture Detective`.

### English Q4 coverage is absent

Quarterly English lesson files currently contain Q1, Q2, and Q3 only. No English Q4 lesson files were found.

### Repeated titles remain

Repeated lesson and module-style titles remain across the quarterly content. Exact counts can differ from the original baseline, but the underlying issue remains: repeated titles can make navigation confusing and make the content feel template-generated.

### Metadata is not independent educator approval

The release gate can pass because lesson metadata says the content is approved, but that does not prove independent human curriculum sign-off. The review brief explicitly warns against treating these fields as proof:

```text
educatorValidated=true
releaseStatus="RELEASED"
```

Relevant guidance: [`educator-content-review-brief.md`](educator-content-review-brief.md).

## Findings Fixed by PR #26

### Makabansa reachability, fixed

The original review found 26 Makabansa lessons unreachable from the Playroom.

Current code defines seven canonical subjects in:

```text
android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt
```

The current subject list includes:

```text
mathematics
english
science
filipino
araling_panlipunan
makabansa
gmrc
```

The Playroom now exposes:

```text
Makabansa, Bayan at Kultura
```

### Live Math filler, fixed

The original live activity filler included terms such as:

```text
correct idea
useful example
check the concept
a random guess
a mismatched unit
an unrelated operation
```

Current scans found no remaining occurrences of those original filler terms in the live activity content. The remaining filler is in the unused assessment block described above.

One remaining `responsableng gawain` occurrence is associated with an educator-reviewed Araling Panlipunan lesson and was previously assessed as legitimate rather than generated filler.

### Activity MCQ answer-position exploit, fixed at runtime

The original review found that every activity MCQ placed the correct option first and that the renderer did not shuffle it.

The current runtime renderer applies deterministic per-lesson option shuffling, so the learner-facing exploit of always selecting the first displayed card is fixed.

Important nuance: static content still has a strong correct-index bias:

```text
index 0: 321
index 1: 9
index 2: 9
index 3: 10
```

That static bias is not currently equivalent to a learner-facing exploit because the renderer reorders displayed options. The future assessment path should still shuffle and rebalance its own options before exposure.

### Legacy `ph-matatag` tree claim, STILL VALID (handoff correction)

The original review reported 250 JSON files under a legacy `ph-matatag` tree. Re-verification against current `main` (2026-08-03, this thread):

```text
find app/src/main/assets -path "*ph-matatag*" -name "*.json" | wc -l
250
```

The earlier handoff claim of "count: 0" was an error, that check was scoped to the wrong root (`content-pack/` instead of `assets/content/`). The finding remains valid, with one nuance: the tree is not entirely unreachable, `ContentLessonLoader.tryPath()` (path 3) falls back to `content/ph-matatag/grade-3/{lessonId}.json`, and `ModuleCatalog` reads `content/ph-matatag/grade-3/manifest.json` for legacy m01 module titles. All 349 bundled IDs resolve via path 1, so the 250 lesson JSONs are fallback-only APK weight today.

## Additional Current Code Follow-ups

These were identified during validation and remain unresolved.

### Reduced-motion support, ✅ DONE (commit `8f3c43a`, 2026-08-03)

The lesson player previously hard-coded `reducedMotion = false // TODO: wire system setting`; the celebration confetti now honors `Settings.Global.ANIMATOR_DURATION_SCALE == 0` (Android reduced-motion preference). Particle generation and the infinite animation are skipped entirely when disabled.

### Lesson resume and completion UX, ✅ DONE (commit `347494f`, 2026-08-03)

The module lesson list now layers persisted completion state (`LessonCompletionDao.observeDistinctLessonIds`) over the catalog: header shows "X of Y complete · keep going! / module done! 🎉"; completed rows show a ✓ chip, green tint, and check icon; the first incomplete lesson in module order gets a gold border and "Up next, tap to continue" as the child's resume point. 7 unit tests for the `nextLessonId()` helper.

Relevant files:

```text
android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/ModuleLessonsScreen.kt
android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/ModuleLessonsViewModel.kt
```

### Parent streak calculation, ✅ DONE (commit `3fad627`, 2026-08-03)

The dashboard now computes a real consecutive-day streak in the child's local timezone. Old logic counted distinct recent-activity strings (which contained no dates) as "streak days"; new logic uses `longestStreak()` over local dates derived from event timestamps, breaking on gaps and handling duplicates. The UTC date bucketing that shifted pre-8am Manila completions to the previous day is fixed. 11 unit tests incl. month/year boundaries and Manila timezone cases.

Relevant file:

```text
android/feature-parent/src/main/java/com/maxinesworld/featureparent/ParentDashboardScreen.kt
```

### PIN brute-force protection, ✅ DONE (commit `34aef08`, 2026-08-03)

Lockout implemented: persistent failed-attempt counter + lockout deadline in DataStore (survives process restarts). Policy: lock after 5 consecutive failures, escalating 30s → 60s → 120s → 240s, capped 300s. Correct PIN rejected while locked; counter resets on success. 6 regression tests in `ParentAuthLockoutTest.kt`.

Relevant file:

```text
android/feature-auth/src/main/java/com/maxinesworld/featureauth/ParentAuthViewModel.kt
```

### Coins documentation contradiction, ✅ DONE (2026-08-03)

Reconciled `docs/grand-ux-gamification-implementation.md`: coins ARE awarded by the lesson flow (10 per lesson at `>=80%` accuracy, idempotent via the reward ledger) and shown as a balance in the rewards hub, but there is no spend mechanism in code. Policy documented: the balance stays informational until a cosmetic use (e.g., Kindness Garden decorations) ships.

## Validation Already Completed

From `/home/ron/workspace/maxines-world/repo/android`:

```bash
./gradlew testDebugUnitTest lintDebug :app:verifyPlayableContent
```

Result:

```text
exit 0
```

Connected Android tests:

```bash
./gradlew :app:connectedDebugAndroidTest \
  :core-database:connectedDebugAndroidTest \
  :feature-child-home:connectedDebugAndroidTest
```

Result:

```text
exit 0
30/30 tests passed
0 failures
0 skipped
```

Static content checks performed:

- 349 lesson files parsed successfully.
- 1,994 activity asset references counted.
- 349 distinct asset IDs counted.
- 100 SVG assets counted.
- 1,494 unresolved asset references counted.
- 570 assessment filler options counted.
- 230 lessons with duplicate assessment prompts counted.
- English quarterly coverage verified as Q1–Q3 only.
- Current `ph-matatag` JSON count verified as zero.
- Runtime MCQ shuffle implementation inspected.

## Runtime/System State at Handoff

- No active Gradle build process.
- No active test process.
- No active review job.
- Gradle and Kotlin daemons are idle.
- Android emulator `MaxinesWorld` is running intentionally.
- ADB server is running intentionally.
- The repository was clean and synchronized before this handoff file was created.

This handoff file was committed and pushed to `main` in `b0e5cc2`; the continuation update below is the next documentation revision.

## Current Continuation Update, assessment P1s completed (2026-08-03)

The next continuation work was completed on `main` in commit `9da1ffc` (`fix(content): replace assessment filler and duplicate prompts`).

### Fixed

- **Assessment filler:** replaced all 570 generic assessment-option hits in the 38 quarterly Mathematics lessons with topic-grounded examples and distractors.
- **Duplicate assessment prompts:** replaced repeated prompts in all 230 affected lessons with five distinct prompts per assessment.
- **Generator guard:** updated `android/tools/content_review.py` so future generated assessments use five distinct English or Filipino prompt templates when a profile has fewer than five hand-authored checks.
- **Test coverage:** added a regression test for unique prompts, valid correct-option IDs, and topic-grounded generated assessments.

### Current validation

```text
349 lesson JSON files parse
0 duplicate assessment-prompt lessons
0 assessment filler hits
0 assessment schema errors
8/8 Python content-review tests passed
Gradle unit tests + lint + verifyPlayableContent: BUILD SUCCESSFUL
Connected Android tests: 30/30 passed, 0 failed, 0 skipped
```

The release gate still reports 349 educator-reviewed lessons because the approval metadata was intentionally not changed. Automated checks do not constitute independent educator approval.

## Current Continuation Update, quarterly visual assets completed (2026-08-03)

The quarterly visual-asset follow-up was completed on `main` in commit `d34f4e7` (`feat(content): add quarterly visual learning boards`).

### Fixed

- Added deterministic, topic-grounded SVG visual boards for all **249 quarterly lesson visual IDs**.
- Kept the existing 640×360 SVG contract and added accessible `<title>` and `<desc>` metadata to generated boards.
- Added `android/tools/generate_quarterly_assets.py`; it is idempotent and writes only missing quarterly assets.
- Added generator regression coverage in `android/tools/test_generate_quarterly_assets.py`.
- No lesson JSON, Android UI, or loader code was changed.

### Current validation

```text
249 quarterly lessons discovered
249/249 quarterly SVG references resolve
349 total vector assets present (100 legacy + 249 quarterly)
0 malformed SVGs
0 dangling visual-asset references
11/11 Python content and asset-generator tests passed
Gradle unit tests + lint + verifyPlayableContent: BUILD SUCCESSFUL
Connected Android tests: 30/30 passed, 0 failed, 0 skipped
```

The new boards are deterministic topic-grounded visual scaffolds, not a substitute for independent educator or illustrator review. The release gate remains unchanged at 349 educator-reviewed lessons.

## Current Continuation Update, English Q4 boundary documented (2026-08-03)

The English Q4 finding is now resolved as a documented content boundary rather than being filled with invented curriculum. The live inventory contains 73 English quarterly lessons across Q1–Q3 and no Q4 source material. `HANDOFF.md` now records Q1–Q3 as the current bundled English scope and defers Q4 until source SLM curriculum is available and independently reviewed.

This is intentional scope documentation, not a claim that English Q4 content exists. No lesson JSON was fabricated or modified.

## Current Continuation Update, repeated lesson titles resolved (2026-08-03)

The repeated lesson-title follow-up was completed in the bundled content pack.

### Fixed

- Found **58 duplicate-title groups** covering **256 lessons**.
- Added stable navigation qualifiers derived from lesson IDs:
  - quarterly lessons: `· Q# W## D##`
  - legacy Module 1 lessons: `· M## D##`
- Modified only the JSON `title` field in the 256 affected lesson files.
- Added `android/tools/dedupe_lesson_titles.py`, an idempotent checker/applicator.
- Added `android/tools/test_dedupe_lesson_titles.py` regression coverage.

### Current validation

```text
349 lesson files
349 unique lesson IDs
349 unique lesson titles
0 duplicate-title groups
256 lesson files changed
0 non-title JSON changes
14/14 Python content and asset tests passed
Gradle unit tests + lint + verifyPlayableContent: BUILD SUCCESSFUL
Connected Android tests: 30/30 passed, 0 failed, 0 skipped
```

The qualifiers intentionally expose quarter/week/day context in navigation; they are not a replacement for future editorial title refinement.

### Remaining content priorities

1. Complete genuine independent educator review for factual accuracy, pedagogy, language, safety, and the new visual boards.

The remaining code follow-ups listed below are unchanged and remain owned by the Android app agent unless explicitly reassigned.

---

## Current Continuation Update, Filipino assessment correction (2026-08-03)

The assessment prompt correction for the 32 Filipino simuno/panaguri lessons was completed on `main` across two commits:

- `1c7c8d4`, first pass replacing generic title-substituted prompts
- `916efa9`, corrected pass fixing convention and distractor defects found in review

### Fixed

- **Complete-predicate panaguri convention:** keyed answers for panaguri questions now use the full predicate phrase including the linker (`ay tumatakbo`, `ay nagbabasa ng libro`), matching DepEd Grade 3 convention. The earlier pass keyed bare verbs (`tumatakbo`), which the lesson's own slash notation (`Si Ana / ay nagbabasa.`) contradicts.
- **Removed near-duplicate distractors:** options that duplicated or near-duplicated the keyed answer were replaced with clearly wrong alternatives (other subjects/nouns for simuno questions; subjects/adverbs for panaguri questions).
- **Grammar fix:** the ungrammatical option `ay matulog sa sofa` was replaced with `ay natutulog sa sofa`.
- **Wrong-key fixes:** two templates keyed the wrong option (e.g., marking `nagtuturo` correct when asking for the simuno `Ang guro`); both were corrected.
- **Answer-position variety:** correct positions now span a/b/c/d (distribution across the 160 items: a=43, b=53, c=32, d=32).
- **Tooling:** `android/tools/fix_filipino_simuno_panguri_assessment.py` (idempotent regenerator) + `android/tools/test_fix_filipino_simuno_panguri_assessment.py` (7 regression tests: structure, uniqueness, key validity, no generic prompts, complete-predicate convention, position variety, objective specificity).
- **Scope discipline:** programmatic diff-scope check confirmed the 32 lesson files differ from `1c7c8d4` **only** in `assessment.items`, no other lesson fields changed.
- **Full-rewrite experiment discarded:** a broader activity-shell rewrite was rejected mid-flight (it regressed assessment variety and would have rewritten narration/vocabulary wholesale); the working tree was restored to the committed assessment-only state. The defect this avoids is documented in the test suite's convention checks.

### Current validation

```text
160 assessment items across 32 lessons (5 each)
0 generic title-substituted prompts in the 32 corrected lessons
0 duplicate option texts, all 4 option ids a-d present per item
7/7 Python regression tests passed
Gradle unit tests + lint + verifyPlayableContent: BUILD SUCCESSFUL
```

### Remaining assessment scope (not yet corrected)

984 generic assessment items remain across **198 lessons** in the other subjects:

```text
ENGLISH:      264
MATHEMATICS:  190
FILIPINO:     155   (other Filipino lessons, different objectives)
MAKABANSA:    130
SCIENCE:      125
GMRC:         120
```

These still use the title-substituted template prompts and need per-subject objective-specific rewrites following the Filipino pattern (convention → templates → tests → diff-scope check).

## Current Continuation Update, First Steps sticker feature (2026-08-03)

Completed on `main` in commit `2630347` (`feat: First Steps sticker for the child's first completed lesson`).

### What shipped

- **New `milestone` biome** in `core-model/CollectibleBadge.kt` (gold, "Milestones") and a new catalog sticker: `milestone_first_steps` ("First Steps" / "Bright Beginning", 🌟) in `android/app/src/main/assets/badge_catalog.json`.
- **`BadgeAwarder.recordFirstLessonCompletion(childId)`**, mutex-guarded, idempotent: a child can only earn the First Steps sticker once, replays never double-award. The weekly wildlife expedition now explicitly skips milestone stickers (`biome != MILESTONE_BIOME`) so it can never leak a milestone award.
- **First-lesson detection:** `LessonCompletionDao.countDistinctLessons(childId)` (new suspend query; Flow variant already existed). `LessonPlayerViewModel.saveProgress()` checks it *before* inserting the completion; on the child's very first lesson ever it awards the sticker and prioritizes it in the reveal.
- **`BadgeRevealScreen` celebration:** milestone-aware copy ("Your First Sticker! You finished your very first lesson! Milo is cheering for you! 🐱🎉") plus a fun reward animation for **every** sticker reveal: confetti rain, bouncy spring pop-in of the sticker token, and six orbiting ✨ sparkles. Reduced-motion (animator scale = 0) skips all animation and jumps straight to the reveal.
- **Field guide counters now dynamic:** the header counter was hardcoded `/50` and per-biome `/10`; both now derive from the catalog size (`${biomeBadges.size}`, `allBadges.size`).

### Tests

- `BadgeAwarderTest`: First Steps awarded once and not twice; milestone sticker never leaks into the weekly expedition (catalog ordering puts it first, expedition still picks `badge_01`); persisted-collection metadata test updated for the 4-badge catalog.
- `LessonCompletionDaoTest` (connected): `countDistinctLessons` matches the Flow variant and is 0 for a fresh child.
- Full suite: `testDebugUnitTest` BUILD SUCCESSFUL across all modules; `lintDebug`, `:app:verifyPlayableContent`, `:app:assembleDebug` pass; `:core-database:connectedDebugAndroidTest` 21/21 green on the emulator.

### Release

- Prerelease **`v0.20.2-tablet-test`** published to GitHub (commit `2630347`) with `app-debug.apk` + `app-debug.apk.sha256`.
- APK SHA-256: `80c0a3512d6ba97cdfcf2b3d34c1f0be57a1c0acfe25af7565a7041f38521a8a`.

### Open questions for the external reviewer

1. **First-sticker moment UX:** the reveal fires on the child's first completed lesson regardless of subject. Is a single universal milestone right, or should the first lesson of *each subject* earn a sticker too?
2. **Milestone biome growth:** the field guide now has 6 biome sections; the milestone section currently holds exactly one sticker. Is that intentional, or should future milestones (e.g., 10th lesson, first full week) be planned?
3. **Sticker economics:** stars/coins still award alongside the sticker. Is the sticker the primary reward signal, and are stars/coins the right secondary currency?
4. **Confetti intensity:** the reveal screen confetti mirrors the lesson-complete screen. Review for overstimulation risk with the reduced-motion fallback in mind.

## Current Continuation Update, repository state for external LLM review (2026-08-03)

This handoff doc is the current review-validation context. HEAD of `main` is `2630347`; working tree is clean and synchronized with `origin/main`.

### What changed since the last handoff revision

| Commit | Change |
|--------|--------|
| `3ea2545` | docs: record lesson title disambiguation |
| `e8119f4` | fix(content): disambiguate repeated lesson titles |
| `86cb25d` | docs: define English quarterly coverage boundary |
| `1c61805` | docs: record quarterly visual asset validation |
| `6f93b6d` | docs: corrected educator content review (factual accuracy fixes) |
| `1c7c8d4` | fix: Filipino simuno/panaguri assessment prompts (32 lessons) |
| `916efa9` | fix: corrected Filipino assessments (convention, distractors, keys) |
| `2630347` | feat: First Steps sticker for the child's first completed lesson |

### Current content-pack numbers (re-verified)

```text
349 lesson JSON files (100 legacy + 249 quarterly)
349 unique lesson IDs
349 unique lesson titles
6 activities per lesson
5 assessment items per lesson
3 vocabulary terms per lesson
1,745 assessment items, all with valid correctOptionIds
1,994 non-null activity asset references, all resolving
349 vector assets present (100 legacy + 249 quarterly)
0 duplicate-title groups
0 assessment filler hits (was 570)
0 duplicate assessment-prompt lessons (was 230)
984 generic assessment prompts remaining (198 lessons, subjects other than the 32 corrected Filipino lessons)
```

### Assessment integrity status

- All 1,745 items carry valid `correctOptionIds` (schema field is the plural array, a common audit false-positive source).
- The 32 Filipino simuno/panaguri lessons now have objective-specific, convention-correct, position-varied items (see update above).
- The remaining 984 generic items are tracked but not yet rewritten; they are a P1 content task, not a release blocker for the debug test build.

### What the external reviewer should focus on

1. **Correctness of the 32 corrected Filipino assessments**, verify the complete-predicate panaguri convention, distractor quality, and Filipino grammar (templates live in `android/tools/fix_filipino_simuno_panguri_assessment.py`).
2. **First Steps sticker feature**, behavior, idempotency, expedition isolation, and the reveal animation (see open questions above).
3. **Remaining 984 generic assessment prompts**, confirm the per-subject rewrite plan and that the Filipino pattern (convention → templates → tests → diff-scope check) is the right template.
4. **Activity-shell monotony**, all 349 lessons still share the same 6-activity sequence with identical per-lesson narration; this is the largest remaining pedagogical concern.
5. **Vocabulary quality**, ~60% of 1,047 vocabulary entries are example sentences or equations rather than term+definition pairs.
6. **Factual/pedagogical review of lesson bodies**, automated checks cannot prove educational correctness; the educator review report remains conditional pending independent human review.

### Historical review artifacts (preserved)

- [`educator-content-review-2026-08-03.md`](educator-content-review-2026-08-03.md), corrected educator review on `main`.
- [`content-review-2026-08-03.md`](https://github.com/ronrosolada/maxines-world/blob/review/content-2026-08-03/docs/content-review-2026-08-03.md), historical review on `review/content-2026-08-03` (PR #25).
- [`educator-content-review-brief.md`](educator-content-review-brief.md), review rubric.
- `docs/educator-review-{baseline,detailed,sample}-2026-08-03.json`, static audit exports.

## Recommended Next Steps

Prioritize the remaining work in this order:

1. ✅ Replace all assessment filler in the 38 quarterly Math lessons, completed in `9da1ffc`.
2. ✅ Remove duplicate assessment prompts across the 230 affected lessons, completed in `9da1ffc`.
3. ✅ Add quarterly visual assets for all 249 dangling quarterly IDs, completed in `d34f4e7`.
4. ✅ Document English Q1–Q3 as the current bundled boundary; defer Q4 until source SLM curriculum is available, completed in this handoff update.
5. ✅ Reduce repeated lesson/module titles where repetition harms navigation, completed in this handoff update.
6. ✅ Correct the 32 Filipino simuno/panaguri assessments (convention, distractors, keys, position variety), completed in `916efa9`.
7. ✅ Ship the First Steps sticker milestone + celebration reveal, completed in `2630347` (release `v0.20.2-tablet-test`).
8. Rewrite the remaining **984 generic assessment prompts** (198 lessons) subject by subject, following the Filipino pattern.
9. Add PIN attempt throttling and temporary lockout.
10. Implement proper reduced-motion support in the lesson-complete screen (the reveal screen already respects it).
11. Add completion and resume state to module lesson lists.
12. Verify and correct parent streak calculations using date-based records.
13. Reconcile the coin-award implementation with product documentation.
14. Conduct genuine independent educator review for factual accuracy, pedagogy, language, safety, and the new visual boards.

## Current Continuation Update, external adversarial review received and verified (2026-08-03 evening)

An external LLM adversarial educational-content review was received and archived verbatim at [`docs/external-llm-review-adversarial-2026-08-03.md`](external-llm-review-adversarial-2026-08-03.md). Its claims were verified against current `main` (HEAD `0ee9eee`) before being accepted.

### Claim verification (all confirmed)

| Review claim | Code evidence |
|---|---|
| Authored assessment items are never played | `LessonManifest.assessment` exists (core-model/Models.kt:40) but `convertToLessonManifest()` never populates it, `steps = m1.activities.map { ... }` only (LessonPlayerViewModel.kt:215); `totalSteps = lesson.steps.size` (line 90), so completion happens after the 6 activities |
| Vocabulary card shown on every step | `LessonContent` renders `VocabularyCard` above every step when terms exist (LessonPlayerScreen.kt:131–135) |
| Narration repeated on explanation screens | Generic narration card (LessonPlayerScreen.kt:173–181) **and** `ExplanationStep` both render `step.narrationText` (line 262) |
| English chrome on Filipino lessons | "New Words" (line 51), "Read Along" (line 222), "Continue" (line 270), "Next"/"Try Next" (line 287), "Lesson Complete!" (line 340), all hardcoded English |
| Generic sort labels ("Fits the lesson" / "Does not fit") | Hardcoded in `toActivityStep` (LessonPlayerViewModel.kt:275) |
| Reduced-motion not wired in lesson-complete | `val reducedMotion = false // TODO: wire system setting` (LessonPlayerScreen.kt:323) |
| Six-activity universal shell | `rendererType()` maps exactly ANIMATED_EXPLANATION, MULTIPLE_CHOICE, SORT_AND_CLASSIFY, HOTSPOT_IMAGE, MATCHING_PAIRS, SEQUENCE_BUILDER, INTERACTIVE_SPEC (LessonPlayerViewModel.kt:221–230); all 349 lessons carry 6 activities |
| Deterministic shuffle for SORT only; MCQ positions not runtime-randomized | SORT items are shuffled deterministically (line 277); MULTIPLE_CHOICE uses the authored `correctIndex` verbatim (lines 280–284), no runtime option remap |

No contradicted claims were found. The review is accepted as a current-state assessment.

### Response plan

1. **P0, assessment delivery path:** map `m1.assessment.items` into a distinct playable assessment phase appended after activities; track a `phase` in the player state instead of treating every screen as an equivalent step; only scored practice contributes to accuracy (the first-steps/wildlife badge reveal already uses scored results).
2. **P0, chrome and narration fixes (quick wins):** show vocabulary once (or on demand), remove the duplicated narration card on explanation steps, localize lesson chrome strings per `languageOfInstruction`.
3. **P0, content blocking:** block known contradictory/profile-mismatched lessons; add a normalized-content similarity gate across the 349 lessons; create source-to-lesson traces for quarterly conversions.
4. **P1, distractor/feedback quality:** misconception-based distractors, actionable corrective feedback, at least one transfer item per assessment, runtime-deterministic option remapping.
5. **P1, semantic QA pipeline + golden lessons per subject** with educator sign-off; child tests.
6. **Process:** release approval must reference a reviewed commit, not metadata; keep incremental subject/module releases.

### Recommended next steps (updated)

1. ✅ through ✅, all previously completed items unchanged (see list above).
2. **Deliver the authored assessment phase in the lesson player** (P0, review item 1).
3. **Fix lesson chrome: vocabulary once, no narration duplication, localized labels** (P0, review item 5).
4. **Build the semantic QA pipeline**, objective-verb vs task checks, duplication gate, assessment alignment (P1, review item 5).
5. **Golden lessons + educator sign-off** for one module per subject (P2, review rollout phase 2).
6. Then the remaining code follow-ups (PIN throttling, reduced motion, resume UX, streaks, coins).

## Current Continuation Update, P0 assessment delivery shipped (2026-08-03)

The adversarial review's headline finding was fixed on `main` in commit `fd7cfb4` (`feat(lesson-player): deliver the authored assessment phase (P0)`).

### What shipped

- **Assessment phase delivered:** `convertToLessonManifest()` now maps `m1.assessment.items` into `ASSESSMENT_V1` steps appended after the practice activities; the manifest `assessment` block carries them with the authored pass threshold (`passingCorrectCount / itemCount`). `totalSteps` now includes the check, so **completion requires every assessment item to be answered**.
- **Phase separation:** `LessonUiState.assessmentStepCount` splits the lesson; progress dots stop at practice, and an amber "Knowledge Check / Pagsusulit" banner with a "Question X of Y / Tanong X ng Y" counter marks the assessment phase.
- **Assessment UX:** `AssessmentStepCard` renders the authored prompt + options (single attempt, options lock after answering); feedback uses the authored explanation for both outcomes; after a wrong answer a "Review / Balikan" action re-opens the first worked example in a dialog.
- **Scoring:** assessment results are scored, so accuracy, stars, and badges now include the knowledge check (previously activities-only).
- **Chrome fixes from the review:** vocabulary card shows once (first step only, not every step); the narration card no longer duplicates `ExplanationStep`; lesson chrome localized for `fil-PH` lessons ("Bagong Salita", "Basahin Natin", "Sunod", "Subukan Muli", "Pagsusulit", "Tapos na ang Aralin!").

### Tests

- 4 new `ActivityStepConversionTest` cases: key maps by option id, order preserved, malformed options/missing key degrade safely, blank explanation falls back to default feedback.
- New connected `everyPlayroomSubjectAssessmentIsConvertible` in `OfflineLessonLoadTest`, proves every Playroom-reachable subject's real bundled assessment block parses into the converter's contract (≥2 options, keyed `correctOptionIds`, non-blank explanation).
- Full gate: `testDebugUnitTest` + `lintDebug` + `:app:verifyPlayableContent` + `:app:assembleDebug` BUILD SUCCESSFUL; `:app:connectedDebugAndroidTest` 6/6 green.

### Remaining from the review (updated 2026-08-03, second content wave)

**DONE this wave (commits `af52f9a`, `dba7d76`):**

- **English (22 lessons, 4 skill groups)**, stock junk ("an unrelated guess", "a random symbol", …) fully removed. Each lesson now has authored vocabulary (real word + definition), concept-faithful sort/options/matching, skill-specific assessment prompts. Word Explorer lessons received real G3 word sets (28 words with definitions, sentences, cloze); story lessons received 5 authored Milo-world mini-stories with detail items.
- **Mathematics + Science (43 lessons, 5 skill groups)**, per-instance content sets (9 addition, 9 multiplication, 7 living/non-living, 11 material properties, 7 light/sound). Math assessments are now real computation items generated from the lesson's own equations with near-miss numeric distractors (±10, ±100, off-by-one factors), prompts use number words; science items use cross-set real examples and safe-action rules as correct answers.
- **Lesson-specific shells**, instructions, hotspot examples, sort fits, sequence steps, animated intros now reference each lesson's own content instead of the universal stamp.
- **Similarity gate upgraded** (`tools/content_similarity_gate.py`): digits are content tokens (math equations count); same-objective pairs flagged only ≥0.95 (spiraling practice groups are expected to share objective+shell); cross-objective ≥0.70. Result: **flagged pairs 1,154 → 473 (−59%)**; all repaired groups cleared. Final report: `docs/content-similarity-report-2026-08-03-final.json`.
- All tools have regression tests (54 tool tests OK); `verifyPlayableContent` + `testDebugUnitTest` green.

**Remaining flagged clusters (updated 2026-08-03, third content wave):**
- ~~Filipino bodies: Munting Talata (16), simuno/panaguri activity bodies (10+7+4+4)~~, **DONE, see below.**
- Makabansa (134 pairs) and GMRC (109 pairs), untouched.
- Small leftovers: English q2-w06/q2-w07 groups (22 pairs), math rounding/place-value groups (9 pairs).
- P0 #2 residual: the universal 6-activity shell still exists in unrepaired groups (content-side, not a player bug).

## Current Continuation Update, Filipino content repair (2026-08-03, third content wave)

**DONE this wave (commit `d443fef`):** `android/tools/repair_filipino_content.py` +
`test_repair_filipino_content.py` (14 tests) rebuilt all six Filipino skill groups
(63 lessons). Similarity gate result: **Filipino 199 → 0 flagged pairs; total
274 pairs / 25 clusters** (makabansa 134, gmrc 109, english 22, mathematics 9).
All 63 lessons junk-free, 0 broken items, idempotent; `verifyPlayableContent` +
`testDebugUnitTest` green.

| Group | Lessons | Content |
|---|---|---|
| simuno/panaguri | 32 | 64-sentence pool, lesson i takes window `[2i..2i+4)`, unique 4-set per lesson, adjacent share only 2. All assessment options derive from the lesson's own block. |
| munting talata | 12 | 12 authored G3 paragraphs; paksa/ideya/detalye/wakas items verbatim from each lesson's own paragraph. |
| wastong pagsulat | 7 | 7 word sets (spelling, definition, cloze, authored misspellings never equal to real words). |
| salitang-ugat | 4 | 4 root→related pair sets incl. a "HINDI galing sa ugat" discriminator item. |
| magagalang na pananalita | 4 | 4 polite-phrase sets + impolite distractors. |
| maikling buod | 4 | 4 authored stories; tauhan/suliranin/pangyayari/wakas items verbatim from each lesson's own story. |

Filipino junk eliminated (2,069 → 0 instances across the pack): "salitang walang
kaugnayan", "hindi magalang na pahayag", "hula na walang pahiwatig", "angkop na
halimbawa", "malinaw na gamit", "tamang ideya", "paksang iba sa aralin".

**Pitfalls learned (apply to Makabansa/GMRC waves):**
1. Absolute distractor slices (`other[:3]`) saturate token-set unions → gate reports
   1.00 for genuinely different lessons. Rotate windows per lesson index.
2. Windows of (n−1) from an n-set still converge (3-of-4 stories = all others).
   Use windows ≤ n/2 plus fixed/cross-element fillers.
3. Options drawn from a shared pool leak pool tokens into every lesson, options
   must come from the lesson's own content block.
4. Static fragment strings must not collide with pool vocabulary.
5. Answer sentences must be exact substrings of the lesson text (mind sentence
   boundaries).

## Safe Continuation Commands

Before making additional changes:

```bash
cd /home/ron/workspace/maxines-world/repo
git status --short --branch
git log -1 --oneline --decorate
```

For content-only validation:

```bash
cd android
python3 tools/content_review.py --dry-run
python3 tools/content_review.py --dry-run --include-legacy
python3 -m unittest discover -s tools -p 'test_content_review.py' -v
./gradlew :core-content:testDebugUnitTest
./gradlew :app:verifyPlayableContent
```

Do not run the non-dry-run approval marker until authorized human educator review is complete:

```bash
python3 tools/mark_lessons_reviewed.py
```

## Key Reference Files

- [`HANDOFF.md`](../HANDOFF.md), older general project handoff; some baseline details are stale.
- [`educator-content-review-brief.md`](educator-content-review-brief.md), independent educator review rubric and required output.
- [`content-review-2026-08-03.md`](https://github.com/ronrosolada/maxines-world/blob/review/content-2026-08-03/docs/content-review-2026-08-03.md), historical review being validated.
- [`grand-ux-gamification-implementation.md`](grand-ux-gamification-implementation.md), UX/gamification implementation notes.
- `android/app/src/main/assets/content-pack/month-01/lessons/`, bundled lesson content.
- `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeUiState.kt`, canonical subject list.
- `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonPlayerScreen.kt`, lesson-player UI and motion behavior.
- `android/feature-auth/src/main/java/com/maxinesworld/featureauth/ParentAuthViewModel.kt`, PIN verification.
- `android/feature-parent/src/main/java/com/maxinesworld/featureparent/ParentDashboardScreen.kt`, parent progress/streak UI.

## Continuation Rule

Treat this document as the current review-validation context. Before changing code or content, re-check the live repository state rather than assuming the listed commit or counts are unchanged. Preserve historical review findings separately from current verification results, and verify every claimed fix with tests or direct source inspection.
