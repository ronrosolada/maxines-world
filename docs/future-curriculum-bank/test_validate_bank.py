import json
import unittest
from collections import Counter, defaultdict
from pathlib import Path

import validate_bank

R = Path(__file__).parent


class BankTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.roadmap = json.loads((R / "roadmap.json").read_text())
        cls.bank = json.loads((R / "assessment-bank.json").read_text())

    def test_bank(self):
        self.assertEqual([], validate_bank.validate())

    def test_counts(self):
        self.assertEqual(48, len(self.roadmap["units"]))
        self.assertEqual(192, len(self.bank["items"]))

    def test_keys_are_balanced_and_unit_sequences_are_diverse(self):
        keys = Counter(item["correctOptionId"] for item in self.bank["items"])
        self.assertEqual(Counter({key: 48 for key in "abcd"}), keys)
        by_unit = defaultdict(list)
        for item in self.bank["items"]:
            by_unit[item["unitId"]].append(item["correctOptionId"])
        sequences = ["".join(keys) for keys in by_unit.values()]
        self.assertGreaterEqual(len(set(sequences)), 8)
        self.assertNotIn("abcd", sequences)
        self.assertLessEqual(max(Counter(sequences).values()), 4)

    def test_fil_medium_records_have_no_prohibited_english(self):
        errors = validate_bank.validate()
        self.assertFalse([error for error in errors if "English bleed" in error])

    def test_explainers_are_worked_and_hints_are_timed(self):
        lessons = json.loads((R / "micro-lessons.json").read_text())["lessons"]
        for lesson in lessons:
            with self.subTest(unit=lesson["unitId"]):
                self.assertIn("Milo", lesson["script"])
                self.assertLessEqual(validate_bank.word_count(lesson["script"]), 105)
                self.assertGreaterEqual(validate_bank.word_count(lesson["script"]), 55)
                self.assertTrue(5 <= lesson["hint"]["targetSeconds"] <= 8)


if __name__ == "__main__":
    unittest.main()
