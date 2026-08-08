#!/usr/bin/env python3
"""Deterministic educational-material gate for the bundled Grade 3 pack.

This complements the schema/quality/similarity gates. It is intentionally
strict about defects that let a child win by memorizing a repeated answer or
leave the learner with no useful retry clue.
"""
from __future__ import annotations

import argparse
import collections
import json
import re
import sys
from pathlib import Path

NORMALIZE = re.compile(r"[^\w\s]+", re.UNICODE)
GENERIC_FEEDBACK = {
    "look at the example again and try once more",
    "balikan ang halimbawa at subukan muli",
    "mahusay nakita mo ang mahalagang ideya",
    "great thinking you found the key idea",
}
GENERIC_INSTRUCTIONS = {
    "pag-aralan ang ideya at pakinggan ang salaysay",
    "suriin ang bawat halimbawa at basahin ang mga halimbawa",
    "ilagay ang bawat card sa angkop o hindi angkop",
    "itambal ang bawat halimbawa sa ideya ng aralin",
    "tapusin nang sunod sunod ang gabay na balik aral",
    "study the idea and listen to milo",
    "explore each example and find the important detail",
    "sort each example into the correct group",
    "choose the best answer",
    "match the ideas that belong together",
    "put the steps in the correct order",
}
PLACEHOLDER_FRAGMENTS = (
    "a clear example", "a second example", "a real-life connection",
    "a correct example", "a related example", "another example",
    "evidence from the example", "matches the lesson idea", "fits the idea",
    "does not match what we learned", "kasanayang kasanayan",
)
FILIPINO_BLEED = re.compile(
    r"\b(?:protected wildlife|protected|organism|adult|overlay|recipe uses|"
    r"community helper|account|record|history|skill|lesson|answer|example|"
    r"direction|point|line|legend|screen)\b",
    re.I,
)

def norm(value: object) -> str:
    return NORMALIZE.sub(" ", str(value).lower()).strip()

def surface(value: object) -> str:
    """Compare learner text without erasing meaningful punctuation.

    Capitalization and end marks are the *skill* in several English lessons,
    so the audit must not call ``Milo reads.`` and ``milo reads.`` duplicates.
    """
    return re.sub(r"\s+", " ", str(value).casefold().strip())

def literal_surface(value: object) -> str:
    """Preserve case and punctuation for capitalization/punctuation lessons."""
    return re.sub(r"\s+", " ", str(value).strip())

def walk_strings(value: object, path: str = ""):
    if isinstance(value, dict):
        for key, item in value.items():
            yield from walk_strings(item, f"{path}.{key}" if path else key)
    elif isinstance(value, list):
        for i, item in enumerate(value):
            yield from walk_strings(item, f"{path}[{i}]")
    elif isinstance(value, str):
        yield path, value

