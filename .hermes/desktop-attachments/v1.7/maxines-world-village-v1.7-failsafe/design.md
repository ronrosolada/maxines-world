# Maxine’s World Village Home v1.7

## Controlling decision

The expected target screenshot remains authoritative. The latest build is rejected because it rendered only overlay chrome on a blank cream canvas.

The v1.7 implementation removes nearly all compositing discretion. Use `mw_village_scene_v17.webp` as the single expanded-layout scene plate. It already contains the village, six buildings, animals, and empty illustrated UI surfaces. Add only native Compose content over the supplied normalized rectangles.

## Layer order

1. `mw_village_scene_v17.webp`, full scene canvas.
2. Native destination text and click semantics.
3. Native profile, quest, reward, and navigation content.
4. Temporary focus and celebration effects.

Never place an opaque root surface between layers 1 and 2.

## Scene geometry

The scene plate is exactly 1536×1024, aspect ratio 3:2. Render the entire scene in a centered 3:2 canvas. Do not crop it. Do not independently position the background and overlays.

The supplied `VillageHomeV17Screen` calculates one scene width and height, then uses those same dimensions for the image and every overlay.

## Strict implementation rule

Do not recreate, resize, tile, or assemble any visual frame. The empty profile panel, quest panel, reward chips, six destination signs, and bottom navigation are already illustrated in the scene plate.

Text remains native for localization and accessibility. Click targets remain native. The illustrated frames are decorative.

## Expanded layout acceptance

At 1353×949:

* The full village is visible.
* Six buildings and six signboards are visible.
* Mountains, river, vegetation, and animals are visible.
* No blank cream canvas occupies the scene.
* Each native label sits inside its illustrated sign.
* No second border appears around any sign.
* Navigation content sits inside the illustrated bottom bar.
* All enabled destinations open the correct subject.

## Compact layout

Below 840dp, use the code’s scrolling fallback. Do not squeeze or crop the full tablet interface.

## Prohibited code and resources

Do not use:

* `BambooPlaqueSurface`.
* Rattan corner assets.
* Horizontal or vertical bamboo rail assets.
* NinePatch bamboo assets.
* A tablet destination grid.
* A cream `Surface` covering the scene.
* `ContentScale.Crop` for the expanded scene plate.
* A separate background Box from the overlay coordinate Box.
* Text baked into new images.

## Build and evidence

Run every command in `README-APPLY.md`. Return the exact output, a 1353×949 screenshot, a 1280×800 screenshot, phone screenshots, and a route checklist for all six subjects.
