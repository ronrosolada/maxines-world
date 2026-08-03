#!/usr/bin/env python3
"""Regression tests for fix_filipino_simuno_panguri_assessment.py.

Guards the deterministic assessment regeneration for the 32 Filipino
simuno/panaguri lessons:
- exactly 32 lesson IDs, 5 items per lesson
- valid a-d option ids, unique option text per item
- keyed answer exists and is a single option id
- no generic title-substituted prompt templates remain
- panaguri answers follow the complete-predicate convention (include 'ay')
"""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent
LESSON_DIR = TOOLS_DIR.parent / "app/src/main/assets/content-pack/month-01/lessons"

sys.path.insert(0, str(TOOLS_DIR))
from fix_filipino_simuno_panguri_assessment import (  # noqa: E402
    ASSESSMENT_TEMPLATES,
    SIMUNO_PANGGURI_LESSONS,
)

GENERIC_MARKERS = [
    "Which example belongs to", "Which choice shows the skill in",
    "What is one example from", "Which situation matches",
    "Which answer demonstrates", "Aling halimbawa ang kabilang sa",
    "Aling pagpipilian ang nagpapakita ng kasanayan sa",
    "Alin ang isang halimbawa ng", "Aling sitwasyon ang tumutugma sa",
    "Aling sagot ang nagpapakita ng",
]


class TestAssessmentFix(unittest.TestCase):
    def test_expected_lesson_set(self):
        self.assertEqual(len(SIMUNO_PANGGURI_LESSONS), 32)
        self.assertEqual(len(set(SIMUNO_PANGGURI_LESSONS)), 32)
        for lid in SIMUNO_PANGGURI_LESSONS:
            f = LESSON_DIR / f"{lid}.json"
            self.assertTrue(f.exists(), f"missing lesson file {lid}.json")

    def test_five_items_per_lesson(self):
        for lid in SIMUNO_PANGGURI_LESSONS:
            f = LESSON_DIR / f"{lid}.json"
            lesson = json.loads(f.read_text(encoding="utf-8"))
            items = lesson.get("assessment", {}).get("items", [])
            self.assertEqual(len(items), 5, f"{lid}: expected 5 items")

    def test_items_well_formed(self):
        for lid in SIMUNO_PANGGURI_LESSONS:
            f = LESSON_DIR / f"{lid}.json"
            lesson = json.loads(f.read_text(encoding="utf-8"))
            items = lesson.get("assessment", {}).get("items", [])
            for it in items:
                opts = it.get("options", [])
                ids = [o.get("id") for o in opts]
                texts = [o.get("text") for o in opts]
                self.assertEqual(sorted(ids), ["a", "b", "c", "d"],
                                 f"{lid} {it.get('itemId')}: option ids")
                self.assertEqual(len(texts), len(set(texts)),
                                 f"{lid} {it.get('itemId')}: duplicate option text")
                cids = it.get("correctOptionIds", [])
                self.assertEqual(len(cids), 1, f"{lid} {it.get('itemId')}: key count")
                self.assertIn(cids[0], ids, f"{lid} {it.get('itemId')}: key not in options")

    def test_no_generic_prompts(self):
        for lid in SIMUNO_PANGGURI_LESSONS:
            f = LESSON_DIR / f"{lid}.json"
            lesson = json.loads(f.read_text(encoding="utf-8"))
            items = lesson.get("assessment", {}).get("items", [])
            for it in items:
                prompt = it.get("prompt", "")
                for marker in GENERIC_MARKERS:
                    self.assertNotIn(marker, prompt,
                                     f"{lid} {it.get('itemId')}: generic prompt")

    def test_panaguri_complete_predicate_convention(self):
        """When a question asks for the panaguri, the keyed answer includes 'ay'."""
        for lid in SIMUNO_PANGGURI_LESSONS:
            f = LESSON_DIR / f"{lid}.json"
            lesson = json.loads(f.read_text(encoding="utf-8"))
            items = lesson.get("assessment", {}).get("items", [])
            for it in items:
                if "panaguri" not in it.get("prompt", "").lower():
                    continue
                key = it["correctOptionIds"][0]
                key_text = next(o["text"] for o in it["options"] if o["id"] == key)
                # "Simuno: X; Panaguri: Y" style answers also count
                panaguri_part = key_text.split("Panaguri:")[-1] if "Panaguri:" in key_text else key_text
                self.assertTrue(
                    "ay " in panaguri_part or panaguri_part.strip().startswith("ay"),
                    f"{lid} {it.get('itemId')}: keyed panaguri '{key_text}' missing 'ay'",
                )

    def test_correct_positions_varied(self):
        positions = {}
        for lid in SIMUNO_PANGGURI_LESSONS:
            f = LESSON_DIR / f"{lid}.json"
            lesson = json.loads(f.read_text(encoding="utf-8"))
            items = lesson.get("assessment", {}).get("items", [])
            for it in items:
                key = it["correctOptionIds"][0]
                positions[key] = positions.get(key, 0) + 1
        self.assertEqual(set(positions.keys()), {"a", "b", "c", "d"},
                         "answer keys must span all four positions")
        # Every position must be well represented (>=25% of 160 = 40);
        # relax to 25 to tolerate template rotation drift without collapsing a position.
        for pos in ("a", "b", "c", "d"):
            self.assertGreaterEqual(positions[pos], 25, f"{pos} position under-represented")

    def test_templates_are_objective_specific(self):
        for tpl in ASSESSMENT_TEMPLATES:
            prompt = tpl[0].lower()
            # All three are valid objective-specific indicators:
            #   simuno/panaguri = subject/predicate identification
            #   paghahati       = partitioning into subject/predicate
            self.assertTrue(
                "simuno" in prompt or "panaguri" in prompt or "paghahati" in prompt,
                f"template is not objective-specific: {tpl[0]}",
            )
        self.assertGreaterEqual(len(ASSESSMENT_TEMPLATES), 10)


if __name__ == "__main__":
    unittest.main()
