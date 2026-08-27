import unittest

from tools.repair_generic_activity_prompts import repair_lesson


class GenericPromptRepairTests(unittest.TestCase):
    def test_rewrites_stock_activity_copy_with_objective(self):
        lesson = {
            "lessonId": "test-g3-q1-w01-d01",
            "language": "en-PH",
            "objective": "Compare numbers using greater than and less than.",
            "activities": [
                {
                    "type": "MULTIPLE_CHOICE",
                    "instruction": "Choose the best answer.",
                    "prompt": "Choose the best answer.",
                    "narration": "Numbers can be compared.",
                },
                {
                    "type": "MATCHING_PAIRS",
                    "instruction": "Match the ideas that belong together.",
                    "prompt": "Match the ideas that belong together.",
                },
            ],
            "assessment": {"items": []},
        }

        changed = repair_lesson(lesson)

        self.assertTrue(changed)
        self.assertIn("Compare numbers using greater than and less than", lesson["activities"][0]["prompt"])
        self.assertEqual(lesson["activities"][0]["prompt"], lesson["activities"][0]["instruction"])
        self.assertIn("Compare numbers using greater than and less than", lesson["activities"][1]["prompt"])

    def test_rewrites_filipino_assessment_template_but_preserves_key(self):
        lesson = {
            "lessonId": "test-g3-q1-w01-d01",
            "language": "fil-PH",
            "objective": "Natutukoy ang simuno at panaguri sa pangungusap.",
            "activities": [],
            "assessment": {
                "items": [{
                    "prompt": "Aling halimbawa ang kabilang sa Aralin?",
                    "options": [{"id": "a", "text": "simuno"}, {"id": "b", "text": "panaguri"}],
                    "correctOptionIds": ["a"],
                }],
            },
        }

        changed = repair_lesson(lesson)

        self.assertTrue(changed)
        self.assertIn("Natutukoy ang simuno at panaguri", lesson["assessment"]["items"][0]["prompt"])
        self.assertEqual(["a"], lesson["assessment"]["items"][0]["correctOptionIds"])

    def test_specific_copy_is_unchanged_and_second_run_is_idempotent(self):
        lesson = {
            "lessonId": "test-g3-q1-w01-d01",
            "language": "en-PH",
            "objective": "Describe a character using story evidence.",
            "activities": [{
                "type": "MULTIPLE_CHOICE",
                "instruction": "Which action shows courage?",
                "prompt": "Which action shows courage?",
            }],
            "assessment": {"items": []},
        }

        self.assertFalse(repair_lesson(lesson))
        snapshot = repr(lesson)
        self.assertFalse(repair_lesson(lesson))
        self.assertEqual(snapshot, repr(lesson))


if __name__ == "__main__":
    unittest.main()
