# Visual and accessibility asset pipeline

The child-facing app keeps its visual assets offline and reproducible.

## Mini-game artwork

`tools/generate_minigame_art.py` generates the 29 curated mini-game thumbnails into
`app/src/main/res/drawable-nodpi/`. The catalog maps every embedded game to one
specific `mw_game_*.png` resource; emoji are intentionally not used as artwork.

```bash
python3 tools/generate_minigame_art.py
python3 tools/test_minigame_artwork.py
```

## GMRC location artwork

`tools/generate_gmrc_art.py` generates the Kindness Corner hero artwork used by
the GMRC module screen.

## Lesson SVG accessibility

`tools/add_svg_accessibility.py` derives each SVG's title and description from
its lesson JSON. For hotspot visuals it also includes the authored instruction
and examples, so screen readers receive the same visual clues the activity
expects the child to find.

```bash
python3 tools/add_svg_accessibility.py
python3 tools/add_svg_accessibility.py --check
python3 tools/test_svg_accessibility.py
```

The check is idempotent and validates that all 358 SVGs have `role="img"`,
`aria-labelledby`, a non-empty `<title>`, a non-empty `<desc>`, and well-formed
XML.

## Reduced motion

Compose color transitions in activity renderers use `LocalAnimationsDisabled`.
When Android's reduced-motion setting is active, transitions use a snap spec.
`tools/test_reduced_motion_guards.py` protects this convention from regression.
