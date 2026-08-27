from pathlib import Path
import re
import struct
import unittest

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/maxinesworld/app/MiniGameCatalog.kt"
ART_DIR = ROOT / "app/src/main/res/drawable-nodpi"


class MiniGameArtworkTest(unittest.TestCase):
    def test_every_embedded_game_has_individual_png_artwork(self):
        source = CATALOG.read_text(encoding="utf-8")
        slugs = re.findall(r'EmbeddedMiniGame\(\s*"([^"]+)"', source)
        self.assertEqual(29, len(slugs))
        missing = [slug for slug in slugs if not (ART_DIR / f"mw_game_{slug.replace('-', '_')}.png").is_file()]
        self.assertEqual([], missing)

    def test_artwork_is_512_by_288_png(self):
        source = CATALOG.read_text(encoding="utf-8")
        slugs = re.findall(r'EmbeddedMiniGame\(\s*"([^"]+)"', source)
        bad = []
        for slug in slugs:
            path = ART_DIR / f"mw_game_{slug.replace('-', '_')}.png"
            if not path.is_file():
                bad.append(f"{slug}: missing")
                continue
            data = path.read_bytes()
            if data[:8] != b"\x89PNG\r\n\x1a\n" or len(data) < 24:
                bad.append(f"{slug}: not PNG")
                continue
            width, height = struct.unpack(">II", data[16:24])
            if (width, height) != (512, 288):
                bad.append(f"{slug}: {width}x{height}")
        self.assertEqual([], bad)

    def test_catalog_contains_no_emoji_artwork_placeholders(self):
        source = CATALOG.read_text(encoding="utf-8")
        emoji = re.search(r"[\U0001F000-\U0001FAFF\u2600-\u27BF]", source)
        self.assertIsNone(emoji, f"emoji artwork placeholder remains: {emoji.group(0) if emoji else ''}")


if __name__ == "__main__":
    unittest.main()
