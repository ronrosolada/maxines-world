---
name: maxines-world-content-delight
description: Make Maxine's World K-3 lessons more engaging for ~8-year-olds — short story hooks, emoji scene banners, playful Filipino copy, kid SVG boards, and light motion that respects reduced-motion. Use when improving lesson interest, graphics, animation, or "make content fun for kids."
triggers:
  - more interesting for kids
  - graphics and animation for lessons
  - kid-friendly content polish
  - content delight / engagement
  - emoji scenes / visualScene
  - make lessons fun
---

# Maxine's World — Content Delight Playbook

Use this after curriculum-faithful content exists (module-builder / maxines-world-content). **Do not replace pedagogy** — decorate and shorten for attention, not dumb down standards.

## Goals for an 8-year-old

1. **See first, read second** — big visual scene before dense text.
2. **Adventure frame** — every lesson opens with a 1-line hook (Filipino for Filipino subject).
3. **Short UI lines** — max ~2 lines / ~90 chars for `instruction`; put detail in TTS narration.
4. **Celebrate wins** — fun correct/retry strings with emoji (not shaming).
5. **Motion with manners** — soft bob/pulse only; honor reduced-motion.

## Content JSON enrichments (Month1 / lesson pack)

Per lesson:

```json
{
  "introduction": "Si Milo ay may misteryo! 🐱✨ …",
  "storyIntro": "same hook",
  "scene": { "character": "Milo", "setting": "Bahay ng Kuwento", "visualScene": "🐱🏫📚🌟" },
  "activities": [{
    "instruction": "Short kid line (≤90 chars).",
    "content": {
      "...activity fields...": true,
      "visualScene": "🐱🏫📚🌟",
      "celebrationEmoji": "🎉⭐🐾"
    },
    "feedback": {
      "correct": "Yehey! Tama! 🎉",
      "retry": "Halos na! Subukan muli 💪"
    },
    "assetId": "filipino-g3-m01-d01-visual"
  }]
}
```

### Rules

| Field | Rule |
|-------|------|
| `visualScene` | 3–5 emoji, theme-specific, no instructional words |
| `instruction` | One kid sentence; keep competency intent |
| MC `options` | Optional light emoji prefix; never change meaning |
| Assessment | Prefer clean text (no emoji clutter on scored items) |
| Language | Match subject: Filipino lessons stay `fil-PH` |
| Copyright | Original SVG/emoji scenes only — no DepEd PDF art |

### Theme table pattern (per day/module)

Assign a unique scene + hook per module (example Filipino Q1):

| Day | Scene | Hook idea |
|-----|-------|-----------|
| Pangngalan | 🐱🏫📚🌟 | Mystery names in the village |
| Panuto | 🧼🙌💧✨ | Step-map adventure |
| Elemento | 🎬👧🏡🌟 | Story movie crew |

## SVG boards

Path: `content-pack/month-01/assets/vectors/{lessonId}-visual.svg`

Kid board checklist:

- Bright gradient sky + hills (soft pastels)
- Simple schoolhouse / book / cat (Milo-inspired silhouette, **do not redraw proprietary Milo art** — abstract cat OK)
- Optional emoji strip at top (scene pack)
- **No dense instructional text** in the image
- 800×450 viewBox, rounded feel

## Player wiring (required for kids to *see* graphics)

`ActivityContentMapper` must map:

- `content.visualScene` → `ActivityStep.imageAssets[0]`
- `content.celebrationEmoji` → optional second imageAsset
- `assetId` → `asset:{id}` token (for future SVG loader)

Renderers should show `KidSceneBanner(imageAssets)` on:

- ANIMATED_EXPLANATION
- MULTIPLE_CHOICE
- HOTSPOT_IMAGE (plus tappable regions)

`KidSceneBanner`: large emoji row + soft vertical bob; skip bob when reduced-motion.

Guide avatar: larger (56dp+) soft scale pulse on explanation step.

Filipino chrome strings via `lessonUiStrings(fil-PH)` (Basahin Natin / Susunod / Ipasa / …).

## Pipeline per subject batch

1. Keep validated lesson structure (activities + assessment).
2. Run/enrich with hooks + `visualScene` + short instructions + SVG boards.
3. Ensure mapper passes `imageAssets`.
4. Rebuild APK / sync content pack.
5. Kid smoke test: open lesson → see banner → real MC options → advance past step 2.
6. Leave `educatorValidated=false` until educator pass.

## Do NOT

- Add continuous high-stimulus flashing
- Replace assessment correctness for “fun”
- Translate Filipino lessons to English for “clarity”
- Ship DepEd PDF illustrations
- Leave empty MC options (always map `content.options` + `correctIndex`)

## Verification

- [ ] Hook visible / introduction kid-length
- [ ] Banner emoji visible on step 1
- [ ] MC options real text (not A/B/C/D)
- [ ] Correct feedback Filipino + celebratory
- [ ] Progress advances after correct answer
- [ ] Reduced-motion path does not require bob for completion

## Related skills

- `maxines-world-module-builder` — first-pass pack from SLMs
- `maxines-world-content` — packaging / DreamNAS / bootstrap
- Player fixes: content mapping + Filipino UI chrome (lesson player)
