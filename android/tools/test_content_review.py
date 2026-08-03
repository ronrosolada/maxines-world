#!/usr/bin/env python3
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from content_review import curate_lesson, make_activities, make_assessment, profile_for, review_flags, sanitize_legacy_lesson


class ContentReviewTest(unittest.TestCase):
    def test_math_area_lesson_gets_precise_title_and_safe_examples(self):
        lesson = {
            "lessonId": "mathematics-g3-q1-w01-d02",
            "subject": "MATHEMATICS",
            "title": "points, lines, line segments,",
            "objective": "solve problems involving areas of squares and rectangles.",
            "language": "en-PH",
            "vocabulary": [{"term": "Area", "definition": "Ang sukat ng espasyo sa loob ng isang hugis."}],
            "activities": [],
            "assessment": {"items": []},
        }
        curated = curate_lesson(lesson)
        self.assertEqual(curated["title"], "Area Detectives")
        self.assertIn("length", curated["activities"][0]["content"].lower())
        self.assertEqual(curated["assessment"]["itemCount"], 5)
        self.assertTrue(all(len(i["correctOptionIds"]) == 1 for i in curated["assessment"]["items"]))

    def test_filipino_lesson_has_filipino_copy_and_no_template_placeholders(self):
        lesson = {
            "lessonId": "filipino-g3-q1-w01-d02",
            "subject": "FILIPINO",
            "title": "Natutukoy ang mga bahagi ng payak na pangungusap ✓ ✓",
            "objective": "Natutukoy ang mga bahagi ng payak na pangungusap ✓ ✓",
            "language": "fil-PH",
            "vocabulary": [],
            "activities": [],
            "assessment": {"items": []},
        }
        curated = curate_lesson(lesson)
        self.assertEqual(curated["language"], "fil-PH")
        self.assertNotIn("Complete the", str(curated))
        self.assertNotIn("A close-but-wrong", str(curated))
        self.assertTrue(all("Which statement" not in i["prompt"] for i in curated["assessment"]["items"]))
        self.assertTrue(any("pangungusap" in i["prompt"].lower() for i in curated["assessment"]["items"]))

    def test_legacy_filipino_shell_is_localized_and_idempotent(self):
        lesson = {
            "lessonId": "filipino-g3-m01-d01",
            "subject": "FILIPINO",
            "title": "Pangngalan sa Ating Mundo",
            "objective": "Uriin ang pangngalan bilang tao, hayop, bagay, lugar, o pangyayari.",
            "language": "fil-PH",
            "introduction": "You will explore examples, sort cards, practice, and answer five questions.",
            "activities": [{
                "type": "MATCHING_PAIRS",
                "content": {"pairs": [{"left": "guro", "right": "fits the lesson idea"}]},
                "instruction": "Complete the guided review in order.",
            }],
            "assessment": {"items": [{
                "prompt": "Alin ang tama?",
                "options": [{"id": "a", "text": "guro"}, {"id": "b", "text": "mabilis"}, {"id": "c", "text": "tumakbo"}],
                "correctOptionIds": ["a"],
            }]},
        }
        curated = sanitize_legacy_lesson(lesson)
        self.assertNotIn("You will explore", str(curated))
        self.assertNotIn("fits the lesson idea", str(curated))
        self.assertEqual(curated, sanitize_legacy_lesson(curated))

    def test_curated_lesson_is_idempotent(self):
        lesson = {
            "lessonId": "makabansa-g3-q4-w07-d04",
            "subject": "makabansa",
            "title": "Napahahalagahan ang papel ng Matrix Song",
            "objective": "Napahahalagahan ang papel ng Matrix Song: activity",
            "language": "fil-PH",
            "activities": [],
            "assessment": {"items": []},
        }
        once = curate_lesson(lesson)
        self.assertEqual(once, curate_lesson(once))

    def test_math_quarterly_activities_use_topic_grounded_distractors(self):
        lesson = {
            "lessonId": "mathematics-g3-q1-w01-d01",
            "subject": "MATHEMATICS",
            "title": "Shape Trail",
            "objective": "Recognize and describe points, lines, line segments, rays, and special line relationships.",
            "language": "en-PH",
            "vocabulary": [],
            "activities": [],
            "assessment": {"items": []},
        }
        curated = curate_lesson(lesson)
        blob = str(curated).lower()
        for filler in (
            "a random guess",
            "a mismatched unit",
            "an unrelated operation",
            "an answer with no label",
            "correct idea",
            "useful example",
            "check the concept",
        ):
            self.assertNotIn(filler, blob)

    def test_generated_assessment_prompts_are_unique_and_topic_grounded(self):
        lessons = [
            {
                "lessonId": "mathematics-g3-q1-w01-d01",
                "subject": "MATHEMATICS",
                "title": "Shape Trail",
                "objective": "Recognize and describe points, lines, line segments, rays, and special line relationships.",
                "language": "en-PH",
                "vocabulary": [],
                "activities": [],
                "assessment": {"items": []},
            },
            {
                "lessonId": "filipino-g3-q2-w06-d03",
                "subject": "FILIPINO",
                "title": "Bahagi ng Pangungusap",
                "objective": "Natutukoy ang mga bahagi ng payak na pangungusap.",
                "language": "fil-PH",
                "vocabulary": [],
                "activities": [],
                "assessment": {"items": []},
            },
        ]
        for lesson in lessons:
            curated = curate_lesson(lesson)
            items = curated["assessment"]["items"]
            prompts = [item["prompt"] for item in items]
            self.assertEqual(len(prompts), len(set(prompts)))
            self.assertTrue(all(len(item["correctOptionIds"]) == 1 for item in items))
            for item in items:
                correct = item["correctOptionIds"][0]
                option_ids = {option["id"] for option in item["options"]}
                self.assertIn(correct, option_ids)

    def test_live_mcq_correct_position_varies_by_lesson(self):
        profile = profile_for({
            "lessonId": "mathematics-g3-q1-w01-d01",
            "subject": "MATHEMATICS",
            "title": "Shape Trail",
            "objective": "Recognize and describe points, lines, line segments, rays, and special line relationships.",
            "language": "en-PH",
            "vocabulary": [],
            "activities": [],
            "assessment": {"items": []},
        })
        positions = []
        for lesson_id in (
            "mathematics-g3-q1-w01-d01",
            "mathematics-g3-q2-w03-d02",
            "mathematics-g3-q3-w05-d04",
            "mathematics-g3-q4-w09-d04",
        ):
            mcq = next(a for a in make_activities(profile, lesson_id) if a["type"] == "MULTIPLE_CHOICE")
            positions.append(mcq["content"]["correctIndex"])
            self.assertEqual(mcq["content"]["options"][mcq["content"]["correctIndex"]], profile["examples"][0])
        self.assertGreater(len(set(positions)), 1)

    def test_review_flags_rejects_placeholders_and_bad_assessment(self):
        lesson = {
            "lessonId": "x",
            "title": "Bad",
            "objective": "Bad",
            "activities": [{"instruction": "Complete the activity.", "content": {"options": []}}],
            "assessment": {"itemCount": 1, "items": [{"prompt": "", "options": [], "correctOptionIds": []}]},
        }
        flags = review_flags(lesson)
        self.assertIn("PLACEHOLDER_TEXT", flags)
        self.assertIn("ASSESSMENT_NOT_SINGLE_CORRECT", flags)


if __name__ == "__main__":
    unittest.main()
