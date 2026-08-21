#!/usr/bin/env python3
"""Batch-mark media catalog entries as educator-released.

Mirrors mark_lessons_reviewed.py for the optional video catalog: before the
video-based Daily Mission can assign videos, the educator (parent/curriculum
reviewer) marks reviewed entries releaseStatus=RELEASED. Only entries that
already carry validated assessment metadata are eligible, so the mission
planner never assigns unassessed media.

Usage:
  python3 mark_media_released.py --catalog /path/to/catalog.json [--grade 3]
                                 [--all] [--dry-run]

The script is idempotent: already-released entries are counted, not rewritten.
A .bak backup of the catalog is written next to it before any modification.
"""
from __future__ import annotations

import argparse
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--grade", type=int, help="only release this gradeLevel")
    parser.add_argument(
        "--all",
        action="store_true",
        help="release every assessed entry regardless of grade",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not args.grade and not args.all:
        parser.error("choose --grade N or --all")

    raw = args.catalog.read_text(encoding="utf-8")
    catalog = json.loads(raw)
    media = catalog.get("media", [])
    stamp = datetime.now(timezone.utc).isoformat(timespec="seconds")

    already = released = skipped_unassessed = 0
    changed: list[str] = []
    for asset in media:
        if asset.get("releaseStatus") == "RELEASED":
            already += 1
            continue
        if "assessment" not in asset:
            skipped_unassessed += 1
            continue
        if not args.all and asset.get("gradeLevel") != args.grade:
            continue
        asset["releaseStatus"] = "RELEASED"
        asset["reviewedAt"] = stamp
        changed.append(asset["mediaId"])
        released += 1

    print(
        f"catalog={args.catalog} total={len(media)} "
        f"already_released={already} newly_released={released} "
        f"skipped_unassessed={skipped_unassessed}"
    )
    for media_id in changed[:10]:
        print(f"  released: {media_id}")
    if len(changed) > 10:
        print(f"  ... and {len(changed) - 10} more")

    if args.dry_run or not changed:
        return 0

    backup = args.catalog.with_suffix(args.catalog.suffix + ".bak")
    shutil.copy2(args.catalog, backup)
    args.catalog.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"wrote {args.catalog} (backup: {backup})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
