package com.maxinesworld.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import com.maxinesworld.coredesignsystem.theme.Coral
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.VillageTeal

@Composable
fun RewardHubScreen(
    childId: String,
    rewardBreakId: String,
    onPlayCatCafe: (Long) -> Unit,
    onPlayParkour: (Long) -> Unit,
    onPlayKittenMatch: (Long) -> Unit,
    onPlaySourceGame: (String, Long) -> Unit,
    onReturnToVillage: () -> Unit,
    viewModel: RewardBreakViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    MiniGameLibraryScreen(
        childId = childId,
        rewardBreakId = rewardBreakId,
        onPlay = onPlaySourceGame,
        onPlayCatCafe = onPlayCatCafe,
        onPlayParkour = onPlayParkour,
        onPlayKittenMatch = onPlayKittenMatch,
        consumeOnBack = true,
        onBack = onReturnToVillage,
        onReturnToVillage = onReturnToVillage,
        viewModel = viewModel,
    )
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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, "Reward break unavailable", tint = Coral, modifier = Modifier.padding(8.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, fontSize = 19.sp, color = Ink)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onReturnToVillage, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Home, "Playroom")
            Spacer(Modifier.width(8.dp))
            Text("Back to Playroom")
        }
    }
}
