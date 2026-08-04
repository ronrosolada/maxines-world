import unittest

from convert_filipino_q1_slm import convert_lesson


class ConvertFilipinoQ1SlmTest(unittest.TestCase):
    def test_converts_source_activity_order_and_assessment_shape(self):
        source = {
            "lessonId": "filipino-g3-q1-w01-d01",
            "grade": 3,
            "quarter": 1,
            "week": 1,
            "day": 1,
            "subject": "FILIPINO",
            "title": "Pangngalan",
            "objective": "Nakagagamit ng pangngalan.",
            "estimatedMinutes": 12,
            "introduction": "Matuto tayo.",
            "vocabulary": [{"term": "pangngalan", "definition": "ngalan"}],
            "activities": [
                {"type": "ANIMATED_EXPLANATION", "instruction": "Panoorin.", "content": "Paliwanag.", "prompt": "Panoorin."},
                {"type": "MULTIPLE_CHOICE", "instruction": "Piliin.", "content": {"options": ["A", "B"], "correctIndex": 1}, "prompt": "Ano?"},
                {"type": "SORT_AND_CLASSIFY", "instruction": "I-grupo.", "content": {"fits": ["A"], "doesNotFit": ["B"]}, "prompt": "Ayusin."},
                {"type": "HOTSPOT_IMAGE", "instruction": "Hanapin.", "content": {"targets": [{"label": "A"}]}, "prompt": "Pindutin."},
                {"type": "MATCHING_PAIRS", "instruction": "Itugma.", "content": {"pairs": [{"left": "A", "right": "B"}]}, "prompt": "Pagdugtungin."},
            ],
            "assessment": {"passingCorrectCount": 1, "items": [{"question": "Ano?", "choices": [{"text": "A", "correct": True}, {"text": "B", "correct": False}]}]},
        }

        lesson = convert_lesson(source)

        self.assertEqual(
            [a["type"] for a in lesson["activities"]],
            ["ANIMATED_EXPLANATION", "HOTSPOT_IMAGE", "SORT_AND_CLASSIFY", "MULTIPLE_CHOICE", "MATCHING_PAIRS", "SEQUENCE_BUILDER"],
        )
        self.assertEqual(lesson["activities"][1]["content"]["examples"], ["A"])
        self.assertEqual(
            lesson["activities"][-1]["content"]["steps"],
            [
                "Pumili ng tao, lugar, o bagay sa paligid.",
                "Tukuyin ang pangngalan sa napiling halimbawa.",
                "Gamitin ang pangngalan sa isang pangungusap.",
                "Basahin ang pangungusap at suriin ang gamit ng pangngalan.",
            ],
        )
        self.assertEqual(lesson["assessment"]["items"][0]["correctOptionIds"], ["a"])
        self.assertEqual(lesson["educatorValidated"], False)
        self.assertEqual(lesson["releaseStatus"], "REQUIRES_EDUCATOR_REVIEW")


if __name__ == "__main__":
    unittest.main()
