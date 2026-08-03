# Content Review — 2026-08-03

Educational content & module-layout review of the bundled G3 content pack (349 lessons),
plus an on-device "8-year-old" session on the emulator.

- **Branch:** `review/content-2026-08-03` (base `origin/main` 357ff81)
- **App:** 0.20.0 debug (`com.maxinesworld.app.debug`), emulator API 33, 3048×2032
- **Content:** `android/app/src/main/assets/content-pack/month-01/lessons/` (349 JSON), shipped inside the APK
- **Method:** static audit of all 349 lessons (identity, status, language, module tree, activities, assessment, assets, catalog projection) + live emulator playthrough of Math m01-d01, Math q1-w01-d01, and AP m01-d01 (partial) + Playroom/onboarding inspection.

---

## 1. Scope & layout summary

| Subject | Lessons | Modules | Coverage |
|---|---|---|---|
| English | 93 | m01 + q1-w01…q3-w14 (19 modules) | m01 20d, then weeks 5/4/4/4/2-day |
| Filipino | 83 | m01 + q1-w01…q4-w15 (16 modules) | m01 20d, then 5/5/5/4/5/5/5/4/4/4/4/1-day |
| Mathematics | 58 | m01 + q1-w01…q4-w09 (10 modules) | m01 20d, then 4/4/5/5/4/4/4/4/4-day |
| Science | 45 | m01 + q1-w01…q4-w09 (10 modules) | m01 20d, then 5/2/4/1/3/2/3/4/1-day |
| Araling Panlipunan | 20 | m01 only (1 module) | 20d |
| GMRC | 24 | q1-w01…q4-w08 (8 modules) | alternates 5d / 1d |
| Makabansa | 26 | q1-w01…q4-w07 (7 modules) | **unreachable** (no UI path) |

**Verified:** all 349 lessons parse, unique `lessonId` == filename, all `educatorValidated=true`,
all `releaseStatus=RELEASED`. Language matrix correct (Math/Science/English `en-PH`; the rest `fil-PH`).

Every lesson has exactly 6 activities in a fixed order:
`ANIMATED_EXPLANATION → HOTSPOT_IMAGE → SORT_AND_CLASSIFY → MULTIPLE_CHOICE → MATCHING_PAIRS → SEQUENCE_BUILDER`.

---

## 2. Module layout & progression findings

1. **Module labels are child-friendly on-device.** The app renders "Milo's Equal-Groups Market",
   "Quarter 1 · Week 1", etc., not raw `m01`/`q1-w01` codes. ✅
2. **m01 first, then renumbered weeks** — internally consistent, sensible ordering. ✅
3. **🟠 Repeated module subtitles:** Math shows "Multiplication Builders" for **4 consecutive modules**
   (Q3-W6, Q3-W7, Q4-W8, Q4-W9) on-device — the module JSON `title` field is duplicated.
4. **🟠 Repeated lesson titles:** 34 duplicate titles in English (e.g. "Picture Detective" ×4), 8 in Math,
   6 in Filipino, 4 in Makabansa, 3 each in Science/GMRC. On-device: **Q1-W01 shows "Shape Trail" 3×
   (D1, D3, D4)** with "Area Detectives" in between. Bodies differ (different `contentHash`), so it's
   template repetition, not duplication — but a child sees the same title repeatedly.
5. **m01 vs Q1 title overlap:** English 18/20 exact-title matches (Picture Detective, Meet the Characters,
   Choose an Ending…). Repetition, not byte-duplication (verified hashes differ).
6. **🟡 Cadence quirks (likely review days, unconfirmed):** 1-day modules in Science (`q2-w04`, `q4-w09`),
   Filipino (`q4-w15`), and every GMRC "week 2/4/6/8" (1d). GMRC alternates 5d/1d.

---

## 3. Reachability (P0)

- **🔴 26 Makabansa lessons are unreachable.** `ModuleCatalog` groups by raw ID prefix
  (`content-pack/month-01/lessons/`), but the home screen only requests
  `english/filipino/mathematics/science/araling-panlipunan/gmrc`. **No route ever requests `makabansa`.**
  Confirmed on-device: Playroom shows 6 islands, no Makabansa card anywhere.
- Note: `ContentLessonLoader.subjectMapping` maps `ARALING_PANLIPUNAN → "makabansa"` internally
  (LessonVM log: `subject=makabansa` for AP lessons), so AP completions are credited to the makabansa
  subject bucket — an inconsistency worth tidying (either surface Makabansa as its own island or fold
  it into AP deliberately).

