#!/usr/bin/env python3
"""Unit tests for validate_arena_packs.py."""
import unittest
from pathlib import Path
from tools.validate_arena_packs import validate_arena


class TestValidateArenaPacks(unittest.TestCase):
    def setUp(self):
        self.packs_dir = Path(__file__).resolve().parents[1] / "app/src/main/assets/assessment-packs"

    def test_live_assessment_packs_are_valid(self):
        is_valid, errors, metrics = validate_arena(self.packs_dir)
        self.assertTrue(is_valid, f"Validation failed with errors: {errors}")
        self.assertEqual(len(errors), 0)
        self.assertEqual(metrics.get("total_packs"), 18)
        self.assertEqual(metrics.get("total_questions"), 180)
        self.assertIn("catalog.json", [p.name for p in self.packs_dir.glob("*.json")])

    def test_all_18_packs_have_10_questions(self):
        _, _, metrics = validate_arena(self.packs_dir)
        pack_stats = metrics.get("pack_stats", {})
        self.assertEqual(len(pack_stats), 18)
        for pid, stats in pack_stats.items():
            self.assertEqual(stats["total_items"], 10, f"Pack {pid} does not have 10 items")


if __name__ == "__main__":
    unittest.main()
