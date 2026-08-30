import pathlib
import unittest


ANDROID = pathlib.Path(__file__).resolve().parents[1]


class AdversarialPlatformContractsTest(unittest.TestCase):
    def test_media_catalog_rejects_unknown_schema_versions(self):
        source = (ANDROID / "core-network/src/main/java/com/maxinesworld/corenetwork/MediaCatalogParser.kt").read_text()
        self.assertIn("catalog.catalogVersion in SUPPORTED_CATALOG_VERSIONS", source)

    def test_bundled_web_games_block_all_network_loads(self):
        source = (ANDROID / "app/src/main/java/com/maxinesworld/app/MiniGameWebScreen.kt").read_text()
        self.assertIn("settings.blockNetworkLoads = true", source)

    def test_update_checksum_token_requires_exact_sha256_shape(self):
        source = (ANDROID / "core-network/src/main/java/com/maxinesworld/corenetwork/AppUpdateManager.kt").read_text()
        self.assertIn("expectedToken.matches(SHA256_PATTERN)", source)


if __name__ == "__main__":
    unittest.main()
