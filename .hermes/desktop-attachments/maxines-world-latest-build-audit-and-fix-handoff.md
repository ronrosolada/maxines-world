# Maxine’s World Latest Build Audit and Fix Handoff

## Audit target

Repository:

```text
https://github.com/ronrosolada/maxines-world
```

Audited branch and commit:

```text
main
79872439f1dce9d7a08244d910f57c0295cf87b8
```

This is a static source audit. The repository was inspected through Glean Document Reader, including its complete Git tree and relevant source files. Gradle compilation and emulator tests were not run because no writable checkout was available.

## Executive verdict

The current application has two separate critical content-loading failures:

1. Bundled lessons cannot load because navigation supplies lesson IDs such as `eng-g3-m01-l01`, while `LessonLoader` constructs a nonexistent flat file path. The bundled manifest maps those IDs to nested paths such as `english/module-01/lesson-01.json`. 
2. NAS synchronization is not implemented. `ApiClient` is empty and `SyncWorker` returns success without fetching, validating, installing, or activating anything. 

The UI also contains correctness and layout defects:

* Sorting activities automatically pass without sorting.
* Sentence-builder controls do nothing but still pass.
* Multiple-choice answers can be submitted repeatedly.
* Unsupported activity types can be bypassed with a successful “Continue.”
* The home screen uses a fixed `420.dp` map and permanent `220.dp` sidebar.
* Several bottom-navigation destinations do nothing.
* Parent controls bypass the parent gate.
* The gold-on-white color pairing fails normal contrast requirements.
* Downloaded content would remain invisible because subjects and lesson IDs are hard-coded.

The current build should not be considered production-ready.

## Required implementation order

Implement these phases in order:

1. Repair bundled lesson resolution.
2. Make activity completion truthful.
3. Repair compact and responsive UI layouts.
4. Introduce a unified content repository.
5. Implement NAS catalog synchronization.
6. Drive the home screen from installed content.
7. Add package validation, activation, and rollback.
8. Repair parent navigation and accessibility.
9. Correct database and build configuration.
10. Add automated and device tests.

Do not start with cosmetic changes while lesson loading and activity scoring remain broken.

# Part 1: Immediate bundled-content repair

## Verified defect

`MaxinesNavGraph.kt` sends valid manifest IDs:

```kotlin
"english" -> "eng-g3-m01-l01"
"filipino" -> "fil-g3-m01-l01"
"mathematics" -> "math-g3-m01-l01"
"science" -> "sci-g3-m01-l01"
"philippine-history" -> "hist-g3-m01-l01"
```



`LessonLoader.kt` incorrectly converts the ID into:

```kotlin
content/ph-matatag/grade-3/eng-g3-m01-l01.json
```



The bundled manifest declares the real path:

```json
{
  "id": "eng-g3-m01-l01",
  "path": "content/ph-matatag/grade-3/english/module-01/lesson-01.json"
}
```



## Required fix

Replace path synthesis with manifest-based ID resolution.

Create models matching the actual bundled manifest:

```kotlin
@Serializable
data class BundledContentManifest(
    val lessons: List<BundledLessonEntry> = emptyList()
)

@Serializable
data class BundledLessonEntry(
    val id: String,
    val path: String
)
```

Replace `LessonLoader` with behavior equivalent to:

