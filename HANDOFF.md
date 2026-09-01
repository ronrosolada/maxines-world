# Maxine's World, Current State & Release Handoff

**Document baseline:** 2026-08-21
**Repository:** `ronrosolada/maxines-world` (public)

Maxine's World is a video-first Android learning app. The child experience is
built from three surfaces: a subject-organized **video library**, the
**Assessment Arena** quiz packs, and **reward mini-games**. This handoff
describes only the current release candidate.

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
```

From the repository root, the video and Assessment Arena manifests are validated
with the Python tooling:

```bash
python3 android/tools/validate_video_checkpoints.py
python3 android/tools/validate_skill_graph.py
python3 android/tools/validate_catalog_parity.py
python3 android/tools/audit_media_assessment_uniqueness.py
python3 android/tools/validate_arena_packs.py
PYTHONPATH=android:android/tools python3 -m unittest discover -s android/tools -p 'test_*.py'
```

`check` includes the offline mini-game isolation gate. Release signing is read
from the user-only file `~/.gradle/maxines-world-signing.properties`; no signing
secret belongs in git.

## Current product surface

### Child experience

- **Playroom** is the canonical child home.
- The home presents six learning areas: English, Mathematics, Filipino, Science,
  Makabansa, and GMRC.
- The Daily Quest is the single explicit start action; subject cards remain
  available as direct navigation.
- Milo's sanctuary renders as a living meadow scene with deterministic piece
  placement (`SanctuaryScene.kt`, pure tested model).
- Milo the cat is the single child-facing companion guide across every subject
  and reward surface (real artwork). The wider character cast from early design
  (Mira, Niko, Lakan, Duke, etc.) is not implemented and ships no artwork.
- Progress, stars, coins, wildlife stickers, and reward-break entitlements are
  persisted locally and use idempotent reward keys.

### Video lessons

- The LAN catalog still holds 237 workbook-selected Grade 1–4 videos.
  Child-facing surfaces (library, daily quests, home progress, prefetch) show
  only **Grade 3 `RELEASED`** videos (95 as of the current catalog). Grade 1,
  Grade 2, Grade 4, and `PREVIEW` rows stay in the catalog for later review
  and are not presented as core curriculum.
- Subject totals in the full catalog are Filipino 100, Makabansa 51,
  Mathematics 24, English 22, GMRC 20, and Science 20; the app filters by
  subject and orders by episode.
- Each video has five subject-appropriate memory-check questions; 4/5 is the
  pass threshold and playback completion gates the check.
- Every catalog row is `PERSONAL_USE`. That is a legal fact, not a product
  toggle: these videos are household LAN-only media and are not a public
  content license. Do not rewrite `licenseStatus` to imply redistribution
  rights.
- See `docs/video-playlist-replacement-2026-08-20.md` and
  `android/docs/optional-video-media.md` for deployment, hashes, rollback,
  and verification evidence.

### Assessment Arena

- Multi-curriculum quiz packs live in
  `android/app/src/main/assets/assessment-packs/` (Philippine DepEd, Singapore
  MOE, US NGSS/CCSS).
- The packs are bundled and available offline; the Arena is the offline-first
  learning surface when LAN video media is not reachable.
- `android/tools/validate_arena_packs.py` checks pack shape, answer-key
  integrity, and answer-position distribution.

### Reward breaks

- Completing Today's mission grants **one 5-minute play break**. Remaining
  time can resume; consume/expiry does not re-arm a fresh window the same day.
  Home copy says "one 5-minute play break". After the break is consumed, the
  quest card returns to Open Sanctuary.
- The Playroom reward library bundles HTML mini-games plus the native reward
  games, but the child shelf is an 8-year-old allowlist (`MiniGameShelf.kt`).
  Wordle, Sudoku, Solitaire, FreeCell, Checkers, Reversi, Mancala, Yahtzee, and
  Domino stay bundled but are hidden from the child route.
- The library exposes categories and clear entitlement/empty states rather than
  pretending that a break is available.
- Games are bounded by the reward-break policy and contain no ads, analytics,
  runtime downloads, or network APIs.
- Attribution and provenance are in `android/app/src/main/assets/mini-games/`.

## Content

- **Video-First Hub (Active):** High-definition video lessons streamed/cached
  from local Caddy (`10.10.10.33` / `10.10.20.33`). Watch verification gates
  reward stickers.
- **Media Assessments (Active):** Defined in
  `android/app/src/main/assets/content-pack/media-assessments.json` for
  post-watch comprehension checks; validated for uniqueness and video-grounding.
- **Assessment Arena (Active):** Multi-curriculum quiz packs in
  `android/app/src/main/assets/assessment-packs/` (Philippine DepEd, Singapore
  MOE, US NGSS/CCSS).

## Assessment policy

- A video memory check **passes at 4/5 (80%)**, matching the accuracy tiers used
  for stars. A malformed (zero-item) assessment fails closed; the player has no
  silent default.
- **Only the authored assessment contributes to accuracy and mastery.** Passive
  video watching and practice interactions cannot inflate the mastery signal
  (`Scorer.evaluateAssessment` consumes only scored results).
- **First-attempt passes are recorded distinctly** from post-retry passes:
  `lesson_completions.passedOnFirstAttempt` (DB v9).
- **Fail-closed rendering**: a payload the validators would reject is dropped
  with a log rather than silently rendered.

## Data and architecture

- Content is bundled-only for the Assessment Arena; optional video media is
  fetched from the trusted home LAN when configured. There is no cloud content
  server, sync path, or telemetry endpoint.
- Room database schema is **v9** with additive migration coverage, including
  wildlife expedition data and `passedOnFirstAttempt`. Never delete shipped
  schema JSONs or lower the database version; future changes require v10+
  migrations.
- The project currently contains 19 Gradle modules: app, five core modules,
  six feature modules, four engine modules, and three native reward-game modules.
- Pre-Playroom village screens (`VillageHomeScreen`, `VillageHomeV17`,
  `VillageChromeV16`) and their assets were removed in 0.31.0; the Playroom is
  the only child home.

## Security and privacy posture

- The release manifest intentionally includes `android.permission.INTERNET`
  for optional media served by the trusted home LAN over HTTPS. Cleartext HTTP
  is not permitted; the homelab CA is bundled as a trust anchor.
- App backups/data extraction are disabled; child data stays on-device.
- No cloud content sync or telemetry download is part of the release design.
- Media downloads are verified end-to-end: catalog paths are validated
  (`media/` prefix, no traversal), downloads are size-capped, SHA-256 checked,
  and promoted atomically.
- Mini-game HTML must contain the required restrictive CSP. The Gradle gate
  enforces CSP directives, no active external URLs, and no browser network APIs.
- Mini-game WebViews disallow file access, mixed content, multiple windows, and
  top-level navigation outside the virtual local origin.
- Target SDK 35 edge-to-edge is enabled explicitly; parent-auth content applies
  safe system-bar insets and IME handling.

## Release gates

- CI runs on every push and PR: content integrity (`:core-content` unit tests),
  video schema + catalog validation, Python tooling tests, workflow lint, and
  assemble + lint + unit tests.
- **Connected tests (API 34 emulator) gate six modules**: `core-database`,
  `app`, `feature-auth`, `feature-child-home`, `feature-rewards`, and
  `feature-lesson-player`.
- `release-gate` runs on `v*` tags: the release must assemble before a tag is
  considered shippable.

Before tagging a release:

1. Run `./gradlew check assembleRelease` with the release signing properties.
2. Run the video/Arena tooling checks from the Quick verification section.
3. Inspect the final APK with `apkanalyzer`/`aapt`:
   - package `com.maxinesworld.app`;
   - intentional INTERNET permission for optional LAN media;
   - release signature present;
   - minification enabled.
4. Install the exact APK on a fresh API 35 emulator and walk through parent
   PIN setup, child creation/selection, Playroom, video playback, Assessment
   Arena, reward break, and back navigation.
5. Confirm CI is green on `main`, then tag and push the release.

## Known non-blocking scope boundaries

- Coins are displayed honestly, but a cosmetic coin-spend surface is future
  work; no fake purchase flow is exposed.
- Compose stays on the 1.7 line (BOM 2024.12.01): the 1.8 line changed IME
  inset propagation and broke the PIN keypad UI test under injected insets.
  Validate on a physical device before bumping.
- Child-facing video surfaces show Grade 3 `RELEASED` only; other grades remain
  `PREVIEW` in the LAN catalog. `PERSONAL_USE` is the household license, not a
  product toggle. Videos stay LAN-only.
- The app has not yet been exercised on a physical device with a real child
  session; emulator coverage is complete but this remains the final product
  validation.

Historical implementation notes remain available through git history; this
file intentionally describes only the current release candidate so it does not
turn into a museum exhibit with a shell prompt.
