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

    def test_network_security_config_forbids_cleartext(self):
        source = (ANDROID / "app/src/main/res/xml/network_security_config.xml").read_text()
        self.assertIn('cleartextTrafficPermitted="false"', source)
        self.assertNotIn('cleartextTrafficPermitted="true"', source)

    def test_app_declares_no_package_install_permission(self):
        manifest = (ANDROID / "app/src/main/AndroidManifest.xml").read_text()
        self.assertNotIn(
            'android:name="android.permission.REQUEST_INSTALL_PACKAGES"',
            manifest,
        )

    def test_child_home_accredited_time_filters_to_child_facing_curriculum(self):
        source = (
            ANDROID / "feature-child-home/src/main/java/com/maxinesworld/featurechildhome/PlayroomHomeViewModel.kt"
        ).read_text()
        self.assertIn("ChildFacingMediaPolicy.childFacing", source)
        self.assertIn("accreditedSecondsForChildFacing", source)
        self.assertNotIn(
            "videoWatchLedgerDao.observeTotalAccreditedSeconds(childId)",
            source,
        )

    def test_video_library_first_pass_and_download_recheck_child_facing_policy(self):
        source = (
            ANDROID / "feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/VideoLibraryViewModel.kt"
        ).read_text()
        self.assertIn("shouldCreditCurriculumWatch", source)
        self.assertIn("downloadChildFacing", source)
        self.assertIn("accreditedSecondsForChildFacing", source)

    def test_media_library_child_download_requires_child_facing_curriculum(self):
        source = (ANDROID / "core-network/src/main/java/com/maxinesworld/corenetwork/MediaLibrary.kt").read_text()
        self.assertIn("fun downloadChildFacing", source)
        self.assertIn("ChildFacingMediaPolicy.isChildFacingCurriculum", source)
        prefetch = (
            ANDROID / "core-network/src/main/java/com/maxinesworld/corenetwork/VideoPrefetchManager.kt"
        ).read_text()
        self.assertIn("downloadChildFacing", prefetch)


if __name__ == "__main__":
    unittest.main()
