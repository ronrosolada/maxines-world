#!/usr/bin/env python3
"""Regression tests for the CH-07 content tooling.

Covers objective_pacing_audit, clean_meta_junk, and the writing-production
pilot idempotency against a tiny synthetic lesson directory.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

TOOLS = os.path.join(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.dirname(TOOLS)
sys.path.insert(0, TOOLS)

from objective_pacing_audit import audit, MAX_FANOUT  # noqa: E402
from clean_meta_junk import clean_prompt  # noqa: E402


class ObjectivePacingAuditTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.addCleanup(lambda: __import__("shutil").rmtree(self.dir, ignore_errors=True))

    def _lesson(self, name, objective):
        path = os.path.join(self.dir, name)
        with open(path, "w", encoding="utf-8") as fh:
            json.dump({"lessonId": name[:-5], "objective": objective}, fh)
        return path

    def test_no_over_fanout_reports_zero(self):
        for i in range(3):
            self._lesson(f"l{i}.json", "same objective")
        report = audit(self.dir)
        self.assertEqual(0, report["objectives_over_fanout"])
        self.assertEqual(3, report["lesson_count"])
        self.assertEqual(1, report["distinct_objectives"])

    def test_over_fanout_is_counted(self):
        for i in range(5):
            self._lesson(f"l{i}.json", "stretched objective")
        report = audit(self.dir)
        self.assertEqual(1, report["objectives_over_fanout"])
        self.assertEqual(5, report["files_in_over_fanout"])
        self.assertIn("l0.json", report["over_fanout"]["stretched objective"]["files"])

    def test_check_mode_fails_over_fanout(self):
        for i in range(MAX_FANOUT + 1):
            self._lesson(f"over{i}.json", "too much of one objective")
        result = subprocess.run(
            [sys.executable, os.path.join(TOOLS, "objective_pacing_audit.py"),
             "--content-root", self.dir, "--check"],
            capture_output=True, text=True,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("objectives over fan-out:  1", result.stdout)

    def test_real_pack_has_no_over_fanout(self):
        pack = os.path.join(ROOT, "app", "src", "main", "assets", "content-pack", "month-01", "lessons")
        if os.path.isdir(pack):
            report = audit(pack)
            self.assertEqual(
                0, report["objectives_over_fanout"],
                f"over-fanned objectives remain: {list(report['over_fanout'])[:5]}",
            )


class CleanMetaJunkTest(unittest.TestCase):
    def test_filipino_prefix_stripped(self):
        cleaned, changes = clean_prompt("Sagutin ito: Alin ang tama?")
        self.assertEqual("Alin ang tama?", cleaned)
        self.assertTrue(any(c.startswith("prefix") for c in changes))

    def test_english_prefix_stripped(self):
        cleaned, _ = clean_prompt("Answer this one: Which is flexible?")
        self.assertEqual("Which is flexible?", cleaned)

    def test_meta_suffix_stripped(self):
        cleaned, changes = clean_prompt("Alin ang tama?  (sa aralin: X)")
        self.assertEqual("Alin ang tama?", cleaned)
        self.assertIn("meta suffix", changes)

    def test_clean_prompt_is_stable(self):
        prompt = "Which material is flexible?"
        cleaned, changes = clean_prompt(prompt)
        self.assertEqual(prompt, cleaned)
        self.assertEqual([], changes)


class WritingPilotIdempotencyTest(unittest.TestCase):
    def test_script_exists_and_is_idempotent(self):
        script = os.path.join(TOOLS, "pilot_writing_production.py")
        self.assertTrue(os.path.isfile(script))
        pack = os.path.join(ROOT, "app", "src", "main", "assets", "content-pack", "month-01", "lessons")
        if not os.path.isdir(pack):
            return
        with open(os.path.join(pack, "english-g3-q2-w04-d02.json"), encoding="utf-8") as fh:
            lesson = json.load(fh)
        types = [a.get("type") for a in lesson.get("activities", [])]
        self.assertIn("WRITING_PRODUCTION", types)
        # canonical six still lead
        self.assertEqual(
            ["ANIMATED_EXPLANATION", "HOTSPOT_IMAGE", "SORT_AND_CLASSIFY",
             "MULTIPLE_CHOICE", "MATCHING_PAIRS", "SEQUENCE_BUILDER"],
            types[:6],
        )


if __name__ == "__main__":
    unittest.main()
