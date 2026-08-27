import pathlib
import unittest


ANDROID_ROOT = pathlib.Path(__file__).resolve().parents[1]
LESSON_PLAYER = ANDROID_ROOT / "feature-lesson-player" / "src" / "main" / "java" / "com" / "maxinesworld" / "featurelessonplayer"


class AudioFocusContractTest(unittest.TestCase):
    def test_lesson_video_requests_media_audio_focus(self):
        source = (LESSON_PLAYER / "VideoStep.kt").read_text(encoding="utf-8")
        self.assertIn("setAudioAttributes(mediaAudioAttributes(), true)", source)

    def test_quick_bits_video_requests_media_audio_focus(self):
        source = (LESSON_PLAYER / "QuickBitsScreen.kt").read_text(encoding="utf-8")
        self.assertIn("setAudioAttributes(mediaAudioAttributes(), true)", source)

    def test_tts_requests_and_releases_transient_audio_focus(self):
        source = (LESSON_PLAYER / "LessonTtsPlayer.kt").read_text(encoding="utf-8")
        self.assertIn("requestAudioFocus", source)
        self.assertIn("abandonAudioFocusRequest", source)
        self.assertIn("AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK", source)


if __name__ == "__main__":
    unittest.main()
