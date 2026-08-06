package com.maxinesworld.featurelessonplayer

import android.provider.Settings
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.VocabTerm
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineactivity.renderers.ActivityRenderer
import com.maxinesworld.engineactivity.renderers.LessonVisual
import com.maxinesworld.engineactivity.renderers.optionOrderFor
import com.maxinesworld.featurerewards.BadgeRevealScreen
import com.maxinesworld.featurerewards.ChallengeProgress
import kotlinx.coroutines.launch

// ─── New Words card ───

/** Minimal lesson-chrome localization: fil-PH lessons get Filipino chrome. */
private fun uiText(language: String?, en: String, fil: String): String =
    if (language?.lowercase()?.startsWith("fil") == true) fil else en

/** Returns true only when narration adds context beyond the activity instruction. */
internal fun shouldShowNarrationCard(step: ActivityStep): Boolean =
    step.type != "ANIMATED_EXPLANATION_V1" &&
        step.narrationText.isNotBlank() &&
        step.narrationText.trim() != step.question.trim()

@Composable
private fun VocabularyCard(terms: List<VocabTerm>, title: String = "New Words") {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SkyBlue.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, title, tint = Teal40, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Teal40)
            }
            Spacer(Modifier.height(8.dp))
            terms.take(4).forEach { term ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(
                        term.term,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        term.definition,
                        color = Ink.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─── Main Screen ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    lessonId: String, childId: String = "",
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onViewFieldGuide: (String) -> Unit = {},
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
                            challengeProgress = state.expeditionProgress,
                            onViewFieldGuide = { onViewFieldGuide(state.badgeAwarded!!.id) },
                            onReturnToVillage = onComplete,
                            onPlayGames = state.rewardBreakId?.let { breakId ->
                                { onRewardBreak(childId, breakId) }
                            },
                        )
                    } else {
                        LessonCompleteScreen(state, onComplete) {
                            state.rewardBreakId?.let { breakId ->
                                onRewardBreak(childId, breakId)
                            }
                        }
                    }
                }
                state.assessmentFailed -> AssessmentRetryCard(
                    language = state.lesson?.languageOfInstruction,
                    onRetry = viewModel::retryAssessment,
                )
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
    val lang = lesson.languageOfInstruction
    val practiceStepCount = (state.totalSteps - state.assessmentStepCount).coerceAtLeast(1)
    val inAssessment = state.currentStep >= practiceStepCount
    val assessmentIndex = (state.currentStep - practiceStepCount).coerceAtLeast(0)
    val reviewExample = lesson.steps
        .firstOrNull { it.type == "ANIMATED_EXPLANATION_V1" }?.narrationText
        ?.takeIf { it.isNotBlank() }
    var showReview by remember { mutableStateOf(false) }
    var feedbackHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val feedbackBottomPadding = if (state.showFeedback && feedbackHeightPx > 0) {
        with(density) { feedbackHeightPx.toDp() + 16.dp }
    } else {
        LessonFeedbackLayout.bottomContentPaddingDp(state.showFeedback).dp
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = feedbackBottomPadding)
        ) {
        // New Words — vocabulary preview on the first step only (review: it was
        // repeated above every step; show once, not continuously)
        if (state.currentStep == 0 && lesson.vocabulary.isNotEmpty()) {
            VocabularyCard(lesson.vocabulary, uiText(lang, "New Words", "Bagong Salita"))
            Spacer(Modifier.height(14.dp))
        }

        if (!inAssessment) {
            // Step progress dots — design v2 §24.3: clear, countable, 48dp touch
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(practiceStepCount) { index ->
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
        } else {
            // Assessment phase banner — visibly separates the knowledge check
            // from practice (review: the authored assessment was never played)
            Card(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.16f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Amber40, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        uiText(lang, "Knowledge Check", "Pagsusulit"),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${uiText(lang, "Question", "Tanong")} ${assessmentIndex + 1} " +
                            "${uiText(lang, "of", "ng")} ${state.assessmentStepCount}",
                        fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink.copy(alpha = 0.65f)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        CharacterGuide(lesson.guideCharacter)
        Spacer(Modifier.height(10.dp))

        // Narration card — skipped on explanation steps: ExplanationStep renders
        // the same text itself (review: narration was displayed twice)
        if (shouldShowNarrationCard(step)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Cream),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Text(step.narrationText, modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
            Spacer(Modifier.height(14.dp))
        }

        // ANIMATED_EXPLANATION keeps the local renderer because it owns the
        // text-to-speech playback path (LessonTtsPlayer), which engine-activity
        // does not have access to. Everything else goes through the shared
        // type-safe dispatcher.
        when {
            step.type == "ASSESSMENT_V1" -> AssessmentStepCard(
                step = step,
                language = lang,
                answered = state.results.any { it.activityId == step.id },
                onResult = { result -> viewModel.onActivityResult(result) },
            )

            step.type == "ANIMATED_EXPLANATION_V1" -> ExplanationStep(step, lesson.languageOfInstruction ?: "english") {
                viewModel.onActivityResult(ActivityResult(step.id, true, 1, 0, 0, scored = false))
            }

            else -> ActivityRenderer(
                step = step,
                onResult = { result -> viewModel.onActivityResult(result) },
                modifier = Modifier.fillMaxWidth(),
                // #29: the success banner is the primary CTA — make it advance
                // the lesson instead of sitting there looking clickable.
                onAdvance = { viewModel.onNextStep() }
            )
        }

        }

        if (state.showFeedback) {
            FeedbackBanner(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .onSizeChanged { feedbackHeightPx = it.height }
                    .navigationBarsPadding(),
                text = state.feedbackText,
                correct = state.feedbackCorrect,
                language = lang,
                isAssessment = inAssessment,
                onNext = { viewModel.onNextStep() },
                onReview = if (!state.feedbackCorrect && inAssessment && reviewExample != null) {
                    { showReview = true }
                } else null,
            )
        }
    }

    if (showReview && reviewExample != null) {
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = { Text(uiText(lang, "Let's review the lesson", "Balikan natin ang aralin"), fontWeight = FontWeight.Bold) },
            text = { Text(reviewExample, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp) },
            confirmButton = {
                TextButton(onClick = { showReview = false }) { Text(uiText(lang, "Got it", "Sige"), fontWeight = FontWeight.Bold) }
            }
        )
    }
}

