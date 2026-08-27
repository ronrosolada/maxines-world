#!/usr/bin/env python3
"""Validate the interactive video checkpoint content pack.

The checkpoint manifest intentionally aligns to media-assessments.json, the tracked
source of stable media IDs.  Run from any working directory:

    python3 android/tools/validate_video_checkpoints.py
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ANDROID = Path(__file__).resolve().parents[1]
DEFAULT_CHECKPOINTS = ANDROID / "app/src/main/assets/content-pack/video-checkpoints.json"
DEFAULT_ASSESSMENTS = ANDROID / "app/src/main/assets/content-pack/media-assessments.json"
OPTION_IDS = {"a", "b", "c", "d"}
CHECKPOINT_TYPES = {
    "PREDICTION",
    "QUICK_CHECK",
    "HANDS_ON_PAPER",
    "VOCAB_SPOTLIGHT",
}
LADDER_KEYS = {
    "hint1_clue",
    "hint2_worked_example",
    "hint3_prereq_subquestion",
}


def load_json(path: Path, errors: list[str]) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{path}: cannot read valid JSON: {exc}")
        return None


def nonblank(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def validate(checkpoints_path: Path, assessments_path: Path) -> list[str]:
    errors: list[str] = []
    document = load_json(checkpoints_path, errors)
    assessments = load_json(assessments_path, errors)
    if not isinstance(document, dict) or not isinstance(assessments, dict):
        return errors

    media = document.get("media")
    assessment_media = assessments.get("media")
    if not isinstance(media, list) or not isinstance(assessment_media, list):
        errors.append("Both manifests must contain a 'media' array")
        return errors

    expected_ids: set[str] = {
        media_id
        for entry in assessment_media
        if isinstance(entry, dict)
        and isinstance((media_id := entry.get("mediaId")), str)
    }
    actual_ids: list[Any] = [
        entry.get("mediaId") if isinstance(entry, dict) else None for entry in media
    ]
    duplicates = sorted({x for x in actual_ids if actual_ids.count(x) > 1 and isinstance(x, str)})
    if duplicates:
        errors.append(f"Duplicate mediaId values: {', '.join(duplicates)}")
    missing = sorted(expected_ids - set(actual_ids))
    extra = sorted(set(actual_ids) - expected_ids, key=str)
    if missing:
        errors.append(f"Missing mediaId values from assessments: {', '.join(missing)}")
    if extra:
        errors.append(f"Unknown mediaId values: {', '.join(map(str, extra))}")

    checkpoint_ids: set[str] = set()
    for media_index, entry in enumerate(media):
        where = f"media[{media_index}]"
        if not isinstance(entry, dict):
            errors.append(f"{where}: must be an object")
            continue
        media_id = entry.get("mediaId")
        if not nonblank(media_id):
            errors.append(f"{where}.mediaId: must be non-blank")
            continue
        items = entry.get("checkpoints")
        if not isinstance(items, list) or not items:
            errors.append(f"{media_id}.checkpoints: must be a non-empty array")
            continue
        previous_position = -1
        for item_index, item in enumerate(items):
            item_where = f"{media_id}.checkpoints[{item_index}]"
            if not isinstance(item, dict):
                errors.append(f"{item_where}: must be an object")
                continue
            checkpoint_id = item.get("checkpointId")
            expected_checkpoint_id = f"{media_id}-cp{item_index + 1:02d}"
            if checkpoint_id != expected_checkpoint_id:
                errors.append(f"{item_where}.checkpointId: expected {expected_checkpoint_id!r}")
            if isinstance(checkpoint_id, str):
                if checkpoint_id in checkpoint_ids:
                    errors.append(f"{item_where}.checkpointId: duplicate {checkpoint_id!r}")
                checkpoint_ids.add(checkpoint_id)
            position = item.get("positionMs")
            if isinstance(position, bool) or not isinstance(position, int) or position <= 0:
                errors.append(f"{item_where}.positionMs: must be a positive integer")
            elif position <= previous_position:
                errors.append(f"{item_where}.positionMs: timestamps must be strictly increasing")
            else:
                previous_position = position
            if item.get("type") not in CHECKPOINT_TYPES:
                errors.append(f"{item_where}.type: invalid checkpoint type")
            if not nonblank(item.get("prompt")):
                errors.append(f"{item_where}.prompt: must be non-blank")

            options = item.get("options")
            option_ids: list[Any] = []
            if not isinstance(options, list) or len(options) != 4:
                errors.append(f"{item_where}.options: must contain exactly four options")
            else:
                for option_index, option in enumerate(options):
                    if not isinstance(option, dict):
                        errors.append(f"{item_where}.options[{option_index}]: must be an object")
                        continue
                    option_ids.append(option.get("id"))
                    if not nonblank(option.get("text")):
                        errors.append(f"{item_where}.options[{option_index}].text: must be non-blank")
                if set(option_ids) != OPTION_IDS or len(set(option_ids)) != 4:
                    errors.append(f"{item_where}.options: IDs must be exactly a, b, c, d")
            correct = item.get("correctOptionId")
            if correct not in OPTION_IDS or correct not in option_ids:
                errors.append(f"{item_where}.correctOptionId: must reference a valid option")

            ladder = item.get("feedbackLadder")
            if not isinstance(ladder, dict):
                errors.append(f"{item_where}.feedbackLadder: must be an object")
            else:
                if set(ladder) != LADDER_KEYS:
                    errors.append(f"{item_where}.feedbackLadder: keys must be exactly {sorted(LADDER_KEYS)}")
                for key in LADDER_KEYS:
                    if not nonblank(ladder.get(key)):
                        errors.append(f"{item_where}.feedbackLadder.{key}: must be non-blank")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoints", type=Path, default=DEFAULT_CHECKPOINTS)
    parser.add_argument("--assessments", type=Path, default=DEFAULT_ASSESSMENTS)
    args = parser.parse_args()
    errors = validate(args.checkpoints, args.assessments)
    if errors:
        print(f"Video checkpoint validation failed ({len(errors)} error(s)):", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    document = json.loads(args.checkpoints.read_text(encoding="utf-8"))
    count = sum(len(entry["checkpoints"]) for entry in document["media"])
    print(f"Video checkpoints valid: {len(document['media'])} media, {count} checkpoints")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
