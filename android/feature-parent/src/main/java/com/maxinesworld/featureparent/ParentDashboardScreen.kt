package com.maxinesworld.featureparent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coremodel.currentLearningStreak
import com.maxinesworld.coremodel.localLearningDates
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.BadgeLoader
import com.maxinesworld.corenetwork.AppUpdateManager
import com.maxinesworld.corenetwork.AppUpdateResult
import com.maxinesworld.corenetwork.VideoPrefetchManager
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** LAN / guest-VLAN endpoints serving self-hosted update APKs, tried in order. */
private val APP_UPDATE_ENDPOINTS = listOf(
    "http://10.10.10.33/app-release.apk",
    "http://10.10.20.33/app-release.apk",
)

/** Number of feed rows shown in Recent Activity. */
internal const val RECENT_ACTIVITY_LIMIT = 5

/** Fallback child id when the dashboard is opened without a profile (defensive parity). */
internal const val DEFAULT_CHILD_ID = "default_child"

private val SUBJECT_LABELS = mapOf(
    "english" to "English", "filipino" to "Filipino",
    "mathematics" to "Math", "science" to "Science",
    "araling-panlipunan" to "Araling Panlipunan",
    "makabansa" to "Makabansa", "gmrc" to "GMRC"
)

data class ParentDashboardState(
    val childName: String = "",
    val grade: Int = 3,
    val totalStars: Int = 0,
    val totalCoins: Int = 0,
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val masterySummary: MasterySummary = MasterySummary(),
    val recentActivity: List<String> = emptyList(),
    /** Consecutive-day learning streak in the child's local timezone. */
    val streakDays: Int = 0,
    val godModeEnabled: Boolean = false,
    val isUpdatingApp: Boolean = false,
    val updateProgress: Float = 0f,
    val updateStatusMessage: String? = null,
    val storageUsedMb: Float = 0f,
    val isPreloadingMedia: Boolean = false,
    val prefetchStatusMessage: String? = null,
    val accreditedWatchSeconds: Int = 0,
    val passedVideoCount: Int = 0,
    val filipinoProficiency: String = "BEGINNER",
    val foundationsProgress: List<FoundationsProgress> = emptyList(),
    val isLoading: Boolean = true
)

data class FoundationsProgress(val label: String, val completed: Int, val total: Int)

private val FOUNDATIONS_GROUPS = listOf(
    "Greetings & Introductions" to 1..4,
    "Family & Everyday Needs" to 5..8,
    "School Words, Colors & Numbers" to 9..14,
    "Daily Life & Stories" to 15..20,
    "Sounds & Early Reading" to 21..24,
)

data class SubjectProgress(
    val subject: String,
    val label: String,
    val lessonsCompleted: Int,
    val accuracy: Float
)

/** One feed row per watched video whose memory check was passed. */
internal fun recentActivityLabels(
    watched: List<VideoWatchLedgerEntity>,
    titleForMedia: (String) -> String,
): List<String> = watched
    .filter { it.quizPassed }
    .sortedByDescending { it.firstPassedAtEpochMillis ?: it.lastWatchedAtEpochMillis }
    .distinctBy { it.mediaId }
    .take(RECENT_ACTIVITY_LIMIT)
    .map { entry ->
        "${titleForMedia(entry.mediaId)} — ${(entry.bestQuizScore * 100).toInt()}%"
    }

/** Resolve both current full subject IDs and legacy abbreviated IDs. */
internal fun subjectKeyForLessonId(lessonId: String): String? {
    val prefix = lessonId.substringBefore("-")
    return when {
        lessonId.startsWith("araling-panlipunan-g3-") -> "araling-panlipunan"
        prefix == "english" || prefix == "eng" -> "english"
        prefix == "filipino" || prefix == "fil" -> "filipino"
        prefix == "mathematics" || prefix == "math" -> "mathematics"
        prefix == "science" || prefix == "sci" -> "science"
        prefix == "makabansa" || prefix == "mkb" -> "makabansa"
        prefix == "gmrc" -> "gmrc"
        else -> null
    }
}

