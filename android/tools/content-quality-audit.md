# Content Quality Audit — 2026-08-01 (post-conversion)

Audit of all 329 playable lessons in `app/src/main/assets/content-pack/month-01/lessons/`
(100 legacy + 229 SLM-converted), performed after the SLM conversion landed.

## Scope & method

Python audit script (inline, one-off) checked every lesson for:

- Required top-level fields (title, objective, introduction, vocabulary)
- Placeholder text (TODO / lorem / XXX / FIXME)
- Activity count (expect exactly 6) and canonical order
- Per-type content payload validity:
  - MULTIPLE_CHOICE: ≥2 options, `correctIndex` in range
  - SORT_AND_CLASSIFY: non-empty `fits` / `doesNotFit`
  - MATCHING_PAIRS: non-empty `pairs`
  - SEQUENCE_BUILDER: ≥2 `steps`
  - HOTSPOT_IMAGE: non-empty `examples` / `targets`
- Assessment: exactly 5 items, 4 options each, exactly 1 correct

## Results

| Check | Result |
|---|---|
| Empty title / objective / introduction | **0** |
| Placeholder text | **0** |
| Wrong activity count | **0** |
| MC options / correctIndex | **0** invalid |
| Sort / matching / sequence / hotspot payloads | **0** invalid |
| Converted assessments (229 lessons) | **clean**: 5 items × 4 options × 1 correct |
| Legacy assessments (100 lessons) | 3 options per item — **by design**, renderer supports any option count (`correctIndex in options.indices`) |

**Verdict: PASS.** No content defects found. The 229 converted lessons are
structurally sound and strictly better-formed than the legacy pack
(4-option assessments, canonical activity order).

## Notes

- `ActiveContentIndex` (filesDir sync) and `catalog.json` are part of the
  **retired** DreamNAS content-server path — not used by bundled content.
- Bundled loading: `ContentLessonLoader.tryPath` path 1
  (`content-pack/month-01/lessons/{id}.json`) resolves all 329 lessons.
- Legacy lessons use 3-option assessments; converted use 4. Both render.
