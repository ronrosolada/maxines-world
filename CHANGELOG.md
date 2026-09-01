# Changelog

All notable changes to Maxine's World. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the
Android `versionName`.

## Unreleased

### Child-facing Grade 3 video library

- Child surfaces (video library, daily quests, home progress, parent prefetch) now show only Grade 3 `RELEASED` videos. Grade 1/2/4 and `PREVIEW` catalog rows stay on the LAN for later review and are not presented as core curriculum.
- `licenseStatus=PERSONAL_USE` is unchanged: household LAN-only media, not a public-distribution license.

### One 5-minute playground session

- Completing Today's mission still unlocks the playground, but the break is one 5-minute session. Leaving and returning can use remaining time; the session is not re-armed for the rest of the day.
- Home copy says "one 5-minute play break". After the break is consumed, the quest card returns to Open Sanctuary.
- The child mini-game shelf is now an 8-year-old allowlist. Wordle, Sudoku, Solitaire, FreeCell, Checkers, Reversi, Mancala, Yahtzee, and Domino stay bundled but are hidden from the child route.

## [0.64.0] - 2026-08-22

### Resilient Daily Mission Fallback & Arena Audio Read-Aloud

- **Resilient Daily Mission Fallback:** When the LAN video catalog is unreachable or unindexed, the Daily Mission automatically falls back to Grade 3 Assessment Arena challenge packs, eliminating dead-end mission states.
- **Assessment Arena Audio Read-Aloud:** Added a child-friendly "🔊 Listen" action on Assessment Arena questions with native offline Text-to-Speech (supporting both `en-US` and `fil-PH`), reading questions and answer options for emerging readers.
- **Automated Verification:** Added test coverage for offline fallback in `DailyQuestManagerTest` ensuring zero-interruption daily mission generation.

## [0.63.0] - 2026-08-22

### Video-Based Daily Mission

- Replaced the child-facing Daily Mission’s legacy lesson assignments with one active Grade 3 released video plus up to two unpassed Grade 3 Assessment Arena packs.
- Added video titles, durations, quiz indicators, subject-aware video navigation, pack-aware Arena navigation, passed-video/Arena completion, and retryable unavailable states.
- Added deterministic fallback to additional frontier videos when Arena packs are exhausted, while preserving the 30-minute sticker ledger policy.
- Preserved the existing sanctuary-piece, five-minute break, and playground day-pass reward transaction.
- Removed orphaned child-facing subject/module routes and screens while retaining Quiz Arena, Assessment Arena, lesson content assets, loaders, and renderers for compatibility.
- Added recovery coverage for legacy persisted quest rows, catalog outages, stale/inactive media, reward-flow updates, and prevention of legacy lesson completions from satisfying missions.

## [0.62.0] - 2026-08-21

### Child Home Clarity and Learning Streak

- Reorganized the child home into focused, maintainable UI components without changing navigation behavior.
- Consolidated Today’s Quest presentation around one clear child-facing action.
- Added concrete video progress counts, readable lock explanations, selected navigation semantics, and whole-card subject destinations.
- Added shared, timezone-aware learning-day streak calculation for parent and child surfaces.
- Added profile-safe child streak data with local-midnight rollover handling and an informational child-visible streak card.
- Kept the release video-first: legacy lesson content and renderers were not changed.

## [0.58.0] - 2026-08-21

### Milo Celebration & Delight System

- **Hero sticker reveal:** Milo now celebrates inside `BadgeRevealScreen`
  as a Canvas state-machine mirroring `sketches/milo-reward-proto/milo.states.js`
  (`idle → celebrate` with bouncy `easeOutBack 800ms`, `breathe-y` + `sway` idle,
  6 orbiting sparkles and lightweight confetti), gated by `LocalAnimationsDisabled`
  / `prefers-reduced-motion`. The prior orbiting-star ring is replaced by
  `MiloCelebration(badgeBiomeColor, isMilestone)` plus a bouncy sticker token.
- **Design-system motion tokens:** `DelightMotion` in `core-design-system`
  (`EaseOutBack`, `EaseOutCubic`, `BouncyMs`, `QuickPopMs`) shared by Milo,
  answer feedback, and sanctuary so the SVG preview curve drives Compose.
- **Lesson answer feedback:** `MaxinesAnswerCard` now pops on `CORRECT`
  (`0.97 → 1.06 → 1.0` spring) and waggles on `INCORRECT` (420ms keyframe shimmy),
  snapped when reduced-motion is enabled.
- **Sanctuary scene:** `SanctuaryScene` Milo bob/bounce and the next-piece
  preview now respect reduced-motion; the next slot twinkles (`0.92 → 1.0`,
  `1100ms EaseInOutSine`).
- **Prototyping harness:** `sketches/milo-reward-proto/` with the full
  `svg-character-animator` preview (4-state Milo, drag bezier curve, duration,
  idle-anchor, X-ray, background swatches, PNG/Swift/Web exports) and
  `delight-lab.html` (8 moments interactive, zero-build, `prefers-reduced-motion` gated).
- **Skills:** `svg-character-animator` per-path `TRANSFORM` + preview harness.

### Content Fixes

- Deduped two lesson-title collisions (`Telling Sentences ×2 → · Q1 W01 D05 / · Q2 W04 D02`;
  `Munting Talata ×2 → · Q1 W03 D01 / · Q1 W03 D03`) so the title-dedupe check is clean.
- Corrected `english-g3-q1-w01-d05` assessment shape from `5/4` to the baseline-expected
  `6` items / `5` to pass, adding the missing sixth telling-sentence check
  (`We walk home.`) so strict content validation reports `0 errors, 0 warnings`.

## [0.57.0] - 2026-08-20

### Playlist Replacement and Release Hardening

