# Maxine's World — Hermes Agent Handoff

**Generated:** 2026-07-14
**Repo:** https://github.com/ronrosolada/maxines-world (private)
**Branch:** `main`
**Latest stable release:** v0.6.12 (APK confirmed working on 2 Xiaomi tablets)
**Latest release:** v0.6.13 (adds Filipino TTS unavailable UI)
**Total Kotlin files:** 54 across 19 modules
**APK size:** ~27 MB

---

## What is Maxine's World?

A private, offline-first Android learning app for an 8-year-old Grade 3 learner (Maxine).
Animal-village themed, MATATAG-aligned Filipino curriculum. Six subjects: English,
Filipino, Mathematics, Science, Makabansa, GMRC. Two reward mini-games (Cat Café Dash,
Pawprint Parkour) unlock after completing lessons. Designed for Xiaomi Pad 6S Pro
tablet (3:2 landscape, 3048×2032).

Stack: Kotlin, Jetpack Compose, Material 3, Room, DataStore, Hilt, Navigation Compose.

---

## Project Location & Environment

```
Windows 10 host: C:\maxines-world (NO spaces in path — KSP/Room processor fails with spaces)
Android SDK: %LOCALAPPDATA%\Android\Sdk (platform 35, build-tools 35.0.0)
JDK: Android Studio bundled JBR (C:\Program Files\Android\Android Studio\jbr)
Gradle: 8.9, Kotlin 2.x, AGP 8.7
```

### Build command:
```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
cd /c/maxines-world/android
./gradlew assembleDebug
```

### Release (requires GitHub CLI authenticated as ronrosolada):
```bash
export PATH="/c/Program Files/GitHub CLI:$PATH"
cd /c/maxines-world
gh release create v0.X.Y android/app/build/outputs/apk/debug/app-debug.apk \
  --title "v0.X.Y — Description" --notes "Changelog"
```

---

## Module Structure (19 modules)

```
app/                          ← Composition root, Hilt, Navigation, DI modules
core-model/                   ← Shared data classes (LessonManifest, ActivityStep, ProgressEvent)
core-database/                ← Room entities, DAOs, database (v2, exportSchema=true)
core-design-system/           ← Theme (colors, typography, MaxinesComponents)
core-network/                 ← Empty stub (offline app)
core-content/                 ← LessonLoader (JSON asset parsing + path resolution)
engine-activity/              ← ActivityResult data class, ActivityEngine typealias
engine-assessment/            ← Scorer engine (pure logic, tested)
engine-mastery/               ← MasteryEngine (activity variation, delayed review)
engine-sync/                  ← SyncWorker stub (returns success, offline app)
engine-minigame/              ← Shared mini-game contracts (MiniGameResult, RewardBreakClock)
feature-auth/                 ← ParentAuthManager (SHA-256 + per-install salt), AuthScreen, PIN setup
feature-child-home/           ← VillageHomeScreen (hero, building grid, Daily Quest)
feature-lesson-player/        ← LessonPlayerScreen + LessonPlayerViewModel + LessonTtsPlayer
feature-progress/             ← Progress tracking stubs
feature-parent/               ← ParentDashboardScreen (real data), ParentGateScreen (PIN challenge)
feature-rewards/              ← RewardsManager, RewardsScreen
game-cat-cafe/                ← Cat Café Dash mini-game (with art + sound assets)
game-pawprint-parkour/        ← Pawprint Parkour mini-game (with Milo sprites + sound)
```

---

## Key Files to Know

| File | Purpose |
|---|---|
| `app/.../MaxinesNavGraph.kt` | All navigation routes, startup destination logic via `StartupCheckEntryPoint` |
| `app/di/DatabaseModule.kt` | Room DB provider, Migration(1→2), 9 DAO providers |
| `core-database/.../MaxinesDatabase.kt` | @Database annotation v2, 9 entities, exportSchema=true |
| `core-database/schemas/.../2.json` | Room-generated schema JSON (source of truth for migration SQL) |
| `core-database/.../Entities.kt` | All Room entities including RewardBreakEntitlement, MiniGameResult |
| `feature-auth/.../ParentAuthManager.kt` | PIN hashing (SHA-256 + per-install SecureRandom salt + const-time compare) |
| `feature-lesson-player/.../LessonPlayerViewModel.kt` | ViewModel + LessonUiState + saveProgress logic |
| `feature-lesson-player/.../LessonPlayerScreen.kt` | Composables: ExplanationStep (TTS), MCQ (retry), SortStep, completion |
| `feature-lesson-player/.../LessonTtsPlayer.kt` | TTS wrapper: Locale.Builder("fil","PH") for Filipino, onUnavailable callback |
| `core-design-system/.../theme/Theme.kt` | Colors, typography (Baloo2 + Nunito via mutable package vars — DO NOT REFACTOR) |
| `core-design-system/.../components/MaxinesComponents.kt` | MaxinesPrimaryButton (shadow affordance), MaxinesAnswerCard |
| `app/src/main/AndroidManifest.xml` | allowBackup=true, INTERNET permission present (working baseline) |