// ─── Assessment step — the authored knowledge check ───

@Composable
private fun AssessmentStepCard(
    step: ActivityStep,
    language: String?,
    answered: Boolean,
    onResult: (ActivityResult) -> Unit,
) {
    // Same deterministic per-item option order as the practice MCQ renderer:
    // the correct position is remapped by step id, so authored key positions
    // (and any static bias) never leak to the learner.
    val optionOrder = remember(step.id, step.options, step.correctIndex) {
        optionOrderFor(step.id, step.options.size, step.correctIndex)
    }
    val options = optionOrder.map { step.options[it] }
    val displayedCorrectIndex = optionOrder.indexOf(step.correctIndex)

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(step.question, style = MaterialTheme.typography.bodyLarge, fontSize = 19.sp,
                lineHeight = 30.sp, color = Ink)
            Spacer(Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                Surface(
                    onClick = {
                        if (!answered) onResult(
                            ActivityResult(step.id, index == displayedCorrectIndex, 1, 0, 0, scored = true)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VillageTeal.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(option, modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge, fontSize = 17.sp, color = Ink)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                uiText(language, "Pick the best answer.", "Piliin ang pinakamagandang sagot."),
                fontSize = 13.sp, color = Ink.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Explanation Step (with TTS) ───

@Composable
fun NarrationControlRow(
    narrationEnabled: Boolean,
    ttsSpeaking: Boolean,
    onToggle: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                if (narrationEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                if (narrationEnabled) "Turn narration off" else "Turn narration on",
                tint = if (narrationEnabled) Teal40 else Ink.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(
            enabled = narrationEnabled,
            onClick = onReplay,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                if (ttsSpeaking) Icons.Default.Stop else Icons.Default.Replay,
                if (ttsSpeaking) "Stop narration" else "Replay narration",
                tint = if (ttsSpeaking) Coral else Teal40,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ExplanationStep(step: ActivityStep, language: String = "english", onContinue: () -> Unit) {
    val context = LocalContext.current
    val ttsPlayer = remember { LessonTtsPlayer(context) }
    val narrationEnabled by NarrationPreferences.enabled(context).collectAsState(initial = true)
    val preferenceScope = rememberCoroutineScope()
    var ttsSpeaking by remember { mutableStateOf(false) }
    var ttsUnavailable by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { ttsPlayer.shutdown() } }

    fun playNarration() {
        if (!narrationEnabled) return
        ttsPlayer.stop()
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

    fun setNarrationEnabled(enabled: Boolean) {
        preferenceScope.launch { NarrationPreferences.setEnabled(context, enabled) }
        if (!enabled) {
            ttsPlayer.stop()
            ttsSpeaking = false
        }
    }

    // #34: auto-play the narration when the step renders — an emerging
    // reader must not have to discover the speaker icon to hear the text.
    // Keyed on step id so returning to the step re-plays, but recomposition
    // does not restart it.
    LaunchedEffect(step.id, narrationEnabled) {
        if (narrationEnabled) {
            playNarration()
        } else {
            ttsPlayer.stop()
            ttsSpeaking = false
        }
    }

    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = narrationEnabled, onClick = { playNarration() }),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            LessonVisual(step)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, "Story", modifier = Modifier.size(32.dp), tint = Teal40)
                Spacer(Modifier.width(12.dp))
                Text(uiText(language, "Read Along", "Basahin Natin"), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Teal40, modifier = Modifier.weight(1f))
                NarrationControlRow(
                    narrationEnabled = narrationEnabled,
                    ttsSpeaking = ttsSpeaking,
                    onToggle = { setNarrationEnabled(!narrationEnabled) },
                    onReplay = {
                        if (ttsSpeaking) {
                            ttsPlayer.stop()
                            ttsSpeaking = false
                        } else {
                            playNarration()
                        }
                    },
                )
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
                    when {
                        !narrationEnabled -> uiText(language, "Narration off. Tap the speaker to turn it on.", "Naka-off ang pagbasa. Pindutin ang speaker para i-on.")
                        ttsSpeaking -> uiText(language, "Reading aloud...", "Binabasa...")
                        ttsUnavailable -> uiText(language, "Filipino voice not available", "Walang Filipino voice")
                        else -> uiText(language, "Tap speaker to listen", "Pindutin ang speaker para makinig")
                    },
                    fontSize = 14.sp, color = Teal40.copy(alpha = 0.5f))
                MaxinesPrimaryButton(onClick = onContinue, text = uiText(language, "Continue", "Sunod"),
                    containerColor = Teal40, modifier = Modifier)
            }
        }
    }
}


// ─── Feedback, Character, Error, Completion ───

@Composable
private fun AssessmentRetryCard(
    language: String?,
    onRetry: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.14f)),
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🌟", fontSize = 42.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    uiText(language, "Let's try the knowledge check again", "Subukan natin muli ang pagsusulit"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    uiText(language, "You can review the lesson and try again.", "Maaari mong balikan ang aralin at subukan muli."),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Ink.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(20.dp))
                MaxinesPrimaryButton(
                    onClick = onRetry,
                    text = uiText(language, "Try again", "Subukan muli"),
                    containerColor = Teal40,
                )
            }
        }
    }
}

