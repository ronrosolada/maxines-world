from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from assessment_duplicate_gate import find_duplicates


class AssessmentDuplicateGateTest(unittest.TestCase):
    def write_lesson(self, directory: Path, name: str, prompt: str) -> None:
        payload = {
            "assessment": {
                "items": [{
                    "itemId": f"{name}-q01",
                    "type": "MULTIPLE_CHOICE",
                    "prompt": prompt,
                    "options": [{"id": "a", "text": "yes"}, {"id": "b", "text": "no"}],
                    "correctOptionIds": ["a"],
                    "explanation": "Yes is correct.",
                }],
            }
        }
        (directory / f"{name}.json").write_text(json.dumps(payload), encoding="utf-8")

    def test_different_prompts_are_allowed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            self.write_lesson(directory, "one", "Which answer is true?")
            self.write_lesson(directory, "two", "Which answer is safe?")
            self.assertEqual({}, find_duplicates(directory))

    def test_identical_learner_payloads_are_blocked(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            self.write_lesson(directory, "one", "Which answer is true?")
            self.write_lesson(directory, "two", "Which answer is true?")
            duplicates = find_duplicates(directory)
            self.assertEqual(1, len(duplicates))
            self.assertEqual(2, len(next(iter(duplicates.values()))))


if __name__ == "__main__":
    unittest.main()
