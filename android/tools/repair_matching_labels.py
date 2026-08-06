#!/usr/bin/env python3
"""Repair only matching labels with explicit, authored category separators.

A set is changed only when every left value contains an em dash or arrow and
produces a distinct non-empty right label. Natural-language pairs are left
untouched for educator review.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
LESSONS = ROOT / "app/src/main/assets/content-pack/month-01/lessons"
SEPARATOR = re.compile(r"\s*(?:—|->|→)\s*", re.UNICODE)


def explicit_label(left: Any) -> str | None:
    if not isinstance(left, str):
        return None
    parts = SEPARATOR.split(left, maxsplit=1)
    if len(parts) != 2:
        return None
    label = parts[1].strip()
    return label or None


def repair_lesson(lesson: dict[str, Any]) -> bool:
    changed = False
    for activity in lesson.get("activities", []):
        if not isinstance(activity, dict) or activity.get("type") != "MATCHING_PAIRS":
            continue
        content = activity.get("content")
        if not isinstance(content, dict):
            continue
        pairs = content.get("pairs")
        if not isinstance(pairs, list) or len(pairs) < 3:
            continue
        rights = [pair.get("right") for pair in pairs if isinstance(pair, dict)]
        if len(rights) != len(pairs) or len(set(rights)) != 1:
            continue
        labels = [explicit_label(pair.get("left")) for pair in pairs]
        if any(label is None for label in labels) or len(set(labels)) != len(labels):
            continue
        for pair, label in zip(pairs, labels):
            if pair.get("right") != label:
                pair["right"] = label
                changed = True
    return changed


def indent_width(source: str) -> int:
    for line in source.splitlines()[1:]:
        if line.strip():
            return len(line) - len(line.lstrip(" ")) or 1
    return 1


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    changed = 0
    paths = sorted(LESSONS.glob("*.json"))
    for path in paths:
        source = path.read_text(encoding="utf-8")
        lesson = json.loads(source)
        if not repair_lesson(lesson):
            continue
        changed += 1
        if not args.check and not args.dry_run:
            path.write_text(json.dumps(lesson, ensure_ascii=False, indent=indent_width(source)) + "\n", encoding="utf-8")
    mode = "CHECK" if args.check or args.dry_run else "UPDATED"
    print(f"{mode}: {len(paths)} lessons; changed={changed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
