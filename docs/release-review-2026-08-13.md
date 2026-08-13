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
   2.1.0→2.1.20, KSP 2.1.20-1.0.32, Room 2.6.1→2.7.1. The Compose BOM bump
   was attempted and reverted: Compose 1.8 changes IME inset propagation and
   broke the PIN keypad UI test under injected insets (reproduced on CI and
   local emulator) — a Compose bump requires physical-device validation
   first.
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

- Local: `./gradlew check assembleRelease` green; content gates clean
  (358 lessons, 0 errors, 0 dupes; 29/29 mini-games CSP-locked); connected
  tests on the MwApi34 AVD: auth 4/4, child-home 17/17, rewards 5/5,
  lesson-player 1/1.
- CI on the `release/v0.31.0` PR and on `main`: all jobs green, including
  connected tests on the API 34 emulator for six modules.
- Release gate on `v0.31.0`: green (`verifyPlayableContent` 358/358 + release
  assemble).
- APK inspection: package `com.maxinesworld.app`, version 0.31.0 (code 32),
  minSdk 26 / target 35, INTERNET permission for optional LAN media only,
  release signature present (CN=Maxines World), SHA-256
  `de81ea063edd7c208afc9c8f21b2b6fef344c0f8a35ec51e5399286109c205ff`.
- GitHub release published with the signed APK attached.

## Remaining non-blocking boundaries

- English Q4 deferred until source curriculum is available.
- No physical-device child session yet (emulator coverage only).
- Compose stays on the 1.7 line until IME behavior is validated on a
  physical device (see finding 5).
- Educator follow-ups #76–#79 remain open by design.
