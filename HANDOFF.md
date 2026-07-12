# Maxine's World — Current State & Handoff

**Date:** 2026-07-13 06:45 UTC+8
**Commit:** `3f7bb81` (main)
**Release:** [v0.10.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.10.0)

## Quick Start (for a new Hermes agent)

```bash
git clone https://github.com/ronrosolada/maxines-world.git C:\maxines-world
cd C:\maxines-world\android
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
gradlew assembleDebug
```

**Critical:** Path must be `C:\maxines-world` (no spaces — KSP/Room limitation).

## Build Status

| Check | Result |
|---|---|
| `assembleDebug` | ✅ PASS — 604 tasks |
| `testDebugUnitTest` (feature-rewards) | ✅ PASS — 17 badge tests |
| `testDebugUnitTest` (core-content) | ✅ PASS — 6 verifier tests |
| `testDebugUnitTest` (engine-sync) | ⚠️ Some tests need fixing |
| Lint | ⚠️ NOT RUN |

## Architecture (19 modules)

```
app/                         Main app module, nav graph, Hilt DI
core-model/                  Domain models, JSON serialization
core-content/                LessonLoader, ContentLessonLoader, ContentVerifier
core-database/               Room DB v3, 11 entities, migrations 1→2→3
core-design-system/          Theme.kt, Color.kt, MaxinesComponents
core-network/                ApiClient (stub)
engine-activity/             7 activity renderers (V1 capabilities)
engine-mastery/              Mastery engine
engine-sync/                 ContentSyncWorker, catalog sync
feature-auth/                Parent PIN auth (SHA-256 + per-install salt)
feature-child-home/          Village home, landscape, DailyQuest
feature-lesson-player/       Lesson player, step dots, activity steps
feature-parent/              Parent dashboard, content management
feature-rewards/             BadgeAwarder, WildlifeFieldGuide, badge system
game-cat-cafe/               Mini-game (Cat Café Dash)
game-pawprint-parkour/       Mini-game (Pawprint Parkour)
engine-minigame/             Mini-game engine
feature-philippine-history/  History feature (stub)
```

## Content System

### Live Server
- **DreamNAS:** 10.10.10.33:80 (Caddy, read-only)
- **Preview catalog:** `http://10.10.10.33/catalogs/preview.json` — 62 packages, 233 lessons
- **Development:** 62 packages at `catalogs/development.json`
- **Production:** 0 packages (educator review pending)
- **Package format:** per-week, per-subject (e.g., `g3-english-q2-w01-v1.zip`)

### Bundled Content (APK assets)
- 100 Month-01 lessons in `assets/content-pack/month-01/`
- 100 SVGs + 20 day manifests
- SHA-256: `0df8d9e517...b916e4`

### Content Loading
- `ContentLessonLoader`: reads from APK assets (multi-path, no Hilt)
- `LessonLoader`: legacy loader, reads from `content/ph-matatag/`
- `ContentVerifier`: SHA-256 + ZIP-slip protection (6 tests)
- `ContentSyncWorker`: WorkManager catalog sync with checksum verification

## Badge System (50 Philippine Endemic Animals)

| Component | Status |
|---|---|
| Database v3 | ✅ DailyChallenge + CollectedBadge entities |
| BadgeAwarder | ✅ 5 subjects/day → 1 badge, idempotent |
| WildlifeFieldGuideScreen | ✅ 50 slots, Pokémon silhouettes |
| BadgeRevealScreen | ✅ 3-step reveal (tracker → pulse → reveal) |
| Village integration | ✅ Daily challenge progress + badge count |
| Tests | ✅ 17 passing (awarder + loader) |

## Scoring Integrity (v0.10.0)

| Guard | Status |
|---|---|
| Duplicate MCQ results blocked | ✅ |
| Lesson completion verifies all steps | ✅ |
| SentenceBuilder → UnsupportedActivity | ✅ |
| Sort requires all items selected | ✅ |
| Unsupported activity doesn't auto-pass | ✅ |

## UI / Design (per design.md v2)

| Element | Status |
|---|---|
| Tactile buttons (bottom shadow) | ✅ MaxinesPrimaryButton |
| Cream surfaces (#FFF7E8) | ✅ Cards, panels |
| Step progress dots (48dp touch) | ✅ Proper spacing + states |
| Baloo 2 + Nunito fonts | ✅ |
| Theme contrast (gold-on-dark) | ✅ Fixed in v0.10.0 |
| Version display on village | ✅ v0.10.0 |

## Known Issues

| Issue | Priority | Notes |
|---|---|---|
| Lesson loading on tablet unconfirmed | P0 | JSON parse bugs fixed (JsonElement, AssessmentItem), Hilt removed. Needs tablet test. |
| SortStep compares against index order | P1 | Should compare against content-defined correct order |
| Village layout fixed-width (420dp map) | P1 | Should use WindowProfile for responsive layout |
| Parent dashboard accessible without PIN | P1 | ParentGateScreen navigates directly to dashboard |
| Profile/Achievements/Backpack nav items | P2 | Some still have empty callbacks |
| ProgressEventDao uses REPLACE | P2 | Should be ABORT for append-only |
| Room schema export path hardcoded | P2 | Should use $projectDir/schemas |
| Missing gradlew.bat | P3 | Windows wrapper not committed |

## Next Steps

1. **Confirm lesson loading on tablet** — install v0.10.0 and tap any subject
2. **Fix parent gate** — require PIN before dashboard access
3. **Responsive village layout** — use WindowProfile
4. **ProgressEventDao** — change REPLACE → ABORT
5. **Full test suite** — fix engine-sync tests, run lint
6. **NAS sync end-to-end** — test catalog download on tablet

## Theme.kt Warning

**Do NOT** refactor to parameterized composable. Must keep mutable package-level vars (`var AppDisplayFont`, `var AppBodyFont`). Xiaomi Pad 6S Pro crashes with parameterized theme (confirmed through 8 isolation builds).
