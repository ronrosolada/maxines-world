# Maxine's World Android Architecture & Performance Review

**Review date:** 2026-08-30  
**Scope:** `/home/ron/projects/maxines-world/android` — 19 Gradle modules, production Kotlin/Compose, Room, Media3, offline/LAN media, OTA content, tests and release controls.

## Executive summary

The codebase has a credible offline-first foundation: clear core/feature/engine/game naming, non-transitive resources, centralized version catalog, exported Room schemas with migration tests, lifecycle release of media primitives, release minification, educator metadata gates, disabled backup, and a deliberately locked-down SVG WebView. The strongest engineering is around durable reward idempotency, migration lineage, and child-safe offline behavior.

Its main architectural constraint is that the module names overstate isolation. Feature modules directly depend on other features and on Room entities/DAOs; `:app` depends on every implementation module; several feature modules run Room KSP despite owning no database; and `:core-design-system` depends on `:core-model`. This produces a broad recompilation graph and permits UI, persistence and navigation policy to leak across boundaries.

The largest runtime risks are (1) one ExoPlayer per composed QuickBits page, (2) lifecycle-unaware Flow collection on multiple screens, (3) almost no Compose stability metadata on UI models, (4) 16 ms game-state publication through `StateFlow`, and (5) large ad-hoc screen implementations with hundreds of raw visual values. The largest data-safety risk is the global destructive Room fallback: a missing migration silently discards child progress.

### Architecture scorecard

| Pillar | Score | Assessment |
|---|---:|---|
| Module boundaries & dependency graph | 6/10 | No cycle detected in declared project dependencies, but feature-to-feature and feature-to-database coupling is extensive. |
| Build health & Gradle | 6/10 | Parallel/cache enabled; configuration cache disabled; repeated plugin/KSP setup and configuration-time Git processes remain. |
| Compose/UDF/performance | 6/10 | StateFlow-based UDF is common and some lifecycle collection exists, but usage is inconsistent; stability and token discipline are weak. |
| Room/data integrity | 7/10 | Strong schema export, migrations, reactive DAOs and transactional writers; destructive fallback and missing indexes lower confidence. |
| Media/offline/OTA | 6/10 | Correct basic releases and verified downloads; player-per-page and weak LAN failover/backoff are significant. |
| Tests/safety/privacy | 7/10 | Broad unit footprint and useful instrumentation/migration tests; uneven module coverage and no benchmark/macrobenchmark module. |
| **Overall** | **6.3/10** | Sound product foundation; prioritize boundary enforcement, media ownership, data-loss prevention and measured Compose performance. |

## Codebase and dependency inventory

19 modules were resolved by Gradle: `:app`, five `:core-*`, six `:feature-*`, four `:engine-*`, and three `:game-*`. Static inventory found 171 production Kotlin files, 80 JVM test files, and 27 Android-test files. No declared project-dependency cycle was found.

Key graph pressure points:

- `:app` directly implements all other 18 modules (`app/build.gradle.kts:88-108`).
- `:feature-child-home` depends on `:feature-rewards` and `:feature-parent` (`feature-child-home/build.gradle.kts:20-26`).
- `:feature-parent` depends on `:feature-auth` and `:feature-rewards` (`feature-parent/build.gradle.kts:20-27`).
- `:feature-lesson-player` depends on `:feature-rewards` plus three engines (`feature-lesson-player/build.gradle.kts:25-33`).
- UI features depend directly on `:core-database`, exposing Room entities/DAOs as cross-layer contracts.
- `:core-design-system` depends on `:core-model` (`core-design-system/build.gradle.kts:20`), allowing domain changes to invalidate the shared UI foundation.

## Prioritized findings

