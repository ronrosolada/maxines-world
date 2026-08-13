# Educator Content Review — 2026-08-13 (RonBot as educator)

Scope: full corpus educator review (all 358 lessons, 2,148 activities,
1,790 assessment items). Videos excluded per instruction. Review + fix +
author as needed.

## Method

1. Structural gates: content_pack_validation (--strict), content_quality_audit,
   content_similarity_gate, retry_feedback_audit, same_keyed_answers.
2. Custom mechanical detectors (answer-position, language bleed, template
   phrases, meta options, vocab placeholders, truncation, intro reuse).
3. Semantic educator review by 3 delegate agents (gpt-5.6-luna via
   openai-codex): English lane, Filipino-family lane (filipino, makabansa,
   gmrc, ARALING_PANLIPUNAN), Math/Science/AP lane.
4. Fixes applied in waves; every wave re-validated by all gates.

## Findings & fixes (wave 1 — mechanical)

| # | Severity | Finding | Fix | Status |
|---|----------|---------|-----|--------|
| 1 | P0 | 57 lessons keyed every assessment answer at the same option index; MCQ renderer does not shuffle → children can win by always picking the same spot | Re-keyed all 57 with rotated positions; keyed text verified identical | DONE |
| 2 | P2 | 100 lessons shared the verbatim "May bagong misyon si Milo! 🐱✨" opener | Diversified across 8 child-facing openers, keeping rule clauses | DONE |
| 3 | M2 | Production objective unassessed: AP m01-d20 (cultural map building) | Authored q06: evidence-gathering first step | DONE |
| 4 | M2 | Production objective unassessed: FIL m01-d07 (word building/spelling) | Authored q06: syllable assembly | DONE |
| 5 | P1 | english q1-w01-d05 q05 was off-objective story recall in a telling-sentence lesson | Authored q06 production item (period-ending telling sentence) | DONE |
| 6 | P1 | 4 maikling-buod lessons: q5 explanations didn't teach summary construction | Rewrote all 4 to teach tauhan+suliranin+pangyayari+wakas | DONE |
| 7 | P1 | 18 legacy explanations carried the meta-wrapper "Ang tamang sagot ay X dahil sumusunod ito sa paliwanag ng aralin:" | Stripped wrapper, kept teaching clause (11 exact forms; 7 variants queued) | PARTIAL → wave 2 |
| 8 | P0/P1 | 62 cross-lesson verbatim assessment groups (66 items) — near-identical re-issues across quarters (e.g. english q2-w03-d04 ≡ q3-w10-d03) | Re-author later-lesson items with fresh scenarios | QUEUED wave 2 |
| 9 | P1 | 299 remaining "Ang tamang sagot ay…" Filipino explanations | Per-item rewrite teaching reasoning | QUEUED wave 2 |

## Policy change

- `content_pack_validation.py` now supports per-lesson overrides
  (`lesson_overrides`) for production-coverage lessons with 6 items /
  passing 5 (≥ 0.8 policy). Baseline updated: AP m01-d20, FIL m01-d07,
  EN q1-w01-d05. New unit test covers override + threshold guard.

## Verified-clean areas (no defects found this round)

- Language bleed: 0 real findings (initial 2,906 flags were false
  positives from a Latin-script heuristic; proper classifier → 0).
- Stemless prompts: 0. Degenerate matching: 0. Meta options: 0.
- Vocab placeholders: 0. Template-phrase overuse: 0. Truncation: 0.
- Retry feedback: 0 generic / 0 missing (prior round's gate).
- Same-keyed literal duplicates: 0.
- 4 buod lessons verified key-consistent with their own stories
  (earlier "scrambled key" suspicion disproven after reading each
  lesson's story).

## Wave 2 (queued, runs after semantic review lands)

1. 62 duplicate-assessment groups → re-author the later lesson's items.
2. 299 Filipino "Ang tamang sagot ay" explanations → reasoning-first rewrite.
3. 7 meta-wrapper variants missed by the strict regex.
4. Any P0/P1 from the 3 semantic reviewers.

## Gates state

- content_pack_validation --strict: 0 errors / 0 warnings
- content_quality_audit: 0 findings
- content_similarity_gate (0.85): 0 clusters
- validator unit tests: 9/9

## Status

**Round 1 (semantic lanes):** math lane complete (290 items, 37 findings — all fixed).
English lanes complete (466 items, 29 findings — all fixed).
**Round 2:** science/AP/makabansa/gmrc lanes timed out with zero emissions; two
recoverable P0s fixed (science q4-w08-d04 q02/q03 safety items).
**Round 3:** persistence-mode lanes running — findings stream to
/tmp/edu_*.jsonl so timeouts cannot lose work.

## Duplicate assessments (verbatim re-issue)

66 byte-identical assessment groups (165 items) found across lessons.
6 English/math pairs re-authored with fresh scenarios (30 items); the
science/gmrc/makabansa groups are queued behind the active review lanes
(no file races while reviewers read).
