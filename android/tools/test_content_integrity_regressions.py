import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LESSON_DIR = ROOT / "app/src/main/assets/content-pack/month-01/lessons"


class ContentIntegrityRegressionTest(unittest.TestCase):
    def test_matching_right_labels_are_unique_within_each_activity(self):
        failures = []
        for path in sorted(LESSON_DIR.glob("*.json")):
            lesson = json.loads(path.read_text(encoding="utf-8"))
            for activity in lesson.get("activities", []):
                if activity.get("type") != "MATCHING_PAIRS":
                    continue
                rights = [pair.get("right", "") for pair in activity.get("content", {}).get("pairs", [])]
                if len(rights) != len(set(rights)):
                    failures.append(f"{path.name}:{activity.get('activityId')}")
        self.assertEqual([], failures)

    def test_fil_packs_do_not_use_english_password_distractor(self):
        failures = []
        for path in sorted(LESSON_DIR.glob("*.json")):
            lesson = json.loads(path.read_text(encoding="utf-8"))
            if not str(lesson.get("language", "")).startswith("fil"):
                continue
            raw = path.read_text(encoding="utf-8").lower()
            if "password ng" in raw or "password" in raw:
                failures.append(path.name)
        self.assertEqual([], failures)

    def test_known_makabansa_key_matches_its_explanation(self):
        path = LESSON_DIR / "makabansa-g3-q2-w02-d03.json"
        lesson = json.loads(path.read_text(encoding="utf-8"))
        item = lesson["assessment"]["items"][1]
        self.assertEqual("makabansa-g3-q2-w02-d03-q02", item["itemId"])
        self.assertEqual(["b"], item["correctOptionIds"])
        self.assertIn("pagpapahalaga sa pinagmulan", item["explanation"])


if __name__ == "__main__":
    unittest.main()
