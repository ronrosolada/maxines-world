# Maxine's World — Current State & Handoff

**Date:** 2026-07-13  
**Version:** [v0.11.0](https://github.com/ronrosolada/maxines-world/releases/tag/v0.11.0)  
**Branch:** `main`  
**Build:** `./gradlew assembleDebug` — 604 tasks, clean

---

## What's Working

| Feature | Status |
|---|---|
| Village Home v2 | ✅ Backdrop image, building row, profile/streak cards |
| Lesson loading | ✅ 100 bundled lessons, 6 subjects |
| Scoring integrity | ✅ Duplicate guard, completion verification |
| Content sync | ✅ NAS catalog, SHA-256 verify, atomic activate |
| Tablet scaling | ✅ Adaptive 1.0–2.0× factor for Xiaomi Pad 6S Pro |
| Design system | ✅ AnswerCardState enum, tactile buttons, theme tokens |
| Network | ✅ LAN security config for 10.10.10.33 |
| Parent gate | ✅ PIN authentication |
| Emulator | ✅ Pixel C @ API 35, WHPX accelerated |

## Architecture

```
android/
├── app/                          # Main app + nav graph
├── core-design-system/           # Theme, colors, components
├── core-database/                # Room DB v3 + migrations
├── core-model/                   # Month1Lesson, DayManifest
├── core-content/                 # ContentLessonLoader (bundled assets)
├── core-network/                 # ContentApi, HTTP client
├── engine-sync/                  # ContentSyncWorker (NAS sync)
├── engine-activity/              # Activity renderers (7 types)
├── feature-child-home/           # VillageHomeScreen v2
├── feature-lesson-player/        # Lesson player + ViewModel
├── feature-parent/               # Parent dashboard, PIN gate
└── feature-rewards/              # Badges, streaks, daily challenges
```

## Village Home v2 (current)

- **Layer 1:** `village_backdrop.png` full-bleed background image
- **Layer 2:** Legibility scrims (dark gradient top + bottom)
- **Layer 3:** `LazyRow` of 5 equal building PNGs with progress pills
- **Overlays:** Profile card top-left, streak pill top-right, DailyQuest banner below scene
- **Bottom nav:** My Profile, Achievements, Backpack, Parents
- **NO hamburger menu, NO version string in child UI**

## Key Design Decisions

- **Theme.kt:** Mutable `var AppDisplayFont` / `AppBodyFont` MUST stay (Xiaomi Pad 6S Pro crash)
- **Fonts:** All composables use `FontFamily.Default` for emulator compatibility
- **Colors:** All from `Color.kt` tokens — no raw `Color(0x…)` in feature code
- **Content:** JSON field names match actual bundled files exactly
- **allowBackup:** `true` (known-working Xiaomi state)
- **ProgressEventDao:** `OnConflictStrategy.ABORT` (append-only)

## Bug Fixes (3 root causes for "Could not load lesson")

1. `content` field: `JsonObject?` → `JsonElement?` (string, object, or array)
2. `AssessmentItem.options`: `List<String>` → `JsonElement?` (objects, not strings)
3. `AssessmentItem.correctOptionIds`: `List<Int>` → `List<String>` (strings, not ints)
4. `ConcurrentHashMap.getOrPut` with null value → NPE — replaced with manual check

## How to Continue

### On this machine

```bash
cd C:\maxines-world\android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.maxinesworld.app.debug/com.maxinesworld.app.MainActivity
```

### On another Hermes agent

> "Pick up Maxine's World at C:\maxines-world from GitHub main branch. Read HANDOFF.md first. Build with `gradlew assembleDebug`. Theme.kt must NOT be refactored (mutable vars). Current priority: building tap targets on emulator, parent gate responsive layout, and NAS sync end-to-end test on tablet."

### Emulator

```bash
# AVD: Maxine_Tablet (Pixel C, API 35, WHPX)
# Launch: emulator -avd Maxine_Tablet
# ADB: emulator-5554
```

## Remaining Work

- [ ] Building tap zones: verify touches reach buildings on emulator (DailyQuest banner may overlap)
- [ ] GMRC subject: Kindness Corner PNG exists but not in building row (waiting for content)
- [ ] Parent dashboard: real Room data (currently sample)
- [ ] Tactile button depress animation (MaxinesPrimaryButton has chunky shadow, needs press state)
- [ ] NAS sync: end-to-end test on tablet connected to 10.10.10.33
- [ ] BiometricPrompt for parent gate
- [ ] 200% font scale testing
- [ ] TalkBack accessibility pass
- [ ] Filipino sentence-builder tokens validation
- [ ] gradlew.bat for Windows developers

## Design Assets Location

```
android/docs/design/
├── design.md                     # Normative design spec
├── design-deviations.md          # 6 intentional deviations
├── reference/                    # 6 subject concept images
└── asset-metadata.json           # SHA-256 hashes
```
