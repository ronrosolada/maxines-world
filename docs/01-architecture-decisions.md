# Maxine's World — Architecture Decisions

## ADR-001: Modular Gradle Architecture

**Decision:** Use a multi-module Gradle project with clear separation between core, feature, and engine modules.

**Structure:**
```
android/
├── app/                          # Application shell, DI, NavGraph
├── core-model/                   # Domain models, use cases, repository interfaces
├── core-network/                 # Retrofit/Ktor client, API definitions, sync
├── core-database/                # Room DB, DAOs, entities, migrations
├── core-design-system/           # Material 3 theme, tokens, shared composables
├── core-content/                 # Lesson JSON loader, content validation, asset paths
├── feature-auth/                 # Parent PIN/biometric, child profile CRUD
├── feature-child-home/           # Village home, subject destinations, Daily Quest
├── feature-lesson-player/        # Lesson activity rendering, narration, feedback
├── feature-progress/             # Progress event recording, mastery calculation
├── feature-parent/               # Dashboard, assignments, screen-time, reports
├── feature-rewards/              # Stars, coins, badges, village energy, cosmetics
├── engine-activity/              # Reusable activity engines (22 types)
├── engine-assessment/            # Scoring, hint tracking, response-time analysis
├── engine-mastery/               # Mastery state machine, spaced-repetition scheduler
└── engine-sync/                  # WorkManager-based sync, idempotency, conflict resolution
```

**Rationale:**
- Enforces dependency rules (feature → core, engine → core, never feature → feature)
- Enables parallel development across modules
- Keeps build times manageable with Gradle caching
- Each module has its own test suite

**Dependencies (strict DAG):**
```
app → feature-* → core-*, engine-*
engine-* → core-model, core-database
feature-* → core-model, core-database, core-design-system, core-content
core-network → core-model
core-database → core-model
core-content → core-model
core-design-system → (standalone)
```

---

## ADR-002: Kotlin + Jetpack Compose + Material 3

**Decision:** Kotlin as the sole language, Jetpack Compose for all UI, Material 3 with custom design tokens.

**Rationale:**
- Kotlin is first-class on Android and required by the handoff spec
- Compose eliminates XML/view-binding boilerplate
- Material 3 provides theming, accessibility, and adaptive layouts out of the box
- Custom design tokens (`MaxineColors`, `MaxineTypography`, `MaxineShapes`) overlay the M3 theme to match the Maxine's World visual identity (warm greens, teals, oranges, rounded shapes)

**Key theming decisions:**
- Warm, inviting color palette (teal primary, amber secondary, soft backgrounds)
- Large touch targets (minimum 48dp, often 56dp for child interactions)
- Custom `VillageTopBar` and `VillageBottomBar` composables
- Tablet-first with adaptive `WindowSizeClass` layouts
- Reduced-motion theme variant controlled by parent setting

---

## ADR-003: Room for Local Storage

**Decision:** Room as the primary local database, backed by SQLite.

**Key entities:**
```
ParentAccount (id, display_name, pin_hash, biometric_enabled, created_at)
ChildProfile (id, parent_id, name, avatar_id, grade, curriculum, created_at)
LessonPackage (id, subject, module_id, schema_version, json_path, checksum, status)
ProgressEvent (id, child_id, skill_id, lesson_id, activity_id, event_type, 
               accuracy, attempts, hints_used, response_time_ms, timestamp, sync_status)
MasteryState (child_id, skill_id, state, accuracy, total_attempts, 
              last_activity_at, next_review_at, version)
Reward (id, child_id, type, subject, earned_at, metadata_json)
ScreenTimeRecord (child_id, date, total_learning_minutes, total_play_minutes)
ScreenTimeLimit (child_id, day_type, limit_minutes, downtime_start, downtime_end)
DailyQuest (child_id, date, subject_rotations, completed_lessons, energy_earned)
```

**Design principles:**
- Append-only progress events (never delete child work)
- Idempotency keys for sync (prevent duplicate reward grants)
- Mastery recomputed from events, not a single mutable field
- Room migrations versioned alongside app versions

---

## ADR-004: Offline-First with WorkManager Sync

**Decision:** The app works fully offline. Progress is buffered locally and syncs via WorkManager when connectivity returns.

