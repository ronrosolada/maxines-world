# Changelog

All notable changes to Maxine's World. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the
Android `versionName`.

## [Unreleased]

### Content & visuals
- Integrated the revised editorial visual set: 358 month-01 lesson visuals plus 5 new Grade 3 pilot visuals (363 assets, checksum-verified).
- Kept the pre-revision `english-g3-q1-w01-d01` visual: the revised art dropped curriculum clues (red flag, parade, lanterns) required by the picture-detective activity.
- Regenerated child-facing feedback copy across 161 lessons so authored correct/explanation text no longer exposes curriculum jargon.

### Child-facing feedback
- Extended copy sanitization to success feedback and assessment explanations (core model, lesson player, and content tooling share the same replacement table).
- Applied the feedback guard to Interactive Spec and Multiple Choice guidance copy.

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
