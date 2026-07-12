# Maxine's World — Implementation Milestones & Risks

## Milestones

### M1: Project Scaffold & Build System
**Goal:** Compilable empty Android project with all modules wired.
- Gradle multi-module setup (app + 15 library modules)
- Version catalog (`libs.versions.toml`) for all dependencies
- Hilt wired across all modules
- Room database with empty schema
- Navigation Compose shell with placeholder screens
- Custom Material 3 theme (MaxineColors, MaxineTypography)
- `core-design-system` with basic composables (buttons, cards, top bars)
- CI check: `./gradlew assembleDebug` passes
- **Estimated:** 4–6 hours

### M2: Parent Login & Child Profile
**Goal:** Parent can authenticate and create/manage a child profile.
- Parent PIN setup (hashed, EncryptedSharedPreferences)
- BiometricPrompt integration (fingerprint/face)
- Child profile CRUD (Room: `ParentAccount`, `ChildProfile`)
- Profile selection screen for child launch
- Auth gate: child cannot access parent screens
- `feature-auth` module with ViewModel + Compose UI
- **Estimated:** 3–5 hours

### M3: Child Village Home
**Goal:** Child sees the village with 5 subject destinations and Daily Quest.
- Village home screen with scrollable world map (Compose Canvas + Lottie)
- Five subject destination cards: English (Story Tree), Filipino (Bahay ng Kuwento), 
  Math (Number Market), Science (Discovery Lab), History (Heritage Harbor)
- Daily Quest sidebar showing today's task, progress bar, rewards
- Child avatar/level display in top bar
- Bottom navigation: Profile, Achievements, Backpack, Parents (gate)
- `feature-child-home` module with adaptive layout (phone/tablet)
- **Estimated:** 5–7 hours

### M4: Offline Lesson Engine
**Goal:** Load lessons from JSON and render them through activity engines.
- `core-content` module: `LessonLoader` from assets + downloaded content
- JSON deserialization with Kotlin Serialization
- `feature-lesson-player`: lesson shell (progress bar, character guide, narration)
- Activity engine interface + 5 initial engines:
  - `animated_explanation` (slideshow with narration)
  - `multiple_choice` (tap-to-select with feedback)
  - `drag_and_drop` (drag items to targets)
  - `sort_and_classify` (sort into groups)
  - `sentence_builder` (tap words to build sentences)
- `ActivityResult` emission and collection
- **Estimated:** 8–12 hours

### M5: Five Pilot Lessons (External JSON)
**Goal:** One complete lesson JSON per subject, ready to load.
1. **English:** "The Cats Who Saved the Garden" (main idea, story comprehension)
2. **Filipino:** "Ang Mga Kuting na Tumulong sa Barangay" (pangunahing kaisipan, pagsasalaysay)
3. **Mathematics:** "Milo's Equal-Groups Market" (multiplication as equal groups)
4. **Science:** "Niko's Plant Investigation" (plant needs, prediction-observation)
5. **Philippine History:** "Lakan and the Lost Barangay Album" (past/present, timeline)
- Each lesson: 5–8 activity steps, assessment block, character guide, lesson JSON
- Bundled as assets in `app/src/main/assets/content/ph-matatag/grade-3/`
- Manifest JSON with all five lessons
- **Estimated:** 6–8 hours

### M6: Progress & Mastery Storage
**Goal:** Store progress events, compute mastery, drive recommendations.
- Room entities: `ProgressEvent`, `MasteryState`, `DailyQuest`
- Append-only event recording from activity results
- Mastery state machine in `engine-mastery` module
- Spaced-repetition scheduler (next review dates)
- Daily Quest rotation logic (5-subject round-robin with review prioritization)
- `feature-progress` module with progress aggregation
- **Estimated:** 5–8 hours

### M7: Parent Dashboard
**Goal:** Parent sees learning summary, can assign lessons, set screen-time.
- Weekly learning summary cards (per subject progress %)
- Skills overview (donut chart: mastered/developing/needs review)
- Daily activity bar chart
- Screen-time controls (daily limit, downtime, weekday/weekend differentiation)
- Lesson assignment UI (browse curriculum, assign to child)
- Recent activity feed
- `feature-parent` module with ViewModel + Compose
- Parent dashboard locked behind PIN/biometric
- **Estimated:** 8–12 hours

