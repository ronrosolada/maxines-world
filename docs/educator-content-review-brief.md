# Maxine's World, Independent Educator Content Review Brief

**Status: Record, dated review document; current canonical status lives in `HANDOFF.md` (spec CH-13).**

**Purpose:** Give a separate LLM or human educator enough context to review the bundled Grade 3 lesson content without relying on chat history.

**Review baseline:** branch `content/educator-review`, commit `d2a3a86c79183c19554d60e32e598c76e78c8ae2`  
**Parent baseline:** `origin/main` at `253233cac15a5924320e35292d6be82ed318a212`  
**As of:** 2026-08-03 (Asia/Singapore)

## Important status note

This branch currently has `educatorValidated=true` and `releaseStatus="RELEASED"` on all 349 lesson files. Those fields came from the prior RonBot curation pass and are **not proof of independent human curriculum sign-off**.

An independent reviewer must:

- inspect the actual learner-facing copy;
- verify factual accuracy and Grade 3 suitability;
- verify that assessments test the stated objective;
- record concrete evidence for every issue;
- recommend `BLOCK`, `CONDITIONAL`, or `ACCEPT`;
- **not** run `mark_lessons_reviewed.py` merely because automated checks pass.

The release gate checks metadata, not educational truth. Computers remain very good at approving paperwork.

## Product and learner context

- Product: child-safe, privacy-first Android learning app for Maxine, age 8.
- Intended level: Philippine Grade 3, DepEd MATATAG-aligned content.
- Subjects in the bundled pack: English, Filipino, Mathematics, Science, GMRC, and Makabansa/Araling Panlipunan.
- Experience: short Milo-guided activities, local examples, simple language, positive feedback, offline playback.
- Safety: no ads, tracking, unsafe experiments, frightening content, personal-data requests, or instructions to contact strangers.

## Content scope

All lesson files are under:

```text
android/app/src/main/assets/content-pack/month-01/lessons/
```

There are **349 JSON lesson files**:

| ID family | Count | Contents |
|---|---:|---|
| `*-g3-m01-dNN` | 100 | Legacy hand-authored month-01 lessons: 20 each for `ARALING_PANLIPUNAN`, `ENGLISH`, `FILIPINO`, `MATHEMATICS`, and `SCIENCE`. |
| `*-g3-qN-wNN-dNN` | 249 | Converted/curated SLM lessons: English 73, Filipino 63, GMRC 24, Makabansa 26, Mathematics 38, Science 25. |

The filesystem uses both uppercase legacy subject values and lowercase quarter-lesson values. Do not infer that `ARALING_PANLIPUNAN` and `makabansa` are identical without checking the lesson objective and app mapping.

Current pack-wide facts verified at this baseline:

- 349 lessons;
- 6 activities per lesson;
- 5 assessment items per lesson;
- languages: `en-PH` and `fil-PH`;
- all files parse as JSON and the structural content tests pass;
- all 349 files currently carry approval metadata.

## What changed on this branch

The branch is a large content pass, not a small documentation-only change. It modifies the bundled JSON content and adds:

- `android/tools/content_review.py`, deterministic curation, shell normalization, assessment generation, and heuristic flags;
- `android/tools/test_content_review.py`, five unit tests for representative curation behavior;
- `android/tools/convert_slm_to_pack.py`, conversion-side changes.

The curation script intentionally does **not** establish official approval. It can make a lesson structurally clean while still leaving a human question about source fidelity, nuance, cultural accuracy, or engagement.

## Existing review and validation files

Read these, but distinguish automated evidence from educator judgment:

