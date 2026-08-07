# Maxine's World — Release Review Brief (2026-08-07)

**Status: Record — dated review document; current canonical status lives in `HANDOFF.md` (spec CH-13).**

**Scope:** Independent review of `main` after PR #71
(`6dc51f0`, squash-merged 2026-08-07: "fix: child feedback and keyboard-safe PIN setup").
This brief records what changed, what was verified, and what an independent
reviewer should re-check. It intentionally separates **facts** (commands,
results) from **flags** (open items, judgment calls).

---

## 1. What changed in PR #71 (squash of 10 commits)

| Area | Change |
|---|---|
| Issues closed | #64 (PIN setup IME overlap), #65 (Sequence CTA semantics), #66 (child-facing place-value feedback), #68 (matching-pairs mismatch feedback + duplicate activity results) |
| IME fix (#64) | Parent-auth screens use `imePadding()` + scrollable columns; `AuthImeLayoutTest` dispatches real `WindowInsets` (size-independent) and asserts Digit 0 / Delete / Set PIN above the IME |
| Feedback copy (#66) | `sanitizeIncorrectFeedback` extended; new `sanitizeCorrectFeedback`/`childFacingCorrectFeedback` in core-model; same replacement table mirrored in `tools/content_review.py`; applied in Interactive Spec, Multiple Choice, lesson-player submit/assessment/activity paths |
| Content regeneration | 161 lesson JSONs regenerated with child-facing copy (jargon → "what we learned") |
| Editorial visuals | 363 SVGs installed: 358 month-01 visuals + 5 new Grade 3 pilots (`content-packs/ph-grade3-v1/`); pilot `assetId` refs wired; `checksums.sha256` refreshed |
| Tests | New `SequenceCtaContractTest` (UI-level: incomplete CTA is a disabled no-op; complete sequence submits correct); `FeedbackSanitizationTest`; extended `ActivityStepConversionTest`; `WildlifeFieldGuideOrderTest`; `ActivityResultTest` |
| Docs | `CHANGELOG.md` `[Unreleased]`; this brief; `HANDOFF.md` refreshed |

## 2. Verification evidence (2026-08-07, local runs)

| Gate | Command | Result |
|---|---|---|
| Unit tests (affected modules) | `./gradlew :core-model:testDebugUnitTest :engine-activity:testDebugUnitTest :feature-lesson-player:testDebugUnitTest` | BUILD SUCCESSFUL |
| Full app instrumented | `./gradlew :app:connectedDebugAndroidTest` (API 35 emulator, `MaxinesWorld` AVD) | **26/26 passed, 0 failed** |
| Auth instrumented | `:feature-auth:connectedDebugAndroidTest` (run by prior session) | 4/4 passed on API 35 |
| Lint + release build | `./gradlew :app:assembleRelease :app:lintDebug` | BUILD SUCCESSFUL |
| Content pack validation | `python3 tools/content_pack_validation.py` (from `android/`) | **358 lessons, 358 files, 0 errors, 0 warnings** |
| Content tools | `python3 tools/test_content_review.py` | 9/9 OK |
| APK inspection | `aapt dump badging` + `apksigner verify` on `app-release.apk` | package `com.maxinesworld.app`; versionName `0.22.0` / versionCode 23; minSdk 26, target 35; **no `android.permission.INTERNET`**; 363 SVG assets; 29.1 MB; signed (CN=Maxines World) |
| Fresh-install walkthrough | clean uninstall → install release APK → launch → PIN setup → tap name field (real Gboard opens) | Screenshot-verified: Digit 0, Delete, Set PIN all fully visible above the IME (#64 confirmed live) |

## 3. Known flags / judgment calls (not blockers)

1. **Fiesta visual exception (deliberate).** `english-g3-q1-w01-d01` keeps the
   pre-revision SVG: the revised art dropped 3 of 7 curriculum clues (red
   flag, parade, lanterns) required by its picture-detective activity, plus
   all `title`/`desc` metadata. The reverted asset passes the visual contract
   test (7/7 clues). Reviewer should confirm the exception is documented and
   intentional (it is, here and in HANDOFF.md).
2. **SVG a11y metadata gap.** The 357 revised month-01 SVGs have no
   `<title>`/`<desc>`. No runtime impact (visuals render; content
   descriptions come from lesson JSON), but the old asset contract included
   the metadata. Flagged for the editorial pipeline; not in the app's critical
   path.
3. **Sequence CTA coverage.** The audit previously noted only pure-logic
   coverage; the new `SequenceCtaContractTest` closes the UI gap (disabled
   no-op + submit contract). "Try Again" / "Keep going →" labels are asserted
   via label logic but not pinned by a dedicated UI test.
4. **Human sign-off.** Automated educator metadata (`educatorValidated=true`,
   `releaseStatus=RELEASED`) is green for all 358 lessons, but independent
   human curriculum review remains a separate responsibility.
5. **Physical-device validation.** All emulator gates pass (API 35); a real
   device session with the child is the remaining product validation.

## 4. Suggested independent review checklist

1. `git log --oneline main -8` — confirm `6dc51f0` (PR #71) sits on `61e922f` (#70).
2. Read the four issue fixes in the squash diff; check the IME test's
   assertion semantics (`AuthImeLayoutTest.kt`) and the sanitization table
   parity between `Models.kt` and `tools/content_review.py`.
3. Re-run the Quick verification commands from `HANDOFF.md` (or spot-check the
   unit + content gates; instrumented gates need an API 35 emulator).
4. Inspect `app-release.apk` badging/signature and confirm the no-INTERNET
   property.
5. Diff the 161 regenerated lesson JSONs: confirm only feedback/explanation
   copy changed and no activity structure, answers, or assetIds moved.
6. Verify the 5 pilot `assetId` refs resolve to files present under
   `content-packs/ph-grade3-v1/assets/vectors/` and are covered by
   `checksums.sha256`.
7. Confirm `CHANGELOG.md` `[Unreleased]` matches the merged changes.
8. Decide on the pending version bump (proposed `0.23.0`, versionCode 24) and
   the `v0.23.0` tag.

## 5. Open work after this review

- Version bump commit + `v0.23.0` tag (per HANDOFF release-gate list).
- Editorial pipeline: restore `<title>`/`<desc>` in revised SVGs.
- Independent human educator review of the 358 lessons.
- Physical-device validation with a real child session.

## 6. Post-review remediation (2026-08-07, same day)

An external LLM review (Opus 5.0) confirmed four findings; remediation status:

### C3 — unreviewed content shipping in the APK — RESOLVED

The review verified (and this session re-verified) that the release APK bundled
**254 unreviewed lessons**: 249 legacy `assets/content/ph-matatag/grade-3/`
fallback lessons (`educatorValidated=false`, `REQUIRES_EDUCATOR_REVIEW`,
explicitly BLOCKED by the 2026-08-07 educator review) **plus 5 pilot lessons**
in `content-packs/ph-grade3-v1/` (no approval metadata at all — manifest
self-declares `"educatorValidated": false`). The release gate only scanned
`content-pack/month-01/lessons`, and `LessonLoader` resolves paths in the
unreviewed trees.

Fix (commit `[pending]`):

1. **Removed all unreviewed lesson content from the APK** — 263 files deleted
   from `src/main/assets` (ph-matatag tree, pilot lessons, pilot manifest/
   modules/checksums, and the 5 pilot SVGs that were only referenced by the
   removed lessons). Everything remains recoverable in git history.
2. **Extended `verifyPlayableContent`** to walk the entire `src/main/assets`
   tree and fail the release build if ANY lesson-shaped JSON (has an
   `activities` list) lacks `educatorValidated=true` + `releaseStatus=RELEASED`.
   The "release gate" now means what it says.
3. **Added `ContentPackIntegrityTest` coverage**: `every lesson JSON shipped in
   app assets is educator-reviewed` — a JVM test that walks all app assets and
   asserts exactly 358 lessons, all approved. Runs in CI without an emulator.
4. Because no unreviewed lesson can now exist in the APK, `LessonLoader`
   cannot resolve one (its legacy paths miss); the loader itself needs no
   release-status check.

Verified: `:app:verifyPlayableContent` → "Release gate OK: 358 playable lessons
are educator-reviewed"; new test PASS; rebuilt release APK no longer contains
`ph-matatag` or `ph-grade3-v1` entries.

### C2 — assessment semantics — VERIFIED, decisions documented

- **Pass threshold**: `passThreshold = passingCorrectCount / itemCount` (authored
  per lesson; baseline allows 3/5 = 60%) with 0.8 fallback when itemCount is 0.
  `Scorer.evaluateAssessment` requires all items answered + accuracy ≥ threshold,
  and fails closed on invalid answer keys. **Decision**: keep authored
  thresholds (3/5 is a deliberate generator default for lessons with weaker
  distractors) but treat 3/5 as the floor to revisit when `misconceptions[]`
  (review C4) lands; documented here so it is a decision, not an artifact.
- **Practice contamination**: none. `saveProgress()` filters `it.scored`
  (exploration activities are never scored), and `Scorer.evaluateAssessment`
  filters `it.scored && it.activityId in assessmentIds` — only authored
  assessment items count toward the assessment verdict.
- **Retry honesty**: `retryAssessment()` restarts only the assessment and
  replaces prior assessment results. A retry-pass child is persisted as a pass
  (accuracy from the final attempt); the record does not distinguish
  first-pass from retry-pass. **Decision**: acceptable for the parent dashboard
  today; if "effort" metrics are wanted, add an attempts column (v9 migration).

### Assessment semantics (spec CH-03/CH-04) — DONE
- Pass policy pinned at **80%** (4/5): validation errors below it, no silent
  runtime default (fail-closed on malformed), baseline `[4]`.
- **Practice excluded from accuracy**: `ActivityStep.scored` contract —
  practice steps unscored, assessment scored; enforced at the single
  `onActivityResult` normalization point; `saveProgress`, completion screen,
  and `Scorer` consume scored results only.
- **Retry recorded distinctly**: `lesson_completions.passedOnFirstAttempt`
  (DB v9 + migration test); first-attempt and post-retry passes are
  distinguishable persisted records.
- Loader hardening (CH-02): legacy `LessonLoader` and `ActiveContentIndex`
  deleted; single bundled path; `parseBundledLesson` rejects non-`RELEASED`
  lessons (unit-tested).

### Other review findings — status

- **C1** (6-activity shell is CI-enforced; pedagogy ceiling): acknowledged,
  confirmed (`ContentPackIntegrityTest` asserts the literal type sequence).
  Phase-model redesign is a P1 product decision for Ron; not started.
- **C4** (distractor/misconception schema): acknowledged; requires content
  schema work. Not started.
- **S1** (docs drift): partially addressed by this doc and HANDOFF refresh;
  ADR-005 supersede marker + `03-combined-audit` status columns remain TODO.
- **S2** (Filipino strings unlocalized): acknowledged; `values-fil` move is a
  P1 follow-up.
- **S3** (SVG a11y): already documented non-blocking flag.
- **S4** (physical device): external; needs Maxine.
- **S5**: noted (core-network deletion, badge audit, vestigial sync tables).
