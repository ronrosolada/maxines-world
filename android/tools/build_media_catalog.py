#!/usr/bin/env python3
"""Build the Maxine's World optional media catalog from staged MP4 files."""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path

ENTRIES = [
    (1, "O6mA_5-JPaw", "kids-tagalog-01-introductions"),
    (2, "lDDTFm2QEb4", "kids-tagalog-02-alphabet-vocabulary"),
    (3, "cRL0j4smJpk", "kids-tagalog-03-family"),
    (4, "7xsSdrBJ5nI", "kids-tagalog-04-action-words"),
    (5, "x_-hua5seLU", "kids-tagalog-05-first-words-counting-colors"),
    (6, "s_HguLAOw4w", "kids-tagalog-06-common-phrases"),
    (7, "RJigy5BsTjA", "kids-tagalog-07-colors"),
    (8, "lFTkaoUBBMw", "kids-tagalog-08-colors-vocabulary"),
    (9, "msOJeNcL6m0", "kids-tagalog-09-count-to-10"),
    (10, "HfuAKOHw4Sk", "kids-tagalog-10-childrens-songs"),
    (11, "EhklmhHE468", "kids-tagalog-11-body-parts"),
    (12, "eLnLpG3q2Js", "kids-tagalog-12-house-vocabulary"),
    (13, "6XQMtLe8wKY", "kids-tagalog-13-opposites"),
    (14, "lxmI2k8Dp5k", "kids-tagalog-14-counting-colors-animals"),
    (15, "HvjgUaqYq7o", "kids-tagalog-15-wild-animals-insects"),
    (16, "o1DBP9Dp5Xw", "kids-tagalog-16-pets-farm-animals"),
    (17, "nIi8lRSL0NQ", "kids-tagalog-17-weather"),
    (18, "Lk7BHIjadXc", "kids-tagalog-18-nature"),
]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def probe(path: Path) -> tuple[str, int, int, int]:
    raw = subprocess.check_output(
        [
            "ffprobe",
            "-v",
            "error",
            "-select_streams",
            "v:0",
            "-show_entries",
            "stream=codec_name,width,height:format=duration",
            "-of",
            "json",
            str(path),
        ],
        text=True,
    )
    data = json.loads(raw)
    stream = (data.get("streams") or [{}])[0]
    duration = round(float((data.get("format") or {}).get("duration", 0)))
    return (
        str(stream.get("codec_name", "")),
        int(stream.get("width", 0)),
        int(stream.get("height", 0)),
        duration,
    )


def title_for(staging: Path, index: int, video_id: str) -> str:
    info_path = staging / f"{index:02d}-{video_id}.info.json"
    if info_path.is_file():
        info = json.loads(info_path.read_text(encoding="utf-8"))
        title = str(info.get("title", "")).strip()
        if title:
            return title
    return f"Kids Tagalog Lesson {index}"


def load_assessments(path: Path | None) -> dict[str, dict]:
    if path is None:
        return {}
    raw = json.loads(path.read_text(encoding="utf-8"))
    rows = raw.get("media", raw.get("videos", [])) if isinstance(raw, dict) else raw
    if not isinstance(rows, list):
        raise ValueError("Assessment source must contain a media/videos list")

    public_keys = (
        "itemId",
        "sequence",
        "type",
        "prompt",
        "options",
        "correctOptionIds",
        "explanation",
    )
    assessments = {}
    for row in rows:
        media_id = str(row.get("mediaId", "")).strip()
        items = row.get("items", [])
        if not media_id or not isinstance(items, list) or len(items) != 10:
            raise ValueError(f"Expected exactly 10 assessment items for {media_id or '<missing mediaId>'}")
        if media_id in assessments:
            raise ValueError(f"Duplicate assessment mediaId: {media_id}")
        public_items = []
        for item in items:
            missing = set(public_keys) - item.keys()
            if missing:
                raise ValueError(f"Missing assessment fields for {media_id}: {sorted(missing)}")
            public_items.append({key: item[key] for key in public_keys})
        assessments[media_id] = {
            "questionCount": 10,
            "passingCorrectCount": int(row.get("passingCorrectCount", 8)),
            "claimsMastery": False,
            "items": public_items,
        }
    return assessments


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staging", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument(
        "--assessments",
        type=Path,
        help="Optional assessment source; exactly ten child-facing items per mediaId",
    )
    args = parser.parse_args()
    assessments = load_assessments(args.assessments)

    media = []
    failures = []
    for index, video_id, media_id in ENTRIES:
        filename = f"{index:02d}-{video_id}.mp4"
        path = args.staging / filename
        if not path.is_file():
            failures.append(f"missing {filename}")
            continue
        try:
            codec, width, height, duration = probe(path)
        except (OSError, ValueError, subprocess.CalledProcessError, json.JSONDecodeError) as error:
            failures.append(f"cannot probe {filename}: {error}")
            continue
        if codec != "h264":
            failures.append(f"{filename}: expected h264, got {codec}")
        if height <= 0 or height > 480:
            failures.append(f"{filename}: expected height <= 480, got {height}")
        if width <= 0 or duration <= 0:
            failures.append(f"{filename}: invalid dimensions/duration")
        media.append(
            {
                "mediaId": media_id,
                "title": title_for(args.staging, index, video_id),
                "file": f"media/kids-tagalog/{filename}",
                "sha256": sha256(path),
                "sizeBytes": path.stat().st_size,
                "durationSeconds": duration,
                "width": width,
                "height": height,
                "mimeType": "video/mp4",
                "releaseStatus": "PREVIEW",
                "licenseStatus": "PERSONAL_USE",
                **({"assessment": assessments[media_id]} if media_id in assessments else {}),
            }
        )

    if assessments:
        expected_ids = {media_id for _, _, media_id in ENTRIES}
        missing_assessments = sorted(expected_ids - assessments.keys())
        extra_assessments = sorted(assessments.keys() - expected_ids)
        if missing_assessments:
            failures.append(f"missing assessments: {', '.join(missing_assessments)}")
        if extra_assessments:
            failures.append(f"unknown assessment mediaIds: {', '.join(extra_assessments)}")

    if failures:
        raise SystemExit("Media catalog validation failed:\n- " + "\n- ".join(failures))

    catalog = {
        "catalogVersion": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "media": media,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {args.output} with {len(media)} assets")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
