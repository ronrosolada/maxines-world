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

import json
import subprocess
import sys
import unittest
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent
LESSON_DIR = TOOLS_DIR.parent / "app/src/main/assets/content-pack/month-01/lessons"

sys.path.insert(0, str(TOOLS_DIR))
from repair_english_skill_content import (  # noqa: E402
    JUNK,
    find_skill,
    has_junk,
    instance_index,
    repair_lesson,
)

SKILL_LESSON_COUNTS = {"word": 7, "root": 6, "story": 5, "complete": 4}
GENERIC_MARKERS = [
    "Which example belongs to", "Which choice shows the skill in",
    "What is one example from", "Which situation matches",
    "Which answer demonstrates",
]


def repaired_lessons():
    for path in sorted(LESSON_DIR.glob("*.json")):
        lesson = json.loads(path.read_text(encoding="utf-8"))
        if repair_lesson(lesson) is not None:
            yield lesson


class TestEnglishRepair(unittest.TestCase):
    def test_expected_lesson_count_per_skill(self):
        counts = {}
        for lesson in repaired_lessons():
            skill = find_skill(lesson)
            counts[skill] = counts.get(skill, 0) + 1
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
                self.assertIn(correct, it["explanation"], lesson["lessonId"])
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
        by_skill = {}
        for lesson in repaired_lessons():
            by_skill.setdefault(find_skill(lesson), []).append(lesson)
        for skill, lessons in by_skill.items():
            bodies = {json.dumps(l["activities"] + l["assessment"]["items"],
                                 sort_keys=True) for l in lessons}
            self.assertEqual(len(lessons), len(bodies),
                             f"skill {skill}: {len(lessons)} lessons, {len(bodies)} bodies")

    def test_instance_index_is_stable(self):
        # Two calls must return the same mapping (deterministic ordering).
        lid = "english-g3-q2-w02-d02"
        self.assertEqual(instance_index(lid, "root"), instance_index(lid, "root"))

    def test_repair_is_idempotent(self):
        for lesson in repaired_lessons():
            first = json.dumps(lesson, sort_keys=True)
            repair_lesson(lesson)
            second = json.dumps(lesson, sort_keys=True)
            self.assertEqual(first, second, f"not idempotent: {lesson['lessonId']}")

    def test_pack_check_passes(self):
        result = subprocess.run(
            [sys.executable, str(TOOLS_DIR / "repair_english_skill_content.py"), "--check"],
            capture_output=True, text=True, cwd=TOOLS_DIR.parent,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
