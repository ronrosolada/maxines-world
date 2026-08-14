#!/usr/bin/env python3
"""Fail when two lesson assessments are byte-equivalent in learner content."""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any


SIGNATURE_FIELDS = ("type", "prompt", "options", "correctOptionIds", "explanation")


def signature(item: dict[str, Any]) -> str:
    payload = {field: item.get(field) for field in SIGNATURE_FIELDS}
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def find_duplicates(lessons: Path) -> dict[str, list[str]]:
    seen: dict[str, list[str]] = defaultdict(list)
    for path in sorted(lessons.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        for item in data.get("assessment", {}).get("items", []):
            if isinstance(item, dict):
                seen[signature(item)].append(f"{path.stem}:{item.get('itemId', '?')}")
    return {key: values for key, values in seen.items() if len(values) > 1}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--lessons",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons",
    )
    args = parser.parse_args(argv)
    if not args.lessons.is_dir():
        print(f"error: lesson directory does not exist: {args.lessons}", file=sys.stderr)
        return 2

    duplicates = find_duplicates(args.lessons)
    print(f"Assessment duplicate groups: {len(duplicates)}")
    for values in duplicates.values():
        print("  " + " | ".join(values))
    return 1 if duplicates else 0


if __name__ == "__main__":
    raise SystemExit(main())
