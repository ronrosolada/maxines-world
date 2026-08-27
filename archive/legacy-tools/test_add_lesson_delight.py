import unittest

from tools.add_lesson_delight import normalize_compass_activity, subject_challenge


class AddLessonDelightTests(unittest.TestCase):
    def test_math_uses_authored_multiple_choice_as_subject_challenge(self):
        lesson = {
            "subject": "MATHEMATICS",
            "objective": "Compare numbers up to 10,000 using =, >, and <.",
            "activities": [{
                "type": "MULTIPLE_CHOICE",
                "content": {
                    "options": ["4,500 > 4,050", "4,500 < 4,050", "4,500 = 4,050"],
                    "correctIndex": 0,
                },
            }],
        }

        challenge = subject_challenge(lesson)

        self.assertIsNotNone(challenge)
        self.assertEqual("math_choice_v1", challenge["kind"])
        self.assertEqual(0, challenge["correctIndex"])
        self.assertEqual("4,500 > 4,050", challenge["options"][0])

    def test_invalid_authored_choice_falls_back_without_filler(self):
        lesson = {
            "subject": "SCIENCE",
            "objective": "Describe materials safely.",
            "activities": [{
                "type": "MULTIPLE_CHOICE",
                "content": {
                    "options": ["a random symbol", "a clear example", "a different topic"],
                    "correctIndex": 1,
                },
            }],
        }

        self.assertIsNone(subject_challenge(lesson))

    def test_compass_answers_are_rotated_deterministically(self):
        lesson = {"lessonId": "english-g3-m01-d01"}
        activity = {
            "type": "INTERACTIVE_SPEC",
            "content": {
                "options": ["correct clue", "distractor one", "distractor two"],
                "correctIndex": 0,
            },
        }

        changed = normalize_compass_activity(lesson, activity)
        first_options = list(activity["content"]["options"])
        first_index = activity["content"]["correctIndex"]

        self.assertTrue(changed)
        self.assertEqual("correct clue", first_options[first_index])
        self.assertNotEqual(0, first_index)

        self.assertFalse(normalize_compass_activity(lesson, activity))
        self.assertEqual(first_options, activity["content"]["options"])
        self.assertEqual(first_index, activity["content"]["correctIndex"])


if __name__ == "__main__":
    unittest.main()
