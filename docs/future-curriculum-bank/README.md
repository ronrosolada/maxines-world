# Future Grade 3–4 Curriculum Bank

Isolated, non-runtime, machine-readable draft bank. It does not modify Android UI or active content. Corpus: 48 units (24 per grade; four per subject group), 192 MCQs, 48 remediation ladders, 48 30–40s explainers with 5–8s hints, and parent-report data for every unit. Grade 4 social studies is **Araling Panlipunan**, not Makabansa.

## Files

- `roadmap.json` — objectives, prerequisite/remediation links, provenance status
- `assessment-bank.json` — four original, topic-specific MCQs per unit
- `remediation-ladders.json`, `micro-lessons.json`, `parent-report-templates.json`
- `schema.json`, `validate_bank.py`, `test_validate_bank.py`, `build_bank.py`

## Provenance and limitations

The framework and URLs below are sourced. Unit wording, scripts, questions, distractors, explanations, ladders, and reports are original authored drafts. Topic-level alignment is not an official competency-code claim. Exact DepEd codes remain a documented gap and require educator verification against the PDFs before release. SG/US crosswalk text is intentionally limited to English/Math/Science and is not asserted as equivalence.

Official sources:
- DepEd index: https://www.deped.gov.ph/revised-k-to-10-curriculum/
- Science: https://www.deped.gov.ph/wp-content/uploads/13-FINAL-MATATAG-Science-CG-2023-Grades-3-10.pdf
- Mathematics: https://www.deped.gov.ph/wp-content/uploads/9-FINAL-MATATAG-Mathematics-CG-2023-Grades-1-10.pdf
- English: https://www.deped.gov.ph/wp-content/uploads/2-FINAL-MATATAG-English-CG-2023-Grades-2-10-withAppendices.pdf
- Filipino: https://www.deped.gov.ph/wp-content/uploads/4-FINAL-MATATAG-FILIPINO-CG-2023-Grades-2-10.pdf
- GMRC: https://www.deped.gov.ph/wp-content/uploads/5-FINAL-MATATAG-GMRC-and-VE-CG-2023-Grades-1-10.pdf
- Makabansa Grades 1–3: https://www.deped.gov.ph/wp-content/uploads/8-FINAL-MATATAG-Makabansa-CG-2023-Grades-1-3.pdf
- Araling Panlipunan Grades 4–10: https://www.deped.gov.ph/wp-content/uploads/1-FINAL-MATATAG-Araling-Panlipunan-CG-2023-Grades-4-10.pdf
- Singapore syllabus index: https://www.moe.gov.sg/primary/curriculum/syllabus
- CCSS Math: https://corestandards.org/mathematics-standards
- CCSS ELA: https://corestandards.org/english-language-arts-standards
- NGSS Grade 3: https://nextgenscience.org/3rd-grade-topics-model
- NGSS Grade 4: https://nextgenscience.org/sites/default/files/4%20combined%20DCI%20standardsf.pdf

Run `python3 build_bank.py && python3 validate_bank.py && python3 -m unittest test_validate_bank.py`.