@Composable
private fun FeedbackBanner(
    modifier: Modifier = Modifier,
    text: String,
    correct: Boolean,
    language: String?,
    isAssessment: Boolean,
    onNext: () -> Unit,
    onReview: (() -> Unit)? = null,
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (correct) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (correct) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = if (correct) SuccessGreen else ErrorRed)
            Spacer(Modifier.width(12.dp))
            Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            if (onReview != null) {
                TextButton(onClick = onReview) { Text(uiText(language, "Review", "Balikan"), color = Teal40) }
            }
            // Assessment is a single-attempt knowledge check: the correction is
            // shown, then the child moves on (no retry inside the check).
            TextButton(onClick = onNext) {
                Text(
                    when {
                        correct || isAssessment -> uiText(language, "Next", "Sunod")
                        else -> uiText(language, "Try Next", "Subukan Muli")
                    },
                    color = Teal40
                )
            }
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
private fun rememberConfettiProgress(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "confetti")
    return transition.animateFloat(
        0f,
        800f,
        infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "fall"
    ).value
}

internal fun confettiAnimationEnabled(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f

@Composable
private fun ErrorDisplay(error: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.ErrorOutline, "Error", tint = ErrorRed, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(error, color = ErrorRed, textAlign = TextAlign.Center)
    }
}

@Composable
fun LessonCompleteScreen(state: LessonUiState, onComplete: () -> Unit, onPlayGames: () -> Unit = {}) {
    val scored = state.results.filter { it.scored }
    val correct = scored.count { it.correct }
    val total = scored.size
    val accuracy = if (total > 0) correct.toFloat() / total else 0f
    val starsEarned = 1 +
        (if (accuracy >= 0.8f) 1 else 0) +
        (if (accuracy >= 0.95f) 1 else 0)
    val coinsEarned = if (accuracy >= 0.8f) 10 else 0

    // Confetti — respect reduced motion (ANIMATOR_DURATION_SCALE == 0 on
    // Android disables system animations; children with this preference get
    // a static celebration screen instead of falling confetti).
    val context = LocalContext.current
    val animationScale = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }
    val reducedMotion = !confettiAnimationEnabled(animationScale)
    val confettiColors = if (!reducedMotion) listOf(Coral, SunshineGold, SkyBlue, StoryPurple, LeafGreen, VillageTeal) else emptyList()
    val particles = remember { List(if (reducedMotion) 0 else 40) { Offset((Math.random() * 1000).toFloat(), (-Math.random() * 800).toFloat()) } }
    val confettiAnim = rememberConfettiProgress(enabled = !reducedMotion)

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
            val lang = state.lesson?.languageOfInstruction
            Text(uiText(lang, "Lesson Complete!", "Tapos na ang Aralin!"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Teal40)
            Text(uiText(lang, "You got $correct out of $total correct!", "Nakuha mo ang $correct sa $total!"), style = MaterialTheme.typography.bodyLarge)
            Text("${(accuracy * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 48.sp, color = if (accuracy >= 0.8f) SuccessGreenText else HeritageGold)

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f))) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Star, "Stars", tint = SunshineGold, modifier = Modifier.size(28.dp)); Text("+$starsEarned Stars", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Toll, "Coins", tint = SunshineGold, modifier = Modifier.size(28.dp)); Text(if (coinsEarned > 0) "+$coinsEarned Coins" else "0 Coins", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }

            Spacer(Modifier.height(24.dp))
            MaxinesPrimaryButton(onClick = onComplete, text = "Continue", modifier = Modifier.fillMaxWidth())
            if (state.rewardBreakId != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPlayGames,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunshineGold,
                        contentColor = OnGold,
                    ),
                ) {
                    Icon(Icons.Default.SportsEsports, "Games", modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play a Reward Game", fontSize = 18.sp)
                }
            }
        }
    }
}