| Priority | Area | Finding / impact | Evidence | Recommended action |
|---|---|---|---|---|
| **P0** | Data safety | Global `fallbackToDestructiveMigration(dropAllTables=false)` can silently erase child progress for any unknown schema path. This contradicts offline-first durability and makes migration omissions production data-loss events. | `app/.../di/DatabaseModule.kt:53-70` | Remove global fallback in release. Register all tested migrations. If a specific legacy version must reset, use `fallbackToDestructiveMigrationFrom(...)` only for explicitly approved pre-production versions. Preserve/quarantine DB and expose a parent recovery flow rather than silently resetting. |
| **P0** | Media/performance | QuickBits constructs an ExoPlayer for every composed pager page. Pager precomposition can keep several decoders, surfaces and buffers alive, causing memory pressure and decoder exhaustion on lower-end tablets. | `feature-lesson-player/.../QuickBitsScreen.kt:418-472` | Hoist a single player into a route-scoped `QuickBitsPlayerController`/ViewModel, switch `MediaItem` on page change, attach only the active `PlayerView`, and explicitly detach the old surface. |
| **P1** | Architecture | Feature-to-feature dependencies and direct DAO/entity imports make features non-independent and permit UI to orchestrate persistence. Changes propagate widely; isolated feature testing/reuse is difficult. | Build files listed above; feature modules include `:core-database` directly. | Introduce narrow domain contracts (`core-domain` or feature `:api` modules). Features depend on interfaces/models only; `:app` binds implementations. Replace `feature-child-home -> feature-parent/rewards` with navigation/action contracts. |
| **P1** | Compose lifecycle | Several routes use `collectAsState()` instead of `collectAsStateWithLifecycle()`, so upstream work and recomposition continue while stopped/backgrounded. | `feature-parent/.../ParentGateScreen.kt:234`; `ParentDashboardScreen.kt:449-451`; `game-cat-cafe/.../CatCafeDashScreen.kt:55`; `game-kitten-match/.../KittenMatchScreen.kt:57`; `feature-rewards/.../TreatShopScreen.kt:107`, `RewardsManager.kt:99`; parkour screen line 36. | Standardize all Android route collection on lifecycle-aware APIs. Keep pure content composables stateless. Add a lint/custom Detekt rule banning route-level `collectAsState()`. |
| **P1** | Compose stability | Only one production `@Immutable` annotation was found, despite many list-heavy UI state data classes. Compose must conservatively treat collection-bearing parameters as unstable, increasing invalidation scope. | Only `game-kitten-match/.../GameLayoutMode.kt:14`; UI states in `LessonPlayerViewModel.kt:31`, `AssessmentArenaViewModel.kt:21`, `QuickBitsViewModel.kt:24`, etc. | Enable Compose compiler stability reports; annotate truly immutable models, use persistent immutable collections where high-frequency lists cross composable boundaries, and avoid annotation until member mutability is proven. |
| **P1** | Game frame loop | Parkour publishes a full UI state through `MutableStateFlow` every 16 ms, forcing snapshot bridge/recomposition at ~60 Hz; packed one-line code also obscures lifecycle and allocation behavior. | `game-pawprint-parkour/.../ParkourViewModel.kt:15-22` | Keep simulation state off Compose, update in a fixed-step loop, expose low-frequency HUD state (e.g. 4–10 Hz), and render position via `Canvas`/draw state without recomposing the screen tree. Pause loop when lifecycle is stopped. |
| **P1** | Database indexing | High-volume progress queries filter/order by child/skill/status/timestamp but `progress_events` has no indexes; several other child-keyed tables also lack indexes. Performance degrades with longitudinal use. | `core-database/.../Entities.kt:28-42`; queries in `Daos.kt:43-70`. | Add composite indexes matching access paths, e.g. `(childId,timestamp)`, `(childId,skillId,timestamp)`, and `(syncStatus)` or partial-query equivalent. Benchmark with representative multi-year data and migrate explicitly. |
| **P1** | Sync correctness | Periodic work uses one global unique name, so scheduling a second child with `KEEP` leaves the first child's input forever. It also converts retryable errors to permanent failure after three periodic attempts and specifies no backoff. | `feature-progress/.../ProgressSyncWorker.kt:32-60` | Name work per child (`...:$childId`) or make worker synchronize all profiles. Configure exponential backoff; classify auth/4xx as failure and IO/5xx as retry. Persist sync cursor; do not rely on static input timestamp. |
| **P1** | Network resilience/security | LAN URLs are hard-coded HTTP host addresses. Connectivity `CONNECTED` does not imply DreamNAS reachability or correct VLAN route. Host changes require an app build. | `core-network/.../ApiClient.kt:42`; `MediaCacheManager.kt:52`; `app/.../di/MediaModule.kt:21-24`; manifest network config. | Inject an ordered endpoint policy from signed/configured local settings (LAN + guest endpoint), probe with short timeouts, cache the last healthy endpoint, and apply jittered retry/circuit breaking. Pin/verify content hashes regardless of transport; prefer locally trusted TLS where deployable. |
| **P1** | Build performance | Room compiler is applied in features that consume DAOs but do not define Room schema, multiplying KSP work. Hilt/KSP also appears in model/design/content modules regardless of actual generated bindings. | `feature-auth/build.gradle.kts:29-30`; `feature-parent:33-34`; `feature-progress:31-32`; `feature-rewards:38-39`; `engine-mastery:25-26`. | Remove `ksp(room-compiler)` outside `:core-database` unless a module declares `@Database/@Dao/@Entity`. Audit Hilt usage module-by-module. Move repeated Android/Kotlin/Compose settings to convention plugins. |
| **P1** | Build configuration | Configuration cache is disabled and app version calculation starts Git processes during configuration. This prevents fast configuration reuse and adds nondeterministic process overhead. | `gradle.properties:2-5`; `app/build.gradle.kts:13-27` | Make build configuration-cache compatible, enable it in CI after validation, and supply version code/name from CI Gradle properties/providers. Avoid eager `ProcessBuilder` during configuration. |
| **P2** | Media lifecycle | Offline player releases correctly, but `PlayerView.player` is not explicitly detached before player release; backgrounding does not pause unless composition leaves. The 200 ms perpetual checkpoint loop wakes even when playback is paused. | `feature-lesson-player/.../VideoStep.kt:182-230` | Observe lifecycle and pause/resume intentionally; detach view (`view.player=null`) on disposal; replace polling with player position scheduling or gate polling on `isPlaying` and lifecycle. Hoist controls into a controller. |
| **P2** | Audio lifecycle/main thread | MediaRecorder and MediaPlayer `prepare()` execute inside button callbacks on the main thread and can block; cache recordings remain after disposal. | `engine-activity/.../AudioRecordPlaybackRenderer.kt:47-105` | Use coroutine `Dispatchers.IO` or async prepare, model recorder state in a controller/ViewModel, handle stop failures, abandon audio focus, and delete temporary files on completion/disposal unless retention is intentional. |
| **P2** | Design-system drift | A scan found 444 occurrences across raw colors, explicit font sizes, lazy/canvas/performance markers, concentrated in very large feature screens. Screens duplicate visual policy and become hard to optimize/test. | `AssessmentArenaScreen.kt` (43 hits), `ParentDashboardScreen.kt` (42), `VideoLibraryScreen.kt` (25), `CatCafeDashScreen.kt` (23), `MiloCelebration.kt` (20). | Expand semantic tokens (spacing, shape, type, motion, content colors) and reusable components. Gradually replace raw `Color.White/Black`, `fontSize`, and fixed sizes in feature code. Add token-drift CI thresholds. |
| **P2** | State ownership/UDF | Most ViewModels expose read-only `StateFlow`, which is good, but some screens own substantial playback/game state and side effects directly in composables. | `QuickBitsScreen.kt:425-472`; `AudioRecordPlaybackRenderer.kt:45-63`; large screen files. | Route → ViewModel/controller → immutable UiState + typed UiAction. Keep platform objects in lifecycle-aware controllers, not saveable UI state. Use sealed effects for navigation/toasts. |
| **P2** | Room model integrity | Many relationships are encoded only as strings with no foreign keys; enums are persisted as unconstrained strings; JSON/delimited arrays are stored in columns. Orphan rows and invalid states are possible. | `Entities.kt:16-312` (`childId`, state/type fields, JSON/delimited sets). | Add foreign keys/indexes where deletion semantics are clear; use typed converters/enums with migration-safe values; normalize growing many-to-many sets (quest assignments/completions) when query needs justify it. |
| **P2** | OTA atomicity | Package registry has useful verified/staged/active states, but active package metadata and package state are separate tables; activation must always be one transaction and filesystem rename must be atomic. | `Entities.kt:259-293`; `ContentPackageDao`/active pointer DAOs. | Centralize install/verify/activate in one repository. Download to temp, fsync, hash/signature verify, atomic rename, then one Room transaction to change active pointer/state. Retain last-known-good package and bounded eviction. |
| **P2** | App composition | `:app` directly depends on engines and all games/features, increasing APK and compile graph; implementation classes are available everywhere in app. | `app/build.gradle.kts:88-108` | Keep app as composition/navigation root. Add feature entry contracts; consider dynamic delivery only if APK size/startup measurement warrants it. At minimum remove direct engine deps already transitively hidden behind feature APIs. |
| **P2** | Startup/performance measurement | No macrobenchmark/baseline-profile module was identified, so startup, scroll and lesson-transition regressions are unguarded. | Module inventory; ProfileInstaller appears transitively but no benchmark producer. | Add `:benchmark` and `:baselineprofile`; measure cold/warm startup, Playroom scroll, lesson open, QuickBits swipe, database-loaded dashboard, and reward game frame timing. Gate P95 frame/startup budgets in release CI. |
| **P2** | Test distribution | Every module has at least one JVM test, but several large UI modules have only one unit test and no Android tests (`feature-progress`, `feature-parent`, games except selected modules). Counts do not establish behavioral coverage. | Inventory: 80 JVM and 27 Android-test Kotlin files; per-module counts from static scan. | Publish Kover/Jacoco branch coverage by module; prioritize state-machine, sync conflict, process recreation, font scale, TalkBack, media lifecycle and multi-child work tests over percentage alone. |
| **P3** | Readability/dead-code risk | Several production game/media classes are minified into one-line Kotlin, defeating reviewability and making duplicate/dead behavior harder to detect. | `ParkourViewModel.kt:15-22`; `PawprintParkourScreen.kt:35-37`; `VideoCheckpointRuntime` and overlay in `VideoStep.kt:282-289`. | Format with ktfmt/Spotless and enforce in CI. Then run Android Lint + dependency analysis + binary API checks to identify genuinely unused classes/dependencies. |

