package com.maxinesworld.app

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.maxinesworld.coredesignsystem.theme.White
import kotlinx.coroutines.launch

private data class BuiltInMiniGame(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val accent: Color,
    @DrawableRes val thumbnailRes: Int,
)

private val BuiltInMiniGames = listOf(
    BuiltInMiniGame(
        id = "cat-cafe",
        title = "Cat Café Dash",
        description = "Serve yummy food to animal friends!",
        category = "Playful",
        accent = Coral,
        thumbnailRes = com.maxinesworld.gamecatcafe.R.drawable.cat_cafe_background,
    ),
    BuiltInMiniGame(
        id = "pawprint-parkour",
        title = "Pawprint Parkour",
        description = "Jump and run with Milo!",
        category = "Adventure",
        accent = SkyBlue,
        thumbnailRes = com.maxinesworld.gamepawprintparkour.R.drawable.parkour_background,
    ),
    BuiltInMiniGame(
        id = "kitten-match",
        title = "Kitten Match",
        description = "Find animal friends hiding in pairs!",
        category = "Memory",
        accent = SunshineGold,
        thumbnailRes = com.maxinesworld.gamekittenmatch.R.drawable.ic_milo,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameLibraryScreen(
    childId: String,
    rewardBreakId: String,
    onPlay: (String, Long) -> Unit,
    onPlayCatCafe: (Long) -> Unit = {},
    onPlayParkour: (Long) -> Unit = {},
    onPlayKittenMatch: (Long) -> Unit = {},
    consumeOnBack: Boolean = false,
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

    fun beginGame(launch: (Long) -> Unit) {
        if (starting) return
        starting = true
        scope.launch {
            val remaining = viewModel.begin(childId, rewardBreakId)
            starting = false
            remaining?.let(launch)
        }
    }

    fun finishBreak() {
        if (returning) return
        if ((state as? RewardBreakUiState.Ready)?.started != true) {
            onReturnToVillage()
            return
        }
        returning = true
        scope.launch {
            viewModel.consume(childId, rewardBreakId)
            onReturnToVillage()
        }
    }

    BackHandler(enabled = !returning) {
        if (consumeOnBack) finishBreak() else onBack()
    }

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
                        title = { Text("Mini Games") },
                        navigationIcon = {
                            IconButton(onClick = { if (consumeOnBack) finishBreak() else onBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (consumeOnBack) "Return to Playroom" else "Back to reward break",
                                )
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
                    columns = GridCells.Adaptive(minSize = 190.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Cream)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LibraryHeader(
                            durationMillis = current.durationMillis,
                            breakExpired = breakExpired,
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = ::finishBreak,
                                enabled = !returning,
                            ) {
                                Icon(Icons.Default.Home, contentDescription = "Playroom")
                                Spacer(Modifier.width(6.dp))
                                Text("Return to Playroom")
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MenuSectionHeader(
                            title = "Maxine's World games",
                            subtitle = "Friendly games made for your reward break",
                        )
                    }
                    items(BuiltInMiniGames, key = { "built-in-${it.id}" }) { game ->
                        MiniGameChoiceCard(
                            title = game.title,
                            category = game.category,
                            description = game.description,
                            accent = game.accent,
                            enabled = !breakExpired && !starting,
                            onClick = {
                                beginGame { duration ->
                                    when (game.id) {
                                        "cat-cafe" -> onPlayCatCafe(duration)
                                        "pawprint-parkour" -> onPlayParkour(duration)
                                        "kitten-match" -> onPlayKittenMatch(duration)
                                    }
                                }
                            },
                        ) {
                            ArtworkThumbnail(game.thumbnailRes, game.accent)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MenuSectionHeader(
                            title = "Friendly favorites",
                            subtitle = "Picked for Maxine — quick and playful",
                        )
                    }
                    items(
                        MiniGameShelf.shelfOrder(MiniGameCatalog.games).take(MiniGameShelf.kidFriendlyCount(MiniGameCatalog.games)),
                        key = EmbeddedMiniGame::slug,
                    ) { game ->
                        val accent = categoryAccent(game.category)
                        MiniGameChoiceCard(
                            title = game.title,
                            category = game.category.label,
                            description = game.description,
                            accent = accent,
                            titleColor = categoryTextColor(game.category),
                            enabled = !breakExpired && !starting,
                            onClick = { beginGame { duration -> onPlay(game.slug, duration) } },
                        ) {
                            ArtworkThumbnail(game.thumbnailRes, accent)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MenuSectionHeader(
                            title = "Puzzle & classic games",
                            subtitle = "${MiniGameCatalog.games.size} more games, bundled for offline play",
                        )
                    }
                    items(
                        MiniGameShelf.shelfOrder(MiniGameCatalog.games).drop(MiniGameShelf.kidFriendlyCount(MiniGameCatalog.games)),
                        key = EmbeddedMiniGame::slug,
                    ) { game ->
                        val accent = categoryAccent(game.category)
                        MiniGameChoiceCard(
                            title = game.title,
                            category = game.category.label,
                            description = game.description,
                            accent = accent,
                            titleColor = categoryTextColor(game.category),
                            enabled = !breakExpired && !starting,
                            onClick = { beginGame { duration -> onPlay(game.slug, duration) } },
                        ) {
                            ArtworkThumbnail(game.thumbnailRes, accent)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AttributionCard()
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
            if (breakExpired) {
                "This reward break has finished."
            } else {
                "All games work offline and are fun-only. They do not award stickers or tokens."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (breakExpired) Coral else Ink.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MenuSectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink)
    }
}

@Composable
private fun MiniGameChoiceCard(
    title: String,
    category: String,
    description: String,
    accent: Color,
    titleColor: Color = Ink,
    enabled: Boolean,
    onClick: () -> Unit,
    thumbnail: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(248.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $description"
                role = Role.Button
                if (!enabled) disabled()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {
                thumbnail()
            }
            Column(Modifier.padding(14.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 2,
                )
                Spacer(Modifier.height(3.dp))
                Text(category, style = MaterialTheme.typography.labelMedium, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink,
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun ArtworkThumbnail(@DrawableRes imageRes: Int, accent: Color) {
    Box(Modifier.fillMaxSize().background(accent.copy(alpha = 0.2f))) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.35f)))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
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
                "${BuiltInMiniGames.size + MiniGameCatalog.games.size} games are bundled locally. No account, analytics, or network access is used.",
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

private fun categoryTextColor(category: EmbeddedMiniGameCategory): Color = when (category) {
    EmbeddedMiniGameCategory.ARCADE -> com.maxinesworld.coredesignsystem.theme.OnCoral
    EmbeddedMiniGameCategory.PUZZLE -> com.maxinesworld.coredesignsystem.theme.OnLeafGreen
    EmbeddedMiniGameCategory.BOARD -> com.maxinesworld.coredesignsystem.theme.OnGold
    EmbeddedMiniGameCategory.WORD -> com.maxinesworld.coredesignsystem.theme.OnSkyBlue
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
