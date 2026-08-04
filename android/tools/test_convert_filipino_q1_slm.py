import unittest

from convert_filipino_q1_slm import _normalize_source, convert_lesson


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
            "storyIntro": "May kuwento si Milo.",
            "scene": {"character": "Milo", "setting": "Silid-aklatan"},
            "accessibility": {"narrationAvailable": True, "captionsAvailable": True},
            "vocabulary": [{"term": "pangngalan", "definition": "ngalan"}],
            "activities": [
                {"type": "ANIMATED_EXPLANATION", "instruction": "Panoorin.", "content": "Paliwanag.", "prompt": "Panoorin."},
                {"type": "MULTIPLE_CHOICE", "instruction": "Piliin.", "content": {"options": ["A", "B"], "correctIndex": 1}, "prompt": "Ano?"},
                {"type": "SORT_AND_CLASSIFY", "instruction": "I-grupo.", "content": {"fits": ["A"], "doesNotFit": ["B"]}, "prompt": "Ayusin."},
                {"type": "HOTSPOT_IMAGE", "instruction": "Hanapin.", "content": {"targets": [{"label": "A"}]}, "prompt": "Pindutin.", "completionRule": {"type": "ALL_TARGETS_VISITED"}},
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
        self.assertEqual(lesson["activities"][1]["completionRule"]["targetCount"], 1)
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
        self.assertEqual(lesson["assessment"]["items"][0]["type"], "MULTIPLE_CHOICE")
        self.assertEqual(lesson["educatorValidated"], False)
        self.assertEqual(lesson["releaseStatus"], "REQUIRES_EDUCATOR_REVIEW")
        self.assertEqual(lesson["storyIntro"], source["storyIntro"])
        self.assertEqual(lesson["scene"], source["scene"])
        self.assertEqual(lesson["accessibility"], source["accessibility"])

    def test_applies_reviewed_content_corrections_before_conversion(self):
        source = {
            "lessonId": "filipino-g3-q1-w07-d01",
            "objective": "Lumang layunin.",
            "vocabulary": [{"term": term, "definition": term} for term in [
                "Malaking letra", "Maliit na letra", "Tuldok", "Tandang pananong", "Tandang padamdam",
            ]],
            "activities": [
                {
                    "type": "SEQUENCE_BUILDER",
                    "content": {"steps": ["Tuldok", "Tandang pananong", "Paalala"]},
                },
                {
                    "type": "MATCHING_PAIRS",
                    "content": {"pairs": [{"left": "Bitbit mo ang tsokolate", "right": "Ito — Bitbit mo ang tsokolate"}]},
                },
            ],
        }
        normalized = _normalize_source(source)
        self.assertEqual([entry["term"] for entry in normalized["vocabulary"]], ["Malaking letra", "Maliit na letra", "Tuldok"])
        self.assertEqual(
            normalized["activities"][0]["content"]["steps"],
            [
                "Basahin ang pangungusap.",
                "Tukuyin kung saan kailangan ang malaking letra at bantas.",
                "Piliin ang angkop na malaking letra at bantas.",
                "Isulat muli at suriin ang buong pangungusap.",
            ],
        )
        self.assertEqual(normalized["activities"][1]["content"]["pairs"][0]["right"], "Ito")

        story_source = {
            "lessonId": "filipino-g3-q1-w06-d02",
            "objective": "Lumang layunin.",
            "assessment": {"items": [{"question": "Q", "choices": []} for _ in range(5)]},
        }
        story_normalized = _normalize_source(story_source)
        self.assertIn("mahahalagang pangyayari", story_normalized["objective"])
        self.assertIn("matalino", story_normalized["assessment"]["items"][4]["question"])

        dictionary_source = {
            "lessonId": "filipino-g3-q1-w04-d02",
            "vocabulary": [{"term": "Paaplabeto", "definition": "..."}],
            "assessment": {
                "items": [{
                    "choices": [{"text": "Sa titik B, pagitan ng 'bintana' at 'buhay'", "correct": True}],
                }],
            },
        }
        dictionary_normalized = _normalize_source(dictionary_source)
        self.assertEqual(dictionary_normalized["vocabulary"][0]["term"], "Paalpabeto")
        self.assertIn("pagkatapos ng 'buhay'", dictionary_normalized["assessment"]["items"][0]["choices"][0]["text"])

    def test_derives_hotspot_from_matching_pairs_not_sort_fits(self):
        source = {
            "lessonId": "filipino-g3-q1-w01-d02",
            "grade": 3,
            "quarter": 1,
            "week": 1,
            "day": 2,
            "subject": "FILIPINO",
            "title": "Panghalip Panao",
            "objective": "Nakagagamit ng panghalip panao.",
            "activities": [
                {"type": "ANIMATED_EXPLANATION", "instruction": "Panoorin.", "content": "Paliwanag."},
                {
                    "type": "SORT_AND_CLASSIFY",
                    "instruction": "I-grupo.",
                    "content": {"fits": ["Ako ang nagsasalita"], "doesNotFit": ["Siya ang pinag-uusapan"]},
                },
                {
                    "type": "MULTIPLE_CHOICE",
                    "instruction": "Piliin.",
                    "content": {"options": ["Ako", "Siya"], "correctIndex": 0},
                },
                {
                    "type": "MATCHING_PAIRS",
                    "instruction": "Itugma.",
                    "content": {"pairs": [{"left": "Kinakausap ang kaklase", "right": "Ikaw"}]},
                },
            ],
            "assessment": {
                "passingCorrectCount": 1,
                "items": [
                    {
                        "question": "Kailan ginagamit ang ako?",
                        "choices": [{"text": "Sa sarili", "correct": True}, {"text": "Sa iba", "correct": False}],
                    }
                ],
            },
        }

        lesson = convert_lesson(source)
        hotspot = lesson["activities"][1]

        self.assertEqual(hotspot["content"]["examples"], ["Kinakausap ang kaklase"])
        self.assertNotEqual(hotspot["content"]["examples"], source["activities"][1]["content"]["fits"])
        self.assertEqual(hotspot["completionRule"]["targetCount"], 1)
        self.assertIn("Panghalip Panao", hotspot["instruction"])
        self.assertIn("Panghalip Panao", hotspot["prompt"])

        sequence_fallback = {
            **source,
            "lessonId": "filipino-g3-q1-w01-d03",
            "activities": [
                source["activities"][0],
                source["activities"][1],
                source["activities"][2],
                {"type": "SEQUENCE_BUILDER", "content": {"steps": ["Unang halimbawa", "Ikalawang halimbawa"]}},
            ],
        }
        fallback_lesson = convert_lesson(sequence_fallback)
        fallback_hotspot = fallback_lesson["activities"][1]
        self.assertEqual(fallback_hotspot["content"]["examples"], ["Unang halimbawa", "Ikalawang halimbawa"])
        self.assertNotEqual(fallback_hotspot["content"]["examples"], source["activities"][1]["content"]["fits"])


if __name__ == "__main__":
    unittest.main()