---

## ⚠️ CRITICAL — Known Crash Root Cause

**Theme parameterized composable without `remember()` causes startup crash on Xiaomi tablets.**

The working Theme.kt uses mutable package-level `var` fields (`AppDisplayFont`, `AppBodyFont`)
that are mutated inside `MaxinesWorldTheme()`. This is not thread-safe but it WORKS on
the target devices.

When refactored to a parameterized composable (`fun buildTypography(displayFont, bodyFont)`),
the app crashes on startup UNLESS `remember(displayFont, bodyFont)` wraps the Typography
creation. Even WITH remember, some versions crashed — this was never fully debugged.

**DO NOT refactor the Theme.kt mutable vars** unless you test on a Xiaomi Pad 6S Pro
after every change.

---

## Testing Isolation Results (v0.6.x Series)

| Version | Changes from v0.5.0 | Result on Xiaomi Pad |
|---|---|---|
| v0.6.5 | Full revert to v0.5.0 + per-install salt | ✅ Works |
| v0.6.6 | All: DB v2 + Theme refactor + manifest changes | ❌ Crash |
| v0.6.7 | DB v2 only (Theme + manifest unchanged) | ✅ Works — DB not the problem |
| v0.6.8 | DB v2 + manifest (allowBackup=false, no INTERNET) | ✅ Works — manifest fine |
| v0.6.9 | DB v2 + manifest + Theme with remember() | ❌ Never confirmed — assumed broken |
| v0.6.10 | v0.6.9 + TTS screen changes | ❌ Crash |
| v0.6.11 | v0.6.9 + minimal TTS fix | ❌ Crash |
| v0.6.12 | v0.6.8 + TTS fix only (no Theme changes) | ✅ Works |
| v0.6.13 | v0.6.12 + full TTS locale + ExplanationStep UI | Not yet tested |

**Conclusion:** v0.6.8 is the last fully confirmed working build.
v0.6.13 adds TTS changes on top of v0.6.12 which should be safe (only lesson-time code).

---

## What's Working

- ✅ App opens on Xiaomi tablets (v0.6.12 confirmed, v0.6.13 should work)
- ✅ PIN setup/login with 6-digit requirement, per-install salt
- ✅ Child-first launch (no PIN needed to open)
- ✅ Parent gate (PIN challenge before dashboard)
- ✅ Village home: hero banner, subject grid, Daily Quest card, bottom nav
- ✅ 6 subject destinations → lesson loading from JSON assets
- ✅ Lesson flow: story intro → questions → completion → rewards
- ✅ TTS narration (English — reads aloud with speaker button)
- ✅ Honest scoring: retry loop (2 attempts), shuffled sort, unscored explanations
- ✅ Confetti celebration on lesson complete
- ✅ Step progress dots
- ✅ Actual star/coin rewards displayed (ceil(accuracy×5), 10 coins at ≥80%)
- ✅ Progress saved to Room (append-only events)
- ✅ Mastery engine: activity variation + delayed review + recent accuracy
- ✅ Parent dashboard: real data from Room, subject progress, mastery, day streak
- ✅ 6 MATATAG lesson JSONs (English, Filipino, Math, Science, Makabansa, GMRC)
- ✅ Baloo 2 + Nunito fonts bundled
- ✅ Adaptive launcher icon (Milo face + cottage)
- ✅ Zero emoji shipped (all Material icons)
- ✅ Tactile buttons (MaxinesPrimaryButton with shadow affordance)
- ✅ Cat Café Dash + Pawprint Parkour mini-games compiled (but reward break may need wiring)
- ✅ Database v2 with Migration(1→2), exportSchema=true

---

## What's NOT Working / Needs Attention

- ❌ **Filipino TTS reads with English accent** — device has no Filipino TTS voice.
  Solution: pre-recorded audio files or install Filipino voice on tablet.
