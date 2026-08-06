import json
import sys
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

from generate_quarterly_assets import board_svg, quarterly_lessons


class QuarterlyAssetGeneratorTest(unittest.TestCase):
    def test_quarterly_inventory_has_one_lesson_per_visual_reference(self):
        lessons = quarterly_lessons()
        asset_ids = {f"{lesson['lessonId']}-visual" for lesson in lessons}
        self.assertEqual(len(lessons), 249)
        self.assertEqual(len(asset_ids), 249)

    def test_generated_board_is_valid_svg_with_accessible_metadata(self):
        lesson = next(
            lesson for lesson in quarterly_lessons()
            if lesson["lessonId"] == "mathematics-g3-q1-w01-d01"
        )
        root = ET.fromstring(board_svg(lesson))
        self.assertEqual(root.tag, "{http://www.w3.org/2000/svg}svg")
        self.assertEqual(root.attrib["viewBox"], "0 0 640 360")
        title = root.find("{http://www.w3.org/2000/svg}title")
        desc = root.find("{http://www.w3.org/2000/svg}desc")
        self.assertIsNotNone(title)
        self.assertIn("Shape Trail", title.text or "")
        self.assertIsNotNone(desc)
        self.assertIn("visual board", desc.text or "")

    def test_existing_quarterly_assets_are_complete_and_well_formed(self):
        assets = TOOLS.parent / "app/src/main/assets/content-pack/month-01/assets/vectors"
        for lesson in quarterly_lessons():
            asset = assets / f"{lesson['lessonId']}-visual.svg"
            self.assertTrue(asset.exists(), asset)
            root = ET.parse(asset).getroot()
            # Shipped visuals are topic-specific bespoke scenes (800×450);
            # the legacy 640×360 board template is no longer emitted
            # (bespoke_lesson_assets.py, 2026-08-06).
            self.assertEqual(root.attrib.get("viewBox"), "0 0 800 450")


if __name__ == "__main__":
    unittest.main()
