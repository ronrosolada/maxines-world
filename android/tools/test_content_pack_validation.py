#!/usr/bin/env python3
"""Tests for the strict/audit content-pack validator."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from content_pack_validation import validate_pack  # noqa: E402


ACTIVITY_TYPES = [
    "ANIMATED_EXPLANATION",
    "HOTSPOT_IMAGE",
    "SORT_AND_CLASSIFY",
    "MULTIPLE_CHOICE",
    "MATCHING_PAIRS",
    "SEQUENCE_BUILDER",
]

SNAPSHOT = {
    "lesson_count": 1,
    "activities_per_lesson": 6,
    "assessment_items_per_lesson": 5,
    "activity_types": ACTIVITY_TYPES,
}


def valid_lesson(lesson_id: str = "english-g3-q1-w01-d01", *, typed=True) -> dict:
    content = {
        "ANIMATED_EXPLANATION": "A focused explanation.",
        "HOTSPOT_IMAGE": {"assetId": "example-stem", "examples": ["one", "two"]},
        "SORT_AND_CLASSIFY": {"fits": ["one"], "doesNotFit": ["two"]},
        "MULTIPLE_CHOICE": {
            "options": [{"id": "a", "text": "one"}, {"id": "b", "text": "two"}],
            "correctIndex": 0,
        },
        "MATCHING_PAIRS": {"pairs": [{"left": "one", "right": "uno"}]},
        "SEQUENCE_BUILDER": {"steps": ["first", "second"]},
    }
    activities = [
        {
            "activityId": f"{lesson_id}-a{n:02d}",
            "sequence": n,
            "type": activity_type,
            "content": content[activity_type],
        }
        for n, activity_type in enumerate(ACTIVITY_TYPES, start=1)
    ]
    items = []
    for n in range(1, 6):
        item = {
            "itemId": f"q{n:02d}",
            "sequence": n,
            "prompt": f"Question {n}",
            "options": [{"id": "a", "text": "one"}, {"id": "b", "text": "two"}],
            "correctOptionIds": ["a"],
            "explanation": "Because one is correct.",
        }
        if typed:
            item["type"] = "MULTIPLE_CHOICE"
        items.append(item)
    return {
        "lessonId": lesson_id,
        "schemaVersion": 1,
        "grade": 3,
        "month": 1,
        "day": 1,
        "subject": "english",
        "title": "A lesson",
        "objective": "Identify one.",
        "estimatedMinutes": 10,
        "educatorValidated": True,
        "releaseStatus": "RELEASED",
        "qualifiesForDailyBadge": True,
        "alignmentStatus": "ALIGNED",
        "language": "en-PH",
        "introduction": "An introduction.",
        "vocabulary": [],
        "activities": activities,
        "assessment": {
            "purpose": "FORMATIVE_MODULE_CHECK",
            "itemCount": 5,
            "passingCorrectCount": 4,
            "claimsMastery": False,
            "items": items,
        },
    }


def write_lesson(root: Path, lesson: dict, name: str | None = None) -> None:
    filename = name or f"{lesson['lessonId']}.json"
    (root / filename).write_text(json.dumps(lesson, indent=2) + "\n", encoding="utf-8")


class ContentPackValidationTests(unittest.TestCase):
    def test_valid_pack_passes_audit(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp)
            write_lesson(pack, valid_lesson())
            report = validate_pack(pack, snapshot=SNAPSHOT)

        self.assertEqual([], report.errors)
        self.assertEqual(0, report.error_count)
        self.assertEqual(1, report.lesson_count)

    def test_allowed_passing_count_set_preserves_authored_threshold(self):
        lesson = valid_lesson()
        lesson["assessment"]["passingCorrectCount"] = 3
        snapshot = {**SNAPSHOT, "assessment_passing_correct_count": [3, 4]}
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp)
            write_lesson(pack, lesson)
            report = validate_pack(pack, snapshot=snapshot)

        self.assertEqual([], report.errors)

    def test_malformed_json_is_an_error_and_does_not_abort_scan(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp)
            write_lesson(pack, valid_lesson())
            (pack / "broken.json").write_text("{not json\n", encoding="utf-8")
            report = validate_pack(pack)

        self.assertTrue(any(f.category == "json" for f in report.errors))
        self.assertEqual(2, report.files_seen)

    def test_missing_assessment_type_is_warning_in_audit_but_error_in_strict_mode(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp)
            write_lesson(pack, valid_lesson(typed=False))
            audit = validate_pack(pack)
            strict = validate_pack(pack, strict=True)

        self.assertEqual([], audit.errors)
        self.assertTrue(any(f.category == "assessment_type" for f in audit.warnings))
        self.assertTrue(any(f.category == "assessment_type" for f in strict.errors))

    def test_invalid_correct_option_is_always_an_error(self):
        lesson = valid_lesson()
        lesson["assessment"]["items"][0]["correctOptionIds"] = ["missing"]
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp)
            write_lesson(pack, lesson)
            report = validate_pack(pack)

        self.assertTrue(any(f.category == "assessment" for f in report.errors))

    def test_empty_required_string_is_a_schema_error(self):
        lesson = valid_lesson()
        lesson["title"] = "  "
        with tempfile.TemporaryDirectory() as tmp:
            pack = Path(tmp)
            write_lesson(pack, lesson)
            report = validate_pack(pack)

        self.assertTrue(any(f.category == "schema" for f in report.errors))


if __name__ == "__main__":
    unittest.main()
