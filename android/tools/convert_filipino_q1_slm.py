#!/usr/bin/env python3
"""Convert the repaired Filipino Q1 SLM pack into Month1Lesson JSON.

The source pack is intentionally kept separate from the APK assets. This
adapter makes the schema bridge explicit, deterministic, and reviewable.
"""

from __future__ import annotations

import argparse
import json
import zipfile
from pathlib import Path
from typing import Any

CANONICAL_TYPES = [
    "ANIMATED_EXPLANATION",
    "HOTSPOT_IMAGE",
    "SORT_AND_CLASSIFY",
    "MULTIPLE_CHOICE",
    "MATCHING_PAIRS",
    "SEQUENCE_BUILDER",
]

SOURCE_ACTIVITY_ORDER = [
    "ANIMATED_EXPLANATION",
    "HOTSPOT_IMAGE",
    "SORT_AND_CLASSIFY",
    "MULTIPLE_CHOICE",
    "MATCHING_PAIRS",
]


def _feedback() -> dict[str, str]:
    return {
        "correct": "Mahusay! Natutuhan mo ang mahalagang ideya. 🎉",
        "retry": "Balikan ang halimbawa at subukan muli. 💪",
    }


def _clean_next_label(value: Any, fallback: str = "Susunod →") -> str:
    cleaned = " ".join(str(value or fallback).replace("✓", "").split())
    return cleaned or fallback


def _lesson_title(source: dict[str, Any]) -> str:
    title = str(source["title"])
    if source["lessonId"] == "filipino-g3-q1-w06-d01" and title == "Elemento ng Kuwento":
        return "Elemento ng Kuwento · Q1 W06 D01"
    return title


def _hotspot_content(content: dict[str, Any]) -> dict[str, Any]:
    if "examples" in content:
        examples = content.get("examples", [])
        return {"examples": [str(example) for example in examples if str(example).strip()]}
    targets = content.get("targets", [])
    examples = [
        target.get("label", target.get("id", "Halimbawa"))
        if isinstance(target, dict)
        else str(target)
        for target in targets
    ]
    return {"examples": [example for example in examples if example]}


def _derived_hotspot_copy(source: dict[str, Any]) -> dict[str, str]:
    title = str(source.get("title", "aralin"))
    topic = title.split(" · ", 1)[0].split(" (", 1)[0].strip() or "aralin"
    return {
        "instruction": f"Tuklasin ang mga halimbawang kaugnay ng {topic}.",
        "prompt": f"Pindutin ang bawat halimbawang kaugnay ng {topic}.",
        "narration": f"Hanapin natin ang mahahalagang halimbawa tungkol sa {topic}.",
    }


def _activity_content(activity_type: str, content: Any) -> Any:
    if activity_type == "HOTSPOT_IMAGE":
        if isinstance(content, dict) and "examples" in content:
            return content
        return _hotspot_content(content if isinstance(content, dict) else {})
    if activity_type == "ANIMATED_EXPLANATION":
        return content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)
    return content


def _completion_rule(activity_type: str, content: Any) -> dict[str, Any]:
    if activity_type == "HOTSPOT_IMAGE":
        count = len(_hotspot_content(content if isinstance(content, dict) else {}).get("examples", []))
        return {"type": "ALL_TARGETS_VISITED", "targetCount": count}
    if activity_type == "SORT_AND_CLASSIFY" and isinstance(content, dict):
        return {
            "type": "ALL_ITEMS_SORTED",
            "itemCount": len(content.get("fits", [])) + len(content.get("doesNotFit", [])),
        }
    return {"type": "COMPLETE"}


def _activity_completion_rule(
    activity_type: str, source_activity: dict[str, Any], normalized_content: Any
) -> dict[str, Any]:
    generated = _completion_rule(activity_type, normalized_content)
    source_rule = source_activity.get("completionRule")
    if not isinstance(source_rule, dict):
        return generated
    merged = {**generated, **source_rule}
    if activity_type == "HOTSPOT_IMAGE":
        merged["targetCount"] = generated["targetCount"]
    return merged


def _source_text(source: dict[str, Any]) -> str:
    return f"{source.get('title', '')} {source.get('objective', '')}".casefold()


