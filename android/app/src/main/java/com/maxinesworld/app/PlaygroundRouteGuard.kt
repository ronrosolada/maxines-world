package com.maxinesworld.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlaygroundRouteGuard(
    onBlockedExit: () -> Unit,
    viewModel: PlaygroundAccessViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        PlaygroundAccessUiState.Loading -> BlockedGate(loading = true)
        is PlaygroundAccessUiState.Allowed -> content()
        is PlaygroundAccessUiState.Blocked -> BlockedGate(onBackToVillage = onBlockedExit)
    }
}

@Composable
private fun BlockedGate(loading: Boolean = false, onBackToVillage: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E8))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (loading) "Checking Playground…" else "Playground is closed",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF3A6B63),
        )
        if (!loading) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Complete today's quests to unlock.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
            )
            onBackToVillage?.let { back ->
                Spacer(Modifier.height(24.dp))
                Button(onClick = back, modifier = Modifier.testTag("blocked_back_button")) {
                    Text("Back to Village")
                }
            }
        }
    }
}