| File | Use |
|---|---|
| `android/tools/content_review.py` | Understand how titles, objectives, activities, assessments, and flags were generated. |
| `android/tools/test_content_review.py` | Run regression tests for the curation functions. |
| `android/tools/content-quality-audit.md` | Historical structural audit; not a semantic approval. |
| `android/tools/content-review-english-gmrc.md` | Earlier English/GMRC review findings; check whether findings were resolved at the review baseline. |
| `android/tools/content-review-filipino-makabansa.md` | Earlier Filipino/Makabansa findings; several findings are intentionally preserved as audit history. |
| `android/tools/content-review-mathematics-science.md` | Earlier Mathematics/Science findings. |
| `android/core-content/src/test/java/com/maxinesworld/corecontent/ContentPackIntegrityTest.kt` | JSON schema, IDs, activity payload, and assessment-shape checks. It does not prove factual correctness. |
| `android/app/build.gradle.kts` | `verifyPlayableContent` release gate. It checks only approval metadata. |
| `android/tools/mark_lessons_reviewed.py` | Human-accountability marker. Do not run until the review is genuinely complete. |

## Recommended review method

### 1. Establish the baseline

```bash
cd /path/to/maxines-world/repo
git status --short --branch
git rev-parse HEAD
find android/app/src/main/assets/content-pack/month-01/lessons -name '*.json' | wc -l
```

Confirm that the reviewer is looking at the intended commit, not a different branch or generated working tree.

### 2. Run non-destructive checks

```bash
cd android
python3 tools/content_review.py --dry-run
python3 tools/content_review.py --dry-run --include-legacy
python3 -m unittest discover -s tools -p 'test_content_review.py' -v
./gradlew :core-content:testDebugUnitTest
./gradlew :app:verifyPlayableContent
```

Expected results at this baseline:

- quarter lessons: `changed=0`, `reviewed=249`, `remaining_count=0`;
- all lessons including legacy: `changed=0`, `reviewed=349`, `remaining_count=0`;
- content-review unit tests: 5 passing;
- playable-content gate: 349 approved by metadata.

These results are necessary but insufficient. A clean heuristic audit is not a teacher.

### 3. Review by subject and progression

Review every lesson, or explicitly document a sampling method that covers every subject, quarter, module, and activity type. For each lesson inspect at least:

- `title`
- `objective`
- `introduction`
- `vocabulary`
- all six `activities[]` entries, including `instruction`, `content`, `narration`, feedback, hints, and accessibility text;
- all five `assessment.items[]`, including prompt, options, correct option, and explanation;
- `subject`, `language`, `lessonId`, and quarter/week/day placement.

Use the source SLM or official curriculum competency when making an alignment claim. If the source is unavailable, mark the claim **unverified** rather than guessing.

### 4. Apply the educator rubric

#### A. Factual accuracy, blocking when wrong

- Is every definition, example, rule, calculation, historical statement, and science claim correct?
- Are exceptions and uncertainty handled honestly at Grade 3 level?
- Are maths units, symbols, place values, operations, and answers correct?
- Are science claims observable, age-appropriate, and free of unsafe advice?
- Are Makabansa examples culturally and historically responsible rather than invented generalizations?
- Does GMRC teach respectful, safe choices without shame, coercion, or one morally simplistic answer to a genuinely contextual situation?

#### B. Curriculum and cognitive alignment

- Does the objective describe one teachable Grade 3 skill?
- Do examples, activities, and assessments practice that same skill?
- Is the difficulty appropriate for the stated quarter/module?
- Does the assessment reward understanding rather than guessing a keyword?
- Are distractors plausible but clearly wrong for a defensible reason - not nonsense or unrelated filler?

#### C. Language quality

- English copy is natural, concise, and grammatically correct.
- Filipino copy is idiomatic, age-appropriate Filipino; avoid unnecessary English bleed.
- `fil-PH` lessons must not contain unexplained English prompts such as “Which statement,” “Complete the lesson,” or generic English feedback.
- Definitions must use words a Grade 3 learner can understand, or explain the harder word.
- Avoid literal translations that sound unnatural in Filipino.

#### D. Engagement and pedagogy

