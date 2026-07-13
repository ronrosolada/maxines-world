# Design Deviations

Documented intentional divergences from `docs/design.md`.

## 1. Theme.kt Mutable Package Variables

**Requirement:** "Replace mutable typography globals with immutable Baloo 2 and Nunito resource families."

**Deviation:** `Theme.kt` MUST keep mutable package-level variables (`var AppDisplayFont`, `var AppBodyFont`).

**Reason:** Parameterized composable `MaxinesWorldTheme(displayFont, bodyFont, content)` with `remember()` causes immediate crash on Xiaomi Pad 6S Pro tablets. Confirmed through 8 isolation builds (v0.6.5–v0.6.13). The crash appears related to Typography identity changes causing CompositionLocal invalidation in Compose runtime.

**Impact:** Font families are still Baloo 2 and Nunito — only the assignment mechanism differs. No visual impact.

**Follow-up:** Test when Compose runtime is updated or when Xiaomi firmware is patched. The fix likely requires a Compose runtime change, not an app change.

**Commit reference:** `e8047de` through `2a068fd` (isolation test series).

## 2. Emoji as Placeholder Feedback Art

**Requirement:** "Do NOT ship emoji as final character, subject, reward, or feedback artwork."

**Deviation:** Some feedback text uses emoji placeholders (e.g., `"🎉 Badge earned!"`).

**Reason:** Production icon/raster assets for celebrations not yet produced. Emoji serves as a recognizable placeholder until the P0 asset manifest is fulfilled.

**Impact:** Visual only. Functionality is correct. Emoji connotes meaning that native UI does not yet represent.

**Follow-up:** Replace with production assets per the P0 asset manifest when available.

## 3. Concept Image as Village Background

**Requirement:** "Concept images communicate mood and hierarchy only. They MUST NOT be embedded as complete application screens."

**Deviation:** `VillageHomeScreen` uses a Canvas-drawn landscape (sky gradient, mountains, grass, buildings) derived from concept art.

**Reason:** The Canvas drawing is NOT an embedded concept image — it is a native Compose rendering that adapts to screen size. It communicates mood and destination positions without being a fixed image.

**Impact:** None — this is compliant with the spirit of the requirement. The Canvas adapts to orientation and breakpoints.

## 4. Continuous Character Bobbing (Pending Reduced-Motion Fix)

**Requirement:** "Do NOT run infinite animation under reduced motion."

**Deviation:** Village character images use `infiniteRepeatable` for gentle bobbing animation.

**Reason:** Reduced-motion support is being added (in-flight sub-agent). Until merged, reduced-motion users will see bobbing.

**Impact:** Users with reduced motion enabled will experience unnecessary animation.

**Follow-up:** Fix in progress.

## 5. Subject Palette for GMRC

**Requirement:** "GMRC MUST receive its own palette, subject-world definition, icon, location art, and lesson-screen examples."

**Deviation:** GMRC has no dedicated palette or location art.

**Reason:** GMRC content packages exist on the NAS but no dedicated assets have been produced. The app maps GMRC to English lesson content as fallback.

**Impact:** GMRC subject uses generic styling. No GMRC-specific village location.

**Follow-up:** Add when P0 asset manifest includes GMRC location + icon.

## 6. Default Callbacks on Some Navigation Items

**Requirement:** "Do NOT claim completion when a required state is represented by a TODO or no-op callback."

**Deviation:** Profile and Backpack navigation items have default `{}` callbacks.

**Reason:** These destinations are not yet implemented. Bottom bar items remain for layout consistency but trigger no navigation.

**Impact:** Tapping Profile or Backpack produces no visible response. Learners may be confused.

**Follow-up:** Implement Profile and Backpack screens, or replace disabled items with grayed-out indicators.
