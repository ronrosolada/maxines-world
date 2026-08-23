package com.maxinesworld.featurelessonplayer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.KindnessTealText
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import java.io.File

private val ScreenCream = Color(0xFFFFFDF4)

@Composable
fun VideoLibraryScreen(
    onBack: () -> Unit,
    viewModel: VideoLibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VideoLibraryContent(
        state = state,
        onDownload = viewModel::download,
        onDownloadAll = viewModel::downloadAll,
        onPlay = viewModel::play,
        onStopPlaying = viewModel::stopPlaying,
        onVideoCompleted = viewModel::markVideoWatched,
        onOpenAssessment = viewModel::openAssessment,
        onSelectAssessmentOption = viewModel::selectAssessmentOption,
        onCheckAssessmentAnswer = viewModel::checkAssessmentAnswer,
        onNextAssessmentQuestion = viewModel::nextAssessmentQuestion,
        onCloseAssessment = viewModel::closeAssessment,
        onRestartAssessment = viewModel::restartAssessment,
        onRetry = viewModel::refresh,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoLibraryContent(
    state: VideoLibraryUiState,
    onDownload: (String) -> Unit,
    onDownloadAll: () -> Unit,
    onPlay: (String) -> Unit,
    onStopPlaying: () -> Unit,
    onVideoCompleted: (String) -> Unit,
    onOpenAssessment: (String) -> Unit,
    onSelectAssessmentOption: (String) -> Unit,
    onCheckAssessmentAnswer: () -> Unit,
    onNextAssessmentQuestion: () -> Unit,
    onCloseAssessment: () -> Unit,
    onRestartAssessment: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = ScreenCream,
        topBar = {
            val titleText = when (state.filterSubjectId?.lowercase()) {
                "mathematics", "math" -> "Mathematics · Number Fun"
                "english" -> "English · Story Time"
                "science" -> "Science · Discovery"
                "filipino" -> "Filipino · Kwentuhan"
                "araling-panlipunan", "makabansa", "heritage-harbor" -> "Makabansa · Bayan at Kultura"
                "gmrc" -> "GMRC · Kindness"
                else -> "Video Lessons"
            }
            TopAppBar(
                title = { Text(titleText, fontWeight = FontWeight.Bold, color = Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenCream),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        ) {
            when {
                state.isLoading && state.allItems.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VillageTeal,
                    )
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "The video list is unavailable.",
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            state.error,
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        MaxinesPrimaryButton(text = "Try again", onClick = onRetry)
                    }
                }

                else -> {
                    val playing = state.allItems.firstOrNull { it.asset.mediaId == state.playingMediaId }
                    val assessment = state.assessmentQuiz
                    val assessmentAsset = assessment?.let { quiz ->
                        state.allItems.firstOrNull { it.asset.mediaId == quiz.mediaId }?.asset
                    }
                    val currentAssessment = assessmentAsset?.assessment
                    val listState = rememberLazyListState()
                    LaunchedEffect(playing?.asset?.mediaId) {
                        if (playing != null) {
                            listState.animateScrollToItem(1)
                        }
                    }
                    val assessmentIndex = 1 + if (playing?.localPath != null) 1 else 0
                    LaunchedEffect(assessment?.mediaId, assessmentIndex) {
                        if (assessment != null) {
                            listState.animateScrollToItem(assessmentIndex)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Curriculum Video Lessons",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = Ink,
                                        )
                                        Text(
                                            "${state.allItems.size} educational video lessons with quizzes",
                                            fontSize = 13.sp,
                                            color = Ink.copy(alpha = 0.6f)
                                        )
                                    }

                                    val unDownloadedCount = state.allItems.count { it.localPath == null }
                                    if (unDownloadedCount > 0 && !state.isDownloadingAll) {
                                        Button(
                                            onClick = onDownloadAll,
                                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                "Download All ($unDownloadedCount)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    } else if (unDownloadedCount == 0 && state.allItems.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = VillageTeal.copy(alpha = 0.15f),
                                        ) {
                                            Text(
                                                "✓ All Downloaded",
                                                fontWeight = FontWeight.Bold,
                                                color = VillageTeal,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                                if (state.isDownloadingAll) {
                                    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = { state.downloadAllProgress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = VillageTeal,
                                            trackColor = VillageTeal.copy(alpha = 0.2f),
                                        )
                                        Text(
                                            "Downloading ${state.downloadAllCompletedCount} of ${state.downloadAllTotalCount} videos...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VillageTeal
                                        )
                                    }
                                }
                            }
                        }
                        if (playing?.localPath != null) {
                            item(key = "player-${playing.asset.mediaId}") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(playing.asset.title, color = Ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                                            TextButton(onClick = onStopPlaying) {
                                                Text("Close Player", color = KindnessTealText, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        OfflineVideoPlayer(
                                            File(playing.localPath),
                                            onCompleted = { onVideoCompleted(playing.asset.mediaId) },
                                        )
                                    }
                                }
                            }
                        }
                        if (assessment != null && currentAssessment != null) {
                            item(key = "assessment-${assessment.mediaId}") {
                                MediaAssessmentQuizCard(
                                    quiz = assessment,
                                    assessment = currentAssessment,
                                    mediaTitle = assessmentAsset.title,
                                    onSelectOption = onSelectAssessmentOption,
                                    onCheckAnswer = onCheckAssessmentAnswer,
                                    onNextQuestion = onNextAssessmentQuestion,
                                    onRestart = onRestartAssessment,
                                    onClose = onCloseAssessment,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Next / Up Next Section
                        if (state.upcomingItems.isNotEmpty()) {
                            item {
                                Text(
                                    "Up Next",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Ink.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(state.upcomingItems, key = { it.asset.mediaId }) { item ->
                                val isWatchedOrPassed = item.isPassed || item.asset.mediaId in state.watchedMediaIds
                                VideoLibraryItemCard(
                                    item = item,
                                    displayEpisodeNumber = item.asset.episodeNumber,
                                    isPlaying = item.asset.mediaId == state.playingMediaId,
                                    isAssessmentOpen = item.asset.mediaId == state.assessmentQuiz?.mediaId,
                                    canTakeAssessment = isWatchedOrPassed,
                                    isLocked = item.isLocked,
                                    onDownload = { onDownload(item.asset.mediaId) },
                                    onPlay = { onPlay(item.asset.mediaId) },
                                    onOpenAssessment = { onOpenAssessment(item.asset.mediaId) },
                                )
                            }
                        }

                        // Completed Lessons Section (Bottom of list)
                        if (state.completedItems.isNotEmpty()) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        "Completed Lessons (${state.completedItems.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = VillageTeal,
                                    )
                                }
                            }
                            items(state.completedItems, key = { "completed-${it.asset.mediaId}" }) { item ->
                                VideoLibraryItemCard(
                                    item = item,
                                    displayEpisodeNumber = item.asset.episodeNumber,
                                    isPlaying = item.asset.mediaId == state.playingMediaId,
                                    isAssessmentOpen = item.asset.mediaId == state.assessmentQuiz?.mediaId,
                                    canTakeAssessment = true,
                                    isLocked = item.isLocked,
                                    onDownload = { onDownload(item.asset.mediaId) },
                                    onPlay = { onPlay(item.asset.mediaId) },
                                    onOpenAssessment = { onOpenAssessment(item.asset.mediaId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaAssessmentQuizCard(
    quiz: MediaAssessmentQuizState,
    assessment: com.maxinesworld.coremodel.MediaAssessment,
    mediaTitle: String,
    onSelectOption: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("What do you remember?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink)
                    Text(mediaTitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.6f))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close quiz", tint = Ink)
                }
            }

            if (quiz.finished) {
                val passed = quiz.correctCount >= assessment.passingCorrectCount
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (passed) "🎉 Great Job, Maxine!" else "Nice try! Let's watch again.",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Ink,
                    )
                    Text(
                        "You answered ${quiz.correctCount} of ${assessment.items.size} correctly.",
                        fontSize = 14.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                    if (passed) {
                        if (quiz.isReplay) {
                            Text(
                                "Practice complete! Great memory review! ✨",
                                fontWeight = FontWeight.Bold,
                                color = VillageTeal,
                                fontSize = 14.sp,
                            )
                        } else {
                            Text(
                                "+5 ⭐ Stars Earned! Lesson Completed!",
                                fontWeight = FontWeight.ExtraBold,
                                color = VillageTeal,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRestart,
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onClose) {
                            Text("Done", color = Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val currentItem = assessment.items.getOrNull(quiz.questionIndex)
                if (currentItem != null) {
                    Text(
                        "Question ${quiz.questionIndex + 1} of ${assessment.items.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VillageTeal,
                    )
                    Text(
                        currentItem.prompt,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Ink,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentItem.options.forEach { option ->
                            val isSelected = quiz.selectedOptionId == option.id
                            val isChecked = quiz.submitted
                            val isCorrectOption = option.id in currentItem.correctOptionIds
                            val isChosenWrong = isChecked && isSelected && !isCorrectOption

                            val containerColor = when {
                                isChecked && isCorrectOption -> VillageTeal.copy(alpha = 0.15f)
                                isChosenWrong -> Color(0xFFFFEBEE)
                                isSelected -> VillageTeal.copy(alpha = 0.12f)
                                else -> Color(0xFFF7F7F7)
                            }
                            val borderColor = when {
                                isChecked && isCorrectOption -> VillageTeal
                                isChosenWrong -> Color(0xFFE53935)
                                isSelected -> VillageTeal
                                else -> Color.Transparent
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = containerColor,
                                border = BorderStroke(1.5.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isChecked) { onSelectOption(option.id) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) VillageTeal else Color.White),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            option.id.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else Ink,
                                        )
                                    }
                                    Text(
                                        option.text,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (quiz.submitted) {
                        val isCorrect = quiz.selectedOptionId in currentItem.correctOptionIds
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCorrect) VillageTeal.copy(alpha = 0.12f) else Color(0xFFFFF3E0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isCorrect) "✨ Correct! Awesome job!" else "💡 Learning Clue: ${currentItem.explanation}",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                        }
                        Button(
                            onClick = onNextQuestion,
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next Question", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onCheckAnswer,
                            enabled = quiz.selectedOptionId != null,
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Check Answer", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun VideoLibraryItemCard(
    item: VideoLibraryItemUi,
    displayEpisodeNumber: Int,
    isPlaying: Boolean,
    isAssessmentOpen: Boolean,
    canTakeAssessment: Boolean,
    isLocked: Boolean = false,
    onDownload: () -> Unit,
    onPlay: () -> Unit,
    onOpenAssessment: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPassed) Color.White.copy(alpha = 0.85f) else Color.White
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (item.isPassed) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Completed lesson",
                        tint = VillageTeal,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VillageTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            displayEpisodeNumber.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            color = VillageTeal,
                            fontSize = 14.sp,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.asset.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (item.isPassed) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VillageTeal.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "✓ Completed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = VillageTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            "${formatDuration(item.asset.durationSeconds)} • Quarter ${item.asset.quarter}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink.copy(alpha = 0.65f),
                        )
                    }
                }

                when {
                    item.isLocked -> {
                        // Sequence guard rail: locked until the previous lesson passes.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Complete the previous lesson first",
                                color = Ink.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    item.isDownloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = KindnessTealText,
                            strokeWidth = 2.dp,
                        )
                    }

                    item.localPath != null -> {
                        TextButton(onClick = onPlay) {
                            Text(if (item.isPassed) "Rewatch" else "Watch", color = KindnessTealText, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        TextButton(onClick = onDownload) {
                            Text("Download", color = KindnessTealText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (item.localPath != null && item.asset.assessment != null && canTakeAssessment) {
                Spacer(Modifier.height(8.dp))
                if (isAssessmentOpen) {
                    TextButton(
                        onClick = onOpenAssessment,
                        enabled = false,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Memory check is open", color = Ink)
                    }
                } else {
                    TextButton(
                        onClick = onOpenAssessment,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            if (item.isPassed) "Review Quiz" else "What do you remember?",
                            color = KindnessTealText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
