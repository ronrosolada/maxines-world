# Milo Reward Celebration — Prototype

Live on `feat/playlist-video-replacement`

## Open the prototype

### 1 — Milo state-machine (hero reveal)

```bash
cd /opt/data/projects/maxines-world/sketches/milo-reward-proto/preview
python3 -m http.server 8819
# → http://10.10.10.5:8819/  (DreamNAS) or http://localhost:8819
```

Four states: **idle → anticipate → celebrate → calm**. Drag the easing curve, swap duration, toggle background swatches, test X-ray and reduced-motion. `celebrate` adds sparkle + confetti orphans (fade in/out, `twinkle` idle). All shared paths route `TRANSFORM` so bezier curves stay perfect.

Load order: `milo.states.js` → `exporters.js` → `engine.jsx` → `app.jsx` (see `index.html`).

### 2 — Delight Lab (all 8 moments)

```
sketches/milo-reward-proto/delight-lab.html
# self-contained, no build — open file:// or via same python server
```

Interactive cards for answer feedback, quest bar, field guide, sanctuary board, lesson-complete-lite, and reward gate morph.

## Where animations land (ranked)

| # | Surface | Hook in repo | Motion |
|---|---------|--------------|--------|
| 1 | **Sticker reveal** | `feature-lesson-player/LessonPlayerScreen` → `BadgeRevealScreen` | Milo `neutral→cheer→star-eyes`, badge `bouncy` 800ms, 24-particle confetti gated by `LocalAnimationsDisabled` |
| 2 | **First Steps milestone** | `BadgeAwarder.FIRST_STEPS_BADGE_ID` | Gold-exclusive overshoot + `twinkle` idle |
| 3 | **Field Guide** | `featurerewards/WildlifeFieldGuideScreen` | Collected `breathe-y` 2.5s; locked tap → `shake` deny |
| 4 | **Sanctuary Board** | `featurechildhome/SanctuaryBoard.kt` | Piece drop + `bob` dust puff, next slot `twinkle` |
| 5 | **Answer feedback** | `coredesignsystem/MaxinesAnswerCard` | Correct `pop` + check, incorrect `waggle` |
| 6 | **Today's Quest** | `featurechildhome/PlayroomHomeScreen` | Progress fills `easeOutCubic 800ms`, chevron `bouncy` on complete |
| 7 | **Lesson complete (no badge)** | `LessonCompleteScreen` | Milo claps 2×, 6-particle confetti lite |
| 8 | **Reward break gate** | `app/RewardHubScreen` | Lock → controller `TRANSFORM`, timer ring draw |

## Next step → Compose

`preview/milo.states.js` is the source of truth. The Compose mirror is `feature-rewards/MiloCelebration.kt` (scaffold in this sketch) — same path ids, same `bouncy` curve, same `0.1` idle ramp over 40% of morph. Plug into `BadgeRevealScreen` behind `animationsEnabledForScale`.

Export to SwiftUI: open Milo preview → Export panel → `MiloCelebration.swift` (same tuning drives all three targets).