## Concrete optimization sketches

### 1. Replace destructive migration fallback

```kotlin
Room.databaseBuilder(context, MaxinesDatabase::class.java, DB_NAME)
    .addMigrations(*MaxinesMigrations.ALL_MIGRATIONS)
    // Release builds fail loudly and preserve the DB for support/recovery.
    // If an ancient non-production version is approved for reset:
    // .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
    .build()
```

Add a migration test from every shipped schema version to 12, including 9→10 and 10→11 (the current migration test references show explicit checks for many paths but should be audited for every shipped start version). CI should compare committed schema JSON and fail on uncommitted schema changes.

### 2. Index the progress event access paths

```kotlin
@Entity(
    tableName = "progress_events",
    indices = [
        Index(value = ["childId", "timestamp"]),
        Index(value = ["childId", "skillId", "timestamp"]),
        Index(value = ["syncStatus"]),
    ],
)
data class ProgressEventEntity(/* ... */)
```

Validate using `EXPLAIN QUERY PLAN` and a benchmark fixture of at least 100k events; do not add every conceivable index because write cost and DB size matter.

### 3. One player for QuickBits

```kotlin
@Stable
class QuickBitsPlayerController(context: Context) : AutoCloseable {
    val player = ExoPlayer.Builder(context.applicationContext).build()

    fun show(item: QuickBitItemUi, positionMs: Long) {
        val uri = item.localFile?.takeIf(File::exists)?.toUri()
            ?: item.item.videoUrl.toUri()
        player.setMediaItem(MediaItem.fromUri(uri), positionMs)
        player.prepare()
        player.playWhenReady = true
    }

    override fun close() {
        player.clearVideoSurface()
        player.release()
    }
}
```

