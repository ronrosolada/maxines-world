#!/usr/bin/env python3
"""Validate transcript-grounded optional video assessments."""
from __future__ import annotations

import argparse
import json
import re
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


def normalized(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.lower()).strip()


def load_rows(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        rows = data.get("media", data.get("videos", []))
        if isinstance(rows, list):
            return rows
    raise ValueError("Assessment source must contain a media/videos list")


def transcript_data(transcript_dir: Path, index: int, video_id: str) -> dict:
    path = transcript_dir / f"{index:02d}-{video_id}.json"
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, nargs="+", required=True)
    parser.add_argument("--transcripts", type=Path, required=True)
    args = parser.parse_args()

    rows = []
    for source in args.source:
        rows.extend(load_rows(source))

    expected = {media_id for _, _, media_id in ENTRIES}
    actual = {str(row.get("mediaId", "")) for row in rows}
    errors: list[str] = []
    if actual != expected:
        errors.append(f"media IDs differ; missing={sorted(expected - actual)}, extra={sorted(actual - expected)}")
    if len(rows) != len(expected):
        errors.append(f"expected {len(expected)} video assessments, found {len(rows)}")

    row_by_id = {row.get("mediaId"): row for row in rows}
    global_prompts: dict[str, str] = {}
    total_items = 0
    for index, video_id, media_id in ENTRIES:
        row = row_by_id.get(media_id)
        if row is None:
            continue
        items = row.get("items", row.get("questions", []))
        if not isinstance(items, list) or len(items) != 10:
            errors.append(f"{media_id}: expected exactly 10 items")
            continue
        if int(row.get("passingCorrectCount", 8)) != 8:
            errors.append(f"{media_id}: passingCorrectCount must be 8")

        transcript = transcript_data(args.transcripts, index, video_id)
        full_text = normalized(transcript.get("full_text", ""))
        segments = transcript.get("segments", [])
        transcript_end = max(
            (float(s.get("start", 0)) + float(s.get("duration", 0)) for s in segments),
            default=0.0,
        )
        answer_letters = []
        prompts = set()
        for expected_sequence, item in enumerate(items, start=1):
            total_items += 1
            item_id = f"{media_id}-q{expected_sequence:02d}"
            if item.get("itemId") != item_id:
                errors.append(f"{media_id}: expected itemId {item_id}")
            if item.get("sequence") != expected_sequence:
                errors.append(f"{item_id}: sequence mismatch")
            if item.get("type") != "MULTIPLE_CHOICE":
                errors.append(f"{item_id}: unsupported type")
            prompt = str(item.get("prompt", "")).strip()
            prompt_key = normalized(prompt)
            if not prompt or prompt_key in prompts:
                errors.append(f"{item_id}: blank or duplicate prompt within video")
            prompts.add(prompt_key)
            if prompt_key in global_prompts:
                errors.append(f"{item_id}: duplicate prompt with {global_prompts[prompt_key]}")
            else:
                global_prompts[prompt_key] = item_id

            options = item.get("options", [])
            option_ids = [option.get("id") for option in options if isinstance(option, dict)]
            if option_ids != ["a", "b", "c", "d"]:
                errors.append(f"{item_id}: options must be ordered a,b,c,d")
            if any(not str(option.get("text", "")).strip() for option in options):
                errors.append(f"{item_id}: blank option")
            correct = item.get("correctOptionIds", [])
            if len(correct) != 1 or correct[0] not in option_ids:
                errors.append(f"{item_id}: invalid answer key")
            else:
                answer_letters.append(correct[0])

            explanation = str(item.get("explanation", "")).strip()
            if not explanation:
                errors.append(f"{item_id}: blank explanation")

            timestamp = float(item.get("sourceTimestampSeconds", -1))
            if timestamp < 0 or (transcript_end and timestamp > transcript_end + 5):
                errors.append(f"{item_id}: source timestamp outside transcript")
            quote = normalized(str(item.get("sourceQuote", "")))
            if not quote:
                errors.append(f"{item_id}: missing source quote")
            else:
                quote_tokens = quote.split()
                present = sum(token in full_text for token in quote_tokens)
                if present < max(1, int(len(quote_tokens) * 0.45)):
                    errors.append(f"{item_id}: source quote has low transcript overlap")

            # Autogenerated captions frequently misspell Tagalog words or omit
            # the keyed answer while retaining the explanatory sentence. The
            # timestamped source quote is the reliable grounding check here.

        if len(set(answer_letters)) < 3:
            errors.append(f"{media_id}: answer-position variety is too low")

    if errors:
        print("Media assessment validation failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"Media assessment validation passed: {total_items} items across {len(expected)} videos")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
