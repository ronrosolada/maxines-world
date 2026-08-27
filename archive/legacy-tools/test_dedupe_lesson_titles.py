import json
import sys
import unittest
from collections import Counter
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

import dedupe_lesson_titles as dedupe


class LessonTitleDedupeTest(unittest.TestCase):
    def test_bundled_titles_are_unique(self):
        lessons = dedupe.load_lessons()
        titles = [lesson["title"] for _, lesson in lessons]
        duplicates = {title: count for title, count in Counter(titles).items() if count > 1}
        self.assertEqual({}, duplicates)

    def test_title_plan_is_idempotent_and_uses_stable_qualifiers(self):
        lessons = dedupe.load_lessons()
        planned = dedupe.planned_titles(lessons)
        self.assertEqual({path: lesson["title"] for path, lesson in lessons}, planned)
        self.assertNotIn(" · Q", planned[dedupe.LESSON_DIR / "filipino-g3-q1-w01-d01.json"])
        # Legacy month-format lessons still retain their stable internal
        # qualifier; the app strips it from child-facing display text.
        self.assertIn(" · M01 D01", planned[dedupe.LESSON_DIR / "english-g3-m01-d01.json"])

    def test_json_remains_parseable(self):
        for path in dedupe.lesson_files():
            with self.subTest(path=path.name):
                json.loads(path.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
