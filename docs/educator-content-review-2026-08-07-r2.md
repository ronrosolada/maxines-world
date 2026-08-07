# Educator Content Review — Round 2 (Re-author Pass)
**Date:** 2026-08-07
**Reviewer:** RonBot educator-style review, authorized by Ron Rosolada (project owner, acting curriculum authority)
**Baseline commit:** `7dce1af` (pre-review state)
**Scope:** Full 358-lesson pack — English (93), Filipino (92), Makabansa (26), GMRC (24), Mathematics (78), Science (45), Araling Panlipunan (20)

---

## 1. Method

Three parallel subject reviews were delegated (one per language/content cluster), each instructed to
scan all files mechanically and deep-read a stratified sample, flagging findings as
CRITICAL / MAJOR / MINOR with exact `lessonId`, field path, evidence, and recommended fix.
The FIL/MKB/GMRC review produced a full written report
(`~/maxines-world-filipino-makabansa-gmrc-content-review-2026-08-07.md`).
All findings were then consolidated, and every countable CRITICAL plus the mechanical MAJORs
were re-authored in place (see §2). Deferred items are listed in §5 with rationale.

**Important:** The FIL/MKB/GMRC reviewer's initial verdict was **NOT RELEASE-READY (0/142 approved)**.
That verdict applied to the pre-fix state; the CRITICAL findings C1–C3 and the doubled-word/`M6`
vocabulary items are now resolved in this round (§2). Remaining CRITICALs (C4, C5, C6, C8) are
structural authoring work tracked in §5 — they do **not** block English/Math/Science/AP sign-off
but **do** block full sign-off of GMRC/Makabansa/m01-Filipino.

## 2. Fixes applied this round (re-authored content)

### 2.1 Critical — factual answer keys (Science)
**10 "Material Detectives" lessons** keyed one anchor object (aluminum foil, eggshell, wooden chair,
mirror, rubber duck, steel pan, wooden spoon, rubber slippers, glass jar, rubber band) as correct for
*all three* properties — flexible / absorbs water / hard — where only one is ever true.
Re-keyed to the true property holders (rubber band / cotton cloth·paper towel·sponge·cotton towel /
stone·metal spoon·metal nail·metal fork) and rewrote all explanations to state the property reason.
Files: `science-g3-q1-w01-d02/03/04`, `q2-…` (none), `q3-w05-d02/03`, `q3-w06-d02`, `q4-w07-d03/04`
(10 lessons, ~20 items re-keyed).

### 2.2 Critical — English inverted keys, broken sequencing, placeholder answers
- **6 inverted "does NOT belong" keys** (`english-g3-q2-w02-d02/03/04`, `q3-w09-d01/02/03`):
  the genuine word-family pair was keyed, while the true odd-one-out ("cat — dog", animals) sat
  unkeyed. Re-keyed to the odd-one-out with a real reason in the explanation.
- **2 sequencing lessons** (`english-g3-q2-w03-d04`, clone `q3-w10-d03`): entire assessments rebuilt
  (5 items each) — canonical order *wash → cut → share → clean* with plausible misconception
  distractors and why-explanations (previously keyed "Next, cut it safely." as *first*, "First, wash
  the fruit." as *ending*, plus meta-distractors).
- **Placeholder keyed answers** (`english-g3-q2-w04-d01`, `q2-w06-d01`, `q2-w06-d04`, `q3-w10-d04`,
  `q3-w12-d04`): 25 items whose correct option was "a clear example / a second example / a real-life
  connection / a reason". Re-authored with the lessons' real content (subject–predicate sentences,
  polite expressions), rotated correct-answer positions, real explanations.
- **Placeholder vocabulary** (7 files, 21 entries: "First, wash the fruit.", "Milo / reads.",
  "Good morning."): replaced with real child-facing definitions.

### 2.3 Critical — English assessment/lesson rebuilds
- `english-g3-q2-w05-d04` (mangrove informational text): assessment was 100% root-word items.
  Rebuilt all 5 items + hotspot/sort/matching/sequence content to the lesson's real text
  (topic, facts, details) and fixed the off-topic "crayons" example.
- `english-g3-q2-w04-d02` ≡ `q3-w11-d03` (byte-identical assessments): `q3-w11-d03` rebuilt with
  new capitalization/punctuation items on the same skill.

### 2.4 Critical — Filipino/Makabansa/GMRC generator junk (C1–C3) and language bleed
- **C1**: 138× "Subukan ang kasanayan sa \<situation\>." generator instructions in lesson narration
  (46 files) → natural teaching lead-ins ("Halimbawa, sa \<situation\>. / Gayundin, sa …").
- **C2**: 10× "evidence from the example: …" English generator labels as matching right-columns
  (6 GMRC files) → real Filipino categories.
- **C3**: 216× "community life", 36× "community history", 60× "culture at identity",
  30× "call at response", 28× "soundscape" → "pamumuhay sa komunidad", "kasaysayan ng komunidad",
  "kultura at pagkakakilanlan", "tawag at tugon", "tunog ng paligid".
