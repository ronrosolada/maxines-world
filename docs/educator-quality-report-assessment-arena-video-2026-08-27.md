# Educator Quality Report: Quiz Arena and Video Assessments

**Review date:** 2026-08-27  
**Learner:** Grade 3 / age 8, Philippines  
**Scope:** 18 Assessment Arena packs (180 items) and `media-assessments.json` (237 videos, 1,185 items)

## Executive decision

| Corpus | Reviewed | Decision | Release blockers |
|---|---:|---|---:|
| Assessment Arena | 18 packs / 180 items | **PASS after fixes** | 0 |
| Video lesson assessment bank | 237 videos / 1,185 items | **BLOCKED** | 3 systemic classes |
| Combined | 1,365 items | **NOT READY for educator approval** | Video bank must be re-authored against its catalog/videos |

The Arena packs are structurally sound, age-appropriate, culturally safe, and evenly keyed after two confirmed science/math defects were corrected. The video bank is technically well formed but is not an acceptable video-memory assessment corpus: most questions are repeated generic subject quizzes, most explanations are boilerplate, and several math questions had multiple correct answers.

## Method

Every item, option, key, and explanation was parsed. Checks covered:

- JSON/catalog structure, IDs, sequence, four-option/single-key integrity;
- recomputation of arithmetic and parity items;
- factual checks for science, grammar, Filipino, GMRC, and Makabansa;
- normalized duplicate prompts and exact repeated banks;
- explanation specificity and child-facing language;
- answer-position distribution;
- unsafe/adult vocabulary and fear-inducing themes;
- Grade-3 readability and unique defensible answers.

A deterministic regression gate was added at `android/tools/review_assessment_quality.py`.

## Assessment Arena findings

### Inventory and balance

- 18/18 catalogued packs present; 10 questions each (180 total).
- Subjects: English, Filipino, Mathematics, Science, GMRC, Makabansa across PH, SG, and US tracks.
- Correct-option distribution is exactly balanced: **a=45, b=45, c=45, d=45**.
- No duplicate prompts or exact duplicate items were found.
- Every item has four unique options, one valid key, and a nonblank explanation.
- No adult, violent, sexual, drug, or other child-unsafe theme was found.

### Corrected P0/P1 defects

1. **Five ambiguous even-number questions in the video bank** listed three even numbers while keying only one. These were rewritten so exactly one listed number is even:
   - `yt-inczdfejnyc-q01`
   - `yt-rahdpzozm8g-q01`
   - `yt-mhjqyako1aq-q01`
   - `yt-cq9oul3gulu-q01`
   - `yt-qguib3yrlcg-q01`

2. **Science precision defect:** `science-g3-ph.json`, item 5 called the visible white plume from a kettle “water vapor.” Water vapor is invisible; the visible plume consists of condensed droplets. The stem and explanation now ask about the invisible gas and explicitly distinguish it from the white cloud.

### Pedagogical judgment

The Arena items use short, direct stems; familiar contexts; culturally appropriate Filipino values; and concrete Grade-3 computations. Explanations generally show the reasoning or define the relevant concept. Challenge-track vocabulary is sometimes bilingual/technical, but it is introduced in context and remains suitable as supported Grade-3 extension work.

## Video assessment findings

### P0 — ambiguous/multi-correct math items (fixed)

Five “Which number is even?” items had three defensibly correct options. A child choosing another even number would be marked wrong. All five were corrected and the new gate recomputes parity validity.

### P0/P1 — assessments are not video-grounded (unresolved blocker)

- **999 duplicate normalized prompts** among 1,185 items.
- Only **186 normalized prompt texts** exist for 1,185 questions.
- Only **92 distinct five-question banks** serve 237 videos; many banks are reused across multiple stable media IDs.
- Examples include the same five generic Filipino language questions repeated across 29 videos and the same general Makabansa questions repeated across many unrelated videos.

This violates the product claim that these are post-video memory checks. A passing score can measure memorization of a recycled bank rather than recall of the watched lesson.

### P1 — generic explanations (unresolved blocker)

- **835 Filipino explanations** use the template “...tumutugon sa konseptong sinusukat ng tanong.”
- **77 English explanations** use “The correct answer is ... Apply the rule or calculation shown in the question.”
- Total: **912/1,185 explanations (77.0%)** fail the child-facing rationale standard.

These statements confirm the key but do not teach why it is correct.

### P1 — source reconciliation unavailable

The tracked assessment file contains only `mediaId` and assessment payloads; it has no title, subject, grade, transcript quote, or timestamp. The checked-in `server/content/catalog.json` is a different 337-entry catalog and matches **0/237** current `yt-*` IDs. The documented authoritative endpoint (`http://10.10.10.33/media/catalog.json`) was unreachable during review. Therefore title/video alignment and curriculum mapping could not be truthfully verified.

This is a release blocker, not a reason to invent title- or transcript-specific content.

### Safety

No confirmed adult or unsafe theme was found in either corpus. Safety-related GMRC/Makabansa choices favor telling a trusted adult, respecting others, and caring for public spaces. No option encourages secrecy, retaliation, dangerous experimentation, or contact with strangers.

## Required remediation for the video bank

1. Recover the authoritative 237-entry deployed catalog (titles, subjects, grades, episodes) and reconcile by exact `mediaId`.
2. Obtain transcripts or verified lesson summaries where possible.
3. Re-author five genuinely topic-grounded memory checks per video; do not make prompts unique merely by prefixing a title.
4. Replace all 912 generic explanations with concise reasoning in the subject language.
5. Require globally unique normalized prompts and one defensible answer.
6. Regenerate the paired deployment catalog, compare every assessment payload by `mediaId`, then run Android/build/emulator gates before promotion.

## Verification evidence

```text
python3 android/tools/validate_arena_packs.py
PASS: All 18 Assessment Arena packs (180 questions) are valid.
Position Distribution: {'a': 45, 'c': 45, 'b': 45, 'd': 45}

PYTHONPATH=android:android/tools python3 -m unittest android.tools.test_validate_arena_packs
Ran 2 tests ... OK

python3 android/tools/review_assessment_quality.py
FAIL (expected): 913 reported defects = 912 generic explanations + one corpus-level duplicate-prompt finding.
No parity ambiguity remains.
```

The educator gate intentionally remains red until the video bank is reconciled and re-authored. Structural success must not be reported as educator approval.
