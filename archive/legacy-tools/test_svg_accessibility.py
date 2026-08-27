from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

from add_svg_accessibility import accessible_svg

ROOT = Path(__file__).resolve().parents[1]
VECTOR_DIR = ROOT / "app/src/main/assets/content-pack/month-01/assets/vectors"
LESSON_DIR = ROOT / "app/src/main/assets/content-pack/month-01/lessons"


class SvgAccessibilityTest(unittest.TestCase):
    def test_every_lesson_visual_has_accessible_name_and_description(self):
        svgs = sorted(VECTOR_DIR.glob("*.svg"))
        self.assertGreaterEqual(len(svgs), 350)
        failures = []
        for path in svgs:
            text = path.read_text(encoding="utf-8")
            if not re.search(r'<svg\b[^>]*\brole=["\']img["\']', text):
                failures.append(f"{path.name}: missing role=img")
            if not re.search(r'<svg\b[^>]*\baria-labelledby=["\'][^"\']+["\']', text):
                failures.append(f"{path.name}: missing aria-labelledby")
            if not re.search(r'<title\b[^>]*>\s*[^<]+\s*</title>', text):
                failures.append(f"{path.name}: missing title")
            if not re.search(r'<desc\b[^>]*>\s*[^<]+\s*</desc>', text):
                failures.append(f"{path.name}: missing desc")
        self.assertEqual([], failures)

    def test_visuals_have_matching_lesson_source(self):
        missing = []
        for path in VECTOR_DIR.glob("*.svg"):
            lesson_id = path.stem.removesuffix("-visual")
            if not (LESSON_DIR / f"{lesson_id}.json").is_file():
                missing.append(path.name)
        self.assertEqual([], missing)

    def test_metadata_is_idempotent(self):
        sample = sorted(VECTOR_DIR.glob("*.svg"))[0]
        text = sample.read_text(encoding="utf-8")
        self.assertEqual(text, accessible_svg(text, sample))

    def test_every_svg_is_well_formed_xml(self):
        for path in sorted(VECTOR_DIR.glob("*.svg")):
            with self.subTest(path=path.name):
                self.assertIsNotNone(ET.parse(path).getroot().tag)


if __name__ == "__main__":
    unittest.main()