def item_signature(item: dict) -> tuple:
    options = tuple((str(o.get("id")), surface(o.get("text"))) for o in item.get("options", []))
    return (surface(item.get("prompt")), options, tuple(item.get("correctOptionIds", [])))

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--lessons", default="app/src/main/assets/content-pack/month-01/lessons")
    args = parser.parse_args()
    files = sorted(Path(args.lessons).glob("*.json"))
    issues: collections.defaultdict[str, list[str]] = collections.defaultdict(list)
    prompts: collections.defaultdict[str, list[str]] = collections.defaultdict(list)
    signatures: collections.defaultdict[tuple, list[str]] = collections.defaultdict(list)
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        lid = str(data.get("lessonId", path.stem))
        for field, value in walk_strings(data):
            # Provenance/review metadata is not learner-facing content. Keep
            # source-language names and audit notes out of the child-language
            # and template-quality gates.
            if field.startswith("contentReview") or field.startswith("sourceRecords"):
                continue
            lowered = norm(value)
            if lowered in GENERIC_FEEDBACK:
                issues["generic_feedback"].append(f"{lid}:{field}: {value}")
            if lowered in GENERIC_INSTRUCTIONS:
                issues["generic_instruction"].append(f"{lid}:{field}: {value}")
            if any(fragment in value.lower() for fragment in PLACEHOLDER_FRAGMENTS):
                issues["placeholder_or_meta"].append(f"{lid}:{field}: {value}")
            if re.search(r"(?:to practice|while practicing|shows this skill|matches what we learned)", value, re.I):
                issues["template_or_objective_prompt"].append(f"{lid}:{field}: {value}")
            if data.get("language") == "fil-PH" and field != "objective" and "assetSpecs" not in field and FILIPINO_BLEED.search(value):
                issues["filipino_english_bleed"].append(f"{lid}:{field}: {value}")
            if (field.endswith(".instruction") or field.endswith(".feedback.correct") or field.endswith(".feedback.retry")) and len(value) > 90:
                issues["learner_text_too_long"].append(f"{lid}:{field} ({len(value)}): {value}")
        for activity in data.get("activities", []) or []:
            kind = activity.get("type")
            content = activity.get("content") or {}
            if kind == "SORT_AND_CLASSIFY":
                fits = {literal_surface(x) for x in content.get("fits", [])}
                misses = {literal_surface(x) for x in content.get("doesNotFit", [])}
                overlap = sorted(fits & misses)
                if overlap:
                    issues["sort_overlap"].append(f"{lid}:{overlap}")
            if kind == "MATCHING_PAIRS":
                pairs = content.get("pairs", [])
                left = [surface(p.get("left")) for p in pairs]
                right = [surface(p.get("right")) for p in pairs]
                if len(right) != len(set(right)):
                    issues["matching_duplicate_right"] .append(lid)
                if any(left_item == right_item for left_item, right_item in zip(left, right)):
                    issues["matching_identity_pair"].append(lid)
                for pair, left_item, right_item in zip(pairs, left, right):
                    raw_left = surface(pair.get("left"))
                    clue = surface(raw_left.rsplit("—", 1)[-1]) if "—" in raw_left else ""
                    if clue and len(right_item.split()) <= 3 and (right_item == clue or right_item in clue):
                        issues["matching_right_repeats_clue"].append(f"{lid}: {pair}")
                if any(not x for x in left + right):
                    issues["matching_empty_side"].append(lid)
            if kind == "MULTIPLE_CHOICE":
                options = [literal_surface(x) for x in content.get("options", [])]
                correct = content.get("correctIndex")
                if len(options) != len(set(options)):
                    issues["activity_duplicate_options"].append(lid)
                if not isinstance(correct, int) or not 0 <= correct < len(options):
                    issues["activity_bad_key"].append(lid)
        for item in (data.get("assessment") or {}).get("items", []) or []:
            key = surface(item.get("prompt"))
            if key:
                prompts[key].append(f"{lid}:{item.get('itemId', '?')}")
            signatures[item_signature(item)].append(f"{lid}:{item.get('itemId', '?')}")
            options = [literal_surface(o.get("text")) for o in item.get("options", [])]
            if len(options) != len(set(options)):
                issues["assessment_duplicate_options"].append(f"{lid}:{item.get('itemId', '?')}")
            ids = {str(o.get("id")) for o in item.get("options", [])}
            keys = [str(x) for x in item.get("correctOptionIds", [])]
            if len(keys) != 1 or keys[0] not in ids:
                issues["assessment_bad_key"].append(f"{lid}:{item.get('itemId', '?')}")
    for key, values in prompts.items():
        lesson_ids = {value.split(":", 1)[0] for value in values}
        if len(lesson_ids) > 1 or len(values) > 1:
            issues["duplicate_prompts"].append(f"{values[:8]}" + (" ..." if len(values) > 8 else ""))
    for key, values in signatures.items():
        lesson_ids = {value.split(":", 1)[0] for value in values}
        if len(lesson_ids) > 1 or len(values) > 1:
            issues["duplicate_assessments"].append(f"{values[:8]}" + (" ..." if len(values) > 8 else ""))
    print(f"lessons: {len(files)}")
    total = 0
    blocking = {"generic_feedback", "generic_instruction", "placeholder_or_meta",
                "sort_overlap", "matching_duplicate_right", "matching_empty_side",
                "activity_duplicate_options", "activity_bad_key", "assessment_duplicate_options",
                "assessment_bad_key", "duplicate_prompts", "duplicate_assessments",
                "template_or_objective_prompt", "matching_identity_pair", "matching_right_repeats_clue",
                "filipino_english_bleed", "learner_text_too_long"}
    for name in sorted(issues):
        count = len(issues[name])
        total += count
        print(f"{name:28}: {count}")
        for sample in issues[name][:4]:
            print(f"  {sample}")
    print(f"TOTAL findings: {total}")
    return 1 if any(issues[name] for name in blocking) else 0

if __name__ == "__main__":
    raise SystemExit(main())
