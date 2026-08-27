import json
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LESSON_PATH = ROOT / "app/src/main/assets/content-pack/month-01/lessons/english-g3-q1-w01-d01.json"
ASSET_PATH = ROOT / "app/src/main/assets/content-pack/month-01/assets/vectors/english-g3-q1-w01-d01-visual.svg"
SVG_NS = "{http://www.w3.org/2000/svg}"


class FiestaVisualAssetTest(unittest.TestCase):
    REQUIRED_SCENE_GROUPS = {
        "red-flag",
        "people-dancing",
        "food-on-table",
        "children-laughing",
        "band-playing",
        "streamers-on-street",
        "parade",
        "lanterns",
    }

    def test_lesson_uses_a_real_fiesta_scene_with_all_eight_clues(self):
        lesson = json.loads(LESSON_PATH.read_text(encoding="utf-8"))
        asset_id = "english-g3-q1-w01-d01-visual"
        self.assertTrue(all(activity.get("assetId") == asset_id for activity in lesson["activities"]))

        root = ET.parse(ASSET_PATH).getroot()
        self.assertEqual(root.attrib.get("role"), "img")
        self.assertEqual(root.attrib.get("aria-labelledby"), "fiesta-title fiesta-desc")
        self.assertEqual(root.findtext(f"{SVG_NS}title"), "The Fiesta Picture")
        self.assertIn("fiesta scene", root.findtext(f"{SVG_NS}desc", "").lower())

        elements_by_id = {
            element.attrib["id"]: element
            for element in root.iter()
            if "id" in element.attrib
        }
        missing = self.REQUIRED_SCENE_GROUPS - elements_by_id.keys()
        self.assertFalse(missing, f"Missing authored fiesta scene groups: {sorted(missing)}")
        for group_id in self.REQUIRED_SCENE_GROUPS:
            self.assertTrue(
                list(elements_by_id[group_id]),
                f"Fiesta group {group_id} has no drawn child elements",
            )

        rendered_text = " ".join(
            (element.text or "").strip()
            for element in root.iter(f"{SVG_NS}text")
        )
        self.assertIn("FIESTA DAY", rendered_text)
        self.assertNotIn("Part 1 Focus", rendered_text)
        self.assertNotIn("Part 2 Focus", rendered_text)
        self.assertNotIn("Part 3 Focus", rendered_text)

    def test_no_bundled_visual_uses_the_retired_generic_focus_board(self):
        assets = ASSET_PATH.parent
        generic = [
            path.name
            for path in assets.glob("*.svg")
            if "Part 1 Focus" in path.read_text(encoding="utf-8")
            or "activity-visuals" in path.read_text(encoding="utf-8")
        ]
        self.assertEqual([], generic)


if __name__ == "__main__":
    unittest.main()