- Replaced the in-app video lesson catalog with the verified 237-video workbook selection across Filipino, Makabansa, Mathematics, English, GMRC, and Science.
- Enforced subject and grade filtering, topic ordering, episode ordering, visible video titles, LAN media playback, offline downloads, and catalog-backed assessment metadata.
- Added five subject-appropriate assessment questions per video with the existing watch plus 4/5 pass-and-reward flow.
- Added the re-enterable playground day-pass after Daily Quest completion, scoped by child and local calendar day.
- Kept release gates strict for educator approval metadata and the 29 bundled offline mini-games.
- Updated release documentation, media contracts, rollback notes, and the connected-test fixture for the playground unlock receipt DAO.

### Playground Day-Pass — Re-Enterable Reward Break


- **Playground unlocked for the rest of the day after Daily Quest (3/3):** completing the Daily Quest now mints an idempotent `playground_unlock_receipts {childId:dayKey}` day-pass (`DailyQuestRewardWriter`) alongside the existing `SANCTUARY_PIECE` + 5-minute break. The receipt is keyed by `"$childId:$dayKey"` with first-write-wins `insertIgnoring`, so process death or retries cannot double-mint.
- **Re-enterable until midnight (local):** `RewardBreakViewModel` checks `isPlaygroundUnlockedToday()` on `load()`/`begin()`/`saveResult()`/`consume()`. When the day-pass exists, each hub entry re-arms to a fresh `5 min` window via atomic `RewardBreakDao.reactivateForDayPass()` (resets `state=ACTIVE`, `startedAt=now`, `remaining=5 min`, clears `consumedAt`). `consume()` no longer sets `CONSUMED` while the pass holds — shows `Playground is open — come back anytime today!` instead.
- **Playroom home:** `PlayroomHomeViewModel` now observes `PlaygroundUnlockReceiptDao.observeByChildAndDay()` and flips the Daily Quest card from `OpenSanctuary / ViewReward` to `Open Playground / OpenPlayground` when the day-pass exists (`QuestUi.playgroundUnlocked`).
- **Midnight expiry:** `dayKey = LocalDate.now(ZoneId.systemDefault()).toString()` — a new day requires a new 3/3 quest. No cleanup job.

## [0.52.0] - 2026-08-19

### Unslop Review & Design System Purification
- **Code Hardening & Swallowed Error Remediation:**
  - Patched `MediaLibrary.kt` to explicitly rethrow coroutine `CancellationException` and prevent silent disk catalog corruption.
  - Hardened Python validator tools (`repair_educator_findings.py`, `packager_validator.py`) with explicit exception types.
  - Stripped leftover LLM narrative artifacts from `MatchingPairsRenderer.kt` and `make_avatar.py`.
- **UI & Brand Color Unification:**
  - Removed AI-default Tailwind violet/indigo palettes (`#7c3aed`, `#6d28d9`, `#7e22ce`) across all 29 embedded HTML5 mini-game assets.
  - Unified color styles to Maxine's Warm Village palette (`VillageTeal`, `Coral`, `HeritageGold`, `Cream`).
  - Eradicated unprompted neon glow drop shadows and gradient heading texts.
- **Documentation Register Alignment:**
  - Unslopped repository documentation, converting corporate em-dash ticks and AI listicle scaffolding into clean technical specifications.

## [0.48.0] - 2026-08-17

### Home Screen Layout Alignment & Hierarchy Unification

- **Removed Redundant Video Lessons Shortcut Card:**
  - Removed top-level `VideoLibraryCard` / `WatchToEarnQuestCard` from Playroom home screen, as curriculum video lessons are directly integrated and accessed within their respective subject modules.
- **Full-Width Quick Bits Hero Placement:**
  - Converted `QuickBitsHomeCard` into a clean, full-width feature banner styled consistently with the `AssessmentArenaBannerCard` (matching 20dp radii, padded layout, 48dp leading icon container, and action pill button).
- **Aligned Rhythm & Grid Consistency:**
  - Unified vertical layout hierarchy: Today's Quest -> Assessment Arena -> Quick Bits -> 6-Subject Grid.

## [0.47.0] - 2026-08-17

### Tactile Micro-Interactions & Animation Polish

- **Spring-Physics Tactile Feedback (`emilkowalski-motion`):**
  - Added physical spring compression feedback (`scale 0.96f` on press) across `QuickBitsHomeCard`, `VideoLibraryCard`, and interactive Assessment choices.
- **Milo Idle Breathing Animation & Bounce:**
  - Added continuous sine-wave idle breathing motion (`±3dp` bobbing) to Milo in `SanctuaryScene`, plus a spring bounce animation on tap.
- **Quick Bits Overlay Stagger:**
  - Added entrance slide-and-fade animation on video info overlays when scrolling between shorts in `QuickBitsScreen`.
- **Assessment Arena Responsive 2-Column Grid:**
  - Upgraded Assessment Arena multiple-choice options on tablet displays to an ergonomic 2-column grid layout with per-item press physics.

## [0.46.0] - 2026-08-16

### Quick Bits: TikTok-Style Educational Shorts Feed & Offline Engine

- **TikTok-Style Vertical Shorts Pager:**
  - Full-screen `VerticalPager` with smooth snapping gestures (swipe up for next short, swipe down for previous).
  - Dedicated `ExoPlayer` instance per page with auto-play, looping, tap-to-pause with animated indicator, and strict memory cleanup on page transition.
  - High-contrast bottom overlays displaying educational short title, creator handle, runtime badge, and category tag.
  - Tactile top category filter bar (*All Shorts*, *🐾 Animals*, *🚀 Space*, *🔬 Science*, *🔢 Math*).
- **Curated 60-Video Educational Catalog (480p H.264+AAC):**
  - Exactly 60 curated, age-appropriate (8–10 years old) educational shorts from trusted channels (Nat Geo Kids, SciShow Kids, NASA Space Place, TED-Ed, BBC Earth Kids, Numberblocks).
  - Balanced distribution across 4 core domains: 15 Animals, 15 Space, 15 Science, 15 Math (~598 MB total).
  - Hosted and streamed locally from the Caddy content server (`10.10.10.33` / `10.10.20.33`).
