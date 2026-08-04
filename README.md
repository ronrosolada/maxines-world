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
- WorkManager
- Modular architecture (16 Gradle modules)

## Project Structure
```
android/
├── app/                          # Application shell
├── core-model/                   # Domain models
├── core-network/                 # API client
├── core-database/                # Room database
├── core-design-system/           # Theme and shared composables
├── core-content/                 # JSON lesson loader
├── feature-auth/                 # Parent PIN + child profile
├── feature-child-home/           # Village home screen
├── feature-lesson-player/        # Lesson activity player
├── feature-progress/             # Progress tracking
├── feature-parent/               # Parent dashboard
├── feature-rewards/              # Stars, coins, badges
├── engine-activity/              # Reusable activity composables
├── engine-assessment/            # Scoring and thresholds
├── engine-mastery/               # Mastery state machine
└── engine-sync/                  # WorkManager sync (progress reporting)
```

## Build
```bash
cd android
./gradlew assembleDebug
```

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