- ⚠️ **Theme parameterized refactor causes crash** — do not touch without Xiaomi Pad testing.
- ⚠️ **Reward break entitlement flow may be partially broken** — saveProgress in
  LessonPlayerViewModel doesn't include the reward break DAO (removed in v0.6.4 rollback).
- ⚠️ **LessonCompletionRepository was removed** (v0.6.4 rollback) — ViewModel still
  accesses DAOs directly without @Transaction wrapping.
- ⚠️ **SentenceBuilderStep → UnsupportedActivity** (no-op, no credit — safe)
- ⚠️ **ArrayStep → MultipleChoiceStep** fallback (works but not real array building)
- ⚠️ **Achievements + Backpack nav items do nothing** (both navigate to child home)
- ⚠️ **Screen-time controls are display-only** — not enforced
- ❌ **No CI pipeline** — no GitHub Actions workflow
- ⚠️ **INTERNET permission present in manifest** despite offline-only app
- ⚠️ **allowBackup=true** (v0.6.12 working baseline includes this)

---

## Audit History

Four independent AI audits were applied to this codebase:

1. **Hermes (self-audit)** — Build system, architecture, 15 fixes
2. **Opus 4.8** — Content alignment (Philippine History → Makabansa), curriculum accuracy
3. **ChatGPT 5.6** — 6 critical usability defects (auth hang, PIN lockout, parent gate bypass, lesson blocking, auto-pass, duplicate results)
4. **Fable 5 (Claude Code)** — Child-first launch, honest scoring, retry, DB fixes

Plus a consolidated 3-review (Hermes + Opus + GPT-5.6) covering security, architecture, and UX.

---

## Remaining Tasks (priority order)

### P0 — Must Fix Before Wider Use
- [ ] Filipino accent — generate pre-recorded audio or install Filipino TTS voice on tablet
- [ ] Wire reward break DAO back into saveProgress (was removed in rollback)
- [ ] Add @Transaction wrapping for multi-entity writes

### P1 — Important
- [ ] Add CI (GitHub Actions: assembleDebug + test)
- [ ] Wire dead nav items (Achievements → RewardsScreen, Backpack placeholder)
- [ ] Manifest: set allowBackup=false once Theme/DB are stable
- [ ] Remove INTERNET permission (app is offline-only)
- [ ] Implement actual sentence-builder activity engine
- [ ] Enforce screen-time limits

### P2 — Polish
- [ ] Manifest-driven subject→lesson routing (remove hardcoded `when()`)
- [ ] StartupViewModel + splash screen (remove DB queries from NavGraph)
- [ ] LessonLoader typed Result instead of null
- [ ] Theme: thread-safe parameterized composable (test on Xiaomi Pad!)
- [ ] Pre-recorded Filipino + English lesson audio
- [ ] Distinct character illustrations (Milo/Mira/Niko/Lakan — currently all use the same icon)

---

## Quickstart for a New Hermes Agent

1. Clone the repo: `git clone https://github.com/ronrosolada/maxines-world.git`
2. Set up JDK + Android SDK (see environment section above)
3. Build: `cd android && ./gradlew assembleDebug`
4. APK at: `android/app/build/outputs/apk/debug/app-debug.apk`
5. Install on tablet: `adb install -r app-debug.apk`
6. Start reading from `feature-auth`, `feature-child-home`, `feature-lesson-player`

**When making changes:**
- Test on a Xiaomi Pad 6S Pro after every change
- Don't refactor Theme.kt without device testing
- Database version bumps MUST include a Migration
- Commit the generated schema JSONs (`core-database/schemas/`)
- Release APK after every meaningful change via `gh release create`

---

## Design Authority References

- `docs/design.md` — Full design system (colors, typography, components, accessibility)
- `docs/01-architecture-decisions.md` — ADRs
- `docs/02-milestones-and-risks.md` — Milestone plan
- `Downloads/Maxine's World app design.zip` — Original design assets (28.2 MB)
- `Downloads/maxines-world-mini-games-bundle.zip` — Mini-game source (23.6 MB)
- `Downloads/maxines-world-engineering-review.md` — Opus review
- `Downloads/maxines-world-comprehensive-review.md` — GPT-5.6 review
- `Downloads/MAXINES_WORLD_CONSOLIDATED_AUDIT.md` — Fable 5 review
- `Downloads/maxines-world-launch-crash-fix.md` — Opus crash fix analysis

---

*This handoff is accurate as of commit `d0e3d5a` on branch `main`.*