- **Resilient 1-Tap Offline Download Engine:**
  - Bulk "Download All" engine with throttled concurrent downloading (`MediaDownloader`), atomic file transfers, and resumable state.
  - Interactive Offline Storage Manager modal showing live downloaded bytes, total MB count, and single-tap cache clearing.
  - Seamless automatic fallback: plays directly from local storage if downloaded, streams from local Caddy server if connected.
- **Playroom Home Launcher Card:**
  - Integrated dedicated "Quick Bits" launcher card directly on the child Playroom home feed alongside Video Lessons.
  - Displays dynamic "60 Videos" badge and category summary.

## [0.45.9] - 2026-08-16

### Gamified & Interactive Milo's Wildlife Sanctuary

- **Interactive Habitat Cards & Touch Affordances:**
  - Upgraded all 12 habitat tiles on the sanctuary board and panoramic scene to interactive touch surfaces with 56dp+ touch targets.
  - Tapping any earned habitat (e.g. *Little Pond*, *Story Tree*, *Kindness Garden*) opens an interactive inspection dialog with high-res habitat art, unlock status, and Milo's Field Notes.
- **Philippine Wildlife Resident Connections:**
  - Linked native animals from the 51 Wildlife Collectible badges (e.g. *Tamaraw*, *Sinarapan*, *Philippine Tarsier*, *Cebu Flowerpecker*, *Palawan Pangolin*, *Luzon Peacock Swallowtail*) directly to their corresponding natural habitats in `SanctuaryCatalog`.
  - Added educational fun facts highlighting Philippine biodiversity conservation.
- **Milo Character Animation & Encouraging Voice Quotes:**
  - Tapping Milo inside the sanctuary triggers a playful bounce animation and displays rotating bilingual encouraging voice quotes (*"Salamat sa pag-aaral! Milo loves our sanctuary!"*, *"Great job today! Every lesson helps our wildlife friends!"*).
- **Parent Auth Lockout Stability:**
  - Fixed automated PIN login transition for fresh profiles and resolved auto-verification test race conditions in `ParentAuthViewModel`.

## [0.41.0] - 2026-08-15

### Complete 59-Video Tagalog Media Curriculum & Assessment Bank

- **Full 59 Tagalog Video Assessments:** Authored and integrated 330 new child-facing interactive questions covering Videos 27 through 59:
  - *Kids Tagalog 27–33:* Siblings & family titles, polite greetings, opposites (mainit/malamig), vehicles & car parts, insects, descriptive adjectives, scents & hygiene.
  - *Kids Tagalog 34–39:* New Year time words, emotional expressions (masaya/malungkot), polite expressions, plant anatomy, greetings (Kamusta ka), extended family.
  - *Kids Tagalog 40–49:* Handwashing hygiene, speed concepts (mabilis/mabagal), mealtime, lost & found storybook, self-introductions, fruits & vegetables, spatial directions, prepositions of location, expressions of preference (gusto/ayoko), respectful speech (po at opo).
  - *Kids Tagalog 50–59:* School supplies, birthday celebration songs, traditional Filipino games (tagu-taguan, sungka, piko), body parts action song, Christmas traditions (parol, bibingka, simbang gabi), farm animals & animal sounds, geometric shapes, color mixing, and counting numbers 1 to 20.
- **100% Video-to-Assessment Parity:** All 59 staged MP4 video lessons hosted on the DreamNAS content server now have full 10-question multiple-choice interactive quiz banks in `media-assessments.json` (590 verified questions total).

## [0.40.0] - 2026-08-15

### Adversarial UX & Touch Ergonomics Overhaul

- **Sort & Classify Renderer Overhaul:**
  - **Bucket Highlight & Glow:** Added high-contrast `VillageTeal` border pulse (2.dp) and background tint to active category buckets so children have clear visual feedback when a card is selected.
  - **Tactile Touch Targets:** Upgraded category bucket touch targets to minimum 56dp height and card items to 48dp height to accommodate 8-year-old child ergonomics.
  - **Educational Retry Guidance:** Populated inline educational feedback (`feedback.incorrect`) in `ErrorRed` on incorrect attempts so learners understand why cards belong in specific categories rather than blindly guessing.
  - **Clear Progress Indicators:** Embedded visual counter copy (*"Place X more cards to check"*) to prevent premature submissions.
- **Cognitive Load & Narration Text Cap:**
  - Enforced a strict 3-line ceiling with `TextOverflow.Ellipsis` on narration instruction cards in `LessonPlayerScreen.kt` to eliminate reading fatigue for early learners.
- **Parent Gate & Caregiver Accessibility:**
  - Softened intimidating lock alert into a calm pause prompt (*"Please take a short pause"* in warm gold/teal).
  - Prominently surfaced the adult mental math bypass button (*"Bypass lockout with quick math question"*) with a 48dp touch surface so non-technical grandparents/caregivers are never stranded during PIN lockouts.
- **Strict Verification:** Passed all curriculum validation, asset render, and content quality audit gates with 0 errors and 0 warnings.

## [0.39.0] - 2026-08-15

### Child-First Startup & Auth Fix

- **Removed Startup PIN Gate:** Fixed startup flow so the app directly navigates children into their playroom (`ChildSelectScreen` or active `ChildHomeScreen`) on startup without prompting for the parent PIN.
- **Parent Access Scoped Appropriately:** The parent PIN remains securely enforced only when entering the **Parent Dashboard** / **Parent Gate** (`ParentGateScreen`), ensuring children can learn immediately with zero friction.

## [0.38.0] - 2026-08-15

### Milo's Wildlife Sanctuary Visual Overhaul

