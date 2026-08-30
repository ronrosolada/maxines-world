#!/usr/bin/env python3
"""Rebuild media-assessments.json from the reviewed catalog assessments.

The catalog is the paired, authoritative source for video identity and the
educator-reviewed five-item assessment attached to each released video. This
script deliberately copies only assessment fields; it does not alter catalog
metadata or media identities.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CATALOG = ROOT / "server/content/catalog.json"
DEFAULT_OUTPUT = ROOT / "android/app/src/main/assets/content-pack/media-assessments.json"
POLICY = {"itemsPerVideo": 5, "passingCorrectCount": 4, "claimsMastery": False}


def build_manifest(catalog: dict) -> dict:
    rows = []
    seen: set[str] = set()
    for media in catalog.get("media", []):
        media_id = media.get("mediaId")
        assessment = media.get("assessment")
        if not media_id or media_id in seen:
            raise ValueError(f"missing or duplicate mediaId: {media_id!r}")
        if not isinstance(assessment, dict):
            raise ValueError(f"{media_id}: assessment is missing")
        items = assessment.get("items", [])
        if len(items) != POLICY["itemsPerVideo"]:
            raise ValueError(f"{media_id}: expected 5 items, found {len(items)}")
        rows.append(
            {
                "mediaId": media_id,
                "questionCount": assessment.get("questionCount"),
                "passingCorrectCount": assessment.get("passingCorrectCount"),
                "claimsMastery": assessment.get("claimsMastery"),
                "items": items,
            }
        )
        seen.add(media_id)
    if len(rows) != 237:
        raise ValueError(f"expected 237 media rows, found {len(rows)}")
    return {"schemaVersion": 1, "assessmentPolicy": POLICY, "media": rows}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, default=DEFAULT_CATALOG)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true", help="fail if output is not reproducible")
    args = parser.parse_args()

    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    rendered = json.dumps(build_manifest(catalog), indent=2, ensure_ascii=False) + "\n"
    if args.check:
        if not args.output.exists() or args.output.read_text(encoding="utf-8") != rendered:
            print(f"{args.output} is not synchronized with {args.catalog}")
            return 1
        print(f"Verified {args.output}: 237 videos / 1,185 reviewed items")
        return 0
    args.output.write_text(rendered, encoding="utf-8")
    print(f"Wrote {args.output}: 237 videos / 1,185 reviewed items")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
