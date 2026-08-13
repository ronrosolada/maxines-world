# Changelog

All notable changes to Maxine's World. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the
Android `versionName`.

## [Unreleased]

## [0.31.1] - 2026-08-13

### Fix

- **Sanctuary complete state**: the earn hint ("Finish all 3 lessons in
  Today's Quest to add this place.") no longer renders once the sanctuary is
  complete (12/12 places, including parent god mode), where it contradicted
  the "Milo's home is complete!" line directly above it. UI test added
  (`completeSanctuaryShowsCompletionWithoutAnEarnHint`).

## [0.31.0] - 2026-08-13

### Quality, hygiene, and trust

- **Fail-closed rendering**: unknown activity types are dropped with a log
  instead of silently rendering as an animated explanation; a lesson with no
  playable steps fails to load instead of opening an empty player. Unit
  tests cover both behaviors.
- **CI connected-test coverage**: `feature-child-home`, `feature-rewards`,
  and `feature-lesson-player` instrumented suites now run on the API 34
  emulator (previously only `core-database`, `app`, and `feature-auth`).
- **Dead code removed**: pre-Playroom screens (`VillageHomeScreen`,
  `VillageHomeV17`, `VillageChromeV16`) and their unreferenced assets
  deleted. Mascot + subject artwork converted PNG → WebP (≈9.7 MB smaller
  APK).
- **Content normalization**: 9 Filipino story lessons re-serialized with the
  canonical top-level key order (values unchanged, gate verified).
- **Dependency refresh**: AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, KSP
  2.1.20-1.0.32, Room 2.7.1. Room 2.7's stricter SQL parser required
  renaming the `lesson_completions` query alias from the reserved word
  `current`. Compose BOM stays on 2024.12.01: the 1.8 line changes IME
  inset propagation, which broke the PIN keypad UI test (`AuthImeLayoutTest`)
  under injected insets — a Compose bump must be validated on a physical
  device first.
- **Educator follow-ups tracked**: M1, M2, M7, and the 122 same-keyed-pair
  finding are now GitHub issues (#76–#79).
- **Docs**: HANDOFF.md reconciled to the current baseline (DB v9, SVG
  accessibility status, issue links).

## [0.30.0] - 2026-08-11

### Playroom — one clear starting point

- Removed the redundant homepage `Start here!` subject badge and the
  duplicate `Start your first adventure` resume card.
- The Daily Quest now owns the single explicit start action (`Start quest`;
  `Continue quest` after progress), reducing first-screen choice overload for
  children. Subject cards remain available as direct navigation.

## [0.29.0] - 2026-08-11

### Playroom — Milo's sanctuary becomes a place you can see

- The home sanctuary card now renders a living meadow scene: every earned
  sanctuary piece appears as a colored spot in the scene, the next piece
  previews in a white outline slot, and Milo (real character art) stands
  guard in front of the meadow.
- Scene placement is a pure, tested model (`SanctuaryScene.kt`): all 12
  catalog pieces have deterministic positions, so the daily-quest payoff is
  visible progress instead of a progress bar.

### Lesson player — real character avatars

- `CharacterGuide` now shows the actual character artwork (Milo, Mira, Niko,
  Lakan, Duke) in a circular avatar instead of a colored initial.

### Reward breaks — kid-first game shelf

- The mini-game library is curated: "Friendly favorites" (memory match,
  whack-a-mole, match-three, piano tiles, snake, stack, breakout, flappy
  bird, color connect, bolt sort, number merge, color block) lead the shelf;
  the 17 classics (wordle, solitaire, sudoku, checkers, …) stay available
  further down. Ordering is a pure tested function (`MiniGameShelf.kt`).

### Video Lessons

- Tagalog video library renamed "Video Lessons" for future multi-subject
  support; 8 full-length Tagalog videos (19–26) with 10-question memory
  checks (8/10 passing, `claimsMastery: false`) added to the catalog.

## [0.28.0] - 2026-08-10

### Playroom home — adversarial UX on a fresh install (8yo)

- Today's Quest no longer looks duplicated on day-1: colliding titles in the
  3-target set now carry a lightweight `· Day N` distinguisher
  (`questTargetDisambiguator` off `lessonId`, via `ModuleCatalog`), so three
  sequential "Word Roots" lessons read as `Word Roots · Day 1/2/3` instead of
  three identical rows.

### Lesson player — hotspot discoverability

- `ALL_TARGETS_VISITED` boards now show `N of M explored` under the question so
  an 8-year-old knows the goal is to visit every example.
- Remaining tappable badges pulse gently (8%, `!animationsDisabled`), visited
  badges stay still — draws the eye without competing with lesson art.

### Verification

- 358/358 educator-reviewed, 29/29 mini-games CSP/offline-clean.
- `testDebugUnitTest` (450 tasks) and `assembleDebug` green; `verifyPlayableContent`
  `Release gate OK`.

## [0.27.0] - 2026-08-10

### Playroom home

- Refined the child home with transparent mascot and subject artwork assets.
- Removed the decorative profile/avatar control from the header.
- Made Milo's Wildlife Sanctuary a next-reward surface with progress and the
  next unlock, while keeping Wildlife Stickers as the collection entry point.
- Added regression coverage for the sanctuary/sticker distinction and the
  updated child-facing copy.

### Media and reward breaks

- Added the optional LAN-hosted media catalog/download path. Bundled lessons and
  bundled reward-break games remain available without network access.
- Consolidated the mini-game library and preserved reward-break entitlement
  lifecycle behavior.

### Release verification

- Educator gate: 358/358 playable lessons approved and released.
- Offline mini-game gate: 29/29 pages validated.
- Connected Android tests passed for child home and reward flows; full JVM
  unit-test suite passed locally.
- Release APK built and verified with APK Signature Scheme v2.

The `INTERNET` permission is intentional for optional media served from the
trusted home LAN; it is not used for telemetry or cloud content sync.

## [0.25.0] - 2026-08-08

### Highlights

- Added the unified Playroom home experience with separate sanctuary next-reward progress and Wildlife Sticker Book collection surfaces.
- Added transparent mascot and subject artwork, animal-photo badge details, and child-facing home-screen polish.
- Added reversible, PIN-gated parent god mode backed by DataStore for development and QA reward-flow verification.
- Added the offline mini-game library and reward-break flow refinements, including safer entitlement and badge-award handling.
- Added optional LAN-hosted media support for the approved home-network deployment.

### Verification

- 358/358 educator-reviewed lessons and 29/29 offline mini-games pass release gates.
- Root JVM tests, connected child-home/rewards tests, signed release assembly, and APK v2 signature verification pass locally.

### Quest

- Today's Quest now shows its 3 deterministic lesson targets (subject + friendly title, ✓/○ completion, accessible) so Maxine sees what counts toward the badge.
- Quest CTA routes to the next incomplete target (`lesson_player/{questLessonId}` — Start/Continue quest) instead of a placeholder.
- Covers the 100% bug where finishing a non-quest lesson made the badge look stuck.

### Child-facing polish

- Sort & Classify: scroll hint when cards exceed the fold; MCQ now nudges on empty Submit (`Pick one answer to check`).
- Place-value fix: `mathematics-g3-m01-d01` corrections rebuilt around the teaching example (`hundreds` / `hundreds place` line preserved, `ten thousands` → `ten thousand`).

### Content

- Replaced generic activity feedback (`Look at the example again` / `Great thinking! You found the key idea`) with topic-grounded, language-aware lines (e.g. `Find the clue in Picture Detective again`, `Great! You found the examples for …`) — 347 lessons.
- Fixed `english-g3-q3-w11-d03` (Sentence Polishing) — 5 title-substituted assessment prompts replaced with real capitalization/punctuation checks.
- Language hygiene: `en-PH` lessons now carry English feedback only (`Mahusay` 0 in `en-PH`).

### Tooling

- `content_review.py`: topic-grounded `make_activities` feedback table, generic-key replacement guard (`_GENERIC_FEEDBACK_KEYS`), language-aware sanitizers.
- Gates: `content_review --include-legacy --dry-run` 0/358/0, `content_quality_audit --check` 0 findings, `content_similarity_gate` 0 @0.85, `content_pack_validation --strict` 358/358 0/0, tooling 91 tests (358 subtests), `core-content` + app unit tests, `verifyPlayableContent` 358 educator-reviewed.

## [0.24.0] - 2026-08-08

### Changed (child-development content review, all 358 lessons)

- Re-authored learner-facing content in 196 lesson files against the product
  goal (factual, age-appropriate, learning-first, encouraging, fun for an
  8-year-old): concrete kid questions replace objective-pasted assessment
  stems; abstract distractors replaced with plausible options; reasoning-based
  explanations replace "uses what we learned" phrasing; topic-specific hooks
  and activity instructions; encouraging feedback rotation replaces
  "You found the key idea" placeholders.
- Fixed scrambled assessment keys (english q3-w13-d03/d04), ambiguous keys
  (science living/non-living), clone assessments (math q1-w01-d01/d04), and
  Filipino simuno stem-leak items (32 lessons regenerated).
- Localized AP titles to fil-PH; simplified Filipino instruction verbs.
- Tooling hardened so generators/sanitizers cannot reintroduce the patterns:
  `repair_educator_findings.py`, `content_review.py`, `Models.kt`
  (FeedbackSanitizationTest, ActivityStepConversionTest), and
  `repair_filipino_content.py` (idempotency restored, 14/14 tool tests).
- Metadata untouched: 358/358 lessons educator-reviewed and RELEASED.

## [0.23.0] - 2026-08-08

### Content & visuals
- Completed the educator-review re-author round (2026-08-07 r2 completion): C4/C5/C6/C8 + M3/M8/M9/M10 + m1–m6, all seven subjects now Approvable.
- GMRC: per-value instructions + sequence steps across all 24 files; all assessments rebuilt as scenario-based judgment items with why-explanations (35 unique transfer scenarios authored).
- Makabansa: authored real community history for q1-w01-d01/02/03 (Barangay Sapa, founded 1955; school 1960; market 1970); all assessments rebuilt as judgment items.
- Filipino m01: 4 stock title-topic checks per lesson replaced with transfer items (80 authored); AP m01: stock "kasanayang ito" assessments replaced with concept-transfer items (100 authored).
- M3 sweep: 812 "kasanayan" jargon instances replaced with concept labels (simuno at panaguri, salitang-ugat, …) across 56 Filipino files; learner-facing "kasanayan" now 0 pack-wide.
- Fixed Taglish/legacy artifacts: `mag-shoot`→`maglaro ng basketbol`, `I-grupo`→`Ipangkat`, MKB doesNotFit culture swaps, 7 wastong-pagsulat word↔meaning matchings, circular vocab definitions.
- English m01-d13 Q5 re-keyed (ship = digraph, not blend); duplicate assessment prompts eliminated pack-wide (0).
- Integrated the revised editorial visual set: 358 month-01 lesson visuals (checksum-verified).
- Kept the pre-revision `english-g3-q1-w01-d01` visual: the revised art dropped curriculum clues (red flag, parade, lanterns) required by the picture-detective activity.
- Regenerated child-facing feedback copy across 161 lessons so authored correct/explanation text no longer exposes curriculum jargon.
- Removed all unreviewed lesson content from the APK (legacy `content/ph-matatag` fallback + pilot pack) and extended the release gate to cover every lesson-bearing asset directory (external review C3).
- Pinned the assessment pass policy at 80% (4/5), removed the silent runtime default, and enforced it in content validation (CH-03).
- Excluded practice activities from accuracy and mastery (`ActivityStep.scored` contract), recorded first-attempt vs retry passes distinctly (DB v9), and removed the legacy loaders/active-content path so only `RELEASED` bundled lessons can load (CH-04, CH-02).

### Child-facing feedback
- Extended copy sanitization to success feedback and assessment explanations (core model, lesson player, and content tooling share the same replacement table).
- Applied the feedback guard to Interactive Spec and Multiple Choice guidance copy.

### Educator review round 2 (2026-08-07)
- Re-keyed 10 Science "Material Detectives" lessons: anchor objects were marked correct for
  flexible/absorbs/hard all at once; keys now point to the true property holders, with
  property-based explanations (findings in `docs/educator-content-review-2026-08-07-r2.md`).
- Fixed 6 inverted English odd-one-out keys, rebuilt the 2 broken sequencing assessments,
  replaced 25 placeholder keyed answers and 21 placeholder vocabulary entries with real content,
  rebuilt the mangrove informational-text lesson's assessment, and de-duplicated the
  `q2-w04-d02`/`q3-w11-d03` clone.
- Purged generator jargon pack-wide: "Subukan ang kasanayan sa…" → teaching lead-ins (138×),
  "shows the skill"/"the lesson skill"/"Try the skill" (0 remaining), "evidence from the
  example" labels (26×), 84 "evidence of this skill: <objective>" meta-prompts → skill
  questions, "number skills"/"about reading and writing" → true domain labels.
- Translated Araling Panlipunan English bleed to Filipino (223+ strings) and fixed the
  "kasanayang kasanayan" doubling artifact (55×) plus 15 circular GMRC vocabulary definitions.
- Fixed 2 pre-existing unit-test drifts found during verification: `StreakTest` positional
  args vs the v9 `LessonCompletionEntity` constructor, and the stale ModuleStructureTest
  assertion on the removed ph-matatag manifest tree.

### Testing
- Added UI-level regression coverage proving the Sequence CTA is a no-op until complete and submits the authored order as correct (#65).

## [0.22.0] - 2026-08-06

### Features
- Added 29 fully bundled, offline reward-break mini-games with child-safe WebView isolation.
- Added the Playroom mini-game library with categories and honest entitlement states.
- Preserved an earned reward break when a child leaves before choosing a game.
- Added an offline PIN-reset explanation and a retry path for transient lesson-load errors.
- Removed the last unused external repository URL from the production catalog.
- Added answer-neutral lesson concept visuals so illustrations support learning without revealing answers.

### Privacy, accessibility & reliability
- Kept the release APK offline-only: no `INTERNET` permission and no runtime content downloads.
- Enforced CSP, page count, external-URL, and browser-network-API checks for every bundled mini-game.
- Explicit target-SDK-35 edge-to-edge configuration with safe system-bar insets in parent authentication.
- Hardened SVG and mini-game WebViews against HTTP(S), mixed-content, and unsafe file-origin loading.
- Fixed PIN setup keyboard overlap and added IME regression coverage.

### Release gates
- All 358 bundled lessons carry the required educator approval metadata.
- Release verification now runs both educator-content and offline-mini-game gates.

## [0.21.1] - 2026-08-06

### Lesson reliability
- Sort & Classify prompts now use the same vocabulary as the visible answer buckets.
- Retrying Sort & Classify preserves correctly placed cards instead of forcing a full restart.
- Added regression coverage for both learner-facing behaviors.

## [0.21.0] - 2026-08-06

### Content
- Filipino Q1 W04–W08: nine new Grade 3 lessons + SVG artwork (PR #55) — pack now 358 lessons
- Educator review pass across all lessons (placeholders removed, unsafe distractors replaced, answer-position bias fixed, similarity gate zeroed)
- Quarterly lesson visuals regenerated as topic-specific scenes (800×450 / 1200×675)
- Repair tooling made idempotent and check-mode read-only (no more working-tree churn)

### Features
- Lesson visuals rendered in every activity type (bespoke SVG scenes)
- Transaction-safe lesson completion with assessment gate and no stale badge on replay
- Daily quests wired into the Playroom (with honest fallback states)
- Child-visible star/coin balances, reward spend surface, reward-break entitlements
- AP merged into Makabansa (Matatag successor) — six-card Playroom grid
- Persistent Read Along narration controls; Kitten Match reward break
- First Steps sticker; recommended starting module; lesson resume state

### Accessibility & reliability
- Genuine OFL fonts (Baloo 2 / Nunito) — fresh installs no longer crash on text input
- TTS locale matched by lesson language (Filipino narrated with a Filipino voice)
- Contrast, reduced motion, lockout, and TalkBack passes on the Playroom home
- Double-tap navigation guard, single TalkBack progress announcement, 14sp+ labels
- Brute-force protection for the parent PIN; day-streak fix (local timezone)
- DB v8 (wildlife_expeditions) with corruption quarantine + destructive-migration fallback; single parent account row
- Content-integrity test now tracks the real pack size and cannot be satisfied by stale build cache

### Tooling & CI
- Content quality gates (schema, semantic audit, educator metadata) enforced in CI
- Educator review gate wired into `check` and `assembleRelease` — a release can never ship unreviewed lessons
- Migration tests on emulator in CI; content tooling suite (81 tests) green

## [0.20.0] - 2026-08-01

- Playroom home (six subject collections) shipped as the main experience
- DB v7 with v4/v6 lineage migrations
- Pre-Playroom PRs #1–#6 retired

## [0.19.0] - 2026-07

- (Previous development baseline; see git history for details)
