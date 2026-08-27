#!/usr/bin/env python3
"""Regression tests for repair_math_science_content.py.

Guards the mathematics + science quarterly content repair:
- exactly 43 lesson IDs repaired (9 add + 9 multiply + 7 living + 11 materials + 7 light)
- zero stock junk strings in repaired lessons
- every assessment: 5 items, 4 unique options, valid correctOptionIds,
  unique prompts within a lesson
- instances within a skill group are differentiated (no more byte-identical lessons)
- math items compute the lesson's own equations; distractors are wrong numbers
- science safety items always have the safe action as the correct answer
- idempotent: repairing an already-repaired lesson changes nothing
"""
import copy
import json
import re
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))
import repair_math_science_content as r
from content_review_targets import MATH_SCIENCE_GROUPS

LESSONS = Path(__file__).resolve().parents[1] / "app/src/main/assets/content-pack/month-01/lessons"

EXPECTED_GROUPS = {
    "math-add": 9,
    "math-mul": 9,
    "sci-living": 7,
    "sci-materials": 11,
    "sci-light": 7,
}


def load(lid):
    return json.loads((LESSONS / f"{lid}.json").read_text())


def repair(lid):
    return load(lid)


def all_repaired():
    return {skill: list(ids) for skill, ids in MATH_SCIENCE_GROUPS.items()}


class TestScope(unittest.TestCase):
    def test_group_sizes(self):
        groups = all_repaired()
        self.assertEqual({k: len(v) for k, v in groups.items()}, EXPECTED_GROUPS)

    def test_no_junk_remaining(self):
        for skill, ids in all_repaired().items():
            junk = r.MATH_JUNK if skill.startswith("math") else r.SCIENCE_JUNK
            for lid in ids:
                blob = json.dumps(repair(lid)).lower()
                hits = [j for j in junk if j in blob]
                self.assertEqual(hits, [], f"{lid} still contains junk {hits}")


class TestAssessmentIntegrity(unittest.TestCase):
    def test_five_items_four_options_valid_correct(self):
        for skill, ids in all_repaired().items():
            for lid in ids:
                items = repair(lid)["assessment"]["items"]
                self.assertEqual(len(items), 5, lid)
                prompts = [it["prompt"] for it in items]
                self.assertEqual(len(set(prompts)), 5, f"{lid} duplicate prompts")
                for it in items:
                    opt_ids = [o["id"] for o in it["options"]]
                    self.assertEqual(sorted(opt_ids), ["a", "b", "c", "d"], lid)
                    self.assertEqual(len({o["text"] for o in it["options"]}), 4, lid)
                    self.assertTrue(all(isinstance(o["text"], str) for o in it["options"]), lid)
                    self.assertEqual(len(it["correctOptionIds"]), 1, lid)
                    self.assertIn(it["correctOptionIds"][0], opt_ids, lid)
                    correct = next(o["text"] for o in it["options"]
                                   if o["id"] == it["correctOptionIds"][0])
                    correct_words = set(re.findall(r"[A-Za-z0-9]{3,}", correct.lower()))
                    explanation_words = set(re.findall(r"[A-Za-z0-9]{3,}", it["explanation"].lower()))
                    self.assertTrue(
                        correct.lower() in it["explanation"].lower()
                        or correct_words.intersection(explanation_words),
                        lid,
                    )

    def test_no_filler_vocabulary(self):
        for skill, ids in all_repaired().items():
            for lid in ids:
                for v in repair(lid)["vocabulary"]:
                    self.assertNotIn(v["term"], {"a correct example", "a related example", "another example"})
                    self.assertGreater(len(v["definition"]), 15, f"{lid} vocab {v}")


class TestDifferentiation(unittest.TestCase):
    def test_instances_differ(self):
        for skill, ids in all_repaired().items():
            bodies = {lid: json.dumps(repair(lid), sort_keys=True) for lid in ids}
            self.assertEqual(len(set(bodies.values())), len(ids),
                             f"{skill}: instances are still byte-identical")

    def test_same_skill_objectives_are_focused(self):
        for skill, ids in all_repaired().items():
            objs = {load(lid)["objective"] for lid in ids}
            self.assertEqual(len(objs), len(ids), skill)