- **High-Fidelity Sanctuary Backdrop:** Replaced the flat green gradient background with a stunning panoramic storybook landscape (`sanctuary_backdrop.webp`) featuring rolling emerald Palawan hills, winding nature paths, and sunlit mountain slopes.
- **12 Illustrated 3D Storybook Piece Tokens:** Replaced generic system vector icons (Parks/Pets) with 12 bespoke, isometric game tokens rendered via ComfyUI (`dreamshaper_8`) on RTX 3070:
  - `sanctuary_piece_meadow.webp` (Sunny Meadow with blooming wildflowers)
  - `sanctuary_piece_pond.webp` (Sparkling Little Pond with floating lilies)
  - `sanctuary_piece_tree.webp` (Ancient leafy Story Tree with sunbeams)
  - `sanctuary_piece_nest.webp` (Cozy woven twig Bird Nest)
  - `sanctuary_piece_garden.webp` (Vibrant Kindness Garden with wooden fence)
  - `sanctuary_piece_path.webp` (Curving cobblestone Forest Path)
  - `sanctuary_piece_shelter.webp` (Thatched wooden Animal Shelter cottage)
  - `sanctuary_piece_butterfly.webp` (Dancing tropical Butterfly Corner)
  - `sanctuary_piece_lookout.webp` (Treehouse Canopy Lookout tower)
  - `sanctuary_piece_reading_nest.webp` (Leafy outdoor Reading Nest bench)
  - `sanctuary_piece_flower_bed.webp` (Blooming tropical Flower Bed)
  - `sanctuary_piece_wildlife_sign.webp` (Carved rustic Wildlife Sanctuary Signpost)
- **SanctuaryScene Component Integration:** Updated `SanctuaryScene.kt` to dynamically bind the new rendered WebP drawables for earned pieces, locked slots, and next-piece unlocks with crisp alpha clipping.

## [0.37.0] - 2026-08-15

### Corpus-Wide AI Storybook Asset Upgrade

- **All Lesson Visuals Upgraded:** Replaced all legacy geometric placeholder SVGs with bespoke 2D storybook scenes generated via ComfyUI (`dreamshaper_8`) on local NVIDIA RTX 3070 GPU.
- **Subject-Specific Artistic Direction:**
  - **Science (45 lessons):** Vibrant laboratory exploration tables, Philippine biomes (Palawan reefs, volcanic rocks, weather stations).
  - **Mathematics (58 lessons):** Colorful counting abacuses, geometric pattern workshops, market arithmetic scenes.
  - **English (93 lessons):** Whimsical reading gardens, alphabet bridges, storybook character scenes.
  - **Filipino (92 lessons):** Traditional Filipino cultural settings, festive fiestas, bahay kubo, and community stories.
  - **Araling Panlipunan (20 lessons):** Historical Philippine landmark explorations, regional map detective rooms.
  - **Makabansa (26 lessons):** Indigenous Philippine flora/fauna biomes and cultural heritage landscapes.
  - **GMRC (24 lessons):** Heartwarming family homes, school playgrounds, cooperative sharing activities.
- **Performance & Rendering:** Compressed into high-density lossless WebP assets embedded directly inside valid SVG wrappers with clean viewBox definitions (zero APK bloat, instant rendering on devices).
- **Validation Gates:** all assets verified, 0 errors, 0 warnings, 0 duplicate clusters, 0 missing assets.

## [0.36.0] - 2026-08-15

### Visual & Pedagogical Asset Enrichment

- **Badge Photo Coverage (100% Complete):** Generated and integrated the missing Panay monitor lizard (`animal_photo_reptile_panay_monitor.webp`) using ComfyUI on local RTX 3070 diffusion pipeline, achieving 100% asset completeness across the entire 51-badge Philippine fauna curriculum catalog.
- **Impeccable Storybook Art & Audio Integration:** Aligned lesson asset workflows with tactile Philippine biodiversity biomes, clean vector outlines, high-contrast child UX, and zero-emoji standards.
- **Curriculum & Asset Validation:** Verified the full lesson pack across 7 subjects with 0 errors and 0 warnings on strict content validation and asset resolution gates.

### Verification

- Content pack validation: full lesson pack, 0 errors, 0 warnings.
- Content quality audit: 0 errors across 7 subjects (Araling Panlipunan, English, Filipino, GMRC, Makabansa, Mathematics, Science).
- Lesson asset verification: 358/358 valid SVGs, 0 missing, 0 orphaned.
- Badge Catalog & Photos: 51/51 badges fully resolved (49 animal photo assets + milestone badge).

## [0.35.1] - 2026-08-15

### Corrective Content and Parent Access Patch

- **Parent access:** initialize the requested fixed default PIN into salted DataStore state on fresh install and after reset; fresh installs no longer enter PIN setup, and existing child profiles retain the parent gate.
- **English visuals:** replaced the remaining generic English focus-board SVGs with lesson-specific visuals; the Fiesta Picture now visibly contains its eight requested hotspots.
- **Content integrity:** fixed duplicate matching labels, Filipino password-language bleed, and a Makabansa assessment answer-key mismatch.
- **Regression coverage:** added checks for generic visual shells, required fiesta scene groups, unique matching labels, Filipino language purity, and fresh-install parent initialization.
- **Audit transparency:** documented the prior review-gate gap: structural asset/render checks did not verify visual-topic semantics, and the stronger educational audit was not included in CI.

### Verification

- Python tooling discovery: 120 tests passed.
- Strict content validation: full lesson pack, 0 errors, 0 warnings.
- Rendered lesson assets: 358/358 valid; missing, orphaned, malformed, and render-failure counts: 0. The optional `visualScene` payload remains absent from 128 activities, but each has a valid lesson-specific SVG asset.
- Generic focus-board visuals: 0 remaining.
- Android auth/parent unit tests and debug assembly passed.
- Emulator clean-install smoke test: Create Child Profile on first run; requested default PIN opens Parent Dashboard.
- The stronger educational audit is intentionally not called clean: 56 repeated assessment-prompt groups and 180 overlong learner-facing strings remain documented follow-up work.

