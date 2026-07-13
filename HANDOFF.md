# Maxine's World — Current State & Handoff

**Date:** 2026-07-13  
**Version:** [v0.13.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.13.0)  
**Branch:** `main` at `4a68a16`  
**Build:** `./gradlew assembleDebug` → 604 tasks clean  

---

## Quick Start

```bash
cd android
./gradlew :feature-child-home:compileDebugKotlin   # verify child home compiles
./gradlew testDebugUnitTest                           # run unit tests
./gradlew assembleDebug                               # full build → APK at app/build/outputs/apk/debug/
```

---

## Architecture

**Platform:** Android (Kotlin, Jetpack Compose, Material 3, Room, Hilt, WorkManager)  
**Target:** Xiaomi Pad 6S Pro 12.4" 3:2 (primary), phones (secondary)  
**Modules:** 19 Gradle modules in `android/` — `app`, `feature-child-home`, `core-design-system`, `core-content`, `core-database`, `core-model`, `engine-activity`, `engine-sync`, `feature-lesson-player`, `feature-parent`, etc.

---

## Village Home (v0.13.0)

### Visual Foundation
- **Master scene:** `village_home_six_landmarks_master.png` (1672×941px, 1.4MB) in `feature-child-home/drawable-nodpi/`
- **ContentScale.Crop** with **SceneViewport** coordinate mapping
- **LANDMARK_LAYOUT.json** → 6 normalized hit zones in 3×2 grid
- **Notable:** No separate `location_*.png` overlays, no Canvas scenery, no emoji

### Bamboo UI (Filipino-inspired)
- **BambooSurface composable** in `core-design-system/components/BambooSurface.kt`
- **Assembled from 7 standard PNG primitives:** fill_sawali, rail_bamboo_h/v, corner_rattan_tl/tr/bl/br
- **NO NinePatch dependency** — all .9.png files permanently removed
- **Applied to:** DestinationLabel (subjectAccent top rail + full frames), ProfileHud (subtle frame)
- **Not applied to:** QuestPill (Card onClick incompatibility), FloatingBottomNav (Surface retained)

### Endemic Animals
- **6 species** placed as decorative, non-interactive elements:
  - Philippine eagle (Story Tree), tarsier (Bahay ng Kuwento), tamaraw (Number Market)
  - Philippine colugo (Discovery Lab), Palawan peacock-pheasant (Heritage Harbor), Visayan warty pig (Kindness Corner)
- **Alpha extracted** from checkerboard reference PNGs via PIL color-distance
- **Animals in:** `feature-child-home/drawable-nodpi/animal_*.png` (256×256 RGBA)
- **Placements:** `ENDEMIC_ANIMAL_PLACEMENT.json` normalized coordinates
- **Non-interactive:** `contentDescription=null`, excluded from semantics, no click targets
- **Status:** PRODUCTION_READY (alpha-extracted) — original references are REFERENCE_ONLY

### 6-Subject Parity
| ID | Destination | Subject | Color | State |
|---|---|---|---|---|
| english | Story Tree | English | #7653B5 | Available 42% |
| filipino | Bahay ng Kuwento | Filipino | #F47C6B | Available 25% |
| mathematics | Number Market | Mathematics | #3C9DDB | **TODAY** 67% |
| science | Discovery Lab | Science | #66A83E | Available 33% |
| history | Heritage Harbor | Philippine History | #B87916 | Available 16% |
| gmrc | Kindness Corner | GMRC | #087F83 | **Locked** "Opening soon" |

### HUD / Overlays
- **Profile card:** Bamboo-framed, avatar, Level, XP bar
- **Streak/currencies:** Flame (streak), star (stars), paw coin — floating pills top-right
- **Daily Quest:** Collapsed pill with Continue button
- **Floating bottom nav:** Profile / Achievements / Backpack / Parents
- **TODAY focus glow:** Gold glow around recommended destination zone

### Responsive Layout
- **Expanded:** ≥840dp landscape → full scene + 6 floating labels
- **Compact:** <840dp or portrait → hero crop header + 2-column grid cards

---

## App Icon

- **Adaptive icon:** Maxine orange tabby face with pink glasses, backpack, necktie
- **Bamboo frame** on teal sky background
- **Layers:** foreground (PNG) + background (XML drawable) + monochrome (Android 13+)
- **Density:** hdpi through xxxhdpi + anydpi-v26/v33 adaptive XML
- **Source master:** 1024×1024 PNG

---

## Key Files for Audit

### Source to review
```
android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/VillageHomeScreen.kt  (~490 lines)
android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/components/BambooSurface.kt  (~120 lines)
android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt  (~270 lines)
android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/theme/Color.kt
android/feature-child-home/src/main/res/drawable-nodpi/*.png  (master scene + animals + bamboo primitives)
android/feature-child-home/src/main/res/drawable/*.xml  (vector icons)
android/app/src/main/res/mipmap-*/  (adaptive icon)
```

### Design specs
```
.hermes/desktop-attachments/maxines-world-bamboo-design-v1.3.md  (authoritative)
.hermes/desktop-attachments/deepseek-handoff/LANDMARK_LAYOUT.json  (hit zones)
.hermes/desktop-attachments/bamboo-v1.3/.../ENDEMIC_ANIMAL_PLACEMENT.json  (animal positions)
```

### Known limitations
- 2 left-side animals needed x-offset tuning (0.055→0.10, 0.08) due to profile HUD overlap
- Animals were alpha-extracted programmatically — a production illustrator should produce clean masters
- BambooSurface not applied to QuestPill (Card onClick param incompatibility)
- Lesson navigation routes use hardcoded IDs in MaxinesNavGraph (manifest-driven routing pending)

---

## Commit History (recent)

```
4a68a16 feat: New adaptive app icon — Maxine orange tabby with pink glasses
968b557 feat: Full design implementation — 6 endemic animals + BambooSurface
ac72260 feat: BambooSurface integrated into DestinationLabel + ProfileHud
4e83b60 feat: BambooSurface composable — standard PNG assembly, no NinePatch
0945a7f feat: Bamboo UI assets + endemic animal placement data
dcf2304 feat: DeepSeek contract — master scene, normalized hit zones, floating HUD
b8f0d9a feat: Complete UI overhaul — layered village, 6 equal destinations
f72cbf8 feat: v2 village home — backdrop image, building row, tactile buttons
```

---

## Content Server

- **DreamNAS:** 10.10.10.33:80
- **Catalog:** `/catalogs/preview.json` (62 packages, 233 lessons)
- **Sync:** ContentSyncWorker (WorkManager), SHA-256 validation, activate pipeline

---

## Environment (this development machine)

- **OS:** Windows 10 (DreamLaptopV2)
- **Repo:** `C:\maxines-world`
- **SDK:** `%LOCALAPPDATA%\Android\Sdk`
- **JDK:** Android Studio jbr (JDK 21)
- **Emulator:** Pixel C API 35, WHPX, AVD `Maxine_Tablet`
- **ADB:** `emulator-5554`
