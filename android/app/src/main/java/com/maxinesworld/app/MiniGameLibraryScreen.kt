package com.maxinesworld.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.coredesignsystem.theme.Coral
import com.maxinesworld.coredesignsystem.theme.Cream
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.LeafGreen
import com.maxinesworld.coredesignsystem.theme.SkyBlue
import com.maxinesworld.coredesignsystem.theme.StoryPurple
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameLibraryScreen(
    childId: String,
    rewardBreakId: String,
    onPlay: (String, Long) -> Unit,
    onBack: () -> Unit,
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

    fun beginGame(game: EmbeddedMiniGame) {
        if (starting) return
        starting = true
        scope.launch {
            val remaining = viewModel.begin(childId, rewardBreakId)
            starting = false
            remaining?.let { onPlay(game.slug, it) }
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

    BackHandler(enabled = !returning) { onBack() }

    when (val current = state) {
        RewardBreakUiState.Loading -> MiniGameLibraryLoading()
        is RewardBreakUiState.Unavailable -> MiniGameLibraryUnavailable(
            message = current.message,
            onReturnToVillage = onReturnToVillage,
        )
        is RewardBreakUiState.Ready -> {
            val breakExpired = current.remainingMillis <= 0L
            val minutes = current.remainingMillis / 60_000L
            val seconds = (current.remainingMillis % 60_000L) / 1_000L
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("More mini-games") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to games")
                            }
                        },
                        actions = {
                            Row(
                                modifier = Modifier.padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = "Time remaining", tint = VillageTeal)
                                Text(
                                    "$minutes:${seconds.toString().padStart(2, '0')}",
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = if (breakExpired) Coral else VillageTeal,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                    )
                },
            ) { paddingValues ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 170.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Cream)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LibraryHeader(
                            durationMillis = current.durationMillis,
                            breakExpired = breakExpired,
                        )
                    }
                    items(MiniGameCatalog.games, key = EmbeddedMiniGame::slug) { game ->
                        MiniGameChoiceCard(
                            game = game,
                            enabled = !breakExpired && !starting,
                            onClick = { beginGame(game) },
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AttributionCard()
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Button(
                            onClick = ::finishBreak,
                            enabled = !returning,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Playroom")
                            Spacer(Modifier.width(8.dp))
                            Text("Return to Playroom")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(durationMillis: Long, breakExpired: Boolean) {
    val durationMinutes = ((durationMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = VillageTeal, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose a game for your $durationMinutes-minute reward break.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = VillageTeal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (breakExpired) "This reward break has finished." else "Everything here is bundled for offline play.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (breakExpired) Coral else Ink.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MiniGameChoiceCard(
    game: EmbeddedMiniGame,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = categoryAccent(game.category)
    Card(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(game.icon, style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    game.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AttributionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StoryPurple.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Offline game collection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StoryPurple,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Embedded from mazipan/mini-games at a pinned source version. MIT licensed. No account, analytics, or network access is used.",
                style = MaterialTheme.typography.bodySmall,
                color = Ink.copy(alpha = 0.75f),
            )
        }
    }
}

private fun categoryAccent(category: EmbeddedMiniGameCategory): Color = when (category) {
    EmbeddedMiniGameCategory.ARCADE -> Coral
    EmbeddedMiniGameCategory.PUZZLE -> LeafGreen
    EmbeddedMiniGameCategory.BOARD -> SunshineGold
    EmbeddedMiniGameCategory.WORD -> SkyBlue
}

@Composable
private fun MiniGameLibraryLoading() {
    Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = VillageTeal)
    }
}

@Composable
private fun MiniGameLibraryUnavailable(
    message: String,
    onReturnToVillage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, textAlign = TextAlign.Center, color = Ink)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onReturnToVillage) { Text("Back to Playroom") }
    }
}
