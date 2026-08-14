#!/usr/bin/env python3
"""
Fix MCQ position bias for the REAL schema used by the 358 bundled lessons:
items use `options: [{id, text}...]` + `correctOptionIds` (NOT correctIndex).
Deterministically shuffles option display order and remaps ids a..n,
then marks educatorValidated=false because assessment order materially
changed after the last educator review.
"""

import json
import glob
import random
from pathlib import Path

LESSONS_DIR = Path("/home/ron/workspace/maxines-world/android/app/src/main/assets/content-pack/month-01/lessons")

def shuffle_item(item, lesson_id, item_id):
    opts = item.get("options")
    if not isinstance(opts, list) or len(opts) < 2:
        return False
    keys = item.get("correctOptionIds")
    # normalize to list
    if isinstance(keys, str):
        keys = [keys]
    if not keys:
        return False

    correct_texts = []
    for o in opts:
        if o.get("id") in keys:
            correct_texts.append(o.get("text"))

    if not correct_texts:
        return False

    # deterministic shuffle by lesson+item
    rng = random.Random(f"{lesson_id}:{item_id}")
    order = list(range(len(opts)))
    rng.shuffle(order)

    new_opts = []
    new_keys = []
    for new_idx, old_idx in enumerate(order):
        new_id = chr(ord("a") + new_idx)
        o = opts[old_idx]
        new_opts.append({"id": new_id, "text": o.get("text", "")})
        if o.get("text") in correct_texts:
            new_keys.append(new_id)

    item["options"] = new_opts
    if isinstance(keys, str):
        item["correctOptionIds"] = new_keys[0]
    else:
        item["correctOptionIds"] = new_keys
    return True

def main():
    total_changed_items = 0
    lessons_changed = 0
    files = sorted(LESSONS_DIR.glob("*.json"))
    for lf in files:
        with open(lf, "r", encoding="utf-8") as f:
            data = json.load(f)
        lesson_id = data.get("lessonId", lf.stem)
        changed = False
        for it in data.get("assessment", {}).get("items", []):
            iid = it.get("itemId", "q")
            if shuffle_item(it, lesson_id, iid):
                total_changed_items += 1
                changed = True
        if changed:
            lessons_changed += 1
            data["educatorValidated"] = False
            with open(lf, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"Lessons re-shuffled: {lessons_changed}/{len(files)}")
    print(f"Assessment items re-shuffled: {total_changed_items}")

if __name__ == "__main__":
    main()
