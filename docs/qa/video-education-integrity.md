# Video Education Integrity — QA & Remediation Log

**Scope:** educational-video correctness for the Grade-3 (DepEd/MATATAG) media hub
(337-video catalog served from the homelab content server at `10.10.10.33`).
**Status:** remediation DONE (2026-08-20), released as **v0.55.0**.
Deployed catalog serves **337 videos × 5 content-based questions each** (was 3),
curriculum-ordered.

## 1. Guard rail — out-of-sequence playback (DONE, app code)

- **Problem:** any lesson video could be played/quizzed regardless of order.
- **Fix:** a lesson is 🔒 locked until **every earlier video in its subject's sequence
  is passed** (watch + quiz). Enforced in `VideoLibraryViewModel.play()` and
  `openAssessment()`, surfaced in `VideoLibraryScreen` as "Complete the previous
  lesson first". Replaying already-passed lessons stays allowed.
- Files: `feature-lesson-player/.../VideoLibraryViewModel.kt`,
  `feature-lesson-player/.../VideoLibraryScreen.kt`.
- ⚠ **Coupling:** the gate orders by the catalog `episodeNumber`, which is currently
  *scrambled* (see §4). The guard mechanism is correct; it becomes pedagogically
  meaningful once the catalog ordering is corrected.

## 2. Assessment expansion — 5–10 content-based questions per video (IN PROGRESS)

- **Finding:** all 337 videos carried **exactly 3** multiple-choice questions
  (`{3: 337}` in the served catalog) — below the ≥5 floor — and `passingCorrectCount=1`.
- **Existing questions are content-accurate** (verified sample across all 6 subjects);
  the gap is quantity. New questions are authored per video from the title/MELC plus
  the existing items (no transcripts exist on the server).
- **Language policy (app-wide):** Math/Science/English assessments in English;
  Filipino/AP(GMRC/Makabansa) in Filipino.
- **Authoring waves** (parallel subagents, one per subject, disjoint output files):
  - Wave 1 ✅ — Mathematics (43 vids, +86), Science (36, +72), English (36, +72) → 5 ea.
    - Merged + validated with **zero structural issues**; read-back sample confirmed
      content-accurate and pedagogically sound.
  - Wave 2 ✅ — Filipino (81), GMRC (36), Makabansa (105) → 5 ea. Filipino-language.
    - Merged + validated: **all 337 videos at 5 questions (674 new items), 0 issues**;
      read-back samples across all 6 subjects confirmed content-accurate.
    - **Deployed** to `server/content/catalog.json` on DreamNAS (backup
      `catalog.json.bak-20260820`); live at `http://10.10.10.33/catalog.json`,
      verified `{5: 337}`, 0 under floor.
- **Schema (canonical, matches app `MediaAssessmentItem`):**
  - option `{ "id": "a".."d", "text": ... }`
  - item-level `"correctOptionIds": ["<one of a-d>"]`
  - per item: `itemId "<mediaId>-qNN"`, `type "MULTIPLE_CHOICE"`, contiguous
    `sequence`, `prompt`, `explanation` (non-blank).
- **Integration script:** merges new items, re-sequences 1..N, fixes
  `itemId`/`questionCount`, sets `passingCorrectCount = max(2, ceil(0.80·N))`,
  rejects duplicate prompts + malformed options, emits an issue report.
  Artifacts staged under `/tmp/mw_new/*.json` and `/tmp/mw_catalog_expanded.json`.

## 3. Canonical curriculum ordering (DONE)

- The catalog `episodeNumber` <-> `mediaId -epNN` <-> title episode index were **three
  disagreeing schemes** (331/337 mismatch); Grade 1/2/other-grade and off-subject
  content was mixed into the Grade-3 subjects (e.g. a science/religion video tagged
  English, a Grade-2 lesson inside Grade-3 Filipino).
- **Fix (non-destructive ordering, per subject):** classify each lesson by
  grade/quarter/week/episode from its title, then order genuine **Grade-3 lessons
  Q1→Q4 by week first**, and quarantine all non-Grade-3 / off-curriculum content
  after them. `episodeNumber` renumbered to contiguous 1..N per subject (every
  subject verified contiguous; assessments untouched; 0 validation problems).
- **Deployed** to `server/content/catalog.json` on DreamNAS; local copy vs live
  `.33` md5 identical (`36af37d8…`). Prior 5-question catalog backed up as
  `catalog.json-ordering.bak-20260820`.
- Together with the guard rail (§1), the Grade-3 learner now progresses through each
  subject's curriculum in correct order; non-Grade-3 content is relegated to the tail.

## 4. Release v0.55.0 (DONE)

1. Expanded catalog deployed to `.../server/content/catalog.json` (DreamNAS), served
   at `http://10.10.10.33/catalog.json` — 337 videos × 5 questions, curriculum-ordered.
2. App `validateAssessment` floor `3..10` → `5..10`; sequence guard rail included.
3. Merged `fix/video-sequence-guardrail` → `main`; tagged **`v0.55.0`**
   (`versionCode 283`, monotonic, upgrade-safe from v0.52/0.53/0.54).
4. Signed release verified (CN=Maxines World), installed on emulator as a clean
   upgrade over v0.54.0, launches with no catalog/assessment errors.
5. Published GitHub release + repointed OTA (`.33/app-release.apk` & `.33/media/`
   served v0.55.0; prior APKs backed up as `*.bak-v054`).

## 5. Today's Video Quest (cross-subject, 30-40 min) — in progress

- **Requirement:** a quest that presents **video lessons from different subjects**
  totalling **30-40 minutes**, completed (watch + 80% quiz on each) for the reward.
- **Design:** deterministic-per-day (`childId`+`dayKey`), derived entirely from the
  existing ledger — **no DB migration**.
  - `VideoQuestPlanner` (pure, unit-tested): picks 2-3 **next-unlocked** lessons from
    different subjects whose combined accredited seconds are in `[1800, 2400]`
    (MIN 30m / MAX 40m; ceiling is hard, cross-subject is best-effort).
  - `VideoLibraryViewModel.recomputeVideoQuest`: builds the frontier (first unpassed
    lesson per subject, guard-rail compliant), runs the planner, exposes
    `state.videoQuest`; on completion grants a **once-per-day +3 ⭐ bonus**
    (`rewardDao.insertIgnoring`, id `video-quest:<child>:<day>` — idempotent).
  - `VideoLibraryScreen.VideoQuestCard`: "Today's Video Quest" header — subjects,
    total minutes, progress bar, Start/Continue → first pending selected video.
- Per-video stickers still flow through the existing 30-min policy; the quest adds a
  daily cross-subject goal + completion bonus on top.
- Status: **released in v0.56.0** (merged `feat/video-quest` → main, monotonic
  versionCode 286). Planner 7/7 unit tests + full lesson-player suite green. Full
  visual card walkthrough pending on the tablet — the emulator's preview home does
  not expose the video-hub tile.