- Does Milo add warmth without replacing the learning task?
- Are examples concrete and connected to a child's life in the Philippines?
- Do activities vary the learner's thinking rather than repeat the same interaction six times?
- Is the learner asked to explain, compare, classify, predict, or apply - not only recall?
- Does feedback tell the learner what to revisit, instead of only saying “correct” or “try again”?
- Is the reading load small enough for a tablet lesson and Grade 3 independent use?

#### E. Child safety and inclusion

- No unsafe physical or online action is required.
- No frightening, violent, sexual, self-harm, weapon, or stranger-contact content appears without an explicit safeguarding reason; normally block it.
- No child is shamed for mistakes, family structure, disability, religion, language, poverty, or ability.
- Religious and values content must respect differences and avoid presenting one family's practice as universal fact.
- Examples should include varied names, communities, abilities, and living situations without turning diversity into a stereotype.

#### F. Technical content integrity

- `lessonId` matches filename.
- Exactly six activities in this order:
  `ANIMATED_EXPLANATION`, `HOTSPOT_IMAGE`, `SORT_AND_CLASSIFY`, `MULTIPLE_CHOICE`, `MATCHING_PAIRS`, `SEQUENCE_BUILDER`.
- Every activity has content that matches its renderer and completion rule.
- Every assessment has exactly five items and exactly one correct option per item.
- No placeholder, circular, empty, duplicate, or answer-revealing text.
- Accessibility text communicates the same task and answer-independent information as the visual interaction.

## High-risk patterns to inspect closely

These are review prompts, not confirmed defects:

- generated profiles may be topic-grounded but still oversimplify a concept;
- broad topic detection can select a generic fallback when an objective contains several concepts;
- repeated activity shells can make lessons technically complete but pedagogically monotonous;
- “one hundredth” can be confused with the ordinal “100th” versus the fraction “one hundredth”;
- science statements using “may,” “can,” or “usually” need context so they do not become false universal rules;
- English examples should be checked for natural grammar, especially pronouns and first-person forms;
- Filipino and Makabansa copy should be checked by a fluent Filipino educator, not only by an English-speaking model;
- legacy Araling Panlipunan content and converted Makabansa content need separate source/alignment checks;
- automated safety keyword flags may be false positives, but every surrounding sentence still requires manual reading;
- a lesson can have valid JSON, five correct answer IDs, and a completely wrong answer explanation. Check the meaning, not just the pointers.

## Required review output

The next reviewer should create or update a report under `docs/`, for example:

```text
docs/educator-content-review-<date>.md
```

Use this minimum format:

```markdown
# Educator Content Review, <date>

## Baseline
- Commit:
- Reviewer/model:
- Source documents actually consulted:
- Scope and sampling method:

## Verdict
- Overall: BLOCK | CONDITIONAL | ACCEPT
- Lessons reviewed:
- Critical findings:
- Major findings:
- Minor findings:
- Lessons safe to release:
- Lessons requiring revision:

## Findings
| Severity | Lesson ID | JSON field/activity | Evidence | Why it matters | Recommended fix |
|---|---|---|---|---|---|

## Subject summaries
### English
### Filipino
### Mathematics
### Science
### GMRC
### Makabansa / Araling Panlipunan

## Verification
- Commands run:
- Results:
- Tests not run and why:

## Approval recommendation
- Do not mark lessons released / mark only these exact lesson IDs:
- Human follow-up required:
```

Every `CRITICAL` or `MAJOR` finding must include an exact lesson ID and field path. “Content feels generic” is a useful observation but not a reproducible finding until it identifies the copy and explains the educational consequence.

## Approval rule

Only after factual, pedagogical, language, safety, and technical findings are resolved should an authorized human decide whether to run:

```bash
cd android
python3 tools/mark_lessons_reviewed.py --dry-run
# Run without --dry-run only after the human review decision.
```

Do not use `educatorValidated`, `alignmentStatus`, `contentReview.reviewer`, or a passing Gradle task as a substitute for independent review. They describe process metadata; they do not make a false statement true.
