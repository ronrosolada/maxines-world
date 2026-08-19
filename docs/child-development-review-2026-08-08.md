# Maxine's World, Child-Development Content Review (2026-08-08)

**Role:** Expert child-development specialist review against the product goal:
*"factual, age-appropriate, learning-first, encouraging, privacy-preserving,
and fun ... for Maxine, an animal-loving eight-year-old."*
**Scope:** all 358 bundled lessons (`content-pack/month-01/lessons/`), 7 subjects.
**Status:** complete, findings + remediation landed, all gates green.

---

## 1. What was reviewed

Every learner-facing string of all 358 lessons: title, introduction, objective,
vocabulary, all 6 activity instructions + content + feedback, and all 5
assessment items per lesson (prompt, options, key, explanation). Plus the
generator/sanitizer tooling that produces lesson text
(`tools/content_review.py`, `tools/repair_educator_findings.py`,
`core-model/.../Models.kt`).

## 2. Baseline findings (before this pass)

| # | Pattern | Count | Child-development impact |
|---|---------|-------|--------------------------|
| F1 | Curriculum objective pasted into assessment stems ("Which example best shows this skill: Recognize and describe points, lines, line segments, rays, and special line relationships?") | 24 lessons (20 math, 4 english) | Working-memory overload; unreadable by an 8-year-old; tests meta-awareness, not the skill |
| F2 | Meta/template assessment items ("Which statement best matches 'Rounding Road'?", "Which one matches the lesson idea?", "Which choice does not follow the lesson idea?") | 60 lessons (all 60 legacy EN/MATH/SCI m01) | Tests title-matching, not skill transfer; teaches test-taking, not content |
| F3 | Abstract/garbage distractors ("a place value that was not named", "a color with no location", "Yesterday"/"Square" as feelings, "matches the lesson") | 47 lessons | No real discrimination; children succeed/fail by luck or pattern, not understanding |
| F4 | Scrambled answer keys (story-character question keyed "beginning"/"result"/"ending") | english-g3-q3-w13-d03/d04 | **Teaches false facts**, a child is told "beginning" is a character |
| F5 | Ambiguous items with 2+ defensible answers ("Which is NOT a living thing?", rock AND water both non-living) | science-g3-q1-w01-d01 (and similar) | Unfair; child cannot learn from a question with two right answers |
| F6 | Generic explanations ("The answer is X. It uses what we learned about Y.") | 392 items / 81 lessons | Feedback teaches nothing; no reasoning path |
| F7 | "Great thinking! You found the key idea. 🎉" correct-feedback placeholder | 696 instances / 116 lessons | Hollow praise; not task-specific |
| F8 | Identical boilerplate hooks ("Milo has a new mission! 🐱✨ ... Ready to explore?") | 201 lessons | Template parroting; hook stops engaging after lesson 1 |
| F9 | Definition-dump intros (legacy m01: "Locate the number between two multiples and choose the nearer benchmark...") | ~60 legacy lessons | Formal register; no hook; adult vocabulary |
| F10 | Template activity instructions ("Study the idea and listen to the narration.", "Pagbukud-bukurin ang mga halimbawa ng X") | ~444 (Filipino trio) + EN/MATH/SCI | Monotony; advanced verbs for 8-year-olds ("Pagbukud-bukurin" → "Pangkatin") |
| F11 | English/mixed-language titles in fil-PH lessons ("Caring for Natural Repinagmulans", typo; "Change at Continuity") | 14 AP titles | Language confusion; typo undermines trust |
| F12 | Stem-leak items (correct option = verbatim copy of the stem) | filipino-g3-q1-w01-d01 Q3/Q5 | Trivial matching; no skill practice |
| F13 | Tooling regenerates the bad patterns ("shows the skill" → "matches the lesson idea" sanitizer; "{objective}" pasting generator) | 2 tools + app sanitizer | Any future content run re-introduces F1/F2 |

**Not found (good):** English bleed in Filipino lessons (0), answer-position
bias (positions 621/422/423/324, no A-bias), multi-key items (0), duplicate
titles (0), GMRC/Makabansa/Filipino assessments (already re-authored in the
2026-08-07 r2 round, scenario-based, concrete, kid-real).

## 3. Remediation

