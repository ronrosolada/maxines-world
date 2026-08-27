#!/usr/bin/env python3
"""Educator-focused quality checks for Quiz Arena and video assessments."""
from __future__ import annotations

import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARENA_DIR = ROOT / "app/src/main/assets/assessment-packs"
VIDEO_PATH = ROOT / "app/src/main/assets/content-pack/media-assessments.json"
GENERIC = (
    "matches the concept being checked",
    "matches the skill",
    "tumutugon sa konseptong sinusukat",
    "apply the rule or calculation shown",
)
UNSAFE = re.compile(r"\b(kill|murder|suicide|sexual|gun|knife|drugs|alcohol)\b", re.I)


def norm(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", text.casefold()).strip()


def main() -> int:
    errors: list[str] = []
    arena_files = sorted(p for p in ARENA_DIR.glob("*.json") if p.name != "catalog.json")
    arena_items: list[tuple[str, dict]] = []
    for path in arena_files:
        data = json.loads(path.read_text(encoding="utf-8"))
        arena_items.extend((path.name, item) for item in data.get("items", []))

    video = json.loads(VIDEO_PATH.read_text(encoding="utf-8"))
    rows = video.get("media", [])
    video_items = [(row.get("mediaId", "?"), item) for row in rows for item in row.get("items", [])]

    if len(arena_files) != 18 or len(arena_items) != 180:
        errors.append(f"expected 18 arena packs/180 items; found {len(arena_files)}/{len(arena_items)}")
    if len(rows) != 237 or len(video_items) != 1185:
        errors.append(f"expected 237 video rows/1185 items; found {len(rows)}/{len(video_items)}")

    prompt_locations: dict[str, list[str]] = defaultdict(list)
    for corpus, entries in (("arena", arena_items), ("video", video_items)):
        for source, item in entries:
            ident = str(item.get("itemId", item.get("sequence", "?")))
            loc = f"{corpus}:{source}:{ident}"
            prompt = str(item.get("prompt", "")).strip()
            options = item.get("options", [])
            option_ids = [o.get("id") for o in options]
            keys = item.get("correctOptionIds", [])
            explanation = str(item.get("explanation", "")).strip()
            blob = " ".join([prompt, explanation] + [str(o.get("text", "")) for o in options])
            if option_ids != ["a", "b", "c", "d"]:
                errors.append(f"{loc}: options must be a,b,c,d")
            if len(keys) != 1 or keys[0] not in option_ids:
                errors.append(f"{loc}: exactly one valid key is required")
            if not explanation:
                errors.append(f"{loc}: explanation is blank")
            if any(fragment in explanation.casefold() for fragment in GENERIC):
                errors.append(f"{loc}: generic explanation")
            if UNSAFE.search(blob):
                errors.append(f"{loc}: unsafe/adult vocabulary")
            if corpus == "video":
                prompt_locations[norm(prompt)].append(loc)

            match = re.search(r"Which number is (even|odd):\s*([0-9, ]+)\?", prompt, re.I)
            if match:
                parity = match.group(1).lower()
                numbers = [int(n) for n in re.findall(r"\d+", match.group(2))]
                valid = [n for n in numbers if (n % 2 == 0) == (parity == "even")]
                if len(valid) != 1:
                    errors.append(f"{loc}: parity stem has {len(valid)} defensible answers: {valid}")

    duplicate_excess = sum(len(v) - 1 for v in prompt_locations.values() if len(v) > 1)
    if duplicate_excess:
        errors.append(f"video: {duplicate_excess} duplicate normalized prompts; assessments are not uniquely video-grounded")

    if errors:
        print(f"Assessment educator quality gate failed with {len(errors)} issue(s):")
        for error in errors:
            print(f"- {error}")
        return 1
    print("Assessment educator quality gate passed: 18 arena packs/180 items and 237 videos/1,185 items")
    return 0


if __name__ == "__main__":
    sys.exit(main())
