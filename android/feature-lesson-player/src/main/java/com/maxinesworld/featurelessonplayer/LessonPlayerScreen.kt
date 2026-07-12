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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val lessonLoader: LessonLoader
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    fun loadLesson(lessonId: String) {
        val lesson = lessonLoader.loadLesson(lessonId)
        _state.update {
            it.copy(
                isLoading = false,
                lesson = lesson,
                totalSteps = lesson?.steps?.size ?: 0,
                error = if (lesson == null) "Could not load lesson: $lessonId" else null
            )
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
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: LessonPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
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
            "animated_explanation" -> ExplanationStep(step)
            "multiple_choice", "story_comprehension", "prediction_observation_explanation" ->
                MultipleChoiceStep(step, viewModel)
            "sort_and_classify", "timeline_builder" ->
                SortStep(step, viewModel)
            "array_builder" ->
                ArrayStep(step, viewModel)
            "sentence_builder" ->
                SentenceBuilderStep(step, viewModel)
            else -> {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Amber90)) {
                    Text(
                        "Activity type: ${step.type}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.onActivityResult(
                                ActivityResult(step.id, true, 1, 0, 0)
                            )
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Continue")
                    }
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
private fun ExplanationStep(step: ActivityStep) {
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
        }
    }
}

@Composable
private fun MultipleChoiceStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    Text(
        step.question,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    Spacer(Modifier.height(16.dp))

    step.options.forEachIndexed { index, option ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    viewModel.onActivityResult(
                        ActivityResult(
                            step.id,
                            index == step.correctIndex,
                            1,
                            0,
                            0
                        )
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Teal90),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        ('A' + index).toString(),
                        fontWeight = FontWeight.Bold,
                        color = Teal40
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(option, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SortStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    Text(
        step.question,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    )
    Spacer(Modifier.height(16.dp))

    step.options.forEach { option ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Amber90)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DragIndicator, "Drag", tint = Amber40)
                Spacer(Modifier.width(8.dp))
                Text(option, modifier = Modifier.weight(1f))
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            // Simplified: auto-pass sorting activities
            viewModel.onActivityResult(ActivityResult(step.id, true, 1, 0, 0))
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal40)
    ) {
        Text("I'm done sorting! ✅", fontSize = 16.sp)
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
