# Grade 3–4 Future Curriculum Bank — Educator & Integration Readiness Audit

**Audit date:** 2026-08-29  
**Scope:** 48 units, 192 MCQs, 48 micro-lessons/hints, 48 remediation ladders, 48 unit parent-report records  
**Decision:** **NOT APPROVED FOR LEARNER RELEASE — CONDITIONALLY READY FOR REVISION**

## Executive summary

The bank is structurally complete and internally linked. Its topic spread is broadly suitable for learners aged 8–10, the six learning-area groups are balanced across Grades 3 and 4, all prerequisite references resolve against the production `skill-graph.json`, and the supplied validator and tests pass.

Educator sign-off is nevertheless withheld. The strongest blocker is a fully predictable answer pattern: **every one of the 48 units uses A, B, C, D in that order**. Although the corpus totals are numerically balanced (48 of each key), learners can infer the key by item position. In addition, **133/192 keyed answers are the uniquely longest option**, creating a second strong test-wise cue. The micro-lessons are mostly narrated objective summaries rather than worked 30–40 second explainers, with a repeated generic ending in 24 English scripts and a parallel repeated ending in the 24 Filipino scripts. Strict language separation is also not met because Filipino-medium content contains avoidable English parentheticals and technical terms. Finally, the roadmap itself states that exact DepEd competency-code confirmation is still required, so curriculum alignment is not release-verifiable yet.

## Coverage and gate results

| Gate | Result | Evidence |
|---|---:|---|
| Roadmap inventory | PASS | 48 units: 24 Grade 3 + 24 Grade 4 |
| Learning-area balance | PASS | 8 each Mathematics, Science, English, Filipino, GMRC; Makabansa/AP combined = 8 |
| Assessments | PASS structurally | 192 items; exactly 4 per unit; 4 unique options and a valid key per item |
| Key totals | PASS numerically | A=48, B=48, C=48, D=48 |
| Key predictability | **FAIL pedagogically** | All 48 unit sequences are exactly `ABCD` |
| Duplicate normalized prompts | PASS | 0 exact duplicate MCQ prompts |
| Generic placeholder markers | PASS | No stock markers such as “matches the lesson,” “fits the idea,” “close-but-wrong,” or “random/unrelated” |
| Micro-lessons/hints | PASS structurally | 48 scripts + 48 hints; scripts 50–66 words, hints 8–22 words |
| Remediation | PASS structurally | 48 ladders; 3 steps each; unit and prerequisite links resolve |
| Parent reports | PASS structurally | 48 records and EN/FIL templates |
| Skill graph references | PASS | 0 missing prerequisite references against Android production `skill-graph.json` |
| Supplied validator | PASS | Exit 0: all counts, links, language labels, balance, duplicates, and scaffolding valid |
| Supplied tests | PASS | 2/2 tests passed (`test_bank`, `test_counts`) |

Commands run:

```bash
python3 validate_bank.py --skill-graph /opt/data/maxines-world/android/app/src/main/assets/content-pack/skill-graph.json
python3 -m unittest -v test_validate_bank.py
```

Validator output:

```text
PASS: 48 units (24 G3, 24 G4), 192 assessments, 48 ladders, 48 explainers/hints, 48 parent-report records; links/language/balance/duplicates/scaffolding valid
```

Test output: `Ran 2 tests ... OK`.

## Findings by priority

### P0 — release blockers

#### 1. Answer placement is completely predictable

Every unit keys q01=A, q02=B, q03=C, q04=D. The overall 48/48/48/48 distribution therefore masks severe within-unit bias. A learner can answer by ordinal position without reading the content.

**Required fix:** deterministically scramble option order per item while preserving key integrity; enforce per-unit non-identical sequences and corpus balance within an agreed tolerance. Add a regression test that rejects a repeated sequence across all units.

#### 2. Option-length cues compromise validity

The correct response is the uniquely longest option in **133/192 items (69.3%)**; only 52 items have no unique shortest/longest keyed cue. This is especially common in Science, GMRC, and Makabansa/AP, where the correct choice is a nuanced sentence and distractors are brief, obviously harmful, or absurd.

Examples include:
- `science-g3-future-01-q04`
- `science-g3-future-02-q03`
- `science-g3-future-03-q01`
- `gmrc-g3-future-04-q02`
- `gmrc-g4-future-03-q04`

