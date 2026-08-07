# Maxine's World — Current State & Release Handoff

**Document baseline:** 2026-08-07
**Release candidate:** `0.22.0` (`versionCode = 23`) — next release bump pending review
**Working branch:** `main` (PR #71 squash-merged 2026-08-07 as `6dc51f0`)
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
The most recent full verification (2026-08-07) is recorded in
`docs/release-review-2026-08-07.md`, which is the recommended starting point
for an independent review of the current main branch.

## Educator review round 2 (2026-08-07)

Authorized by Ron (owner) as the educator-style review + content re-author pass.
Three subject reviews (EN 93 / FIL+MKB+GMRC 142 / MATH+SCI+AP 123) plus a
mechanical sweep were consolidated; every countable CRITICAL and the mechanical
MAJORs were re-authored in place (287 files). See
`docs/educator-content-review-2026-08-07-r2.md` for the full findings table,
verdicts, and the deferred list (GMRC C4/C5, Makabansa C6, m01-Filipino C8,
feedback-engine M7). Verdicts: EN/MATH/SCI/AP approvable; FIL/MKB/GMRC
conditional (deferred items are follow-up work, not blockers for the owner to
ship). No `mark_lessons_reviewed.py` run was performed in this round; all 358
lessons retain their prior metadata.

## Current product surface

### Child experience

- **Playroom** is the canonical child home.
- The home presents six learning areas: English, Mathematics, Filipino, Science,
  Makabansa, and GMRC.
- AP/Heritage content is represented by the Makabansa experience; GMRC is
  available from the first session and is not a level-gated curriculum.
- Lesson visuals are answer-neutral: they reinforce the concept without
  revealing the response.
- Progress, stars, coins, wildlife stickers, and reward-break entitlements are
  persisted locally and use idempotent reward keys.

### Reward breaks

- The Playroom reward library contains 29 bundled HTML mini-games plus the
  native reward games.
- The library exposes categories and clear entitlement/empty states rather than
  pretending that a break is available.
- Games are bounded by the reward-break policy and contain no ads, analytics,
  runtime downloads, or network APIs.
- Attribution and provenance are in `android/app/src/main/assets/mini-games/`.

## Content

- The APK bundles **358 playable lesson JSON files** under
  `android/app/src/main/assets/content-pack/month-01/lessons/`.
- All 358 currently carry `educatorValidated=true` and
  `releaseStatus=RELEASED`; `:app:verifyPlayableContent` enforces this metadata
  across **every** lesson-bearing asset directory, and `LessonLoader`/
  `ContentLessonLoader` reject any non-`RELEASED` lesson at parse time
  (spec CH-02). The legacy ph-matatag fallback tree, the retired
  `ActiveContentIndex` sync path, and the unapproved pilot pack were removed
  on 2026-08-07 (external review C3).
- Lesson visuals: **358 bundled SVG assets** (month-01 vectors, one per
  lesson). A pilot pack (`content-packs/ph-grade3-v1/`) was removed from the
  APK on 2026-08-07 because it had no educator approval metadata — see the
  C3 remediation in `docs/release-review-2026-08-07.md`. The pilot content
  remains in git history for the future educator-review cycle.
- One deliberate exception: `english-g3-q1-w01-d01` keeps the pre-revision
  visual because the revised art dropped 3 of 7 curriculum clues (red flag,
  parade, lanterns) required by its picture-detective activity.
- Known editorial flag (non-blocking): the 357 revised month-01 SVGs ship
  without `<title>`/`<desc>` accessibility metadata; the old asset contract
  included it.
- `tools/content_quality_audit.py --check` and
  `tools/dedupe_lesson_titles.py --check` are read-only and must remain clean.
- English Q4 remains intentionally deferred until source curriculum is
  available and independently reviewed. This is a scope boundary, not a fake
  placeholder.
- Approval metadata and automated structural checks do not claim independent
  human curriculum sign-off; that review remains a separate responsibility.

## Data and architecture

- Content is bundled-only; there is no runtime content server or sync path.
- Room database schema is **v8** with additive migration coverage, including
  wildlife expedition data. Never delete shipped schema JSONs or lower the
  database version; future changes require v9+ migrations.
- The project currently contains 19 Gradle modules: app, five core modules,
  six feature modules, four engine modules, and three native reward-game modules.
- The network module is a retained placeholder and is not used to fetch data.

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

## Security and privacy posture

- The release manifest has no `android.permission.INTERNET` permission.
- App backups/data extraction are disabled; child data stays on-device.
- No runtime content or telemetry download is part of the release design.
- Mini-game HTML must contain the required restrictive CSP. The Gradle gate
  enforces 29/29 pages, CSP directives, no active external URLs, and no browser
  network APIs.
- Mini-game WebViews disallow file access, mixed content, multiple windows, and
  top-level navigation outside the virtual local origin.
- SVG preview WebViews reject HTTP(S) resource requests.
- Target SDK 35 edge-to-edge is enabled explicitly; parent-auth content applies
  safe system-bar insets and IME handling.

## Release gates

Latest local verification on 2026-08-07 (see `docs/release-review-2026-08-07.md`):

- Gradle `check assembleRelease`, lint, content audits, and 89+ Python tooling
  tests passed.
- API 35 emulator: **26/26 app connected tests** (incl. the new Sequence CTA
  contract tests) and 4/4 auth connected tests passed.
- Content pack validation: 358 lessons, 0 errors, 0 warnings.
- Release APK inspected: `0.22.0` (code 23), minSdk 26 / target 35, no
  `INTERNET` permission, 363 SVG assets, release signature present.
- Fresh-install walkthrough on a clean API 35 emulator reached PIN setup,
  opened the real IME, and confirmed Digit 0 / Delete / Set PIN stay above the
  keyboard (#64).

Before tagging the next release (`0.23.0` pending review):

1. Run `./gradlew check assembleRelease` with the release signing properties.
2. Run the content tooling checks from the Quick verification section.
3. Inspect the final APK with `apkanalyzer`/`aapt`:
   - package `com.maxinesworld.app`;
   - version `0.23.0`, code `24` (after the version bump commit);
   - no INTERNET permission;
   - release signature present;
   - minification enabled.
4. Install the exact APK on a fresh API 35 emulator and walk through parent
   PIN setup, child creation/selection, Playroom, lesson launch, reward break,
   and back navigation.
5. Run connected Android tests on the target emulator.
6. Review `git diff --check`, require a clean tree, then tag and push only the
   committed release source and exact APK.

## Known non-blocking scope boundaries

- English Q4 is deferred as documented above.
- Coins are displayed honestly, but a cosmetic coin-spend surface is future
  work; no fake purchase flow is exposed.
- The 357 revised month-01 SVGs ship without `<title>`/`<desc>` accessibility
  metadata (flagged to the editorial pipeline; no runtime impact today).
- Independent human educator review remains valuable even though the release
  metadata gate is green.
- The app has not yet been exercised on a physical device with a real child
  session; emulator coverage is complete but this remains the final product
  validation.

Historical implementation notes remain available through git history; this
file intentionally describes only the current release candidate so it does not
turn into a museum exhibit with a shell prompt.
