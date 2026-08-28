#!/usr/bin/env python3
"""Validate parity between server catalog, media assessments, and video checkpoints.

Ensures that the server/content/catalog.json media IDs match 100% with the tracked
media-assessments.json and video-checkpoints.json manifests.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SERVER_CATALOG = REPO_ROOT / "server/content/catalog.json"
DEFAULT_MEDIA_ASSESSMENTS = (
    REPO_ROOT / "android/app/src/main/assets/content-pack/media-assessments.json"
)
DEFAULT_VIDEO_CHECKPOINTS = (
    REPO_ROOT / "android/app/src/main/assets/content-pack/video-checkpoints.json"
)


def load_json(path: Path, errors: list[str]) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{path}: cannot read valid JSON: {exc}")
        return None


def validate_parity(
    server_catalog_path: Path,
    media_assessments_path: Path,
    video_checkpoints_path: Path,
) -> list[str]:
    errors: list[str] = []

    server_data = load_json(server_catalog_path, errors)
    assessments_data = load_json(media_assessments_path, errors)
    checkpoints_data = load_json(video_checkpoints_path, errors)

    if not server_data or not assessments_data or not checkpoints_data:
        return errors

    server_media = server_data.get("media")
    assessments_media = assessments_data.get("media")
    checkpoints_media = checkpoints_data.get("media")

    if not isinstance(server_media, list):
        errors.append(f"{server_catalog_path}: 'media' must be a list")
        return errors
    if not isinstance(assessments_media, list):
        errors.append(f"{media_assessments_path}: 'media' must be a list")
        return errors
    if not isinstance(checkpoints_media, list):
        errors.append(f"{video_checkpoints_path}: 'media' must be a list")
        return errors

    server_ids: list[str] = [
        str(m.get("mediaId"))
        for m in server_media
        if isinstance(m, dict) and m.get("mediaId")
    ]
    assessments_ids: list[str] = [
        str(m.get("mediaId"))
        for m in assessments_media
        if isinstance(m, dict) and m.get("mediaId")
    ]
    checkpoints_ids: list[str] = [
        str(m.get("mediaId"))
        for m in checkpoints_media
        if isinstance(m, dict) and m.get("mediaId")
    ]

    # Check duplicates within each
    for label, id_list in [
        ("server catalog", server_ids),
        ("media assessments", assessments_ids),
        ("video checkpoints", checkpoints_ids),
    ]:
        dups = {x for x in id_list if id_list.count(x) > 1}
        if dups:
            errors.append(f"Duplicate mediaId entries in {label}: {sorted(dups)}")

    server_set = set(server_ids)
    assessments_set = set(assessments_ids)
    checkpoints_set = set(checkpoints_ids)

    diff_server_assessments = server_set ^ assessments_set
    if diff_server_assessments:
        errors.append(
            f"Parity mismatch between server catalog ({len(server_set)}) and media assessments ({len(assessments_set)}): "
            f"in server only: {sorted(server_set - assessments_set)}, in assessments only: {sorted(assessments_set - server_set)}"
        )

    diff_server_checkpoints = server_set ^ checkpoints_set
    if diff_server_checkpoints:
        errors.append(
            f"Parity mismatch between server catalog ({len(server_set)}) and video checkpoints ({len(checkpoints_set)}): "
            f"in server only: {sorted(server_set - checkpoints_set)}, in checkpoints only: {sorted(checkpoints_set - server_set)}"
        )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--server-catalog", type=Path, default=DEFAULT_SERVER_CATALOG
    )
    parser.add_argument(
        "--media-assessments", type=Path, default=DEFAULT_MEDIA_ASSESSMENTS
    )
    parser.add_argument(
        "--video-checkpoints", type=Path, default=DEFAULT_VIDEO_CHECKPOINTS
    )
    args = parser.parse_args()

    errors = validate_parity(
        args.server_catalog, args.media_assessments, args.video_checkpoints
    )
    if errors:
        print(
            f"Catalog parity validation failed with {len(errors)} error(s):",
            file=sys.stderr,
        )
        for err in errors:
            print(f"- {err}", file=sys.stderr)
        return 1

    print(
        f"Catalog parity validated successfully across 3 manifests ({len(json.loads(args.server_catalog.read_text())['media'])} media entries)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
