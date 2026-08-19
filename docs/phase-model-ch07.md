# CH-07 Phase Model, Content & Engine Follow-ups

Status: **planning artifact**, nothing here silently drops an educator-review
finding. Each section maps to a GitHub issue and a verification gate.

Sources: `docs/educator-content-review-2026-08-07-r2.md` §5 (M1, M2, M7,
same-keyed pairs), design.md §2 (animal density).

---

## 1. M1, Objective pacing (Filipino + Makabansa), issue #76

**Finding:** 46 real objectives are stretched over 142 files, thinning
per-lesson focus.

**Plan (scheduled re-authoring pass):**

1. Build `tools/objective_pacing_audit.py`: map each lesson's `objective`
   against the curriculum mapping; report objective → file fan-out.
   Target: ≤3 files per objective for G3 (spiral review), with each file
   naming ONE focus objective.
2. Re-author the over-fanned objectives in waves (filipino Q1–Q4, then
   makabansa), reusing the existing `content_pack_staging.py` pipeline.
   - Keep `lessonId`s stable; change `objective` + `introduction` +
     activity instructions only where pacing is the fix.
   - Never drop a lesson from the 142 set, pacing pass redistributes
     focus, it does not delete coverage.
3. Gate: `objective_pacing_audit.py` returns 0 fan-out > 3 before the next
   content merge. Wire into CI next to `content_similarity_gate.py`.

**Do not silently drop:** the 142 files remain shipped until their
re-authored replacements land and the educator gate re-approves.

## 2. M2, Production objectives are never assessed, issue #77

**Finding:** Production objectives (writing/producing tasks) exist in the
curriculum mapping, but no activity type assesses them.

**Plan (new activity type, engine + content):**

1. **Engine:** add `WRITING_PRODUCTION_V1` activity type:
   - Renderer: child composes a short answer (text input) or selects
     sentence-builder tiles; no free-text grading, G3 writing is assessed
     against a small checklist the child self-marks with the guide
     (e.g., "Did I write a whole sentence? Does it start with a capital?").
   - Scoring: checklist-derived completion (not AI-graded), consistent
     with the offline/privacy-first contract.
   - Model: extend `ActivityStep` with `writingChecklist: List<String>`.
2. **Content:** author production items for the objectives the mapping
   marks as producing, starting with Filipino (salaysay) and English
   (personal recount), the two subjects where M2 was observed.
3. **Gate:** every authored production item must pass
   `content_pack_validation.py` and the educator gate; items remain
   behind the release flag until the educator signs off.

## 3. M7, Retry feedback never says what went wrong, issue #78

**Status: RESOLVED (v0.31.1 line).**

- Corpus audit: `tools/retry_feedback_audit.py` reports
  **0 generic retry strings, 0 missing retry strings** across 358 lessons.
- Engine: when authored retry copy is absent or generic, the fallback now
  names the correct answer (`Not quite. The answer is "X".` /
  `Hindi pa tama. Ang sagot ay "X".`).
- The 4 remaining generic strings found during the pass were re-authored
  with corrective guidance.
- Emojis in feedback (5,460 instances) are deliberate delight copy
  (`add_lesson_delight.py`) approved in review r2, reported by the audit
  as advisory, not a gate.

## 4. Same-keyed answer pairs, issue #79

**Status: RESOLVED (v0.31.1 line).**

- Detector: `tools/same_keyed_answers.py` classifies groups into
  same-file (skill-differentiation), cross-file (advisory, spiral
  review reuses vocabulary), and literal duplicates (action required).
- The 2 literal duplicate pairs (identical prompt + identical correct
  answer in one lesson) were re-keyed with new values:
  `mathematics-g3-q1-w02-d03-q05` and `mathematics-g3-q1-w02-d04-q05`.
- Remaining 217 groups are benign per review r2 (e.g., simuno/panaguri
  items testing identify-vs-supply on the same sentence).

## 5. Village animal density (B), design.md §2

**Status: spec reconciled; art pass scheduled here.**

- The shipped home is the card-based Playroom; the 8–14 standing-animal
  village scenery requires the full-illustration pass.
- Milestone note added to design.md pointing at this document.

---

## Sequencing

1. v0.31.1 release (current line).
2. M7 + same-keyed hardening land (this cycle) → v0.31.2 line.
3. M2 engine scaffold (`WRITING_PRODUCTION_V1`) behind release flag.
4. M1 pacing audit tooling, then wave re-authoring (Filipino first).
5. Village art pass against the density table.
