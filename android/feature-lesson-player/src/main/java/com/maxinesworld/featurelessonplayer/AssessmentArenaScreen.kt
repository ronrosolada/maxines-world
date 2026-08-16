package com.maxinesworld.featurelessonplayer

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        onSubmitAnswer = viewModel::submitAnswer,
        onNextQuestion = viewModel::nextQuestion,
        onRestartQuiz = viewModel::restartQuiz,
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
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
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
                onSubmitAnswer = onSubmitAnswer,
                onNextQuestion = onNextQuestion,
                onRestartQuiz = onRestartQuiz,
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

        // Milo Reward Celebration Modal Dialog
        if (state.showCelebrationDialog && state.activeQuiz?.isPassed == true) {
            MiloCelebrationDialog(
                stars = state.activeQuiz.earnedStars,
                tokens = state.activeQuiz.earnedTokens,
                score = state.activeQuiz.correctCount,
                onDismiss = onDismissCelebration,
            )
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
                                "Assessment Arena 🏆",
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
                                Text("⭐", fontSize = 12.sp)
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
                                "Assessment Arena 🏆",
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
                            Text("⭐", fontSize = 16.sp)
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
                "mathematics" to ("Mathematics 🔢" to "Number Fun"),
                "science" to ("Science 🔬" to "Discovery"),
                "english" to ("English 📖" to "Story Time"),
                "gmrc" to ("GMRC 💖" to "Kindness"),
                "filipino" to ("Filipino 🇵🇭" to "Kwentuhan"),
                "makabansa" to ("Makabansa 🗺️" to "Bayan at Kultura"),
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
                        color = if (isSelected) VillageTeal else White,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) VillageTeal else VillageTeal.copy(alpha = 0.2f)),
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
            shape = RoundedCornerShape(18.dp),
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
                                if (isPassed) "Retake 🔄" else "Take Quiz ▶",
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
                            if (isPassed) "Retake 🔄" else "Take Quiz ▶",
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
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestartQuiz: () -> Unit,
    onExitQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentItem = quiz.items.getOrNull(quiz.currentIndex)

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
            IconButton(onClick = onExitQuiz) {
                Icon(Icons.Default.Close, contentDescription = "Exit Quiz", tint = DeepNight)
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
                        if (quiz.isPassed) "🎉 Amazing Job, Maxine!" else "💪 Good Effort, Maxine!",
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
                                "Rewards Earned: +${quiz.earnedStars} ⭐ Stars  •  +${quiz.earnedTokens} 🐾 Tokens",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DeepNight,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    } else {
                        Text(
                            "Score at least 8/10 to pass and earn rewards.",
                            fontSize = 14.sp,
                            color = OnError,
                            fontWeight = FontWeight.Medium,
                        )
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
                            Text("Try Again")
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

            // Options List (A, B, C, D)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentItem.options.forEach { option ->
                    val isSelected = quiz.selectedOptionId == option.id
                    val isSubmitted = quiz.isAnswerSubmitted
                    val isCorrectOption = option.id in currentItem.correctOptionIds

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

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected || isSubmitted) 2.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitted) { onSelectOption(option.id) },
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
                                    option.id.uppercase(),
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
            }

            Spacer(Modifier.height(16.dp))

            // Feedback / Explanation Box (on submission)
            if (quiz.isAnswerSubmitted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (quiz.isCorrect) SuccessGreen.copy(alpha = 0.12f) else SunshineGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (quiz.isCorrect) SuccessGreen else SunshineGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (quiz.isCorrect) "✨ Correct! Awesome job!" else "💡 Milo's Learning Clue:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DeepNight,
                        )
                        Spacer(Modifier.height(4.dp))
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
                        Text("Check Answer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Button(
                        onClick = onNextQuestion,
                        colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            if (quiz.currentIndex + 1 < quiz.items.size) "Next Question →" else "Finish Quiz 🏁",
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
                Text("🏆", fontSize = 32.sp)
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
                            Text("⭐", fontSize = 24.sp)
                            Text("+$stars Stars", fontWeight = FontWeight.Bold, color = DeepNight)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🐾", fontSize = 24.sp)
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
                Text("Claim Rewards! ✨", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = White,
        shape = RoundedCornerShape(22.dp),
    )
}