def _derived_sequence_steps(source: dict[str, Any]) -> list[str]:
    text = _source_text(source)
    rules = [
        (("pangngalan",), [
            "Pumili ng tao, lugar, o bagay sa paligid.",
            "Tukuyin ang pangngalan sa napiling halimbawa.",
            "Gamitin ang pangngalan sa isang pangungusap.",
            "Basahin ang pangungusap at suriin ang gamit ng pangngalan.",
        ]),
        (("naunang kaalaman", "karanasan"), [
            "Pakinggan o basahin ang teksto.",
            "Alalahanin ang sariling kaalaman o karanasan.",
            "Hanapin ang pahiwatig na kaugnay ng teksto.",
            "Ipaliwanag kung paano nakatulong ang iyong kaalaman.",
        ]),
        (("tanong tungkol sa kuwento",), [
            "Pakinggan o basahin ang kuwento.",
            "Tukuyin ang tauhan at mahahalagang pangyayari.",
            "Balikan ang bahaging may sagot sa tanong.",
            "Buoin ang sagot gamit ang impormasyon mula sa kuwento.",
        ]),
        (("bahagi ng aklat",), [
            "Basahin ang tanong tungkol sa hinahanap na impormasyon.",
            "Piliin ang bahaging maaaring maglaman ng sagot.",
            "Hanapin ang impormasyon sa napiling bahagi ng aklat.",
            "Gamitin ang nahanap na impormasyon sa sagot.",
        ]),
        (("tatlong pantig", "salitang hiram"), [
            "Basahin nang malinaw ang salita.",
            "Hatiin ang salita sa mga pantig.",
            "Bigkasin ang bawat pantig at pagsamahin ang mga ito.",
            "Suriin kung tama ang pagbasa sa salita.",
        ]),
        (("pagbabaybay",), [
            "Pakinggan at bigkasin ang salita.",
            "Tukuyin ang mga tunog sa tamang pagkakasunod-sunod.",
            "Ayusin ang mga letra upang mabuo ang salita.",
            "Basahin muli at suriin ang baybay.",
        ]),
        (("diksyunaryo",), [
            "Hanapin ang salitang nais ipaliwanag.",
            "Ayusin ang salita ayon sa unang letra.",
            "Basahin ang tala sa diksyunaryo.",
            "Gamitin ang kahulugan sa isang pangungusap.",
        ]),
        (("panghalip panao",), [
            "Tukuyin ang pangngalang papalitan.",
            "Piliin kung ako, ikaw, o siya ang angkop.",
            "Palitan ang pangngalan ng wastong panghalip.",
            "Basahin ang pangungusap at suriin ang kahulugan.",
        ]),
        (("magagalang na pananalita",), [
            "Basahin ang sitwasyon at tukuyin ang kausap.",
            "Isipin kung pagbati, pakiusap, o paumanhin ang kailangan.",
            "Piliin ang magalang na pananalitang angkop.",
            "Basahin ang buong pahayag nang maayos.",
        ]),
        (("elemento ng kuwento",), [
            "Pakinggan o basahin ang kuwento.",
            "Tukuyin ang mga tauhan.",
            "Hanapin ang tagpuan at mahahalagang pangyayari.",
            "Ayusin ang banghay mula simula hanggang wakas.",
        ]),
        (("pagsasalaysay muli",), [
            "Pakinggan o basahin ang teksto.",
            "Tukuyin ang mahahalagang pangyayari.",
            "Ayusin ang mga pangyayari sa tamang pagkakasunod-sunod.",
            "Isalaysay muli ang teksto gamit ang sariling pananalita.",
        ]),
        (("malaki at maliit na letra", "bantas"), [
            "Basahin ang salita o pangungusap.",
            "Tukuyin kung saan kailangan ang malaking letra.",
            "Piliin ang bantas na angkop sa pahayag.",
            "Isulat muli at suriin ang buong pangungusap.",
        ]),
        (("panghalip na pamatlig",), [
            "Tukuyin kung malapit o malayo ang tinutukoy.",
            "Piliin kung ito, iyan, o iyon ang angkop.",
            "Palitan ang pangngalan ng wastong panghalip.",
            "Basahin ang pangungusap at suriin ang gamit nito.",
        ]),
        (("pagbuo ng kuwento",), [
            "Pakinggan o basahin ang huwarang kuwento.",
            "Tukuyin ang tauhan, tagpuan, at suliranin.",
            "Ayusin ang mahahalagang pangyayari.",
            "Bumuo at isalaysay ang sariling kuwento.",
        ]),
    ]
    for needles, steps in rules:
        if any(needle in text for needle in needles):
            return steps
    raise ValueError(f"{source.get('lessonId')}: no approved derived sequence policy")


