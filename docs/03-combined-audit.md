# Maxine's World — Combined Audit & Fix Plan

**Status: Historical — superseded by `HANDOFF.md` (canonical).** This file records the 2026-08-03 Hermes + Opus 4.8 audit. Every row now carries a status; rows without a linked resolution commit were closed by code verification on 2026-08-07 (external review round 2 + spec CH-14).

## Cross-Reference: Hermes Technical Audit + Opus 4.8 Content Audit

### 🔴 P0/Critical — Blockers (fix first)

| # | Source | Issue | File(s) | Status | Resolved in |
|---|---|---|---|---|---|
| 1 | Opus | **Rename "Philippine History" → "Makabansa"** — not a MATATAG Grade 3 subject. Reframe as Makabansa | manifest.json, lesson JSON, nav graph, VillageHomeScreen | ✅ Closed | 2026-08-06 (audit A1 product decision; Makabansa is the canonical home island; subjectMapping folds AP→Makabansa) |
| 2 | Hermes | **Lesson loading broken** — LessonLoader path doesn't match content pack structure | LessonLoader.kt, MaxinesNavGraph.kt | ✅ Closed | 2026-08-05; re-verified 2026-08-07 (single bundled path, `OfflineLessonLoadTest` green, spec CH-02) |
| 3 | Hermes | **Progress never saved to DB** — lesson completion doesn't write ProgressEventEntity or MasteryRecordEntity | LessonPlayerScreen.kt | ✅ Closed | 2026-08-05 (`LessonCompletionRepository.complete` writes completion + progress events + rewards; repository tests green) |

### 🟠 P1/High — Important

| # | Source | Issue | File(s) | Status | Resolved in |
|---|---|---|---|---|---|
| 4 | Opus | **Activity-engine mismatches** — sequencing uses `sort_and_classify` instead of `timeline_builder` | English + Filipino lesson JSONs | ✅ Closed | 2026-08-05 (typed activity payloads + `rendererType()` mapping; `RendererContractTest` 14/14) |
| 5 | Opus | **Math lesson introduces division w/o teaching it** — step-05 drops new operation | Math lesson JSON | ✅ Closed | 2026-08-03/07 (math content repairs; educator review 2026-08-07; 0 findings) |
| 6 | Opus | **Science "yellow leaves" answer shaky** — oversimplified, may teach misconception | Science lesson JSON | ✅ Closed | 2026-08-03/07 (science repairs incl. safety items — no sun-gazing options remain; content audits 0 findings) |
| 7 | Opus | **Module/lesson IDs inconsistent** — manifest says m05/m07/m04, lessons say m01 | All lesson JSONs + manifest.json | ✅ Closed | 2026-08-07 (the mismatched legacy manifest shipped with the ph-matatag fallback bundle; bundle removed from APK, external review C3) |
| 8 | Opus | **Assessment minQuestions vs gradable** — Filipino has 3 gradable steps, minQuestions=4 | Filipino lesson JSON | ✅ Closed | 2026-08-05 (assessment steps generated for every authored item; `everyPlayroomSubjectAssessmentIsConvertible` green) |
| 9 | Opus | **Missing GMRC + Makabansa subjects** — MATATAG has 6 areas, app has 5 | manifest.json, VillageHomeScreen | ✅ Closed | 2026-08-04 (GMRC + Makabansa converted lessons ship; `ContentPackIntegrityTest` subject coverage green) |
| 10 | Hermes | **No TTS narration** — every lesson has narrationText, never read aloud | LessonPlayerScreen.kt | ✅ Closed | 2026-08-06 (TTS narration wired; locale follows lesson language) |
| 11 | Hermes | **All characters are 🐱** — Milo/Mira/Niko/Lakan indistinguishable | VillageHomeScreen.kt, AuthScreen.kt | ✅ Closed | 2026-08-06 (character artwork/identity pass; Milo canonical guide) |
| 12 | Hermes | **Static PIN salt** — same salt on every install | ParentAuthManager.kt | ✅ Closed | 2026-08-07 verified: per-install 16-byte `SecureRandom` salt persisted in DataStore; salted double-SHA-256. NOTE: ADR-012's PBKDF2/EncryptedSharedPreferences description is stale (doc drift — CH-13) |
| 13 | Hermes | **BiometricPrompt dead button** — icon shows but does nothing | AuthScreen.kt | ✅ Closed | 2026-08-07 verified: control removed (explicit comment); `USE_BIOMETRIC` permission removed from manifest 2026-08-07 |
| 14 | Hermes | **Mastery engine missing required criteria** — no activity variation, no delayed review | MasteryEngine.kt | ✅ Closed | 2026-08-07 verified: recent accuracy ≥80% (last 5), ≥2 activity types, <2 hints/event, delayed review across days, min attempt count |

### 🟡 P2/Medium — Polish

| # | Source | Issue | File(s) | Status | Resolved in |
|---|---|---|---|---|---|
| 15 | Opus | **Filipino sentence-builder token mismatch** — question vs options don't match | Filipino lesson JSON | ✅ Closed | 2026-08-04 (Filipino repairs; educator review 2026-08-07 0 findings) |
| 16 | Opus | **Add curriculumStandard field + term tags** to lesson schema | All lesson JSONs | 🟡 Partial | `alignmentStatus` ships on all 358; `curriculumStandard` exists on `LessonManifest` but not `Month1Lesson` — fold into CH-07 phase-model schema work |
| 17 | Hermes | **Parent dashboard shows hardcoded fake data** | ParentDashboardScreen.kt | ✅ Closed | 2026-08-05 (dashboard reads Room data; parent features shipped) |
| 18 | Hermes | **Daily Quest shows quest rewards, not child's balance** | VillageHomeScreen.kt | ✅ Closed | 2026-08-05 (daily quests + balance from Room; reward ledger) |
| 19 | Hermes | **Day streak hardcoded to 7** | VillageHomeScreen.kt | ✅ Closed | 2026-08-06 by design: XP/streak counters removed from Child Home — vanity counters did not serve the learning loop (deliberate decision; do not reintroduce) |
| 20 | Hermes | **Celebration animations missing** | LessonPlayerScreen.kt | ✅ Closed | 2026-08-06 (confetti with reduced-motion respect in `LessonCompleteScreen`) |
| 21 | Hermes | **Village map is a menu, not a village** | VillageHomeScreen.kt | ✅ Closed | 2026-08-06 by design: village home superseded by the Playroom product surface (audit session decision) |
| 22 | Hermes | **Backpack/Achievements/Profile nav dead ends** | MaxinesNavGraph.kt | ✅ Closed | 2026-08-06 (nav hardening pass; no dead-end destinations) |
| 23 | Hermes | **Drag-drop activities auto-pass** | LessonPlayerScreen.kt | ✅ Closed | 2026-08-07 verified: every renderer gates completion on the child's input; `SequenceCtaContractTest` proves incomplete CTA is a disabled no-op; no auto-pass path exists |