```kotlin
package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.LessonManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeContentStore: ActiveContentStore
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    private val bundledIndex: Map<String, String> by lazy {
        context.assets
            .open(BUNDLED_MANIFEST)
            .bufferedReader()
            .use { reader ->
                json.decodeFromString<BundledContentManifest>(
                    reader.readText()
                )
            }
            .lessons
            .associate { entry -> entry.id to entry.path }
    }

    suspend fun loadLesson(lessonId: String): Result<LessonManifest> {
        return runCatching {
            activeContentStore.resolveLesson(lessonId)
                ?.let(::decodeFile)
                ?: decodeBundledLesson(lessonId)
        }
    }

    private fun decodeBundledLesson(lessonId: String): LessonManifest {
        val path = bundledIndex[lessonId]
            ?: error("Lesson is not declared in bundled manifest: $lessonId")

        require(isSafeRelativePath(path)) {
            "Unsafe bundled lesson path: $path"
        }

        return context.assets.open(path).bufferedReader().use { reader ->
            json.decodeFromString<LessonManifest>(reader.readText())
        }
    }

    private fun decodeFile(file: File): LessonManifest {
        require(activeContentStore.isInsideActivePackage(file)) {
            "Lesson escaped the active package directory"
        }

        return file.bufferedReader().use { reader ->
            json.decodeFromString<LessonManifest>(reader.readText())
        }
    }

    private fun isSafeRelativePath(path: String): Boolean {
        return path.isNotBlank() &&
            !path.startsWith("/") &&
            !path.startsWith("\\") &&
            path.split('/', '\\').none { it == ".." }
    }

    private companion object {
        const val BUNDLED_MANIFEST =
            "content/ph-matatag/grade-3/manifest.json"
    }
}
```

Do not continue swallowing all exceptions and returning `null`. Use `Result` or a sealed error so the parent diagnostics screen can distinguish:

* Missing lesson ID
* Invalid manifest
* Missing asset
* Unsupported schema
* Invalid JSON
* Unsafe path
* No active package

## Required bundled-content test

```kotlin
@Test
fun everyBundledLessonIdResolvesToDeclaredPath() = runTest {
    bundledManifest.lessons.forEach { entry ->
        val lesson = lessonLoader.loadLesson(entry.id).getOrThrow()
        assertEquals(entry.id, lesson.id)
    }
}
```

Also test all five IDs currently used by navigation.

# Part 2: Unify the two bundled content layouts

## Verified defect

The repository contains two unrelated content roots:

```text
app/src/main/assets/content-packs/ph-grade3-v1/
app/src/main/assets/content/ph-matatag/grade-3/
```

The first contains slug-named lesson files and checksums. The second contains nested subject/module paths. Only the second is referenced indirectly by the current loader, and even that lookup is incorrect. 

## Required fix

Define one internal normalized model:

```kotlin
data class LessonLocation(
    val lessonId: String,
    val source: ContentSource,
    val relativePath: String,
    val packageId: String,
    val contentVersion: Int
)

enum class ContentSource {
    ACTIVE_PACKAGE,
    BUNDLED_FALLBACK
}
```

Create a single repository:

```kotlin
interface ContentRepository {
    fun observeCatalog(): Flow<InstalledCatalog>

    suspend fun getLearningDay(
        childId: String,
        localDate: LocalDate
    ): LearningDay

    suspend fun getLesson(lessonId: String): Result<LessonManifest>

    suspend fun listLessons(subjectId: String): List<LessonSummary>

    suspend fun synchronize(): ContentSyncResult
}
```

Resolution order must be:

```text
Compatible active NAS package
→ previous active package when rollback is required
→ bundled fallback package
→ controlled not-found error
```

Do not allow feature modules to open arbitrary paths.

# Part 3: Implement the NAS content pipeline

## Verified defect

The current network class contains no client implementation. 

The current worker does no work:

```kotlin
override suspend fun doWork(): Result {
    return Result.success()
}
```



Internet permission exists, but permission alone does not implement downloading. 

## Required architecture

```text
Parent-configured catalog URL
→ HTTPS catalog request
→ catalog schema validation
→ package compatibility check
→ streamed ZIP download into staging
→ expected-size verification
→ SHA-256 verification
→ safe ZIP extraction
→ package schema validation
→ lesson and asset validation
→ atomic activation
→ repository refresh
→ dynamic UI update
```

## Required catalog model

```kotlin
@Serializable
data class RemoteCatalog(
    val catalogVersion: Int,
    val channel: String,
    val packages: List<RemotePackage>
)

@Serializable
data class RemotePackage(
    val packageId: String,
    val contentVersion: Int,
    val schemaVersion: Int,
    val minimumAppVersionCode: Int,
    val requiredCapabilities: Set<String> = emptySet(),
    val educatorValidated: Boolean,
    val releaseStatus: String,
    val sizeBytes: Long,
    val sha256: String,
    val url: String
)
```

## Required API boundary