**Educational consequence:** the items often measure recognition of the most detailed/prosocial answer rather than the stated competency.

**Required fix:** make distractors parallel in length, grammar, specificity, and plausibility; use misconception-based distractors rather than caricatured misconduct.

#### 3. Exact DepEd alignment is not yet established

All 48 roadmap records carry `sourced-framework-authored-alignment`, but the primary reference explicitly says: **“exact competency-code confirmation required before release.”** Topic-level comparability is not sufficient evidence for a DepEd-aligned release claim.

**Required fix:** map every unit objective to the current official MATATAG learning competency/code and grade/quarter placement, record source document/version/page, and obtain Filipino/English/Science/Math/AP/GMRC subject-expert confirmation.

#### 4. Factual/civic wording requires subject-expert correction

Confirmed high-risk examples:

- `makabansa-g4-future-02-q01`: says Filipino was declared the official language of communication and education. This needs constitutionally precise treatment of **Filipino and English as official languages for communication and instruction**, with Filipino as the national language.
- `makabansa-g4-future-02-q02`: calls the eight entities represented by the sun’s rays “walong lalawigan,” but the keyed explanation includes **Maynila**, requiring historically precise wording such as the first eight areas/provinces placed under martial law or that revolted, based on the chosen authoritative source.
- `makabansa-g4-future-04-q01`: attributes “buhay, kalayaan, at kapanatagan ng sarili” to the Philippine Bill of Rights. The constitutional wording is life, liberty, or property under due process; the present phrasing appears to blend frameworks.
- `makabansa-g4-future-03-q02`: states renewable sources “hindi nauubos at malinis,” an overgeneralization. Renewable sources replenish naturally but still have environmental impacts.

**Required fix:** re-source these items against authoritative Philippine government/constitutional materials and re-review all civic/science factual claims before release.

### P1 — major pedagogical revisions

#### 5. Micro-lessons are summaries, not complete explainers

All scripts fit the nominal word budget (50–66 words), which is broadly compatible with 30–40 seconds. However, most scripts repeat the title/objective, give one declarative rule, and close with a generic process line. The 24 English scripts repeat:

> “Watch closely as we break down the problem, pause to think through the steps, and verify your answer with evidence. Try the next challenge!”

The 24 Filipino scripts use an equivalent repeated ending. Many scripts never actually demonstrate the promised breakdown, model, worked example, misconception, or check-for-understanding.

**Required fix:** give each script one concrete example and visible reasoning sequence (model → think-aloud → answer check), remove objective-pasting, and vary narration naturally. Time rendered audio, not word count alone. Hints should cue a strategy without nearly supplying the answer.

#### 6. Strict language policy is not met

Top-level language labels correctly follow the requested split: Math/Science/English = English; Filipino/Makabansa/AP/GMRC = Filipino. Learner-facing Filipino records nevertheless contain avoidable English code-switching, including:

- `makabansa-g3-future-03`: “Rice Granary of the Philippines”
- `gmrc-g3-future-02-q04`: “Honesty is the best policy”
- `gmrc-g4-future-02-q01`: “3Rs,” “Reduce, Reuse, Recycle,” “reusable,” “single-use plastic”
- `gmrc-g4-future-02-q04`: “greenhouse gas emissions,” “climate change,” “watt”
- `gmrc-g4-future-03`: “scam,” “bank account,” “Growth Mindset,” “Critically Minded,” “I-verify bago i-share”
- `makabansa-g4-future-03`: “Sustainable Development,” “Renewable Energy,” “Open-Pit Mining,” “crop rotation,” “open burning”

Some borrowed terms may be pedagogically useful, but they violate a *strict* FIL policy unless paired with clear Filipino-first equivalents and governed by an approved terminology rule.

**Required fix:** use Filipino-first learner text; retain an English term only where curriculum-approved and immediately define it in Filipino. Extend validation from checking language metadata to checking all learner-facing strings.

#### 7. Cognitive demand is uneven and often recognition-only

Math items generally use concrete computation and plausible errors. English/Filipino language items include useful sentence-level application. In contrast, many GMRC and civic items use one obviously virtuous response against three implausible or extreme behaviors (destroying property, violence, ridicule, blatant lying). These do not adequately assess judgment, perspective taking, or transfer.

