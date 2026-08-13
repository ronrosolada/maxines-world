# Maxine's World — Current State & Release Handoff

**Document baseline:** 2026-08-13
**Release candidate:** `0.31.0` (`versionCode = 32`)
**Working branch:** `release/v0.31.0` (merged to `main` before tagging)
**Repository:** `ronrosolada/maxines-world` (public)

## Product goal

Maxine's World is an offline-first Android learning app for Maxine, an
animal-loving eight-year-old. It must be factual, age-appropriate,
learning-first, encouraging, privacy-preserving, and fun without letting
rewards or decoration obscure understanding.

## Quick verification

```bash
cd android
./gradlew check --stacktrace
./gradlew assembleRelease --stacktrace
python3 tools/content_quality_audit.py --check
python3 tools/dedupe_lesson_titles.py --check
python3 tools/content_pack_validation.py --strict
python3 tools/test_content_review.py
```

`check` includes the educator-content gate and the offline mini-game gate.
Release signing is read from the user-only file
`~/.gradle/maxines-world-signing.properties`; no signing secret belongs in git.
The release verification for this candidate is recorded in
`docs/release-review-2026-08-13.md`, which is the recommended starting point
for an independent review.

## Educator review round 2 (2026-08-07)

Authorized by Ron (owner) as the educator-style review + content re-author pass.
Three subject reviews (EN 93 / FIL+MKB+GMRC 142 / MATH+SCI+AP 123) plus a
mechanical sweep were consolidated; every countable CRITICAL and the mechanical
MAJORs were re-authored in place (287 files). See
`docs/educator-content-review-2026-08-07-r2.md` for the full findings table and
verdicts. All seven subjects are **Approvable**.

Remaining follow-ups are tracked as GitHub issues, not silently dropped:

