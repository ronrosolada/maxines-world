# Child-Safety & Pedagogy Red-Team — 2026-08-30

## Executive result

**Scope:** released lesson content and lesson-flow strings only. This review sampled **12 complete lesson journeys across six subjects**, all Philippine Assessment Arena packs (Filipino, Makabansa, GMRC included), and Milo's Arena hint/feedback/celebration dialogue. It did not modify database, network, WebView, OTA, or ViewModel code.

**Findings:** 0 critical, 2 high, 4 medium, 3 low. Two clearly correct content defects were fixed: one ambiguous Science item and Filipino-policy language bleed in six mirrored Philippine Arena assets. Curriculum and interaction decisions remain recommendations.

## Method and sample

Each sampled journey was read end-to-end: introduction, vocabulary, all required activities, retry/correct feedback, accessibility alternatives, assessment prompts/options/keys/explanations, reward/completion strings, and navigation affordances.

| Subject | Sampled lesson journeys |
|---|---|
| English | `english-g3-m01-d01` (Picture Detective); `english-g3-q3-w14-d02` (Text Type Toolbox Review) |
| Filipino | `filipino-g3-m01-d01` (Pangngalan sa Ating Mundo); `filipino-g3-q4-w15-d01` (Talata: Si Jose) |
| Mathematics | `mathematics-g3-m01-d01` (Building Numbers to 10,000); `mathematics-g3-q4-w09-d04` (Shape Moves) |
| Science | `science-g3-m01-d01` (Material Detectives); `science-g3-q4-w09-d01` (Rainbows Bend Light) |
| Makabansa | `makabansa-g3-q1-w01-d01` (Ang Unang Barangay Hall); `makabansa-g3-q4-w07-d04` (Pamumuhay sa Lawa) |
| GMRC | `gmrc-g3-q1-w01-d01` (Tiwala sa Sarili); `gmrc-g3-q4-w08-d01` (Magalang sa Kapitbahayan) |

Arena coverage included every `android/app/src/main/assets/assessment-packs/*.json` pack and its mirrored `android/core-content` copy. Milo coverage included the dialogue in `AssessmentArenaScreen.kt`, completion copy in `LessonCompletionScreen.kt`, and visual celebration behavior in `MiloCelebration.kt`.

## Prioritized findings

### High — H1: Philippine FIL/Makabansa/GMRC Arena packs violated the Filipino-only policy (fixed)

**Evidence**

- `android/app/src/main/assets/assessment-packs/filipino-g3-ph.json`: `"Grade 3 Filipino: Wika at Gramatika"`; `"pangkasalukuyan o present tense"`; `"kasalungat (opposite)"`; `"pangngalang pambalana (common noun)"`.
- `android/app/src/main/assets/assessment-packs/makabansa-g3-ph.json`: `"Grade 3 Makabansa: Heograpiya at Kultura"`; `"tableland"`; `"Banaue Rice Terraces"`; `"Philippine Eagle"`; `"reforestation"`.
- `android/app/src/main/assets/assessment-packs/gmrc-g3-ph.json`: `"Grade 3 GMRC"`; `"honesty"`; `"kindness"`; `"respeto"`; `"turn"`.
- The same learner-facing strings existed in the mirrored `android/core-content/src/main/assets/assessment-packs/` files.

**Impact:** contradicts the app policy requiring 100% Filipino in Philippine Filipino/Makabansa/GMRC content, raises comprehension demands, and makes TTS code-switch unexpectedly.

**Fix:** replaced the English words with Filipino equivalents in all six mirrored Philippine pack files. IDs, option IDs, answer keys, item order, and schema are unchanged.

### High — H2: Milo's Filipino Arena narration uses English dialogue prefixes (deferred)

**Evidence**

- `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/AssessmentArenaScreen.kt:952`: `if (quiz.isCorrect) "Correct! Awesome job!" else "Milo's learning clue:"`
- `AssessmentArenaScreen.kt:962-964`: the TTS language is switched to `fil-PH`, but the spoken prefixes remain `"Correct! "` and `"Milo's Learning Clue: "`.
- `AssessmentArenaScreen.kt:916`: `"Milo's Smart Hint"`; line 923 fallback: `"Read each choice carefully..."`; lines 1002/1012: `"Check Answer"`, `"Next question"`, `"Finish quiz"`.

**Impact:** Filipino/Makabansa/GMRC learners receive English chrome and an English prefix spoken with a Filipino voice. This fails the strict language policy and may reduce comprehension.

**Recommendation:** localize Arena chrome and Milo dialogue from the active pack language, including TTS prefixes and fallback hints. Deferred because this requires a flow-level localization decision and tests, not a content-value correction.

### Medium — M1: Science question had three defensible “light behavior” answers (fixed)

**Evidence before fix**

- `android/app/src/main/assets/content-pack/month-01/lessons/science-g3-q4-w09-d01.json`, item `science-g3-q4-w09-d01-q04`: `"Which example shows light behavior?"`
- Options included `"A mirror reflects light"`, keyed `"A rainbow appears when light bends"`, and `"A window lets light through"`.

All three are light behaviors, so the keyed answer was not unique.

**Fix:** changed only the prompt to `"Which example shows light bending into colors?"`; the existing keyed rainbow answer is now unique. IDs, options, key, and schema are unchanged.