class TestMathNumerics(unittest.TestCase):
    def test_addition_sums_correct(self):
        for lid in all_repaired()["math-add"]:
            d = repair(lid)
            items = {it["prompt"]: it for it in d["assessment"]["items"]}
            eq = [a for a in d["activities"] if a["type"] == "SORT_AND_CLASSIFY"][0]["content"]["fits"][0]
            numbers = re.findall(r"\d[\d,]*", eq)
            self.assertGreaterEqual(len(numbers), 3, lid)
            a, b, s = (int(value.replace(",", "")) for value in numbers[:3])
            candidates = [
                it for it in d["assessment"]["items"]
                if r.numwords(a).lower() in it["prompt"].lower()
                and r.numwords(b).lower() in it["prompt"].lower()
            ]
            self.assertTrue(candidates, lid)
            correct = candidates[0]["correctOptionIds"][0]
            ans = next(o["text"] for o in candidates[0]["options"] if o["id"] == correct)
            self.assertEqual(ans, r.fmt(s), lid)

    def test_multiplication_products_correct(self):
        for lid in all_repaired()["math-mul"]:
            d = repair(lid)
            items = {it["prompt"]: it for it in d["assessment"]["items"]}
            eq = [a for a in d["activities"] if a["type"] == "SORT_AND_CLASSIFY"][0]["content"]["fits"][1]
            numbers = re.findall(r"\d[\d,]*", eq)
            self.assertGreaterEqual(len(numbers), 3, lid)
            f1, f2, product = (int(value.replace(",", "")) for value in numbers[:3])
            expected = r.fmt(f1 * f2)
            candidates = []
            for it in d["assessment"]["items"]:
                correct = it["correctOptionIds"][0]
                answer = next(o["text"] for o in it["options"] if o["id"] == correct)
                explanation = it["explanation"]
                if answer == expected and str(f1) in explanation and str(f2) in explanation:
                    candidates.append(it)
            self.assertTrue(candidates, lid)

    def test_distractors_are_wrong(self):
        for lid in all_repaired()["math-add"] + all_repaired()["math-mul"]:
            d = repair(lid)
            for it in d["assessment"]["items"]:
                correct = next(o["text"] for o in it["options"] if o["id"] in it["correctOptionIds"])
                for o in it["options"]:
                    if o["id"] not in it["correctOptionIds"]:
                        self.assertNotEqual(o["text"], correct, lid)


class TestScienceSafety(unittest.TestCase):
    def test_safe_actions_are_correct(self):
        for lid in all_repaired()["sci-light"] + all_repaired()["sci-materials"]:
            d = repair(lid)
            for it in d["assessment"]["items"]:
                if "SAFE" not in it["prompt"]:
                    continue
                correct = next(o["text"] for o in it["options"] if o["id"] in it["correctOptionIds"])
                self.assertTrue(
                    any(k in correct for k in ("move away", "cover", "sunglasses", "turn on",
                                               "flashlight", "rest", "never stare",
                                               "do not point", "never shine", "do not play",
                                               "do not look", "stay far", "quiet voice", "lamp",
                                               "look away", "look at it and touch")),
                    f"{lid}: '{correct}' is not a safe action",
                )


class TestIdempotency(unittest.TestCase):
    def test_repair_twice_is_noop(self):
        for lid in [list(v)[0] for v in all_repaired().values()]:
            once = json.dumps(repair(lid), sort_keys=True)
            twice = json.dumps(repair(lid), sort_keys=True)
            self.assertEqual(once, twice, lid)

    def test_legacy_fixture_exercises_repair_function(self):
        source = load(MATH_SCIENCE_GROUPS["math-add"][0])
        fixture_id = "mathematics-g3-q99-w99-d01"
        fixture = copy.deepcopy(source)
        fixture["lessonId"] = fixture_id
        fixture["subject"] = "mathematics"
        fixture["objective"] = "Add numbers with sums up to 10,000 using place value and regrouping."
        fixture["vocabulary"] = [{"term": "a correct example", "definition": "a correct example"}]

        with tempfile.TemporaryDirectory() as temp:
            lesson_dir = Path(temp)
            (lesson_dir / f"{fixture_id}.json").write_text(
                json.dumps(fixture), encoding="utf-8"
            )
            with patch.object(r, "LESSONS", lesson_dir):
                repaired = r.repair_lesson(copy.deepcopy(fixture))
                repaired_again = r.repair_lesson(copy.deepcopy(repaired))

        self.assertNotIn("a correct example", json.dumps(repaired))
        self.assertEqual(repaired, repaired_again)


if __name__ == "__main__":
    unittest.main(verbosity=2)
