# Maxine’s World UI Overhaul — Apply Guide

## What this patch changes

- Replaces the flat Canvas village with a layered illustrated scene.
- Removes the empty Daily Challenge strip.
- Adds six equal destinations: English, Filipino, Mathematics, Science, Makabansa/History, and GMRC.
- Uses the supplied transparent building and guide-character assets.
- Adds a generated text-free 16:9 background plate.
- Replaces emoji and brown overlays with native, accessible Compose surfaces.
- Uses a tablet scene and a compact two-column card layout from the same destination model.
- Removes production no-op actions: unavailable navigation items are visibly disabled.
- Makes the active Daily Quest the single strongest call to action.

## Apply

From this package root, copy the `android` directory over the repository root:

```bash
rsync -av android/ /path/to/maxines-world/android/
cd /path/to/maxines-world/android
./gradlew :feature-child-home:compileDebugKotlin
./gradlew assembleDebug
```

No existing resource is overwritten except `VillageHomeScreen.kt`; all image resource names are new.

## Intentional limitation

`Kindness Corner` has equal size, detail, artwork, and placement, but is marked “Opening soon” because the audited repository has no GMRC lesson route. Do not map it to an unrelated English lesson. Enable it only after adding a real GMRC content ID to `MaxinesNavGraph.kt`.

Profile, Achievements, and Backpack remain visible but disabled because the audited navigation graph has no routes for them. This is deliberate: disabled is honest and testable; a visible no-op is not.

## Required next engineering steps

1. Move the destination list into a repository/view-model once curriculum catalog support exists.
2. Replace the sample progress values with real per-child progress.
3. Add real Profile, Achievements, and Backpack routes, then pass their callbacks.
4. Add a GMRC lesson route and set `available = true` for Kindness Corner.
5. Bundle Baloo 2 and Nunito TTF files and update the design-system typography. Do not use downloadable fonts because the app is offline-first.
6. Localize all visible strings into `strings.xml` before release.
7. Add screenshot tests for 360×800, 600×960, 1280×800, and 1672×941.

## Pass/fail visual checks

- All six destinations are visible or intentionally reachable; none is clipped.
- Every destination occupies the same grid area and uses a building plus guide character.
- The Daily Quest has objective, duration, progress, rewards, and Start/Continue action.
- No emoji is present in the production homepage.
- No text is baked into artwork.
- No enabled control uses an empty callback.
- Every subject marker has a native semantic button role and state description.
- At 200% font size, the compact layout scrolls without horizontal overflow.
- Tablet content is centered and capped at 1440dp.
- The background remains decorative and is omitted from TalkBack focus.

## Art provenance

- Building and character PNGs: user-supplied `Maxine's World app design.zip`.
- `village_background.webp`: newly generated for this overhaul; it contains no text, UI, characters, or buildings.
