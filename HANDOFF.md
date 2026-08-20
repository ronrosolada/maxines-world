# Maxine's World, Current State & Release Handoff

**Document baseline:** 2026-08-20
**Release candidate:** `0.56.0-6-g8647a8ae` (`versionCode = 292`)
**Working branch:** `feat/playlist-video-replacement`
**Repository:** `ronrosolada/maxines-world` (public)

## Release 0.56.0 playlist replacement (2026-08-20)

- Scope: replaced the optional video catalog with 237 workbook-selected Grade 1–4 videos across Filipino, Makabansa, Mathematics, English, GMRC, and Science.
- Subject totals: Filipino 100, Makabansa 51, Mathematics 24, English 22, GMRC 20, Science 20.
- Grade totals: Grade 1 29, Grade 2 53, Grade 3 95, Grade 4 60.
- Assessment policy: five subject-specific multiple-choice memory checks per video, 1,185 total items, 4/5 required, `claimsMastery=false`.
- Language policy: English for English/Mathematics/Science; Filipino for Filipino/Makabansa/GMRC.
- Media status: `PREVIEW` / `PERSONAL_USE`; MP4s and both catalogs are deployed to DreamNAS at `10.10.10.33`.
- Release APK SHA-256: `549e93357753c0570108984ec4ae5dca552b4d0982715f2db8a477203dc23795`.
- APK delivery: `http://10.10.10.33/app-release.apk`; prior APK retained as `app-release.apk.bak-playlist-20260820-121500`.
- Full implementation, validation, deployment, and rollback details: [`docs/video-playlist-replacement-2026-08-20.md`](docs/video-playlist-replacement-2026-08-20.md).
- The feature branch contains the media manifest, documentation, and emulator-test fixture updates; push it and open a PR before merging to `main`.

## Release 0.35.1 candidate (2026-08-15)

- Scope: fixed parent-access initialization, replaced the generic English visual-shell cluster, corrected confirmed content-integrity defects, and added regression coverage.
- Content QA: 358 lessons/files, 0 strict-validation errors/warnings, 0 quality findings, 0 similarity pairs, 0 pacing violations, and 358/358 valid/renderable lesson assets.
- Visual QA: 0 retired generic focus-board SVGs; the Fiesta Picture contains all eight required scene groups; 128 activities still use the optional SVG-only visual path without a `visualScene` JSON payload.
- Tooling QA: full Python discovery passed **120 tests**.
- Android QA: `verifyPlayableContent`, offline mini-game gate, app/feature unit tests, lint, debug assembly, signed release assembly, and the release verifier all passed.
- Release APK: `android/app/build/outputs/apk/release/app-release.apk`; `com.maxinesworld.app`, version `0.35.1`, code `38`, APK Signature Scheme v2 verified.
- Emulator smoke: clean install showed Create Child Profile rather than PIN setup; after profile creation the requested default PIN opened Parent Dashboard; zero fatal Android runtime exceptions.
- Review-gap QA: `docs/content-review-gap-analysis-2026-08-15.md` records why the previous structural review missed visual-topic semantics. The stronger audit still reports 56 repeated assessment-prompt groups and 180 overlong learner-facing strings; these remain explicit follow-up work.
- Signing remains workstation-only through `~/.gradle/maxines-world-signing.properties` (mode `600`); no signing values belong in Git.
- Remaining release steps: commit, push `main`, push the matching `v0.35.1` tag, and publish the GitHub release with the verified APK. CI may remain blocked by the previously recorded GitHub billing/spending-limit restriction.

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

