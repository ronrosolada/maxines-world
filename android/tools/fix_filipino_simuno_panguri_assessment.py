#!/usr/bin/env python3
"""Fix Filipino simuno/panaguri assessment prompts - 32 lessons.

Replaces generic title-substituted prompts with objective-specific questions.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

LESSON_DIR = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"

# The 32 affected lesson IDs (from audit)
SIMUNO_PANGGURI_LESSONS = [
    "filipino-g3-q1-w01-d01", "filipino-g3-q1-w01-d02", "filipino-g3-q1-w01-d03",
    "filipino-g3-q1-w02-d01", "filipino-g3-q1-w02-d02", "filipino-g3-q1-w02-d03",
    "filipino-g3-q1-w02-d05", "filipino-g3-q1-w03-d02",
    "filipino-g3-q2-w04-d02", "filipino-g3-q2-w04-d03", "filipino-g3-q2-w04-d04",
    "filipino-g3-q2-w05-d03", "filipino-g3-q2-w05-d04",
    "filipino-g3-q2-w06-d01", "filipino-g3-q2-w06-d03", "filipino-g3-q2-w06-d04",
    "filipino-g3-q3-w08-d02", "filipino-g3-q3-w08-d03", "filipino-g3-q3-w08-d04",
    "filipino-g3-q3-w09-d02", "filipino-g3-q3-w09-d03", "filipino-g3-q3-w09-d04",
    "filipino-g3-q3-w10-d01",
    "filipino-g3-q4-w11-d01", "filipino-g3-q4-w11-d03", "filipino-g3-q4-w11-d04",
    "filipino-g3-q4-w12-d01", "filipino-g3-q4-w12-d04",
    "filipino-g3-q4-w13-d01", "filipino-g3-q4-w13-d02", "filipino-g3-q4-w13-d04",
    "filipino-g3-q4-w14-d01",
]

# Objective-specific assessment templates for simuno/panaguri
# Each template has: (prompt, options_list, correct_index, explanation)
#
# CONVENTION (must hold for every panaguri question):
#   - "simuno"  = the subject (who/what the sentence is about)
#   - "panaguri" = the COMPLETE predicate phrase including the linker,
#                  e.g. "ay tumatakbo", "ay nagbabasa ng libro"
# Distractors never duplicate or near-duplicate the keyed answer:
#   - simuno questions: distractors are predicates or other nouns
#   - panaguri questions: distractors are subjects / nouns / adverbs
# Correct positions are varied (a/b/c/d) but correctness drives ordering.
ASSESSMENT_TEMPLATES = [
    # Template 0 - correct: a(0)
    (
        "Alin ang simuno sa pangungusap na 'Si Ana ay nagbabasa'?",
        ["Si Ana", "ay nagbabasa", "Ang aklat", "Ang aso"],
        0,
        "Ang simuno ay 'Si Ana' — siya ang pinag-uusapan sa pangungusap."
    ),
    # Template 1 - correct: b(1)
    (
        "Alin ang panaguri sa pangungusap na 'Ang aso ay tumatakbo'?",
        ["Ang aso", "ay tumatakbo", "Ang mga bata", "mabilis"],
        1,
        "Ang panaguri ay 'ay tumatakbo' — ito ang nagsasabi tungkol sa simuno."
    ),
    # Template 2 - correct: c(2)
    (
        "Sa pangungusap na 'Si Milo ay natututo', alin ang simuno at alin ang panaguri?",
        ["Simuno: natututo; Panaguri: Si Milo", "Simuno: ay; Panaguri: natututo", "Simuno: Si Milo; Panaguri: ay natututo", "Simuno: Si Milo; Panaguri: ay"],
        2,
        "Ang simuno ay 'Si Milo' (pinag-uusapan), ang panaguri ay 'ay natututo' (nagsasabi tungkol kay Milo)."
    ),
    # Template 3 - correct: d(3)
    (
        "Alin sa sumusunod ang tamang paghahati ng simuno at panaguri?",
        ["Ang mga bata ay / naglalaro", "Ang mga / bata ay naglalaro", "Ang / mga bata ay naglalaro", "Ang mga bata / ay naglalaro"],
        3,
        "Ang tamang paghahati: 'Ang mga bata' (simuno) / 'ay naglalaro' (panaguri)."
    ),
    # Template 4 - correct: a(0)
    (
        "Kung ang simuno ay 'Ang pusa', alin ang maaaring panaguri?",
        ["ay natutulog sa sofa", "Ang pusa", "Ang sofa", "mabilis"],
        0,
        "Ang panaguri ay nagsasabi tungkol sa simuno: 'ay natutulog sa sofa'."
    ),
    # Template 5 - correct: b(1)
    (
        "Alin ang simuno sa pangungusap na 'Ang guro ay nagtuturo'?",
        ["Ang aklat", "Ang guro", "ay nagtuturo", "Ang mga bata"],
        1,
        "Ang simuno ay 'Ang guro' — siya ang pinag-uusapan."
    ),
    # Template 6 - correct: b(1)
    (
        "Alin ang panaguri sa 'Si Lolo ay nagbabasa ng libro'?",
        ["Si Lolo", "ay nagbabasa ng libro", "Ang libro", "Ang bahay"],
        1,
        "Ang panaguri ay 'ay nagbabasa ng libro' — nagsasabi tungkol sa ginagawa ni Lolo."
    ),
    # Template 7 - correct: d(3)
    (
        "Sa 'Ang bata ay tumatakbo pababa', alin ang tamang paghahati?",
        ["Ang bata ay / tumatakbo pababa", "Ang bata / tumatakbo pababa", "Ang / bata ay tumatakbo pababa", "Ang bata / ay tumatakbo pababa"],
        3,
        "Simuno: 'Ang bata' | Panaguri: 'ay tumatakbo pababa'."
    ),
    # Template 8 - correct: a(0)
    (
        "Kung ang simuno ay 'Ang mga ibon', alin ang panaguri?",
        ["ay lumilipad sa langit", "Ang mga ibon", "Ang langit", "Sa langit"],
        0,
        "Ang panaguri ay 'ay lumilipad sa langit' — nagsasabi tungkol sa ibon."
    ),
    # Template 9 - correct: b(1)
    (
        "Alin ang tamang simuno-panaguri sa 'Si Nanay ay nagluluto'?",
        ["Simuno: nagluluto; Panaguri: Si Nanay", "Simuno: Si Nanay; Panaguri: ay nagluluto", "Simuno: ay; Panaguri: nagluluto", "Simuno: Si Nanay; Panaguri: ay"],
        1,
        "Ang simuno ay 'Si Nanay', ang panaguri ay 'ay nagluluto'."
    ),
    # Template 10 - correct: c(2)
    (
        "Sa pangungusap na 'Ang aso ay tumatakbo', ano ang panaguri?",
        ["Ang aso", "Ang takbo", "ay tumatakbo", "mabilis"],
        2,
        "Ang panaguri ay 'ay tumatakbo' — ito ang kilos na ginawa ng simuno."
    ),
    # Template 11 - correct: d(3)
    (
        "Alin ang tamang paghahati: 'Si Tatay ay nagmamaneho'?",
        ["Si Tatay ay / nagmamaneho", "Si Tatay / nagmamaneho", "Si / Tatay ay nagmamaneho", "Si Tatay / ay nagmamaneho"],
        3,
        "Simuno: 'Si Tatay' | Panaguri: 'ay nagmamaneho'."
    ),
    # Template 12 - correct: a(0)
    (
        "Kung ang simuno ay 'Ang bibe', alin ang panaguri?",
        ["ay lumalangoy sa lawa", "Ang bibe", "Ang lawa", "Sa lawa"],
        0,
        "Ang panaguri ay nagsasabi tungkol sa gawain ng bibe."
    ),
    # Template 13 - correct: b(1)
    (
        "Alin ang simuno sa 'Ang mga bata ay naglalaro'?",
        ["naglalaro sa labas", "Ang mga bata", "ay naglalaro", "Ang laruan"],
        1,
        "Ang simuno ay 'Ang mga bata' — sila ang pinag-uusapan."
    ),
    # Template 14 - correct: c(2)
    (
        "Ano ang panaguri sa 'Si Lola ay nagbabasa'?",
        ["Si Lola", "Ang libro", "ay nagbabasa", "Ang tahanan"],
        2,
        "Ang panaguri ay 'ay nagbabasa' — ito ang gawain ni Lola."
    ),
]


def load_lesson(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def save_lesson(path: Path, lesson: dict):
    path.write_text(json.dumps(lesson, ensure_ascii=False, indent=2), encoding="utf-8")


def fix_simuno_pangnguri_assessment(lesson: dict, template_idx: int) -> dict:
    """Replace the 5 generic assessment items with objective-specific ones."""
    assessment = lesson.get("assessment", {})
    items = assessment.get("items", [])
    
    # We'll use the same template set rotated for variety
    new_items = []
    for j in range(5):
        tpl_idx = (template_idx + j) % len(ASSESSMENT_TEMPLATES)
        prompt, options, correct_idx, explanation = ASSESSMENT_TEMPLATES[tpl_idx]
        
        # Build options with IDs a, b, c, d
        option_objects = []
        for k, text in enumerate(options):
            option_objects.append({
                "id": chr(ord('a') + k),
                "text": text
            })
        
        correct_id = chr(ord('a') + correct_idx)
        
        new_items.append({
            "itemId": f"{lesson['lessonId']}-q{j+1:02d}",
            "sequence": j + 1,
            "type": "MULTIPLE_CHOICE",
            "prompt": prompt,
            "options": option_objects,
            "correctOptionIds": [correct_id],
            "explanation": explanation
        })
    
    assessment["items"] = new_items
    lesson["assessment"] = assessment
    return lesson


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Report planned changes without writing")
    parser.add_argument("--dry-run", action="store_true", help="Alias for --check")
    args = parser.parse_args()
    
    if args.check or args.dry_run:
        print(f"Would fix {len(SIMUNO_PANGGURI_LESSONS)} lessons:")
        for lid in SIMUNO_PANGGURI_LESSONS:
            print(f"  {lid}")
        return 0
    
    fixed = 0
    errors = 0
    for lid in SIMUNO_PANGGURI_LESSONS:
        path = LESSON_DIR / f"{lid}.json"
        if not path.exists():
            print(f"NOT FOUND: {lid}")
            errors += 1
            continue
        
        lesson = load_lesson(path)
        # Use lesson index for template rotation
        idx = SIMUNO_PANGGURI_LESSONS.index(lid)
        lesson = fix_simuno_pangnguri_assessment(lesson, idx)
        save_lesson(path, lesson)
        fixed += 1
    
    print(f"Fixed {fixed} lessons. Errors: {errors}")
    return 0 if errors == 0 else 1


if __name__ == "__main__":
    import argparse
    sys.exit(main())