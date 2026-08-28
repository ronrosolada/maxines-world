package com.maxinesworld.featurelessonplayer

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.Cream
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.Teal40
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.CheckpointType
import com.maxinesworld.coremodel.VideoCheckpointItem
import kotlinx.coroutines.delay
import java.io.File

@Composable
internal fun VideoStep(
    step: ActivityStep,
    mediaState: MediaDownloadUiState,
    onDownload: () -> Unit,
    onContinue: () -> Unit,
    checkpoints: List<VideoCheckpointItem> = emptyList(),
) {
    val mediaId = step.mediaId
    val localFile = mediaState.filePath
        ?.let(::File)
        ?.takeIf { it.isFile && it.length() > 0L }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = step.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )

        when {
            mediaId.isNullOrBlank() -> {
                MediaStatusCard(
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Teal40) },
                    message = "This video is not configured yet.",
                )
            }

            localFile != null -> OfflineVideoPlayer(localFile, checkpoints = checkpoints)

            else -> {
                MediaStatusCard(
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Teal40) },
                    message = when {
                        mediaState.isDownloading -> "Downloading this video for offline play…"
                        mediaState.error != null -> "The video could not be downloaded. Check the home Wi-Fi and try again."
                        else -> "Download this short video once, then watch it offline."
                    },
                )
                if (mediaState.isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Teal40,
                    )
                } else {
                    MaxinesPrimaryButton(
                        onClick = onDownload,
                        text = if (mediaState.error == null) "Download video" else "Try again",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = mediaId.isNotBlank(),
                    )
                }
            }
        }

        if (localFile == null && !mediaId.isNullOrBlank()) {
            Text(
                text = "You can skip this optional video and continue the lesson.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink.copy(alpha = 0.70f),
            )
        }

        MaxinesPrimaryButton(
            onClick = onContinue,
            text = if (localFile == null) "Skip for now" else "Continue",
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (localFile == null) Teal40.copy(alpha = 0.82f) else Teal40,
        )
    }
}

@Composable
private fun MediaStatusCard(
    icon: @Composable () -> Unit,
    message: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Cream),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(message, color = Ink, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun OfflineVideoPlayer(
    file: File,
    onCompleted: () -> Unit = {},
    checkpoints: List<VideoCheckpointItem> = emptyList(),
) {
    val context = LocalContext.current
    val currentOnCompleted = androidx.compose.runtime.rememberUpdatedState(onCompleted)
    var currentSpeed by androidx.compose.runtime.saveable.rememberSaveable { mutableFloatStateOf(1.0f) }
    var savedPosition by androidx.compose.runtime.saveable.rememberSaveable(file.absolutePath) { androidx.compose.runtime.mutableLongStateOf(0L) }
    val runtime = remember(file.absolutePath, checkpoints) { VideoCheckpointRuntime(checkpoints) }
    var activeCheckpoint by remember { mutableStateOf<VideoCheckpointItem?>(null) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val player = remember(file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(mediaAudioAttributes(), true)
            setMediaItem(MediaItem.fromUri(file.toUri()))
            playbackParameters = PlaybackParameters(currentSpeed)
            playWhenReady = true
            if (savedPosition > 0L) {
                seekTo(savedPosition)
            }
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnCompleted.value()
            }
        }
        player.addListener(listener)
        onDispose {
            savedPosition = player.currentPosition
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, runtime) { while (true) { runtime.check(player.currentPosition)?.let { player.pause(); activeCheckpoint=it; selectedOptionId=null; submitted=false }; delay(200) } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { viewContext ->
                    (LayoutInflater.from(viewContext)
                        .inflate(R.layout.view_offline_video_player, null, false) as PlayerView).apply {
                        this.player = player
                        contentDescription = "Offline lesson video player"
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                update = { it.player = player },
            )
            activeCheckpoint?.let { cp -> VideoCheckpointOverlay(cp, selectedOptionId, submitted, runtime.visibleHint, runtime.answerIsCorrect, { if(!submitted) selectedOptionId=it }, { submitted=selectedOptionId!=null; runtime.submit(selectedOptionId) }, { if(runtime.acknowledge()){ activeCheckpoint=null; player.play() } }) }
        }

        // Playback speed control bar (1.0x, 1.25x, 1.5x, 2.0x)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = "Playback Speed",
                    tint = VillageTeal,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Speed:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink.copy(alpha = 0.7f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSelected = currentSpeed == speed
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) VillageTeal else VillageTeal.copy(alpha = 0.10f),
                        modifier = Modifier.clickable {
                            currentSpeed = speed
                            player.playbackParameters = PlaybackParameters(speed)
                        }
                    ) {
                        Text(
                            text = if (speed == 1.0f) "1.0x" else "${speed}x",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else VillageTeal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

internal class VideoCheckpointRuntime(checkpoints: List<VideoCheckpointItem>) {
 private val ordered=checkpoints.sortedBy{it.positionMs}; private val completed=mutableSetOf<String>(); var activeCheckpoint:VideoCheckpointItem?=null; private set; var selectedOptionId:String?=null; private set; var answerIsCorrect=false; private set; private var wrongAttempts=0
 val completedCheckpointIds:Set<String> get()=completed.toSet(); val visibleHint:String? get()=activeCheckpoint?.feedbackLadder?.let{when(wrongAttempts.coerceAtMost(3)){1->it.hint1Clue;2->it.hint2WorkedExample;3->it.hint3PrereqSubquestion;else->null}}
 fun check(positionMs:Long):VideoCheckpointItem? { if(activeCheckpoint!=null)return null; return ordered.firstOrNull{it.checkpointId !in completed&&positionMs>=it.positionMs}?.also{activeCheckpoint=it;selectedOptionId=null;answerIsCorrect=false;wrongAttempts=0} }
 fun submit(optionId:String?):Boolean {val cp=activeCheckpoint?:return false;if(optionId==null)return false;selectedOptionId=optionId;answerIsCorrect=optionId==cp.correctOptionId;if(!answerIsCorrect)wrongAttempts++;return answerIsCorrect}
 fun acknowledge():Boolean {val cp=activeCheckpoint?:return false;if(selectedOptionId==null)return false;completed+=cp.checkpointId;activeCheckpoint=null;return true}
}
@Composable private fun VideoCheckpointOverlay(cp:VideoCheckpointItem,selected:String?,submitted:Boolean,hint:String?,correct:Boolean,select:(String)->Unit,submit:()->Unit,ack:()->Unit){Surface(modifier=Modifier.fillMaxSize(),color=Cream.copy(alpha=.97f)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(when(cp.type){CheckpointType.PREDICTION->"Make a prediction";CheckpointType.QUICK_CHECK->"Quick check";CheckpointType.HANDS_ON_PAPER->"Hands on paper";CheckpointType.VOCAB_SPOTLIGHT->"Vocabulary spotlight"},fontWeight=FontWeight.Bold,color=VillageTeal);Text(cp.prompt,fontWeight=FontWeight.Bold,color=Ink);cp.options.forEach{o->Surface(modifier=Modifier.fillMaxWidth().clickable{select(o.id)},shape=RoundedCornerShape(8.dp),color=if(selected==o.id)Teal40.copy(alpha=.25f)else Color.White){Text(o.text,Modifier.padding(8.dp),color=Ink)}};if(submitted){Text(if(correct)"Correct!" else hint?:"Try another answer.",color=Ink);MaxinesPrimaryButton(onClick=ack,text="Continue video",modifier=Modifier.fillMaxWidth())}else TextButton(onClick=submit,enabled=selected!=null){Text("Check answer")}}}}
