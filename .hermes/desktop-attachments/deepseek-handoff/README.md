# Maxine’s World DeepSeek UI Handoff

Start with `DEEPSEEK_IMPLEMENTATION.md`. It is the controlling implementation contract.

## Core assets

* `assets/background/village_home_six_landmarks_master.png`: new opaque 16:9 scene plate with all six subjects at equal visual prominence and no baked UI.
* `assets/vectors/*.xml`: Android VectorDrawable symbols for native HUD, navigation, and subject badges.
* `LANDMARK_LAYOUT.json`: normalized tablet hit zones and HUD zones.
* `LAYOUT_MAP.svg`: visual coordinate map; documentation only, not a runtime asset.
* `references/current_build.png`: the latest build that must be improved.
* `references/original_target_board.png`: mood and richness reference, not a bitmap UI to ship.
* `source-design/`: authoritative design source material.

## Runtime rule

Use the new scene plate as one background image on expanded/tablet layouts. Do not place the old flat building sprites on top of it. All labels, progress, lock states, TODAY state, quest, currencies, and navigation remain native Compose.

## Asset integrity

Check `ASSET_MANIFEST.json` before implementation. Do not silently replace or re-encode the scene until screenshot comparison is complete.