**Required fix:** replace extreme distractors with age-appropriate, plausible near-misses; add short scenarios where two responses seem reasonable but one best satisfies the principle; avoid distressing or stigmatizing disability-related distractors.

#### 8. Remediation ladders need clearer scaffold progression

All links and three-step structures are valid, but several ladders are ordered as task retry → representation → prerequisite review rather than a clearly adaptive least-to-most or most-to-least scaffold. Some actions also assume unverified UI affordances (for example, on-screen blocks) or adult-facilitated activities.

**Required fix:** define the intended progression and trigger for each level; verify every referenced manipulative exists offline in the Android lesson player; make each step observable and measurable; include an exit check before returning to the assessment.

#### 9. Parent reports are technically complete but too generic

The 48 records have appropriate language and metrics, but most prompts are title substitutions in a fixed frame. They do not define mastery thresholds, translate metrics into actionable evidence, or provide a concrete home activity.

**Required fix:** add unit-specific exemplars, plain-language evidence statements, a brief no-special-materials home activity, and safeguards against deficit labeling. Report hint/remediation use as support data, not as learner weakness by itself.

### P2 — quality and validation improvements

- Replace language auto-detection based only on metadata with learner-string scanning plus an approved bilingual glossary.
- Add checks for answer-sequence predictability, keyed-option length cues, parallel option construction, and multiple-defensible answers.
- Add factual-source fields at assessment level for civic, science, health, and safety claims.
- Add audio-duration validation for scripts and hints; the JSON target duration alone does not prove timing.
- Add semantic review gates; the current two tests mainly cover counts and structural validation.
- Preserve local Philippine contexts, but verify that examples do not overgeneralize one region, family structure, religion, disability, or socioeconomic experience.

## Age appropriateness and inclusion

**Broadly suitable:** place value, fractions, area, ecosystems, matter, reading comprehension, Filipino grammar, community, honesty, empathy, environmental stewardship, and media literacy are reasonable for Grades 3–4 when properly sequenced.

**Needs calibration:** terms such as *dipterocarp*, *syntactic*, *intangible cultural heritage*, *open-pit mining*, *greenhouse gas emissions*, constitutional rights language, and bank-account scams require child-friendly definition and visual/concrete examples. Long multi-clause options can create a reading-load confound, particularly when the objective is Science, GMRC, or AP rather than reading comprehension.

**Inclusion concern:** disability scenarios intend inclusion, but distractors involving mockery, fear, or isolation can unnecessarily rehearse harmful behavior. Prefer realistic barriers and supportive choices without sensationalizing harm.

## Strengths worth preserving

- Complete 48-unit architecture with balanced grade and learning-area coverage.
- Strong internal referential integrity and zero missing skill-graph links.
- Exact four-items-per-unit assessment coverage and zero exact duplicate prompts.
- Math assessments generally target actual calculation rather than meta-recognition.
- Frequent use of Philippine settings, organisms, currency, community institutions, and civic contexts.
- Remediation and parent-report concepts are integrated from the design stage rather than added after release.
- All records remain explicitly marked `FUTURE_DRAFT_REQUIRES_EDUCATOR_REVIEW`; no premature approval metadata was found.

## Release recommendation and sign-off

### Educator decision

**SIGN-OFF WITHHELD.** Do not promote these 48 units to released/educator-validated status in their current form.

### Minimum re-review entry criteria

1. Scramble all MCQ options and prove that unit-level key sequences are not predictable.
2. Reduce keyed-answer length cues and replace implausible distractors.
3. Correct and source the identified civic/science factual statements.
4. Complete exact MATATAG competency/code mapping for all 48 units.
5. Enforce the strict EN/FIL policy across every learner-facing field.
6. Reauthor each micro-lesson as a genuine worked explainer and verify rendered duration.
7. Subject-expert review all 192 items for one uniquely defensible answer and objective alignment.
8. Re-run validator/tests plus the new semantic and bias gates, then conduct a small learner usability/read-aloud trial with Grades 3–4.

**Integration readiness:** structurally ready for continued development; **not ready for learner release or a DepEd-alignment claim**.
