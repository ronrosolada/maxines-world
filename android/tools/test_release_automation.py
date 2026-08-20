import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[2] / "tools" / "bump_and_release.sh"


class ReleaseAutomationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.script = SCRIPT.read_text(encoding="utf-8")

    def test_release_runs_local_quality_gates_before_tagging(self):
        self.assertIn("testDebugUnitTest", self.script)
        self.assertIn("test_video_assessment_quality", self.script)
        self.assertNotIn("content_pack_validation.py --strict --require-released", self.script)
        self.assertNotIn("assessment_duplicate_gate.py", self.script)
        self.assertIn("gh run watch", self.script)
        self.assertLess(self.script.index("testDebugUnitTest"), self.script.index('git tag -a'))

    def test_release_waits_for_gate_and_verifies_deployed_hash(self):
        self.assertIn("gh run watch", self.script)
        self.assertIn("sha256sum", self.script)
        self.assertIn("curl -fsSI", self.script)
        self.assertNotIn("git pull origin main", self.script)


if __name__ == "__main__":
    unittest.main()
