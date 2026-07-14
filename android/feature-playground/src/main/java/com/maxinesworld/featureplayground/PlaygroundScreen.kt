package com.maxinesworld.featureplayground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.gamekittenmatch.KittenMatchDefinition
import com.maxinesworld.gamefireflycatch.FireflyCatchDefinition
import com.maxinesworld.gamepawbeats.PawBeatsDefinition

private val Teal = Color(0xFF087F83)
private val Ink = Color(0xFF183B4A)
private val PaperTop = Color(0xFFFFF8E8)
private val Amber = Color(0xFFF0AF28)
private val Coral = Color(0xFFF27A61)
private val SkyBlue = Color(0xFF65B0D0)
private val LeafGreen = Color(0xFF5BA84D)

@Immutable
data class PlaygroundMiniGameCard(
    val gameId: String,
    val title: String,
    val purpose: String,
    val accent: Color,
)

val playgroundGames = listOf(
    PlaygroundMiniGameCard(KittenMatchDefinition.gameId, "Kitten Match", "Practice pattern recognition and memory skills", Coral),
    PlaygroundMiniGameCard(FireflyCatchDefinition.gameId, "Firefly Garden", "Explore counting and spatial awareness", SkyBlue),
    PlaygroundMiniGameCard(PawBeatsDefinition.gameId, "Paw Beats", "Develop rhythm and sequence skills", LeafGreen),
)

@Composable
fun PlaygroundScreen(
    childId: String,
    onBack: () -> Unit,
    onPlayGame: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("playground_screen"),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(PaperTop).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← Village", color = Teal) }
                Spacer(Modifier.weight(1f))
                Text("Playground", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp)) // balance
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "You've unlocked the playground! Take a break and play.",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
            )

            playgroundGames.forEach { game ->
                PlaygroundGameCard(
                    game = game,
                    onClick = { onPlayGame(game.gameId) },
                )
            }
        }
    }
}

@Composable
fun PlaygroundGameCard(
    game: PlaygroundMiniGameCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${game.title}: ${game.purpose}" }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(game.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text(game.purpose, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = game.accent.copy(alpha = 0.9f)),
                ) { Text("Play", color = Color.White) }
            }
        }
    }
}
