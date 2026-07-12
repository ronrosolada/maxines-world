package com.maxinesworld.gamecatcafe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
fun CatCafeDashScreen(
    childId: String,
    rewardBreakId: String,
    modifier: Modifier = Modifier,
    durationMillis: Long = RewardBreakClock.DEFAULT_DURATION_MILLIS,
    onExit: (MiniGameResult) -> Unit,
    viewModel: CatCafeViewModel = viewModel(
        factory = CatCafeViewModelFactory(childId, rewardBreakId, durationMillis)
    )
) {
    val ui by viewModel.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showExit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sounds = remember { CafeSoundPlayer(context.applicationContext) }
    DisposableEffect(sounds) { onDispose { sounds.close() } }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler { showExit = true }

    LaunchedEffect(ui.breakExpired, ui.game.roundFinished) {
        if (ui.breakExpired && ui.game.roundFinished) {
            if (ui.soundEnabled) sounds.end()
            delay(1600)
            onExit(viewModel.result())
        }
    }

    Box(modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.cat_cafe_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f)))

        val isWide = LocalConfiguration.current.screenWidthDp >= 840
        if (isWide) {
            Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                GameStage(ui, viewModel, sounds, Modifier.weight(3f).fillMaxHeight())
                SidePanel(ui, viewModel, { showExit = true }, Modifier.widthIn(min = 300.dp, max = 390.dp).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactHeader(ui, { showExit = true }, viewModel)
                GameStage(ui, viewModel, sounds, Modifier.weight(1f).fillMaxWidth())
            }
        }

        if (ui.paused) PauseOverlay { viewModel.resume() }
        if (showExit) ExitDialog(
            onStay = { showExit = false },
            onExit = { onExit(viewModel.result()) }
        )
    }
}

