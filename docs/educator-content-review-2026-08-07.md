# Educator Content Review — 2026-08-07

## Scope and decision

Review date: 2026-08-07 09:06:58 +08:00

Learner: Philippine Grade 3 / age 8

Canonical release bundle: `android/app/src/main/assets/content-pack/month-01/lessons/`

Fallback bundle: `android/app/src/main/assets/content/ph-matatag/grade-3/`

Reviewer: RonBot educator-style review. No separate curriculum-owner or human educator sign-off is recorded.

Decision:

- Canonical bundle: CONDITIONAL RELEASE CANDIDATE. All automated, structural, assessment-integrity, safety, similarity, Android build, and connected-emulator gates pass. Formal human educator/curriculum-owner sign-off is still required before a child-facing release.
- Fallback bundle: BLOCKED. Its 249 pending lessons remain `educatorValidated=false` / `releaseStatus=REQUIRES_EDUCATOR_REVIEW`; they were restored unchanged and were not promoted by this review.
- Approval metadata changed by this review: none.

## Canonical review result

All 358 canonical month-01 lesson JSON files were checked. The independent review and follow-up repair pass covered:

- assessment key/explanation agreement, including negative questions;
- objective alignment and meaningful distractors;
- placeholder and generated-shell text;
- duplicate and near-duplicate lesson content;
- math calculations, ordering, equal-groups representations, and answer keys;
- Filipino language/cultural wording and English bleed;
- GMRC, Makabansa, and Araling Panlipunan terminology and distractors;
- child safety, including unsafe light/sun examples;
- matching/sorting payloads, unique target counts, and activity rule compatibility;
- item-ID collisions and deterministic answer-position bias;
- asset IDs and offline lesson loading.

Confirmed defects repaired in the canonical bundle included:

- nine equal-groups mathematics items whose options did not contain the requested multiplication sentence;
- the false least-to-greatest ordering example in `mathematics-g3-q1-w02-d04`;
- contradictory negative-question explanations;
- generic `Milo asks` explanations and colliding early-English item IDs;
- generated placeholder distractors and generic Filipino/Makabansa matching labels;
- ambiguous spelling distractors in seven Filipino spelling lessons;
- the incorrect reply key in `filipino-g3-q1-w02-d04` (`Walang anuman po.` is keyed for the reply to `Salamat po?`);
- unsafe science choices involving looking directly at the sun or staring at bright light;
- overlapping Filipino sorting categories;
- five substantive English near-duplicate lessons. The affected personal-experience and sentence-polishing lessons now have distinct examples, activities, matching pairs, sequences, prompts, and assessment banks;
- the authored hotspot contract in `english-g3-q1-w01-d01`: its eight fiesta clues now require `ALL_TARGETS_VISITED` with `targetCount: 8`.

No `educatorValidated`, `releaseStatus`, `alignmentStatus`, `contentReview`, or other approval field was changed.

## Verification evidence

All commands were run from `android/` after the repairs.

- `python3 tools/content_pack_validation.py --strict --require-released`: 358 lessons, 0 errors, 0 warnings.
- `python3 tools/content_quality_audit.py --check`: 358 lessons, 0 findings.
- `python3 tools/content_similarity_gate.py --threshold 0.85`: 0 near-duplicate pairs, 0 clusters.
- `python3 tools/dedupe_lesson_titles.py --check`: 0 duplicate title groups.
- `python3 -m unittest discover -s tools -p 'test_*.py' -v`: exit 0.
- `./gradlew :core-content:testDebugUnitTest :app:verifyPlayableContent`: pass; release gate reports 358 playable educator-reviewed lessons.
- `./gradlew :app:assembleDebug`: BUILD SUCCESSFUL.
- `./gradlew :app:connectedDebugAndroidTest`: 23/23 tests passed on `MaxinesWorld(AVD) - 15` after the hotspot-rule correction.
- Fresh emulator launch of `com.maxinesworld.app.debug`: successful; onboarding UI rendered with no startup fatal exception or ANR in the captured logcat window.
- Independent final audit: 358 canonical lessons parse; all assessment items have valid keys and explanations; no placeholder/unsafe/ambiguous/sort/match/math findings; canonical and fallback lesson IDs remain consistent where shared.

The first connected run exposed one stale test/content contract (`VIEW_AND_ACKNOWLEDGE` versus the authored eight-target hotspot). That was corrected in the lesson asset and the complete 23-test suite was rerun successfully.

## Fallback bundle status

The fallback bundle contains 249 pending JSON lessons. It was restored to its pre-review state after detecting that wholesale mirroring would incorrectly copy canonical release metadata into the fallback queue. No fallback approval metadata or learner-facing fallback file is being claimed as reviewed or released by this report.

The fallback bundle remains blocked until it receives a separate source-traceability, factual, language, pedagogical, safety, assessment, and human educator review—or is proven unreachable and removed/deprecated safely.

## Approval statement

This report records a completed engineering and educator-style review of the canonical 358-lesson bundle, not formal curriculum-owner approval. The canonical bundle is ready for explicit human sign-off; the fallback bundle is not approved for child-facing release.
