# Maxine's World — Combined Audit & Fix Plan

## Cross-Reference: Hermes Technical Audit + Opus 4.8 Content Audit

### 🔴 P0/Critical — Blockers (fix first)

| # | Source | Issue | File(s) |
|---|---|---|---|
| 1 | Opus | **Rename "Philippine History" → "Makabansa"** — not a MATATAG Grade 3 subject. Reframe as Makabansa | manifest.json, lesson JSON, nav graph, VillageHomeScreen |
| 2 | Hermes | **Lesson loading broken** — LessonLoader path doesn't match content pack structure | LessonLoader.kt, MaxinesNavGraph.kt |
| 3 | Hermes | **Progress never saved to DB** — lesson completion doesn't write ProgressEventEntity or MasteryRecordEntity | LessonPlayerScreen.kt |

### 🟠 P1/High — Important

| # | Source | Issue | File(s) |
|---|---|---|---|
| 4 | Opus | **Activity-engine mismatches** — sequencing uses `sort_and_classify` instead of `timeline_builder` | English + Filipino lesson JSONs |
| 5 | Opus | **Math lesson introduces division w/o teaching it** — step-05 drops new operation | Math lesson JSON |
| 6 | Opus | **Science "yellow leaves" answer shaky** — oversimplified, may teach misconception | Science lesson JSON |
| 7 | Opus | **Module/lesson IDs inconsistent** — manifest says m05/m07/m04, lessons say m01 | All lesson JSONs + manifest.json |
| 8 | Opus | **Assessment minQuestions vs gradable** — Filipino has 3 gradable steps, minQuestions=4 | Filipino lesson JSON |
| 9 | Opus | **Missing GMRC + Makabansa subjects** — MATATAG has 6 areas, app has 5 | manifest.json, VillageHomeScreen |
| 10 | Hermes | **No TTS narration** — every lesson has narrationText, never read aloud | LessonPlayerScreen.kt |
| 11 | Hermes | **All characters are 🐱** — Milo/Mira/Niko/Lakan indistinguishable | VillageHomeScreen.kt, AuthScreen.kt |
| 12 | Hermes | **Static PIN salt** — same salt on every install | ParentAuthManager.kt |
| 13 | Hermes | **BiometricPrompt dead button** — icon shows but does nothing | AuthScreen.kt |
| 14 | Hermes | **Mastery engine missing required criteria** — no activity variation, no delayed review | MasteryEngine.kt |

### 🟡 P2/Medium — Polish

| # | Source | Issue | File(s) |
|---|---|---|---|
| 15 | Opus | **Filipino sentence-builder token mismatch** — question vs options don't match | Filipino lesson JSON |
| 16 | Opus | **Add curriculumStandard field + term tags** to lesson schema | All lesson JSONs |
| 17 | Hermes | **Parent dashboard shows hardcoded fake data** | ParentDashboardScreen.kt |
| 18 | Hermes | **Daily Quest shows quest rewards, not child's balance** | VillageHomeScreen.kt |
| 19 | Hermes | **Day streak hardcoded to 7** | VillageHomeScreen.kt |
| 20 | Hermes | **Celebration animations missing** | LessonPlayerScreen.kt |
| 21 | Hermes | **Village map is a menu, not a village** | VillageHomeScreen.kt |
| 22 | Hermes | **Backpack/Achievements/Profile nav dead ends** | MaxinesNavGraph.kt |
| 23 | Hermes | **Drag-drop activities auto-pass** | LessonPlayerScreen.kt |