---

## 4. Assessment & MCQ integrity (P0 / P1)

### Live (child-facing) path — activity MCQs
- **🔴 Correct answer is option A in 349/349 activity MCQs (all subjects, all lessons).**
  `MultipleChoiceRenderer` renders `step.options` in order; **no shuffle exists** for MCQ
  (shuffle only in sort/sequence). An 8-year-old can score 5/5 by always tapping the first card —
  verified live on Math q1-w01-d01 ("point A" was option 1 and correct).
- **🔴 114 live filler distractors in Math, 38 lessons** (activity MCQs): "a random guess" (38),
  "a mismatched unit" (38), "an unrelated operation" (38). Live example: Q1-W01-D01 MCQ options were
  `point A / a random guess / a mismatched unit / an unrelated operation`.
- **🔴 Live template-filler content in Math quarterly lessons (38 lessons):**
  - Vocab definitions: `point A → "correct idea"`, `segment AB → "useful example"`,
    `ray CD → "check the concept"` (114 entries, all Math) — confirmed on-device.
  - Sort cards include literal placeholders: "a mismatched unit", "an answer with no label",
    "a random guess", "an unrelated operation" (4 of 8 cards in Q1-W01-D01 sort) — confirmed on-device.

### Dead (not yet rendered) — assessment block
- The `assessment` JSON block has **no UI consumer** (no screen, no nav route; only `Scorer.kt` +
  tests reference it). The "You are ready for the five-question check" text is flavor; after the 6
  activities the app goes straight to Lesson Complete. So:
  - 570 assessment-option filler hits (38 Math lessons) — **P1** (dead now, will leak when assessment ships).
  - 230 lessons repeat an assessment prompt within their own 5-item set — **P1**.
  - Assessment correct-position bias: AP 100% A (100/100), English 253A, Math 138A, Science 125A;
    only GMRC balanced (24/48/24/24). If the assessment is ever wired up, shuffle + rebalance required.

---

## 5. Asset coverage (P1)

- 1,994 activity `assetId` refs → 349 distinct IDs; **only 100 SVGs exist, all `m01`** (`assets/vectors/*`).
- **1,494 refs (all quarterly lessons) have no SVG** → hotspot renderer falls back to 4 labeled text boxes.
- On-device (Math q1-w01-d01 "explore" step): the board is plain tan boxes with labels — **no artwork**.
  Vision analysis: "no drawing of a point, line segment, ray… would not read as a picture activity."
  "Picture Detective" with no picture. Not a crash, but the core mechanic (visual identification) is absent.
- Legacy `ph-matatag` tree: 250 JSONs still shipped in the APK, **unreachable** (legacy `LessonLoader`
  used only by androidTest). ~Dead weight.

---

## 6. On-device session log (as an 8-year-old)

1. **Onboarding is a parent gate:** "Welcome to Maxine's World! Set up a PIN to keep the parent area
   secure." → name + 6-digit PIN → "Create Child Profile" (Child's name) → "Who's learning today?"
   (Maxine, Grade 3). Clear, but the first screen is for the adult — fine for COPPA, worth knowing.
2. **Playroom:** Milo greeting + 6 islands + Today's Quest (3 adventures / 2 areas) + Wildlife Stickers
   (0/50). No Makabansa. GMRC ("Kindness") is tappable — the "LOCKED" card in
   `PlayroomHomeViewModel` is dead code.
3. **Math m01-d01 (Building Numbers to 10,000)** — played end-to-end:
   - Real vocab (number/thousand/hundred), Milo narration + Read Along TTS, explore-examples gating (✓),
     sort (tap-to-bucket → Submit → "Great job! 🎉"), MCQ (correct answer first), matching 3/3,
     sequence State→Study→Explain (one "Try Again" after a wrong order — good retry loop).
   - **Completion:** "Lesson Complete! 5 out of 5, 100%, +3 Stars, +10 Coins". Playroom updated
     (Math 1%, quest 2/3). **This track is solid, sensible, and grade-appropriate.**
