#!/usr/bin/env python3
"""Replace stock activity and assessment prompts with objective-linked copy.

This is a deterministic, text-only repair. It never changes activity IDs,
content payloads, answer keys, or progress metadata. It is intentionally
conservative: authored prompts that do not contain an exact stock marker are
left untouched.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"

ACTIVITY_MARKERS = (
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
)

ASSESSMENT_MARKERS = (
    "Which example belongs to",
    "Which choice shows the skill in",
    "What is one example from",
    "Which situation matches",
    "Which answer demonstrates",
    "Aling halimbawa ang kabilang sa",
    "Aling pagpipilian ang nagpapakita ng kasanayan sa",
    "Alin ang isang halimbawa ng",
    "Aling sitwasyon ang tumutugma sa",
    "Aling sagot ang nagpapakita ng",
)

ENGLISH_ACTIVITY_COPY = {
    "ANIMATED_EXPLANATION": "Learn today’s goal with Milo: {objective}",
    "HOTSPOT_IMAGE": "Explore each example and find evidence for: {objective}",
    "SORT_AND_CLASSIFY": "Sort each example using today’s goal: {objective}",
    "MULTIPLE_CHOICE": "Choose the example that best shows: {objective}",
    "MATCHING_PAIRS": "Match each example to today’s goal: {objective}",
    "SEQUENCE_BUILDER": "Put the practice steps in order for: {objective}",
}

FILIPINO_ACTIVITY_COPY = {
    "ANIMATED_EXPLANATION": "Alamin ang layunin ngayon kasama si Milo: {objective}",
    "HOTSPOT_IMAGE": "Tuklasin ang mga halimbawa at hanapin ang patunay para sa: {objective}",
    "SORT_AND_CLASSIFY": "Ayusin ang bawat halimbawa ayon sa layunin ngayon: {objective}",
    "MULTIPLE_CHOICE": "Piliin ang halimbawang pinakamalinaw na nagpapakita ng: {objective}",
    "MATCHING_PAIRS": "Itugma ang bawat halimbawa sa layunin ngayon: {objective}",
    "SEQUENCE_BUILDER": "Ayusin ang mga hakbang sa pagsasanay para sa: {objective}",
}


def is_filipino(lesson: dict[str, Any]) -> bool:
    return str(lesson.get("language", "")).casefold().startswith("fil")


def objective_text(lesson: dict[str, Any]) -> str:
    return str(lesson.get("objective", "today’s lesson goal")).strip().rstrip(".")


def has_marker(value: Any, markers: tuple[str, ...]) -> bool:
    return isinstance(value, str) and any(marker in value for marker in markers)


def activity_copy(lesson: dict[str, Any], activity_type: str) -> str:
    templates = FILIPINO_ACTIVITY_COPY if is_filipino(lesson) else ENGLISH_ACTIVITY_COPY
    template = templates.get(activity_type, "Practice today’s goal: {objective}")
    return template.format(objective=objective_text(lesson))


def assessment_copy(lesson: dict[str, Any]) -> str:
    objective = objective_text(lesson)
    if is_filipino(lesson):
        return f"Aling halimbawa ang nagpapakita ng layunin ngayon: {objective}?"
    return f"Which example best demonstrates today’s goal: {objective}?"


def repair_lesson(lesson: dict[str, Any]) -> bool:
    changed = False
    for activity in lesson.get("activities", []):
        if not isinstance(activity, dict):
            continue
        activity_type = str(activity.get("type", ""))
        replacement = activity_copy(lesson, activity_type)
        for field in ("instruction", "prompt"):
            if has_marker(activity.get(field), ACTIVITY_MARKERS) and activity.get(field) != replacement:
                activity[field] = replacement
                changed = True
        if has_marker(activity.get("narration"), ACTIVITY_MARKERS):
            activity["narration"] = replacement
            changed = True

    for item in lesson.get("assessment", {}).get("items", []):
        if not isinstance(item, dict):
            continue
        if has_marker(item.get("prompt"), ASSESSMENT_MARKERS):
            replacement = assessment_copy(lesson)
            if item.get("prompt") != replacement:
                item["prompt"] = replacement
                changed = True
    return changed


def indent_width(source: str) -> int:
    for line in source.splitlines()[1:]:
        if line.strip():
            return len(line) - len(line.lstrip(" ")) or 1
    return 1


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="report changes without writing")
    parser.add_argument("--dry-run", action="store_true", help="report changes without writing")
    args = parser.parse_args(argv)

    changed = 0
    for path in sorted(LESSONS.glob("*.json")):
        source = path.read_text(encoding="utf-8")
        lesson = json.loads(source)
        if not repair_lesson(lesson):
            continue
        changed += 1
        if not args.check and not args.dry_run:
            path.write_text(json.dumps(lesson, ensure_ascii=False, indent=indent_width(source)) + "\n", encoding="utf-8")

    mode = "CHECK" if args.check or args.dry_run else "UPDATED"
    print(f"{mode}: {len(list(LESSONS.glob('*.json')))} lessons; changed={changed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
