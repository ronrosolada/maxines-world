package com.maxinesworld.featurelessonplayer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.corecontent.LessonLoader
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.ProgressEventEntity
import com.maxinesworld.coredatabase.MasteryRecordDao
import com.maxinesworld.coredatabase.MasteryRecordEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.enginemastery.MasteryEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

// ─── Lesson Player ViewModel ───

data class LessonUiState(
    val isLoading: Boolean = true,
    val lesson: LessonManifest? = null,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val results: List<ActivityResult> = emptyList(),
    val showFeedback: Boolean = false,
    val feedbackText: String = "",
    val feedbackCorrect: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    private val lessonLoader: LessonLoader,
    private val progressEventDao: ProgressEventDao,
    private val masteryRecordDao: MasteryRecordDao,
    private val rewardDao: RewardDao,
    private val masteryEngine: MasteryEngine
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()
    private var childId: String = ""
    private var progressSaved = false

    fun loadLesson(lessonId: String, childId: String = "") {
        this.childId = childId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val lesson = withContext(Dispatchers.IO) {
                lessonLoader.loadLesson(lessonId)
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    lesson = lesson,
                    totalSteps = lesson?.steps?.size ?: 0,
                    error = if (lesson == null) "Could not load lesson. Please go back and try again." else null
                )
            }
        }
    }

    fun onNextStep() {
        _state.update {
            val next = it.currentStep + 1
            if (next >= it.totalSteps) {
                it.copy(currentStep = next, isComplete = true)
            } else {
                it.copy(currentStep = next, showFeedback = false)
            }
        }
        // Save progress when lesson completes
        if (_state.value.isComplete) {
            saveProgress()
        }
    }

    private fun saveProgress() {
        if (progressSaved) return
        progressSaved = true
        val lesson = _state.value.lesson ?: return
        // Only scored results count (exclude explanations)
        val scoredResults = _state.value.results.filter { it.scored }
        if (childId.isBlank() || scoredResults.isEmpty()) return

        viewModelScope.launch {
            // Record progress events for each activity
            scoredResults.forEach { result ->
                progressEventDao.insert(
                    ProgressEventEntity(
                        id = UUID.randomUUID().toString(),
                        childId = childId,
                        skillId = lesson.skillIds.firstOrNull() ?: lesson.id,
                        lessonId = lesson.id,
                        activityId = result.activityId,
                        eventType = "activity_result",
                        accuracy = if (result.correct) 1.0 else 0.0,
                        attempts = result.attempts,
                        hintsUsed = result.hintsUsed,
                        responseTimeMs = result.responseTimeMs
                    )
                )
            }

            // Update mastery for each skill
            for (skillId in lesson.skillIds) {
                val events = progressEventDao.getByChildAndSkill(childId, skillId)
                val masteryState = masteryEngine.computeMastery(events.map { e ->
                    com.maxinesworld.coremodel.ProgressEvent(e.id, e.childId, e.skillId, e.lessonId, e.activityId, e.eventType, e.accuracy, e.attempts, e.hintsUsed, e.responseTimeMs, e.timestamp)
                })
                masteryRecordDao.upsert(
                    MasteryRecordEntity(
                        id = "${childId}_$skillId",
                        childId = childId,
                        skillId = skillId,
                        state = masteryState.name,
                        accuracy = if (events.isNotEmpty()) events.map { it.accuracy }.average() else 0.0,
                        totalAttempts = events.size,
                        lastActivityAt = System.currentTimeMillis()
                    )
                )
            }

            // Grant rewards based on scored results only
            val scoredCorrect = scoredResults.count { it.correct }
            val scoredTotal = scoredResults.size
            val accuracy = if (scoredTotal > 0) scoredCorrect.toDouble() / scoredTotal else 0.0
            // Stars: 1-5 scaled by accuracy over scored steps
            val starsEarned = kotlin.math.ceil(accuracy * 5).toInt().coerceIn(1, 5)
            rewardDao.insert(
                RewardEntity(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    type = "STAR",
                    subject = lesson.subject,
                    amount = starsEarned
                )
            )
            if (accuracy >= 0.8) {
                rewardDao.insert(
                    RewardEntity(
                        id = UUID.randomUUID().toString(),
                        childId = childId,
                        type = "COIN",
                        subject = lesson.subject,
                        amount = 10
                    )
                )
            }
        }
    }

    fun onActivityResult(result: ActivityResult) {
        val isCorrect = result.correct
        val lesson = _state.value.lesson
        val step = lesson?.steps?.getOrNull(_state.value.currentStep)

        _state.update {
            it.copy(
                results = it.results + result,
                showFeedback = true,
                feedbackText = if (isCorrect)
                    step?.feedback?.correct ?: "Great job! 🎉"
                else
                    step?.feedback?.incorrect ?: "Let's try again! 💪",
                feedbackCorrect = isCorrect
            )
        }
    }
}

