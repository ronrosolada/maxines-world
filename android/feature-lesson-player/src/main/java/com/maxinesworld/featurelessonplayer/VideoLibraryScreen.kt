package com.maxinesworld.featurelessonplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to home",
                            tint = Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenCream),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = VillageTeal,
                        modifier = Modifier.align(Alignment.Center),
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
                                            "Watch lessons offline to earn wildlife stickers.",
                                            color = Ink.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                    val undownloadedCount = state.allItems.count { it.localPath == null }
                                    if (undownloadedCount > 0) {
                                        Button(
                                            onClick = onDownloadAll,
                                            enabled = !state.isDownloadingAll,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VillageTeal,
                                                disabledContainerColor = VillageTeal.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                        ) {
                                            Text(
                                                if (state.isDownloadingAll) "Downloading..." else "Download All ($undownloadedCount)",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
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
                        if (assessment != null && currentAssessment != null && assessmentAsset != null) {
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
                                VideoLibraryItemCard(
                                    item = item,
                                    isPlaying = item.asset.mediaId == state.playingMediaId,
                                    isAssessmentOpen = item.asset.mediaId == state.assessmentQuiz?.mediaId,
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
                                    isPlaying = item.asset.mediaId == state.playingMediaId,
                                    isAssessmentOpen = item.asset.mediaId == state.assessmentQuiz?.mediaId,
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("What do you remember?", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink)
                    Text(mediaTitle, fontSize = 13.sp, color = Ink.copy(alpha = 0.7f))
                }
                TextButton(onClick = onClose) {
                    Text("Close", color = KindnessTealText, fontWeight = FontWeight.Bold)
                }
            }

            if (quiz.finished) {
                val passed = quiz.correctCount >= assessment.passingCorrectCount
                Text(
                    if (passed) "Great job! ⭐" else "Nice try! Keep practicing.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink
                )
                Text(
                    "You got ${quiz.correctCount} of ${assessment.items.size} correct.",
                    fontSize = 14.sp,
                    color = Ink
                )
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try again", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                val currentItem = assessment.items.getOrNull(quiz.questionIndex)
                if (currentItem != null) {
                    Text(
                        "Question ${quiz.questionIndex + 1} of ${assessment.items.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VillageTeal
                    )
                    Text(
                        currentItem.prompt,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentItem.options.forEach { option ->
                            val isSelected = quiz.selectedOptionId == option.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) VillageTeal.copy(alpha = 0.15f) else ScreenCream,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!quiz.submitted) onSelectOption(option.id) }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${option.id.uppercase()}.",
                                        fontWeight = FontWeight.Bold,
                                        color = VillageTeal,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        option.text,
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
                        Text(
                            if (isCorrect) "✓ Correct!" else "Not quite right.",
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) VillageTeal else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            currentItem.explanation,
                            fontSize = 13.sp,
                            color = Ink.copy(alpha = 0.8f)
                        )
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
    isPlaying: Boolean,
    isAssessmentOpen: Boolean,
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
                } else if (item.localPath != null) {
                    Icon(
                        Icons.Filled.PlayCircle,
                        contentDescription = "Ready to play",
                        tint = KindnessTealText,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = "Available for download",
                        tint = KindnessTealText,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.asset.title,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (item.isPassed) "Completed · 480p · ${formatDuration(item.asset.durationSeconds)}"
                        else "480p · ${formatDuration(item.asset.durationSeconds)}",
                        color = if (item.isPassed) VillageTeal else Ink,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (item.error != null) {
                        Text(
                            item.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                when {
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
            if (item.localPath != null && item.asset.assessment != null) {
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
