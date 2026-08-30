package com.maxinesworld.featurerangerjournal

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val JournalTeal = Color(0xFF087F83)
private val JournalCoral = Color(0xFFF47C6B)
private val JournalGold = Color(0xFFF5B82E)
private val JournalCream = Color(0xFFFFF7E8)
private val JournalInk = Color(0xFF183B4A)
private val JournalWhite = Color(0xFFFFFFFF)

@Composable
fun rangerJournalViewModel(childId: String): RangerJournalViewModel {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { DataStoreJournalStore(context) }
    return viewModel(factory = RangerJournalViewModelFactory(childId, store))
}

@Composable
fun RangerJournalScreen(
    childId: String,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RangerJournalViewModel = rangerJournalViewModel(childId),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteCandidate by remember { mutableStateOf<JournalEntry?>(null) }

    // Re-sync when the screen re-enters composition (e.g. returning from the camera).
    LaunchedEffect(Unit) { viewModel.refresh() }

    val sceneNames = JournalScenes.all.associate { scene -> scene.id to sceneDisplayName(scene.id) }
    val totalLabel = stringResource(R.string.ranger_export_section_total)
    val takenLabel = stringResource(R.string.ranger_export_field_taken)
    val noteLabel = stringResource(R.string.ranger_export_field_note)
    val favoriteLabel = stringResource(R.string.ranger_export_field_favorite)
    val shareTitle = stringResource(R.string.ranger_share_intent_title)
    val backLabel = stringResource(R.string.ranger_back)
    val takePhotoLabel = stringResource(R.string.ranger_take_photo)
    val shareLabel = stringResource(R.string.ranger_share)
    val markFavoriteLabel = stringResource(R.string.ranger_mark_favorite)
    val unmarkFavoriteLabel = stringResource(R.string.ranger_unmark_favorite)
    val removePhotoLabel = stringResource(R.string.ranger_remove_photo)

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFBFE8F5), JournalCream)))) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            JournalHeader(
                photoCount = state.entries.size,
                onBack = onBack,
                onTakePhoto = onTakePhoto,
                onShare = {
                    val markdown = RangerJournalExporter.exportMarkdown(
                        entries = state.entries,
                        sceneName = { sceneNames[it] ?: it },
                        totalLabel = totalLabel,
                        takenLabel = takenLabel,
                        noteLabel = noteLabel,
                        favoriteLabel = favoriteLabel,
                    )
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/markdown"
                        putExtra(Intent.EXTRA_TEXT, markdown)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, shareTitle)) }
                },
                backLabel = backLabel,
                takePhotoLabel = takePhotoLabel,
                shareLabel = shareLabel,
            )
            when {
                state.loading && state.entries.isEmpty() -> JournalEmptyLoading()
                state.entries.isEmpty() -> JournalEmptyState(onTakePhoto)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 230.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.entries, key = JournalEntry::id) { entry ->
                        PolaroidJournalCard(
                            entry = entry,
                            sceneName = sceneNames[entry.sceneId] ?: entry.sceneId,
                            markFavoriteLabel = markFavoriteLabel,
                            unmarkFavoriteLabel = unmarkFavoriteLabel,
                            removePhotoLabel = removePhotoLabel,
                            onToggleFavorite = { viewModel.toggleFavorite(entry.id) },
                            onDelete = { deleteCandidate = entry },
                        )
                    }
                }
            }
        }
    }

    deleteCandidate?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.ranger_remove_confirm_title)) },
            text = { Text(stringResource(R.string.ranger_remove_confirm_body)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.delete(entry.id)
                    deleteCandidate = null
                }) { Text(stringResource(R.string.ranger_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.ranger_cancel)) }
            },
        )
    }
}

@Composable
private fun JournalHeader(
    photoCount: Int,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onShare: () -> Unit,
    backLabel: String,
    takePhotoLabel: String,
    shareLabel: String,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(52.dp)
                    .semantics { contentDescription = backLabel },
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, backLabel, tint = JournalInk)
            }
            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = JournalTeal, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ranger_journal_title),
                    color = JournalInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                )
                Text(
                    stringResource(R.string.ranger_photo_count, photoCount),
                    color = JournalTeal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
            IconButton(
                onClick = onTakePhoto,
                modifier = Modifier
                    .size(56.dp)
                    .background(JournalTeal, CircleShape)
                    .semantics { contentDescription = takePhotoLabel },
            ) {
                Icon(Icons.Filled.CameraAlt, takePhotoLabel, tint = JournalWhite)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onShare) {
                Icon(Icons.Filled.Share, shareLabel, tint = JournalTeal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(shareLabel, color = JournalTeal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PolaroidJournalCard(
    entry: JournalEntry,
    sceneName: String,
    markFavoriteLabel: String,
    unmarkFavoriteLabel: String,
    removePhotoLabel: String,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val scene = JournalScenes.byId(entry.sceneId) ?: JournalScenes.tarsier
    val date = remember(entry.takenAtEpochMillis) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(entry.takenAtEpochMillis))
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Polaroid photo of $sceneName, $date${entry.caption.takeIf { it.isNotBlank() }?.let { ". $it" } ?: ""}"
                role = Role.Image
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = JournalWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                JournalSceneArtwork(scene, Modifier.fillMaxSize())
                if (entry.isFavorite) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = CircleShape,
                        color = JournalGold.copy(alpha = 0.95f),
                    ) {
                        Icon(Icons.Filled.Star, stringResource(R.string.ranger_favorite), tint = JournalWhite, modifier = Modifier.padding(8.dp).size(18.dp))
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(sceneName, color = JournalInk, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(date, color = JournalInk.copy(alpha = 0.65f), fontSize = 13.sp)
                if (entry.caption.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(entry.caption, color = JournalInk, fontSize = 14.sp, maxLines = 3)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = if (entry.isFavorite) unmarkFavoriteLabel else markFavoriteLabel
                            },
                    ) {
                        Icon(
                            if (entry.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            null,
                            tint = if (entry.isFavorite) JournalGold else JournalInk.copy(alpha = 0.5f),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = removePhotoLabel },
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = JournalCoral)
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEmptyState(onTakePhoto: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.MenuBook, null, tint = JournalTeal.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.ranger_empty_title),
            color = JournalInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.ranger_empty_body),
            color = JournalInk.copy(alpha = 0.75f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onTakePhoto,
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(JournalTeal),
        ) {
            Icon(Icons.Filled.CameraAlt, stringResource(R.string.ranger_take_photo))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ranger_take_photo), fontSize = 18.sp)
        }
    }
}

@Composable
private fun JournalEmptyLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = JournalTeal)
    }
}

@Composable
internal fun sceneDisplayName(sceneId: String): String = when (sceneId) {
    "eagle" -> stringResource(R.string.ranger_scene_eagle)
    "tamaraw" -> stringResource(R.string.ranger_scene_tamaraw)
    "pawikan" -> stringResource(R.string.ranger_scene_pawikan)
    else -> stringResource(R.string.ranger_scene_tarsier)
}