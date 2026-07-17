package com.maxinesworld.gamefireflycatch

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
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

private val Night = Color(0xFF163A4A)
private val Teal = Color(0xFF087F83)
private val Coral = Color(0xFFF47C6B)
private val Cream = Color(0xFFFFF7E8)

@Composable
fun FireflyCatchScreen(
    childId: String,
    rewardBreakId: String,
    modifier: Modifier = Modifier,
    soundEnabled: Boolean = true,
    reducedMotion: Boolean = false,
    durationMillis: Long = RewardBreakClock.DEFAULT_DURATION_MILLIS,
    onExit: (MiniGameResult) -> Unit,
    viewModel: FireflyCatchViewModel = viewModel(
        factory = FireflyCatchViewModelFactory(childId, rewardBreakId, durationMillis)
    )
) {
    val ui by viewModel.state.collectAsState()
    // Host-owned setting; this screen contains no nonessential continuous motion.
    @Suppress("UNUSED_VARIABLE") val motionAllowed = !reducedMotion
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
    LaunchedEffect(ui.breakExpired) {
        if (ui.breakExpired) { if (soundEnabled) sounds.celebrate(); delay(1600); submitResult() }
    }

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Night, Teal)))) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Firefly Garden", ui.remainingMillis, ui.durationMillis, ui.game.score, viewModel::pause) { showExit = true }
            Text(ui.game.feedback, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Field(ui, onTap = { id -> viewModel.tap(id); if (soundEnabled) sounds.tap() },
                Modifier.weight(1f).fillMaxWidth())
        }
        if (ui.paused) PauseOverlay { viewModel.resume() }
        if (showExit) ExitDialog({ showExit = false }, { submitResult() })
    }
}

@Composable
private fun Field(ui: FireflyCatchUiState, onTap: (Int) -> Unit, modifier: Modifier) {
    var w by remember { mutableStateOf(0) }
    var h by remember { mutableStateOf(0) }
    Box(
        modifier.clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = .18f))
            .onGloballyPositioned { w = it.size.width; h = it.size.height }
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        ui.game.sprites.forEach { s ->
            val xdp = with(density) { (s.xPct * w).toDp() }
            val ydp = with(density) { (s.yPct * h).toDp() }
            Box(
                Modifier.offset(x = xdp, y = ydp).size(64.dp)
                    .clip(CircleShape)
                    .background(if (s.kind == SpriteKind.BEE) Color.Transparent else Color(0x33FFF3B0))
                    .clickable { onTap(s.id) }
                    .semantics { role = Role.Button; contentDescription = "Tap the ${s.kind.label}" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(s.kind.drawableRes()),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun Header(title: String, remaining: Long, duration: Long, score: Int, onPause: () -> Unit, onExit: () -> Unit) {
    Surface(color = Teal, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Score $score", color = Color(0xFFF5B82E), fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                Text("Paused", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Night)
                Text("Tap anywhere to continue", fontSize = 18.sp, color = Night)
            }
        }
    }
}

@Composable
private fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text("Leave the game?") },
        text = { Text("Your score is saved. You can play again after another Daily Quest.") },
        confirmButton = { Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Save and leave") } },
        dismissButton = { OutlinedButton(onClick = onStay) { Text("Keep playing") } }
    )
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun SpriteKind.drawableRes(): Int = when (this) {
    SpriteKind.FIREFLY -> R.drawable.ic_firefly
    SpriteKind.BUTTERFLY -> R.drawable.ic_butterfly
    SpriteKind.BEE -> R.drawable.ic_bee
}
