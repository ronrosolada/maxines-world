package com.maxinesworld.featurelessonplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.featurerewards.BadgeRevealScreen
import com.maxinesworld.featurerewards.ChallengeProgress

// ─── Main Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    lessonId: String, childId: String = "",
    onBack: () -> Unit, onComplete: () -> Unit,
    onRewardBreak: (String, String) -> Unit = { _, _ -> },
    viewModel: LessonPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(lessonId, childId) { viewModel.loadLesson(lessonId, childId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.lesson?.title ?: "Loading...", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Teal40)
                state.error != null -> ErrorDisplay(state.error!!, Modifier.align(Alignment.Center))
                state.isComplete -> {
                    // Show badge reveal if a badge was just earned
                    if (state.badgeAwarded != null) {
                        BadgeRevealScreen(
                            badge = state.badgeAwarded!!,
                            challengeProgress = ChallengeProgress(completedCount = 5),
                            onViewFieldGuide = { /* TODO: navigate */ },
                            onReturnToVillage = onComplete
                        )
                    } else {
                        LessonCompleteScreen(state, onComplete)
                    }
                }
                else -> LessonContent(state, viewModel)
            }
        }
    }
}

// ─── Lesson Content ───

@Composable
private fun LessonContent(state: LessonUiState, viewModel: LessonPlayerViewModel) {
    val lesson = state.lesson ?: return
    val step = lesson.steps.getOrNull(state.currentStep) ?: return

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Step progress dots — design v2 §24.3: clear, countable, 48dp touch
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(state.totalSteps) { index ->
                val isDone = index < state.currentStep
                val isCurrent = index == state.currentStep
                val dotColor = when {
                    isDone -> SuccessGreen
                    isCurrent -> VillageTeal
                    else -> VillageTeal.copy(alpha = 0.15f)
                }
                // Full tap-target area with centered content
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(if (isCurrent) 36.dp else 32.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) Icon(Icons.Default.Check, "Done",
                            tint = White, modifier = Modifier.size(18.dp))
                        else if (isCurrent) Box(Modifier.size(14.dp).clip(CircleShape).background(White))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        CharacterGuide(lesson.guideCharacter)
        Spacer(Modifier.height(10.dp))

        if (step.narrationText.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Cream),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Text(step.narrationText, modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
            Spacer(Modifier.height(14.dp))
        }

        when (step.type) {
            "ANIMATED_EXPLANATION_V1", "animated_explanation" -> ExplanationStep(step, lesson.languageOfInstruction ?: "english") {
                viewModel.onActivityResult(ActivityResult(step.id, true, 1, 0, 0, scored = false))
            }
            "MULTIPLE_CHOICE_V1", "multiple_choice", "story_comprehension", "prediction_observation_explanation" -> MultipleChoiceStep(step, viewModel)
            "SORT_AND_CLASSIFY_V1", "sort_and_classify", "timeline_builder" -> SortStep(step, viewModel)
            "SEQUENCE_BUILDER_V1", "sentence_builder", "array_builder" -> SentenceBuilderStep(step, viewModel)
            "HOTSPOT_IMAGE_V1" -> HotspotImageStep(step, viewModel)
            "MATCHING_PAIRS_V1" -> MatchingPairsStep(step, viewModel)
            "INTERACTIVE_SPEC_V1" -> InteractiveSpecStep(step, viewModel)
            else -> UnsupportedActivity(step)
        }

        if (state.showFeedback) {
            Spacer(Modifier.height(12.dp))
            FeedbackBanner(state.feedbackText, state.feedbackCorrect) { viewModel.onNextStep() }
        }
    }
}

// ─── Explanation Step (with TTS) ───

@Composable
private fun ExplanationStep(step: ActivityStep, language: String = "english", onContinue: () -> Unit) {
    val context = LocalContext.current
    val ttsPlayer = remember { LessonTtsPlayer(context) }
    var ttsSpeaking by remember { mutableStateOf(false) }
    var ttsUnavailable by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { ttsPlayer.shutdown() } }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Cream), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, "Story", modifier = Modifier.size(32.dp), tint = Teal40)
                Spacer(Modifier.width(12.dp))
                Text("Read Along", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Teal40, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (ttsSpeaking) {
                        ttsPlayer.stop(); ttsSpeaking = false
                    } else {
                        ttsUnavailable = false
                        ttsSpeaking = true
                        ttsPlayer.speak(
                            text = step.narrationText,
                            language = language,
                            onComplete = { ttsSpeaking = false },
                            onUnavailable = {
                                ttsSpeaking = false
                                ttsUnavailable = true
                            }
                        )
                    }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (ttsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                        if (ttsSpeaking) "Stop" else "Read aloud",
                        tint = if (ttsSpeaking) Coral else Teal40,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (ttsUnavailable) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.15f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, "Info", tint = SunshineGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Filipino voice not available on this device — please read along instead.",
                            fontSize = 14.sp, color = Ink.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(step.narrationText, style = MaterialTheme.typography.bodyLarge, fontSize = 20.sp, lineHeight = 32.sp, color = Ink.copy(alpha = 0.85f))
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (ttsSpeaking) "Reading aloud..."
                    else if (ttsUnavailable) "Filipino voice not available"
                    else "Tap speaker to listen",
                    fontSize = 14.sp, color = Teal40.copy(alpha = 0.5f))
                MaxinesPrimaryButton(onClick = onContinue, text = "Continue",
                    containerColor = Teal40, modifier = Modifier)
            }
        }
    }
}

