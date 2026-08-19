# Educator Content Review, 2026-08-06

**Status: Record, dated review document; current canonical status lives in `HANDOFF.md` (spec CH-13).**

## Baseline

- **Repository:** `/home/ron/workspace/maxines-world/repo`
- **Branch:** `feat/lesson-visuals-and-player`
- **Commit at review:** `99ae333`
- **Reviewer:** RonBot, independent educator-review pass with independent subject-level review workers
- **Learner:** Philippine Grade 3 / age 8; child-safe, privacy-first Maxine's World
- **Scope:** all 349 JSON lessons under `android/app/src/main/assets/content-pack/month-01/lessons/`
- **Sampling method:** every learner-facing string and payload was scanned; every lesson was checked for structure, assessment key integrity, safety markers, language bleed, duplicate/placeholder content, answer-position bias, and similarity. The subject/quarter matrix was checked, followed by representative emulator navigation through onboarding, catalog, module list, lesson list, and a live Mathematics lesson.
- **Source material consulted:** the repository educator brief, archived 2026-08-03 educator/adversarial reviews, `ph-matatag-g3-filipino-q1-slm-v2.zip` `source-review.json`, current content loader/player code, and current validation tools. No complete official competency/source corpus is present in the repository.

## Verdict

- **Overall:** **CONDITIONAL**
- **Learner-facing reproducible defects found in this pass:** resolved
- **Lessons structurally/technically verified:** 349/349
- **Lessons safe to release from the content-integrity perspective:** 349/349 after the fixes in this working tree
- **Lessons requiring source/authorized-human follow-up:** 349/349 for factual/curriculum sign-off
- **Critical findings remaining:** 0 confirmed in the scanned content
- **Major findings remaining:** source traceability and independent human curriculum approval; repeated source objectives still need curriculum-owner progression decisions
- **Human approval marker:** not run. Existing `educatorValidated=true` / `releaseStatus="RELEASED"` metadata predates this review and is not treated as independent sign-off.

## Findings fixed in this loop

| Severity | Scope | Evidence | Fix |
|---|---|---|---|
| Major | 60 assessment items | Negative prompts selected a non-example while explanations said the choice followed the lesson | Rewrote explanations to explicitly state why the keyed choice does not fit; verified keyed text remained attached after option rotation |
| Major | 115 matching activities | Right-hand labels were identical or generic, including `fits the lesson idea`, `mabuting asal`, and `angkop na halimbawa` | Generated distinct, topic-linked right labels and rejected duplicate labels per activity |
| Major | 145 lessons / stock copy occurrences | Learner-facing `a correct example` and related template language | Replaced with topic/objective-specific copy; the remaining confirmed placeholder scan is zero |
| Major | 7 Science lessons | Learner-visible unsafe distractor `look directly at the Sun` | Replaced with a safe, clearly incorrect distractor |
| Major | 241 live MC activities and legacy assessment options | Correct answer was concentrated in option position 0 | Deterministically rebalanced live and assessment option positions while remapping keys; verified keyed answer text before/after rotation |
| Major | 500 legacy assessment items | Assessment items lacked explicit `type` values | Added `MULTIPLE_CHOICE` without changing answer identity |
| Major | 123 lessons | Stock shells such as `Study the idea and listen to Milo` / `Choose the best answer` | Replaced with one-sentence, subject/objective-specific learner instructions |
| Major | 20 Mathematics sequence activities | Generic `read/choose operation/solve/check` steps were used for geometry objectives | Replaced with geometry-specific observation/identification/check steps |
| Major | 4 English vocabulary entries | Full sentence examples were mislabeled as vocabulary terms | Replaced with actual terms (`compound sentence`, `joining word`, `complete idea`) and child-friendly definitions |
| Major | 274 near-duplicate pairs / 25 clusters | Similarity gate found exact or pedagogically identical payloads despite answer-order changes | Added concrete transfer contexts per cluster member and removed the final similarity violation; the release similarity scan is now zero |
| Minor | generator behavior | `content_review.py` would regenerate clean non-English quarterly lessons unconditionally | Changed it to rewrite only lessons with actual flags/placeholders, making dry-run idempotent and safe |
| Minor | alignment audit | Literal keyword overlap produced false warnings for numeric/inflected objectives | Audit now warns only when content and alignment status are both absent; all current lessons have content and explicit alignment status |

## Current unresolved conditions

### Source traceability / factual sign-off, blocking for `ACCEPT`

