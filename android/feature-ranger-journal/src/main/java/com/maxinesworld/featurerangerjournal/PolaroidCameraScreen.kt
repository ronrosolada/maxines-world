package com.maxinesworld.featurerangerjournal

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val CameraInk = Color(0xFF183B4A)
private val CameraWhite = Color(0xFFFFFFFF)
private val CameraCoral = Color(0xFFF47C6B)

/** Soft mechanical shutter click via SoundPool. */
class ShutterSoundPlayer(context: Context) : AutoCloseable {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val shutter = pool.load(context, R.raw.ranger_shutter, 1)

    fun click() {
        pool.play(shutter, 0.7f, 0.7f, 1, 0, 1f)
    }

    override fun close() = pool.release()
}

@Composable
fun PolaroidCameraScreen(
    childId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSceneId: String? = null,
    viewModel: RangerJournalViewModel = rangerJournalViewModel(childId),
) {
    val context = LocalContext.current
    val sounds = remember { ShutterSoundPlayer(context.applicationContext) }
    val haptic = LocalHapticFeedback.current
    DisposableEffect(sounds) { onDispose { sounds.close() } }

    var selectedSceneId by rememberSaveable {
        mutableStateOf(initialSceneId?.takeIf { JournalScenes.byId(it) != null } ?: JournalScenes.tarsier.id)
    }
    var flash by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by rememberSaveable { mutableStateOf("") }

    val cameraHint = stringResource(R.string.ranger_camera_hint)
    val takePhotoLabel = stringResource(R.string.ranger_take_photo)
    val backLabel = stringResource(R.string.ranger_back)
    val captionHint = stringResource(R.string.ranger_caption_hint)
    val savePhotoLabel = stringResource(R.string.ranger_save_photo)
    val skipNoteLabel = stringResource(R.string.ranger_skip_note)

    LaunchedEffect(flash) {
        if (flash) {
            delay(160)
            flash = false
        }
    }

    val selectedScene = JournalScenes.byId(selectedSceneId) ?: JournalScenes.tarsier

    Box(modifier.fillMaxSize().background(Color(0xFF1A2A3A))) {
        // Viewfinder: full-bleed scene art
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = cameraHint }) {
            drawScene(selectedScene)
        }
        Box(Modifier.fillMaxSize().padding(28.dp)) {
            // Viewfinder frame guides
            listOf(
                Alignment.TopStart, Alignment.TopEnd,
                Alignment.BottomStart, Alignment.BottomEnd,
            ).forEach { corner ->
                Box(
                    Modifier
                        .align(corner)
                        .size(34.dp)
                        .border(3.dp, CameraWhite.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(vertical = 12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(JournalScenes.all, key = JournalScene::id) { scene ->
                        SceneChip(
                            scene = scene,
                            selected = scene.id == selectedSceneId,
                            onClick = { selectedSceneId = scene.id },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ShutterButton(
                    label = takePhotoLabel,
                    onClick = {
                        sounds.click()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        flash = true
                        showNoteDialog = true
                    },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    cameraHint,
                    color = CameraWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }

        FilledIconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(52.dp)
                .semantics { contentDescription = backLabel },
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, backLabel)
        }

        if (flash) {
            Box(Modifier.fillMaxSize().background(CameraWhite.copy(alpha = 0.92f)))
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(stringResource(R.string.ranger_caption_hint)) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(120) },
                    placeholder = { Text(captionHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addEntry(selectedSceneId, noteText)
                    showNoteDialog = false
                    onBack()
                }) { Text(savePhotoLabel) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.addEntry(selectedSceneId, "")
                    showNoteDialog = false
                    onBack()
                }) { Text(skipNoteLabel) }
            },
        )
    }
}

@Composable
private fun SceneChip(scene: JournalScene, selected: Boolean, onClick: () -> Unit) {
    val label = sceneDisplayName(scene.id)
    androidx.compose.material3.Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) CameraWhite else Color.Black.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .height(40.dp)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(scene.accent))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (selected) CameraInk else CameraWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit, label: String) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(CameraWhite.copy(alpha = 0.25f))
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(CameraCoral)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PhotoCamera, label, tint = CameraWhite, modifier = Modifier.size(30.dp))
        }
    }
}