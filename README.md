# Maxine's World

An Android-first educational app for Grade 3 learners.  
Offline-first, curriculum-aligned, animal-village themed.

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
└── engine-sync/                  # WorkManager sync
```

## Build
```bash
cd android
./gradlew assembleDebug
```

## Architecture
See [docs/01-architecture-decisions.md](docs/01-architecture-decisions.md)  
See [docs/02-milestones-and-risks.md](docs/02-milestones-and-risks.md)
