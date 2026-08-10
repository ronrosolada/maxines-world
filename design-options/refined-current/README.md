# Refined current design — Playroom flow

This sample is a refinement of the current Maxine’s World Playroom, not a redesign and not a village/map concept.

## Preserved

- Gold → coral background family
- Cream surfaces, teal primary action, subject colors
- Baloo 2 + Nunito typography
- Mascot, subject, reward, sanctuary, and sticker-book language
- Tactile rounded cards and bottom navigation
- Child-safe, learning-first tone

## Improvements shown

1. **Flow alignment** — follows the actual Playroom sequence: greeting → resume learning → optional videos → quest → subjects → sanctuary/stickers → navigation.
2. **Clearer arrival hierarchy** — greeting first, profile/stats grouped at the top, with the helper mascot kept supportive rather than competing with the lesson.
3. **One obvious next action** — the resume card and Today’s Quest use clear, action-led CTAs.
4. **Subject cards scan faster** — a consistent six-icon hand-painted set generated through ComfyUI/Comfy Cloud, background-removed to transparent foregrounds and normalized to 1254×1254 RGBA assets, with formal subject names, progress, status, and explicit lesson entry.
5. **Subject section is functional** — Android routes each available card to its subject modules screen; the HTML review sample exposes the same interaction with an explicit “Open lessons” action.
6. **Rewards remain downstream** — sanctuary and wildlife stickers stay visible without becoming the home-screen purpose.
7. **Native navigation treatment** — interactive bottom navigation is drawn as controls rather than a decorative image.
8. **Responsive behavior** — desktop, tablet, and phone layouts retain the same hierarchy without hidden essential navigation.

The small header mascot/avatar was removed from both the Compose implementation and the HTML review sample because it was decorative and had no profile destination. The larger guide mascot remains the single intentional focal character, using a transparent-background RGBA asset so it integrates with the card instead of appearing as a pasted square.
