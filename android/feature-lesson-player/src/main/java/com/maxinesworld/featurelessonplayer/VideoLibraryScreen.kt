package com.maxinesworld.featurelessonplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.Cream
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.Teal40
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
                    LazyColumn(
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
                        items(state.items, key = { it.asset.mediaId }) { item ->
                            VideoLibraryItemCard(
                                item = item,
                                onDownload = { onDownload(item.asset.mediaId) },
                                onPlay = { onPlay(item.asset.mediaId) },
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
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return "%d:%02d".format(minutes, remainder)
}
