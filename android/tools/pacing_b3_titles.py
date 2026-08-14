#!/usr/bin/env python3
"""Wave B3: replace template-suffixed titles with unique child-facing names.

friendlyLessonTitleOf() already strips " · Q# W## D##" at display time, but
22 base names are duplicated across 51 files, so lesson lists show identical
labels. This pass writes clean, content-grounded, unique titles for all 69
template-titled lessons. Duplicate-content spiral reviews get an explicit
"Review" name. Idempotent via the titlePass marker.
"""
import json
import os
import re
import sys

ROOT = "/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons"
MARKER = "titles-b3-2026-08-15"
TPL = re.compile(r"\s*·\s*Q\d+\s*W\d+\s*D\d+\s*$")

# Duplicate-base files get distinct authored names.
AUTHORED = {
    "english-g3-q1-w01-d01": "The Fiesta Picture",
    "english-g3-q1-w01-d02": "Ana and the Brown Dog",
    "english-g3-q2-w07-d01": "The Red Kite",
    "english-g3-q3-w13-d04": "The Red Kite Review",
    "english-g3-q3-w13-d03": "Ana Shares Her Umbrella",
    "english-g3-q2-w02-d01": "Word Twins",
    "english-g3-q3-w08-d04": "Word Twins Review",
    "english-g3-q2-w03-d03": "Voice Clues",
    "english-g3-q3-w10-d02": "Voice Clues Review",
    "english-g3-q2-w03-d04": "Sentence Order",
    "english-g3-q3-w10-d03": "Sentence Order Review",
    "english-g3-q2-w04-d01": "Sentence Parts",
    "english-g3-q3-w10-d04": "Sentence Parts Review",
    "english-g3-q2-w04-d03": "Compound Sentence Crew",
    "english-g3-q3-w11-d02": "Compound Sentence Crew Review",
    "english-g3-q2-w05-d01": "Signal Words",
    "english-g3-q3-w11-d04": "Signal Words Review",
    "english-g3-q2-w05-d02": "Smooth Reading",
    "english-g3-q3-w12-d01": "Smooth Reading Review",
    "english-g3-q2-w05-d04": "Mangrove Fact Finders",
    "english-g3-q3-w12-d03": "Mangrove Facts Review",
    "english-g3-q2-w06-d01": "Kind Words: Good Morning",
    "english-g3-q2-w06-d04": "Kind Words: Please and Thank You",
    "english-g3-q3-w12-d04": "Kind Words Review",
    "english-g3-q2-w06-d03": "My Rainy Walk",
    "english-g3-q3-w13-d01": "My Garden Seed",
    "english-g3-q3-w13-d02": "The Little Puppy",
    "english-g3-q2-w07-d03": "Text Type Toolbox",
    "english-g3-q3-w14-d02": "Text Type Toolbox Review",
    "gmrc-g3-q1-w01-d03": "Ang Aking Higaan",
    "gmrc-g3-q4-w07-d03": "Ang Susi ng Silid-Aralan",
    "gmrc-g3-q1-w01-d04": "Pananalig at Paggalang",
    "gmrc-g3-q3-w05-d04": "Iba't Ibang Paraan ng Pagsamba",
    "gmrc-g3-q4-w07-d04": "Pananalig at Paggalang Review",
    "makabansa-g3-q1-w01-d01": "Ang Unang Barangay Hall",
    "makabansa-g3-q1-w01-d02": "Ang Unang Paaralan",
    "makabansa-g3-q1-w01-d03": "Ang Unang Pamilihan",
    "makabansa-g3-q4-w04-d01": "Tunog ng Paligid",
    "makabansa-g3-q4-w04-d02": "Ritmo at Awit ng Komunidad",
    "filipino-g3-q1-w06-d01": "Ang Tauhan sa Kuwento",
    "mathematics-g3-q1-w01-d01": "Point Detective",
    "mathematics-g3-q1-w01-d03": "Point Detective Review",
    "mathematics-g3-q1-w01-d04": "The Endless Ray",
    "mathematics-g3-q1-w02-d02": "Round 47 to 50",
    "mathematics-g3-q2-w04-d03": "Round 47 to 50 Review",
    "mathematics-g3-q3-w06-d02": "Round 68 to 70",
    "mathematics-g3-q3-w05-d01": "Chance Check",
    "mathematics-g3-q3-w05-d03": "Chance Check Review",
    "mathematics-g3-q3-w07-d03": "Pattern Path",
    "mathematics-g3-q3-w07-d04": "Pattern Path Review",
    "mathematics-g3-q4-w09-d02": "Fraction Models",
    "mathematics-g3-q4-w09-d03": "Fraction Models Review",
}


def main() -> int:
    authored = 0
    stripped = 0
    seen: dict[str, str] = {}
    errors = 0
    for name in sorted(os.listdir(ROOT)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(ROOT, name)
        with open(path, encoding="utf-8") as fh:
            lesson = json.load(fh)
        title = lesson.get("title", "")
        if not TPL.search(title):
            continue
        lesson_id = name[:-5]
        if lesson_id in AUTHORED:
            new_title = AUTHORED[lesson_id]
            authored += 1
        else:
            new_title = TPL.sub("", title).strip()
            stripped += 1
        if new_title in seen:
            print(
                f"  COLLISION: {new_title} already assigned to {seen[new_title]}",
                file=sys.stderr,
            )
            errors += 1
            continue
        seen[new_title] = lesson_id
        lesson["title"] = new_title
        lesson["titlePass"] = MARKER
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(lesson, fh, indent=2, ensure_ascii=False)
            fh.write("\n")

    print(f"B3: {authored} authored titles, {stripped} stripped titles, {errors} collisions")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
