#!/usr/bin/env python3
"""Unit tests for the MC length/detail content contract."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from validate_mc_length_tells import audit_paths, length_tell_reasons


def mc_item(correct: str, *distractors: str, key: str = "a") -> dict:
    letters = ["a", "b", "c", "d"]
    texts = [correct, *distractors]
    if key != "a":
        # Place the keyed text at the requested letter.
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


class LengthTellHeuristicTest(unittest.TestCase):
    def test_planted_longest_complete_sentence_fails(self) -> None:
        item = mc_item(
            "Mia found three shells on the beach.",
            "Mia had fun.",
            "Mia found something.",
            "Mia went somewhere.",
        )
        reasons = length_tell_reasons(item)
        self.assertTrue(any("length_gap" in r or "word_gap" in r or "median_gap" in r for r in reasons), reasons)

    def test_planted_extra_clause_fails(self) -> None:
        item = mc_item(
            "The rope stays in place because the forces are balanced",
            "The rope moves quickly to the left",
            "The rope moves quickly to the right",
            "The rope disappears",
            key="b",
        )
        reasons = length_tell_reasons(item)
        self.assertTrue(reasons, reasons)

    def test_planted_unique_bilingual_gloss_fails(self) -> None:
        item = mc_item(
            "Balat (Skin)",
            "Dila",
            "Tenga",
            "Ilong",
            key="d",
        )
        reasons = length_tell_reasons(item)
        self.assertTrue(any("unique_gloss" in r for r in reasons), reasons)

    def test_balanced_specific_options_pass(self) -> None:
        item = mc_item(
            "Ibalik ang barya sa kaklase",
            "Itago ang barya sa bulsa",
            "Iwanan ang barya sa sahig",
            "Gamitin ang barya sa tindahan",
        )
        self.assertEqual([], length_tell_reasons(item))

    def test_slightly_longer_vocabulary_key_passes(self) -> None:
        # "hardworking" is longer than "farmer" but not a completeness leak.
        item = mc_item("hardworking", "farmer", "watered", "morning")
        self.assertEqual([], length_tell_reasons(item))

    def test_numeric_and_currency_options_pass(self) -> None:
        item = mc_item("₱240.00", "₱110.00", "₱260.00", "₱275.00", key="b")
        self.assertEqual([], length_tell_reasons(item))
        item = mc_item("144", "124", "140", "164", key="d")
        self.assertEqual([], length_tell_reasons(item))

    def test_equally_specific_sentences_pass_even_if_one_is_a_bit_longer(self) -> None:
        item = mc_item(
            "I see a small boat on blue water.",
            "I think the painter felt very tired.",
            "The boat will travel far tomorrow.",
            "Someone probably owns this water.",
        )
        self.assertEqual([], length_tell_reasons(item))


class LiveCorpusAndPlantedFileTest(unittest.TestCase):
    def test_audit_fails_on_a_planted_leak_file(self) -> None:
        planted = {
            "id": "planted-leak",
            "items": [
                mc_item(
                    "Honeybees play an essential role in growing plants and food",
                    "Honeybees have stripes",
                    "Bears like to eat honey",
                    "Flowers have pretty colors",
                    key="b",
                )
            ],
        }
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "planted.json"
            path.write_text(json.dumps(planted), encoding="utf-8")
            errors = audit_paths([path])
        self.assertTrue(errors, "planted longest/most-detailed key must fail the contract")
        self.assertTrue(
            any("sample-q01" in error or "planted-leak" in error for error in errors),
            errors,
        )

    def test_live_product_mc_items_have_no_length_tells(self) -> None:
        android = Path(__file__).resolve().parents[1]
        repo = android.parent
        errors = audit_paths(
            [
                android / "app/src/main/assets/content-pack/media-assessments.json",
                repo / "server/content/catalog.json",
                android / "app/src/main/assets/assessment-packs",
                android / "core-content/src/main/assets/assessment-packs",
            ]
        )
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
