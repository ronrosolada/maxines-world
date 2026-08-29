#!/usr/bin/env python3
"""Wave C pilot: append WRITING_PRODUCTION production activities.

CH-07 M2: production objectives (writing/producing tasks) exist in the
curriculum mapping but no activity type assessed them. This pilot appends
one WRITING_PRODUCTION activity (sentence-builder tiles + self-mark
checklist, no free-text grading) to six lessons whose objectives are
production-shaped:

  - English capital/end-punctuation lessons (4)
  - Filipino talata lessons (2)

The activity rides AFTER the canonical six, exactly where the validator
allows optional activity types (WRITING_PRODUCTION, VIDEO). Idempotent:
lessons that already carry a WRITING_PRODUCTION activity are skipped.
"""
import json
import os
import sys

ROOT = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "content-pack", "month-01", "lessons")
)

# lesson_id -> (instruction, tiles, checklist, hint, accessibility)
PILOT = {
    "english-g3-q2-w04-d02": {
        "instruction": "Build the sentence with the word tiles. Start with a capital and end with a period!",
        "tiles": ["Milo", "reads", "a", "book", "."],
        "checklist": [
            "My sentence starts with a capital letter.",
            "My sentence ends with a period.",
            "My sentence tells a whole idea.",
        ],
        "hint": "A telling sentence starts with a capital letter and ends with a period.",
        "accessibility": "Build the sentence 'Milo reads a book.' from word tiles, then check your own writing with the checklist.",
    },
    "english-g3-q2-w04-d04": {
        "instruction": "Build the question with the word tiles. Don't forget the question mark!",
        "tiles": ["Where", "is", "the", "pencil", "?"],
        "checklist": [
            "My question starts with a capital letter.",
            "My question ends with a question mark.",
            "My question asks something.",
        ],
        "hint": "An asking sentence ends with a question mark.",
        "accessibility": "Build the question 'Where is the pencil?' from word tiles, then check your own writing with the checklist.",
    },
    "english-g3-q3-w11-d01": {
        "instruction": "Build the neighborhood sentence with the word tiles.",
        "tiles": ["Ana", "helps", "her", "neighbor", "."],
        "checklist": [
            "My sentence starts with a capital letter.",
            "My sentence ends with a period.",
            "My sentence tells a whole idea.",
        ],
        "hint": "Start with a capital letter and end with a period.",
        "accessibility": "Build the sentence 'Ana helps her neighbor.' from word tiles, then check your own writing with the checklist.",
    },
    "english-g3-q3-w11-d03": {
        "instruction": "Build the sentence with the word tiles, then check your own writing.",
        "tiles": ["Milo", "reads", "every", "day", "."],
        "checklist": [
            "My sentence starts with a capital letter.",
            "My sentence ends with a period.",
            "My sentence tells a whole idea.",
        ],
        "hint": "Every sentence needs a capital letter and the right end mark.",
        "accessibility": "Build the sentence 'Milo reads every day.' from word tiles, then check your own writing with the checklist.",
    },
    "filipino-g3-q1-w03-d01": {
        "instruction": "Buuin ang pangungusap gamit ang mga salita. Simulan sa malaking titik at tapusin sa tuldok!",
        "tiles": ["Ang", "pusa", "ko", "ay", "maputi", "."],
        "checklist": [
            "Nagsimula sa malaking titik ang pangungusap ko.",
            "May tuldok sa dulo ang pangungusap ko.",
            "May paksa at detalye ang pangungusap ko.",
        ],
        "hint": "Nagsisimula sa malaking titik ang pangungusap at nagtatapos sa tuldok.",
        "accessibility": "Buuin ang pangungusap na 'Ang pusa ko ay maputi.' gamit ang mga salita, pagkatapos ay suriin ang iyong pagsulat gamit ang checklist.",
    },
    "filipino-g3-q1-w03-d03": {
        "instruction": "Buuin ang pangungusap gamit ang mga salita. Simulan sa malaking titik at tapusin sa tuldok!",
        "tiles": ["Masaya", "ang", "pamilya", "ni", "Ana", "sa", "Sabado", "."],
        "checklist": [
            "Nagsimula sa malaking titik ang pangungusap ko.",
            "May tuldok sa dulo ang pangungusap ko.",
            "May paksa at detalye ang pangungusap ko.",
        ],
        "hint": "Nagsisimula sa malaking titik ang pangungusap at nagtatapos sa tuldok.",
        "accessibility": "Buuin ang pangungusap na 'Masaya ang pamilya ni Ana sa Sabado.' gamit ang mga salita, pagkatapos ay suriin ang iyong pagsulat gamit ang checklist.",
    },
}


def main() -> int:
    added = 0
    skipped = 0
    for lesson_id, spec in PILOT.items():
        path = os.path.join(ROOT, f"{lesson_id}.json")
        if not os.path.isfile(path):
            print(f"  WARN {lesson_id}: file missing", file=sys.stderr)
            continue
        with open(path, encoding="utf-8") as fh:
            lesson = json.load(fh)

        activities = lesson.get("activities")
        if not isinstance(activities, list):
            print(f"  WARN {lesson_id}: no activities list", file=sys.stderr)
            continue
        if any(
            isinstance(a, dict) and a.get("type") == "WRITING_PRODUCTION"
            for a in activities
        ):
            skipped += 1
            continue

        # Reuse the lesson's authored visual for continuity.
        visual_id = next(
            (
                a.get("assetId")
                for a in activities
                if isinstance(a, dict) and a.get("assetId")
            ),
            None,
        )

        sequence = len(activities) + 1
        activities.append(
            {
                "activityId": f"{lesson_id}-a{sequence:02d}",
                "sequence": sequence,
                "type": "WRITING_PRODUCTION",
                "capability": "WRITING_PRODUCTION_V1",
                "required": True,
                "assetId": visual_id,
                "instruction": spec["instruction"],
                "content": {
                    "tiles": spec["tiles"],
                    "checklist": spec["checklist"],
                },
                "completionRule": {"type": "COMPLETE"},
                "feedback": {
                    "correct": "Great job! Your sentence is ready! 🎉"
                    if lesson.get("language") == "en-PH"
                    else "Ang galing! Buo na ang iyong pangungusap! 🎉",
                    "retry": "Check your sentence again. 💪"
                    if lesson.get("language") == "en-PH"
                    else "Suriin muli ang iyong pangungusap. 💪",
                },
                "prompt": spec["instruction"],
                "narration": spec["instruction"],
                "guideHint": spec["hint"],
                "hint": spec["hint"],
                "nextLabel": "Next →"
                if lesson.get("language") == "en-PH"
                else "Susunod →",
                "accessibilityAlternative": spec["accessibility"],
            }
        )

        with open(path, "w", encoding="utf-8") as fh:
            json.dump(lesson, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
        added += 1
        print(f"  + {lesson_id}: appended WRITING_PRODUCTION (a{sequence:02d})")

    print(f"C pilot: {added} activities appended, {skipped} already present")
    return 0


if __name__ == "__main__":
    sys.exit(main())