Create/close it at the route lifecycle, and ensure inactive pages render thumbnails rather than owning decoders. In `AndroidView.onRelease`, set `player = null`.

### 4. Lifecycle-aware state collection

```kotlin
@Composable
fun ParentDashboardRoute(/* ... */) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ParentDashboardScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
```

Apply consistently to every Android route. For game clocks, stop simulation through `repeatOnLifecycle(STARTED)` rather than only suppressing UI updates.

### 5. Per-child WorkManager identity and backoff

```kotlin
val request = PeriodicWorkRequestBuilder<ProgressSyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .setInputData(workDataOf(KEY_CHILD_ID to childId))
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "$WORK_NAME_PERIODIC:$childId",
    ExistingPeriodicWorkPolicy.UPDATE,
    request,
)
```

Prefer a durable cursor table and an injected Hilt Worker (`@HiltWorker` + `HiltWorkerFactory`) so process recreation creates the worker correctly.

### 6. Enforce dependency direction

Target shape:

```text
app (composition/navigation)
 ├─ feature-*-api       (routes/contracts only)
 ├─ feature-*-impl      -> core-domain, core-ui, engine APIs
 └─ data bindings       -> core-database, core-network

core-ui/design-system   -> no domain/database dependency
core-domain             -> pure Kotlin models + repository interfaces
core-data               -> database/network implementations
engine-*                -> pure model/domain where possible
```

