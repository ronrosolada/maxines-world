from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
RENDERER_DIR = ROOT / "engine-activity/src/main/java/com/maxinesworld/engineactivity/renderers"


class ReducedMotionGuardTest(unittest.TestCase):
    def test_activity_color_transitions_honor_system_reduced_motion(self):
        offenders = []
        for path in sorted(RENDERER_DIR.glob("*.kt")):
            text = path.read_text(encoding="utf-8")
            if "animateColorAsState" not in text:
                continue
            required = (
                "LocalAnimationsDisabled.current",
                "animationSpec = if (animationsDisabled) snap()",
            )
            missing = [marker for marker in required if marker not in text]
            if missing:
                offenders.append(f"{path.name}: {', '.join(missing)}")
        self.assertEqual([], offenders)


if __name__ == "__main__":
    unittest.main()
