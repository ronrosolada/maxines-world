# Filipino Foundations and Integrated Learning System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Maxine’s World from an un-scaffolded native video library into an integrated, zero-beginner Filipino acquisition system with 24 audio-first Pre-A1 micro-lessons, runtime video checkpoints, mastery and spaced review wired to Daily Quest, transcript-grounded unique video assessments, and strict single-catalog parity.

**Architecture:**
- **Layer 1: Unified Media/Catalog Parity** — Reconcile checked-in and live 237-video manifests into one authoritative source with a CI parity validator.
- **Layer 2: Filipino Foundations v1** — Add `filipinoProficiency` profile state, 24 Pre-A1 micro-lessons, and zero-beginner Daily Quest routing.
- **Layer 3: Video Checkpoints Runtime** — Parse `video-checkpoints.json`, wire in-video pause prompts into ExoPlayer, and record formative attempt telemetry.
- **Layer 4: Competency & Spaced Review Engine** — Load `skill-graph.json`, connect `MasteryEngine` and `MiloReviewQueueResolver` to write progress events/mastery records and schedule spaced reviews.
- **Layer 5: Evidence-Grounded Video Assessments** — Re-author the 100 Filipino video assessments (500 items) so every video has a unique, topic-grounded bank and non-template explanation.

**Tech Stack:** Kotlin, Jetpack Compose, ExoPlayer/Media3, Room (v9/v10), Python 3.11 validator tooling.
**Spec:** `docs/filipino-educational-deep-dive-2026-08-28.md`

## Global Constraints
- Target 8-year-old Maxine; learning first, facts trustworthy, zero cloud telemetry.
- Strict language policy: English for Math/Science/English; Filipino for Filipino/Makabansa/GMRC.
- Offline-first: all foundations, checkpoints, skill graphs, and assessments must load from bundled assets or local cache.
- Strict gate order: JVM unit tests, content gates, lint, signed release assembly, and live API 35 emulator verification.

---

### Task 1: Reconcile Media Catalogs and Enforce CI Parity
**Files:**
- Modify: `server/content/catalog.json`
- Create: `android/tools/validate_catalog_parity.py`
- Modify: `.github/workflows/ci.yml`
- Test: `android/tools/test_validate_catalog_parity.py`

- [ ] **Step 1: Write failing catalog parity validator and test**
- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Update checked-in catalog to match authoritative 237-video schema and media-assessments.json**
- [ ] **Step 4: Verify parity validator passes**
- [ ] **Step 5: Commit**

---

### Task 2: Implement Filipino Foundations Data Model and Child-Home Routing
**Files:**
- Modify: `android/core-model/src/main/java/com/maxinesworld/coremodel/Models.kt`
- Modify: `android/core-database/src/main/java/com/maxinesworld/coredatabase/Entities.kt`
- Modify: `android/core-database/src/main/java/com/maxinesworld/coredatabase/Daos.kt`
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/DailyQuestManager.kt`
- Create: `android/app/src/main/assets/content-pack/foundations/filipino-foundations.json`
- Test: `android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/DailyQuestManagerFoundationsTest.kt`

- [ ] **Step 1: Write unit tests asserting Filipino Foundations routing for beginner profiles**
- [ ] **Step 2: Run test to verify RED**
- [ ] **Step 3: Add `filipinoProficiency` state and author 24 Pre-A1 foundations lessons**
- [ ] **Step 4: Run tests and verify GREEN**
- [ ] **Step 5: Commit**

---

### Task 3: Wire Interactive Video Checkpoints into ExoPlayer
**Files:**
- Modify: `android/core-model/src/main/java/com/maxinesworld/coremodel/MediaModels.kt`
- Create: `android/core-content/src/main/java/com/maxinesworld/corecontent/VideoCheckpointRepository.kt`
- Modify: `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/VideoStep.kt`
- Modify: `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/VideoLibraryViewModel.kt`
- Test: `android/feature-lesson-player/src/test/java/com/maxinesworld/featurelessonplayer/VideoCheckpointRuntimeTest.kt`

- [ ] **Step 1: Write failing runtime test for video checkpoint triggering and state machine**
- [ ] **Step 2: Run test to verify RED**
- [ ] **Step 3: Implement checkpoint loader, ExoPlayer position polling, and pause/checkpoint UI**
- [ ] **Step 4: Run test to verify GREEN**
- [ ] **Step 5: Commit**

---

### Task 4: Connect Skill Graph, MasteryEngine, and Spaced Review to Quest Progression
**Files:**
- Create: `android/core-content/src/main/java/com/maxinesworld/corecontent/SkillGraphRepository.kt`
- Modify: `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonPlayerViewModel.kt`
- Modify: `android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/VideoLibraryViewModel.kt`
- Modify: `android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/DailyQuestManager.kt`
- Test: `android/core-content/src/test/java/com/maxinesworld/corecontent/SkillGraphRepositoryTest.kt`
- Test: `android/feature-child-home/src/test/java/com/maxinesworld/featurechildhome/SpacedReviewQuestIntegrationTest.kt`

- [ ] **Step 1: Write failing tests for skill graph loading and due spaced-review quest scheduling**
- [ ] **Step 2: Run tests to verify RED**
- [ ] **Step 3: Wire MasteryRecord persistence upon passing assessments and inject due reviews into DailyQuest**
- [ ] **Step 4: Run tests to verify GREEN**
- [ ] **Step 5: Commit**

---

### Task 5: Re-author Filipino Video Assessments with Topic-Grounded Evidence
**Files:**
- Modify: `android/app/src/main/assets/content-pack/media-assessments.json`
- Create: `android/tools/audit_media_assessment_uniqueness.py`
- Test: `android/tools/test_audit_media_assessment_uniqueness.py`

- [ ] **Step 1: Write strict uniqueness and explanation-quality validator**
- [ ] **Step 2: Run validator to observe failure on current 9 reused banks**
- [ ] **Step 3: Re-author 100 Filipino video entries (500 items) with unique prompts and non-generic pedagogical explanations**
- [ ] **Step 4: Verify validator passes (0 duplicate groups, 0 template explanations)**
- [ ] **Step 5: Commit**

---

### Task 6: Full Verification, Smoke Test, and GitHub Release
- [ ] **Step 1: Run all Python quality and content gates**
- [ ] **Step 2: Run all Android unit tests across all 19 Gradle modules**
- [ ] **Step 3: Assemble signed release APK and verify v2 signature and APK metadata**
- [ ] **Step 4: Install APK on API 35 emulator, execute smoke tests across foundations, video checkpoints, and Arena**
- [ ] **Step 5: Open PR, merge, tag `v0.72.0`, publish GitHub release, and sync to DreamNAS OTA**
