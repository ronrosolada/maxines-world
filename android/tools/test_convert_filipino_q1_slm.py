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

    def test_applies_educator_content_repairs(self):
        spelling = {
            "lessonId": "filipino-g3-q1-w04-d01",
            "storyIntro": "old story",
            "vocabulary": [
                {"term": "Baybay", "definition": "old"},
                {"term": "Tsubibo", "definition": "old"},
                {"term": "Batobalani", "definition": "old"},
            ],
            "activities": [
                {"type": "ANIMATED_EXPLANATION", "content": "old explanation"},
                {"type": "MATCHING_PAIRS", "content": {"pairs": []}},
            ],
            "assessment": {"items": [{"choices": []}, {"choices": [{"text": "old", "correct": True}]}]},
        }
        spelling_normalized = _normalize_source(spelling)
        self.assertNotIn("Maya-maya, dumating", spelling_normalized["storyIntro"])
        self.assertIn("katangiang magnetiko", spelling_normalized["vocabulary"][2]["definition"])
        spelling_choices = spelling_normalized["assessment"]["items"][1]["choices"]
        self.assertEqual(len({choice["text"] for choice in spelling_choices}), 4)
        self.assertIn("Sasakyang panlibangan", spelling_normalized["activities"][1]["content"]["pairs"][0]["right"])

        dictionary = {
            "lessonId": "filipino-g3-q1-w04-d02",
            "activities": [
                {"type": "MULTIPLE_CHOICE", "content": {"options": []}},
                {"type": "SORT_AND_CLASSIFY", "content": {"fits": ["old"], "doesNotFit": []}},
            ],
            "assessment": {"items": [{"choices": []} for _ in range(4)]},
        }
        dictionary_normalized = _normalize_source(dictionary)
        self.assertIn("salitang 'mata'", dictionary_normalized["activities"][0]["instruction"])
        self.assertIn("TAMANG SAGOT", dictionary_normalized["activities"][1]["instruction"])
        self.assertIn("nauuna ang h sa l", dictionary_normalized["assessment"]["items"][3]["choices"][0]["text"])

        pronoun = {
            "lessonId": "filipino-g3-q1-w05-d01",
            "storyIntro": "Ako gusto kong maging dentista.",
            "activities": [
                {"type": "MULTIPLE_CHOICE", "content": {"options": []}},
                {"type": "MATCHING_PAIRS", "content": {"pairs": [{"left": "old", "right": "Ako"}]}},
            ],
            "assessment": {"items": []},
        }
        pronoun_normalized = _normalize_source(pronoun)
        self.assertIn("Ako naman", pronoun_normalized["storyIntro"])
        self.assertIn("Si Karla", pronoun_normalized["activities"][0]["instruction"])
        self.assertEqual(pronoun_normalized["activities"][1]["content"]["pairs"][0]["left"], "Nagsasalita ka tungkol sa iyong sarili")

        animal_story = {
            "lessonId": "filipino-g3-q1-w06-d01",
            "activities": [
                {"type": "ANIMATED_EXPLANATION", "content": "old"},
                {"type": "SORT_AND_CLASSIFY", "content": {"fits": [], "doesNotFit": []}},
                {"type": "SEQUENCE_BUILDER", "content": {"steps": []}},
            ],
            "assessment": {"items": []},
        }
        animal_normalized = _normalize_source(animal_story)
        animal_text = animal_normalized["activities"][0]["content"]
        self.assertIn("Humingi sila ng tulong sa nanay ni Ana", animal_text)
        self.assertNotIn("dinala sa bahay ni Ana", animal_text)
        self.assertIn("beterinaryo", animal_text)
        self.assertTrue(any("Paghingi ng tulong para sa ibon" in item for item in animal_normalized["activities"][1]["content"]["fits"]))

        retell = {
            "lessonId": "filipino-g3-q1-w06-d02",
            "activities": [
                {"type": "ANIMATED_EXPLANATION", "content": "matalino, masayahin, at maganda"},
                {"type": "MULTIPLE_CHOICE", "content": {"options": []}},
                {"type": "MATCHING_PAIRS", "content": {"pairs": []}},
            ],
            "assessment": {"items": [{"question": "old", "choices": []} for _ in range(5)]},
        }
        retell_normalized = _normalize_source(retell)
        self.assertIn("masipag", retell_normalized["activities"][0]["content"])
        self.assertNotIn("Ano ang pamagat", retell_normalized["assessment"]["items"][0]["question"])
        self.assertEqual(len(retell_normalized["activities"][2]["content"]["pairs"]), 5)

        demonstratives = {
            "lessonId": "filipino-g3-q1-w07-d02",
            "vocabulary": [
                {"term": "Ito", "definition": "old"},
                {"term": "Iyan", "definition": "old"},
                {"term": "Iyon", "definition": "old"},
            ],
            "activities": [{"type": "ANIMATED_EXPLANATION", "content": "old"}],
            "assessment": {"items": []},
        }
        demonstratives_normalized = _normalize_source(demonstratives)
        self.assertIn("malapit sa kausap", demonstratives_normalized["vocabulary"][1]["definition"])
        self.assertIn("malayo sa nagsasalita at sa kausap", demonstratives_normalized["vocabulary"][2]["definition"])
        self.assertIn("malapit sa kausap", demonstratives_normalized["activities"][0]["content"])

        story_structure = {
            "lessonId": "filipino-g3-q1-w08-d01",
            "storyIntro": "old story",
            "activities": [{"type": "ANIMATED_EXPLANATION", "content": "old story with pulubi"}],
            "assessment": {"items": []},
        }
        story_structure_normalized = _normalize_source(story_structure)
        safe_story = story_structure_normalized["activities"][0]["content"]
        self.assertNotIn("pulubi", safe_story)
        self.assertIn("tulong sa guro", safe_story)
        self.assertIn("ligtas na lugar", safe_story)

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