Do not leave an empty concrete class. Use a testable interface:

```kotlin
interface ContentApi {
    suspend fun fetchCatalog(catalogUrl: String): RemoteCatalog

    suspend fun downloadPackage(
        packageUrl: String,
        destination: File,
        onProgress: suspend (downloaded: Long, total: Long?) -> Unit
    )
}
```

The implementation must:

* Stream the response.
* Apply connection and read timeouts.
* Close response bodies.
* Reject unsuccessful HTTP status codes.
* Reject redirects to unsupported schemes.
* Use HTTPS in production.
* Permit LAN HTTP only in a debug-specific network security configuration.
* Avoid embedding NAS credentials in the APK.
* Never upload learner data.

## Required worker

Configure the worker to call a coordinator:

```kotlin
@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted parameters: WorkerParameters,
    private val syncCoordinator: ContentSyncCoordinator
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        return when (val result = syncCoordinator.synchronize()) {
            is ContentSyncResult.Success -> Result.success()
            is ContentSyncResult.NoUpdate -> Result.success()
            is ContentSyncResult.TransientFailure -> Result.retry()
            is ContentSyncResult.InvalidCatalog,
            is ContentSyncResult.InvalidPackage,
            is ContentSyncResult.IncompatiblePackage ->
                Result.failure(
                    workDataOf("reason" to result.userSafeReason)
                )
        }
    }
}
```

Never return success when download, checksum validation, extraction, or activation failed.

## Required Hilt WorkManager configuration

The application currently only extends `Application` with `@HiltAndroidApp`. 

Replace it with:

```kotlin
@HiltAndroidApp
class MaxinesWorldApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

Add the required imports and ensure the manifest does not initialize WorkManager with a conflicting default initializer.

## Required package directories

```text
filesDir/content/
├── staging/
├── active/
│   └── package-id/
│       └── content-version/
├── rollback/
└── quarantine/
```

## Required ZIP safety

For every ZIP entry:

```kotlin
private fun safeDestination(root: File, entryName: String): File {
    require(entryName.isNotBlank())
    require(!entryName.startsWith("/"))
    require(!entryName.startsWith("\\"))

    val canonicalRoot = root.canonicalFile
    val destination = File(canonicalRoot, entryName).canonicalFile

    require(
        destination.path == canonicalRoot.path ||
            destination.path.startsWith(
                canonicalRoot.path + File.separator
            )
    ) {
        "Unsafe ZIP entry: $entryName"
    }

    return destination
}
```

Also enforce:

* Maximum entry count
* Maximum compressed size
* Maximum extracted size
* No duplicate conflicting paths
* No symbolic-link escape
* No activation of partial extraction
* Cleanup after failure

## Required activation behavior

Activation must be atomic:

1. Download to a temporary archive.
2. Verify expected size.
3. Verify SHA-256.
4. Extract into a temporary version directory.
5. Validate the package manifest.
6. Validate every lesson and referenced asset.
7. Move the validated directory to its immutable final location.
8. Update the active-package pointer transactionally.
9. Retain the previous valid package.
10. Notify the content repository.
11. Delete stale staging data.

Installed lessons must continue working if the NAS becomes unavailable.

# Part 4: Drive the UI from the active catalog

## Verified defect

`VillageHomeScreen.kt` defines five hard-coded subjects and fixed module counts. 

`MaxinesNavGraph.kt` maps each subject directly to a single pilot lesson. 

Downloaded content would therefore remain undiscoverable even after a successful package installation.

## Required fix

Move subject and lesson selection into a ViewModel:

```kotlin
data class SubjectUiModel(
    val id: String,
    val title: String,
    val completedLessons: Int,
    val availableLessons: Int,
    val nextLessonId: String?,
    val status: SubjectStatus
)

data class VillageHomeUiState(
    val isLoading: Boolean = true,
    val subjects: List<SubjectUiModel> = emptyList(),
    val dailyProgress: Int = 0,
    val requiredDailySubjects: Int = 5,
    val contentStatus: ContentStatus = ContentStatus.Unknown,
    val error: String? = null
)
```

The screen must consume:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

Navigation should receive the selected lesson ID from repository-backed state:

```kotlin
onSubjectTap = { subjectId ->
    viewModel.openNextLesson(subjectId)
}