data class MasterySummary(
    val mastered: Int = 0,
    val developing: Int = 0,
    val needsReview: Int = 0
)

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val childProfileDao: ChildProfileDao,
    private val rewardDao: RewardDao,
    private val masteryRecordDao: MasteryRecordDao,
    private val progressEventDao: ProgressEventDao,
    private val mediaLibrary: MediaLibrary,
    private val godModeManager: GodModeManager,
    private val badgeLoader: BadgeLoader,
    private val collectedBadgeDao: CollectedBadgeDao,
    private val videoWatchLedgerDao: VideoWatchLedgerDao,
    private val videoPrefetchManager: VideoPrefetchManager,
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    /**
     * DB-independent UI state (update / prefetch / storage / god-mode toggle). Kept separate
     * from the Room-derived dashboard snapshot so imperative actions can patch their own
     * fields without fighting the reactive pipeline.
     */
    private data class UiExtras(
        val godModeEnabled: Boolean = false,
        val isUpdatingApp: Boolean = false,
        val updateProgress: Float = 0f,
        val updateStatusMessage: String? = null,
        val storageUsedMb: Float = 0f,
        val isPreloadingMedia: Boolean = false,
        val prefetchStatusMessage: String? = null,
    )

    /** Snapshot of the Room-backed tables the dashboard derives its numbers from. */
    private data class AcademicSnapshot(
        val child: ChildProfileEntity?,
        val rewards: List<RewardEntity>,
        val mastery: List<MasteryRecordEntity>,
        val progress: List<ProgressEventEntity>,
    )

    private data class ActivitySnapshot(
        val watched: List<VideoWatchLedgerEntity>,
        val accreditedSeconds: Int,
        val passedMediaIds: List<String>,
    )

    private val extras = MutableStateFlow(UiExtras())

    /** The child whose reactive dashboard pipeline is currently wired, if any. */
    private var observedChildId: String? = null

    private val _state = MutableStateFlow(ParentDashboardState(isLoading = true))
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()

    val availableBadges = MutableStateFlow<List<CollectibleBadge>>(emptyList())

    /** Driven by [CollectedBadgeDao.observeBadgeIdsByChild]; awards/revokes propagate automatically. */
    private val _earnedBadgeIds = MutableStateFlow(emptySet<String>())
    val earnedBadgeIds: StateFlow<Set<String>> = _earnedBadgeIds.asStateFlow()

    /**
     * Starts observing this child's data. Every stat on the dashboard now updates in
     * real time as Room rows change (rewards, completions, watch ledger, stickers);
     * no manual reload is needed while the screen is open.
     */
    fun load(childId: String) {
        if (observedChildId == childId) return
        observedChildId = childId
        viewModelScope.launch {
            extras.update {
                it.copy(
                    godModeEnabled = runCatching { godModeManager.isEnabledNow(childId) }.getOrDefault(false),
                    storageUsedMb = calculateStorageUsedMb(),
                )
            }
            availableBadges.value = runCatching { badgeLoader.loadAll() }.getOrDefault(emptyList())

            // Sticker stream drives earnedBadgeIds directly, so parent awards and revocations
            // propagate to every collector without manual re-reads.
            collectedBadgeDao.observeBadgeIdsByChild(childId)
                .catch { emit(emptyList()) }
                .onEach { badgeIds -> _earnedBadgeIds.value = badgeIds.toSet() }
                .launchIn(viewModelScope)

            val titleByMediaId: Map<String, String> = runCatching {
                mediaLibrary.getCatalog().media.associate { it.mediaId to it.title }
            }.getOrDefault(emptyMap())

            val academic = combine(
                childProfileDao.observeById(childId),
                rewardDao.observeByChild(childId),
                masteryRecordDao.observeByChild(childId),
                progressEventDao.observeByChild(childId),
            ) { child, rewards, mastery, progress ->
                AcademicSnapshot(child, rewards, mastery, progress)
            }

            val activity = combine(
                videoWatchLedgerDao.observeLedger(childId),
                videoWatchLedgerDao.observeTotalAccreditedSeconds(childId),
                videoWatchLedgerDao.observePassedMediaIds(childId),
            ) { watched, accreditedSeconds, passedMediaIds ->
                ActivitySnapshot(watched, accreditedSeconds, passedMediaIds)
            }

            combine(academic, activity, extras) { a, act, ui ->
                buildDashboardState(ui, a, act) { mediaId -> titleByMediaId[mediaId] ?: "Video lesson" }
            }
                .catch { emit(ParentDashboardState(isLoading = false)) }
                .onEach { dashboardState -> _state.value = dashboardState }
                .launchIn(viewModelScope)
        }
    }

    fun onUpdateProficiency(proficiency: String) {
        if (proficiency !in setOf("BEGINNER", "INTERMEDIATE", "ADVANCED")) return
        val childId = observedChildId ?: return
        viewModelScope.launch {
            val child = childProfileDao.getById(childId) ?: return@launch
            if (child.filipinoProficiency != proficiency) {
                childProfileDao.upsert(child.copy(filipinoProficiency = proficiency))
            }
        }
    }

    /** Pure projection of Room snapshots + imperative UI extras into screen state. */
    private fun buildDashboardState(
        ui: UiExtras,
        academic: AcademicSnapshot,
        activity: ActivitySnapshot,
        titleForMedia: (String) -> String,
    ): ParentDashboardState {
        val child = academic.child

        // Subject progress
        val bySubject = academic.progress.groupBy { event ->
            subjectKeyForLessonId(event.lessonId) ?: event.lessonId.substringBefore("-")
        }
        val subjectProgress = bySubject.map { (subj, events) ->
            SubjectProgress(
                subject = subj,
                label = SUBJECT_LABELS[subj] ?: subj,
                lessonsCompleted = events.map { it.lessonId }.distinct().size,
                accuracy = if (events.isNotEmpty()) events.map { it.accuracy }.average().toFloat() else 0f
            )
        }.sortedByDescending { it.lessonsCompleted }

        // Mastery summary
        val mastered = academic.mastery.count { it.state == "MASTERED" }
        val developing = academic.mastery.count { it.state == "DEVELOPING" }
        val needsReview = academic.mastery.count { it.state == "NEEDS_REVIEW" || it.state == "NOT_STARTED" }
        val completedFoundationIds = academic.progress.map { it.lessonId }
            .filter { it.startsWith("filipino-foundations-") }.toSet()
        val foundationsProgress = FOUNDATIONS_GROUPS.map { (label, lessonNumbers) ->
            FoundationsProgress(
                label = label,
                completed = lessonNumbers.count { number ->
                    "filipino-foundations-${number.toString().padStart(2, '0')}" in completedFoundationIds
                },
                total = lessonNumbers.count(),
            )
        }

        return ParentDashboardState(
            childName = child?.name ?: "Learner",
            grade = child?.grade ?: 3,
            totalStars = academic.rewards.filter { it.type == "STAR" }.sumOf { it.amount },
            totalCoins = academic.rewards.filter { it.type == "COIN" }.sumOf { it.amount },
            subjectProgress = subjectProgress,
            masterySummary = MasterySummary(mastered, developing, needsReview),
            recentActivity = recentActivityLabels(activity.watched, titleForMedia),
            streakDays = currentLearningStreak(
                localDates = localLearningDates(academic.progress.map { it.timestamp }, ZoneId.systemDefault()),
                today = LocalDate.now(),
            ),
            godModeEnabled = ui.godModeEnabled,
            isUpdatingApp = ui.isUpdatingApp,
            updateProgress = ui.updateProgress,
            updateStatusMessage = ui.updateStatusMessage,
            storageUsedMb = ui.storageUsedMb,
            isPreloadingMedia = ui.isPreloadingMedia,
            prefetchStatusMessage = ui.prefetchStatusMessage,
            accreditedWatchSeconds = activity.accreditedSeconds,
            passedVideoCount = activity.passedMediaIds.size,
            filipinoProficiency = child?.filipinoProficiency ?: "BEGINNER",
            foundationsProgress = foundationsProgress,
            isLoading = false
        )
    }

    fun awardSticker(childId: String, badge: CollectibleBadge) {
        viewModelScope.launch {
            val actualChildId = childId.ifBlank { DEFAULT_CHILD_ID }
            collectedBadgeDao.insert(
                CollectedBadgeEntity(
                    id = "${actualChildId}_${badge.id}",
                    childId = actualChildId,
                    badgeId = badge.id,
                    biome = badge.biome,
                    earnedDate = "parent_awarded",
                    earnedAtEpochMillis = System.currentTimeMillis()
                )
            )
            // earnedBadgeIds refreshes via collectedBadgeDao.observeBadgeIdsByChild.
        }
    }

    fun revokeSticker(childId: String, badgeId: String) {
        viewModelScope.launch {
            val actualChildId = childId.ifBlank { DEFAULT_CHILD_ID }
            // Actually delete the row — previously this only re-read the earned set,
            // so revoked stickers reappeared on every reload.
            collectedBadgeDao.deleteByChildAndBadgeId(actualChildId, badgeId)
            // earnedBadgeIds refreshes via collectedBadgeDao.observeBadgeIdsByChild.
        }
    }

    fun setGodModeEnabled(childId: String, enabled: Boolean) {
        viewModelScope.launch {
            godModeManager.setEnabled(childId, enabled)
            extras.update { it.copy(godModeEnabled = enabled) }
        }
    }

    fun clearMediaCache() {
        videoPrefetchManager.clearStorage()
        val storageMb = calculateStorageUsedMb()
        extras.update { it.copy(storageUsedMb = storageMb, prefetchStatusMessage = "Storage cleared") }
    }

    fun prefetchMedia(count: Int = 3) {
        if (extras.value.isPreloadingMedia) return
        viewModelScope.launch {
            extras.update { it.copy(isPreloadingMedia = true, prefetchStatusMessage = "Prefetching next $count video lessons...") }
            val countPrefetched = videoPrefetchManager.prefetchNextVideos(count)
            val storageMb = calculateStorageUsedMb()
            val message = if (countPrefetched > 0) {
                "Downloaded $countPrefetched lesson(s) for offline use"
            } else {
                "Videos are already cached or network unavailable"
            }
            extras.update {
                it.copy(
                    isPreloadingMedia = false,
                    storageUsedMb = storageMb,
                    prefetchStatusMessage = message
                )
            }
        }
    }

    private fun calculateStorageUsedMb(): Float {
        val bytes = videoPrefetchManager.getStorageUsedBytes()
        return bytes / (1024f * 1024f)
    }

    /**
     * Delegates the download/verify pipeline to [AppUpdateManager] and only translates the
     * outcome into UI state. The screen itself contains no networking or installer logic.
     */
    fun downloadAndInstallUpdate() {
        if (extras.value.isUpdatingApp) return
        extras.update {
            it.copy(
                isUpdatingApp = true,
                updateProgress = 0f,
                updateStatusMessage = "Connecting to DreamNAS update server..."
            )
        }

        viewModelScope.launch {
            when (val result = appUpdateManager.downloadVerifiedApk(
                candidateEndpoints = APP_UPDATE_ENDPOINTS,
                onStatus = { message -> extras.update { it.copy(updateStatusMessage = message) } },
                onProgress = { fraction -> extras.update { it.copy(updateProgress = fraction) } },
            )) {
                is AppUpdateResult.ReadyToInstall -> {
                    extras.update {
                        it.copy(
                            isUpdatingApp = false,
                            updateProgress = 1.0f,
                            updateStatusMessage = "Download complete! Opening package installer...",
                        )
                    }
                    appUpdateManager.install(result.apkFile)
                }
                is AppUpdateResult.Rejected -> extras.update {
                    it.copy(isUpdatingApp = false, updateStatusMessage = result.reason)
                }
                is AppUpdateResult.Failed -> extras.update {
                    it.copy(isUpdatingApp = false, updateStatusMessage = result.reason)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(childId: String, onBack: () -> Unit, viewModel: ParentDashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val availableBadges by viewModel.availableBadges.collectAsStateWithLifecycle()
    val earnedBadgeIds by viewModel.earnedBadgeIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAwardStickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(childId) { viewModel.load(childId) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Parent Dashboard", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = VillageTeal, titleContentColor = White, navigationIconContentColor = White)
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VillageTeal)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Cream)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Child info card
                Card(colors = CardDefaults.cardColors(containerColor = VillageTeal.copy(alpha = 0.08f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(state.childName, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ink)
                            Text("Grade ${state.grade}", fontSize = 16.sp, color = Ink.copy(alpha = 0.6f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatBadge("Star", state.totalStars, SunshineGold)
                            StatBadge("Coin", state.totalCoins, VillageTeal)
                        }
                    }
                }

                FilipinoLanguageLevelCard(state.filipinoProficiency, viewModel::onUpdateProficiency)
                FilipinoFoundationsProgressCard(state.foundationsProgress)
                CaregiverPhrasesDeck()

                // App Update Card for LAN & Guest VLAN
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VillageTeal.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = VillageTeal)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "App Updates (Local DreamNAS)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Ink
                                )
                                val packageInfo = remember(context) {
                                    runCatching {
                                        context.packageManager.getPackageInfo(context.packageName, 0)
                                    }.getOrNull()
                                }
                                @Suppress("DEPRECATION")
                                val installedVersionCode = runCatching {
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                                }.getOrNull()
                                val installedVersion = packageInfo?.versionName ?: "Unknown"
                                Text(
                                    "Installed: v$installedVersion (code ${installedVersionCode ?: "?"}) · Direct update over Wi-Fi",
                                    fontSize = 13.sp,
                                    color = Ink.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (state.isUpdatingApp) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { state.updateProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = VillageTeal,
                                trackColor = VillageTeal.copy(alpha = 0.2f),
                            )
                        }

                        if (state.updateStatusMessage != null) {
                            Text(
                                state.updateStatusMessage!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VillageTeal
                            )
                        }

                        Button(
                            onClick = { viewModel.downloadAndInstallUpdate() },
                            enabled = !state.isUpdatingApp,
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (state.isUpdatingApp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text(
                                    if (state.isUpdatingApp) "Downloading Update..." else "Check & Install Latest Update",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Manual Sticker Awarding Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SunshineGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = SunshineGold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Manual Sticker Rewards",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Ink
                                )
                                Text(
                                    "Unlocked: ${earnedBadgeIds.size} of ${availableBadges.size} Wildlife Stickers",
                                    fontSize = 13.sp,
                                    color = Ink.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Button(
                            onClick = { showAwardStickerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SunshineGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Ink)
                                Text(
                                    "Award Animal Sticker to Maxine",
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                            }
                        }
                    }
                }

                // Mastery summary
                Card {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MasteryChip("Mastered", state.masterySummary.mastered, SuccessGreen)
                        MasteryChip("Developing", state.masterySummary.developing, SkyBlue)
                        MasteryChip("Needs Review", state.masterySummary.needsReview, Coral)
                    }
                }

                // Subject progress
                Text("Subject Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                state.subjectProgress.forEach { sp ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sp.label, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Ink)
                                val lessonWord = if (sp.lessonsCompleted == 1) "lesson" else "lessons"
                                Text("${sp.lessonsCompleted} $lessonWord · ${(sp.accuracy * 100).toInt()}% accuracy", fontSize = 14.sp, color = Ink.copy(alpha = 0.6f))
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { sp.accuracy.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = VillageTeal, trackColor = VillageTeal.copy(alpha = 0.15f)
                            )
                        }
                    }
                }

                // Recent activity
                if (state.recentActivity.isNotEmpty()) {
                    Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            state.recentActivity.forEach { activity ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Icon(Icons.Default.CheckCircle, "Done", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(activity, fontSize = 14.sp, color = Ink.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Text("No learning activity yet — completed lessons will appear here.", modifier = Modifier.padding(16.dp), fontSize = 15.sp, color = Ink.copy(alpha = 0.5f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Day streak — consecutive local days with learning activity
                if (state.streakDays > 0) {
                    Card(colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.08f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, "Streak", tint = Coral, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("${state.streakDays} day streak!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Coral)
                                Text("Keep learning every day to grow your Playroom", fontSize = 14.sp, color = Ink.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                // Accredited Video Learning Stats Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(VillageTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = VillageTeal)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Accredited Video Learning",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Ink
                                )
                                val watchMinutes = state.accreditedWatchSeconds / 60
                                Text(
                                    "$watchMinutes mins accredited · ${state.passedVideoCount} video checks passed",
                                    fontSize = 13.sp,
                                    color = Ink.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Smart Media Storage Management
                Text("Offline Video Storage", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    "Cached Video Lessons",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Ink,
                                )
                                Text(
                                    "%.1f MB on device".format(state.storageUsedMb),
                                    fontSize = 14.sp,
                                    color = Ink.copy(alpha = 0.6f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.prefetchMedia(3) },
                                    enabled = !state.isPreloadingMedia,
                                    colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    if (state.isPreloadingMedia) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Pre-cache Next 3", fontSize = 13.sp, color = Color.White)
                                    }
                                }
                                if (state.storageUsedMb > 0f) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearMediaCache() },
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Free Space", fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        val statusMsg = state.prefetchStatusMessage
                        if (statusMsg != null) {
                            Text(
                                statusMsg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VillageTeal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.10f))) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("God mode", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                            Text(
                                "Unlock Playground, every sticker, and all sanctuary rewards for this child. Existing progress is not changed.",
                                fontSize = 14.sp,
                                color = Ink.copy(alpha = 0.65f),
                            )
                        }
                        Switch(
                            checked = state.godModeEnabled,
                            onCheckedChange = { enabled -> viewModel.setGodModeEnabled(childId, enabled) },
                            modifier = Modifier.semantics {
                                contentDescription = if (state.godModeEnabled) "God mode enabled" else "God mode disabled"
                            },
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showAwardStickerDialog) {
        AwardStickerDialog(
            badges = availableBadges,
            earnedBadgeIds = earnedBadgeIds,
            onDismiss = { showAwardStickerDialog = false },
            onAward = { badge ->
                viewModel.awardSticker(childId, badge)
                Toast.makeText(context, "Awarded ${badge.name} sticker to Maxine.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AwardStickerDialog(
    badges: List<CollectibleBadge>,
    earnedBadgeIds: Set<String>,
    onDismiss: () -> Unit,
    onAward: (CollectibleBadge) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Award Sticker to Child", fontWeight = FontWeight.Bold, color = Ink)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Text(
                    "Select a wildlife sticker to unlock immediately:",
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sortedBadges = badges.sortedWith(
                        compareBy<CollectibleBadge> { it.id in earnedBadgeIds }
                            .thenBy { it.name }
                    )

                    items(sortedBadges.size) { index ->
                        val badge = sortedBadges[index]
                        val isEarned = badge.id in earnedBadgeIds

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEarned) VillageTeal.copy(alpha = 0.08f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEarned) VillageTeal.copy(alpha = 0.3f) else Ink.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isEarned) VillageTeal.copy(alpha = 0.15f) else SunshineGold.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Pets,
                                        contentDescription = null,
                                        tint = if (isEarned) VillageTeal else SunshineGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        badge.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Ink
                                    )
                                    Text(
                                        "${badge.biome.replace('_', ' ').replaceFirstChar(Char::titlecase)} · ${badge.title}",
                                        fontSize = 12.sp,
                                        color = Ink.copy(alpha = 0.6f)
                                    )
                                }

                                if (isEarned) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = VillageTeal.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            "Unlocked",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VillageTeal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { onAward(badge) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SunshineGold),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Award",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Ink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold, color = VillageTeal)
            }
        }
    )
}

@Composable
private fun FilipinoLanguageLevelCard(selected: String, onSelected: (String) -> Unit) {
    val choices = listOf(
        "BEGINNER" to "Baguhan / Zero-Beginner",
        "INTERMEDIATE" to "May Kaunti / Some Basics",
        "ADVANCED" to "Handa sa Baitang 3 / Grade Ready",
    )
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filipino Language Level", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Text("Choose the level that best describes your child today.", fontSize = 13.sp, color = Ink.copy(alpha = 0.65f))
            choices.forEach { (value, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == value, onClick = { onSelected(value) })
                    Text(label, modifier = Modifier.weight(1f), color = Ink, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun FilipinoFoundationsProgressCard(groups: List<FoundationsProgress>) {
    val completed = groups.sumOf { it.completed }
    val total = groups.sumOf { it.total }.coerceAtLeast(1)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Filipino Foundations Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Text("$completed of 24 Pre-A1 micro-lessons complete", fontSize = 14.sp, color = Ink.copy(alpha = 0.65f))
            LinearProgressIndicator(
                progress = { completed.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = VillageTeal, trackColor = VillageTeal.copy(alpha = 0.15f),
            )
            groups.forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(group.label, fontSize = 14.sp, color = Ink)
                    Text("${group.completed}/${group.total}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VillageTeal)
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
        Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun MasteryChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
        Text(label, fontSize = 13.sp, color = Ink.copy(alpha = 0.6f))
    }
}