**Mechanism:**
1. Lessons and assets are downloaded as versioned packages
2. `ProgressEvent` rows are written locally with `sync_status = PENDING`
3. `WorkManager` periodic worker (15 min, with connectivity constraint) batches uploads
4. Server responds with idempotency-confirmed event IDs
5. Mastery recomputed locally after confirmed sync
6. Content package downloads also use WorkManager (one-time with network constraint)

**Conflict resolution:**
- Progress events are append-only — no update conflicts
- Mastery is always computed from the complete event stream
- Last-write-wins for profile/avatar changes (low risk)
- Content packages use checksum verification before activation

---

## ADR-005: Versioned External JSON Lessons

**Decision:** All curriculum structure, lesson definitions, questions, and activity parameters live in versioned JSON files outside the APK.

**Package structure:**
```
content/
└── ph-matatag/
    └── grade-3/
        ├── manifest.json          # All subjects, modules, versions, checksums
        ├── english/
        │   ├── module-01/
        │   │   ├── lesson-01.json
        │   │   └── assets/        # Images, audio (narration)
        │   └── module-02/...
        ├── filipino/...
        ├── mathematics/...
        ├── science/...
        └── philippine-history/...
```

**Lesson JSON schema (simplified):**
```json
{
  "id": "eng-g3-m01-l01",
  "schemaVersion": 1,
  "subject": "english",
  "title": "The Cats Who Saved the Garden",
  "objective": "Identify main idea and supporting details in a story.",
  "guideCharacter": "mira",
  "estimatedMinutes": 10,
  "steps": [
    {
      "type": "animated_explanation",
      "narrationText": "Mira reads a story about cats saving a garden...",
      "narrationAudio": "assets/narration/l01-intro.mp3",
      "imageAssets": ["assets/illustrations/l01-scene1.png"]
    },
    {
      "type": "multiple_choice",
      "question": "What is the main idea of the story?",
      "options": ["Cats love to sleep", "Cats worked together to save the garden", "Gardens need water"],
      "correctIndex": 1,
      "feedback": { "correct": "Great! The cats worked as a team!", "incorrect": "Let's think again..." }
    }
  ],
  "assessment": { "passThreshold": 0.8, "minQuestions": 5 }
}
```

**Loading mechanism:**
- `core-content` module provides `LessonLoader` which reads from `assets/content/` (bundled fallback) or `context.filesDir/content/` (downloaded)
- Checksum verification before activating any downloaded package
- Previous valid package preserved as rollback

---

## ADR-006: Hilt for Dependency Injection

**Decision:** Hilt (Dagger-based) for compile-time DI across all modules.

**Scoping:**
- `@Singleton`: Database, ContentLoader, SyncManager
- `@ActivityRetainedScoped`: ViewModels that survive config changes
- `@ViewModelScoped`: Per-screen state holders
- `@FeatureScoped` (custom): Per-feature graph modules

**Module structure:**
- Each feature module exposes a `FeatureModule` (Hilt module)
- `app` module provides the `SingletonComponent` bindings
- Test modules provide fake implementations via `@TestInstallIn`

---

## ADR-007: MVVM with Unidirectional Data Flow

**Decision:** Each screen uses ViewModel + Compose UI with UDF.

**Pattern:**
```
UI (Compose) → user intent → ViewModel.onAction()
ViewModel → state: StateFlow<ScreenUiState> → UI recomposes
ViewModel → side effects: SharedFlow<ScreenEffect> → UI handles (nav, snackbar)
```

**UiState design:**
- Immutable data classes
- Single `UiState` per screen (not per-widget state)
- Loading/Error/Content sealed hierarchies
- Child-safe: no sensitive data in UI state

---

## ADR-008: Navigation Compose with Type-Safe Routes

**Decision:** Navigation Compose with a sealed class route system.

**Route structure:**
```
sealed class Route {
    object ParentLogin : Route("parent-login")
    object ChildSelect : Route("child-select")
    data class ChildHome(val childId: String) : Route("child-home/{childId}")
    data class LessonPlayer(val childId: String, val lessonId: String) : Route("lesson/{childId}/{lessonId}")
    object ParentDashboard : Route("parent-dashboard")
    // ...
}
```

**Child safety gate:**
- Launcher activity routes to `ParentLogin` if no valid session
- Child screens cannot navigate to parent screens without auth challenge
- Back-navigation from child screens goes to child home, not parent login

