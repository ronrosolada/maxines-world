package com.maxinesworld.featureparent

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredatabase.*
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
    val isLoading: Boolean = true
)

data class SubjectProgress(
    val subject: String,
    val label: String,
    val lessonsCompleted: Int,
    val accuracy: Float
)

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
    private val progressEventDao: ProgressEventDao
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(ParentDashboardState())
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()

    fun load(childId: String) {
        viewModelScope.launch {
            val child = childProfileDao.getById(childId)
            val starsTotal = rewardDao.getTotalByType(childId, "STAR") ?: 0
            val coinsTotal = rewardDao.getTotalByType(childId, "COIN") ?: 0
            val mastery = masteryRecordDao.getByChild(childId)
            val progress = progressEventDao.getByChild(childId)

            // Subject progress
            val bySubject = progress.groupBy { event ->
                // Extract subject from lessonId prefix (e.g., "eng-g3-m01-l01" → "english")
                val prefix = event.lessonId.substringBefore("-")
                when (prefix) {
                    "eng" -> "english"
                    "fil" -> "filipino"
                    "math" -> "mathematics"
                    "sci" -> "science"
                    "mkb" -> "makabansa"
                    "gmrc" -> "gmrc"
                    else -> prefix
                }
            }
            val subjectLabels = mapOf(
                "english" to "English", "filipino" to "Filipino",
                "mathematics" to "Math", "science" to "Science",
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

            // Recent activity
            val recentActivity = progress.takeLast(5).map { event ->
                val lessonName = event.lessonId.take(20)
                val accuracy = (event.accuracy * 100).toInt()
                "$lessonName — ${accuracy}%"
            }.reversed()

            _state.value = ParentDashboardState(
                childName = child?.name ?: "Learner",
                grade = child?.grade ?: 3,
                totalStars = starsTotal,
                totalCoins = coinsTotal,
                subjectProgress = subjectProgress,
                masterySummary = MasterySummary(mastered, developing, needsReview),
                recentActivity = recentActivity,
                isLoading = false
            )
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
                                Text("${sp.lessonsCompleted} lessons · ${(sp.accuracy * 100).toInt()}%", fontSize = 14.sp, color = Ink.copy(alpha = 0.6f))
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
                        Text("No learning activity yet — lessons completed will appear here.", modifier = Modifier.padding(16.dp), fontSize = 15.sp, color = Ink.copy(alpha = 0.5f))
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
