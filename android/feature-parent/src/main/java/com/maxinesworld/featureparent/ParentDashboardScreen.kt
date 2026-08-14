package com.maxinesworld.featureparent

import android.content.Context
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
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredesignsystem.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

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
    val isLoading: Boolean = true
)

data class SubjectProgress(
    val subject: String,
    val label: String,
    val lessonsCompleted: Int,
    val accuracy: Float
)

/** One feed row per completed lesson, never one row per assessment question. */
internal fun recentActivityLabels(
    completions: List<LessonCompletionEntity>,
    titleForLesson: (String) -> String,
): List<String> = completions
    .sortedWith(
        compareByDescending<LessonCompletionEntity> { it.completedAtEpochMillis }
            .thenByDescending { it.id },
    )
    .distinctBy { it.lessonId }
    .take(5)
    .map { completion ->
        "${titleForLesson(completion.lessonId)} — ${(completion.accuracy * 100).toInt()}%"
    }

/** Human-readable app version line shown to parents (e.g. "Maxine's World v0.33.0"). */
internal fun appVersionLabel(versionName: String): String =
    if (versionName.isBlank()) "Maxine's World" else "Maxine's World v$versionName"

/** Read the installed app version; blank on any failure so the footer never crashes. */
internal fun resolveAppVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty()

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
    private val lessonCompletionDao: LessonCompletionDao,
    private val moduleCatalog: ModuleCatalog,
    private val godModeManager: GodModeManager,
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(ParentDashboardState())
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()

    fun load(childId: String) {
        viewModelScope.launch {
            val godModeEnabled = godModeManager.isEnabled()
            val child = childProfileDao.getById(childId)
            val starsTotal = rewardDao.getTotalByType(childId, "STAR") ?: 0
            val coinsTotal = rewardDao.getTotalByType(childId, "COIN") ?: 0
            val mastery = masteryRecordDao.getByChild(childId)
            val progress = progressEventDao.getByChild(childId)
            val completions = lessonCompletionDao.getRecentByChild(childId, limit = 5)

            // Subject progress
            val bySubject = progress.groupBy { event ->
                subjectKeyForLessonId(event.lessonId) ?: event.lessonId.substringBefore("-")
            }
            val subjectLabels = mapOf(
                "english" to "English", "filipino" to "Filipino",
                "mathematics" to "Math", "science" to "Science",
                "araling-panlipunan" to "Araling Panlipunan",
                "makabansa" to "Makabansa", "gmrc" to "GMRC"
            )

            val subjectProgress = bySubject.map { (subj, events) ->
                SubjectProgress(
                    subject = subj,
                    label = subjectLabels[subj] ?: subj,
                    lessonsCompleted = events.map { it.lessonId }.distinct().size,
                    accuracy = if (events.isNotEmpty()) events.map { it.accuracy }.average().toFloat() else 0f
                )
            }.sortedByDescending { it.lessonsCompleted }

            // Mastery summary
            val mastered = mastery.count { it.state == "MASTERED" }
            val developing = mastery.count { it.state == "DEVELOPING" }
            val needsReview = mastery.count { it.state == "NEEDS_REVIEW" || it.state == "NOT_STARTED" }

            // Recent activity — friendly lesson titles, never raw schema IDs
            // (adversarial UX review #30: "mathematics-g3-m01-d" means nothing
            // to a parent).
            val titleByLessonId = mutableMapOf<String, String>()
            suspend fun friendlyTitle(lessonId: String): String {
                titleByLessonId[lessonId]?.let { return it }
                val subject = subjectKeyForLessonId(lessonId)
                val title = subject?.let { s ->
                    moduleCatalog.modulesFor(s).asSequence()
                        .flatMap { it.lessons.asSequence() }
                        .firstOrNull { it.lessonId == lessonId }?.title
                } ?: "Lesson"
                titleByLessonId[lessonId] = title
                return title
            }
            completions
                .map { it.lessonId }
                .distinct()
                .forEach { lessonId -> friendlyTitle(lessonId) }
            val recentActivity = recentActivityLabels(completions) { lessonId ->
                titleByLessonId[lessonId] ?: "Lesson"
            }

            _state.value = ParentDashboardState(
                childName = child?.name ?: "Learner",
                grade = child?.grade ?: 3,
                totalStars = starsTotal,
                totalCoins = coinsTotal,
                subjectProgress = subjectProgress,
                masterySummary = MasterySummary(mastered, developing, needsReview),
                recentActivity = recentActivity,
                streakDays = longestStreak(localDatesFromEpochMillis(progress.map { it.timestamp })),
                godModeEnabled = godModeEnabled,
                isLoading = false
            )
        }
    }

    fun setGodModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            godModeManager.setEnabled(enabled)
            _state.update { it.copy(godModeEnabled = enabled) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(childId: String, onBack: () -> Unit, viewModel: ParentDashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(childId) { viewModel.load(childId) }

    Column(Modifier.fillMaxSize()) {
        // Teal nav rail header
        TopAppBar(
            title = { Text("Parent Dashboard", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = VillageTeal, titleContentColor = Color.White, navigationIconContentColor = Color.White)
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VillageTeal)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            onCheckedChange = viewModel::setGodModeEnabled,
                            modifier = Modifier.semantics {
                                contentDescription = if (state.godModeEnabled) "God mode enabled" else "God mode disabled"
                            },
                        )
                    }
                }

                // App version — helps parents report issues with the right release.
                val context = LocalContext.current
                val versionName = remember { resolveAppVersionName(context) }
                Text(
                    appVersionLabel(versionName),
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = 0.45f),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.height(32.dp))
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