LaunchedEffect(Unit) {
    viewModel.navigationEvents.collect { event ->
        when (event) {
            is HomeNavigationEvent.OpenLesson ->
                navController.navigate(
                    Routes.lessonPlayer(childId, event.lessonId)
                )
        }
    }
}
```

Delete the hard-coded `when (subject)` lesson mapping after repository-driven navigation works.

# Part 5: Repair the lesson player

## Critical scoring defects

### Sorting automatically passes

The sorting UI labels rows as draggable but provides no reordering state. Its button reports unconditional success. 

### Sentence building automatically passes

The word chips have empty click handlers and Submit reports unconditional success. 

### Unsupported activities automatically pass

The fallback renderer displays the activity type and offers a button that creates a successful result. 

### Multiple-choice answers can be submitted repeatedly

Options remain clickable after feedback, and each click appends another result. 

## Required state correction

Replace the result list with one result per activity:

```kotlin
data class LessonUiState(
    val isLoading: Boolean = true,
    val lesson: LessonManifest? = null,
    val currentStep: Int = 0,
    val resultsByActivityId: Map<String, ActivityResult> = emptyMap(),
    val selectedAnswer: Int? = null,
    val showFeedback: Boolean = false,
    val feedbackText: String = "",
    val feedbackCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val error: LessonError? = null
)
```

Guard submission:

```kotlin
fun submitResult(result: ActivityResult) {
    _state.update { current ->
        val step = current.lesson
            ?.steps
            ?.getOrNull(current.currentStep)
            ?: return@update current

        if (current.showFeedback) {
            return@update current
        }

        if (result.activityId != step.id) {
            return@update current.copy(
                error = LessonError.InvalidResult(step.id)
            )
        }

        current.copy(
            resultsByActivityId =
                current.resultsByActivityId + (step.id to result),
            showFeedback = true,
            feedbackCorrect = result.correct,
            feedbackText = feedbackFor(step, result.correct)
        )
    }
}
```

For a wrong answer, provide a retry action that removes the current failed result and remains on the same step.

For a correct answer, advance exactly once.

## Multiple-choice fix

```kotlin
.clickable(
    enabled = !state.showFeedback,
    onClick = {
        viewModel.submitChoice(
            activityId = step.id,
            selectedIndex = index
        )
    }
)
```

Do not allow the Composable to decide the correct answer independently. Put validation in the ViewModel or activity engine.

## Sorting fix

Implement real ordered state:

```kotlin
data class SortActivityState(
    val orderedItemIds: List<String>
)
```

Support:

* Move up and move down buttons
* Optional drag-and-drop
* TalkBack-compatible reorder actions
* Submit only after every item is present
* Exact comparison against declared answer order or groups

If the current content schema cannot represent the correct order or classification groups, reject the activity during package validation. Do not auto-pass it.

## Sentence-builder fix

Maintain selected words:

```kotlin
var selectedWordIds by rememberSaveable(step.id) {
    mutableStateOf(emptyList<String>())
}
```

Each available word must append to the sentence. Selected words must be removable or reorderable.

Validate against structured accepted answers:

```kotlin
data class SentenceAnswer(
    val acceptedTokenOrders: List<List<String>>
)
```

Do not compare only a localized display string when stable token IDs are available.

## Unsupported-activity fix

Replace the successful fallback with a blocking error:

```kotlin
UnsupportedActivityCard(
    capability = step.type,
    onExitLesson = onBack
)
```

The activity must not count as completed.

Package validation should reject unsupported capabilities before activation.

## Lesson-completion rule

Do not mark a lesson complete merely because the step index passed the final position.

Require:

```kotlin
val requiredIds = lesson.steps
    .filter { it.required }
    .map { it.id }
    .toSet()

val completedIds = state.resultsByActivityId
    .filterValues { it.completed }
    .keys

