# Maxine's World — Current State & Handoff

**Date:** 2026-07-13  
**Version:** [v0.16.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.16.0)  
**Commit:** `02ac440` on `main`  
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

## Active Implementation: v1.6 Target UI (VillageChromeV16)

### Architecture
- **VillageChromeV16.kt** — self-contained composable (profile, quest, rewards, nav, 6 plaques)
- **ArtPanelV16** — renders custom PNG backgrounds with content overlays
- **sceneAnchor modifier** — normalized per-destination positioning (0-1 coords)
- **CompactVillageChromeV16** — LazyColumn fallback for <840dp width

### PNG Assets (5 files in drawable-nodpi)
| File | Use |
|---|---|
| `mw_profile_panel.png` | Profile card background (280×104dp) |
| `mw_quest_panel.png` | Daily Quest background (340×104dp) |
| `mw_subject_plaque.png` | Destination plaque background (196×84dp) |
| `mw_bottom_nav.png` | Bottom nav background |
| `mw_reward_chip.png` | Reward chip background |

### Destination Anchors
| ID | Destination | X | Y | Status |
|---|---|---|---|---|
| english | Story Tree | 0.17 | 0.43 | 42% |
| filipino | Bahay ng Kuwento | 0.54 | 0.45 | 21% |
| mathematics | Number Market | 0.86 | 0.43 | TODAY 67% |
| science | Discovery Lab | 0.17 | 0.68 | 17% |
| history | Heritage Harbor | 0.50 | 0.75 | 14% |
| gmrc | Kindness Corner | 0.83 | 0.75 | 18% |

### Known Issue
**PNG panel backgrounds render blank on emulator.** Content slots (profile text, quest, nav icons) load correctly but the PNG composite doesn't show. The previous v1.5 composable gradient approach (warm paper + ink outline) renders correctly.

### Previous Working Version
- **v1.5** (commit `c931add`): Storybook wayfinding with composable gradients, all 6 plaques visible, no bamboo. Roll back to this if needed.

---

## Key Files

```
feature-child-home/
  VillageChromeV16.kt           — v1.6 target UI (self-contained)
  VillageHomeScreen.kt          — v1.5 storybook (previous, still works)
  res/drawable-nodpi/mw_*.png   — 5 PNG panel backgrounds
  res/drawable/ic_*.xml         — 14 vector drawables

app/
  MaxinesNavGraph.kt            — navigation, wired to VillageChromeV16

core-design-system/
  theme/Color.kt                — centralized color tokens
```

---

## App Icon
- Adaptive: Maxine orange tabby, pink glasses, bamboo frame, teal sky
- Foreground PNG + background XML + monochrome
- Density: hdpi–xxxhdpi + anydpi-v26/v33

---

## Content Server
- DreamNAS: 10.10.10.33:80 / catalogs/preview.json (62 pkgs, 233 lessons)
- Worker: ContentSyncWorker (SHA-256 validation)