Example contract:

```kotlin
interface ProgressRepository {
    fun observeDashboard(childId: ChildId): Flow<ProgressDashboard>
    suspend fun record(result: ActivityResult): RecordResult
}
```

Room entities remain internal to the data implementation; features no longer import DAOs.

### 7. Convention plugins and KSP scope

Create `build-logic` convention plugins such as `maxines.android.library`, `maxines.android.compose`, and `maxines.android.hilt`. Remove Room compiler from modules that define no Room annotations:

```kotlin
// feature-auth/build.gradle.kts
plugins { id("maxines.android.compose") }
dependencies {
    implementation(project(":core-domain"))
    // No ksp(room-compiler); no direct Room API.
}
```

Enable Compose compiler metrics in performance CI and archive `reports/compose_metrics` to identify unstable/skippability regressions.

## What is already good

- Declared module dependency graph is acyclic.
- `android.nonTransitiveRClass=true`, parallel execution and Gradle build cache are enabled (`gradle.properties`).
- Kotlin 2.1.20 uses the compatible KSP1 line `2.1.20-1.0.32`; this avoids the known old-Hilt/KSP2 incompatibility.
- Room schemas are exported and migration tests use `MigrationTestHelper` (`core-database/build.gradle.kts:17-24`, `MigrationTest.kt`).
- Important multi-table reward writes use `database.withTransaction` (`TreatShop.kt:78-100`, `DailyQuestRewardWriter.kt:53-54`), and a reusable `RoomTransactionRunner` exists.
- Reward/idempotency tables use appropriate uniqueness constraints, reducing replay and double-award risk.
- ExoPlayer, MediaPlayer, MediaRecorder, SoundPool and ToneGenerator generally have explicit release paths.
- SVG rendering disables JavaScript, DOM storage and content access, blocks URL navigation/network requests, and inlines verified assets (`AssetSvgPreview.kt`).
- Backups are disabled and app data extraction is constrained (`AndroidManifest.xml:11-20`).
- Release minification is enabled and signing secrets are outside the repository (`app/build.gradle.kts:42-66`).
- Theme typography is correctly constructed from supplied font families and reduced-motion state is re-read on resume (`Theme.kt:36-100`).
- Some critical routes already use `collectAsStateWithLifecycle`, providing a clear migration pattern.
- Core learning remains bundled/offline; optional LAN media is additive rather than a hard startup dependency.

## Testing, security and child-safety gaps

1. **Performance:** Add Macrobenchmark, Baseline Profile and JankStats/Perfetto scenarios. Establish budgets: startup P95, QuickBits swipe frame P95, max active decoder count, lesson-open latency, and DB dashboard latency at realistic data volume.
2. **Lifecycle:** Instrument process/background/rotation tests for each player, recorder and game loop; assert one active player and no resumed audio after background.
3. **Accessibility:** Add tests at font scales 1.0/1.3/2.0, compact/medium/expanded widths, TalkBack semantics, 48–64 dp touch targets, and reduced motion. Replace remaining clickable Surface/Card semantics gaps with explicit roles/labels.
4. **Privacy:** Microphone copy correctly states local-only, but add an explicit retention policy and delete cached recordings. Keep analytics/advertising SDKs prohibited by dependency policy.
5. **Parent gate:** Keep lockout state durable across process death and wall-clock manipulation; test brute-force limits and ensure install/update surfaces remain parent-only. `REQUEST_INSTALL_PACKAGES` is high-risk and should be absent from child builds if in-app APK installation is not essential.
6. **Network:** Assert cleartext is scoped only to approved RFC1918 hosts in `network_security_config`; test guest VLAN unreachable, endpoint switch, interrupted range download, hash mismatch, disk full and rollback.
7. **Static quality:** Enforce ktfmt/Spotless, Android Lint fatal baselines only for reviewed legacy issues, dependency analysis, and a module graph rule forbidding `feature-* -> feature-*` except API modules.

