# Maxine's World — Current State & Release Handoff

**Document baseline:** 2026-08-06
**Release candidate:** `0.22.0` (`versionCode = 23`)
**Working branch:** `feat/lesson-concept-visuals`
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
```

`check` includes the educator-content gate and the offline mini-game gate.
Release signing is read from the user-only file
`~/.gradle/maxines-world-signing.properties`; no signing secret belongs in git.

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
  `releaseStatus=RELEASED`; `:app:verifyPlayableContent` enforces this metadata.
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

Before tagging `v0.22.0`:

1. Run `./gradlew check assembleRelease` with the release signing properties.
2. Run the content tooling checks from the Quick verification section.
3. Inspect the final APK with `apkanalyzer`/`aapt`:
   - package `com.maxinesworld.app`;
   - version `0.22.0`, code `23`;
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
- Independent human educator review remains valuable even though the release
  metadata gate is green.

Historical implementation notes remain available through git history; this
file intentionally describes only the current release candidate so it does not
turn into a museum exhibit with a shell prompt.
