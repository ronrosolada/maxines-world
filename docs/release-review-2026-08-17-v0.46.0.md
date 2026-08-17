# Release Review — v0.46.0 (2026-08-17)

**Branch:** `feat/deped-video-hub-pivot`  
**Release commit:** `6a632c55` (`fix: complete adversarial UX review for release 0.46.0`)  
**Version:** `0.46.0` / version code `58`

## Review scope

This review covers the final adversarial UX pass across the DepEd Video Hub,
Grade 3 Assessment Arena, child home, and parent dashboard. The review focused
on narrow-phone behavior, child-sized touch targets, screen-reader semantics,
contrast, repeat-submission safety, deterministic media ordering, and the
accuracy of the lesson artwork description.

## Findings and fixes

### Child and phone UX

- Assessment Arena subject cards now use a minimum 56dp height and expose a
  horizontal swipe hint for all six subjects.
- Quiz retry and exit actions use a wrapping `FlowRow`, with two-column layout
  where space permits, so actions remain reachable on narrow phones.
- Child-home scrolling adds extra bottom padding on narrow layouts and at large
  font scales, preventing the final controls from being obscured or difficult to
  reach.
- The pass message is derived from the actual quiz size rather than hard-coded
  to 8/10. The ViewModel applies `ceil(itemCount × 0.8)`, so three-item packs
  correctly require three answers and standard ten-item packs require eight.

### TalkBack and semantics

- Subject cards use `Role.Tab`, `selected`, and a concise content description
  that identifies the subject, track, and selection state.
- Answer choices use `Role.RadioButton`, `selected`, and explicit state text for
  selected, not selected, correct, and incorrect states.
- The visual question segments are merged into one announcement: `Question N of
  M`, avoiding repeated or noisy progress narration.
- The arena's swipe instruction is visible to sighted users and complements the
  semantic tab navigation.

### WCAG contrast and visual clarity

- Low-contrast brand fills were replaced with dedicated readable text tokens in
  the Assessment Arena (`SuccessGreenText`) and parent dashboard (`SuccessGreenText`,
  `OnSkyBlue`, and `OnCoral`).
- `ColorContrastTest` enforces a minimum 4.5:1 WCAG AA ratio for brand text
  tokens and white-background feedback text, including gold, coral, leaf green,
  sky blue, success, error, kindness teal, review, and success-feedback tokens.
- The Fiesta Picture SVG description now names the curriculum clues—red flag,
  dancing children, food, streamers, parade, and lanterns—instead of generic
  instructional copy, improving non-visual context and auditability.

### Idempotency and deterministic behavior

- Assessment and video quiz rewards now use stable child/source IDs and metadata
  lookups before insertion. `insertIgnoring` provides conflict-safe persistence,
  so a pass/retake/pass sequence cannot award duplicate stars or coins.
- Video Hub items are sorted deterministically by grade, quarter, episode, and
  case-insensitive title. Passed items are partitioned into a separate bottom
  section while retaining the same stable order.
- The new `AssessmentRewardAndVideoOrderingTest` covers both one-reward retry
  behavior and ordering/partition rules.

## Verification results

The following commands were run on the release branch after the documentation
changes:

| Gate | Result |
|---|---|
| `:feature-lesson-player:testDebugUnitTest` | Passed; 58 tests, 0 failures |
| `:core-design-system:testDebugUnitTest` | Passed; 5 tests, 0 failures |
| `:feature-child-home:testDebugUnitTest` | Passed; 29 tests, 0 failures |
| `:feature-parent:testDebugUnitTest` | Passed; 19 tests, 0 failures |
| Targeted JVM total | Passed; 111 tests, 0 failures, 0 errors, 0 skipped |
| `git diff --check` | Passed |
| Markdown local-link/path check | Passed; 7 links checked, 0 broken |

The targeted Gradle tests were rerun with `--rerun-tasks` and completed with
`BUILD SUCCESSFUL` (198 Gradle tasks executed). They cover the changed reward,
ordering, accessibility-token, and responsive-logic areas. Compilation emitted
three pre-existing Kotlin warnings in child-home and one in the Video Hub; no
test failures or errors occurred. Connected UI tests remain device-dependent
and are not claimed here without a fresh emulator run.

## Release checklist

- [x] Version name `0.46.0` and version code `58` recorded in the Android app.
- [x] Assessment pass threshold is item-count aware.
- [x] TalkBack tab/radio/progress semantics added.
- [x] WCAG AA contrast-token tests present.
- [x] Reward insertion is retry-safe and deterministic.
- [x] Video Hub ordering and passed-item partitioning are deterministic.
- [x] README and changelog describe the current Video Hub, Assessment Arena,
  and local OTA features.
- [ ] Remote CI/device verification is outside this documentation commit unless
  separately run after push.

## Boundaries

- The media and OTA paths are trusted-LAN features; they are not cloud sync,
  telemetry, or a replacement for bundled offline lessons.
- This review verifies semantics and automated contrast coverage; a physical
  child session and independent TalkBack session remain valuable follow-up
  validation.
