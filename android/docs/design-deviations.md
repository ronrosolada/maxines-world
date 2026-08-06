# Design Deviations

Documented intentional divergences from `docs/design.md`.

## 1. Theme.kt Mutable Package Variables

**Requirement:** "Replace mutable typography globals with immutable Baloo 2 and Nunito resource families."

**Deviation:** `Theme.kt` MUST keep mutable package-level variables (`var AppDisplayFont`, `var AppBodyFont`).

**Reason:** Parameterized composable `MaxinesWorldTheme(displayFont, bodyFont, content)` with `remember()` causes immediate crash on Xiaomi Pad 6S Pro tablets. Confirmed through 8 isolation builds (v0.6.5–v0.6.13). The crash appears related to Typography identity changes causing CompositionLocal invalidation in Compose runtime.

**Impact:** Font families are still Baloo 2 and Nunito — only the assignment mechanism differs. No visual impact.

**Follow-up:** Test when Compose runtime is updated or when Xiaomi firmware is patched. The fix likely requires a Compose runtime change, not an app change.

**Commit reference:** `e8047de` through `2a068fd` (isolation test series).

## 2. Authored Emoji in Legacy Content

**Requirement:** "Do NOT ship emoji as final character, subject, reward, or feedback artwork."

**Deviation:** A small amount of bundled/authored lesson copy and the legacy badge catalog asset still contain emoji keys/placeholders; the runtime badge and sticker models no longer expose them.

**Reason:** Production icon/raster assets for celebrations not yet produced. Emoji serves as a recognizable placeholder until the P0 asset manifest is fulfilled.

**Impact:** Runtime reward, badge, sticker, character-guide, and renderer fallback UI now uses vector/icon treatments, but legacy content can still surface emoji in learner copy until the content asset pass is complete.

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

**Status:** Resolved for curriculum behavior. GMRC has its own subject token and routes to real bundled GMRC lessons from the first session. Dedicated location art remains an asset follow-up.

**Reason:** The app now uses bundled GMRC content; the remaining gap is visual production rather than curriculum availability.

**Impact:** GMRC still needs dedicated location art and lesson-screen examples to fully satisfy the visual requirement.

**Follow-up:** Add when P0 asset manifest includes GMRC location + icon.

## 6. Default Callbacks on Some Navigation Items

**Requirement:** "Do NOT claim completion when a required state is represented by a TODO or no-op callback."

**Status:** Resolved in the current child navigation. Profile and Backpack are not shipped as tappable bottom-bar items; the current bar exposes Home, Collection, and Parents only.

**Reason:** Unimplemented destinations were removed rather than left as dead controls.

**Impact:** No child-facing dead tap remains for these reserved destinations.

**Follow-up:** Add these destinations only with complete screens and navigation tests.