### Medium — M2: sampled lesson difficulty can jump from recognition to abstract process ordering (deferred)

**Evidence**

- `filipino-g3-m01-d01.json:160-181`: after naming and sorting nouns, the required `SEQUENCE_BUILDER` asks children to order `"Basahin... Tukuyin... Ipaliwanag..."`.
- `mathematics-g3-m01-d01.json` requires ordering a metacognitive procedure after place-value recognition; retry copy includes a dense, mechanically generated sequence explanation.

**Impact:** a child who understands the subject can be blocked by a second executive-function task (ordering an analysis routine), especially with working-memory or reading difficulties.

**Recommendation:** make procedural ordering optional practice or add a worked example and allow forward/back navigation without losing state. This is a curriculum/flow decision.

### Medium — M3: Arena celebrates only passing scores and frames rewards as claimed (deferred)

**Evidence**

- `AssessmentArenaScreen.kt:130`: celebration is visible only when `state.activeQuiz?.isPassed == true`.
- Lines 1036 and 1075: `"Quiz Passed!"` and `"Claim rewards"`.
- Hub copy at lines 211 and 259 advertises `+10` stars only at `≥80%`.

**Impact:** the pass-only mascot celebration can imply that effort below 80% is unworthy of acknowledgment. “Claim rewards” also suggests a manual claim transaction even though reward persistence is a separate implementation concern.

**Recommendation:** provide neutral effort/progress feedback for every completion, reserve the trophy for mastery, and clarify whether the button dismisses or actually claims a reward. No code change made because reward semantics are architectural/product decisions.

### Medium — M4: incomplete localization on lesson completion can expose English sanctuary copy (deferred)

**Evidence**

- `LessonCompletionScreen.kt:160` localizes the heading, but lines 164-165 always emit `"Milo's sanctuary gained: ..."` / `"Milo's sanctuary gained a new piece."`.
- Line 137 always exposes the English content description `"Sanctuary tokens"` even for Filipino lessons.

**Impact:** violates consistent Filipino presentation and creates mixed-language TalkBack output.

**Recommendation:** route all visible and semantic completion strings through the same language function/resource system.

### Low — L1: visual option marker is smaller than a touch target, though the containing card is tappable (deferred)

**Evidence**

- `AssessmentArenaScreen.kt:864-868`: option letter bubble is `32.dp`.
- The surrounding option surface is clickable and padded (`:860`, `:881-889`), so this is not itself the only target.

**Impact:** low as implemented, but accessibility tests should assert the whole option row exposes one ≥48 dp semantic click target and an accessible selected state.

**Recommendation:** add Compose accessibility tests for target size, selected-state announcement, focus order, and large-font reflow.

### Low — L2: color/contrast and focus behavior are not proven by static content review (deferred)

**Evidence**

- `AssessmentArenaScreen.kt` repeatedly uses translucent text such as `DeepNight.copy(alpha = 0.5f/0.6f/0.8f)` and colored status surfaces.
- Lesson assets provide textual alternatives (for example `filipino-g3-m01-d01.json:47,71,100,124,157,181`), but static JSON cannot prove runtime focus order or contrast after compositing.

**Recommendation:** instrument WCAG contrast checks on final colors and run TalkBack/large-font/switch-access tests for lesson activities and Arena. No color was changed without rendered evidence.

### Low — L3: metadata contains external URLs, but sampled lesson content exposes no child-clickable external navigation (no fix)

**Evidence**

- Sample lesson source records contain DepEd URLs (for example `filipino-g3-m01-d01.json`, `sourceRecords[].sourceUrl`).
- The optional video activity uses a local `mediaId` and explicitly supports offline download/skip; no sampled learner-facing field contains a web link.

**Assessment:** no unsafe child-facing external navigation was found in the reviewed lesson content. Source URLs should remain metadata-only; add a regression test if future renderers begin displaying them.

## Emotional-safety observations

No shame language directed at the child was found in the 12 sampled journeys. Retry feedback generally gives a concrete clue (for example GMRC: `"Tanungin: nakatutulong ba ang kilos sa pagkatuto at pag-unlad?"`) rather than blame. Negative words such as “wrong” in English grammar explanations describe sentence forms, not the learner. GMRC distractors include unsafe or unkind actions as contrastive examples; explanations reject those actions without labeling the child.

## Fixed versus deferred

### Fixed

1. Made `science-g3-q4-w09-d01-q04` single-answer by narrowing the stem to light bending into colors.
2. Removed identified English language bleed from Philippine Filipino, Makabansa, and GMRC Arena packs in both asset locations.

### Deferred recommendations

1. Localize all Arena/Milo/completion UI and TTS prefixes by pack language.
2. Add non-shaming completion feedback below the mastery threshold and clarify reward-claim semantics.
3. Review mandatory sequence-builder difficulty and add scaffolding/optional status where appropriate.
4. Add runtime accessibility gates for contrast, focus order, selected state, large text, and full-row touch targets.
5. Keep source URLs noninteractive in child-facing lesson renderers.

## Validation

Required gates and their final results are recorded after the fixes:

- `python3 -m unittest discover -s android/tools`
- `python3 android/tools/validate_arena_packs.py`
- `python3 docs/future-curriculum-bank/validate_bank.py`
- `git diff --check`

See the branch commit for the exact validated diff. No merge to `main` was performed.
