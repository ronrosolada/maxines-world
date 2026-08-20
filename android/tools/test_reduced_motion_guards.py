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

    def test_infinite_transitions_are_not_created_when_motion_is_disabled(self):
        sanctuary = (ROOT / "feature-child-home/src/main/java/com/maxinesworld/featurechildhome/SanctuaryScene.kt").read_text(encoding="utf-8")
        hotspot = (RENDERER_DIR / "HotspotImageRenderer.kt").read_text(encoding="utf-8")
        self.assertIn("val idleOffset by if (reduceMotion)", sanctuary)
        self.assertIn("val pulseScale by if (animationsDisabled)", hotspot)
        self.assertIn("rememberInfiniteTransition", sanctuary)
        self.assertIn("rememberInfiniteTransition", hotspot)


if __name__ == "__main__":
    unittest.main()
