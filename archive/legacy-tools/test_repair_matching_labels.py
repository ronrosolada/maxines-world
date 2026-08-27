import unittest

from tools.repair_matching_labels import repair_lesson


class MatchingLabelRepairTests(unittest.TestCase):
    def test_derives_unique_right_labels_from_explicit_left_categories(self):
        lesson = {
            "lessonId": "test",
            "activities": [{
                "type": "MATCHING_PAIRS",
                "content": {"pairs": [
                    {"left": "stone—solid", "right": "shows the skill"},
                    {"left": "water—liquid", "right": "shows the skill"},
                    {"left": "air—gas", "right": "shows the skill"},
                ]},
            }],
        }

        self.assertTrue(repair_lesson(lesson))
        self.assertEqual(["solid", "liquid", "gas"], [p["right"] for p in lesson["activities"][0]["content"]["pairs"]])
        self.assertFalse(repair_lesson(lesson))

    def test_leaves_ambiguous_pairs_untouched(self):
        lesson = {
            "lessonId": "test",
            "activities": [{
                "type": "MATCHING_PAIRS",
                "content": {"pairs": [
                    {"left": "The child holds a kite.", "right": "shows the skill"},
                    {"left": "Two birds sit in a tree.", "right": "shows the skill"},
                    {"left": "A dog runs beside a bench.", "right": "shows the skill"},
                ]},
            }],
        }

        self.assertFalse(repair_lesson(lesson))
        self.assertEqual("shows the skill", lesson["activities"][0]["content"]["pairs"][0]["right"])


if __name__ == "__main__":
    unittest.main()