def _assessment_choices(source: dict[str, Any]) -> tuple[list[str], list[str]]:
    correct: list[str] = []
    incorrect: list[str] = []
    for item in source.get("assessment", {}).get("items", []):
        for choice in item.get("choices", []):
            if not isinstance(choice, dict):
                continue
            text = str(choice.get("text", "")).strip()
            if not text:
                continue
            (correct if choice.get("correct") is True else incorrect).append(text)
    return correct, incorrect


def _derived_activity(source: dict[str, Any], activity_type: str, source_by_type: dict[str, dict[str, Any]]) -> dict[str, Any]:
    correct, incorrect = _assessment_choices(source)
    if activity_type == "HOTSPOT_IMAGE":
        matching_content = source_by_type.get("MATCHING_PAIRS", {}).get("content", {})
        pairs = matching_content.get("pairs", []) if isinstance(matching_content, dict) else []
        examples = [
            str(pair.get("left", "")).strip()
            for pair in pairs
            if isinstance(pair, dict) and str(pair.get("left", "")).strip()
        ]
        if not examples:
            sequence_content = source_by_type.get("SEQUENCE_BUILDER", {}).get("content", {})
            steps = sequence_content.get("steps", []) if isinstance(sequence_content, dict) else []
            examples = [str(step).strip() for step in steps if str(step).strip()]
        if not examples:
            sort_content = source_by_type.get("SORT_AND_CLASSIFY", {}).get("content", {})
            examples = list(sort_content.get("fits", [])) if isinstance(sort_content, dict) else []
        if not examples:
            examples = correct
        if not examples:
            raise ValueError(f"{source.get('lessonId')}: cannot derive hotspot examples")
        copy = _derived_hotspot_copy(source)
        return {
            **copy,
            "content": {"examples": examples[:6]},
        }
    if activity_type == "SORT_AND_CLASSIFY":
        if correct and incorrect:
            return {
                "instruction": "I-grupo ang mga sagot ayon sa pagkakatugma nito sa aralin.",
                "prompt": "Ilagay sa tamang grupo ang bawat sagot.",
                "content": {"fits": correct[:6], "doesNotFit": incorrect[:6]},
            }
        raise ValueError(f"{source.get('lessonId')}: cannot derive sort categories")
    if activity_type == "MATCHING_PAIRS":
        pairs = []
        for item in source.get("assessment", {}).get("items", []):
            choices = [choice for choice in item.get("choices", []) if isinstance(choice, dict) and choice.get("correct") is True]
            if choices:
                pairs.append({"left": item.get("question", ""), "right": choices[0].get("text", "")})
        if not pairs:
            raise ValueError(f"{source.get('lessonId')}: cannot derive matching pairs")
        return {
            "instruction": "Itapat ang bawat tanong sa tamang sagot mula sa aralin.",
            "prompt": "Itapat ang tanong sa tamang sagot.",
            "content": {"pairs": pairs[:5]},
        }
    if activity_type == "SEQUENCE_BUILDER":
        steps = _derived_sequence_steps(source)
        return {
            "instruction": "Ayusin ang mga hakbang ayon sa tamang pagkakasunod-sunod.",
            "prompt": "Ayusin ang mga hakbang mula una hanggang huli.",
            "narration": "Magsimula sa unang hakbang, saka sundan ang mga susunod na hakbang ng aralin.",
            "content": {"steps": steps},
        }
    raise ValueError(f"{source.get('lessonId')}: unsupported derived activity {activity_type}")


def _assessment_items(source: dict[str, Any], lesson_id: str) -> list[dict[str, Any]]:
    converted: list[dict[str, Any]] = []
    for sequence, item in enumerate(source.get("assessment", {}).get("items", []), start=1):
        choices = item.get("choices", [])
        options = []
        correct_ids = []
        for index, choice in enumerate(choices):
            option_id = chr(ord("a") + index)
            text = choice.get("text", "") if isinstance(choice, dict) else str(choice)
            options.append({"id": option_id, "text": text})
            if isinstance(choice, dict) and choice.get("correct") is True:
                correct_ids.append(option_id)
        if len(correct_ids) != 1:
            raise ValueError(f"{lesson_id}: assessment item {sequence} must have one correct choice")
        converted.append(
            {
                "sequence": sequence,
                "itemId": f"{lesson_id}-q{sequence}",
                "type": "MULTIPLE_CHOICE",
                "prompt": item.get("question", ""),
                "options": options,
                "correctOptionIds": correct_ids,
                "explanation": f"Ang pinakamabuting sagot ay: {next(option['text'] for option in options if option['id'] == correct_ids[0])}",
            }
        )
    return converted


