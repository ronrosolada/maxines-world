package com.maxinesworld.featurerewards

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.maxinesworld.coremodel.BadgeBiome
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coredesignsystem.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WildlifeFieldGuideScreen(
    childId: String,
    badgeAwarder: BadgeAwarder,
    onBack: () -> Unit
) {
    val allBadges by produceState<List<CollectibleBadge>>(emptyList()) {
        value = badgeAwarder.getCollectedBadges(childId)
    }
    val totalCollected = allBadges.count { it.isCollected }
    var selectedBadge by remember { mutableStateOf<CollectibleBadge?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wildlife Field Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { Text("$totalCollected / 50", Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, color = VillageTeal) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().background(Cream)) {
            BadgeBiome.entries.forEach { biome ->
                val key = biome.name.lowercase()
                val biomeBadges = allBadges.filter { it.biome == key }
                val biomeCollected = biomeBadges.count { it.isCollected }
                val accent = Color(biome.colorHex)

                item {
                    Card(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Park, biome.displayName, tint = accent, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(biome.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accent)
                                Text(biome.description, fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
                            }
                            Text("$biomeCollected/10", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                color = if (biomeCollected == 10) SunshineGold else accent)
                        }
                    }
                }

                // Badge grid using rows of 5
                biomeBadges.chunked(5).forEach { row ->
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { badge ->
                                BadgeSlot(badge, accent, Modifier.weight(1f).aspectRatio(1f)) { selectedBadge = badge }
                            }
                            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    selectedBadge?.let { badge ->
        BadgeDetailSheet(badge = badge, onDismiss = { selectedBadge = null })
    }
}

@Composable
private fun BadgeSlot(badge: CollectibleBadge, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (badge.isCollected) accent.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.04f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isCollected) 2.dp else 0.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (badge.isCollected) {
                Text(badge.emoji, fontSize = 28.sp)
                Icon(Icons.Default.Park, "Collected", tint = accent.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(14.dp))
            } else {
                Text(badge.emoji, fontSize = 28.sp, color = Color.Black.copy(alpha = 0.12f))
                Icon(Icons.Default.Lock, "Locked", tint = Color.Black.copy(alpha = 0.15f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(12.dp))
            }
        }
    }
}

@Composable
fun BadgeRevealScreen(
    badge: CollectibleBadge,
    challengeProgress: ChallengeProgress,
    onViewFieldGuide: () -> Unit,
    onReturnToVillage: () -> Unit
) {
    val biome = BadgeBiome.fromId(badge.biome)
    val accent = Color(biome.colorHex)
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200); step = 1
        kotlinx.coroutines.delay(800); step = 2
    }

    Scaffold(containerColor = Cream) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (step) {
                0 -> {
                    Text("Daily Challenge Complete!", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = VillageTeal, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    DailyChallengeProgressRow(challengeProgress)
                }
                1 -> {
                    val pulse by rememberInfiniteTransition().animateFloat(0.8f, 1.2f, animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse))
                    Box(Modifier.size(140.dp).scale(pulse).clip(CircleShape).background(accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text(badge.emoji, fontSize = 64.sp, color = Color.Black.copy(alpha = 0.2f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Discovering...", fontSize = 18.sp, color = accent)
                }
                2 -> {
                    Box(Modifier.size(140.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Text(badge.emoji, fontSize = 72.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = accent, textAlign = TextAlign.Center)
                    Text(badge.name, fontSize = 16.sp, color = Ink.copy(alpha = 0.6f))
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f))) {
                        Row(Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Lightbulb, "Fun Fact", tint = SunshineGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(badge.funFact, fontSize = 15.sp, lineHeight = 22.sp, color = Ink.copy(alpha = 0.85f))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onReturnToVillage, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal40), modifier = Modifier.height(52.dp)) { Text("Back to Village") }
                        OutlinedButton(onClick = onViewFieldGuide, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(52.dp)) { Text("View Field Guide") }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyChallengeProgressRow(progress: ChallengeProgress) {
    val subjects = listOf(
        Triple("English", progress.english, Icons.Default.MenuBook),
        Triple("Filipino", progress.filipino, Icons.Default.AutoStories),
        Triple("Math", progress.mathematics, Icons.Default.Calculate),
        Triple("Science", progress.science, Icons.Default.Science),
        Triple("History", progress.makabansa, Icons.Default.Flag)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        subjects.forEach { (name, done, icon) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(if (done) SuccessGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    Icon(icon, name, tint = if (done) SuccessGreen else Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                }
                Text(name, fontSize = 11.sp, color = if (done) SuccessGreen else Ink.copy(alpha = 0.3f))
            }
        }
    }
}
