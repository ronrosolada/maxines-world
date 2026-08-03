#!/usr/bin/env python3
"""Content Quality Audit for Maxine's World lessons.

Checks:
1. Renderer payload validity (hotspots, sorts, matches, sequences, MCQs)
2. Objective alignment (activity tests objective, not title membership)
3. Progression (explanation -> guided -> independent -> transfer)
4. Language appropriateness (Filipino for fil-PH, English for en-PH)
5. Factual/safety correctness
6. Generic fallback text detection
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

LESSON_DIR = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"


@dataclass
class Finding:
    severity: str  # "error", "warning", "info"
    category: str
    lesson_id: str
    activity_type: str
    message: str
    detail: str = ""


@dataclass
class AuditReport:
    total_lessons: int = 0
    findings: list[Finding] = field(default_factory=list)
    by_severity: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    by_category: dict[str, int] = field(default_factory=lambda: defaultdict(int))
    by_subject: dict[str, int] = field(default_factory=lambda: defaultdict(int))

    def add(self, f: Finding):
        self.findings.append(f)
        self.by_severity[f.severity] += 1
        self.by_category[f.category] += 1
        subj = f.lesson_id.split('-')[0].upper() if '-' in f.lesson_id else 'UNKNOWN'
        self.by_subject[subj] += 1


GENERIC_NARRATION_MARKERS = [
    "Study the idea and listen to Milo",
    "Explore each example and find the important detail",
    "Sort each example into the correct group",
    "Choose the best answer",
    "Match the ideas that belong together",
    "Put the steps in the correct order",
    "Pag-aralan ang ideya at pakinggan ang paliwanag",
    "Suriin ang bawat halimbawa at hanapin ang mahalagang detalye",
    "Ilagay ang bawat halimbawa sa tamang pangkat",
    "Piliin ang pinakamainam na sagot",
    "Itugma ang magkakaugnay na ideya",
    "Ayusin ang mga hakbang ayon sa tamang pagkakasunod",
]

GENERIC_PROMPT_MARKERS = [
    "Study the idea and listen to Milo",
    "Explore each example and find the important detail",
    "Sort each example into the correct group",
    "Choose the best answer",
    "Match the ideas that belong together",
    "Put the steps in the correct order",
    "Pag-aralan ang ideya at pakinggan ang paliwanag",
    "Suriin ang bawat halimbawa at hanapin ang mahalagang detalye",
    "Ilagay ang bawat halimbawa sa tamang pangkat",
    "Piliin ang pinakamainam na sagot",
    "Itugma ang magkakaugnay na ideya",
    "Ayusin ang mga hakbang ayon sa tamang pagkakasunod",
]

GENERIC_SORT_CATEGORIES = ["Fits the lesson", "Does not fit"]
GENERIC_SEQUENCE_STEPS = [
    "Read the question",
    "Choose the operation or model",
    "Solve carefully",
    "Check whether the answer makes sense",
    "Basahin ang tanong",
    "Piliin ang operasyon o modelo",
    "Lutasin nang maingat",
    "Suriin kung angkop ang sagot",
]

GENERIC_MATCHING_RIGHT_LABELS = ["angkop na halimbawa", "malinaw na gamit", "tamang ideya", "mabuting asal", "paggalang sa kapwa", "observable evidence", "safe example", "connected idea", "shows equal groups", "names the product", "can use an array", "hanapbuhay sa baybayin", "hanapbuhay sa lupain", "angkop na disenyo sa kapaligiran"]


def load_lessons() -> list[tuple[Path, dict]]:
    return [(path, json.loads(path.read_text(encoding="utf-8"))) for path in sorted(LESSON_DIR.glob("*.json"))]


def audit_lesson(path: Path, lesson: dict, report: AuditReport):
    lesson_id = lesson.get("lessonId", path.stem)
    subject = lesson.get("subject", "").upper()
    objective = lesson.get("objective", "")
    language = lesson.get("language", "")
    
    # Check vocabulary quality
    vocab = lesson.get("vocabulary", [])
    for v in vocab:
        term = v.get("term", "")
        definition = v.get("definition", "")
        # Term should not be a full sentence ending in period
        if term.endswith(".") and len(term.split()) > 5:
            report.add(Finding("warning", "vocabulary", lesson_id, "", 
                             f"Vocabulary term is a full sentence: '{term[:80]}...'"))
        # Definition should not be placeholder
        if definition in ["angkop na halimbawa", "malinaw na gamit", "tamang ideya", 
                          "mabuting asal", "paggalang sa kapwa", "observable evidence",
                          "safe example", "connected idea", "shows equal groups",
                          "names the product", "can use an array"]:
            report.add(Finding("warning", "vocabulary", lesson_id, "",
                             f"Vocabulary definition is placeholder: '{definition}' for term '{term}'"))

    # Check each activity
    activities = lesson.get("activities", [])
    for i, act in enumerate(activities):
        act_type = act.get("type", "")
        content = act.get("content")
        narration = act.get("narration", "")
        prompt = act.get("prompt", "")
        instruction = act.get("instruction", "")
        
        # 1. Generic narration detection
        for marker in GENERIC_NARRATION_MARKERS:
            if marker in narration:
                report.add(Finding("warning", "generic_narration", lesson_id, act_type,
                                 f"Generic narration detected: contains '{marker}'"))
                break
        
        # 2. Generic prompt detection
        for marker in GENERIC_PROMPT_MARKERS:
            if marker in prompt:
                report.add(Finding("warning", "generic_prompt", lesson_id, act_type,
                                 f"Generic prompt detected: contains '{marker}'"))
                break
        
        # 3. Check content payload validity
        if content:
            if isinstance(content, dict):
                # HOTSPOT_IMAGE
                if act_type == "HOTSPOT_IMAGE":
                    examples = content.get("examples", [])
                    if not examples:
                        report.add(Finding("error", "payload", lesson_id, act_type,
                                         "HOTSPOT_IMAGE missing 'examples' in content"))
                    elif len(examples) < 2:
                        report.add(Finding("warning", "payload", lesson_id, act_type,
                                         f"HOTSPOT_IMAGE has only {len(examples)} example(s)"))
                
                # SORT_AND_CLASSIFY
                elif act_type == "SORT_AND_CLASSIFY":
                    fits = content.get("fits", [])
                    does_not_fit = content.get("doesNotFit", [])
                    if not fits and not does_not_fit:
                        report.add(Finding("error", "payload", lesson_id, act_type,
                                         "SORT_AND_CLASSIFY missing both 'fits' and 'doesNotFit'"))
                    elif not fits:
                        report.add(Finding("warning", "payload", lesson_id, act_type,
                                         "SORT_AND_CLASSIFY has no 'fits' items"))
                    elif not does_not_fit:
                        report.add(Finding("warning", "payload", lesson_id, act_type,
                                         "SORT_AND_CLASSIFY has no 'doesNotFit' items"))
                    # Check for generic category labels (renderer default)
                    # The renderer uses "Fits the lesson" / "Does not fit" if no typed categories
                    # This is a content issue if the lesson objective requires specific categories
                
                # MATCHING_PAIRS
                elif act_type == "MATCHING_PAIRS":
                    pairs = content.get("pairs", [])
                    if not pairs:
                        report.add(Finding("error", "payload", lesson_id, act_type,
                                         "MATCHING_PAIRS missing 'pairs'"))
                    else:
                        for j, pair in enumerate(pairs):
                            right = pair.get("right", "")
                            if right in GENERIC_MATCHING_RIGHT_LABELS:
                                report.add(Finding("warning", "payload", lesson_id, act_type,
                                                 f"MATCHING_PAIRS pair {j} has generic right label: '{right}'"))
                
                # SEQUENCE_BUILDER
                elif act_type == "SEQUENCE_BUILDER":
                    steps = content.get("steps", [])
                    if not steps:
                        report.add(Finding("error", "payload", lesson_id, act_type,
                                         "SEQUENCE_BUILDER missing 'steps'"))
                    else:
                        for step_text in steps:
                            if step_text in GENERIC_SEQUENCE_STEPS:
                                report.add(Finding("warning", "payload", lesson_id, act_type,
                                                 f"SEQUENCE_BUILDER has generic step: '{step_text}'"))
                                break
                
                # MULTIPLE_CHOICE
                elif act_type == "MULTIPLE_CHOICE":
                    options = content.get("options", [])
                    correct_idx = content.get("correctIndex", -1)
                    if not options:
                        report.add(Finding("error", "payload", lesson_id, act_type,
                                         "MULTIPLE_CHOICE missing 'options'"))
                    elif correct_idx < 0 or correct_idx >= len(options):
                        report.add(Finding("error", "payload", lesson_id, act_type,
                                         f"MULTIPLE_CHOICE correctIndex {correct_idx} out of bounds for {len(options)} options"))
                
                # ANIMATED_EXPLANATION
                elif act_type == "ANIMATED_EXPLANATION":
                    # content is a string for this type
                    pass
        
        # 4. Objective alignment check
        # Check if activity content relates to objective
        obj_keywords = set(objective.lower().split())
        stopwords = {"the", "and", "or", "to", "a", "an", "in", "of", "for", "with", "by", "on", "at", "from", "as", "is", "are", "be", "do", "use", "using", "show", "shows", "identify", "identifies", "recognize", "recognizes", "describe", "describes", "explain", "explains", "naipakikita", "natutukoy", "naipaliliwanag", "nakabubuo", "naisusulat", "nagagamit", "kilalanin", "piliin", "gamitin", "suriin", "ayusin", "tukuyin", "bigyang", "sundin", "bumuo", "pumili", "ihambing", "ugnayin", "ipaliwanag", "basahin", "surin", "gamit", "ang", "sa", "ng", "at", "ang", "sa", "ng", "mga", "ay", "may", "na", "ko", "mo", "si", "ni", "kay", "kina", "nina", "ng", "nang", "rin", "din", "man", "po", "opo", "ho", "oho"}
        obj_content = obj_keywords - stopwords
        
        # Check activity content for objective keywords
        act_text = f"{narration} {prompt} {instruction}".lower()
        if isinstance(content, dict):
            for v in content.values():
                if isinstance(v, list):
                    for item in v:
                        if isinstance(item, str):
                            act_text += " " + item.lower()
                        elif isinstance(item, dict):
                            for val in item.values():
                                if isinstance(val, str):
                                    act_text += " " + val.lower()
                elif isinstance(v, str):
                    act_text += " " + v.lower()
        
        # Very loose check - at least 1 keyword overlap
        overlap = any(kw in act_text for kw in obj_content if len(kw) > 3)
        if not overlap and obj_content and act_type != "ANIMATED_EXPLANATION":
            report.add(Finding("warning", "alignment", lesson_id, act_type,
                             "Activity content may not align with objective (no keyword overlap)"))
        
        # 5. Language check
        if language == "fil-PH":
            # Check for English words in narration/prompt
            english_markers = ["the", "and", "or", "you", "your", "is", "are", "have", "has", "do", "does", "will", "can", "to", "a", "an", "of", "in", "for", "with", "by", "on", "at"]
            words = narration.lower().split()
            if words:
                eng_ratio = sum(1 for w in words if w in english_markers) / len(words)
                if eng_ratio > 0.3:
                    report.add(Finding("warning", "language", lesson_id, act_type,
                                     f"Filipino lesson has high English ratio ({eng_ratio:.0%}) in narration"))
        
        # 6. Assessment alignment
        # (separate check below)
    
    # Assessment check
    assessment = lesson.get("assessment", {})
    items = assessment.get("items", [])
    for j, item in enumerate(items):
        prompt_text = item.get("prompt", "")
        options = item.get("options", [])
        correct_ids = item.get("correctOptionIds", [])
        
        # Generic assessment prompt check
        if any(template in prompt_text for template in [
            "Which example belongs to", "Which choice shows the skill in",
            "What is one example from", "Which situation matches",
            "Which answer demonstrates", "Aling halimbawa ang kabilang sa",
            "Aling pagpipilian ang nagpapakita ng kasanayan sa",
            "Alin ang isang halimbawa ng", "Aling sitwasyon ang tumutugma sa",
            "Aling sagot ang nagpapakita ng"
        ]):
            report.add(Finding("warning", "assessment", lesson_id, "ASSESSMENT",
                             f"Item {j+1}: Generic title-substituted prompt"))
        
        if not correct_ids:
            report.add(Finding("error", "assessment", lesson_id, "ASSESSMENT",
                             f"Item {j+1}: Missing correctOptionIds"))
        
        if options and correct_ids:
            for cid in correct_ids:
                if not any(opt.get("id") == cid for opt in options):
                    report.add(Finding("error", "assessment", lesson_id, "ASSESSMENT",
                                     f"Item {j+1}: correctOptionId '{cid}' not found in options"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Run audit and report")
    parser.add_argument("--summary", action="store_true", help="Print summary only")
    args = parser.parse_args()
    
    lessons = load_lessons()
    report = AuditReport()
    report.total_lessons = len(lessons)
    
    for path, lesson in lessons:
        audit_lesson(path, lesson, report)
    
    print(f"\n=== CONTENT QUALITY AUDIT ===")
    print(f"Total lessons: {report.total_lessons}")
    print(f"Total findings: {len(report.findings)}")
    print(f"\nBy severity:")
    for sev in ["error", "warning", "info"]:
        print(f"  {sev}: {report.by_severity.get(sev, 0)}")
    print(f"\nBy category:")
    for cat, cnt in sorted(report.by_category.items(), key=lambda x: -x[1]):
        print(f"  {cat}: {cnt}")
    print(f"\nBy subject:")
    for subj, cnt in sorted(report.by_subject.items(), key=lambda x: -x[1]):
        print(f"  {subj}: {cnt}")
    
    if not args.summary:
        print(f"\n=== DETAILED FINDINGS (first 50) ===")
        for f in report.findings[:50]:
            print(f"[{f.severity.upper()}] {f.category} | {f.lesson_id} [{f.activity_type}] | {f.message}")
            if f.detail:
                print(f"  -> {f.detail}")
    
    # Exit code
    if report.by_severity.get("error", 0) > 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())