#!/usr/bin/env python3
"""Add reusable visual cues and one objective-linked practice activity per lesson.

The transformer is intentionally deterministic and idempotent. It edits only the
bundled lesson JSON files, preserves authored assessment content, and uses the
existing INTERACTIVE_SPEC_V1 runtime capability for the added activity.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


SUBJECT_SCENES = {
    "english": "🐱📚🔎",
    "filipino": "🐱🗣️📖",
    "mathematics": "🐱🔢🧩",
    "science": "🐱🔬🌱",
    "makabansa": "🐱🗺️🏘️",
    "araling_panlipunan": "🐱🗺️🏘️",
    "gmrc": "🐱💛🤝",
}

ACTIVITY_SCENE_SUFFIX = {
    "ANIMATED_EXPLANATION": "✨",
    "HOTSPOT_IMAGE": "🔎",
    "SORT_AND_CLASSIFY": "🧺",
    "MULTIPLE_CHOICE": "🎯",
    "MATCHING_PAIRS": "🔗",
    "SEQUENCE_BUILDER": "🪜",
    "INTERACTIVE_SPEC": "🧭",
}

CELEBRATION = "🎉⭐🐾"

GENERIC_OPTION_PLACEHOLDERS = {
    "a random symbol", "an unrelated guess", "a detail not in the lesson",
    "a different topic", "a correct example", "a related example",
    "another example", "a clear example", "a second example",
    "a real-life connection", "an unsupported claim", "two unrelated objects",
    "a color with no event", "effect before its cause", "a title only",
    "hula na walang batayan", "hindi magalang na pahayag",
    "salitang walang kaugnayan", "paksang iba sa aralin",
}


def subject_key(lesson: dict[str, Any]) -> str:
    raw = str(lesson.get("subject", "")).strip().lower()
    return raw.replace("-", "_").replace(" ", "_")


def is_filipino(lesson: dict[str, Any]) -> bool:
    language = str(lesson.get("language", "")).lower()
    return language.startswith("fil") or subject_key(lesson) == "filipino"


def scene_for(lesson: dict[str, Any], activity_type: str) -> str:
    base = SUBJECT_SCENES.get(subject_key(lesson), "🐱✨🔎")
    return base + ACTIVITY_SCENE_SUFFIX.get(activity_type, "✨")


def unique_strings(values: list[Any]) -> list[str]:
    result: list[str] = []
    for value in values:
        text = str(value).strip()
        if text and text not in result:
            result.append(text)
    return result


def subject_challenge(lesson: dict[str, Any]) -> dict[str, Any] | None:
    """Return a safe subject-specific choice set from authored MC content."""
    kind_by_subject = {
        "mathematics": "math_choice_v1",
        "science": "science_choice_v1",
        "gmrc": "values_choice_v1",
        "makabansa": "community_choice_v1",
        "araling_panlipunan": "community_choice_v1",
    }
    kind = kind_by_subject.get(subject_key(lesson))
    if kind is None:
        return None
    for activity in lesson.get("activities", []):
        if not isinstance(activity, dict) or activity.get("type") != "MULTIPLE_CHOICE":
            continue
        content = activity.get("content")
        if not isinstance(content, dict):
            continue
        options = content.get("options")
        correct_index = content.get("correctIndex")
        if not isinstance(options, list) or not 3 <= len(options) <= 4:
            continue
        if not isinstance(correct_index, int) or correct_index not in range(len(options)):
            continue
        normalized = [str(option).strip() for option in options]
        if any(not option for option in normalized) or len(set(normalized)) != len(normalized):
            continue
        if any(option.casefold() in GENERIC_OPTION_PLACEHOLDERS for option in normalized):
            continue
        return {"kind": kind, "options": normalized, "correctIndex": correct_index}
    return None


def vocabulary_definitions(lesson: dict[str, Any]) -> tuple[str, list[str], bool] | None:
    vocabulary = lesson.get("vocabulary")
    if not isinstance(vocabulary, list):
        return None
    terms = [item for item in vocabulary if isinstance(item, dict)]
    if not terms:
        return None
    first_term = str(terms[0].get("term", "")).strip()
    definitions = unique_strings([item.get("definition", "") for item in terms])
    # Some vocabulary packs intentionally group two words under one shared
    # definition (for example, a synonym pair). Reuse additional authored
    # matching-pair explanations before considering the lesson incomplete.
    for activity in lesson.get("activities", []):
        if not isinstance(activity, dict) or not isinstance(activity.get("content"), dict):
            continue
        pairs = activity["content"].get("pairs", [])
        if isinstance(pairs, list):
            definitions.extend(
                str(pair.get("right", ""))
                for pair in pairs
                if isinstance(pair, dict)
            )
    definitions = unique_strings(definitions)
    if first_term and len(definitions) >= 3:
        return first_term, definitions[:3], True

    # A few values-led lessons intentionally reuse one definition for several
    # related terms. Their sort activity still contains authored positive and
    # negative examples, which make a stronger clue-compass activity than
    # fabricated definition distractors.
    for activity in lesson.get("activities", []):
        if not isinstance(activity, dict) or not isinstance(activity.get("content"), dict):
            continue
        content = activity["content"]
        fits = content.get("fits", [])
        does_not_fit = content.get("doesNotFit", [])
        choices = unique_strings(
            list(fits[:1] if isinstance(fits, list) else [])
            + list(does_not_fit[:2] if isinstance(does_not_fit, list) else [])
        )
        if len(choices) >= 3:
            return "", choices[:3], False
    return None


def localized_copy(lesson: dict[str, Any], term: str, definition_mode: bool) -> dict[str, str]:
    if not definition_mode:
        if subject_challenge(lesson) is not None:
            if is_filipino(lesson):
                return {
                    "instruction": "Piliin ang halimbawang tumutugma sa misyon ng aralin sa clue compass.",
                    "narration": "Suriin ang mga halimbawa at piliin ang pinakatamang sagot.",
                    "hint": "Balikan ang layunin at hanapin ang halimbawang tumutugma rito.",
                    "correct": "Tama! Nahanap mo ang sagot sa misyon. 🎉",
                    "retry": "Malapit na! Basahin muli ang bawat halimbawa. 💪",
                    "next": "Sunod →",
                }
            return {
                "instruction": "Choose the example that matches today’s subject mission on Milo’s compass.",
                "narration": "Read each example and choose the one that best fits the lesson goal.",
                "hint": "Review today’s goal, then look for the example that matches it.",
                "correct": "Great mission solving! 🎉",
                "retry": "Almost there! Read each example again. 💪",
                "next": "Next →",
            }
        if is_filipino(lesson):
            return {
                "instruction": "Piliin ang halimbawang kabilang sa misyon ni Milo sa clue compass.",
                "narration": "Basahin ang tatlong halimbawa at piliin ang tumutugma sa aralin.",
                "hint": "Hanapin ang halimbawang kabilang sa tamang pangkat.",
                "correct": "Tama! Nahanap mo ang clue. 🎉",
                "retry": "Malapit na! Basahin muli ang mga halimbawa. 💪",
                "next": "Sunod →",
            }
        return {
            "instruction": "Choose the clue that belongs to today’s mission on Milo’s compass.",
            "narration": "Read the three examples, then choose the one that fits today’s lesson.",
            "hint": "Look for the example that belongs in the correct group.",
            "correct": "Great clue hunting! 🎉",
            "retry": "Almost there! Read the examples again. 💪",
            "next": "Next →",
        }
    if is_filipino(lesson):
        return {
            "instruction": f"Hanapin ang kahulugan ng “{term}” sa clue compass.",
            "narration": "Basahin ang tatlong pahiwatig at piliin ang kahulugang tama.",
            "hint": f"Hanapin ang paliwanag para sa {term}.",
            "correct": "Tama! Nahanap mo ang clue. 🎉",
            "retry": "Malapit na! Basahin muli ang mga pahiwatig. 💪",
            "next": "Sunod →",
        }
    return {
        "instruction": f"Find the meaning of “{term}” on Milo’s clue compass.",
        "narration": "Read the three clues, then choose the meaning that fits.",
        "hint": f"Look for the explanation of {term}.",
        "correct": "Great clue hunting! 🎉",
        "retry": "Almost there! Read the clues again. 💪",
        "next": "Next →",
    }


def add_visual_metadata(lesson: dict[str, Any]) -> bool:
    changed = False
    for activity in lesson.get("activities", []):
        if not isinstance(activity, dict):
            continue
        activity_type = str(activity.get("type", ""))
        content = activity.get("content")
        existing_scene = content.get("visualScene") if isinstance(content, dict) else None
        scene = str(existing_scene or activity.get("visualScene") or scene_for(lesson, activity_type))
        if activity.get("visualScene") != scene:
            activity["visualScene"] = scene
            changed = True
        if activity.get("celebrationEmoji") != CELEBRATION:
            activity["celebrationEmoji"] = CELEBRATION
            changed = True
        if isinstance(content, dict):
            if content.get("visualScene") != scene:
                content["visualScene"] = scene
                changed = True
            if content.get("celebrationEmoji") != CELEBRATION:
                content["celebrationEmoji"] = CELEBRATION
                changed = True
    return changed


def normalize_compass_activity(lesson: dict[str, Any], activity: dict[str, Any]) -> bool:
    """Rotate compass choices so the authored answer is not always first."""
    if activity.get("type") != "INTERACTIVE_SPEC":
        return False
    content = activity.get("content")
    if not isinstance(content, dict):
        return False
    if content.get("answerOrder") == "lesson-sha256-v1":
        return False
    options = content.get("options")
    current_index = content.get("correctIndex")
    if not isinstance(options, list) or len(options) < 2:
        return False
    if not isinstance(current_index, int) or current_index not in range(len(options)):
        return False

    correct = options[current_index]
    canonical = [correct] + [option for index, option in enumerate(options) if index != current_index]
    lesson_id = str(lesson.get("lessonId", ""))
    target_index = hashlib.sha256(lesson_id.encode("utf-8")).digest()[0] % len(canonical)
    rotated = canonical[-target_index:] + canonical[:-target_index] if target_index else canonical

    changed = (
        rotated != options
        or target_index != current_index
        or content.get("answerOrder") != "lesson-sha256-v1"
    )
    if changed:
        content["options"] = rotated
        content["correctIndex"] = target_index
        content["answerOrder"] = "lesson-sha256-v1"
    return changed


def build_compass_activity(lesson: dict[str, Any]) -> dict[str, Any] | None:
    lesson_id = str(lesson.get("lessonId", "")).strip()
    challenge = subject_challenge(lesson)
    vocabulary = vocabulary_definitions(lesson)
    if not lesson_id or (challenge is None and vocabulary is None):
        return None
    if challenge is not None:
        term, definitions, definition_mode = "", challenge["options"], False
        challenge_kind = challenge["kind"]
        correct_index = challenge["correctIndex"]
    else:
        if vocabulary is None:
            return None
        term, definitions, definition_mode = vocabulary
        challenge_kind = "vocabulary_clue_v1"
        correct_index = 0
    copy = localized_copy(lesson, term, definition_mode)
    sequence = max((int(a.get("sequence", 0)) for a in lesson.get("activities", []) if isinstance(a, dict)), default=0) + 1
    return {
        "activityId": f"{lesson_id}-a{sequence:02d}",
        "sequence": sequence,
        "type": "INTERACTIVE_SPEC",
        "capability": "INTERACTIVE_SPEC_V1",
        "required": True,
        "assetId": f"{lesson_id}-visual",
        "visualScene": scene_for(lesson, "INTERACTIVE_SPEC"),
        "celebrationEmoji": CELEBRATION,
        "instruction": copy["instruction"],
        "content": {
            "options": definitions,
            "correctIndex": correct_index,
            "challengeKind": challenge_kind,
            "visualScene": scene_for(lesson, "INTERACTIVE_SPEC"),
            "celebrationEmoji": CELEBRATION,
            "hint": copy["hint"],
        },
        "completionRule": {"type": "CORRECT_RESPONSE"},
        "feedback": {"correct": copy["correct"], "retry": copy["retry"]},
        "prompt": copy["instruction"],
        "narration": copy["narration"],
        "guideHint": copy["hint"],
        "nextLabel": copy["next"],
        "accessibilityAlternative": copy["instruction"],
    }


def refresh_subject_challenge(lesson: dict[str, Any], activity: dict[str, Any]) -> bool:
    """Upgrade an existing compass when authored subject choices are safe."""
    challenge = subject_challenge(lesson)
    if challenge is None or activity.get("type") != "INTERACTIVE_SPEC":
        return False
    content = activity.get("content")
    if not isinstance(content, dict) or content.get("challengeKind") == challenge["kind"]:
        return False
    desired = build_compass_activity(lesson)
    if desired is None:
        return False
    for key in (
        "instruction", "visualScene", "celebrationEmoji", "prompt", "narration",
        "guideHint", "nextLabel", "accessibilityAlternative", "feedback",
    ):
        if key in desired and activity.get(key) != desired[key]:
            activity[key] = desired[key]
    desired_content = desired.get("content", {})
    for key in (
        "options", "correctIndex", "challengeKind", "visualScene",
        "celebrationEmoji", "hint",
    ):
        if key in desired_content and content.get(key) != desired_content[key]:
            content[key] = desired_content[key]
    content.pop("answerOrder", None)
    return True


def transform(path: Path, write: bool) -> tuple[bool, str]:
    original = path.read_text(encoding="utf-8")
    lesson = json.loads(original)
    activities = lesson.get("activities")
    if not isinstance(activities, list):
        return False, "missing activities list"

    changed = False
    if lesson.get("educatorValidated") is not False:
        lesson["educatorValidated"] = False
        changed = True
    if lesson.get("releaseStatus") != "REQUIRES_EDUCATOR_REVIEW":
        lesson["releaseStatus"] = "REQUIRES_EDUCATOR_REVIEW"
        changed = True

    changed = add_visual_metadata(lesson) or changed
    has_compass = any(
        isinstance(activity, dict) and activity.get("type") == "INTERACTIVE_SPEC"
        for activity in activities
    )
    if not has_compass:
        compass = build_compass_activity(lesson)
        if compass is None:
            return False, "missing three vocabulary definitions"
        activities.append(compass)
        lesson["estimatedMinutes"] = int(lesson.get("estimatedMinutes", 12)) + 2
        changed = True

    for activity in activities:
        if isinstance(activity, dict):
            changed = refresh_subject_challenge(lesson, activity) or changed
            if normalize_compass_activity(lesson, activity):
                changed = True

    if write and changed:
        indent = 2
        for line in original.splitlines()[1:]:
            whitespace = line[: len(line) - len(line.lstrip())]
            if whitespace:
                indent = len(whitespace.expandtabs(2))
                break
        path.write_text(
            json.dumps(lesson, ensure_ascii=False, indent=indent) + "\n",
            encoding="utf-8",
        )
    return changed, "updated" if changed else "already complete"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons",
    )
    parser.add_argument("--check", action="store_true", help="validate without writing")
    args = parser.parse_args()

    files = sorted(args.root.glob("*.json"))
    if not files:
        print(f"No lesson JSON files found under {args.root}")
        return 1

    changed = 0
    failures: list[tuple[str, str]] = []
    for path in files:
        try:
            did_change, message = transform(path, write=not args.check)
            if did_change:
                changed += 1
            if message.startswith("missing"):
                failures.append((path.name, message))
        except Exception as exc:  # pragma: no cover - CLI guard
            failures.append((path.name, f"{type(exc).__name__}: {exc}"))

    mode = "CHECK" if args.check else "UPDATED"
    print(f"{mode}: {len(files)} lessons; changed={changed}; failures={len(failures)}")
    for name, reason in failures:
        print(f"FAIL {name}: {reason}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