### M8: Basic Rewards System
**Goal:** Child earns stars, coins, badges, and village energy from learning.
- Star earning per correct answer + lesson completion
- Coin earning (lesson completion bonus, streak bonus)
- Badge awarding (subject-specific milestones)
- Village energy accumulation (restore/decorate locations)
- Rewards display in child profile screen
- No loss on missed days, no paid rewards, no public leaderboard
- `feature-rewards` module
- **Estimated:** 4–6 hours

### M9: Automated Tests
**Goal:** Comprehensive test suite covering all core logic.
- Unit tests: mastery computation, JSON parsing, reward eligibility
- Integration tests: Room DAOs, ContentLoader, ViewModel flows
- Compose UI tests: critical screens (login, child home, lesson player)
- Test coverage ≥70% on engine and core modules
- Tests run as `./gradlew test` and `./gradlew connectedAndroidTest`
- **Estimated:** 6–10 hours

---

## Milestone Dependency Graph

```
M1 (scaffold)
 ├─→ M2 (auth)
 │    └─→ M3 (village home)
 │         └─→ M4 (lesson engine)
 │              └─→ M5 (pilot lessons)
 │                   └─→ M6 (progress/mastery)
 │                        ├─→ M7 (parent dashboard)
 │                        └─→ M8 (rewards)
 └─→ M9 (tests) — runs alongside all milestones, final pass after M8
```

---

## Unresolved Risks

### R1: Art Asset Gap (HIGH)
**Risk:** The concept PNGs are design references, not production assets. We need vector characters (cat poses, expressions, costumes), village scene elements, icons, badges, and backgrounds.
**Mitigation:** MVP uses color-block placeholders and simple vector shapes from Compose Canvas + Material icons. Character sprites deferred to post-MVP asset production by an illustrator. Lottie placeholder animations (free "cute animal" packs) for initial build.

### R2: Narration Audio Production (HIGH)
**Risk:** Every lesson step needs narrated audio in English and Filipino. Recording, editing, and syncing this is a significant production effort.
**Mitigation:** MVP uses Android TTS (`TextToSpeech`) for narration, which works offline and is free. Recorded audio assets can replace TTS later. TTS quality in Filipino should be tested.

### R3: Filipino TTS Quality (MEDIUM)
**Risk:** Android TTS for Filipino may have limited voice quality or availability.
**Mitigation:** Test on target device early. Fallback: pre-recorded Filipino narration as MP3 assets for the pilot lesson. Google TTS engine supports Filipino on most modern Android devices.

### R4: Curriculum Validation (HIGH)
**Risk:** Lesson content must be validated by qualified Philippine educators before release. The handoff explicitly forbids inventing DepEd competency codes.
**Mitigation:** Pilot lessons use skill-based objectives without DepEd codes. Curriculum metadata fields exist in the JSON schema but are left empty or marked `educatorValidated: false`. Content CMS review workflow is designed but not staffed.

### R5: Backend Absence (MEDIUM)
**Risk:** The handoff describes a full backend (auth, content API, sync, reporting), but implementing it alongside the Android app is a parallel project.
**Mitigation:** MVP is fully offline. Content bundles ship in APK assets. "Sync" stubs log to console. Parent dashboard reads local Room data. A backend can be added incrementally.

### R6: Physical Device Testing (MEDIUM)
**Risk:** The app targets a specific child (age 8, Grade 3). Her actual device, reading level, touch precision, and engagement patterns are unknown until tested.
**Mitigation:** Build the pilot, install on target device, observe a 10-minute session. Iterate on touch target sizes, font sizes, and reward pacing based on real usage.

### R7: Build Environment on Windows (LOW)
**Risk:** The current Windows machine lacks JDK, Android SDK, and Gradle.
**Mitigation:** Install via `sdkmanager` CLI or Android Studio. JDK 17 via `winget` or manual install. Gradle wrapper included in project (no system Gradle needed).

### R8: Content Package Growth (LOW)
**Risk:** As more lessons are added, the APK size could grow large if all content is bundled.
**Mitigation:** Pilot bundles 5 lessons (~2 MB). Production would use WorkManager download-on-demand. Android App Bundle (AAB) allows Play Store to serve only needed assets.

### R9: Dual-Language Complexity (MEDIUM)
**Risk:** English and Filipino lessons share activity engines but need separate narration, text, and sometimes activity logic.
**Mitigation:** Lesson JSON carries `language` field. Activity engines are language-agnostic (consume text from JSON, not hardcoded). TTS locale selected per lesson.

---

## Total MVP Estimate
**40–65 engineering hours** for a working vertical slice with all 9 milestones.

This assumes: existing Kotlin/Compose proficiency, no backend implementation, placeholder art assets, and TTS narration.
