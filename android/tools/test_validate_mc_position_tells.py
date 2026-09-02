#!/usr/bin/env python3
"""Unit tests for the MC first-option / slot-position content contract."""
from __future__ import annotations

import json
import tempfile
import unittest
from collections import Counter
from pathlib import Path

from validate_mc_position_tells import (
    MAX_SLOT_SHARE,
    MIN_SLOT_SHARE,
    audit_paths,
    audit_product_corpora,
    slot_share_errors,
)


def mc_item(correct: str, *distractors: str, key: str = "a") -> dict:
    letters = ["a", "b", "c", "d"]
    texts = [correct, *distractors]
    if key != "a":
        ordered = list(distractors)
        idx = letters.index(key)
        ordered.insert(idx, correct)
        texts = ordered
    options = [{"id": letter, "text": text} for letter, text in zip(letters, texts)]
    return {
        "itemId": "sample-q01",
        "sequence": 1,
        "type": "MULTIPLE_CHOICE",
        "prompt": "Sample prompt?",
        "options": options,
        "correctOptionIds": [key],
        "explanation": "Sample explanation.",
    }


def pack_with_keys(keys: list[str]) -> dict:
    items = []
    for i, key in enumerate(keys, start=1):
        item = mc_item("correct", "wrong-1", "wrong-2", "wrong-3", key=key)
        item["itemId"] = f"sample-q{i:02d}"
        item["sequence"] = i
        items.append(item)
    return {"id": "planted-slots", "items": items}


class SlotShareThresholdTest(unittest.TestCase):
    def test_documented_band_rejects_pre_fix_video_ratios(self) -> None:
        # Exact live counts measured on main before this change.
        n = 1185
        old = Counter({"a": 390, "b": 297, "c": 268, "d": 230})
        self.assertAlmostEqual(old["a"] / n, 0.329, places=3)
        errors = slot_share_errors("pre-fix video", old, n)
        self.assertTrue(any("slot a" in e and "32.9%" in e for e in errors), errors)
        self.assertTrue(any("slot d" in e and "19.4%" in e for e in errors), errors)

    def test_even_quarter_share_passes(self) -> None:
        n = 1185
        balanced = Counter({"a": 297, "b": 296, "c": 296, "d": 296})
        self.assertEqual([], slot_share_errors("rewritten video", balanced, n))

    def test_threshold_constants_are_the_documented_band(self) -> None:
        self.assertEqual(0.30, MAX_SLOT_SHARE)
        self.assertEqual(0.20, MIN_SLOT_SHARE)
        self.assertGreater(390 / 1185, MAX_SLOT_SHARE)
        self.assertLess(230 / 1185, MIN_SLOT_SHARE)


class PlantedFileAndLiveCorpusTest(unittest.TestCase):
    def test_audit_fails_on_a_planted_a_heavy_file(self) -> None:
        # 40 items, 14 keyed to A = 35% — above 30%, same class of leak as 32.9%.
        keys = ["a"] * 14 + ["b"] * 9 + ["c"] * 9 + ["d"] * 8
        planted = pack_with_keys(keys)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "planted.json"
            path.write_text(json.dumps(planted), encoding="utf-8")
            errors = audit_paths([path])
        self.assertTrue(errors, "planted A-heavy keys must fail the contract")
        self.assertTrue(any("slot a" in error for error in errors), errors)

    def test_audit_fails_when_letter_and_visual_slot_diverge(self) -> None:
        # Visual shuffle that leaves every correctOptionIds value as "a".
        item = mc_item("correct", "wrong-1", "wrong-2", "wrong-3", key="a")
        item["options"] = [
            {"id": "b", "text": "wrong-1"},
            {"id": "c", "text": "wrong-2"},
            {"id": "d", "text": "wrong-3"},
            {"id": "a", "text": "correct"},
        ]
        planted = {"id": "diverged", "items": [item] * 40}
        # Force 40 copies with unique ids so n is large enough for the share band.
        planted["items"] = []
        for i in range(40):
            copy = json.loads(json.dumps(item))
            copy["itemId"] = f"diverged-q{i:02d}"
            planted["items"].append(copy)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "diverged.json"
            path.write_text(json.dumps(planted), encoding="utf-8")
            errors = audit_paths([path])
        self.assertTrue(any("diverge" in error for error in errors), errors)

    def test_balanced_planted_file_passes(self) -> None:
        keys = ["a", "b", "c", "d"] * 10
        planted = pack_with_keys(keys)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "balanced.json"
            path.write_text(json.dumps(planted), encoding="utf-8")
            errors = audit_paths([path])
        self.assertEqual([], errors)

    def test_live_product_mc_slots_are_not_outliers(self) -> None:
        errors = audit_product_corpora()
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
