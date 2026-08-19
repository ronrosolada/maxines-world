# Video Education Integrity — QA & Remediation Log

**Scope:** educational-video correctness for the Grade-3 (DepEd/MATATAG) media hub
(337-video catalog served from the homelab content server at `10.10.10.33`).
**Status:** remediation in progress (2026-08-20). Branch `fix/video-sequence-guardrail`.

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
  - Wave 2 ⏳ — Filipino (81), GMRC (36), Makabansa (105). Filipino-language.
- **Schema (canonical, matches app `MediaAssessmentItem`):**
  - option `{ "id": "a".."d", "text": ... }`
  - item-level `"correctOptionIds": ["<one of a-d>"]`
  - per item: `itemId "<mediaId>-qNN"`, `type "MULTIPLE_CHOICE"`, contiguous
    `sequence`, `prompt`, `explanation` (non-blank).
- **Integration script:** merges new items, re-sequences 1..N, fixes
  `itemId`/`questionCount`, sets `passingCorrectCount = max(2, ceil(0.80·N))`,
  rejects duplicate prompts + malformed options, emits an issue report.
  Artifacts staged under `/tmp/mw_new/*.json` and `/tmp/mw_catalog_expanded.json`.

## 3. Pending: canonical curriculum ordering

- The catalog `episodeNumber` <-> `mediaId -epNN` <-> title episode index are **three
  disagreeing schemes** (331/337 mismatch); subjects/grade-levels are also mixed
  (Grade 1/2, and non-English content tagged English). Recommended: regenerate the
  catalog with a per-subject curriculum sequence (Q1→Q4 by week) + validation gate.

## 4. Deployment plan (after Wave 2 + validation)

1. Merge expanded catalog → write to
   `/mnt/user/appdata/maxines-world-content/server/content/catalog.json` (DreamNAS);
   served immediately at `http://10.10.10.33/catalog.json`.
2. Safe for existing clients: 5..10 passes the current 3..10 validator floor.
3. Release v0.55.0: bump app `validateAssessment` floor `3..10` → `5..10`,
   include the sequence guard rail. Verify on emulator (Pixel-C/API35).
