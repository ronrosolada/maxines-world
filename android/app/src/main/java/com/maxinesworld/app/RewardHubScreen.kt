package com.maxinesworld.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.coredesignsystem.theme.*
import kotlinx.coroutines.launch

@Composable
fun RewardHubScreen(
    childId: String,
    rewardBreakId: String,
    onPlayCatCafe: (Long) -> Unit,
    onPlayParkour: (Long) -> Unit,
    onPlayKittenMatch: (Long) -> Unit,
    onReturnToVillage: () -> Unit,
    viewModel: RewardBreakViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }
    var returning by remember { mutableStateOf(false) }

    LaunchedEffect(childId, rewardBreakId) {
        viewModel.load(childId, rewardBreakId)
    }

    fun beginGame(destination: (Long) -> Unit) {
        if (starting) return
        starting = true
        scope.launch {
            val remaining = viewModel.begin(childId, rewardBreakId)
            starting = false
            remaining?.let(destination)
        }
    }

    fun finishBreak() {
        if (returning) return
        returning = true
        scope.launch {
            viewModel.consume(childId, rewardBreakId)
            onReturnToVillage()
        }
    }

    BackHandler(enabled = !returning) {
        finishBreak()
    }

    when (val current = state) {
        RewardBreakUiState.Loading -> RewardBreakLoading()
        is RewardBreakUiState.Unavailable -> RewardBreakUnavailable(
            message = current.message,
            onReturnToVillage = onReturnToVillage,
        )
        is RewardBreakUiState.Ready -> {
            val breakExpired = current.remainingMillis <= 0L
            val durationMinutes = ((current.durationMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
            val minutesLeft = current.remainingMillis / 60_000L
            val secondsLeft = (current.remainingMillis % 60_000L) / 1000L

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Great work today!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = VillageTeal,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You've earned a $durationMinutes-minute reward break. Choose a game!",
                    fontSize = 18.sp,
                    color = Ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(40.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GameCard(
                        title = "Cat Café Dash",
                        icon = Icons.Default.Restaurant,
                        color = Coral,
                        description = "Serve yummy food to animal friends!",
                        enabled = !breakExpired && !starting,
                        onClick = { beginGame(onPlayCatCafe) },
                    )
                    GameCard(
                        title = "Pawprint Parkour",
                        icon = Icons.Default.DirectionsRun,
                        color = SkyBlue,
                        description = "Jump and run with Milo!",
                        enabled = !breakExpired && !starting,
                        onClick = { beginGame(onPlayParkour) },
                    )
                    GameCard(
                        title = "Kitten Match",
                        icon = Icons.Default.Pets,
                        color = SunshineGold,
                        description = "Find animal friends hiding in pairs!",
                        enabled = !breakExpired && !starting,
                        onClick = { beginGame(onPlayKittenMatch) },
                    )
                }

                Spacer(Modifier.height(32.dp))
                Text(
                    "${minutesLeft}:${secondsLeft.toString().padStart(2, '0')} remaining",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (breakExpired) Coral else VillageTeal,
                )

                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = ::finishBreak,
                    enabled = !returning,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp),
                ) {
                    Icon(Icons.Default.Home, "Village")
                    Spacer(Modifier.width(8.dp))
                    Text("Return to Village", fontSize = 18.sp)
                }
            }
        }
    }
}

/** Fails closed when a child attempts to deep-link directly into a game. */
@Composable
fun RewardBreakRouteGuard(
    childId: String,
    rewardBreakId: String,
    viewModel: RewardBreakViewModel,
    onReturnToVillage: () -> Unit,
    content: @Composable (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(childId, rewardBreakId) {
        viewModel.load(childId, rewardBreakId, requireActive = true)
    }

    when (val current = state) {
        RewardBreakUiState.Loading -> RewardBreakLoading()
        is RewardBreakUiState.Unavailable -> RewardBreakUnavailable(
            message = current.message,
            onReturnToVillage = onReturnToVillage,
        )
        is RewardBreakUiState.Ready -> {
            if (current.started) content(current.remainingMillis)
            else RewardBreakUnavailable(
                message = "Open the reward break from the Playroom first.",
                onReturnToVillage = onReturnToVillage,
            )
        }
    }
}

@Composable
private fun RewardBreakLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = VillageTeal)
    }
}

@Composable
private fun RewardBreakUnavailable(message: String, onReturnToVillage: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, "Reward break unavailable", tint = Coral, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, fontSize = 19.sp, color = Ink)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onReturnToVillage, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Home, "Village")
            Spacer(Modifier.width(8.dp))
            Text("Back to Playroom")
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, title, tint = color, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(description, fontSize = 15.sp, color = Ink.copy(alpha = 0.6f), textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
    }
}
