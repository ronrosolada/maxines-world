package com.maxinesworld.engineactivity.renderers

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineactivity.AudioEvaluation
import com.maxinesworld.engineactivity.AudioEvaluationEngine
import java.io.File

/** Records and replays a short phrase locally; no audio leaves the device. */
@Suppress("DEPRECATION")
@Composable
fun AudioRecordPlaybackRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val evaluator = remember { AudioEvaluationEngine() }
    val output = remember(step.id) { File(context.cacheDir, "voice-${step.id.hashCode()}.m4a") }
    val envelope = remember { mutableStateListOf<Double>() }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var startedAt by remember { mutableStateOf(0L) }
    var evaluation by remember { mutableStateOf<AudioEvaluation?>(null) }
    var permissionNeeded by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionNeeded = !granted }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.release() }
            runCatching { player?.release() }
        }
    }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pakinggan at ulitin: ${step.targetPhrase.ifBlank { step.narrationText }}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (recorder == null) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionNeeded = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        permissionNeeded = false
                        envelope.clear()
                        evaluation = null
                        recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()).apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setOutputFile(output.absolutePath)
                            prepare()
                            start()
                        }
                        startedAt = SystemClock.elapsedRealtime()
                    }
                } else {
                    val duration = SystemClock.elapsedRealtime() - startedAt
                    val activeRecorder = recorder
                    envelope += runCatching { activeRecorder?.maxAmplitude?.div(32767.0) ?: 0.0 }.getOrDefault(0.0)
                    runCatching { activeRecorder?.stop() }
                    activeRecorder?.release()
                    recorder = null
                    evaluation = evaluator.evaluateAudio(duration, envelope.toList())
                }
            }) { Text(if (recorder == null) "Mag-record" else "Itigil") }

            OutlinedButton(enabled = output.exists() && recorder == null, onClick = {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(output.absolutePath)
                    setOnCompletionListener { it.release(); player = null }
                    prepare()
                    start()
                }
            }) { Text("Pakinggan") }
        }
        if (permissionNeeded) Text("Payagan ang mikropono para makapag-record. Hindi ipapadala ang audio.")
        evaluation?.let { result ->
            Text(result.message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { evaluation = null; onHint() }) { Text("Subukan ulit") }
                Button(onClick = {
                    onResult(ActivityResult(step.id, true, 1, 0, SystemClock.elapsedRealtime() - startedAt, scored = false))
                }) { Text("Magpatuloy") }
            }
        }
    }
}