val canComplete = completedIds.containsAll(requiredIds)
```

Assessment pass rules must be handled separately from instructional activity completion.

# Part 6: Responsive UI repair

## Verified defects

The home screen always uses a horizontal `Row`, and its quest panel always consumes `220.dp`. 

The village map is fixed at `420.dp` while containing five cards. 

A responsive `WindowProfile` utility exists but is not used by these screens. It defines compact, medium, expanded, and large-tablet breakpoints. 

Sentence chips are placed in one non-wrapping row. 

## Required home-screen layout

Use `BoxWithConstraints` and `windowProfileForWidth`:

```kotlin
@Composable
fun VillageHomeScreen(
    state: VillageHomeUiState,
    onSubjectTap: (String) -> Unit,
    onParentGate: () -> Unit,
    onProfile: () -> Unit,
    onAchievements: () -> Unit,
    onBackpack: () -> Unit
) {
    BoxWithConstraints {
        val profile = windowProfileForWidth(maxWidth)

        Scaffold(
            topBar = { VillageTopBar(state) },
            bottomBar = {
                VillageBottomBar(
                    selectedDestination = VillageDestination.Home,
                    onProfile = onProfile,
                    onAchievements = onAchievements,
                    onBackpack = onBackpack,
                    onParentGate = onParentGate
                )
            }
        ) { padding ->
            if (profile.isWide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = profile.pageMargin)
                ) {
                    SubjectList(
                        subjects = state.subjects,
                        onSubjectTap = onSubjectTap,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(24.dp))

                    DailyQuestPanel(
                        state = state,
                        modifier = Modifier
                            .widthIn(min = 240.dp, max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(profile.pageMargin),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { VillageGreeting(state) }

                    items(
                        items = state.subjects,
                        key = { it.id }
                    ) { subject ->
                        SubjectCard(subject, onSubjectTap)
                    }

                    item {
                        DailyQuestPanel(
                            state = state,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
```

Do not use a fixed-height decorative map as the only container for all subjects.

## Required sentence layout

Use `FlowRow` when available:

```kotlin
FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    words.forEach { word ->
        AssistChip(...)
    }
}
```

## Required completion-screen layout

Make the completion screen scrollable:

```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally
)
```

Do not vertically center content that may be taller than the viewport.

# Part 7: Navigation and parent-gate repair

## Verified defects

The Parents item navigates directly from child home to the parent dashboard without authentication. 

Profile, Achievements, and Backpack retain default empty callbacks. Every bottom-navigation item uses `selected = false`. 

## Required fix

Introduce a real parent gate:

```text
Child Home
→ Parent Gate
→ successful PIN or biometric validation
→ Parent Dashboard
```

Do not navigate directly from child home to the dashboard.

Add a dedicated route:

```kotlin
const val PARENT_GATE = "parent_gate/{childId}"
```

Only the gate’s successful-authentication callback may navigate to `PARENT_DASHBOARD`.

For unfinished destinations:

* Implement the destination, or
* Remove the item from the production navigation bar, or
* Display it as disabled with a clear “Coming later” label.

Do not display controls that silently do nothing.

Derive selected state from the current route.

# Part 8: Theme and accessibility repair

## Verified defect

The light theme pairs `SunshineGold` with white text. 

This pairing has insufficient contrast for normal text.

The screens also use palette constants directly instead of semantic Material color roles.

## Required fix

Use a dark foreground on gold:

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = White,
    secondary = SunshineGold,
    onSecondary = Color(0xFF2B2100),
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF251A00),
    tertiary = Orange40,
    surface = SurfaceLight,
    surfaceContainer = SurfaceContainer,
    background = SurfaceLight,
    error = ErrorRed
)
```

Verify exact contrast values with an automated checker before merging.

Use:

```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.onSurface
MaterialTheme.colorScheme.surfaceContainer
MaterialTheme.colorScheme.error
```

instead of direct palette constants for general screen elements.

Retain subject colors only when foreground contrast has been tested.

## Feedback accessibility

Add live-region semantics:

```kotlin
Modifier.semantics {
    liveRegion = LiveRegionMode.Polite
}
```

Required accessibility behavior:

* Disable answered options while feedback is shown.
* Announce whether the response was correct.
* Announce the feedback text.
* Provide a clearly labeled retry or next action.
* Maintain logical focus order.
* Support 200% font scaling.
* Provide at least `48.dp` touch targets.
* Do not rely on color alone.

# Part 9: Database and build corrections

## Room schema configuration

The Room schema directory is hard-coded to:

```text
C:/mw-room-schemas
```

while `exportSchema` is false. This prevents portable, version-controlled schema output. 

Required changes:

```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
```

```kotlin
@Database(
    entities = [
        // existing and new entities
    ],
    version = NEXT_DATABASE_VERSION,
    exportSchema = true
)
```

Commit generated schemas and add explicit migrations.

## Append-only progress

`ProgressEventDao.insert` currently uses `REPLACE`, contradicting append-only progress behavior. 

Change it to:

```kotlin
@Insert(onConflict = OnConflictStrategy.ABORT)
suspend fun insert(event: ProgressEventEntity)
```

Use a separate sync-state table or targeted sync-status update rather than replacing educational event payloads.

## Missing package persistence

Add entities for at least:

```kotlin
InstalledContentPackageEntity
ActiveContentPackageEntity
ContentSyncAttemptEntity
```

Store lesson files in the filesystem, not Room blobs.

## Foreign keys and indexes

Add explicit ownership constraints and indexes for fields queried by:

* `parentId`
* `childId`
* `lessonId`
* `skillId`
* `syncStatus`
* `localDate`
* `packageId`
* `contentVersion`

The present entities have primary keys but no documented foreign-key relationships or query indexes. 

## Windows wrapper

The repository contains `gradlew` but no `gradlew.bat`, despite Windows being a documented development environment. 

Regenerate and commit the standard Gradle wrapper scripts.

# Part 10: Required automated tests

## Build tests

Run and report:

```text
./gradlew clean
./gradlew assembleDebug
./gradlew test
./gradlew lint
```

On Windows:

```text
gradlew.bat clean assembleDebug test lint
```

Do not report a command as passing unless it was run.

## Bundled-content tests

* Every manifest lesson ID resolves.
* Every declared path exists.
* Every lesson decodes.
* Missing IDs return typed errors.
* Unsafe paths are rejected.
* All five home-screen lessons open.

## NAS tests

Use MockWebServer or the project’s equivalent.

Test:

* Valid catalog
* Invalid catalog
* Unsupported schema
* Incompatible app version
* Unsupported activity capability
* Successful streamed download
* Interrupted download
* Incorrect expected size
* Incorrect SHA-256
* Malformed ZIP
* ZIP traversal
* Missing lesson
* Missing asset
* Invalid answer key
* Atomic activation
* Rollback after failed activation
* NAS unavailable while installed content remains usable
* Worker returns retry for transient failures
* Worker never returns success after validation failure

## Lesson-player tests

* One result maximum per activity attempt.
* Repeated multiple-choice taps are ignored.
* Wrong answer stays on the current step.
* Correct answer advances once.
* Sorting cannot pass without the required arrangement.
* Sentence building cannot pass with an empty sentence.
* Unsupported activity cannot pass.
* Lesson completion requires all required activities.
* Score denominator is not inflated by repeated taps.
* Progress survives recreation and process restart.

## UI tests

Test:

```text
Compact phone portrait
Compact phone landscape
Medium-width device
Expanded tablet
Large tablet
Font scale 100%
Font scale 150%
Font scale 200%
TalkBack
Reduced motion
```

Assert:

* No clipped subject cards
* No fixed sidebar on compact screens
* Completion screen scrolls
* Sentence tokens wrap
* Exactly one navigation item is selected
* Every visible navigation item performs an action
* Parent dashboard cannot open without authentication
* Feedback is announced
* Controls meet minimum touch-target size

# Part 11: Acceptance criteria

The repair is complete only when:

* All five bundled pilot lessons open successfully.
* The app no longer constructs lesson paths from IDs.
* NAS content can be downloaded, verified, installed, activated, and opened.
* Downloaded lessons appear without an APK rebuild.
* Installed content remains usable while offline.
* Invalid packages never replace a working package.
* The previous package can be restored.
* `SyncWorker` reports truthful results.
* Sorting and sentence activities require genuine learner input.
* Unsupported activities cannot be bypassed.
* Repeated taps do not corrupt scoring.
* Compact and large layouts render without clipping.
* Parent controls require authentication.
* Visible navigation controls work.
* Room schemas are portable and exported.
* Progress events cannot be replaced silently.
* Tests and lint pass.
* `educatorValidated=false` remains enforced for unreviewed packages.

# Part 12: Required final report from the implementing LLM

After applying the fixes, report:

## Files changed

Group files by:

* Content domain
* Network
* Synchronization
* Package storage
* Database
* Navigation
* Home UI
* Lesson player
* Theme and accessibility
* Tests
* Documentation

## Validation results

Use only:

```text
PASS
FAIL
NOT RUN
```

Report:

* Debug build
* Unit tests
* Lint
* Room migration tests
* Content-manifest tests
* NAS download tests
* ZIP-security tests
* Compose UI tests
* Instrumentation tests
* Offline smoke test
* Rollback smoke test

## Known limitations

List every remaining placeholder, unsupported activity, missing asset, unreviewed lesson, test gap, and device limitation.

## Git status

Provide:

```text
Branch
Commit hash
Commit message
Working-tree status
```

Do not claim the repository was fixed if only this handoff was produced.

## Audit conclusion

The immediate “content not loading” problem is caused by both an incorrect bundled lesson resolver and a completely stubbed NAS synchronization path. The broken UI is not a single styling issue: fixed-width layouts, inactive navigation, unauthenticated parent access, misleading activity controls, unconditional success paths, and duplicate-result handling all require correction.

Implement the content resolver and truthful activity state first. Responsive styling alone will not make the current build safe or functional.


---

## Sources

- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/app/src/main/java/com/maxinesworld/app/MaxinesNavGraph.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-content/src/main/java/com/maxinesworld/corecontent/LessonLoader.kt)
- [.json](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/app/src/main/assets/content/ph-matatag/grade-3/manifest.json)
- [Apiclient.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-network/src/main/java/com/maxinesworld/corenetwork/ApiClient.kt)
- [Syncworker.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/engine-sync/src/main/java/com/maxinesworld/enginesync/SyncWorker.kt)
- [api.github.com](https://api.github.com/repos/ronrosolada/maxines-world/git/trees/main?recursive=1)
- [Androidmanifest.xml](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/app/src/main/AndroidManifest.xml)
- [Maxinesworldapp.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/app/src/main/java/com/maxinesworld/app/MaxinesWorldApp.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/feature-child-home/src/main/java/com/maxinesworld/featurechildhome/VillageHomeScreen.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/feature-lesson-player/src/main/java/com/maxinesworld/featurelessonplayer/LessonPlayerScreen.kt)
- [Windowprofile.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/WindowProfile.kt)
- [.kt](https://raw.githubusercontent.com/ronrosolada/maxines-world/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-design-system/src/main/java/com/maxinesworld/coredesignsystem/theme/Theme.kt)
- [.kts](https://github.com/ronrosolada/maxines-world/blob/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-database/build.gradle.kts)
- [.kt](https://github.com/ronrosolada/maxines-world/blob/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-database/src/main/java/com/maxinesworld/coredatabase/MaxinesDatabase.kt)
- [.kt](https://github.com/ronrosolada/maxines-world/blob/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-database/src/main/java/com/maxinesworld/coredatabase/Daos.kt)
- [.kt](https://github.com/ronrosolada/maxines-world/blob/79872439f1dce9d7a08244d910f57c0295cf87b8/android/core-database/src/main/java/com/maxinesworld/coredatabase/Entities.kt)
- [api.github.com](https://api.github.com/repos/ronrosolada/maxines-world/git/trees/79872439f1dce9d7a08244d910f57c0295cf87b8?recursive=1)
- [docs/02-milestones-and-risks.md at 79872439f1dce9d7a08244d910f57c0295cf87b8 · ronrosolada/maxines-world](https://github.com/ronrosolada/maxines-world/blob/79872439f1dce9d7a08244d910f57c0295cf87b8/docs/02-milestones-and-risks.md)