---

## ADR-009: Activity Engine Architecture

**Decision:** Each activity type is a composable that consumes a typed data class and emits `ActivityResult` events.

**Engine interface:**
```kotlin
@Composable
fun ActivityEngine(
    activity: ActivityDefinition,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier
)
```

**Initial engines (MVP):**
1. `AnimatedExplanationEngine` — story/narration with image slides
2. `MultipleChoiceEngine` — tap-to-select
3. `DragAndDropEngine` — drag items to targets
4. `SortAndClassifyEngine` — sort into categories
5. `SentenceBuilderEngine` — tap words to build sentences
6. `StoryComprehensionEngine` — read/listen + answer
7. `ArrayBuilderEngine` — build equal groups (math)
8. `PredictionObservationEngine` — predict, observe, explain (science)
9. `TimelineBuilderEngine` — sequence events (history)

**Result contract:**
```kotlin
data class ActivityResult(
    val activityId: String,
    val correct: Boolean,
    val attempts: Int,
    val hintsUsed: Int,
    val responseTimeMs: Long,
    val answerData: JsonObject  // activity-type-specific
)
```

---

## ADR-010: Mastery Computation

**Decision:** Mastery is computed from the event stream, not stored as mutable state.

**State machine:**
```
NOT_STARTED → INTRODUCED → PRACTICING → PROFICIENT → MASTERED
                                       ↘ NEEDS_REVIEW → PRACTICING
```

**Thresholds (configurable per subject):**
- `MASTERED`: ≥10 meaningful attempts, ≥80% accuracy over last 20 attempts, 
  ≥2 different activity types, ≥1 successful delayed review (7+ days)
- `NEEDS_REVIEW`: accuracy drops below 60% over 10+ recent attempts, 
  or 14 days without practice on a PROFICIENT skill

**Spaced repetition schedule:**
- After INTRODUCED: review in 1 day
- After PRACTICING: review in 3 days
- After PROFICIENT: review in 7 days
- After MASTERED: review in 30 days

---

## ADR-011: Testing Strategy

**Decision:** Test pyramid — unit tests (heavy), integration tests (medium), UI tests (light).

**Layers:**
1. **Unit tests** (JUnit5 + MockK):
   - Mastery computation (pure logic)
   - Lesson JSON parsing and validation
   - Reward eligibility rules
   - Progress event aggregation

2. **Integration tests** (Robolectric or instrumented):
   - Room DAO queries
   - ContentLoader with real JSON files
   - ViewModel + fake repositories
   - WorkManager sync chain

3. **UI tests** (Compose testing):
   - Screen rendering with test data
   - Navigation flow (parent login → child home → lesson → dashboard)
   - Touch target accessibility (minimum 48dp verification)

**Test module structure:**
- `:core-database:test` — Room DAO tests
- `:core-content:test` — JSON loading tests
- `:engine-mastery:test` — mastery logic tests
- `:feature-auth:test` — login flow tests
- `:feature-lesson-player:test` — activity engine tests
- `:app:test`, `:app:androidTest` — integration and UI tests

---

## ADR-012: Security & Privacy

**Decision:** Defense-in-depth for child data.

**Measures:**
- Parent PIN: hashed with `PBKDF2WithHmacSHA256`, stored in `EncryptedSharedPreferences`
- Biometric auth: `BiometricPrompt` with `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL`
- No child email, phone, precise location, or advertising ID
- Room database in app-private storage (not external)
- No logs containing child progress data in release builds
- HTTPS for all network calls (when online)
- Data export: JSON bundle, parent-authenticated, with 24h deletion after request

---

## Summary of Key Technical Choices

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Modular MVVM with UDF |
| DI | Hilt |
| Local DB | Room (append-only progress) |
| Navigation | Navigation Compose (type-safe routes) |
| Networking | Retrofit + Kotlin Serialization |
| Sync | WorkManager |
| Content | Versioned external JSON packages |
| Testing | JUnit5 + MockK + Compose testing |
| Security | PBKDF2 PIN hash + BiometricPrompt |
| Animation | Lottie (MVP placeholder) → Rive (long-term) |
| Min SDK | 26 (Android 8.0, >95% active devices) |
| Target SDK | 35 |