// ─── Unsupported Activity Safe Fallback ───

@Composable
private fun UnsupportedActivity(step: ActivityStep) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f))) {
        Column(Modifier.padding(16.dp)) {
            Text("Activity type \"${step.type}\" is not yet supported.", style = MaterialTheme.typography.bodyMedium, color = Warning)
            Text("This lesson step needs an engine update. Your progress is saved.", fontSize = 14.sp, color = Warning.copy(alpha = 0.7f))
        }
    }
}

// ─── Activity Steps ───

@Composable
private fun MultipleChoiceStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(1) }
    var showRetry by remember { mutableStateOf(false) }

    LaunchedEffect(step.id) { selectedIndex = null; submitted = false; attemptCount = 1; showRetry = false }

    fun submit(index: Int) {
        val isCorrect = index == step.correctIndex
        if (isCorrect || attemptCount >= 2) {
            submitted = true; selectedIndex = index
            viewModel.onActivityResult(ActivityResult(step.id, isCorrect, attemptCount, 0, 0))
        } else {
            attemptCount++; selectedIndex = index; showRetry = true
        }
    }

    Text(step.question, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    Spacer(Modifier.height(16.dp))

    step.options.forEachIndexed { index, option ->
        val bgColor = when {
            submitted && index == step.correctIndex -> SuccessGreen.copy(alpha = 0.2f)
            showRetry && index == selectedIndex -> ErrorRed.copy(alpha = 0.15f)
            index == selectedIndex -> Teal90 else -> SurfaceContainer
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(enabled = !submitted && !(showRetry && index != selectedIndex)) { submit(index) },
            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (index == selectedIndex) 4.dp else 2.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val circleColor = when { submitted && index == step.correctIndex -> SuccessGreen; showRetry && index == selectedIndex -> ErrorRed; else -> Teal90 }
                Box(Modifier.size(40.dp).clip(CircleShape).background(circleColor), contentAlignment = Alignment.Center) {
                    if (submitted && index == step.correctIndex) Icon(Icons.Default.Check, "Correct", tint = Color.White)
                    else if (showRetry && index == selectedIndex) Icon(Icons.Default.Close, "Wrong", tint = Color.White)
                    else Text(('A' + index).toString(), fontWeight = FontWeight.Bold, color = Teal40)
                }
                Spacer(Modifier.width(12.dp))
                Text(option, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (showRetry) {
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { selectedIndex = null; showRetry = false }) { Text("Try again", color = Teal40) }
    }
}

// ─── Sort, Array, Sentence Builder Steps ───

@Composable
private fun SortStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    val options = step.options
    val shuffledIndices = remember(step.id) { options.indices.shuffled() }
    var selectedOrder by remember(step.id) { mutableStateOf<List<Int>>(emptyList()) }

    Text(step.question, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    Spacer(Modifier.height(12.dp))

    if (selectedOrder.isNotEmpty()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))) {
            Column(Modifier.padding(12.dp)) {
                Text("Your order:", fontWeight = FontWeight.Bold, color = Teal40)
                selectedOrder.forEachIndexed { idx, optionIdx -> Text("${idx + 1}. ${options[optionIdx]}", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    Text("Tap items in the correct order:", style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.height(8.dp))

    shuffledIndices.forEach { shuffledIdx ->
        if (shuffledIdx !in selectedOrder) {
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { selectedOrder = selectedOrder + shuffledIdx },
                shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Amber90)) {
                Text(options[shuffledIdx], modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (selectedOrder.size == options.size) {
        Spacer(Modifier.height(12.dp))
        val isCorrect = selectedOrder == options.indices.toList()
        Text(if (isCorrect) "Correct order!" else "Not quite right.", fontWeight = FontWeight.Bold, color = if (isCorrect) SuccessGreen else ErrorRed)
        MaxinesPrimaryButton(
            onClick = { viewModel.onActivityResult(ActivityResult(step.id, isCorrect, 1, 0, 0)) },
            text = "Submit", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ArrayStep(step: ActivityStep, viewModel: LessonPlayerViewModel) = MultipleChoiceStep(step, viewModel)

@Composable
private fun SentenceBuilderStep(step: ActivityStep, viewModel: LessonPlayerViewModel) = UnsupportedActivity(step)

// ─── V1 Engine Renderer Wrappers ───

@Composable
private fun HotspotImageStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    com.maxinesworld.engineactivity.renderers.ActivityRenderer(
        step = step,
        onResult = { viewModel.onActivityResult(it) },
        onHint = { }
    )
}

@Composable
private fun MatchingPairsStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    com.maxinesworld.engineactivity.renderers.ActivityRenderer(
        step = step,
        onResult = { viewModel.onActivityResult(it) },
        onHint = { }
    )
}

@Composable
private fun InteractiveSpecStep(step: ActivityStep, viewModel: LessonPlayerViewModel) {
    com.maxinesworld.engineactivity.renderers.ActivityRenderer(
        step = step,
        onResult = { viewModel.onActivityResult(it) },
        onHint = { }
    )
}

// ─── Feedback, Character, Error, Completion ───

@Composable
private fun FeedbackBanner(text: String, correct: Boolean, onNext: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (correct) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (correct) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = if (correct) SuccessGreen else ErrorRed)
            Spacer(Modifier.width(12.dp))
            Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            TextButton(onClick = onNext) { Text(if (correct) "Next" else "Try Next", color = Teal40) }
        }
    }
}

@Composable
private fun CharacterGuide(character: String) {
    val emoji = when (character.lowercase()) { "milo" -> "🧡"; "mira" -> "💜"; "niko" -> "🩶"; "lakan" -> "🇵🇭"; "duke" -> "💙"; else -> "🐱" }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Orange80), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(8.dp))
        Text(character.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium, color = Teal40)
    }
}

@Composable
private fun ErrorDisplay(error: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, "Error", tint = ErrorRed, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(error, color = ErrorRed, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LessonCompleteScreen(state: LessonUiState, onComplete: () -> Unit, onPlayGames: () -> Unit = {}) {
    val scored = state.results.filter { it.scored }
    val correct = scored.count { it.correct }
    val total = scored.size
    val accuracy = if (total > 0) correct.toFloat() / total else 0f
    val starsEarned = kotlin.math.ceil(accuracy * 5).toInt().coerceIn(1, 5)
    val coinsEarned = if (accuracy >= 0.8f) 10 else 0

    // Confetti — respect reduced motion
    val reducedMotion = false // TODO: wire system setting
    val confettiColors = if (!reducedMotion) listOf(Coral, SunshineGold, SkyBlue, StoryPurple, LeafGreen, VillageTeal) else emptyList()
    val particles = remember { List(if (reducedMotion) 0 else 40) { Offset((Math.random() * 1000).toFloat(), (-Math.random() * 800).toFloat()) } }
    val confettiAnim by rememberInfiniteTransition(label = "confetti").animateFloat(0f, 800f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), "fall")

    Box(Modifier.fillMaxSize()) {
        if (!reducedMotion) {
            Canvas(Modifier.fillMaxSize()) {
                particles.forEachIndexed { i, pos ->
                    val y = (pos.y + confettiAnim + (i * 37)) % size.height
                    val x = (pos.x + kotlin.math.sin(confettiAnim / 200 + i) * 50) % size.width
                    drawCircle(confettiColors[i % confettiColors.size].copy(alpha = 0.6f), radius = (4 + (i % 5)).toFloat(), center = Offset(x.toFloat(), y))
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Lesson Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Teal40)
            Text("You got $correct out of $total correct!", style = MaterialTheme.typography.bodyLarge)
            Text("${(accuracy * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 48.sp, color = if (accuracy >= 0.8f) SuccessGreen else Amber40)

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f))) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Star, "Stars", tint = SunshineGold, modifier = Modifier.size(28.dp)); Text("+$starsEarned Stars", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Toll, "Coins", tint = SunshineGold, modifier = Modifier.size(28.dp)); Text(if (coinsEarned > 0) "+$coinsEarned Coins" else "0 Coins", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }

            Spacer(Modifier.height(24.dp))
            MaxinesPrimaryButton(onClick = onComplete, text = "Continue", modifier = Modifier.fillMaxWidth())
        }
    }
}