## [0.35.0] - 2026-08-15

### Educator Effectiveness, Content Integrity, and Visual Quality

- **Corpus-wide educator remediation**: re-authored and validated all 358 playable lessons, with one documented disposition per lesson and no blocked lessons.
- **Assessment integrity**: corrected source-alignment, answer-key, duplicate-option, numeric, and scenario-wording defects across Math, Science, and Filipino content; the full Python tooling suite now passes 115 tests.
- **Visual craft floor**: regenerated the 358-asset SVG corpus to remove emoji-as-icon placeholders, preserve bespoke artwork boundaries, and retain accessible title/description metadata.
- **Release documentation**: added the educator-effectiveness review report and updated the handoff with the validation evidence and scope caveats.

### Verification

- Strict content validation: full lesson pack, 0 errors, 0 warnings.
- Content quality audit: 0 findings; assessment duplicate groups: 0; similarity pairs: 0; objective pacing violations: 0.
- Lesson assets: 358/358 valid; missing, orphaned, malformed, and render-failure counts: 0.
- Python tooling discovery: 115 tests passed.
- Android release gate: educator approval verification, core-content tests, app unit tests, lint, debug assembly, and signed release assembly passed.
- Release APK: application ID `com.maxinesworld.app`, version code `37`, version name `0.35.0`, APK Signature Scheme v2 verified.

## [0.34.0] - 2026-08-15

### Educator Review, Writing, and Playful Progress

- **Lesson pacing reauthored**: differentiated 168 over-repeated objective instances so each lesson has one clear focus while preserving deliberate spiral review.
- **Child-facing copy cleaned**: removed leaked author metadata, repaired grammar defects, diversified repeated titles, and kept Filipino and English feedback language-matched.
- **Writing production pilot**: added offline sentence-building activities with deterministic word tiles, self-check lists, accessible semantics, and a guided retry path instead of free-text grading.
- **Quest reliability**: hardened deterministic daily-quest selection against the `Int.MIN_VALUE` hash edge case.
- **Sanctuary completion**: stopped finite sanctuary rewards from wrapping into duplicate pieces; completed homes now continue to grant the play break without promising another piece.
- **Wildlife photos**: added five verified real-life badge photos from the reviewed asset set. The Panay monitor remains on an honest photo-coming-soon fallback because the available Commons candidate was an illustration, not a photograph.
- **CI pacing gate**: added objective fan-out validation to prevent future lesson pacing regressions.

### Verification

- Full lesson pack: strict content validation, 0 errors, 0 warnings.
- 331 distinct objectives; 0 objective groups over the three-lesson fan-out target.
- 0 duplicate lesson title groups; 0 malformed or failed lesson visuals.
- 0 content-quality findings; 0 generic retry-feedback findings.
- Targeted Android unit tests pass for engine activity, child home, rewards, content, and lesson player.
- Release educator gate: 358/358 playable lessons approved.
- Offline mini-game gate: 29/29 bundled games pass CSP and network-isolation checks.


## [0.33.0] - 2026-08-14

### Educator Review & Curriculum Quality

- **Deep educator audit, zero findings**: every lesson now passes the full content quality, validation, similarity, title-dedupe, and asset gates with 0 findings (down from 552) — all clean.
- **Assessment integrity**: fixed the real-schema answer-position bias (`correctOptionIds`) — correct answers were at position `a` 41.6% of the time; now 27.3/26.6/25.7/20.4% across a–d (1,793 items re-shuffled deterministically).
- **Matching-pair re-authoring**: repaired 46 identity/duplicate pair sets and 75 clue-repeating pairs across Araling Panlipunan, Filipino, Science, Math, English, and GMRC; every pair now has a distinct, meaningful match.
- **Cross-lesson contamination removed**: 434+ feedback strings and explanations that referenced a *different* lesson's title were rewritten to point at their own lesson (e.g. "Find the clue in Five Sense Helpers again" instead of a wrong lesson).
- **Dull feedback replaced**: 60+ generic "Nice work. Continue to the next step." confirmations replaced with celebratory, language-matched feedback ("Hooray! Fantastic job, Maxine! 🎉⭐" / "Yehey! Napakagaling mo, Maxine! 🎉⭐").
- **Duplicate prompts diversified**: 455+ reused assessment prompts now carry child-friendly variants or lesson-specific context so no question is memorizable across lessons.
- **Language purity**: 27+ English-word bleeds in Filipino lessons fixed (legend→legenda, screen→iskrin, community helper→katuwang sa pamayanan, atbp.).
- **Learner text length**: all instructions and feedback strings ≤90 chars, preserving the pedagogical guidance (e.g. place-value hints kept, shortened).
- **Generic instructions**: 120 stock phrases replaced with activity-specific directions.
- **Visuals**: all 358 activity SVGs regenerated with the master Milo launcher icon anchor (orange tabby, pink glasses, green eyes) and subject-themed boards.
- **Educator approval**: every lesson re-stamped `educatorValidated=true` + `contentReview` provenance (2026-08-14 pass) after the review.

### Content distribution

- Content server catalog republished at `http://10.10.10.33/catalog.json` — 152 packages, all `v1.3.0`, stale versions purged.

### Testing & Verification

- All six repo gates pass with 0 findings (see above).
- `run_quality_gates.py` (engine): title uniqueness, 0 generic phrases, MCQ balance across 1,793 items, all PASSED.

## [0.32.0] - 2026-08-14

### Highlights & Pedagogical Integrity

