# Maxine's World — Current State & Handoff

**Date:** 2026-07-13  
**Version:** [v0.15.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.15.0)  
**Commit:** `c931add` on `main`  
**Build:** `./gradlew assembleDebug` → 604 tasks clean  

---

## Quick Start

```bash
cd android
./gradlew :feature-child-home:compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleDebug  # APK → app/build/outputs/apk/debug/
```

---

## Village Home (v1.5 Storybook Wayfinding)

### Visual
- **6 uniform 184×72dp storybook plaques** — warm paper gradient, 1dp ink outline, 6dp accent marker, 24dp subject icon
- **NO bamboo rails, rattan corners, hanging tabs, or NinePatch** — all deleted per DESIGN_SPEC.txt
- **Village ≥75% unobscured** — plaques individually anchored beside building entrances
- Profile: 270×90dp, Daily Quest: 380×84dp, Reward chips: 44dp

### Surface Tokens
- Paper top: #FFF8E8, Paper bottom: #F4E2BE, Ink: #183B4A
- Outline: #365662 at ~65% opacity, Shadow: Ink at ~15-18%

### 6 Subjects
| ID | Destination | Subject | Color | Anchor | Progress |
|---|---|---|---|---|---|
| english | Story Tree | English | #7653B5 | 0.15, 0.47 | 42% |
| filipino | Bahay ng Kuwento | Filipino | #F47C6B | 0.50, 0.42 | 25% |
| mathematics | Number Market | Mathematics | #3C9DDB | 0.84, 0.40 | **TODAY** 67% |
| science | Discovery Lab | Science | #66A83E | 0.15, 0.73 | 33% |
| history | Heritage Harbor | Philippine History | #B87916 | 0.50, 0.72 | 16% |
| gmrc | Kindness Corner | GMRC | #087F83 | 0.84, 0.72 | **Locked** |

### Components
- `StorybookPanel` — shared surface (paper gradient + shadow + outline)
- `DestinationPlaque` — one semantic button per destination
- `PlayerProfilePanel`, `DailyQuestPanel`, `RewardRail`, `RewardChip`
- `StorybookBottomNavigation` — low-profile NavigationBar
- `defaultVillageDestinations()` — canonical data list

### Architecture
```
feature-child-home/
  VillageHomeScreen.kt          — root Scaffold + all composables (416 lines)
  res/drawable/ic_*.xml         — 14 vector drawables (subjects, nav, rewards)

app/
  MaxinesNavGraph.kt            — navigation, wire callbacks
  res/mipmap-*/                 — adaptive app icon
```

### Deleted (v1.5 cleanup)
- `BambooPlaqueSurface.kt`, `BambooSurface.kt` — composables
- `fill_sawali.png`, `rail_bamboo_*.png`, `corner_rattan_*.png` — 9 assets
- All `.9.png` bamboo files — zero remaining

---

## App Icon
- Adaptive: Maxine orange tabby, pink glasses, bamboo frame, teal sky
- Foreground PNG + background XML + monochrome (Android 13+)
- Density: hdpi–xxxhdpi + anydpi-v26/v33

---

## Known Limitations
- Anchor coordinates tuned for 1280×800; per-device calibration pending
- Daily Quest uses hardcoded text; real data from viewmodel pending
- Compact/phone layout not implemented (tablet landscape only)
- Lesson navigation uses hardcoded IDs in MaxinesNavGraph
- Endemic animal PNGs exist but not wired in v1.5 composition
