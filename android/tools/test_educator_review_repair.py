import copy
import importlib.util
import json
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location(
    "educator_review_repair", TOOLS_DIR / "educator_review_repair.py"
)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("unable to load educator_review_repair.py")
REVIEW = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REVIEW)


class EducatorReviewRepairTest(unittest.TestCase):
    def test_canonical_pack_is_idempotent(self):
        for path in sorted(REVIEW.CANONICAL.glob("*.json")):
            lesson = json.loads(path.read_text(encoding="utf-8"))
            self.assertFalse(REVIEW.repair_lesson(lesson), path.name)

    def test_makabansa_repair_updates_explanations_with_reordered_options(self):
        path = REVIEW.CANONICAL / "makabansa-g3-q4-w07-d03.json"
        lesson = json.loads(path.read_text(encoding="utf-8"))
        lesson["assessment"]["items"][0]["explanation"] = "stale answer text"

        self.assertTrue(REVIEW.repair_lesson(lesson))
        item = lesson["assessment"]["items"][0]
        correct_id = item["correctOptionIds"][0]
        correct_text = next(option["text"] for option in item["options"] if option["id"] == correct_id)
        self.assertEqual(f"Ang pinakamainam na sagot ay: {correct_text}", item["explanation"])

        repaired = copy.deepcopy(lesson)
        self.assertFalse(REVIEW.repair_lesson(repaired))
        self.assertEqual(lesson, repaired)


if __name__ == "__main__":
    unittest.main()