def convert_lesson(source: dict[str, Any]) -> dict[str, Any]:
    lesson_id = source["lessonId"]
    source_by_type = {activity["type"]: activity for activity in source.get("activities", [])}
    derived_types: list[str] = []

    activities: list[dict[str, Any]] = []
    for sequence, activity_type in enumerate(CANONICAL_TYPES, start=1):
        source_activity = source_by_type.get(activity_type)
        if source_activity is None:
            source_activity = _derived_activity(source, activity_type, source_by_type)
            derived_types.append(activity_type)
        content = source_activity.get("content", {})
        normalized_content = _activity_content(activity_type, content)
        activities.append(
            {
                "activityId": f"{lesson_id}-a{sequence:02d}",
                "sequence": sequence,
                "type": activity_type,
                "capability": f"{activity_type}_V1",
                "required": True,
                "assetId": source_activity.get("assetId", f"{lesson_id}-visual"),
                "instruction": source_activity.get("instruction", "Sundin ang panuto."),
                "content": normalized_content,
                "completionRule": _activity_completion_rule(activity_type, source_activity, normalized_content),
                "feedback": source_activity.get("feedback") or _feedback(),
                "prompt": source_activity.get("prompt", source_activity.get("instruction", "Sundin ang panuto.")),
                "narration": source_activity.get("narration", source_activity.get("instruction", "Sundin ang panuto.")),
                "guideHint": source_activity.get("guideHint", "Kailangan ng pahiwatig? Basahin muli ang panuto."),
                "nextLabel": _clean_next_label(source_activity.get("nextLabel")),
                "accessibilityAlternative": source_activity.get(
                    "accessibilityAlternative", source_activity.get("instruction", "Sundin ang panuto.")
                ),
            }
        )

    assessment_items = _assessment_items(source, lesson_id)
    lesson = {
        "lessonId": lesson_id,
        "schemaVersion": 1,
        "grade": source["grade"],
        "month": 1,
        "day": source["day"],
        "subject": source["subject"].lower(),
        "title": _lesson_title(source),
        "objective": source["objective"],
        "estimatedMinutes": source.get("estimatedMinutes", 12),
        "educatorValidated": False,
        "releaseStatus": "REQUIRES_EDUCATOR_REVIEW",
        "qualifiesForDailyBadge": True,
        "alignmentStatus": "DERIVED_ACTIVITY_REVIEW_PENDING" if derived_types else "SOURCE_REVIEW_PENDING",
        "language": "fil-PH",
        "introduction": source.get("introduction", "Handa ka na bang matuto kasama si Milo?"),
        "vocabulary": source.get("vocabulary", []),
        "activities": activities,
        "assessment": {
            "purpose": "FORMATIVE_MODULE_CHECK",
            "itemCount": len(assessment_items),
            "passingCorrectCount": source.get("assessment", {}).get("passingCorrectCount", max(1, len(assessment_items) - 1)),
            "claimsMastery": False,
            "items": assessment_items,
        },
        "contentReview": {
            "reviewer": "Pending owner/educator review",
            "source": "ph-matatag-g3-filipino-q1-slm-v2.zip",
            "rewritten": bool(derived_types),
            "derivedActivityTypes": derived_types,
        },
    }
    for field in ("storyIntro", "scene", "accessibility"):
        if field in source:
            lesson[field] = source[field]
    return lesson


def convert_zip(source_zip: Path, lessons_dir: Path, assets_dir: Path) -> int:
    lessons_dir.mkdir(parents=True, exist_ok=True)
    assets_dir.mkdir(parents=True, exist_ok=True)
    converted = 0
    with zipfile.ZipFile(source_zip) as archive:
        prefix = "ph-matatag-g3-filipino-q1-slm-v2/content-pack/"
        for name in archive.namelist():
            relative = name.removeprefix(prefix)
            if relative.startswith("lessons/") and relative.endswith(".json"):
                source = json.loads(archive.read(name))
                output = convert_lesson(source)
                (lessons_dir / Path(relative).name).write_text(
                    json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
                )
                converted += 1
            elif relative.startswith("assets/") and relative.endswith(".svg"):
                (assets_dir / Path(relative).name).write_bytes(archive.read(name))
    return converted


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source_zip", type=Path)
    parser.add_argument("lessons_dir", type=Path)
    parser.add_argument("assets_dir", type=Path)
    args = parser.parse_args()
    print(f"converted_lessons={convert_zip(args.source_zip, args.lessons_dir, args.assets_dir)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
