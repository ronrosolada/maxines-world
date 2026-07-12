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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredatabase.MasteryRecordEntity
import com.maxinesworld.coredesignsystem.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ParentDashboardState(
    val childName: String = "Maxine",
    val age: Int = 8,
    val grade: Int = 3,
    val totalStars: Int = 1250,
    val subjectProgress: Map<String, SubjectProgress> = mapOf(
        "english" to SubjectProgress("English", 85, 17, 20),
        "mathematics" to SubjectProgress("Math", 78, 14, 18),
        "science" to SubjectProgress("Science", 92, 22, 24)
    ),
    val totalTimeMinutes: Int = 405,
    val timeChangePercent: Int = 12,
    val mastery: MasterySummary = MasterySummary(32, 18, 6),
    val dailyLimitMinutes: Int = 120,
    val dailyUsedMinutes: Int = 75,
    val weekdayLimit: Int = 120,
    val weekendLimit: Int = 150,
    val downtimeStart: String = "19:30",
    val downtimeEnd: String = "07:00",
    val recentActivity: List<String> = listOf(
        "Completed: Fractions Basics (Math) — Today, 10:15 AM",
        "Earned 50 Stars for a perfect quiz! — Yesterday, 4:30 PM",
        "Completed: Main Idea and Details (English) — Yesterday, 11:20 AM"
    )
)

data class SubjectProgress(
    val name: String,
    val percent: Int,
    val completed: Int,
    val total: Int
)

data class MasterySummary(
    val mastered: Int,
    val developing: Int,
    val needsReview: Int
)

@HiltViewModel
class ParentDashboardViewModel @Inject constructor() : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(ParentDashboardState())
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    childId: String,
    onBack: () -> Unit,
    viewModel: ParentDashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Child header
            ChildHeader(state)
            Spacer(Modifier.height(20.dp))

            // Weekly Learning Summary
            Text("Weekly Learning Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            state.subjectProgress.values.forEach { progress ->
                SubjectProgressCard(progress)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Skills Overview
            SkillsOverview(state.mastery)
            Spacer(Modifier.height(20.dp))

            // Screen Time
            ScreenTimeCard(state)
            Spacer(Modifier.height(20.dp))

            // Recent Activity
            Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            state.recentActivity.forEach { activity ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Text(activity, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ChildHeader(state: ParentDashboardState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Teal90)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("😺", fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(state.childName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Age ${state.age} · Grade ${state.grade}", style = MaterialTheme.typography.bodyMedium)
                Text("⭐ ${state.totalStars} Stars Earned", fontWeight = FontWeight.Medium, color = EnergyGold)
            }
        }
    }
}

@Composable
private fun SubjectProgressCard(progress: SubjectProgress) {
    val progressColor = when (progress.name) {
        "English" -> StoryTreeGreen
        "Math" -> NumberMarketRed
        "Science" -> DiscoveryLabPurple
        else -> Teal40
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = progressColor.copy(alpha = 0.1f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(progress.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "${progress.percent}% Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = progressColor
                )
                LinearProgressIndicator(
                    progress = { progress.percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f)
                )
                Text(
                    "Lessons: ${progress.completed}/${progress.total}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            TextButton(onClick = {}) { Text("View Details") }
        }
    }
}

@Composable
private fun SkillsOverview(mastery: MasterySummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Skills Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                SkillStat("Mastered", mastery.mastered, SuccessGreen)
                SkillStat("Developing", mastery.developing, Amber40)
                SkillStat("Needs Review", mastery.needsReview, ErrorRed)
            }
        }
    }
}

@Composable
private fun SkillStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ScreenTimeCard(state: ParentDashboardState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Screen Time Controls", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Lock, "Edit Limits", tint = Teal40)
                }
            }

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Limit", style = MaterialTheme.typography.labelSmall)
                    Text("${state.dailyLimitMinutes / 60}h ${state.dailyLimitMinutes % 60}m", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { state.dailyUsedMinutes.toFloat() / state.dailyLimitMinutes },
                        modifier = Modifier.width(60.dp).height(6.dp),
                        color = Teal40
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Weekdays", style = MaterialTheme.typography.labelSmall)
                    Text("${state.weekdayLimit / 60}h ${state.weekdayLimit % 60}m", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Weekends", style = MaterialTheme.typography.labelSmall)
                    Text("${state.weekendLimit / 60}h ${state.weekendLimit % 60}m", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Downtime", style = MaterialTheme.typography.labelSmall)
                    Text("${state.downtimeStart}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
