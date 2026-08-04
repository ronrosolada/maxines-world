# Content quality gates

The content tooling is intentionally split from content authoring. It reads the
lesson pack and produces reports; it does not rewrite lesson JSON, assessments,
or assets.

## Baseline snapshot

`tools/content_pack_baseline.json` records the approved Month 01 shape at the
pinned Phase 0 commit:

- 349 lesson files
- six activities per lesson in canonical renderer order
- five assessment items per lesson
- `passingCorrectCount=4`

A deliberate corpus or shape change must update the snapshot in the same
change as the content and explain why. The validator will reject silent count
or ordering drift.

## Validator

```bash
python3 android/tools/content_pack_validation.py \
  --snapshot android/tools/content_pack_baseline.json \
  --asset-dir android/app/src/main/assets/content-pack/month-01/assets/vectors \
  --report /tmp/content-pack-validation.json
```

Audit mode fails on malformed JSON, invalid lesson shape, broken assessment
answer references, and missing declared vector assets. Known soft findings are
reported as warnings. `--strict` promotes those soft findings to errors; on
the Phase 0 pack this currently exposes 315 assessment items missing an
explicit `type` field. That cleanup is deliberately not hidden by this
 tooling-only change.

`--require-released` is the explicit educator-approval policy check. The
release metadata gate remains separate from schema and semantic checks.

## Failure-safe staging

`tools/content_pack_staging.py` provides `stage_lesson_transform()` for future
content transforms. It:

1. parses and transforms all selected lessons in memory;
2. aborts before output creation on malformed input or callback failure;
3. copies the complete source tree to a temporary sibling directory; and
4. publishes the complete result with one atomic directory rename.

The destination must be new. In-place writes are intentionally refused. This
keeps repair scripts from leaving a half-updated pack when lesson N fails.

## CI checks

Pull requests run separate checks for:

- schema and declared vector-asset references;
- semantic audit and near-duplicate reporting;
- Python content-tooling regression tests;
- educator approval metadata; and
- GitHub Actions workflow syntax via the versioned `rhysd/actionlint` Docker image.

The semantic audit is report-only until the existing baseline findings have
been reviewed and either remediated or explicitly allowlisted. Reports are
uploaded as CI artifacts so warnings remain visible rather than becoming
terminal noise.