@Composable
private fun GameStage(ui: CatCafeUiState, vm: CatCafeViewModel, sounds: CafeSoundPlayer, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(Cream.copy(alpha = .96f))) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OrderHeader(ui.game.order)
            Spacer(Modifier.height(8.dp))
            Text(ui.game.feedback, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (ui.game.showHint) Text("Hint: match every picture on the order card.", color = Teal, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            Tray(ui.game.tray, vm::remove, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            FoodGrid(enabled = !ui.game.roundFinished, onFood = { vm.add(it); if (ui.soundEnabled) sounds.tap() }, Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            if (ui.game.roundFinished) {
                Button(
                    onClick = { if (!ui.breakExpired) { vm.nextRound(); if (ui.soundEnabled) sounds.tap() } },
                    enabled = !ui.breakExpired,
                    modifier = Modifier.heightIn(min = 60.dp).fillMaxWidth(.72f),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) { Text(if (ui.breakExpired) "Break complete" else "Next customer", fontSize = 20.sp) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.clear(); if (ui.soundEnabled) sounds.tap() }, modifier = Modifier.heightIn(min = 56.dp)) { Text("Clear tray", fontSize = 18.sp) }
                    Button(onClick = {
                        val correct = ui.game.tray.sortedBy { it.name } == ui.game.order.items.sortedBy { it.name }
                        vm.serve()
                        if (ui.soundEnabled) { if (correct) sounds.correct() else sounds.retry() }
                    }, modifier = Modifier.heightIn(min = 56.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)) {
                        Icon(Icons.Default.Restaurant, null); Spacer(Modifier.width(8.dp)); Text("Serve order", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHeader(order: CafeOrder) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Image(painterResource(order.customer.drawable), "${order.customer.label}, café customer", Modifier.size(100.dp), contentScale = ContentScale.Fit)
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(14.dp)) {
                Text("${order.customer.label}'s order", color = Ink, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    order.items.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(item.drawable), item.label, Modifier.size(62.dp), contentScale = ContentScale.Fit)
                            Text(item.label, fontSize = 13.sp, color = Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tray(items: List<FoodItem>, onRemove: (Int) -> Unit, modifier: Modifier) {
    Card(modifier.height(92.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color(0xFFFFE0A8))) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (items.isEmpty()) Text("Your tray is empty", color = Ink, fontSize = 17.sp)
            items.forEachIndexed { index, item ->
                Image(
                    painterResource(item.drawable), "${item.label} on tray; tap to remove",
                    Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).clickable { onRemove(index) }.padding(3.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun FoodGrid(enabled: Boolean, onFood: (FoodItem) -> Unit, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(94.dp), modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FoodItem.entries) { item ->
            Card(
                Modifier.aspectRatio(1f).clickable(enabled = enabled) { onFood(item) }.semantics { contentDescription = "Add ${item.label} to tray" },
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .96f))
            ) {
                Column(Modifier.fillMaxSize().padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(painterResource(item.drawable), null, Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
                    Text(item.label, fontSize = 13.sp, color = Ink, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SidePanel(ui: CatCafeUiState, vm: CatCafeViewModel, onExit: () -> Unit, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(Teal.copy(alpha = .96f))) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cat Café Dash", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            Text(formatTime(ui.remainingMillis), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp,
                modifier = Modifier.semantics { contentDescription = "${formatTime(ui.remainingMillis)} reward break time remaining" })
            LinearProgressIndicator(
                progress = { (ui.remainingMillis.toFloat() / ui.durationMillis).coerceIn(0f,1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape), color = Color(0xFFF5B82E)
            )
            Spacer(Modifier.height(20.dp))
            Image(painterResource(if (ui.game.roundFinished) R.drawable.milo_happy else R.drawable.milo_neutral), "Milo the café helper", Modifier.height(220.dp), contentScale = ContentScale.Fit)
            Text("Orders served", color = Color.White.copy(alpha=.86f), fontSize = 17.sp)
            Text("${ui.game.correctOrders}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = vm::toggleSound, Modifier.size(56.dp).background(Color.White.copy(alpha=.15f), CircleShape)) {
                    Icon(if (ui.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, "Toggle sound", tint = Color.White)
                }
                IconButton(onClick = vm::toggleReducedMotion, Modifier.size(56.dp).background(Color.White.copy(alpha=.15f), CircleShape)) {
                    Icon(Icons.Default.AccessibilityNew, "Toggle reduced motion", tint = Color.White)
                }
                IconButton(onClick = vm::pause, Modifier.size(56.dp).background(Color.White.copy(alpha=.15f), CircleShape)) {
                    Icon(Icons.Default.Pause, "Pause game", tint = Color.White)
                }
                IconButton(onClick = onExit, Modifier.size(56.dp).background(Color.White.copy(alpha=.15f), CircleShape)) {
                    Icon(Icons.Default.ExitToApp, "Leave game", tint = Color.White)
                }
            }
            if (ui.breakExpired) Text("Finish this order, then the café closes for a rest.", color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CompactHeader(ui: CatCafeUiState, onExit: () -> Unit, vm: CatCafeViewModel) {
    Surface(color = Teal, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Cat Café Dash", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(formatTime(ui.remainingMillis), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            IconButton(onClick = vm::pause) { Icon(Icons.Default.Pause, "Pause", tint = Color.White) }
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, "Leave game", tint = Color.White) }
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.72f)).clickable(onClick=onResume), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Pause, null, Modifier.size(56.dp), tint=Teal)
                Text("Café paused", fontSize=28.sp, fontWeight=FontWeight.Bold, color=Ink)
                Text("Tap anywhere to continue", fontSize=18.sp, color=Ink)
            }
        }
    }
}

@Composable
private fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest=onStay,
        title={ Text("Leave the café?") },
        text={ Text("Your served orders will be saved. You can return after another Daily Quest.") },
        confirmButton={ Button(onClick=onExit, colors=ButtonDefaults.buttonColors(containerColor=Teal)) { Text("Save and leave") } },
        dismissButton={ OutlinedButton(onClick=onStay) { Text("Keep playing") } }
    )
}

private fun formatTime(ms: Long): String {
    val seconds=(ms/1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds/60, seconds%60)
}
