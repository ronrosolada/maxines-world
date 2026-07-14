package com.maxinesworld.gamepawbeats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxinesworld.engineminigame.MiniGameResult
import com.maxinesworld.engineminigame.RewardBreakClock
import kotlinx.coroutines.delay

private val Teal = Color(0xFF087F83)
private val Coral = Color(0xFFF47C6B)
private val Cream = Color(0xFFFFF7E8)
private val Ink = Color(0xFF183B4A)

@Composable
fun PawBeatsScreen(
    childId: String,
    rewardBreakId: String,
    modifier: Modifier = Modifier,
    soundEnabled: Boolean = true,
    reducedMotion: Boolean = false,
    durationMillis: Long = RewardBreakClock.DEFAULT_DURATION_MILLIS,
    onExit: (MiniGameResult) -> Unit,
    viewModel: PawBeatsViewModel = viewModel(
        factory = PawBeatsViewModelFactory(childId, rewardBreakId, durationMillis)
    )
) {
    val ui by viewModel.state.collectAsState()
    // Host-owned setting; this screen contains no nonessential continuous motion.
    @Suppress("UNUSED_VARIABLE") val motionAllowed = !reducedMotion
    val flash by viewModel.flash.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val sounds = remember { ToneCuePlayer() }
    var showExit by remember { mutableStateOf(false) }
    var resultSubmitted by rememberSaveable(rewardBreakId) { mutableStateOf(false) }
    val submitResult = {
        if (!resultSubmitted) {
            resultSubmitted = true
            onExit(viewModel.result())
        }
    }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    BackHandler { showExit = true }
    LaunchedEffect(flash) { if (flash != null && soundEnabled) sounds.pad(flash!!.index) }
    LaunchedEffect(ui.breakExpired) {
        if (ui.breakExpired) { if (soundEnabled) sounds.celebrate(); delay(1600); submitResult() }
    }

    Box(modifier.fillMaxSize().background(Cream)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header("Paw Beats", ui.remainingMillis, ui.durationMillis, ui.game.round, viewModel::pause) { showExit = true }
            Text(ui.game.feedback, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(if (ui.game.awaitingInput) "Your turn" else flash?.let { "Listen: ${it.label}" } ?: "Listen...", color = Teal, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            PadBoard(
                lit = flash,
                enabled = ui.game.awaitingInput && !ui.paused && !ui.breakExpired,
                onPad = { pad -> viewModel.tap(pad); if (soundEnabled) sounds.pad(pad.index) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        if (ui.paused) PauseOverlay { viewModel.resume() }
        if (showExit) ExitDialog({ showExit = false }, { submitResult() })
    }
}

@Composable
private fun PadBoard(lit: Pad?, enabled: Boolean, onPad: (Pad) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PadTile(Pad.CAT, lit, enabled, onPad, Modifier.weight(1f).fillMaxHeight())
            PadTile(Pad.FROG, lit, enabled, onPad, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PadTile(Pad.BIRD, lit, enabled, onPad, Modifier.weight(1f).fillMaxHeight())
            PadTile(Pad.OWL, lit, enabled, onPad, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun PadTile(pad: Pad, lit: Pad?, enabled: Boolean, onPad: (Pad) -> Unit, modifier: Modifier) {
    val isLit = lit == pad
    Card(
        modifier.clickable(enabled = enabled) { onPad(pad) }
            .semantics { role = Role.Button; contentDescription = "${pad.label} pad" },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(if (isLit) Color.White else pad.color.copy(alpha = .92f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(pad.drawableRes()),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Unspecified
                )
                Text(pad.label, color = if (isLit) pad.color else Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun Header(title: String, remaining: Long, duration: Long, round: Int, onPause: () -> Unit, onExit: () -> Unit) {
    Surface(color = Teal, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Round $round", color = Color(0xFFF5B82E), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(formatTime(remaining), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                IconButton(onClick = onPause) { Icon(Icons.Default.Pause, "Pause", tint = Color.White) }
                IconButton(onClick = onExit) { Icon(Icons.Default.Close, "Leave game", tint = Color.White) }
            }
            LinearProgressIndicator(
                progress = { (remaining.toFloat() / duration).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Coral
            )
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)).clickable(onClick = onResume),
        contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Pause, null, Modifier.size(56.dp), tint = Teal)
                Text("Paused", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Tap anywhere to continue", fontSize = 18.sp, color = Ink)
            }
        }
    }
}

@Composable
private fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text("Leave the game?") },
        text = { Text("Your best tune is saved. You can play again after another Daily Quest.") },
        confirmButton = { Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Save and leave") } },
        dismissButton = { OutlinedButton(onClick = onStay) { Text("Keep playing") } }
    )
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun Pad.drawableRes(): Int = when (this) {
    Pad.CAT -> R.drawable.ic_cat_pad
    Pad.FROG -> R.drawable.ic_frog_pad
    Pad.BIRD -> R.drawable.ic_bird_pad
    Pad.OWL -> R.drawable.ic_owl_pad
}
