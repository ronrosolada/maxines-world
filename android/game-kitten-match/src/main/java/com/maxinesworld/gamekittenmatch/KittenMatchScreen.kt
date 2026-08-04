package com.maxinesworld.gamekittenmatch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
fun KittenMatchScreen(
    childId: String,
    rewardBreakId: String,
    modifier: Modifier = Modifier,
    soundEnabled: Boolean = true,
    reducedMotion: Boolean = false,
    durationMillis: Long = RewardBreakClock.DEFAULT_DURATION_MILLIS,
    onExit: (MiniGameResult) -> Unit,
    viewModel: KittenMatchViewModel = viewModel(
        factory = KittenMatchViewModelFactory(childId, rewardBreakId, durationMillis)
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
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    BackHandler { showExit = true }
    LaunchedEffect(ui.breakExpired) {
        if (ui.breakExpired) { if (soundEnabled) sounds.celebrate(); delay(1600); submitResult() }
    }

    Box(modifier.fillMaxSize().background(Cream)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Header("Kitten Match", ui.remainingMillis, ui.durationMillis, viewModel::pause) { showExit = true }
            Text(ui.game.feedback, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Pairs found: ${ui.game.matchedPairs}", color = Teal, fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp), modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.game.cards, key = { it.slot }) { card ->
                    CardTile(card) { viewModel.flip(card.slot); if (soundEnabled) sounds.tap() }
                }
            }
            if (ui.game.boardCleared) {
                Button(
                    onClick = { if (!ui.breakExpired) { viewModel.nextRound(); if (soundEnabled) sounds.good() } },
                    enabled = !ui.breakExpired, modifier = Modifier.heightIn(min = 60.dp).fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) { Text(if (ui.breakExpired) "Break complete" else "New board", fontSize = 20.sp) }
            }
        }
        if (ui.paused) PauseOverlay { viewModel.resume() }
        if (showExit) ExitDialog({ showExit = false }, { submitResult() })
    }
}

@Composable
private fun CardTile(card: MatchCard, onTap: () -> Unit) {
    val faceUp = card.faceUp || card.matched
    Card(
        Modifier.aspectRatio(1f)
            .clickable(enabled = !faceUp) { onTap() }
            .semantics { role = Role.Button; contentDescription = if (faceUp) card.face.label else "Face-down card" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            if (card.matched) Color(0xFFB8E4C9) else if (faceUp) Color.White else Teal
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (faceUp) {
                Icon(
                    painter = painterResource(card.face.drawableRes()),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.Unspecified
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun Header(title: String, remaining: Long, duration: Long, onPause: () -> Unit, onExit: () -> Unit) {
    Surface(color = Teal, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
        text = { Text("Your matched friends are saved. You can play again after another Daily Quest.") },
        confirmButton = { Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Save and leave") } },
        dismissButton = { OutlinedButton(onClick = onStay) { Text("Keep playing") } }
    )
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun MatchFace.drawableRes(): Int = when (this) {
    MatchFace.MAXINE -> R.drawable.ic_maxine
    MatchFace.MILO -> R.drawable.ic_milo
    MatchFace.TARSIER -> R.drawable.ic_tarsier
    MatchFace.EAGLE -> R.drawable.ic_eagle
    MatchFace.TAMARAW -> R.drawable.ic_tamaraw
    MatchFace.COLUGO -> R.drawable.ic_colugo
    MatchFace.PEACOCK -> R.drawable.ic_peacock
    MatchFace.WARTY_PIG -> R.drawable.ic_warty_pig
}