## Phased implementation roadmap

### Sprint 0 — guardrails and measurement (2–4 days)

- Remove/restrict destructive migration fallback; complete all-version migration matrix.
- Add architecture dependency tests and formatting enforcement.
- Turn on Compose compiler metrics/reports and add benchmark/baseline-profile modules.
- Capture baselines: cold/warm startup, Playroom scroll, QuickBits swipe, lesson open, game frame timing, DB query plans.
- Add per-child sync regression test and media active-player-count test.

**Exit criteria:** no unapproved destructive migration path; reproducible baseline artifacts; dependency graph policy in CI.

### Sprint 1 — runtime hot paths (1 sprint)

- Replace QuickBits player-per-page with one route-scoped player; detach surfaces.
- Make all Flow collection lifecycle-aware.
- Refactor parkour loop to decouple simulation/draw/HUD publication.
- Move recorder/player preparation off main and clean temporary audio.
- Add/validate indexes with representative datasets.

**Exit criteria:** one decoder/player for QuickBits; no background playback/work; measured frame/query improvement without functional regression.

### Sprint 2 — boundary correction (1–2 sprints)

- Introduce pure domain repository interfaces and feature navigation contracts.
- Move Room entities/DAOs behind data implementations.
- Break `feature-child-home -> feature-parent/rewards` and `feature-parent -> feature-auth/rewards` implementation dependencies.
- Remove unnecessary app/feature engine dependencies and excess Room/Hilt KSP processors.
- Adopt convention plugins and enable configuration cache.

**Exit criteria:** features compile against API/domain contracts; `core-design-system` is domain-independent; configuration cache passes; clean-build time and invalidation fan-out improve.

### Sprint 3 — design/state consolidation (1–2 sprints)

- Split large screen files into route/content/components with immutable state and typed actions.
- Introduce semantic spacing/shape/motion/color tokens; migrate high-hit screens first.
- Apply proven `@Immutable`/persistent collection changes based on compiler reports, not blanket annotations.
- Add adaptive/font-scale/TalkBack/reduced-motion test matrix.

**Exit criteria:** compiler stability report shows improved skippability in target screens; token drift below agreed threshold; accessibility matrix green.

### Sprint 4 — resilient OTA and sync (1 sprint)

- Implement endpoint policy/failover for LAN and guest VLAN, health probes, exponential backoff and persisted cursors.
- Make content activation filesystem-atomic + DB-transactional with last-known-good rollback.
- Add bounded cache eviction based on free space/LRU while pinning currently active lessons/media.
- Remove or parent-isolate `REQUEST_INSTALL_PACKAGES` unless demonstrably required.

**Exit criteria:** chaos tests pass for host unreachable, VLAN switch, partial download, corruption, disk full, process death during activation and rollback.

## Verification checklist

- [x] Gradle resolved all 19 projects (`./gradlew projects`).
- [x] Static module dependency inventory produced no declared cycle.
- [x] Production/test/androidTest Kotlin footprint inventoried module-by-module.
- [x] Room entities, DAOs, migrations and transaction call sites inspected.
- [x] Compose state collection, stability annotations and visual-token hotspots scanned.
- [x] ExoPlayer/MediaPlayer/MediaRecorder/SoundPool/ToneGenerator lifecycle sites inspected.
- [x] Manifest, LAN endpoints, WorkManager and SVG WebView security reviewed.
- [x] Full `testDebugUnitTest :app:lintDebug :app:assembleDebug` completed successfully.

### Build verification result

`./gradlew testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon --console=plain` completed with **BUILD SUCCESSFUL** in **2m 3s**; **933 actionable tasks: 84 executed, 849 up-to-date**. App lint generated `app/build/reports/lint-results-debug.html`. This verifies JVM tests, app lint, and debug assembly on the review host; Android instrumentation tests still require a device/emulator.

## Review limitations

This is a source, dependency and build/test review, not a device profiling session. Recomposition, allocation, decoder and frame-time findings are high-confidence static risks but must be quantified with Compose compiler reports, Macrobenchmark and Perfetto on the target tablet(s). File counts are not coverage percentages; instrumented tests were inventoried but require an emulator/device to execute.