- **Corpus-Wide Educator Content Review**: Completed full pedagogical pass across the full lesson pack; resolved duplicate assessment items, rotated answer key positions, diversified mascot openers, and eliminated objective-pasting anti-patterns.
- **Sort & Classify Unplace & Re-Sort Flow**: Learners can now tap any placed card before submission to unplace it back to the tray or move it to a different category bucket without needing to submit and retry.
- **Safe, Non-Destructive Parent PIN Recovery**: Added COPPA-compliant guardian verification challenge (`14 × 8`, `17 × 7` mental math gate) allowing guardians who forget their 6-digit PIN to reset it safely without wiping child profiles, learning completions, or earned stickers.
- **Reassuring, Bilingual PIN Lockout Display**: Replaced raw lockout error text with a live countdown timer card in English and Filipino (*"Kusang magbubukas pagkatapos ng Xs"*).

### Testing & Verification

- The full lesson pack passes the content quality, validation, and similarity gates with 0 errors.
- 101/101 Python content test suites passed.
- 87/87 connected Android instrumentation tests passed on API 35 emulator.


## [0.31.1] - 2026-08-13

### Fix

- **Sanctuary complete state**: the earn hint ("Finish all 3 lessons in
  Today's Quest to add this place.") no longer renders once the sanctuary is
  complete (12/12 places, including parent god mode), where it contradicted
  the "Milo's home is complete!" line directly above it. UI test added
  (`completeSanctuaryShowsCompletionWithoutAnEarnHint`).

## [0.31.0] - 2026-08-13

### Quality, hygiene, and trust

- **Fail-closed rendering**: unknown activity types are dropped with a log
  instead of silently rendering as an animated explanation; a lesson with no
  playable steps fails to load instead of opening an empty player. Unit
  tests cover both behaviors.
- **CI connected-test coverage**: `feature-child-home`, `feature-rewards`,
  and `feature-lesson-player` instrumented suites now run on the API 34
  emulator (previously only `core-database`, `app`, and `feature-auth`).
- **Dead code removed**: pre-Playroom screens (`VillageHomeScreen`,
  `VillageHomeV17`, `VillageChromeV16`) and their unreferenced assets
  deleted. Mascot + subject artwork converted PNG → WebP (≈9.7 MB smaller
  APK).
- **Content normalization**: 9 Filipino story lessons re-serialized with the
  canonical top-level key order (values unchanged, gate verified).
- **Dependency refresh**: AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, KSP
  2.1.20-1.0.32, Room 2.7.1. Room 2.7's stricter SQL parser required
  renaming the `lesson_completions` query alias from the reserved word
  `current`. Compose BOM stays on 2024.12.01: the 1.8 line changes IME
  inset propagation, which broke the PIN keypad UI test (`AuthImeLayoutTest`)
  under injected insets — a Compose bump must be validated on a physical
  device first.
- **Educator follow-ups tracked**: M1, M2, M7, and the 122 same-keyed-pair
  finding are now GitHub issues (#76–#79).
- **Docs**: HANDOFF.md reconciled to the current baseline (DB v9, SVG
  accessibility status, issue links).

## [0.30.0] - 2026-08-11

### Playroom — one clear starting point

- Removed the redundant homepage `Start here!` subject badge and the
  duplicate `Start your first adventure` resume card.
- The Daily Quest now owns the single explicit start action (`Start quest`;
  `Continue quest` after progress), reducing first-screen choice overload for
  children. Subject cards remain available as direct navigation.

## [0.29.0] - 2026-08-11

### Playroom — Milo's sanctuary becomes a place you can see

- The home sanctuary card now renders a living meadow scene: every earned
  sanctuary piece appears as a colored spot in the scene, the next piece
  previews in a white outline slot, and Milo (real character art) stands
  guard in front of the meadow.
- Scene placement is a pure, tested model (`SanctuaryScene.kt`): all 12
  catalog pieces have deterministic positions, so the daily-quest payoff is
  visible progress instead of a progress bar.

### Lesson player — real character avatars

- `CharacterGuide` now shows the actual character artwork (Milo, Mira, Niko,
  Lakan, Duke) in a circular avatar instead of a colored initial.

### Reward breaks — kid-first game shelf

- The mini-game library is curated: "Friendly favorites" (memory match,
  whack-a-mole, match-three, piano tiles, snake, stack, breakout, flappy
  bird, color connect, bolt sort, number merge, color block) lead the shelf;
  the 17 classics (wordle, solitaire, sudoku, checkers, …) stay available
  further down. Ordering is a pure tested function (`MiniGameShelf.kt`).

### Video Lessons

- Tagalog video library renamed "Video Lessons" for future multi-subject
  support; 8 full-length Tagalog videos (19–26) with 10-question memory
  checks (8/10 passing, `claimsMastery: false`) added to the catalog.

## [0.28.0] - 2026-08-10

### Playroom home — adversarial UX on a fresh install (8yo)

- Today's Quest no longer looks duplicated on day-1: colliding titles in the
  3-target set now carry a lightweight `· Day N` distinguisher
  (`questTargetDisambiguator` off `lessonId`, via `ModuleCatalog`), so three
  sequential "Word Roots" lessons read as `Word Roots · Day 1/2/3` instead of
  three identical rows.

### Lesson player — hotspot discoverability

- `ALL_TARGETS_VISITED` boards now show `N of M explored` under the question so
  an 8-year-old knows the goal is to visit every example.
- Remaining tappable badges pulse gently (8%, `!animationsDisabled`), visited
  badges stay still — draws the eye without competing with lesson art.

### Verification

- Full lesson pack educator-reviewed, 29/29 mini-games CSP/offline-clean.
- `testDebugUnitTest` (450 tasks) and `assembleDebug` green; content release gate
  `Release gate OK`.

## [0.27.0] - 2026-08-10

### Playroom home

- Refined the child home with transparent mascot and subject artwork assets.
- Removed the decorative profile/avatar control from the header.
- Made Milo's Wildlife Sanctuary a next-reward surface with progress and the
  next unlock, while keeping Wildlife Stickers as the collection entry point.
- Added regression coverage for the sanctuary/sticker distinction and the
  updated child-facing copy.

### Media and reward breaks

- Added the optional LAN-hosted media catalog/download path. Bundled lessons and
  bundled reward-break games remain available without network access.
- Consolidated the mini-game library and preserved reward-break entitlement
  lifecycle behavior.

### Release verification

- Educator gate: 358/358 playable lessons approved and released.
- Offline mini-game gate: 29/29 pages validated.
- Connected Android tests passed for child home and reward flows; full JVM
  unit-test suite passed locally.
- Release APK built and verified with APK Signature Scheme v2.

The `INTERNET` permission is intentional for optional media served from the
trusted home LAN; it is not used for telemetry or cloud content sync.

## [0.25.0] - 2026-08-08

### Highlights

- Added the unified Playroom home experience with separate sanctuary next-reward progress and Wildlife Sticker Book collection surfaces.
- Added transparent mascot and subject artwork, animal-photo badge details, and child-facing home-screen polish.
- Added reversible, PIN-gated parent god mode backed by DataStore for development and QA reward-flow verification.
- Added the offline mini-game library and reward-break flow refinements, including safer entitlement and badge-award handling.
- Added optional LAN-hosted media support for the approved home-network deployment.

### Verification

- 358/358 educator-reviewed lessons and 29/29 offline mini-games pass release gates.
- Root JVM tests, connected child-home/rewards tests, signed release assembly, and APK v2 signature verification pass locally.

### Quest

- Today's Quest now shows its 3 deterministic lesson targets (subject + friendly title, ✓/○ completion, accessible) so Maxine sees what counts toward the badge.
- Quest CTA routes to the next incomplete target (`lesson_player/{questLessonId}` — Start/Continue quest) instead of a placeholder.
- Covers the 100% bug where finishing a non-quest lesson made the badge look stuck.

### Child-facing polish

- Sort & Classify: scroll hint when cards exceed the fold; MCQ now nudges on empty Submit (`Pick one answer to check`).
- Place-value fix: `mathematics-g3-m01-d01` corrections rebuilt around the teaching example (`hundreds` / `hundreds place` line preserved, `ten thousands` → `ten thousand`).

### Content

- Replaced generic activity feedback (`Look at the example again` / `Great thinking! You found the key idea`) with topic-grounded, language-aware lines (e.g. `Find the clue in Picture Detective again`, `Great! You found the examples for …`) — 347 lessons.
- Fixed `english-g3-q3-w11-d03` (Sentence Polishing) — 5 title-substituted assessment prompts replaced with real capitalization/punctuation checks.
- Language hygiene: `en-PH` lessons now carry English feedback only (`Mahusay` 0 in `en-PH`).

### Tooling

- `content_review.py`: topic-grounded `make_activities` feedback table, generic-key replacement guard (`_GENERIC_FEEDBACK_KEYS`), language-aware sanitizers.
- Gates: content-review dry-run 0/358/0, quality audit 0 findings, similarity gate 0 @0.85, strict content validation 358/358 0/0, tooling 91 tests, `core-content` + app unit tests, content release gate educator-reviewed.

## [0.24.0] - 2026-08-08

### Changed (child-development content review, full lesson pack)

- Re-authored learner-facing content in 196 lesson files against the product
  goal (factual, age-appropriate, learning-first, encouraging, fun for an
  8-year-old): concrete kid questions replace objective-pasted assessment
  stems; abstract distractors replaced with plausible options; reasoning-based
  explanations replace "uses what we learned" phrasing; topic-specific hooks
  and activity instructions; encouraging feedback rotation replaces
  "You found the key idea" placeholders.
- Fixed scrambled assessment keys (english q3-w13-d03/d04), ambiguous keys
  (science living/non-living), clone assessments (math q1-w01-d01/d04), and
  Filipino simuno stem-leak items (32 lessons regenerated).
- Localized AP titles to fil-PH; simplified Filipino instruction verbs.
- Tooling hardened so generators/sanitizers cannot reintroduce the patterns:
  `repair_educator_findings.py`, `content_review.py`, `Models.kt`
  (FeedbackSanitizationTest, ActivityStepConversionTest), and
  `repair_filipino_content.py` (idempotency restored, 14/14 tool tests).
- Metadata untouched: full lesson pack educator-reviewed and RELEASED.

## [0.23.0] - 2026-08-08

### Content & visuals
- Completed the educator-review re-author round (2026-08-07 r2 completion): C4/C5/C6/C8 + M3/M8/M9/M10 + m1–m6, all seven subjects now Approvable.
- GMRC: per-value instructions + sequence steps across all 24 files; all assessments rebuilt as scenario-based judgment items with why-explanations (35 unique transfer scenarios authored).
- Makabansa: authored real community history for q1-w01-d01/02/03 (Barangay Sapa, founded 1955; school 1960; market 1970); all assessments rebuilt as judgment items.
- Filipino m01: 4 stock title-topic checks per lesson replaced with transfer items (80 authored); AP m01: stock "kasanayang ito" assessments replaced with concept-transfer items (100 authored).
- M3 sweep: 812 "kasanayan" jargon instances replaced with concept labels (simuno at panaguri, salitang-ugat, …) across 56 Filipino files; learner-facing "kasanayan" now 0 pack-wide.
- Fixed Taglish/legacy artifacts: `mag-shoot`→`maglaro ng basketbol`, `I-grupo`→`Ipangkat`, MKB doesNotFit culture swaps, 7 wastong-pagsulat word↔meaning matchings, circular vocab definitions.
- English m01-d13 Q5 re-keyed (ship = digraph, not blend); duplicate assessment prompts eliminated pack-wide (0).
- Integrated the revised editorial visual set: 358 month-01 lesson visuals (checksum-verified).
- Kept the pre-revision `english-g3-q1-w01-d01` visual: the revised art dropped curriculum clues (red flag, parade, lanterns) required by the picture-detective activity.
- Regenerated child-facing feedback copy across 161 lessons so authored correct/explanation text no longer exposes curriculum jargon.
- Removed all unreviewed lesson content from the APK (legacy `content/ph-matatag` fallback + pilot pack) and extended the release gate to cover every lesson-bearing asset directory (external review C3).
- Pinned the assessment pass policy at 80% (4/5), removed the silent runtime default, and enforced it in content validation (CH-03).
- Excluded practice activities from accuracy and mastery (`ActivityStep.scored` contract), recorded first-attempt vs retry passes distinctly (DB v9), and removed the legacy loaders/active-content path so only `RELEASED` bundled lessons can load (CH-04, CH-02).

### Child-facing feedback
- Extended copy sanitization to success feedback and assessment explanations (core model, lesson player, and content tooling share the same replacement table).
- Applied the feedback guard to Interactive Spec and Multiple Choice guidance copy.

### Educator review round 2 (2026-08-07)
- Re-keyed 10 Science "Material Detectives" lessons: anchor objects were marked correct for
  flexible/absorbs/hard all at once; keys now point to the true property holders, with
  property-based explanations.
- Fixed 6 inverted English odd-one-out keys, rebuilt the 2 broken sequencing assessments,
  replaced 25 placeholder keyed answers and 21 placeholder vocabulary entries with real content,
  rebuilt the mangrove informational lesson's assessment, and de-duplicated the
  `q2-w04-d02`/`q3-w11-d03` clone.
- Purged generator jargon pack-wide: "Subukan ang kasanayan sa…" → teaching lead-ins (138×),
  "shows the skill"/"the lesson skill"/"Try the skill" (0 remaining), "evidence from the
  example" labels (26×), 84 "evidence of this skill: <objective>" meta-prompts → skill
  questions, "number skills"/"about reading and writing" → true domain labels.
- Translated Araling Panlipunan English bleed to Filipino (223+ strings) and fixed the
  "kasanayang kasanayan" doubling artifact (55×) plus 15 circular GMRC vocabulary definitions.
- Fixed 2 pre-existing unit-test drifts found during verification: `StreakTest` positional
  args vs the v9 `LessonCompletionEntity` constructor, and the stale ModuleStructureTest
  assertion on the removed ph-matatag manifest tree.

### Testing
- Added UI-level regression coverage proving the Sequence CTA is a no-op until complete and submits the authored order as correct (#65).

## [0.22.0] - 2026-08-06

### Features
- Added 29 fully bundled, offline reward-break mini-games with child-safe WebView isolation.
- Added the Playroom mini-game library with categories and honest entitlement states.
- Preserved an earned reward break when a child leaves before choosing a game.
- Added an offline PIN-reset explanation and a retry path for transient lesson-load errors.
- Removed the last unused external repository URL from the production catalog.
- Added answer-neutral lesson concept visuals so illustrations support learning without revealing answers.

### Privacy, accessibility & reliability
- Kept the release APK offline-only: no `INTERNET` permission and no runtime content downloads.
- Enforced CSP, page count, external-URL, and browser-network-API checks for every bundled mini-game.
- Explicit target-SDK-35 edge-to-edge configuration with safe system-bar insets in parent authentication.
- Hardened SVG and mini-game WebViews against HTTP(S), mixed-content, and unsafe file-origin loading.
- Fixed PIN setup keyboard overlap and added IME regression coverage.

### Release gates
- All 358 bundled lessons carry the required educator approval metadata.
- Release verification now runs both educator-content and offline-mini-game gates.

## [0.21.1] - 2026-08-06

### Lesson reliability
- Sort & Classify prompts now use the same vocabulary as the visible answer buckets.
- Retrying Sort & Classify preserves correctly placed cards instead of forcing a full restart.
- Added regression coverage for both learner-facing behaviors.

## [0.21.0] - 2026-08-06

### Content
- Filipino Q1 W04–W08: nine new Grade 3 lessons + SVG artwork (PR #55) — pack expanded
- Educator review pass across all lessons (placeholders removed, unsafe distractors replaced, answer-position bias fixed, similarity gate zeroed)
- Quarterly lesson visuals regenerated as topic-specific scenes (800×450 / 1200×675)
- Repair tooling made idempotent and check-mode read-only (no more working-tree churn)

### Features
- Lesson visuals rendered in every activity type (bespoke SVG scenes)
- Transaction-safe lesson completion with assessment gate and no stale badge on replay
- Daily quests wired into the Playroom (with honest fallback states)
- Child-visible star/coin balances, reward spend surface, reward-break entitlements
- AP merged into Makabansa (Matatag successor) — six-card Playroom grid
- Persistent Read Along narration controls; Kitten Match reward break
- First Steps sticker; recommended starting module; lesson resume state

### Accessibility & reliability
- Genuine OFL fonts (Baloo 2 / Nunito) — fresh installs no longer crash on text input
- TTS locale matched by lesson language (Filipino narrated with a Filipino voice)
- Contrast, reduced motion, lockout, and TalkBack passes on the Playroom home
- Double-tap navigation guard, single TalkBack progress announcement, 14sp+ labels
- Brute-force protection for the parent PIN; day-streak fix (local timezone)
- DB v8 (wildlife_expeditions) with corruption quarantine + destructive-migration fallback; single parent account row
- Content-integrity test now tracks the real pack size and cannot be satisfied by stale build cache

### Tooling & CI
- Content quality gates (schema, semantic audit, educator metadata) enforced in CI
- Educator review gate wired into `check` and `assembleRelease` — a release can never ship unreviewed lessons
- Migration tests on emulator in CI; content tooling suite (81 tests) green

## [0.20.0] - 2026-08-01

- Playroom home (six subject collections) shipped as the main experience
- DB v7 with v4/v6 lineage migrations
- Pre-Playroom PRs #1–#6 retired

## [0.19.0] - 2026-07

- (Previous development baseline; see git history for details)