// ─── Lesson Player Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    lessonId: String,
    childId: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: LessonPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(lessonId, childId) {
        viewModel.loadLesson(lessonId, childId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.lesson?.title ?: "Loading...",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Teal40
                    )
                }
                state.error != null -> {
                    ErrorDisplay(state.error!!, Modifier.align(Alignment.Center))
                }
                state.isComplete -> {
                    LessonCompleteScreen(state, onComplete)
                }
                else -> {
                    LessonContent(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun LessonContent(state: LessonUiState, viewModel: LessonPlayerViewModel) {
    val lesson = state.lesson ?: return
    val step = lesson.steps.getOrNull(state.currentStep) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { (state.currentStep + 1).toFloat() / state.totalSteps },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Teal40,
            trackColor = Teal40.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Step ${state.currentStep + 1} of ${state.totalSteps}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // Character guide
        CharacterGuide(lesson.guideCharacter)
        Spacer(Modifier.height(12.dp))

        // Narration
        if (step.narrationText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Teal90)
            ) {
                Text(
                    step.narrationText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // Activity
        when (step.type) {
            "animated_explanation" -> ExplanationStep(step) {
                viewModel.onActivityResult(
                    ActivityResult(step.id, true, 1, 0, 0, scored = false)
                )
            }
            "multiple_choice", "story_comprehension", "prediction_observation_explanation" ->
                MultipleChoiceStep(step, viewModel)
            "sort_and_classify", "timeline_builder" ->
                SortStep(step, viewModel)
            "array_builder" ->
                ArrayStep(step, viewModel)
            "sentence_builder" ->
                SentenceBuilderStep(step, viewModel)
            else -> {
                // Unsupported activity type — show error, do NOT auto-pass
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "⚠️ Activity type \"${step.type}\" is not yet supported.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Warning
                        )
                        Text(
                            "This lesson step needs an engine update. Your progress is saved.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                // Still allow advancing past unsupported steps without awarding credit
                Button(
                    onClick = {
                        viewModel.onActivityResult(ActivityResult(step.id, false, 0, 0, 0))
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Skip (not scored)")
                }
            }
        }

        // Feedback overlay
        if (state.showFeedback) {
            Spacer(Modifier.height(12.dp))
            FeedbackBanner(state.feedbackText, state.feedbackCorrect) {
                viewModel.onNextStep()
            }
        }
    }
}

@Composable
private fun CharacterGuide(character: String) {
    val emoji = when (character) {
        "milo" -> "🐱🧡"
        "mira" -> "🐱💜"
        "niko" -> "🐱🩶"
        "lakan" -> "🐱🇵🇭"
        else -> "🐱"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Orange80),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            character.replaceFirstChar { it.uppercase() },
            fontWeight = FontWeight.Medium,
            color = Teal40
        )
    }
}

@Composable
private fun ExplanationStep(step: ActivityStep, onContinue: () -> Unit) {
    // Explanation steps auto-advance — they're unscored intros
    // The parent LessonContent handles showing the FeedbackBanner with Next
    // This composable just shows the narration content
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.VolumeUp,
                "Listen",
                modifier = Modifier.size(48.dp),
                tint = Teal40
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Listen to the story! 🎧",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Teal40
            )
            Spacer(Modifier.height(8.dp))
            Text(
                step.narrationText.take(100) + if (step.narrationText.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal40)
            ) {
                Text("Continue →", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun MultipleChoiceStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(1) }
    var showRetry by remember { mutableStateOf(false) }

    // Reset if step changes
    LaunchedEffect(step.id) {
        selectedIndex = null
        submitted = false
        attemptCount = 1
        showRetry = false
    }

    fun submit(index: Int) {
        val isCorrect = index == step.correctIndex
        if (isCorrect || attemptCount >= 2) {
            // Correct on first try, or second attempt — lock and submit
            submitted = true
            selectedIndex = index
            viewModel.onActivityResult(
                ActivityResult(step.id, isCorrect, attemptCount, 0, 0)
            )
        } else {
            // Wrong on first try — show retry without submitting
            attemptCount++
            selectedIndex = index
            showRetry = true
        }
    }

    Text(
        step.question,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    Spacer(Modifier.height(16.dp))

    step.options.forEachIndexed { index, option ->
        val bgColor = when {
            submitted && index == step.correctIndex -> SuccessGreen.copy(alpha = 0.2f)
            showRetry && index == selectedIndex -> ErrorRed.copy(alpha = 0.15f)
            index == selectedIndex -> Teal90
            else -> SurfaceContainer
        }
        val isDisabled = submitted || (showRetry && index != selectedIndex)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(enabled = !submitted && !(showRetry && index != selectedIndex)) {
                    submit(index)
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (index == selectedIndex) 4.dp else 2.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val circleColor = when {
                    submitted && index == step.correctIndex -> SuccessGreen
                    showRetry && index == selectedIndex -> ErrorRed
                    else -> Teal90
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (submitted && index == step.correctIndex) Text("✓", fontWeight = FontWeight.Bold, color = Color.White)
                    else if (showRetry && index == selectedIndex) Text("✗", fontWeight = FontWeight.Bold, color = Color.White)
                    else Text(('A' + index).toString(), fontWeight = FontWeight.Bold, color = Teal40)
                }
                Spacer(Modifier.width(12.dp))
                Text(option, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // Retry prompt
    if (showRetry) {
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    step.feedback?.incorrect ?: "Not quite — try once more!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    selectedIndex = null
                    showRetry = false
                }) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun SortStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    val options = step.options
    var selectedOrder by remember { mutableStateOf<List<Int>>(emptyList()) }

    Text(
        step.question,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    Spacer(Modifier.height(12.dp))

    // Show selected items in order
    if (selectedOrder.isNotEmpty()) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Your order:", fontWeight = FontWeight.Bold, color = Teal40)
                selectedOrder.forEachIndexed { idx, optionIdx ->
                    Text("${idx + 1}. ${options[optionIdx]}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // Show remaining options to pick
    Text("Tap items in the correct order:", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))

    options.indices.forEach { index ->
        if (index !in selectedOrder) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { selectedOrder = selectedOrder + index },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Amber90)
            ) {
                Text(options[index], modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // Reset button
    if (selectedOrder.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { selectedOrder = emptyList() }) {
            Text("↺ Start over")
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            // Check if the order matches the expected sequence
            // For timeline/sort, the options are listed in correct order in the JSON
            val isCorrect = selectedOrder == options.indices.toList() && selectedOrder.size == options.size
            viewModel.onActivityResult(
                ActivityResult(step.id, isCorrect, 1, 0, 0)
            )
        },
        enabled = selectedOrder.size == options.size,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal40)
    ) {
        Text(
            if (selectedOrder.size < options.size) "Select all items first (${selectedOrder.size}/${options.size})"
            else "Check my order ✓",
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ArrayStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    // Simplified array builder — shows the problem and uses multiple choice
    MultipleChoiceStep(step, viewModel)
}

@Composable
private fun SentenceBuilderStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    Text(
        step.question,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        step.options.forEach { word ->
            AssistChip(
                onClick = { },
                label = { Text(word) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Teal90
                )
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tap words to build your sentence here", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            viewModel.onActivityResult(ActivityResult(step.id, true, 1, 0, 0))
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal40)
    ) {
        Text("Submit", fontSize = 16.sp)
    }
}

@Composable
private fun FeedbackBanner(text: String, correct: Boolean, onNext: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (correct) SuccessGreen.copy(alpha = 0.15f)
            else ErrorRed.copy(alpha = 0.15f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (correct) "✅ $text" else "💭 $text",
                style = MaterialTheme.typography.bodyLarge,
                color = if (correct) SuccessGreen else ErrorRed
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (correct) SuccessGreen else Teal40
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (correct) "Next →" else "Try Next →")
            }
        }
    }
}

@Composable
private fun LessonCompleteScreen(state: LessonUiState, onComplete: () -> Unit) {
    val correct = state.results.count { it.correct }
    val total = state.results.size
    val accuracy = if (total > 0) correct.toFloat() / total else 0f

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Lesson Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Teal40
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You got $correct out of $total correct!",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(8.dp))

        // Accuracy display
        Text(
            "${(accuracy * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = if (accuracy >= 0.8f) SuccessGreen else Amber40
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { accuracy },
            modifier = Modifier.width(200.dp).height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = if (accuracy >= 0.8f) SuccessGreen else Amber40
        )
        Spacer(Modifier.height(24.dp))

        // Rewards preview
        if (accuracy >= 0.8f) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EnergyGold.copy(alpha = 0.15f))
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⭐", fontSize = 28.sp)
                        Text("+5 Stars", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪙", fontSize = 28.sp)
                        Text("+10 Coins", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
        ) {
            Text("Back to Village 🏠", fontSize = 18.sp)
        }
    }
}

@Composable
private fun ErrorDisplay(error: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("😿", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(error, color = ErrorRed, textAlign = TextAlign.Center)
    }
}