4. **Math q1-w01-d01 (Shape Trail)** — the contrast is stark:
   - Vocab placeholders ("point A — correct idea"), text-only hotspot board, sort with 4 placeholder
     cards, MCQ with 3 placeholder distractors, matching of placeholder pairs.
   - Only the sequence step (Read → Choose → Solve → Check) is real content.
   - Still completed 5/5 (+3 stars, +10 coins) — the rewards don't discriminate quality.
5. **AP m01-d01 (Map Symbols Detective)** — clean, real Filipino content (legend/direksiyon/label,
   "asul na guhit—ilog ayon sa legend", poor-habit sort cards). **Mid-lesson the process was
   force-stopped by a package reinstall** (`am_kill: stop com.maxinesworld.app.debug due to
   installPackageLI`, session 984782121, `lastUpdateTime 12:40:41`) — environment interference, not an
   app crash (no FATAL for our package; stale dropbox entries are from Aug 1 and already known).
   Data wiped → returned to PIN screen. AP MCQ not reached live; the always-first-option pattern is
   already proven statically + by the no-shuffle renderer + live Math evidence.
6. **Completion screen:** functional but flat — sparse pastel confetti, no burst/animation; the
   "celebration" reads like a report card (vision analysis).

### "Does it make sense and engage an 8-year-old?" — verdict
- **Math m01 track: yes.** Clear progression, real content, Milo hook present, gated activities,
  retry loop, working rewards.
- **Math quarterly track: no.** Filler vocab, filler distractors, no artwork — an 8-year-old will
  read "a random guess" aloud and ask what it means. Also trivially "winnable" by tapping the top card.
- **Other subjects:** English/Science/Filipino m01 presumably match the good Math m01 pattern;
  quarterly lessons share the same generation issues (filler vocabulary confirmed in Math; asset
  coverage confirms no artwork anywhere outside m01).
- **Milo hook coverage:** AP intros mention Milo in 0/20 lessons (others: Math 38/58, Science 25/45,
  English 53/93, Filipino 63/83, GMRC & Makabansa 24-26/24-26).
- **Structure repetition:** every lesson = same 6 activities in the same order — predictable;
  the fillers and title repetition make it feel template-generated in the quarterly track.

---

## 7. Prioritized recommendations

| Pri | Fix | Why |
|---|---|---|
| P0 | Remove/replace all template-filler content in the 38 Math quarterly lessons (vocab definitions, sort cards, MCQ distractors) | Live, child-facing nonsense |
| P0 | Shuffle MCQ options (or randomize `correctIndex`) in the content pipeline + renderer | Correct-answer-always-first is a teachable exploit; 349/349 lessons affected |
| P0 | Decide Makabansa: add a home route/island or fold its 26 lessons into AP explicitly | 26 lessons are currently invisible; AP→makabansa internal mapping already conflates them |
| P1 | Generate SVG artwork for quarterly lessons (or remove the `assetId` refs and accept text-only deliberately) | 1,494 refs resolve to nothing; "Picture Detective" has no picture |
| P1 | De-duplicate assessment prompts (230 lessons) + fix assessment filler options before wiring assessment UI | Dead now, will ship broken later |
| P1 | De-duplicate module/lesson titles (Math "Multiplication Builders" ×4 modules, "Shape Trail" ×3 in one week, "Picture Detective" ×4) | Confuses navigation and feels low-effort |
| P2 | Confirm 1-day modules are intentional (review days?) | GMRC/Science cadence looks like gaps |
| P2 | Beef up the completion celebration (burst animation, sound, mascot reaction) | Currently flat for a kids' app |
| P2 | Remove unreachable `ph-matatag` tree (250 JSONs) from the APK | Dead weight |
| P2 | Add AP Milo hooks (0/20) for character consistency | Guide character should show up in every subject |

---

## 8. Files referenced

- `android/core-content/src/main/java/com/maxinesworld/corecontent/ModuleCatalog.kt` (catalog grouping)
- `android/core-content/src/main/java/com/maxinesworld/corecontent/ContentLessonLoader.kt` (paths, `subjectMapping`)
- `android/feature-lesson-player/.../LessonPlayerViewModel.kt` (`toActivityStep`, no assessment conversion)
- `android/engine-activity/.../renderers/MultipleChoiceRenderer.kt` (no shuffle)
- `android/engine-activity/.../renderers/HotspotImageRenderer.kt` (labeled-box fallback)
- `android/feature-child-home/.../PlayroomHomeViewModel.kt` (6 canonical subjects; GMRC Available)
- Content: `content-pack/month-01/lessons/*.json` (349), `content-pack/month-01/assets/vectors/*.svg` (100, m01 only)
- Audit artifacts: `/tmp/mw-content-review/audit.out`, emulator dumps `/tmp/mw-emulator/*.png|xml`

