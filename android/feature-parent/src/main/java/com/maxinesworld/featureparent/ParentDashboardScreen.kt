package com.maxinesworld.featureparent

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.*
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredesignsystem.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
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
    val isUpdatingApp: Boolean = false,
    val updateProgress: Float = 0f,
    val updateStatusMessage: String? = null,
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
) : ViewModel() {

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

    fun downloadAndInstallUpdate(context: Context) {
        if (_state.value.isUpdatingApp) return
        _state.update {
            it.copy(
                isUpdatingApp = true,
                updateProgress = 0f,
                updateStatusMessage = "Connecting to DreamNAS update server..."
            )
        }

        viewModelScope.launch {
            val candidateEndpoints = listOf(
                "http://10.10.10.33/app-release.apk",
                "http://10.10.20.33/app-release.apk"
            )

            var downloadedFile: File? = null
            var lastError: Exception? = null

            for (endpoint in candidateEndpoints) {
                try {
                    _state.update { it.copy(updateStatusMessage = "Downloading APK from $endpoint...") }
                    val file = withContext(Dispatchers.IO) {
                        val url = URL(endpoint)
                        val conn = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 15000
                            requestMethod = "GET"
                        }
                        conn.connect()
                        if (conn.responseCode !in 200..299) {
                            throw Exception("HTTP ${conn.responseCode}")
                        }
                        val contentLength = conn.contentLength.toLong()
                        val cacheDir = context.externalCacheDir ?: context.cacheDir
                        val apkFile = File(cacheDir, "maxines_world_update.apk")
                        if (apkFile.exists()) apkFile.delete()

                        conn.inputStream.use { input ->
                            FileOutputStream(apkFile).use { output ->
                                val buffer = ByteArray(8192)
                                var totalBytes = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                    totalBytes += read
                                    if (contentLength > 0) {
                                        val progress = totalBytes.toFloat() / contentLength.toFloat()
                                        _state.update { s -> s.copy(updateProgress = progress) }
                                    }
                                }
                            }
                        }
                        conn.disconnect()
                        apkFile
                    }
                    downloadedFile = file
                    break
                } catch (e: Exception) {
                    lastError = e
                }
            }

            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0) {
                _state.update {
                    it.copy(
                        isUpdatingApp = false,
                        updateProgress = 1.0f,
                        updateStatusMessage = "Download complete! Opening package installer..."
                    )
                }
                withContext(Dispatchers.Main) {
                    launchPackageInstaller(context, downloadedFile)
                }
            } else {
                _state.update {
                    it.copy(
                        isUpdatingApp = false,
                        updateStatusMessage = "Failed to download update: ${lastError?.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    private fun launchPackageInstaller(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(childId: String, onBack: () -> Unit, viewModel: ParentDashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(childId) { viewModel.load(childId) }

    Column(Modifier.fillMaxSize()) {
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
                                val installedVersion = packageInfo?.versionName ?: "Unknown"
                                Text(
                                    "Installed: v$installedVersion · Direct update over Wi-Fi",
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
                            onClick = { viewModel.downloadAndInstallUpdate(context) },
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
