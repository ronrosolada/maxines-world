#!/usr/bin/env python3
"""Regression tests for content_similarity_gate.py (run: python3 -m unittest tools.test_content_similarity_gate -v)."""

import json
import tempfile
import unittest
from pathlib import Path

from tools.content_similarity_gate import (
    find_near_duplicates,
    jaccard,
    lesson_pedagogical_text,
    tokenize,
    cluster_pairs,
)


def lesson(title="T", objective="O", intro="", activities=None, assessment=None):
    return {
        "title": title,
        "objective": objective,
        "introduction": intro,
        "activities": activities or [],
        "assessment": assessment or {"items": []},
    }


class TokenizeTests(unittest.TestCase):
    def test_strips_punctuation_keeps_digits(self):
        # Digits are real content (math equations); punctuation is noise.
        tokens = tokenize("Ang aso ay tumatakbo! 3x? 245 + 123")
        self.assertEqual({"aso", "tumatakbo", "3x", "245", "123"}, tokens)

    def test_filters_stop_words(self):
        tokens = tokenize("the and ang ng sa ay of to is")
        self.assertEqual(set(), tokens)

    def test_handles_nfc_equivalence(self):
        a = tokenize("nag-iisip")
        b = tokenize("nag-iisip")
        self.assertEqual(a, b)


class JaccardTests(unittest.TestCase):
    def test_identical_sets(self):
        self.assertEqual(1.0, jaccard({"a", "b"}, {"a", "b"}))

    def test_disjoint_sets(self):
        self.assertEqual(0.0, jaccard({"a"}, {"b"}))

    def test_empty_sets(self):
        self.assertEqual(0.0, jaccard(set(), set()))


class PedagogicalTextTests(unittest.TestCase):
    def test_includes_objective_intro_activities_and_assessment(self):
        l = lesson(
            objective="Identify the simuno",
            intro="Ang simuno ang pinag-uusapan.",
            activities=[{"instruction": "Piliin ang tamang sagot", "content": "x"}],
            assessment={"items": [{"prompt": "Alin ang simuno?", "options": [{"text": "Si Ana"}]}]},
        )
        text = lesson_pedagogical_text(l)
        for needle in ("Identify the simuno", "pinag-uusapan", "Piliin", "Alin ang simuno?", "Si Ana"):
            self.assertIn(needle, text)

    def test_nested_content_fields_are_included(self):
        l = lesson(
            activities=[{
                "instruction": "Iuri ang mga ito",
                "content": {"fits": ["f1"], "doesNotFit": ["d1"], "steps": ["s1"]},
            }]
        )
        text = lesson_pedagogical_text(l)
        for needle in ("f1", "d1", "s1"):
            self.assertIn(needle, text)


class NearDuplicateTests(unittest.TestCase):
    def test_duplicate_bodies_are_flagged(self):
        body = {"objective": "Ang aso ay tumatakbo at ang pusa ay natutulog.",
                "intro": "Ang mga bata ay naglalaro sa parke.",
                "activities": [{"instruction": "Piliin ang tamang sagot para sa pangungusap"}],
                "assessment": {"items": [{"prompt": "Alin ang simuno sa pangungusap?"}]}}
        l1 = lesson(**body)
        l2 = lesson(**body)
        pairs = find_near_duplicates({"l1": l1, "l2": l2}, 0.70)
        self.assertEqual(1, len(pairs))
        self.assertGreaterEqual(pairs[0][2], 0.99)

    def test_distinct_lessons_are_not_flagged(self):
        l1 = lesson(objective="Identify the subject of a sentence in Filipino.",
                    intro="Ang simuno ang pinag-uusapan sa pangungusap.",
                    activities=[{"instruction": "Piliin ang simuno"}])
        l2 = lesson(objective="Solve two-digit subtraction with regrouping in Math.",
                    intro="Bawasan ang 52 ng 27 gamit ang pagpapangkat.",
                    activities=[{"instruction": "Kalkulahin ang tamang sagot"}])
        pairs = find_near_duplicates({"l1": l1, "l2": l2}, 0.70)
        self.assertEqual([], pairs)

    def test_threshold_respects_similarity(self):
        shared = {"activities": [{"instruction": "Piliin ang tamang sagot para sa pagsasanay"}]}
        l1 = lesson(**shared, objective="Gumuhit ng mapa ng ating komunidad",
                    intro="Ang mapa ay nagpapakita ng mga lugar")
        l2 = lesson(**shared, objective="Magkwento tungkol sa paboritong alaga",
                    intro="Ang alaga ay matalik na kaibigan")
        pairs = find_near_duplicates({"l1": l1, "l2": l2}, 0.99)
        self.assertEqual([], pairs)


class ClusterTests(unittest.TestCase):
    def test_connected_lessons_form_one_cluster(self):
        pairs = [("a", "b", 0.9), ("b", "c", 0.9)]
        clusters = cluster_pairs(pairs)
        self.assertEqual([["a", "b", "c"]], clusters)


class EndToEndTests(unittest.TestCase):
    def test_report_written_via_main(self):
        from tools.content_similarity_gate import main as gate_main
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            (d / "a.json").write_text(json_dumps_lesson("isa", "isa isa isa"))
            (d / "b.json").write_text(json_dumps_lesson("dalawa", "dalawa dalawa dalawa"))
            report = d / "report.json"
            rc = gate_main(["--pack", str(d), "--threshold", "0.70", "--json", str(report)])
            self.assertEqual(0, rc)
            data = json.loads(report.read_text(encoding="utf-8"))
            self.assertEqual(2, data["scanned"])
            self.assertEqual(0, data["near_duplicate_pairs"])


def json_dumps_lesson(lid, body_text):
    return json.dumps({
        "lessonId": lid, "objective": body_text, "introduction": body_text,
        "activities": [{"instruction": body_text}],
        "assessment": {"items": []},
    }, ensure_ascii=False)


if __name__ == "__main__":
    unittest.main()
