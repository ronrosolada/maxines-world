#!/usr/bin/env python3
"""Unit tests for validate_arena_packs.py."""
import sys
import unittest
from pathlib import Path

# Ensure android root is in python path for tools imports
ANDROID_ROOT = Path(__file__).resolve().parents[1]
if str(ANDROID_ROOT) not in sys.path:
    sys.path.insert(0, str(ANDROID_ROOT))

from tools.validate_arena_packs import validate_arena


class TestValidateArenaPacks(unittest.TestCase):
    def setUp(self):
        self.packs_dir = ANDROID_ROOT / "app/src/main/assets/assessment-packs"

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

    def test_overall_answer_positions_are_not_a_first_option_tell(self):
        is_valid, errors, metrics = validate_arena(self.packs_dir)
        self.assertTrue(is_valid, errors)
        dist = metrics.get("overall_distribution", {})
        total = metrics.get("total_questions") or 0
        self.assertEqual(total, 180)
        for letter in ("a", "b", "c", "d"):
            share = dist.get(letter, 0) / total
            self.assertGreaterEqual(share, 0.20, f"{letter} share {share:.1%} below 20%")
            self.assertLessEqual(share, 0.30, f"{letter} share {share:.1%} above 30%")


if __name__ == "__main__":
    unittest.main()
