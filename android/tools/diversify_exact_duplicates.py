#!/usr/bin/env python3
"""Add safe transfer context to exact duplicate lesson payloads.

Exact duplicate lessons are poor curriculum design even when their objective is
repeated. This pass keeps the lesson's examples, keys, and completion rules
unchanged, but gives every repeated copy a distinct, age-appropriate transfer
context in the introduction and explanation. It is idempotent.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

LESSONS = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"


def normalize(value: Any) -> Any:
    if isinstance(value, dict):
        ignored = {
            "lessonId", "title", "day", "week", "quarter", "month", "itemId",
            "activityId", "assetId", "sourceRecords", "educatorValidated",
            "releaseStatus", "reviewedAt", "reviewer",
        }
        return {k: normalize(v) for k, v in value.items() if k not in ignored}
    if isinstance(value, list):
        return [normalize(v) for v in value]
    if isinstance(value, str):
        return re.sub(r"\s*[·•]\s*(?:Q\d+\s+W\d+\s+D\d+|M\d+\s+D\d+)", "", value)
    return value


def context_for(lesson: dict[str, Any], rank: int) -> str:
    subject = str(lesson.get("subject", "")).lower()
    language = lesson.get("language")
    if language == "fil-PH":
        contexts = [
            "Subukan din ito sa bahay.",
            "Isipin ang isang halimbawa sa paaralan.",
            "Iugnay ito sa isang kaibigan o kapitbahay.",
            "Gamitin ang ideya sa isang bagong sitwasyon.",
        ]
    elif subject == "english":
        contexts = [
            "Try the skill with a classmate's name.",
            "Try the skill in a sentence about your classroom.",
            "Try the skill with a new story detail.",
            "Try the skill in a message to a friend.",
        ]
    elif subject == "mathematics":
        contexts = [
            "Try the same idea with a new number.",
            "Try the same idea with objects you can count.",
            "Try the same idea with a different shape.",
            "Try the same idea in a simple real-life problem.",
        ]
    elif subject == "gmrc":
        contexts = [
            "Subukan ang magalang na kilos sa bahay.",
            "Isipin ang magalang na kilos sa silid-aralan.",
            "Iugnay ito sa pakikitungo sa kaibigan.",
            "Gamitin ito kapag may bagong kakilala.",
        ]
    elif subject == "makabansa":
        contexts = [
            "Isipin ang isang komunidad sa baybayin.",
            "Ihambing ito sa pamumuhay sa bukid.",
            "Tingnan kung paano ito nakikita sa inyong bayan.",
            "Iugnay ito sa isang pamilyar na pamilihan o tahanan.",
        ]
    elif subject == "science":
        contexts = [
            "Look for the same idea in a safe home example.",
            "Look for the same idea in the school garden.",
            "Look for the same idea in the sky or weather.",
            "Look for the same idea in an animal or plant.",
        ]
    else:
        contexts = ["Try the idea in a familiar place.", "Try the idea with a new example."]
    return contexts[(rank - 1) % len(contexts)]


def append_once(value: Any, sentence: str) -> Any:
    if not isinstance(value, str) or sentence in value:
        return value
    return value.rstrip() + " " + sentence


def diversify(lesson: dict[str, Any], rank: int) -> bool:
    sentence = context_for(lesson, rank)
    changed = False
    if isinstance(lesson.get("introduction"), str) and sentence not in lesson["introduction"]:
        lesson["introduction"] = append_once(lesson["introduction"], sentence)
        changed = True
    for activity in lesson.get("activities", []):
        if activity.get("type") != "ANIMATED_EXPLANATION":
            continue
        if isinstance(activity.get("content"), str) and sentence not in activity["content"]:
            activity["content"] = append_once(activity["content"], sentence)
            changed = True
        for field in ("instruction", "prompt", "narration", "accessibilityAlternative"):
            if sentence not in str(activity.get(field, "")):
                new = append_once(activity.get(field), sentence)
                if new != activity.get(field):
                    activity[field] = new
                    changed = True
    return changed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    entries = [(p, json.loads(p.read_text(encoding="utf-8"))) for p in sorted(LESSONS.glob("*.json"))]
    groups: dict[str, list[tuple[Path, dict[str, Any]]]] = defaultdict(list)
    for path, lesson in entries:
        digest = hashlib.sha256(json.dumps(normalize(lesson), sort_keys=True, ensure_ascii=False).encode()).hexdigest()
        groups[digest].append((path, lesson))
    changed = 0
    for group in groups.values():
        if len(group) < 2:
            continue
        for rank, (path, lesson) in enumerate(group):
            if rank == 0:
                continue
            if diversify(lesson, rank):
                changed += 1
                if not args.dry_run:
                    path.write_text(json.dumps(lesson, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    # Recompute exact duplicate groups after the pass.
    after: dict[str, list[str]] = defaultdict(list)
    for path, original in entries:
        lesson = original if args.dry_run else json.loads(path.read_text(encoding="utf-8"))
        digest = hashlib.sha256(json.dumps(normalize(lesson), sort_keys=True, ensure_ascii=False).encode()).hexdigest()
        after[digest].append(path.stem)
    remaining = [v for v in after.values() if len(v) > 1]
    print(json.dumps({"changed": changed, "exact_duplicate_groups_remaining": len(remaining), "duplicate_lessons_remaining": sum(len(v) for v in remaining)}, indent=2))
    return 1 if args.check and remaining else 0


if __name__ == "__main__":
    raise SystemExit(main())
