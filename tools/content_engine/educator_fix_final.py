#!/usr/bin/env python3
"""Educator pass 3: final same-lesson prompt variants + activity-content screen bleed."""
import json
import re
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

PROMPTS = {
    ("mathematics-g3-m01-d04", "mathematics-g3-m01-d04-q05"): "Which comparison sign is correct?",
    ("mathematics-g3-m01-d08", "mathematics-g3-m01-d08-q05"): "Which shows the greater amount of money?",
    ("mathematics-g3-m01-d16", "mathematics-g3-m01-d16-q05"): "Which multiplication answer is correct?",
    ("mathematics-g3-q1-w02-d03", "mathematics-g3-q1-w02-d03-q01"): "Which comparison is true?",
    ("mathematics-g3-q1-w02-d03", "mathematics-g3-q1-w02-d03-q02"): "Which comparison is correct?",
    ("mathematics-g3-q1-w02-d03", "mathematics-g3-q1-w02-d03-q03"): "Which number sentence is true?",
    ("mathematics-g3-q1-w02-d03", "mathematics-g3-q1-w02-d03-q04"): "Which one shows the correct comparison?",
    ("mathematics-g3-q1-w02-d03", "mathematics-g3-q1-w02-d03-q05"): "Which pair of numbers is compared correctly?",
}

def main():
    stats = {"prompts": 0, "bleed": 0, "lessons": 0}
    for p in sorted(LESSONS.glob("*.json")):
        d = json.loads(p.read_text(encoding="utf-8"))
        lid = d["lessonId"]
        changed = False
        for it in d.get("assessment", {}).get("items", []):
            key = (lid, it.get("itemId"))
            if key in PROMPTS:
                it["prompt"] = PROMPTS[key]
                stats["prompts"] += 1
                changed = True
        for a in d.get("activities", []):
            content = a.get("content")
            if isinstance(content, dict):
                for opt in content.get("options", []):
                    if isinstance(opt, dict):
                        t = opt.get("text", "")
                        if "screen" in t.lower():
                            opt["text"] = re.sub(r"\bscreen\b", "iskrin", t, flags=re.I)
                            stats["bleed"] += 1
                            changed = True
        # simpler: walk whole document for 'screen' in fil-PH lessons
        if (d.get("language") or "").startswith("fil"):
            raw = json.dumps(d, ensure_ascii=False)
            n = raw.count("screen")
            if n:
                d = json.loads(re.sub(r"\bscreen\b", "iskrin", raw, flags=re.I))
                stats["bleed"] += n
                changed = True
        if changed:
            stats["lessons"] += 1
            p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")
    print(stats)

if __name__ == "__main__":
    main()
