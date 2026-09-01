# Maxine's World

An Android-first educational app for Grade 3 learners.  
Offline-first, curriculum-aligned, animal-village themed.

## Project Goal

Create **Maxine's World** for Maxine, an eight-year-old girl who loves animals.
The app should aid her studies with factual, age-appropriate educational
content while making learning engaging and fun through animal guides, clear
activities, and encouraging feedback.

Every product, content, and design decision should serve that goal: learning
comes first, facts must be trustworthy, and delight should help Maxine stay
curious rather than distract from understanding.

## Tech Stack
- Kotlin + Jetpack Compose + Material 3
- Room + DataStore
- Hilt DI
- Modular architecture (19 Gradle modules)
- Bundled HTML reward-break games with CSP-isolated WebViews

## Content Architecture
- **Video-First Lessons:** Video lessons streamed/cached from the homelab Caddy hub with real-time Whisper subtitle sync and post-watch video quizzes (`android/app/src/main/assets/content-pack/media-assessments.json`).
- **Assessment Arena:** Multi-curriculum quiz packs (`android/app/src/main/assets/assessment-packs/`) covering Philippine DepEd, Singapore MOE, and US standards.
- **Reward Mini-Games:** Bundled HTML reward-break games plus native reward games, gated by the reward-break entitlement engine.

## Project Structure
```
android/
├── app/                          # Application shell
├── core-model/                   # Domain models
├── core-network/                 # Optional trusted-LAN media catalog/downloads
├── core-database/                # Room database
├── core-design-system/           # Theme and shared composables
├── core-content/                 # JSON lesson loader
├── feature-auth/                 # Parent PIN + child profile
├── feature-child-home/           # Playroom home screen
├── feature-lesson-player/        # Lesson activity player
├── feature-progress/             # Progress tracking
├── feature-parent/               # Parent dashboard
├── feature-rewards/              # Stars, coins, badges
├── engine-activity/              # Reusable activity composables
├── engine-assessment/            # Scoring and thresholds
├── engine-mastery/               # Mastery state machine
├── engine-minigame/              # Reward-break entitlement engine
├── game-cat-cafe/                # Native cat-cafe reward game
├── game-pawprint-parkour/        # Native pawprint parkour game
└── game-kitten-match/            # Native kitten-match reward game
```

## Build
```bash
cd android
./gradlew assembleDebug
```

The release is offline-first: the Assessment Arena packs and reward-break games
are bundled, with optional video media available from the trusted home LAN. It
must pass the bundled mini-game isolation gate and the video/catalog validators:

```bash
./gradlew check assembleRelease
python3 android/tools/validate_video_checkpoints.py
python3 android/tools/validate_skill_graph.py
python3 android/tools/validate_catalog_parity.py
python3 android/tools/audit_media_assessment_uniqueness.py
python3 android/tools/validate_arena_packs.py
```

The release manifest intentionally includes `android.permission.INTERNET` for
the optional LAN media path. Cleartext traffic is restricted to the configured
home-LAN media host; no cloud content sync or telemetry is included.

Release signing is configured through the user-level
`~/.gradle/maxines-world-signing.properties` file; signing secrets are never
stored in this repository.

## Independent educator review

The video library and Assessment Arena have a dedicated educator quality report
for independent review:

- [Assessment Arena + video educator quality report](docs/educator-quality-report-assessment-arena-video-2026-08-27.md)
- [Current state and handoff](HANDOFF.md)

Passing structural tests are not substitutes for factual, pedagogical, language,
and safety review.

## Optional video lessons

The current personal-use preview catalog contains 237 workbook-selected videos
across Filipino, Makabansa, Mathematics, English, GMRC, and Science. It keeps
the Assessment Arena offline-first while offering verified LAN downloads, subject tags,
episode ordering, and five-question memory checks with a 4/5 pass threshold.
See the complete [playlist replacement and release documentation](docs/video-playlist-replacement-2026-08-20.md)
and the [optional media contract](android/docs/optional-video-media.md).

## Architecture
See [docs/01-architecture-decisions.md](docs/01-architecture-decisions.md)  
See [docs/02-milestones-and-risks.md](docs/02-milestones-and-risks.md)

## License
- **Code:** All rights reserved, see [LICENSE](LICENSE)
- **Content (lessons, art, design):** CC BY-NC 4.0, see [CONTENT-LICENSE](CONTENT-LICENSE)
- **Attribution & provenance:** see [NOTICE](NOTICE)
- Fonts (Baloo 2, Nunito): SIL Open Font License
