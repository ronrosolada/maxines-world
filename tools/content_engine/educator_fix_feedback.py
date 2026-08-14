#!/usr/bin/env python3
"""
Educator fix: cross-lesson retry references + dull generic correct-feedback.

1. Retry text that references a DIFFERENT lesson's title ("Find the clue in X again"
   / "Balikan ang pahiwatig sa X") is rewritten to reference the lesson's OWN title
   or a generic encouraging phrase.
2. Dull correct-feedback ("Nice work. Continue to the next step.") is replaced with
   celebratory, language-matched feedback.

Deterministic, idempotent, preserves all other fields.
"""
import json
import glob
import re
from pathlib import Path

LESSONS = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

DULL_EN = re.compile(r"(nice work|good job|great job|correct)[!.]?\s*continue to the next step\.?", re.I)
DULL_FIL = re.compile(r"(mahusay|magaling|tama)[!.]?\s*(magpatuloy sa susunod na hakbang|ipagpatuloy sa susunod)\.?", re.I)

RETRY_EN = re.compile(r"find the clue in (.+?) again", re.I)
RETRY_FIL = re.compile(r"balikan ang pahiwatig sa (.+?)[.!]?$", re.I)

CELEBRATE_EN = "Hooray! Fantastic job, Maxine! 🎉⭐"
CELEBRATE_FIL = "Yehey! Napakagaling mo, Maxine! 🎉⭐"

def title_clean(t):
    return re.sub(r"\s*·.*$", "", t or "").strip()

def fix_lesson(path, stats):
    d = json.loads(path.read_text(encoding="utf-8"))
    lid = d.get("lessonId", path.stem)
    own = title_clean(d.get("title", "")).lower()
    lang = (d.get("language") or "").lower()
    is_fil = lang.startswith("fil")
    changed = False

    for a in d.get("activities", []):
        fb = a.get("feedback") or {}
        retry = fb.get("retry") or ""
        correct = fb.get("correct") or ""

        # 1. Cross-lesson retry references
        m = (RETRY_FIL if is_fil else RETRY_EN).search(retry)
        if m:
            ref = title_clean(m.group(1)).lower()
            if ref and ref != own:
                if is_fil:
                    new_retry = f"Balikan ang pahiwatig sa {title_clean(d.get('title',''))}. 💪"
                else:
                    new_retry = f"Find the clue in {title_clean(d.get('title',''))} again. 💪"
                fb["retry"] = new_retry
                stats["cross_lesson_retry"] += 1
                changed = True

        # 2. Dull generic correct feedback
        if (DULL_FIL if is_fil else DULL_EN).search(correct):
            fb["correct"] = CELEBRATE_FIL if is_fil else CELEBRATE_EN
            stats["dull_correct"] += 1
            changed = True

        if fb and fb != a.get("feedback"):
            a["feedback"] = fb

    if changed:
        path.write_text(json.dumps(d, indent=2, ensure_ascii=False), encoding="utf-8")
    return changed

def main():
    stats = {"cross_lesson_retry": 0, "dull_correct": 0, "lessons_touched": 0}
    for p in sorted(LESSONS.glob("*.json")):
        if fix_lesson(p, stats):
            stats["lessons_touched"] += 1
    print(stats)

if __name__ == "__main__":
    main()
