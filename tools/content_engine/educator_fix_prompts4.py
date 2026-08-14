#!/usr/bin/env python3
"""Educator pass 4: last three duplicate-prompt groups."""
import json
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

PROMPTS = {
    ("mathematics-g3-m01-d04", "mathematics-g3-m01-d04-q02"): "Which of these comparisons is true?",
    ("mathematics-g3-q3-w05-d01", "mathematics-g3-q3-w05-d01-q01"): "Which sentence about chance is correct?",
    ("mathematics-g3-q3-w05-d01", "mathematics-g3-q3-w05-d01-q02"): "Which statement about chance is right?",
    ("mathematics-g3-q3-w05-d01", "mathematics-g3-q3-w05-d01-q03"): "Which chance sentence is true?",
    ("mathematics-g3-q3-w05-d01", "mathematics-g3-q3-w05-d01-q04"): "Which one tells the chance correctly?",
    ("mathematics-g3-q3-w05-d01", "mathematics-g3-q3-w05-d01-q05"): "Which sentence shows the right chance?",
    ("mathematics-g3-q3-w05-d03", "mathematics-g3-q3-w05-d03-q01"): "Which sentence about chance is right?",
    ("mathematics-g3-q3-w05-d03", "mathematics-g3-q3-w05-d03-q02"): "Which of these chance statements is true?",
    ("mathematics-g3-q3-w05-d03", "mathematics-g3-q3-w05-d03-q03"): "Which chance statement is correct?",
    ("mathematics-g3-q3-w05-d03", "mathematics-g3-q3-w05-d03-q04"): "Which one about chance is right?",
    ("mathematics-g3-q3-w05-d03", "mathematics-g3-q3-w05-d03-q05"): "Which sentence tells the chance correctly?",
}

def main():
    stats = {"prompts": 0, "lessons": 0}
    for p in sorted(LESSONS.glob("*.json")):
        d = json.loads(p.read_text(encoding="utf-8"))
        changed = False
        for it in d.get("assessment", {}).get("items", []):
            key = (d["lessonId"], it.get("itemId"))
            if key in PROMPTS:
                it["prompt"] = PROMPTS[key]
                stats["prompts"] += 1
                changed = True
        if changed:
            stats["lessons"] += 1
            p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")
    print(stats)

if __name__ == "__main__":
    main()
