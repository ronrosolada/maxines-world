# Changelog

All notable changes to Maxine's World. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the
Android `versionName`.

## [Unreleased]

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
