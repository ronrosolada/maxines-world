# Maxine's World — Current State & Handoff

**Date:** 2026-08-01
**Version:** [v0.19.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.19.0)
**Commit:** `27f7d4c` on `main`
**Repo visibility:** PUBLIC (https://github.com/ronrosolada/maxines-world)
**Build:** `./gradlew assembleDebug` → BUILD SUCCESSFUL (CI-verified)

---

## Quick Start

```bash
cd android
./gradlew :core-content:testDebugUnitTest --stacktrace   # 100-lesson content integrity
./gradlew testDebugUnitTest --stacktrace                 # full JVM unit suite
./gradlew assembleDebug --stacktrace                     # APK → app/build/outputs/apk/debug/
./gradlew lintDebug --stacktrace                         # static analysis (not yet in CI — see notes)
```

## Current Baseline (v0.19.0)

### Canonical Home: Playroom (NOT VillageChromeV16 / VillageHomeV17)
- `PlayroomHomeScreen.kt` (feature-child-home) is the wired home — see `MaxinesNavGraph.kt` → `PlayroomHomeScreen`.
- 3×2 activity islands: Story Time (English), Number Fun (Mathematics), Kwentuhan (Filipino), Discovery (Science), Heritage (Araling Panlipunan), Kindness (GMRC).
- `VillageChromeV16.kt` and `VillageHomeV17.kt` remain in the source tree but are **not wired** — legacy, do not restore as home.
- Kindness island unlocks at **Level 4 = 12 distinct completed lessons** (`ChildLevelPolicy`). Replay-safe: repeated attempts don't inflate progress. Live locked/unlocked messaging on the Playroom home.

### Lesson Model & Navigation
- Typed `ActivityStep` model (`sortCategories`, `sortItems`, `matchPairs`, `sequenceSteps`, `hotspotExamples`) parsed in `LessonPlayerViewModel.toActivityStep()` — the old positional model is dead.
- Island tap → `lessonIdForSubject(subject)` → `Routes.lessonPlayer(childId, lessonId)` in `MaxinesNavGraph.kt`.
- Content: **bundled-only** — `ActiveContentIndex` scans `assets/content-pack/` (catalog v2); `LessonLoader` resolves lessons from bundled assets. No external content server; every lesson ships inside the APK.

### Database: Room v7 (immutable)
- `core-database/MaxinesDatabase.kt` → `version = 7`.
- Additive migrations: **MIGRATION_3_7, MIGRATION_4_7, MIGRATION_6_7** — all tested (MigrationTestHelper + emulator, representative data).
- Adopted v4/v6 lineage tables: `lesson_completions`, `reward_ledger`, `inventory`, `daily_quest_sets`, `daily_quest_completions`, `playground_unlock_receipts`, `content_packages`, `active_content_package`, `content_sync_runs` + `collected_badges` composite-index fix.
- **Never** reduce the version, delete schema JSONs (4.json/6.json = shipped builds), or use destructive migration fallback for child data.
- Known risk: schema files are immutable compatibility artifacts; any future fix ships as v8+ additive migration.

### App Metadata
- `versionCode = 19`, `versionName = "0.19.0"` (versionCode was stuck at 1 until v0.19.0; keep monotonic).

## Key Files

```
feature-child-home/
  PlayroomHomeScreen.kt       — canonical home (Playroom, OD Colorful design)
  VillageChromeV16.kt         — LEGACY, unwired (v1.6 target UI)
  VillageHomeV17.kt           — LEGACY, unwired (v1.7)

feature-lesson-player/
  LessonPlayerScreen.kt       — lesson player
  LessonPlayerViewModel.kt    — loadLesson + toActivityStep (typed model)

app/
  MaxinesNavGraph.kt          — navigation, wired to PlayroomHomeScreen

core-model/
  ChildLevelPolicy.kt         — level = distinctLessons/4 + 1; Kindness at 12

core-content/
  ActiveContentIndex.kt       — catalog v2 (synced content index)
  LessonLoader.kt             — bundled asset fallback

core-database/
  MaxinesDatabase.kt          — Room v7
  Migrations.kt               — 3→7, 4→7, 6→7 (additive)
```

## Content Server — RETIRED (bundled-only)
- **Decision (2026-08-01):** no external content server. All educational content ships inside the APK (`assets/content-pack/`).
- `nas-deployment/` removed — DreamNAS is no longer a content source; the app never contacts it.
## Content
- **Authoring home:** `ronrosolada/maxines-world-content` (62 weekly packages, catalog v2) — content is authored/updated there, then bundled into APK releases.
- **Playable pack: 349 lessons** in `content-pack/month-01/lessons/`:
  - 100 legacy hand-authored (5 subjects × 20 days, `-g3-m01-d` IDs)
  - **249 converted from DepEd SLM source** (`-g3-q` IDs, 6 subjects incl. gmrc + makabansa) via `android/tools/convert_slm_to_pack.py` — idempotent, deterministic, regenerates from `assets/content/ph-matatag/grade-3/`
  - **English Q1 (20 lessons) authored** via `android/tools/author_english_q1.py` — DepEd Matatag-aligned, fills the only missing-quarter gap
- **GMRC KNOWN GAP: RESOLVED** — Kindness island maps to `gmrc-g3-q1-w01-d01` (real GMRC content, not an AP placeholder).
- Coverage: english Q1–Q3 (73), filipino Q1–Q4 (63), gmrc Q1–Q4 (24), makabansa Q1–Q4 (26), mathematics Q1–Q4 (38), science Q1–Q4 (25). English Q4 is the only remaining gap (no source material).
- Conversion report: `android/tools/content-conversion-report.md`
- **Delivery: bundled-only** — every month/quarter lesson ships inside the APK; no content server, no runtime download (decision 2026-08-01).

## CI (`.github/workflows/ci.yml`)
- Job 1: content integrity (`:core-content:testDebugUnitTest` — 329 lessons).
- Job 2: `assembleDebug` + full `testDebugUnitTest`.
- Job 3: emulator-backed migration tests + offline-load instrumented tests.
- **Not yet in CI:** lint. (Roadmap.)

## Known Risks / Open Items
1. English Q4 has no SLM source content — only remaining quarter gap; author when source material becomes available.
2. **Release gate (ACTIVE):** `:app:verifyPlayableContent` blocks releases until every lesson is educator-approved (349/349 unreviewed). Approval = human review → `python3 android/tools/mark_lessons_reviewed.py` → commit. See `.github/workflows/release-gate.yml` (runs on v* tags).
3. Badge ownership uniqueness in v7 needs an audit (child-specific vs global `badgeId`).
4. Legacy PRs #1–#6, #8, #9: pre-Playroom stacks — **do not merge wholesale**; salvage only valuable behavior as fresh branches off `main`.
5. Large binaries (screenshots, ZIPs) in repo root — asset hygiene planned (Phase 7).
6. Root `HANDOFF.md` is the canonical handoff — this file. Do not revive the v0.16.0 document.

## Security posture (2026-08-01 external review addressed)
- **No INTERNET permission** — app is fully offline (bundled content only); sync engine removed.
- **Backups disabled** — `allowBackup=false` + `data_extraction_rules.xml`; child data never leaves the device.
- **Cleartext config removed** — `network_security_config.xml` deleted with the sync path.
- **App instrumentation in CI** — emulator job runs `:app:connectedDebugAndroidTest` + `:core-database:connectedDebugAndroidTest`.

## Known CI issue (2026-08-01): GitHub runner KVM regression
The `migration-tests` emulator job fails with `ProbeKVM: This user doesn't have
permissions to use KVM (/dev/kvm)` → "No compatible devices connected". This is a
GitHub-hosted-runner fleet regression (job passed 15:14Z, broke mid-day, affects
both ubuntu-latest and ubuntu-22.04). NOT a code issue — all tests pass locally on
the AVD. Do not rework the tests or the workflow; retry the failed run
(`gh run rerun <id> --failed`) until the fleet recovers. Job is intentionally
pinned to ubuntu-22.04 and kept REQUIRED.