- 249 quarterly lessons have no `sourceRecords` in the bundled JSON.
- 100 legacy lessons retain `sourceRecords.reviewStatus = "PENDING_HUMAN_REVIEW"`.
- The bundled Filipino Q1 source package explicitly records `educatorValidated: false` and says to ship it as `REQUIRES_EDUCATOR_REVIEW`.
- The repository does not contain a complete official DepEd competency/source corpus for all subjects and quarters. I did not invent source mappings or mark unverified claims as verified.

This is why the verdict is conditional rather than `ACCEPT`; it is an evidence/accountability gap, not a remaining placeholder or schema defect.

### Curriculum progression, requires curriculum-owner decision

The pack still contains 40 duplicate-objective groups covering 216 lessons; the largest is the Filipino `simuno at panaguri` objective at 32 lessons. Exact pedagogical payload duplicates and similarity violations are fixed, and each repeated lesson now has distinct transfer context, but repeated source objectives still need an educator to decide whether they represent intentional practice, a spiral, or overproduction. That decision cannot be made safely from the generated JSON alone.

### Scope gaps preserved, not fabricated

- English quarterly content currently covers Q1–Q3; no English Q4 source package is present.
- Araling Panlipunan quarterly content is not present; the pack contains the 20 legacy lessons.

No speculative Q4/AP lessons were generated to make the matrix look complete.

## Subject summaries

- **English:** legacy + Q1–Q3 content scanned; grammar/prompt copy cleaned, stock distractors removed, answer positions rebalanced. Q4 remains a source/scope gap.
- **Filipino:** legacy + Q1–Q4 content scanned; Filipino UI/accessibility copy and generic matching/assessment language cleaned. Source-backed Filipino Q1 package remains explicitly unvalidated.
- **Mathematics:** legacy + Q1–Q4 content scanned; negative assessment logic, geometry sequences, unsafe/stock distractors, and answer-position bias fixed. Numeric key integrity passed.
- **Science:** legacy + Q1–Q4 content scanned; unsafe Sun distractors removed and safety wording checked. Full factual source sign-off remains pending.
- **GMRC:** Q1–Q4 scanned; matching labels and repeated contexts diversified. Values/cultural nuance still needs fluent educator/source review.
- **Makabansa / Araling Panlipunan:** Makabansa Q1–Q4 and legacy AP scanned; matching labels and exact duplicates diversified. Historical/cultural claims remain source-review dependent.

## Verification

All of the following completed successfully after the final content changes:

```text
python3 tools/repair_educator_findings.py --check
  changed=0, remaining_count=0

python3 tools/diversify_exact_duplicates.py --check
  exact duplicate groups=0

python3 tools/content_review.py --dry-run
python3 tools/content_review.py --dry-run --include-legacy
  changed=0, remaining_count=0

python3 tools/content_quality_audit.py --summary
  349 lessons; 0 findings

python3 tools/dedupe_lesson_titles.py --check
  duplicate title groups=0

python3 tools/content_similarity_gate.py --threshold 0.85
  scanned=349; near-duplicate pairs=0; clusters=0

python3 -m unittest discover -s tools -p 'test_content_review.py' -v
  pass

./gradlew :core-content:testDebugUnitTest :app:verifyPlayableContent
  pass

./gradlew :app:assembleDebug
  BUILD SUCCESSFUL

python3 -m py_compile <review/repair/audit scripts>
git diff --check
  pass
```

### Emulator smoke verification

- Emulator: `emulator-5554`
- Installed: `com.maxinesworld.app.debug`
- Clean onboarding rendered: PIN setup → child profile → profile selection
- Catalog rendered all seven visible subjects/tracks
- Mathematics module list rendered 20 lessons and quarter/week sections
- `Building Numbers to 10,000` opened in the lesson player
- Live player showed vocabulary, visual story, Read Along controls, narration text, and Continue
- No fatal exception, ANR, or content-load error observed in the checked route

## Approval recommendation

- **Do not run** `tools/mark_lessons_reviewed.py` automatically.
- Keep the current content changes available for review.
- Before `ACCEPT`, obtain a fluent subject educator/curriculum owner to:
  1. attach or verify exact source evidence for all 349 lessons;
  2. review the 40 repeated-objective groups and label intentional practice vs. revision;
  3. decide whether English Q4/AP quarterly scope is intentionally deferred;
  4. record an authorized reviewed commit and explicit sign-off.

The automated and learner-facing repair loop is complete. The remaining work is evidence ownership, not another blind generator pass. Computers are wonderfully deterministic; curriculum accountability is still stubbornly human.