- [#76](https://github.com/ronrosolada/maxines-world/issues/76) — M1: 46 real
  objectives stretched over 142 Filipino/Makabansa files (pacing/scope)
- [#77](https://github.com/ronrosolada/maxines-world/issues/77) — M2:
  production objectives never assessed (writing tasks missing)
- [#78](https://github.com/ronrosolada/maxines-world/issues/78) — M7: retry
  feedback never says what went wrong (688 occurrences)
- [#79](https://github.com/ronrosolada/maxines-world/issues/79) — 122
  same-keyed-answer pairs across EN/FIL/MATH/SCI (minor)

No `mark_lessons_reviewed.py` run was performed in this round; all 358 lessons
retain their prior metadata.

## Current product surface

### Child experience

- **Playroom** is the canonical child home.
- The home presents six learning areas: English, Mathematics, Filipino, Science,
  Makabansa, and GMRC.
- The Daily Quest is the single explicit start action; subject cards remain
  available as direct navigation.
- Milo's sanctuary renders as a living meadow scene with deterministic piece
  placement (`SanctuaryScene.kt`, pure tested model).
- Character guides show real artwork (Milo, Mira, Niko, Lakan, Duke).
- AP/Heritage content is represented by the Makabansa experience; GMRC is
  available from the first session and is not a level-gated curriculum.
- Lesson visuals are answer-neutral: they reinforce the concept without
  revealing the response.
- Progress, stars, coins, wildlife stickers, and reward-break entitlements are
  persisted locally and use idempotent reward keys.

### Reward breaks

- The Playroom reward library contains 29 bundled HTML mini-games plus the
  native reward games, with a curated kid-first shelf ordering
  (`MiniGameShelf.kt`, pure tested model).
- The library exposes categories and clear entitlement/empty states rather than
  pretending that a break is available.
- Games are bounded by the reward-break policy and contain no ads, analytics,
  runtime downloads, or network APIs.
- Attribution and provenance are in `android/app/src/main/assets/mini-games/`.

### Video lessons

- 8 full-length Tagalog videos with 10-question memory checks are available
  through optional LAN media; the memory check gates on playback completion
  (media assessment gate).

## Content

- The APK bundles **358 playable lesson JSON files** under
  `android/app/src/main/assets/content-pack/month-01/lessons/`.
- All 358 currently carry `educatorValidated=true` and
  `releaseStatus=RELEASED`; `:app:verifyPlayableContent` enforces this metadata
  across **every** lesson-bearing asset directory, and `LessonLoader`/
  `ContentLessonLoader` reject any non-`RELEASED` lesson at parse time
  (spec CH-02).
- Lesson visuals: **358 bundled SVG assets** (month-01 vectors, one per
  lesson). All 358 carry `<title>` and `<desc>` accessibility metadata.
- One deliberate exception: `english-g3-q1-w01-d01` keeps the pre-revision
  visual because the revised art dropped 3 of 7 curriculum clues (red flag,
  parade, lanterns) required by its picture-detective activity.
- `tools/content_quality_audit.py --check` and
  `tools/dedupe_lesson_titles.py --check` are read-only and must remain clean.
- English Q4 remains intentionally deferred until source curriculum is
  available and independently reviewed. This is a scope boundary, not a fake
  placeholder.
- Approval metadata and automated structural checks do not claim independent
  human curriculum sign-off; that review remains a separate responsibility.

## Data and architecture

- Content is bundled-only; there is no runtime content server or sync path.
- Room database schema is **v9** with additive migration coverage, including
  wildlife expedition data and `passedOnFirstAttempt`. Never delete shipped
  schema JSONs or lower the database version; future changes require v10+
  migrations.
- The project currently contains 19 Gradle modules: app, five core modules,
  six feature modules, four engine modules, and three native reward-game modules.
- Optional media is fetched from the trusted home LAN when configured. The
  network path is not used for telemetry, cloud content sync, or lesson
  delivery; the core lessons remain bundled.
- Pre-Playroom village screens (`VillageHomeScreen`, `VillageHomeV17`,
  `VillageChromeV16`) and their assets were removed in 0.31.0 — the Playroom is
  the only child home.

## Assessment policy

- A lesson **passes when at least 80% of assessment items are correct on
  first scoring** (4/5 on the standard five-item check; `0.8` matches the
  accuracy tiers used for stars). Enforced two ways: `content_pack_validation.py`
  errors on any lesson whose `passingCorrectCount / itemCount < 0.8`, and the
  player has no silent default — a malformed (zero-item) assessment fails
  closed.
- **Only the authored assessment contributes to accuracy and mastery.**
  Every practice activity step is `scored = false` by contract
  (`ActivityStep.scored`, enforced in `LessonPlayerViewModel.onActivityResult`);
  `saveProgress()`, the lesson-complete screen, and `Scorer.evaluateAssessment`
  all consume only scored (assessment) results. Passive exploration and
  practice answers cannot inflate the mastery signal.
- **First-attempt passes are recorded distinctly** from post-retry passes:
  `lesson_completions.passedOnFirstAttempt` (DB v9). A child who fails and
  retries is not recorded identically to a first-pass child.
- **Fail-closed rendering**: an activity with an unknown on-disk type is
  dropped with a log instead of being silently re-rendered; a lesson with no
  playable steps fails to load. The player never trusts a payload the content
  gate would reject.

## Security and privacy posture

- The release manifest intentionally includes `android.permission.INTERNET`
  for optional media served by the trusted home LAN. Cleartext is permitted
  only for the configured LAN media host; it must be replaced with HTTPS before
  exposing the endpoint outside the home network.
- App backups/data extraction are disabled; child data stays on-device.
- No cloud content sync or telemetry download is part of the release design.
- Media downloads are verified end-to-end: catalog paths are validated
  (`media/` prefix, no traversal), downloads are size-capped, SHA-256 checked,
  and promoted atomically.
- Mini-game HTML must contain the required restrictive CSP. The Gradle gate
  enforces 29/29 pages, CSP directives, no active external URLs, and no browser
  network APIs.
- Mini-game WebViews disallow file access, mixed content, multiple windows, and
  top-level navigation outside the virtual local origin.
- SVG preview WebViews reject HTTP(S) resource requests.
- Target SDK 35 edge-to-edge is enabled explicitly; parent-auth content applies
  safe system-bar insets and IME handling.

## Release gates

- CI runs on every push and PR: content integrity, schema + assets, semantic
  audit, tooling tests, educator metadata, workflow lint, assemble + lint +
  unit tests.
- **Connected tests (API 34 emulator) now gate six modules**: `core-database`,
  `app`, `feature-auth`, `feature-child-home`, `feature-rewards`, and
  `feature-lesson-player`.
- `release-gate` runs on `v*` tags: `verifyPlayableContent` must pass and the
  release must assemble before a tag is considered shippable.
- Latest full verification is recorded in `docs/release-review-2026-08-13.md`.

Before tagging `v0.31.0`:

1. Run `./gradlew check assembleRelease` with the release signing properties.
2. Run the content tooling checks from the Quick verification section.
3. Inspect the final APK with `apkanalyzer`/`aapt`:
   - package `com.maxinesworld.app`;
   - version `0.31.0`, code `32`;
   - intentional INTERNET permission for optional LAN media;
   - release signature present;
   - minification enabled.
4. Install the exact APK on a fresh API 35 emulator and walk through parent
   PIN setup, child creation/selection, Playroom, lesson launch, reward break,
   and back navigation.
5. Confirm CI is green on `main`, then tag and push the release.

## Known non-blocking scope boundaries

- English Q4 is deferred as documented above.
- Coins are displayed honestly, but a cosmetic coin-spend surface is future
  work; no fake purchase flow is exposed.
- Compose stays on the 1.7 line (BOM 2024.12.01): the 1.8 line changed IME
  inset propagation and broke the PIN keypad UI test under injected insets.
  Validate on a physical device before bumping.
- Independent human educator review remains valuable even though the release
  metadata gate is green.
- The app has not yet been exercised on a physical device with a real child
  session; emulator coverage is complete but this remains the final product
  validation.

Historical implementation notes remain available through git history; this
file intentionally describes only the current release candidate so it does not
turn into a museum exhibit with a shell prompt.