- [#76](https://github.com/ronrosolada/maxines-world/issues/76), M1: 46 real
  objectives stretched over 142 Filipino/Makabansa files (pacing/scope)
- [#77](https://github.com/ronrosolada/maxines-world/issues/77), M2:
  production objectives never assessed (writing tasks missing)
- [#78](https://github.com/ronrosolada/maxines-world/issues/78), M7: retry
  feedback never says what went wrong (688 occurrences)
- [#79](https://github.com/ronrosolada/maxines-world/issues/79), 122
  same-keyed-answer pairs across EN/FIL/MATH/SCI (minor)

No `mark_lessons_reviewed.py` run was performed in this round; all 358 lessons
retain their prior metadata.

## Maxine (Child-Learner) & Live Emulator Content Review & Re-Authoring (2026-08-15)

Reviewed from the perspective of 8-year-old learner Maxine with live UI verification on the Android tablet emulator (`3048x2032` SwiftShader / API 35) and comprehensive static scans across all 6 main subjects (338 lessons).

### Re-Authoring Pass Completed (2026-08-15)
All critical blockers, single-letter casing traps, circular definitions, and cross-contaminations were re-authored across 65 lesson files:
1. **P0 Blockers Resolved:**
   - `english-g3-q3-w10-d03.json`: Purged contaminated paper-boat assessment and re-authored with complete, aligned fruit preparation sequencing questions and matching activities.
   - `mathematics-g3-q3-w07-d04.json`: Corrected pattern activity key (`correctIndex: 1`), fixed sequence prompt rule to *"add 3 each time"*, and enriched pattern explanations.
2. **P1 Concept & Language Bleed Fixes:**
   - Phonics (`english-g3-q1-w03-d03.json`): Updated blend explanations to clarify that both consonant sounds remain audible.
   - Fraction Models template contamination (`mathematics-g3-q4-w09-d02.json`, `d03.json`): Replaced template artifact strings with clear *"greater than one whole"* explanations.
   - Language Bleed (`english-g3-q2-w06-d03`, `english-g3-q3-w13-d01/d02`): Replaced *"salaysay"* with *"personal story"* in English narration.
3. **Casing & Punctuation Distractor Redesign:**
   - Re-authored multiple-choice questions across `english-g3-m01-d05`, `english-g3-q1-w01-d05`, `english-g3-q2-w04-d02/d04`, `english-g3-q3-w11-d01/d03`, `english-g3-q3-w12-d01`, `filipino-g3-m01-d05/d07/d13`, `filipino-g3-q1-w05-d01`, and `filipino-g3-q1-w07-d01` with distinct, plausible, full contextual sentences.
4. **Vocabulary & Activity Enrichment:**
   - Math geometry vocabulary (`point`, `line segment`, `ray`, `line`) in `mathematics-g3-q1-w01-d01` to `d04` expanded with kid-friendly concrete definitions.
   - Science Optics unit (`science-g3-q3-w05` to `science-g3-q4-w09`) and Waste lesson (`m01-d14`) enriched with distinctive terms (*refraction, prism, spectrum, opaque, transparent, translucent, glare*).
   - Filipino *Simuno* cluster (32 lessons) diversified across multiple grammar facets (*pandiwa*, *pang-uri*, *karaniwang ayos*, *di-karaniwang ayos*).
   - Science giveaway sorting/matching cards in `science-g3-m01-d01` updated to observation-based descriptions.

### Graphics Asset Overhaul, Emoji Eradication & Scene Uniqueness (2026-08-15)

A fleet-wide graphics pass was completed against the craft-floor standards (no emoji-as-icons, consistent stroke/ink, real depth, unique seeded scenes):

1. **Duplicate-scene eradication:** 15 normalized-identical SVG groups (71 files, legacy 640×360 board templates) regenerated with the seeded `scene_svg` (800×450, lesson-ID-seeded motifs/layout) → **0 remaining normalized-duplicate groups**.
2. **Emoji-as-icon eradication:** 287 of 358 SVGs carried raw emoji glyphs as `<text>` icons. Generator-owned files were regenerated with drawn vector motifs, and the ten hand-authored English Q1 W01–W04 hotspot assets were replaced with lesson-specific drawn scenes rather than retaining the generic focus-board shell.
3. **Canonical a11y metadata:** `scene_svg` patched to emit byte-identical metadata to `add_svg_accessibility.accessible_svg` (unique `svg-title-*`/`svg-desc-*` IDs, objective-rich descriptions), the idempotency test that this surfaced is now green.
4. **Verification:** `test_svg_accessibility.py` 4/4 green; `test_generate_quarterly_assets.py` 3/3 green; `verify_lesson_assets.py` 0 missing/malformed/orphaned; **0 emoji glyphs and 0 duplicate visual layouts across all 358 SVGs**.

### Verification Status
- `content_pack_validation.py --strict`: **0 errors, 0 warnings** (358 lessons)
- `content_quality_audit.py --check`: **0 findings**
- `dedupe_lesson_titles.py --check`: **0 duplicate groups**
- `assessment_duplicate_gate.py`: **0 duplicate groups**
- `objective_pacing_audit.py --check`: **0 over-fan-out**
- `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL (all unit test suites green)**
- Follow-up `educational_material_audit.py` is intentionally tracked separately: it currently reports 56 repeated assessment-prompt groups and 180 overlong learner-facing strings. These are not claimed as zero in the corrective release.

## Current product surface

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

- 237 workbook-selected Grade 1–4 videos are available through optional LAN media.
- Subject totals are Filipino 100, Makabansa 51, Mathematics 24, English 22,
  GMRC 20, and Science 20; the app filters by subject and orders by episode.
- Each video has five subject-appropriate memory-check questions; 4/5 is the
  pass threshold and playback completion gates the check.
- The media catalog is `PREVIEW` / `PERSONAL_USE`; it is not a public content
  release and must remain LAN-only until HTTPS and licensing are reviewed.
- See `docs/video-playlist-replacement-2026-08-20.md` for deployment, hashes,
  rollback, and verification evidence.

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
- The former generic English hotspot-board exception is retired. The ten English
  Q1 W01–W04 hotspot assets now use lesson-specific visuals; W01D01 explicitly
  contains the red flag, dancing people, food table, laughing children, band,
  streamers, parade, and lanterns required by its picture-detective activity.
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
  `VillageChromeV16`) and their assets were removed in 0.31.0, the Playroom is
  the only child home.

## Assessment policy

- A lesson **passes when at least 80% of assessment items are correct on
  first scoring** (4/5 on the standard five-item check; `0.8` matches the
  accuracy tiers used for stars). Enforced two ways: `content_pack_validation.py`
  errors on any lesson whose `passingCorrectCount / itemCount < 0.8`, and the
  player has no silent default, a malformed (zero-item) assessment fails
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

## Educator effectiveness pass & final validation (2026-08-15)

MoA completed the corpus-wide educator effectiveness review and in-place re-authoring.
Every lesson has exactly one final disposition; no lesson is blocked:

- **358/358 lessons dispositioned**, 358 FIXED, 0 OK, 0 BLOCKED.
- Subject totals: Mathematics 58, English 93, Filipino 92, Science 45,
  Makabansa 26, GMRC 24, Araling Panlipunan 20.
- Durable per-lesson report: `docs/educator-effectiveness-review-2026-08-15.md`.
- Protected lesson metadata, activity enums, asset references, and answer-key
  integrity were preserved by the MoA lanes and verified after completion.

Final validation after the last content edit:

- Strict content-pack validation: **358 lessons, 0 errors, 0 warnings**.
- Content quality audit: **0 findings**.
- Assessment duplicate gate: **0 duplicate groups**.
- Content similarity gate: **0 near-duplicate pairs** at threshold 0.85.
- Objective pacing: **0 over-fan-out objectives**, 0 missing objectives.
- Lesson assets: **358/358 SVGs**, no missing/orphan/malformed/render-failed assets.
- SVG accessibility generator check: **0 pending updates**; accessibility tests 4/4.
- Android `:app:testDebugUnitTest`: **BUILD SUCCESSFUL**.
- Android `:app:assembleDebug`: **BUILD SUCCESSFUL**.

During final validation, ten narrow mechanical content repairs were made (four
Science HOTSPOT payload shape fixes, one Mathematics duplicate-prompt repair,
and five Filipino answer/source-alignment fixes). All were revalidated by the
same gates above. Historical Python repair fixtures still contain legacy
exact-text/format assumptions for some scenario-based prompts; they are not
release gates and do not reflect pack/schema failures.

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
