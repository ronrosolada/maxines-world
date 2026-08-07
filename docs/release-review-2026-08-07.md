# Maxine's World — Release Review Brief (2026-08-07)

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