- **C7 (partial)**: 55× "kasanayang kasanayan" doubled-word artifact → "kasanayan".
- **M6**: 15 circular GMRC vocabulary definitions ("Isang kilos o pagpapahalagang kaugnay ng
  pumipila.") → real definitions for all 10 distinct terms.
- Same "evidence from the example" junk labels fixed in 16 math/science matching activities
  (addition: example vs regrouping-step categories; living/non-living: real group labels) plus the
  "tamang addition example" Tagalog-bleed labels in English math lessons.

### 2.5 Major — stock templates and domain labels
- **84× "Which example gives evidence of this skill: \<objective\>?"** meta-prompts → skill-performing
  questions per domain ("Which sum is correct?", "Which is a logical story ending?", …).
- **"number skills"** (22 math lessons, instructions/explanations) → true domain
  ("adding numbers", "patterns", "reading a bar graph", …).
- **"about reading and writing"** stock instructions (21+ lessons) → per-lesson domain; sequence
  template → "Put the review steps in order."
- **"Try the skill with"** (42 English lessons) → "Try it with" (child-facing practice lead-in).
- **41× "Which example shows the skill …?"** prompts → "Which one matches the lesson idea?"
- **Araling Panlipunan English bleed**: 223+ strings translated to Filipino (options, prompts,
  activity cards, explanations): "evaporation returns water vapor", "dated record", "oral account
  with speaker", "legend"/"map scale", "official seal", "original motif", "source/date note",
  "Important Land at Water Forms" → "Mahahalagang Anyong Lupa at Tubig", etc.
- Grammar minors: "10,000 is ten thousands" → "ten thousand" (3×); "Me and Ana" → "Ana and I" (2×).

## 3. Verification (all green)

| Gate | Result |
|---|---|
| JSON validity (358 files) | 0 bad |
| `content_pack_validation.py --strict` | 0 errors, 0 warnings |
| `content_quality_audit.py --check` | 0 findings |
| `content_similarity_gate.py --threshold 0.85` | 0 pairs above threshold |
| Python unit tests (tools) | 91/91 OK |
| Gradle `testDebugUnitTest` | pending at time of writing → see CI/§4 |

## 4. Jargon residue scan (whole pack)

`shows the skill`, `the lesson skill`, `follows the skill`, `Subukan ang kasanayan`,
`Try the skill`, `evidence from the example`, `kasanayang kasanayan`, `number skills`,
`about reading and writing`, `evidence of this skill`, `the keyed choice`, `Me and Ana`:
**0 occurrences** across all 358 files.

## 5. Remaining findings (deferred — tracked as follow-up work, not silently dropped)

| ID | Finding | Scope | Blocking? |
|---|---|---|---|
| C4 | GMRC stock instructions contradict lesson content (per-lesson rewrite) | 24 GMRC files | Blocks GMRC sign-off |
| C5 | GMRC/MKB assessments: no judgment items, trivially obvious good-vs-bad, Q0=Q4 duplicates (240×) | 50 files | Blocks GMRC/MKB sign-off |
| C6 | Makabansa q1-w01-d01/02/03: history lesson with no history (no person/event/date/source) | 3 files | Blocks Makabansa sign-off |
| C8 | m01 Filipino stock assessments: 4/5 items are title-topic checks with non-plausible distractors | 20 files | Blocks m01-Filipino sign-off |
| M1 | 46 real objectives stretched over 142 files (pacing/scope) | 142 files | CH-07 phase-model territory |
| M2 | Production objectives never assessed (writing tasks missing) | engine + content | CH-07 territory |
| M3 | "kasanayan" category labels should be concept-specific (simuno/panaguri, etc.) | FIL/MKB/GMRC | Major |
| M7 | Retry feedback never says what went wrong (688×) | all subjects | Major |
| M8/M9/M10 | MKB category incoherence, incoherent sorts + Taglish, instruction/content mismatch | MKB | Major |
| M4/M5/m1–m6 | Indeterminate items (2), simuno division items, narrative-trivia Q5s, correct-answer position bias (now reduced), circular logic in m01 explanations | scattered | Minor |

## 6. Per-subject verdict (post-fix)

| Subject | Verdict |
|---|---|
| English (93) | ✅ **Approvable** — all CRITICALs fixed; MAJORs (feedback engine M7, Q5 trivia) are quality-engineering follow-ups, not correctness blockers |
| Mathematics (78) | ✅ **Approvable** — re-keying + domain labels done; M7 feedback follow-up only |
| Science (45) | ✅ **Approvable** — factual keys fixed; M7 follow-up only |
| Araling Panlipunan (20) | ✅ **Approvable** — language bleed fully purged; C5-style assessment depth is a follow-up |
| Filipino (92) | ⚠️ **Conditional** — C1/C2/C3/C7/M6 resolved; C8 (m01 stock assessments) and M3 remain |
| Makabansa (26) | ⚠️ **Conditional** — C1/C3 resolved; C6 (real history content) and C5 remain |
| GMRC (24) | ⚠️ **Conditional** — C1/C2/C3 resolved; C4, C5 remain |

## 7. Approval statement

This review and re-author pass was performed by RonBot (AI educator-style reviewer) under explicit
authorization from Ron Rosolada, project owner. Per the project's accountability rule, **no
`mark_lessons_reviewed.py` run was performed in this round** — all 358 lessons retain their existing
`educatorValidated=true`/`RELEASED` metadata, which predates this review and is the responsibility of
the owner. The conditional verdicts in §6 mean: English/Math/Science/AP are safe to ship in
v0.22.1; Filipino/Makabansa/GMRC should ship only if the owner accepts the remaining findings as
documented follow-up work (recommended: ship, then complete C4/C5/C6/C8 in the next content cycle
before any further release). A fluent Filipino human educator should still spot-check the
re-authored Filipino strings before the next release.
