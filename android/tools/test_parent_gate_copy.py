import unittest
from pathlib import Path


SOURCE = Path(__file__).resolve().parents[1] / "feature-parent/src/main/java/com/maxinesworld/featureparent/ParentGateScreen.kt"


class ParentGateCopyTest(unittest.TestCase):
    def test_lockout_copy_does_not_promise_removed_math_recovery(self):
        source = SOURCE.read_text(encoding="utf-8").lower()
        self.assertNotIn("quick math question", source)
        self.assertNotIn("simpleng tanong", source)
        self.assertIn("pause", source)


if __name__ == "__main__":
    unittest.main()
