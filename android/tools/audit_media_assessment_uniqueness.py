#!/usr/bin/env python3
"""Reject duplicate assessment prompt banks and weak explanations."""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path

DEFAULT_SOURCE = Path("android/app/src/main/assets/content-pack/media-assessments.json")
EXPECTED_ITEM_COUNT = 1185
TEMPLATE_EXPLANATION_PATTERNS = (
    re.compile(r"tumutugon\s+sa\s+konseptong\s+sinusukat\s+ng\s+tanong", re.I),
    re.compile(r"matches?\s+the\s+concept\s+(?:being\s+)?(?:asked|measured|tested)", re.I),
    re.compile(r"apply\s+the\s+rule\s+or\s+calculation\s+shown", re.I),
    re.compile(r"(?:the\s+)?correct\s+answer\s+is\b", re.I),
)


def normalize(text: object) -> str:
    value = unicodedata.normalize("NFKC", str(text)).casefold()
    return " ".join(re.findall(r"[\w]+", value, flags=re.UNICODE))


def audit_assessments(data: dict, expected_item_count: int | None = EXPECTED_ITEM_COUNT) -> list[str]:
    errors: list[str] = []
    media = data.get("media", [])
    if not isinstance(media, list):
        return ["top-level media must be a list"]

    seen_groups: dict[tuple[str, ...], str] = {}
    item_count = 0
    for row in media:
        media_id = str(row.get("mediaId", "<missing-media-id>"))
        items = row.get("items", [])
        if not isinstance(items, list):
            errors.append(f"{media_id}: items must be a list")
            continue
        item_count += len(items)
        group = tuple(sorted(normalize(item.get("prompt", "")) for item in items))
        if group in seen_groups:
            errors.append(f"{media_id}: duplicate prompt group with {seen_groups[group]}")
        else:
            seen_groups[group] = media_id

        for index, item in enumerate(items, 1):
            item_id = str(item.get("itemId", f"{media_id} item {index}"))
            explanation = str(item.get("explanation", "")).strip()
            if not explanation:
                errors.append(f"{item_id}: blank explanation")
            elif any(pattern.search(explanation) for pattern in TEMPLATE_EXPLANATION_PATTERNS):
                errors.append(f"{item_id}: template explanation")

    if expected_item_count is not None and item_count != expected_item_count:
        errors.append(f"expected {expected_item_count} assessment items, found {item_count}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, nargs="?", default=DEFAULT_SOURCE)
    parser.add_argument("--expected-items", type=int, default=EXPECTED_ITEM_COUNT)
    args = parser.parse_args()
    data = json.loads(args.source.read_text(encoding="utf-8"))
    errors = audit_assessments(data, expected_item_count=args.expected_items)
    if errors:
        print("Media assessment uniqueness audit failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"Media assessment uniqueness audit passed: {args.expected_items} items, 0 duplicate prompt groups")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
