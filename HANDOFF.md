# Maxine's World — Current State & Handoff

**Date:** 2026-07-13  
**Version:** [v0.14.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.14.0)  
**Branch:** `main` at `ab8df1e`  
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

## Village Home (v1.4 Correction)

### Visual Design
- **Style:** Hanging bamboo signposts matching the reference target board
- **7 signs:** 1 Daily Quest + 6 destination signs — each hangs from a building/tree with a top-center tab
- **No grid:** Each sign individually anchored to its building entrance
- **Village dominant:** >70% of the illustrated scene unobscured
- **No large cream cards**, no 3-column dashboard, no fillMaxHeight

### Bamboo Sign Architecture
- **BambooPlaqueSurface** in `core-design-system/components/` (176×72dp uniform)
- Assembled from 7 standard PNG primitives: sawali fill, bamboo rails (h/v), rattan corners (tl/tr/bl/br)
- **NO NinePatch dependency** — zero .9.png files
- Accent top rail colored per subject, hanging tab at top center

### Destination Anchors (normalized 0-1)
| ID | Destination | X | Y | State |
|---|---|---|---|---|
| english | Story Tree | 0.14 | 0.44 | 42% |
| filipino | Bahay ng Kuwento | 0.50 | 0.30 | 25% |
| mathematics | Number Market | 0.84 | 0.36 | **TODAY** 67% |
| science | Discovery Lab | 0.14 | 0.72 | 33% |
| history | Heritage Harbor | 0.50 | 0.72 | 16% |
| gmrc | Kindness Corner | 0.84 | 0.72 | **Locked** |
| quest | Daily Quest | 0.14 | 0.16 | Active |

### HUD
- **Profile:** Compact, 260dp max, top-left, no empty block
- **Currencies:** Tiny pills top-right (14dp icons)
- **Daily Quest:** Hanging sign from Story Tree branch (not a wide card)
- **Bottom nav:** Bamboo-framed, low-profile, 4 items

### Endemic Animals
- 6 species placed as subtle environmental discoveries
- Alpha-extracted from checkerboard references (PIL color-distance)
- Non-interactive, excluded from semantics
- POSITIONS TBD after sign placement finalization

### App Icon
- Adaptive icon: Maxine orange tabby with pink glasses, bamboo frame, teal sky
- Foreground PNG + background XML + monochrome for Android 13+

---

## Architecture

```
feature-child-home/
  VillageHomeScreen.kt       — root Scaffold + scene Box + all composables

core-design-system/
  components/BambooSurface.kt       — sawali + rails + corners (general purpose)
  components/BambooPlaqueSurface.kt — 176×72dp uniform sign (correction v1.4)
  theme/Color.kt                    — centralized color tokens

app/
  MaxinesNavGraph.kt         — navigation, subject routing
  res/mipmap-*/               — adaptive app icon (hdpi–xxxhdpi + anydpi-v26/v33)
```

---

## Key Files for Audit

```
android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/VillageHomeScreen.kt
android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/components/BambooPlaqueSurface.kt
android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/components/BambooSurface.kt
android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt
android/feature-child-home/src/main/res/drawable-nodpi/village_home_six_landmarks_master.png
android/feature-child-home/src/main/res/drawable-nodpi/animal_*.png  (6 files)
android/core-design-system/src/main/res/drawable-nodpi/fill_sawali.png
android/core-design-system/src/main/res/drawable-nodpi/rail_bamboo_*.png
android/core-design-system/src/main/res/drawable-nodpi/corner_rattan_*.png
```

---

## Commit History (recent)

```
ab8df1e feat: Hanging bamboo signs — per correction v1.4 + reference image
850c32b docs: Full HANDOFF.md update for LLM audit
4a68a16 feat: New adaptive app icon
968b557 feat: Full design implementation — 6 endemic animals + BambooSurface
ac72260 feat: BambooSurface integrated into DestinationLabel + ProfileHud
4e83b60 feat: BambooSurface composable — standard PNG assembly
0945a7f feat: Bamboo UI assets + endemic animal placement data
dcf2304 feat: DeepSeek contract — master scene, normalized hit zones, floating HUD
```

---

## Known Limitations
- Sign anchor coordinates tuned for 1280×800 reference; may need per-device calibration
- Animals alpha-extracted programmatically — production illustrator should produce clean masters
- Daily Quest uses hardcoded text; real quest data from viewmodel pending
- No compact/phone layout implemented in v1.4 correction (tablet landscape only)
- Lesson navigation uses hardcoded IDs in MaxinesNavGraph

---

## Content Server
- **DreamNAS:** 10.10.10.33:80 / catalogs/preview.json (62 pkgs, 233 lessons)
- Worker: ContentSyncWorker (SHA-256 validation, activate pipeline)