---

## 9. Validation update — post-PR #26 (2026-08-03)

**Validated against:** `main` @ `11b49af` (`fix: close educational content review P0s (#26)`), clean and synced.

**Method:** re-ran the static audit (`/tmp/mw-content-review/audit.py`) on the current content-pack, plus direct source inspection and targeted scans. Note: `diff` of the pre- and post-#26 audit output is **empty** — the bundled lesson JSONs are byte-identical to the review baseline, i.e. PR #26 was code-only.

**Automated checks** (from `/home/ron/workspace/maxines-world/repo/android`):

```bash
./gradlew testDebugUnitTest lintDebug :app:verifyPlayableContent   # exit 0
./gradlew :app:connectedDebugAndroidTest :core-database:connectedDebugAndroidTest \
  :feature-child-home:connectedDebugAndroidTest                    # 30/30 passed, 0 failed, 0 skipped
```

### 9.1 Fixed by PR #26 — verified on current `main`

| Original finding | Status | Verification |
|---|---|---|
| §3: 26 Makabansa lessons unreachable (no UI path) | ✅ Fixed | `PlayroomHomeUiState.kt:111` defines the island (`id = "makabansa"`, playful name "Bayan at Kultura"). `ModuleCatalog.modulesFor()` is subject-agnostic — it groups any subject prefix found in lesson IDs — so the module/lesson list resolves once the island routes to `makabansa`. |
| §4: Live (child-facing) Math filler — vocab definitions ("correct idea", "useful example", "check the concept"), sort cards, MCQ distractors | ✅ Fixed | Targeted scan of the **activity path only** (MCQ `content.options`, prompts, sort cards, pairs) across all 349 lessons: **0 occurrences** of "a random guess", "a mismatched unit", "an unrelated operation", "an answer with no label", "correct idea", "useful example", "check the concept". All 570 remaining hits (audit §6/§11) are confined to the unused `assessment` block. One `responsableng gawain` remains in `araling-panlipunan-g3-m01-d08` — educator-reviewed and assessed as legitimate, not filler. |
| §4: Correct answer always option A (349/349), renderer never shuffled | ✅ Fixed at runtime | `MultipleChoiceRenderer.kt:194` shuffles options deterministically per step (`order.shuffle(Random(stepId.hashCode()))`). Static content still has strong correct-index bias (321/9/9/10 across 349 activity MCQs), but displayed order is re-shuffled, so "always tap the first card" no longer scores 5/5. The future assessment path must still shuffle and rebalance its own options before exposure. |

### 9.2 Still valid on current `main` — content unchanged

| Finding | Current evidence | Priority |
|---|---|---|
| Assessment filler: 570 option hits in the 38 quarterly Math lessons ("a mismatched unit" 152, "an unrelated operation" 152, "a random guess" 152, "an answer with no label" 114) | audit §6/§11; block still has no UI consumer (no screen/route — only `Scorer.kt` + tests reference it) | P1 — replace before the assessment flow is exposed |
| Duplicate assessment prompts: 230 lessons repeat a prompt within their own 5-item set | audit §6 (`dup_prompt_in_lesson: 230`) | P1 |
| Asset coverage: 1,994 activity `assetId` refs → 349 distinct IDs; only 100 SVGs (all `m01`); 1,494 refs unresolved → hotspot fallback labels | audit §7 + `assets/vectors` (100 files) | P1 — "Picture Detective" still has no picture |
| English Q4 gap: quarterly English = q1 (20) + q2 (27) + q3 (26); no `english-g3-q4-*` files (93 lessons total incl. m01) | filename scan of `content-pack/month-01/lessons/` | P2 — author Q4 or document the coverage boundary |
| Repeated titles / module subtitles | audit §8: English m01↔q1 exact-title overlap 18/20; `ModuleCatalog.kt:95` hides English m01 as mitigation; repeats persist elsewhere ("Shape Trail" ×3 in Q1-W01, "Picture Detective" ×4, Math "Multiplication Builders" ×4 consecutive modules) | P2 |
| `educatorValidated=true` / `releaseStatus="RELEASED"` on 349/349 | audit §2 — self-attested metadata, not independent human curriculum sign-off | gate |
| Legacy `ph-matatag` tree: 250 JSONs still shipped | `assets/content/ph-matatag/` — 250 JSON files. **Correction to the validation handoff's "count: 0" claim** (that check was scoped to the wrong root). Nuance vs. the original review: not strictly unreachable — `ContentLessonLoader.tryPath()` path 3 falls back to `content/ph-matatag/grade-3/{lessonId}.json`, and `ModuleCatalog` reads `grade-3/manifest.json` for legacy m01 titles. All 349 bundled IDs resolve via path 1, so the 250 lesson JSONs are fallback-only APK weight today. | P2 |