### Completed by reviewer (this pass)
- **F11**: 15 AP titles translated to fil-PH (e.g. "Caring for Natural
  Repinagmulans" → "Pag-aalaga sa mga Likas na Yaman", "Change at Continuity" →
  "Pagbabago at Pagpapatuloy").
- **F12**: filipino-g3-q1-w01-d01 Q3/Q5 re-authored as split-choice items
  (candidate splits of the same sentence; stem no longer leaks the key).
- **F10 (trio)**: 91 Filipino/GMRC/Makabansa lessons, instruction verbs
  simplified to 8-year-old register ("Pagbukud-bukurin" → "Pangkatin",
  "Tuklasin" → "Hanapin sa larawan", "Pakinggan ang paliwanag tungkol sa X" →
  "Pakinggan ang paliwanag ni Milo").
- **F13**: `tools/content_review.py` sanitizer: "matches the lesson idea" →
  "shows what we learned" / "match what we learned"; `repair_educator_findings.py`
  generator no longer pastes `{objective}` into stems (short kid-parsable
  prompts instead); app `Models.kt` sanitizer + `FeedbackSanitizationTest`
  updated to match. All 9 Python tool tests + `:core-model:testDebugUnitTest`
  green.

### In progress (parallel sub-agents, disjoint subject sets)
- English (93 lessons): F1/F2/F3/F4/F6/F7/F8/F9/F10
- Mathematics (58 lessons): F1/F2/F3/F5-adjacent/F6/F7/F8/F9/F10
- Science (45 lessons): F2/F3/F5/F6/F7/F8/F9/F10

### Completed (4 worker waves + direct sweeps)
- **English 93/93, Mathematics 58/58, Science 45/45** re-authored across four
  parallel worker waves (each file: prompts, options, keys, explanations,
  activity instructions, intros, vocab, feedback).
- Scrambled keys fully rebuilt (english-g3-q3-w13-d03/d04, new prompts,
  options, keys, explanations; e.g. story-character items keyed to real
  characters).
- Ambiguous keys fixed (science living/non-living: water removed from
  non-living options; single-correct keys verified for every item).
- Clone assessment eliminated (mathematics-g3-q1-w01-d01 ↔ d04 were
  byte-identical after re-authoring; d04 got a fresh 5-item set).
- Teacher-voice scaffolding ("Try it with a classmate's name…") stripped from
  narration/accessibilityAlternative/content in 42 files.
- "Great thinking! You found the key idea." placeholder replaced with
  topic-relevant or rotated celebrations everywhere (0 remaining).
- Backtick option-id oddity normalized (` → e, keys re-pointed, 8 files).
- `repair_filipino_content.py` simuno generator fixed: Q3/Q5 no longer embed
  the correct split in the stem; all 32 simuno lessons regenerated; tool
  idempotency restored (14/14 tool tests).
- Sanitizer chains (app `Models.kt`, `content_review.py`,
  `repair_educator_findings.py`) no longer emit "matches the lesson idea" or
  paste curriculum objectives into stems.

### Deliberate, documented patterns (not blockers)
- **Milo ritual hook** ("May bagong misyon si Milo! 🐱✨ <concept>. Handa ka na
  bang sumubok?") kept in the Filipino trio (113 lessons): a predictable
  mascot opening with a topic-specific concept sentence is age-appropriate
  ritual, and the concept sentence repeats across quarters as spaced
  repetition (similarity gate confirms 0 whole-lesson clones).
- **3-option assessment items** in the legacy m01 set (320 items): original
  schema, pack-validated, developmentally fine for 8-year-olds.

## 4. Verification gates (to re-run after remediation)
### Final results (all green)

| Gate | Result |
|---|---|
| `/tmp/final_gate.py`, meta stems / abstract options / generic whys / key-idea feedback | **0 / 0 / 0 / 0** (only documented trio ritual hooks remain) |
| Content pack validation `--strict` | 358 lessons, 0 errors, 0 warnings |
| Content quality audit | 0 errors, 0 warnings |
| Title dedupe | 0 duplicate groups |
| Content similarity gate (≥0.85) | 0 pairs |
| Python tooling suites (14 files) | 14/14 pass |
| Android unit tests (incl. ContentPackIntegrityTest, FeedbackSanitizationTest, ActivityStepConversionTest) | BUILD SUCCESSFUL |
| Metadata integrity | 358/358 still `educatorValidated=true` + `RELEASED` |
| Assessment structure | every item: ≥1 key, keyed option present, unique option ids |
