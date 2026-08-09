package com.maxinesworld.featurelessonplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.Cream
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.Teal40
import com.maxinesworld.coremodel.MediaAsset
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    state: VideoLibraryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDownload: (String) -> Unit,
    onPlay: (String) -> Unit,
    onStopPlaying: () -> Unit,
    onStartAssessment: (String) -> Unit,
    onSelectAssessmentOption: (String) -> Unit,
    onSubmitAssessment: () -> Unit,
    onNextAssessment: () -> Unit,
    onRestartAssessment: () -> Unit,
    onCloseAssessment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Tagalog videos", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Teal40)
                state.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("The video list is unavailable.", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(state.error, color = Ink.copy(alpha = 0.72f))
                    MaxinesPrimaryButton(onClick = onRetry, text = "Try again")
                }
                else -> {
                    val playing = state.playingMediaId?.let { id ->
                        state.items.firstOrNull { it.asset.mediaId == id }
                    }
                    val assessment = state.assessmentQuiz
                    val assessmentAsset = assessment?.let { quiz ->
                        state.items.firstOrNull { it.asset.mediaId == quiz.mediaId }?.asset
                    }
                    val listState = rememberLazyListState()
                    val assessmentIndex = 1 + if (playing?.localPath != null) 1 else 0
                    LaunchedEffect(assessment?.mediaId, assessmentIndex) {
                        if (assessment != null) {
                            listState.animateScrollToItem(assessmentIndex)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text(
                                "Choose a video to download once and watch offline. These videos are optional and never block a lesson.",
                                color = Ink,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        if (playing?.localPath != null) {
                            item(key = "player-${playing.asset.mediaId}") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                                    shape = RoundedCornerShape(18.dp),
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(playing.asset.title, color = Ink, fontWeight = FontWeight.ExtraBold)
                                        OfflineVideoPlayer(File(playing.localPath))
                                        TextButton(onClick = onStopPlaying, modifier = Modifier.fillMaxWidth()) {
                                            Text("Close player")
                                        }
                                    }
                                }
                            }
                        }
                        if (assessment != null && assessmentAsset != null) {
                            item(key = "assessment-${assessment.mediaId}") {
                                MediaAssessmentCard(
                                    asset = assessmentAsset,
                                    quiz = assessment,
                                    onSelectOption = onSelectAssessmentOption,
                                    onSubmit = onSubmitAssessment,
                                    onNext = onNextAssessment,
                                    onRestart = onRestartAssessment,
                                    onClose = onCloseAssessment,
                                )
                            }
                        }
                        items(state.items, key = { it.asset.mediaId }) { item ->
                            VideoLibraryItemCard(
                                item = item,
                                onDownload = { onDownload(item.asset.mediaId) },
                                onPlay = { onPlay(item.asset.mediaId) },
                                onStartAssessment = { onStartAssessment(item.asset.mediaId) },
                                assessmentOpen = assessment?.mediaId == item.asset.mediaId,
                            )
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
    onDownload: () -> Unit,
    onPlay: () -> Unit,
    onStartAssessment: () -> Unit,
    assessmentOpen: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.localPath == null) Icons.Default.CloudDownload else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Teal40,
                )
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                ) {
                    Text(item.asset.title, color = Ink, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${item.asset.height}p · ${formatDuration(item.asset.durationSeconds)}",
                        color = Ink.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                when {
                    item.localPath != null -> TextButton(onClick = onPlay) { Text("Watch") }
                    item.isDownloading -> CircularProgressIndicator(color = Teal40)
                    else -> TextButton(onClick = onDownload) { Text("Download") }
                }
            }
            item.error?.let { error ->
                Text(
                    "Download failed: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (item.localPath != null && item.asset.assessment != null) {
                TextButton(
                    onClick = onStartAssessment,
                    enabled = !assessmentOpen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            role = Role.Button
                        },
                ) {
                    Text(if (assessmentOpen) "Memory check is open" else "What do you remember?")
                }
            }
        }
    }
}

@Composable
private fun MediaAssessmentCard(
    asset: MediaAsset,
    quiz: MediaAssessmentQuizState,
    onSelectOption: (String) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
) {
    val assessment = asset.assessment ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("What do you remember?", color = Ink, fontWeight = FontWeight.ExtraBold)
                    Text(asset.title, color = Ink.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onClose) { Text("Close") }
            }

            if (quiz.finished) {
                val passed = quiz.correctCount >= assessment.passingCorrectCount
                Text(
                    if (passed) "Great remembering!" else "Nice try!",
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "You got ${quiz.correctCount} of ${assessment.items.size}. This is a practice check and does not change lesson rewards.",
                    color = Ink,
                )
                Text(
                    if (passed) {
                        "You remembered the video clues. You can watch another video whenever you like."
                    } else {
                        "You can watch the video again and try this check another time."
                    },
                    color = Ink.copy(alpha = 0.72f),
                )
                MaxinesPrimaryButton(onClick = onRestart, text = "Try again", modifier = Modifier.fillMaxWidth())
            } else {
                val item = assessment.items.getOrNull(quiz.questionIndex)
                if (item == null) {
                    Text("This memory check is unavailable right now.", color = Ink)
                } else {
                    Text(
                        "Question ${quiz.questionIndex + 1} of ${assessment.items.size}",
                        color = Teal40,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(item.prompt, color = Ink, style = MaterialTheme.typography.titleMedium)
                    item.options.forEach { option ->
                        val selected = quiz.selectedOptionId == option.id
                        val correct = option.id in item.correctOptionIds
                        val containerColor = when {
                            quiz.submitted && correct -> MaterialTheme.colorScheme.primaryContainer
                            quiz.submitted && selected -> MaterialTheme.colorScheme.errorContainer
                            selected -> Teal40.copy(alpha = 0.16f)
                            else -> androidx.compose.ui.graphics.Color.White
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !quiz.submitted) { onSelectOption(option.id) }
                                .semantics {
                                    role = Role.Button
                                },
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(option.text, color = Ink, modifier = Modifier.padding(14.dp))
                        }
                    }
                    if (quiz.submitted) {
                        Text(
                            if (quiz.selectedOptionId in item.correctOptionIds) "Correct!" else "Let's look at the clue.",
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(item.explanation, color = Ink.copy(alpha = 0.78f))
                    }
                    MaxinesPrimaryButton(
                        onClick = if (quiz.submitted) onNext else onSubmit,
                        enabled = quiz.submitted || quiz.selectedOptionId != null,
                        text = if (quiz.submitted) {
                            if (quiz.questionIndex == assessment.items.lastIndex) "See result" else "Next question"
                        } else {
                            "Check answer"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "%d:%02d".format(minutes, remainder)
}