### 9.3 Open code follow-ups (identified during validation, not yet addressed)

1. **Reduced-motion:** `LessonPlayerScreen.kt` (~lines 322–326) hard-codes reduced-motion behavior instead of respecting the device accessibility setting.
2. **Resume/completion UX:** `ModuleLessonsScreen.kt` / `ModuleLessonsViewModel.kt` don't expose completed lessons, current lesson, resume position, or recommended next lesson (Playroom-level subject progress does exist).
3. **Parent streak:** `ParentDashboardScreen.kt` derives streak from display/activity strings rather than date-based completion records — review across consecutive days, timezone boundaries, duplicate completions, missed days.
4. **PIN brute force:** `ParentAuthViewModel.kt` (~lines 85–95) clears input and shows "Incorrect PIN. Try again." with no attempt counter, delay, cooldown, or lockout.
5. **Coins documentation contradiction:** the lesson flow persists and awards coins while related docs claim coins are no longer awarded — reconcile implementation with the documented reward policy.

### 9.4 Prioritized remaining work (current state)

| Pri | Item |
|---|---|
| P1 | Replace assessment filler in the 38 quarterly Math lessons |
| P1 | Deduplicate assessment prompts in the 230 affected lessons |
| P1 | Decide: quarterly SVG artwork, or intentionally redesign those activities as text-based (drops the 1,494 dangling refs) |
| P2 | Resolve English Q4 gap or document the coverage boundary |
| P2 | PIN attempt throttling + temporary lockout |
| P2 | Proper reduced-motion support (respect system setting) |
| P2 | Completion/resume state in module lesson lists |
| P2 | Date-based parent streak calculation |
| P2 | Reconcile coin-award implementation with product docs |
| P2 | Reduce repeated lesson/module titles where repetition harms navigation |
| Gate | Genuine independent educator review (factual accuracy, pedagogy, language, safety) — `mark_lessons_reviewed.py` must not run before it |

---

## 10. Current validation after assessment P1 fixes — 2026-08-03

Sections 7 and 9.2 preserve the historical/pre-fix findings. The current `main` state is now `999caa5`, with content changes in `9da1ffc` (`fix(content): replace assessment filler and duplicate prompts`).

### Resolved since §9.2

- **Assessment filler:** all 570 generic option hits in the 38 quarterly Mathematics lessons were replaced with topic-grounded examples and distractors.
- **Duplicate prompts:** all 230 affected lessons now have five distinct assessment prompts.
- **Generator regression guard:** `android/tools/content_review.py` now generates five distinct English or Filipino fallback prompts when a profile has fewer than five hand-authored checks.

The content commit's diff was verified at the object level: all 230 changed lesson files differ from their `main` baseline only in their `assessment` object. No activity, title, vocabulary, release metadata, or review-marker fields were changed.

### Current checks

```text
349 lesson JSON files parse
0 duplicate assessment-prompt lessons
0 assessment filler hits
0 assessment schema errors
8/8 Python content-review tests passed
Gradle unit tests + lint + :app:verifyPlayableContent: BUILD SUCCESSFUL
Connected Android tests: 30/30 passed, 0 failed, 0 skipped
```

The release gate still reports 349 educator-reviewed lessons because approval metadata was intentionally preserved. Automated validation is not independent educator approval.

### Current remaining priorities

1. Decide whether to create quarterly SVG artwork or intentionally redesign those activities as text-based (1,494 dangling refs).
2. Resolve the English Q4 coverage gap or document the intended boundary.
3. Reduce repeated lesson/module titles where repetition harms navigation.
4. Complete genuine independent educator review for factual accuracy, pedagogy, language, and safety.
5. Address the code follow-ups listed in §9.3 through the Android app owner.

