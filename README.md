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

## Current release

The current signed Android release is **v0.48.1 (versionCode 61)**. The parent
Dashboard can obtain it through the trusted-LAN OTA path; the release APK is
published at the content server root and its `/media/` mirror.

## Current learning features

- **DepEd Video Hub:** subject routing, chronological grade/quarter/episode
  ordering, bulk downloads, watch-to-earn progress, and optional trusted-LAN
  media catalogs. Bundled lessons remain available offline.
- **Grade 3 Assessment Arena:** interactive quizzes for six subjects across
  Philippine, Singapore, and United States tracks, with responsive phone
  layouts, item-count-aware 80% passing thresholds, and reward integration.
- **Local OTA updates:** the parent dashboard can discover and hand off a
  trusted-LAN APK update for installation, while showing the installed app
  version. This is a local update path—not cloud content sync or telemetry.

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

The release is offline-first: lessons and reward-break games are bundled, with
optional media available from the trusted home LAN. It must pass both the
educator-content gate and the bundled mini-game isolation gate:

```bash
./gradlew check assembleRelease
python3 tools/content_quality_audit.py --check
python3 tools/dedupe_lesson_titles.py --check
```

The release manifest intentionally includes `android.permission.INTERNET` for
the optional LAN media path. Cleartext traffic is restricted to the configured
home-LAN media host; no cloud content sync or telemetry is included.

Release signing is configured through the user-level
`~/.gradle/maxines-world-signing.properties` file; signing secrets are never
stored in this repository.

## Independent educator review

The bundled Grade 3 lesson pack has a dedicated handoff for independent LLM or
human curriculum review:

- [Educator content review brief](docs/educator-content-review-brief.md)
- [Current state and handoff](HANDOFF.md)

The brief names the exact review baseline, lesson counts, non-destructive audit
commands, educator rubric, high-risk patterns, and required findings format.
Approval metadata and passing structural tests are not substitutes for factual,
pedagogical, language, and safety review.

## Architecture
See [docs/01-architecture-decisions.md](docs/01-architecture-decisions.md)  
See [docs/02-milestones-and-risks.md](docs/02-milestones-and-risks.md)

## License
- **Code:** All rights reserved — see [LICENSE](LICENSE)
- **Content (lessons, art, design):** CC BY-NC 4.0 — see [CONTENT-LICENSE](CONTENT-LICENSE)
- **Attribution & provenance:** see [NOTICE](NOTICE)
- Fonts (Baloo 2, Nunito): SIL Open Font License
