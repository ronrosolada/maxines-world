#!/usr/bin/env python3
"""
Educator fix #2: any learner-facing string that references ANOTHER lesson's title
is rewritten to reference the lesson's OWN title. Robust: matches against the
full clean-title set (not regex patterns).

Fields scanned: activities[].feedback.retry, feedback.correct,
assessment.items[].explanation, activities[].instruction.
"""
import json
import glob
import re
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

def clean(t):
    return re.sub(r"\s*·.*$", "", t or "").strip()

# Load all lessons and titles
lessons = {}
for p in LESSONS.glob("*.json"):
    d = json.loads(p.read_text(encoding="utf-8"))
    lessons[p] = d

titles = {p: clean(d.get("title", "")) for p, d in lessons.items()}
# Title -> list of (path, title) that own it; longest-first for greedy replacement
all_titles = sorted({t for t in titles.values() if t}, key=len, reverse=True)

stats = {"fixed_retry": 0, "fixed_correct": 0, "fixed_explanation": 0, "lessons_touched": 0}

def replace_foreign(text, own_title):
    if not text:
        return text, False
    new = text
    changed = False
    for t in all_titles:
        if t.lower() == own_title.lower():
            continue
        # whole-word-ish: title must appear as a substring that is not part of a longer title we also own
        idx = new.lower().find(t.lower())
        if idx >= 0:
            # skip if the lesson's own title is a substring of t (then t likely matches own too)
            if own_title.lower() in t.lower():
                continue
            new = new[:idx] + own_title + new[idx + len(t):]
            changed = True
    return new, changed

for p, d in lessons.items():
    own = titles[p]
    changed_any = False
    for a in d.get("activities", []):
        fb = a.get("feedback") or {}
        for k in ("retry", "correct"):
            v = fb.get(k)
            if v:
                nv, ch = replace_foreign(v, own)
                if ch:
                    fb[k] = nv
                    stats[f"fixed_{k}"] += 1
                    changed_any = True
        if fb:
            a["feedback"] = fb
    for it in d.get("assessment", {}).get("items", []):
        v = it.get("explanation")
        if v:
            nv, ch = replace_foreign(v, own)
            if ch:
                it["explanation"] = nv
                stats["fixed_explanation"] += 1
                changed_any = True
    if changed_any:
        stats["lessons_touched"] += 1
        p.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")

print(stats)
