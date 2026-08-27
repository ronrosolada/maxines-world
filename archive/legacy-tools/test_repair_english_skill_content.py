#!/usr/bin/env python3
"""Regression tests for repair_english_skill_content.py.

Guards the English quarterly content repair:
- exactly 22 lesson IDs repaired (7 word + 6 root + 5 story + 4 complete)
- zero stock-junk strings anywhere in the repaired lessons
- 5 assessment items per lesson, unique option ids, valid correctOptionIds
- unique prompts within each lesson, no generic title-substituted templates
- correct option text matches the explanation text
- instances of the same skill group are differentiated (bodies differ)
- repair is idempotent (running the tool on the pack twice = no change)
"""
from __future__ import annotations

import copy
import json
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

TOOLS_DIR = Path(__file__).resolve().parent
LESSON_DIR = TOOLS_DIR.parent / "app/src/main/assets/content-pack/month-01/lessons"

sys.path.insert(0, str(TOOLS_DIR))
import repair_english_skill_content as r  # noqa: E402
from repair_english_skill_content import (  # noqa: E402
    JUNK,
    find_skill,
    has_junk,
    instance_index,
    repair_lesson,
)
from content_review_targets import ENGLISH_GROUPS

SKILL_LESSON_COUNTS = {"word": 7, "root": 6, "story": 5, "complete": 4}
GENERIC_MARKERS = [
    "Which example belongs to", "Which choice shows the skill in",
    "What is one example from", "Which situation matches",
    "Which answer demonstrates",
]


def repaired_lessons():
    for lesson_ids in ENGLISH_GROUPS.values():
        for lesson_id in lesson_ids:
            yield json.loads(
                (LESSON_DIR / f"{lesson_id}.json").read_text(encoding="utf-8")
            )


class TestEnglishRepair(unittest.TestCase):
    def test_expected_lesson_count_per_skill(self):
        counts = {skill: len(lesson_ids) for skill, lesson_ids in ENGLISH_GROUPS.items()}
        self.assertEqual(counts, SKILL_LESSON_COUNTS)

    def test_no_junk_anywhere(self):
        for lesson in repaired_lessons():
            blob = json.dumps(lesson).lower()
            hits = [j for j in JUNK if j in blob]
            self.assertEqual([], hits, f"{lesson['lessonId']}: {hits}")

    def test_assessment_structure(self):
        for lesson in repaired_lessons():
            items = lesson["assessment"]["items"]
            self.assertEqual(5, len(items), lesson["lessonId"])
            prompts = []
            for it in items:
                ids = [o["id"] for o in it["options"]]
                self.assertEqual(sorted(ids), ["a", "b", "c", "d"], lesson["lessonId"])
                self.assertEqual(1, len(it["correctOptionIds"]), lesson["lessonId"])
                self.assertIn(it["correctOptionIds"][0], ids, lesson["lessonId"])
                correct = next(o["text"] for o in it["options"]
                               if o["id"] in it["correctOptionIds"])
                correct_words = set(re.findall(r"[A-Za-z]{3,}", correct.lower()))
                explanation_words = set(re.findall(r"[A-Za-z]{3,}", it["explanation"].lower()))
                self.assertTrue(
                    correct_words.intersection(explanation_words),
                    f"explanation does not connect to correct option: {lesson['lessonId']}",
                )
                prompts.append(it["prompt"])
            self.assertEqual(len(set(prompts)), 5, f"dup prompts in {lesson['lessonId']}")

    def test_no_generic_prompt_templates(self):
        for lesson in repaired_lessons():
            for it in lesson["assessment"]["items"]:
                for marker in GENERIC_MARKERS:
                    self.assertNotIn(marker, it["prompt"], lesson["lessonId"])

    def test_vocabulary_is_real(self):
        for lesson in repaired_lessons():
            for v in lesson["vocabulary"]:
                self.assertNotIn(v["term"].lower(), [j.lower() for j in JUNK])
                self.assertNotIn(v["definition"].lower(), [j.lower() for j in JUNK])
                self.assertNotEqual(v["term"], v["definition"])

    def test_same_skill_instances_are_differentiated(self):
        by_id = {lesson["lessonId"]: lesson for lesson in repaired_lessons()}
        for skill, lesson_ids in ENGLISH_GROUPS.items():
            lessons = [by_id[lesson_id] for lesson_id in lesson_ids]
            bodies = {json.dumps(l["activities"] + l["assessment"]["items"],
                                 sort_keys=True) for l in lessons}
            self.assertEqual(len(lessons), len(bodies),
                             f"skill {skill}: {len(lessons)} lessons, {len(bodies)} bodies")

    def test_focus_objectives_are_unique(self):
        for skill, lesson_ids in ENGLISH_GROUPS.items():
            objectives = {
                next(
                    lesson["objective"]
                    for lesson in repaired_lessons()
                    if lesson["lessonId"] == lesson_id
                )
                for lesson_id in lesson_ids
            }
            self.assertEqual(len(objectives), len(lesson_ids), skill)

    def test_repair_is_idempotent(self):
        for lesson in repaired_lessons():
            first = json.dumps(lesson, sort_keys=True)
            second_lesson = copy.deepcopy(lesson)
            self.assertIsNone(r.repair_lesson(second_lesson))
            second = json.dumps(second_lesson, sort_keys=True)
            self.assertEqual(first, second, f"not idempotent: {lesson['lessonId']}")

    def test_legacy_fixture_exercises_repair_function(self):
        source = next(repaired_lessons())
        fixture_id = "english-g3-q99-w99-d01"
        fixture = copy.deepcopy(source)
        fixture["lessonId"] = fixture_id
        fixture["objective"] = "Use high-frequency and content-specific words in context."
        fixture["vocabulary"] = [{"term": "a correct example", "definition": "a correct example"}]

        with tempfile.TemporaryDirectory() as temp:
            lesson_dir = Path(temp)
            (lesson_dir / f"{fixture_id}.json").write_text(
                json.dumps(fixture), encoding="utf-8"
            )
            with patch.object(r, "LESSONS", lesson_dir):
                repaired = r.repair_lesson(copy.deepcopy(fixture))
                repaired_again = r.repair_lesson(copy.deepcopy(repaired))

        self.assertIsNotNone(repaired)
        self.assertNotIn("a correct example", json.dumps(repaired))
        self.assertEqual(repaired, repaired_again)

    def test_pack_check_passes(self):
        result = subprocess.run(
            [sys.executable, str(TOOLS_DIR / "repair_english_skill_content.py"), "--check"],
            capture_output=True, text=True, cwd=TOOLS_DIR.parent,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
