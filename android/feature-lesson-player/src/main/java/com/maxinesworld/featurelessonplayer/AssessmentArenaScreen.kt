package com.maxinesworld.featurelessonplayer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.coremodel.AssessmentPackMetadata
import com.maxinesworld.coremodel.AssessmentQuestionItem

@Composable
fun AssessmentArenaRoute(
    onBack: () -> Unit,
    viewModel: AssessmentArenaViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AssessmentArenaScreen(
        state = state,
        onBack = onBack,
        onSelectSubject = viewModel::selectSubject,
        onSelectCurriculum = viewModel::selectCurriculum,
        onStartQuiz = viewModel::startQuiz,
        onSelectOption = viewModel::selectOption,
        onToggleHint = viewModel::toggleHint,
        onSubmitAnswer = viewModel::submitAnswer,
        onNextQuestion = viewModel::nextQuestion,
        onRestartQuiz = viewModel::restartQuiz,
        onReviewClues = viewModel::reviewClues,
        onExitClueReview = viewModel::exitClueReview,
        onExitQuiz = viewModel::exitQuiz,
        onDismissCelebration = viewModel::dismissCelebration,
        modifier = modifier,
    )
}

@Composable
fun AssessmentArenaScreen(
    state: AssessmentArenaUiState,
    onBack: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectCurriculum: (String) -> Unit,
    onStartQuiz: (String) -> Unit,
    onSelectOption: (String) -> Unit,
    onToggleHint: () -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    onReviewClues: () -> Unit,
    onExitClueReview: () -> Unit,
    onExitQuiz: () -> Unit,
    onDismissCelebration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Cream, White)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (state.activeQuiz != null) {
            // Interactive Quiz View
            ActiveQuizView(
                quiz = state.activeQuiz,
                onSelectOption = onSelectOption,
                onToggleHint = onToggleHint,
                onSubmitAnswer = onSubmitAnswer,
                onNextQuestion = onNextQuestion,
                onRestartQuiz = onRestartQuiz,
                onReviewClues = onReviewClues,
                onExitClueReview = onExitClueReview,
                onExitQuiz = onExitQuiz,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Master Arena Hub View
            ArenaHubView(
                state = state,
                onBack = onBack,
                onSelectSubject = onSelectSubject,
                onSelectCurriculum = onSelectCurriculum,
                onStartQuiz = onStartQuiz,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val animationsDisabled = LocalAnimationsDisabled.current
        AnimatedVisibility(
            visible = state.showCelebrationDialog && state.activeQuiz?.isPassed == true,
            enter = if (animationsDisabled) {
                fadeIn(animationSpec = tween(120))
            } else {
                fadeIn(animationSpec = tween(360)) + slideInVertically(
                    animationSpec = tween(360),
                    initialOffsetY = { it / 8 },
                )
            },
            exit = fadeOut(animationSpec = if (animationsDisabled) tween(120) else tween(180)),
        ) {
            state.activeQuiz?.let { quiz ->
                MiloCelebrationDialog(
                    stars = quiz.earnedStars,
                    tokens = quiz.earnedTokens,
                    score = quiz.correctCount,
                    onDismiss = onDismissCelebration,
                )
            }
        }
    }
}

@Composable
private fun ArenaHubView(
    state: AssessmentArenaUiState,
    onBack: () -> Unit,
    onSelectSubject: (String) -> Unit,
    onSelectCurriculum: (String) -> Unit,
    onStartQuiz: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 600.dp
        val horizontalPad = if (isCompact) 16.dp else 24.dp
        val verticalPad = if (isCompact) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPad, vertical = verticalPad),
        ) {
            // App Bar
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VillageTeal)
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Assessment Arena",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = DeepNight,
                            )
                            Text(
                                "Grade 3 Challenge Track",
                                fontSize = 12.sp,
                                color = DeepNight.copy(alpha = 0.6f),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = SunshineGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SunshineGold),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Stars", tint = OnGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "+10 (≥80%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = DeepNight,
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VillageTeal)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Assessment Arena",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = DeepNight,
                            )
                            Text(
                                "Grade 3 International Challenge Track",
                                fontSize = 14.sp,
                                color = DeepNight.copy(alpha = 0.6f),
                            )
                        }
                    }

                    // Reward Chips
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = SunshineGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SunshineGold),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Stars", tint = OnGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "+10 Stars on Pass (≥80%)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepNight,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isCompact) 10.dp else 16.dp))

            // 6 Subject Selector Tabs
            val subjects = listOf(
                "mathematics" to ("Mathematics" to "Number Fun"),
                "science" to ("Science" to "Discovery"),
                "english" to ("English" to "Story Time"),
                "gmrc" to ("GMRC" to "Kindness"),
                "filipino" to ("Filipino" to "Kwentuhan"),
                "makabansa" to ("Makabansa" to "Bayan at Kultura"),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(subjects) { (id, labelPair) ->
                    val (title, sub) = labelPair
                    val isSelected = state.selectedSubjectId.equals(id, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) VillageTeal else Cream,
                        border = BorderStroke(1.dp, if (isSelected) VillageTeal else Ink.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .clickable { onSelectSubject(id) }
                            .height(if (isCompact) 48.dp else 58.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isCompact) 12.sp else 14.sp,
                                color = if (isSelected) White else DeepNight,
                            )
                            Text(
                                sub,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                color = if (isSelected) White.copy(alpha = 0.8f) else DeepNight.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (isCompact) 12.dp else 20.dp))

            // Normalized filter for subjects
            val currentSubjNormalized = when (state.selectedSubjectId.lowercase().trim()) {
                "math", "mathematics", "number_fun" -> "mathematics"
                "sci", "science", "discovery" -> "science"
                "eng", "english", "story_time" -> "english"
                "gmrc", "values", "kindness" -> "gmrc"
                "fil", "filipino", "tagalog", "kwentuhan" -> "filipino"
                "makabansa", "araling_panlipunan", "ap", "bayan_at_kultura" -> "makabansa"
                else -> state.selectedSubjectId.lowercase().trim()
            }

            val subjectPacks = state.packs.filter { pack ->
                pack.subjectId.equals(currentSubjNormalized, ignoreCase = true)
            }

            if (subjectPacks.isEmpty() && state.isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VillageTeal)
                }
            } else if (subjectPacks.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Select a subject tab above to view available assessments.", color = DeepNight.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(subjectPacks, key = { it.id }) { pack ->
                        CurriculumPackCard(
                            pack = pack,
                            isPassed = pack.id in state.passedPackIds,
                            onStart = { onStartQuiz(pack.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurriculumPackCard(
    pack: AssessmentPackMetadata,
    isPassed: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 600.dp

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isPassed) SuccessGreen else VillageTeal.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VillageTeal.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(pack.flagEmoji, fontSize = 22.sp)
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pack.curriculumName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = DeepNight,
                                )
                                if (isPassed) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            "✓ PASSED",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = SuccessGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                pack.description,
                                fontSize = 12.sp,
                                color = DeepNight.copy(alpha = 0.6f),
                                lineHeight = 15.sp,
                                maxLines = 2,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "10 questions • Passing 8/10",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = VillageTeal,
                        )

                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPassed) VillageTeal.copy(alpha = 0.15f) else VillageTeal,
                                contentColor = if (isPassed) VillageTeal else White,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Text(
                                if (isPassed) "Retake quiz" else "Take quiz",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        // Flag / Badge Container
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(VillageTeal.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(pack.flagEmoji, fontSize = 28.sp)
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    pack.curriculumName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = DeepNight,
                                )
                                if (isPassed) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            "✓ PASSED",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = SuccessGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                pack.description,
                                fontSize = 13.sp,
                                color = DeepNight.copy(alpha = 0.6f),
                                lineHeight = 17.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "10 questions • Passing score 8/10",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = VillageTeal,
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPassed) VillageTeal.copy(alpha = 0.15f) else VillageTeal,
                            contentColor = if (isPassed) VillageTeal else White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            if (isPassed) "Retake quiz" else "Take quiz",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveQuizView(
    quiz: ActiveAssessmentQuizState,
    onSelectOption: (String) -> Unit,
    onToggleHint: () -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    onReviewClues: () -> Unit,
    onExitClueReview: () -> Unit,
    onExitQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentItem = quiz.items.getOrNull(quiz.currentIndex)
    val displayedOptions = quiz.displayedOptions
    val copy = arenaCopy(quiz.packId)
    var showExitDialog by remember { mutableStateOf(false) }

    val ttsContext = androidx.compose.ui.platform.LocalContext.current
    val ttsPlayer = remember { LessonTtsPlayer(ttsContext) }
    val soundPlayer = remember { ArenaSoundEffectPlayer(ttsContext) }
    val haptic = LocalHapticFeedback.current
    val feedbackDisabled = LocalAnimationsDisabled.current
    DisposableEffect(Unit) {
        onDispose { ttsPlayer.stop(); soundPlayer.close() }
    }
    LaunchedEffect(quiz.currentIndex, quiz.isAnswerSubmitted) {
        arenaAnswerSound(quiz.isAnswerSubmitted, quiz.isCorrect)?.let { effect ->
            if (!feedbackDisabled) {
                soundPlayer.play(effect)
                haptic.performHapticFeedback(
                    if (effect == ArenaSoundEffect.CORRECT) HapticFeedbackType.LongPress
                    else HapticFeedbackType.TextHandleMove,
                )
            }
        }
    }
    LaunchedEffect(quiz.isFinished, quiz.isPassed) {
        if (quiz.isFinished && quiz.isPassed && !feedbackDisabled) {
            soundPlayer.play(ArenaSoundEffect.CELEBRATION)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val handleExitRequest = {
        if (!quiz.isFinished && (quiz.currentIndex > 0 || quiz.selectedOptionId != null || quiz.isAnswerSubmitted)) {
            showExitDialog = true
        } else {
            onExitQuiz()
        }
    }

    BackHandler(enabled = true) {
        if (quiz.isReviewingClues) onExitClueReview() else handleExitRequest()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Leave Quiz?",
                    fontWeight = FontWeight.Bold,
                    color = DeepNight,
                )
            },
            text = {
                Text(
                    "Are you sure you want to leave this quiz? Your current question progress will not be saved.",
                    fontSize = 15.sp,
                    color = DeepNight.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onExitQuiz()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnError),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Leave Quiz", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Stay & Keep Playing")
                }
            },
            containerColor = White,
            shape = RoundedCornerShape(20.dp),
        )
    }

    if (quiz.isReviewingClues) {
        ClueReviewView(
            quiz = quiz,
            copy = copy,
            onBack = onExitClueReview,
            onRestartQuiz = onRestartQuiz,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Quiz Header with Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = handleExitRequest) {
                Icon(Icons.Default.Close, contentDescription = "Exit Quiz", tint = DeepNight)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VillageTeal.copy(alpha = 0.12f),
                    modifier = Modifier.clickable {
                        if (currentItem != null) {
                            val fullSpeechText = buildString {
                                append(currentItem.prompt)
                                displayedOptions.forEachIndexed { index, option ->
                                    append(". Option ${mcSlotLabel(index)}: ${option.text}")
                                }
                                append(".")
                            }
                            ttsPlayer.speak(fullSpeechText, copy.ttsLanguage)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = VillageTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Listen", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VillageTeal)
                    }
                }

                if (!quiz.isAnswerSubmitted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (quiz.isHintVisible) SunshineGold.copy(alpha = 0.35f) else SunshineGold.copy(alpha = 0.15f),
                        border = if (quiz.isHintVisible) BorderStroke(1.dp, SunshineGold) else null,
                        modifier = Modifier.clickable { onToggleHint() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Hint", tint = OnGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (quiz.isHintVisible) "Hide Hint" else "Hint", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepNight)
                        }
                    }
                }
            }

            // Question Progress Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quiz.items.indices.forEach { index ->
                    val isPast = index < quiz.currentIndex
                    val isCurrent = index == quiz.currentIndex
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isCurrent -> VillageTeal
                                    isPast -> SuccessGreen
                                    else -> DeepNight.copy(alpha = 0.15f)
                                }
                            )
                    )
                }
            }

            Text(
                "Q${quiz.currentIndex + 1} of ${quiz.items.size}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = VillageTeal,
            )
        }

        Spacer(Modifier.height(18.dp))

        if (quiz.isFinished) {
            // Summary Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        arenaCompletionState(quiz.isPassed, copy.isFilipino).message,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = DeepNight,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You scored ${quiz.correctCount} out of ${quiz.items.size} correct.",
                        fontSize = 16.sp,
                        color = DeepNight.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(14.dp))

                    if (quiz.isPassed) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SunshineGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, SunshineGold),
                        ) {
                            Text(
                                "Rewards earned: +${quiz.earnedStars} stars · +${quiz.earnedTokens} tokens",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DeepNight,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    } else {
                        Text(if (copy.isFilipino) "Balikan natin ang mga pahiwatig at subukan muli." else "Review Milo's clues, then try again when you're ready.", fontSize = 14.sp, color = VillageTeal, fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Button(
                            onClick = onRestartQuiz,
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(6.dp))
                            Text(copy.retry)
                        }

                        if (arenaCompletionState(quiz.isPassed, copy.isFilipino).showReviewClues) {
                            OutlinedButton(
                                onClick = onReviewClues,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag(ArenaTestTags.ReviewCluesButton),
                            ) {
                                Text(copy.reviewClues)
                            }
                        }

                        OutlinedButton(
                            onClick = onExitQuiz,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Back to Arena")
                        }
                    }
                }
            }
        } else if (currentItem != null) {
            // Question Prompt Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, VillageTeal.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        currentItem.prompt,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = DeepNight,
                        lineHeight = 26.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Options Layout (2-Column Grid on Tablet/Wide, 1-Column on Phone)
            val chunkedOptions = displayedOptions.mapIndexed { index, option -> index to option }.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chunkedOptions.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowOptions.forEach { (index, option) ->
                            val isSelected = quiz.selectedOptionId == option.id
                            val isSubmitted = quiz.isAnswerSubmitted
                            val isCorrectOption = option.id in currentItem.correctOptionIds
                            val slotLabel = mcSlotLabel(index)

                            val containerColor = when {
                                isSubmitted && isCorrectOption -> SuccessGreen.copy(alpha = 0.15f)
                                isSubmitted && isSelected && !isCorrectOption -> OnError.copy(alpha = 0.15f)
                                isSelected -> VillageTeal.copy(alpha = 0.12f)
                                else -> White
                            }

                            val borderColor = when {
                                isSubmitted && isCorrectOption -> SuccessGreen
                                isSubmitted && isSelected && !isCorrectOption -> OnError
                                isSelected -> VillageTeal
                                else -> DeepNight.copy(alpha = 0.15f)
                            }

                            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val optionScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.97f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "QuizOptionPress"
                            )

                            Surface(
                                onClick = { onSelectOption(option.id) },
                                enabled = !isSubmitted,
                                shape = RoundedCornerShape(16.dp),
                                color = containerColor,
                                border = BorderStroke(if (isSelected || isSubmitted) 2.dp else 1.dp, borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer {
                                        scaleX = optionScale
                                        scaleY = optionScale
                                    },
                                interactionSource = interactionSource,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Letter Bubble
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) VillageTeal else DeepNight.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            slotLabel,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) White else DeepNight,
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Text(
                                        option.text,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 16.sp,
                                        color = DeepNight,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        if (rowOptions.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Interactive Tiered Hint Box (before submission)
            if (!quiz.isAnswerSubmitted && quiz.isHintVisible) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SunshineGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, SunshineGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Hint", tint = OnGold, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                copy.clueHeader,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DeepNight,
                            )
                            Spacer(Modifier.height(4.dp))
                            val hintText = currentItem.hint.ifBlank { copy.hintFallback }
                            Text(
                                hintText,
                                fontSize = 14.sp,
                                color = DeepNight.copy(alpha = 0.85f),
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Feedback / Explanation Box (on submission) with TTS Audio Read-Aloud
            if (quiz.isAnswerSubmitted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (quiz.isCorrect) SuccessGreen.copy(alpha = 0.12f) else SunshineGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (quiz.isCorrect) SuccessGreen else SunshineGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (quiz.isCorrect) copy.correctHeader else copy.clueHeader,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DeepNight,
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (quiz.isCorrect) SuccessGreen.copy(alpha = 0.25f) else SunshineGold.copy(alpha = 0.35f),
                                modifier = Modifier.clickable {
                                    val prefix = if (quiz.isCorrect) copy.correctTtsPrefix else copy.clueTtsPrefix
                                    ttsPlayer.speak(prefix + currentItem.explanation, copy.ttsLanguage)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Read explanation", tint = DeepNight, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Read Explanation", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepNight)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            currentItem.explanation,
                            fontSize = 14.sp,
                            color = DeepNight.copy(alpha = 0.8f),
                            lineHeight = 20.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Action Button (Check Answer -> Next Question)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!quiz.isAnswerSubmitted) {
                    Button(
                        onClick = onSubmitAnswer,
                        enabled = quiz.selectedOptionId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(copy.checkAnswer, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Button(
                        onClick = onNextQuestion,
                        colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            if (quiz.currentIndex + 1 < quiz.items.size) copy.nextQuestion else copy.finishQuiz,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClueReviewView(
    quiz: ActiveAssessmentQuizState,
    copy: ArenaCopy,
    onBack: () -> Unit,
    onRestartQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reviewItems = arenaClueReviewItems(quiz.items)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag(ArenaTestTags.ClueReview),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = copy.reviewBack,
                    tint = VillageTeal,
                )
            }
            Text(
                copy.reviewClues,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = DeepNight,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        reviewItems.forEachIndexed { index, item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, VillageTeal.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag(ArenaTestTags.clueReviewItem(index)),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        item.prompt,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = DeepNight,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        copy.clueHeader,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = VillageTeal,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.explanation,
                        fontSize = 14.sp,
                        color = DeepNight.copy(alpha = 0.8f),
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(copy.reviewBack)
            }
            Button(
                onClick = onRestartQuiz,
                colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(copy.retry)
            }
        }
    }
}

@Composable
private fun MiloCelebrationDialog(
    stars: Int,
    tokens: Int,
    score: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Quiz trophy", tint = SunshineGold, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text("Quiz Passed!", fontWeight = FontWeight.Black, color = DeepNight)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Congratulations Maxine! You scored $score/10 on this Grade 3 assessment.",
                    fontSize = 15.sp,
                    color = DeepNight,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SunshineGold.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, SunshineGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, contentDescription = "Stars", tint = SunshineGold, modifier = Modifier.size(24.dp))
                            Text("+$stars Stars", fontWeight = FontWeight.Bold, color = DeepNight)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Pets, contentDescription = "Tokens", tint = VillageTeal, modifier = Modifier.size(24.dp))
                            Text("+$tokens Tokens", fontWeight = FontWeight.Bold, color = DeepNight)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Claim rewards", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(22.dp),
    )
}
