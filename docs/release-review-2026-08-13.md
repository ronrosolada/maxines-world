# Release Review — v0.31.0 (2026-08-13)

QA/review of `ronrosolada/maxines-world` at v0.30.0, evaluated against the
stated product goal: **learning comes first, facts must be trustworthy, and
delight should help Maxine stay curious rather than distract from
understanding.**

## What was verified at v0.30.0 (passing)

| Area | Result |
|---|---|
| Educator content gate | 358/358 lessons `educatorValidated=true` + `RELEASED`; gate wired into `check` and `assembleRelease` |
| Content integrity | 358 lessons, 0 errors, 0 warnings (`content_pack_validation.py --strict`) |
| Semantic quality | `content_quality_audit.py --check` clean; `dedupe_lesson_titles.py --check` 0 dupes |
| Privacy posture | `allowBackup=false`, all domains excluded from backup/transfer, cleartext pinned to the LAN media host only |
| Mini-game isolation | 29/29 HTML games CSP-locked, no external URLs or browser network APIs |
| Media pipeline | catalog path validation, size caps, SHA-256 + atomic promotion |
| CI | green on main; emulator migration tests for 3 modules |
| Repo hygiene | 0 TODO/FIXME, no lint baselines, changelog current |

## Findings fixed in this release

1. **HANDOFF.md was three releases stale** (described v0.27.0, wrong DB
   version, wrong SVG accessibility flag, stale deferred list) — rewritten to
   the 0.31.0 baseline; deferred educator items now link to issues #76–#79.
2. **PR #75 (media assessment gates) unmerged** — rebased onto main, CI-green,
   squash-merged (#75). Tagalog memory checks now gate on video playback
   completion.
3. **CI instrumented-test gap** — the emulator job now also runs
   `feature-child-home`, `feature-rewards`, and `feature-lesson-player`
   connected tests (six modules total).
4. **Silent renderer fallback** — `rendererType()` now returns null for
   unknown types; conversion drops the step with a log; a lesson with no
   playable steps fails to load. Unit tests added.
5. **Dependency drift** — AGP 8.7.3→8.9.2, Gradle 8.9→8.11.1, Kotlin
   2.1.0→2.1.21, KSP 2.1.21-1.0.29, Compose BOM 2024.12.01→2025.05.00, Room
   2.6.1→2.7.1, Navigation Compose 2.8.5→2.9.0.
6. **Dead code and APK bloat** — removed `VillageHomeScreen`, `VillageHomeV17`,
   `VillageChromeV16` and their unreferenced assets; mascot + subject artwork
   converted to WebP (≈9.7 MB smaller APK).
7. **Content formatting** — 9 Filipino story lessons re-serialized with the
   canonical top-level key order (values unchanged; content gates re-verified
   clean).
8. **Educator debt visibility** — M1, M2, M7, and the 122 same-keyed-pair
   finding are now tracked GitHub issues instead of living only in a review
   document.

## Verification results

_(completed after the local + CI build runs)_

- Local: `./gradlew check assembleRelease` + content tooling checks
- CI: full pipeline on the `release/v0.31.0` PR, including connected tests on
  the API 34 emulator for six modules
- Release gate: `verifyPlayableContent` + release assemble on `v0.31.0` tag
- APK inspection: package, version 0.31.0 (code 32), INTERNET permission for
  optional LAN media only, release signature, minification

## Remaining non-blocking boundaries

- English Q4 deferred until source curriculum is available.
- No physical-device child session yet (emulator coverage only).
- Educator follow-ups #76–#79 remain open by design.
