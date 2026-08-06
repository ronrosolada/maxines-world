package com.maxinesworld.featurerewards

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    initialBadgeId: String? = null,
    onBack: () -> Unit
) {
    val allBadges by produceState<List<CollectibleBadge>>(emptyList()) {
        value = badgeAwarder.getCollectedBadges(childId)
    }
    val totalCollected = allBadges.count { it.isCollected }
    var selectedBadge by remember { mutableStateOf<CollectibleBadge?>(null) }

    // #32: order biomes so ones with collected stickers come first — the
    // milestone sticker was unreachable at the bottom of five empty sections.
    // Stable sort: collected-first by count desc, then natural biome order.
    val biomeOrder = remember(allBadges) {
        val collectedFirst = BadgeBiome.entries.sortedByDescending { biome ->
            allBadges.count { it.biome == biome.name.lowercase() && it.isCollected }
        }
        // Keep uncollected biomes in their canonical order after any with stickers.
        collectedFirst
    }

    // Scroll to the newly earned sticker when arriving via the reveal screen.
    val listState = rememberLazyListState()
    LaunchedEffect(allBadges, initialBadgeId) {
        if (initialBadgeId != null) {
            selectedBadge = allBadges.firstOrNull { it.id == initialBadgeId && it.isCollected }
            val targetBiome = allBadges.firstOrNull { it.id == initialBadgeId }?.biome
            if (targetBiome != null) {
                var index = 0
                for (biome in biomeOrder) {
                    if (biome.name.lowercase() == targetBiome) break
                    val count = allBadges.count { it.biome == biome.name.lowercase() }
                    index += 1 + ((count + 4) / 5) + 1 // header + grid rows + spacer
                }
                listState.animateScrollToItem(index)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wildlife Field Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { Text("$totalCollected / ${allBadges.size}", Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, color = VillageTeal) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().background(Cream), state = listState) {
            biomeOrder.forEach { biome ->
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
                            Text("$biomeCollected/${biomeBadges.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                color = if (biomeCollected == biomeBadges.size) SunshineGold else accent)
                        }
                    }
                }

                // Badge grid using rows of 5
                biomeBadges.chunked(5).forEach { row ->
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { badge ->
                                PokemonStyleBadgeSlot(badge, accent, Modifier.weight(1f).aspectRatio(1f)) {
                                    if (badge.isCollected) selectedBadge = badge
                                }
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

// ─── Pokémon-style Badge Slot — silhouette until earned ───

@Composable
private fun PokemonStyleBadgeSlot(
    badge: CollectibleBadge,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isCollected) accent.copy(alpha = 0.12f)
                else Color.Black.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isCollected) 3.dp else 0.dp)
    ) {
        Box(modifier, contentAlignment = Alignment.Center) {
            if (badge.isCollected) {
                // ✨ Earned — full color, scalloped frame glow
                Canvas(Modifier.fillMaxSize(0.75f)) {
                    drawCircle(accent.copy(alpha = 0.2f), radius = size.minDimension / 2)
                    drawCircle(accent.copy(alpha = 0.08f), radius = size.minDimension / 2.4f)
                }
                Text(badge.emoji, fontSize = 30.sp)
                // Leaf checkmark
                Icon(Icons.Default.Park, "Collected", tint = accent,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(14.dp))
            } else {
                // 🔒 Locked — pure silhouette, no colors, no emoji
                Canvas(Modifier.fillMaxSize(0.7f)) {
                    val r = size.minDimension / 2
                    // Scalloped token outer ring
                    val scallopCount = 8
                    val path = Path()
                    for (i in 0 until scallopCount) {
                        val angle = (i.toFloat() / scallopCount) * 360f
                        val rad = Math.toRadians(angle.toDouble())
                        val outerRadius = r * 0.95f
                        val innerRadius = r * 0.78f
                        val cx = center.x + (outerRadius * Math.cos(rad)).toFloat()
                        val cy = center.y + (outerRadius * Math.sin(rad)).toFloat()
                        if (i == 0) path.moveTo(cx, cy) else path.lineTo(cx, cy)
                        val midAngle = angle + (360f / scallopCount / 2f)
                        val midRad = Math.toRadians(midAngle.toDouble())
                        val mx = center.x + (innerRadius * Math.cos(midRad)).toFloat()
                        val my = center.y + (innerRadius * Math.sin(midRad)).toFloat()
                        path.lineTo(mx, my)
                    }
                    path.close()
                    drawPath(path, Color.Black.copy(alpha = 0.18f))
                    drawPath(path, Color.Black.copy(alpha = 0.3f), style = Stroke(width = 2f))
                    // Question mark / mystery paw
                    drawCircle(Color.Black.copy(alpha = 0.15f), radius = r * 0.25f, center = center)
                }
                // Small lock indicator
                Icon(Icons.Default.Lock, "Undiscovered", tint = Color.Black.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(10.dp))
            }
        }
    }
}

// ─── Badge Reveal — Pokémon-style discovery ───

@Composable
fun BadgeRevealScreen(
    badge: CollectibleBadge,
    challengeProgress: ChallengeProgress,
    onViewFieldGuide: () -> Unit,
    onReturnToVillage: () -> Unit,
    onPlayGames: (() -> Unit)? = null,
) {
    val biome = BadgeBiome.fromId(badge.biome)
    val accent = Color(biome.colorHex)
    val context = LocalContext.current
    val reduceMotion = remember {
        !badgeRevealAnimationEnabled(
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        )
    }
    val isMilestone = badge.biome == BadgeAwarder.MILESTONE_BIOME
    var step by remember { mutableIntStateOf(if (reduceMotion) 2 else 0) }

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            kotlinx.coroutines.delay(800)
            step = 1
            kotlinx.coroutines.delay(1000)
            step = 2
        }
    }

    // ── Celebration effects (confetti, pop-in, orbiting sparkles) ──
    val confettiColors = if (!reduceMotion) {
        listOf(Coral, SunshineGold, SkyBlue, StoryPurple, LeafGreen, VillageTeal)
    } else {
        emptyList()
    }
    val particles = remember {
        List(if (reduceMotion) 0 else 40) {
            Offset((Math.random() * 1000).toFloat(), (-Math.random() * 800).toFloat())
        }
    }
    val confettiAnim = if (reduceMotion) {
        0f
    } else {
        rememberInfiniteTransition(label = "confetti").animateFloat(
            0f, 800f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), "fall"
        ).value
    }
    val popScale by animateFloatAsState(
        targetValue = if (step == 2) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "stickerPop",
    )
    val sparkleSpin = if (reduceMotion) {
        0f
    } else {
        rememberInfiniteTransition(label = "sparkle").animateFloat(
            0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), "spin"
        ).value
    }

    Scaffold(containerColor = Cream) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // Confetti rain behind everything during the reveal
            if (!reduceMotion && step == 2) {
                Canvas(Modifier.fillMaxSize()) {
                    particles.forEachIndexed { i, pos ->
                        val y = (pos.y + confettiAnim + (i * 37)) % size.height
                        val x = (pos.x + kotlin.math.sin(confettiAnim / 200 + i) * 50) % size.width
                        drawCircle(
                            confettiColors[i % confettiColors.size].copy(alpha = 0.6f),
                            radius = (4 + (i % 5)).toFloat(),
                            center = Offset(x.toFloat(), y)
                        )
                    }
                }
            }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (step) {
                    0 -> {
                        if (isMilestone) {
                            Text("Your First Sticker!", fontWeight = FontWeight.Bold, fontSize = 26.sp,
                                color = accent, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Text("You finished your very first lesson!", fontSize = 18.sp,
                                color = Ink.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("Milo is cheering for you! 🐱🎉", fontSize = 16.sp,
                                color = VillageTeal, textAlign = TextAlign.Center)
                        } else {
                            Text("Wildlife Expedition Complete!", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = VillageTeal, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            WildlifeExpeditionProgressRow(challengeProgress)
                        }
                    }
                    1 -> {
                        val pulse by rememberInfiniteTransition().animateFloat(0.85f, 1.15f,
                            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse))
                        Box(Modifier.size(150.dp).scale(pulse)) {
                            Canvas(Modifier.fillMaxSize()) {
                                val r = size.minDimension / 2
                                drawCircle(Color(0xFF1A1A2E).copy(alpha = 0.85f), radius = r, center = center)
                                drawCircle(Color.Black.copy(alpha = 0.6f), radius = r * 0.85f, style = Stroke(width = 3f))
                                drawCircle(accent.copy(alpha = 0.15f), radius = r * 0.6f, center = center)
                                drawCircle(accent.copy(alpha = 0.3f), radius = r * 0.15f, center = center)
                            }
                            Text("?", fontSize = 48.sp, color = accent.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Discovering...", fontSize = 18.sp, color = accent, fontWeight = FontWeight.Medium)
                    }
                    2 -> {
                        // Orbiting sparkles around the sticker
                        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                            val density = LocalDensity.current
                            val orbitRadius = with(density) { 100.dp.toPx() }
                            repeat(6) { i ->
                                val rad = Math.toRadians((sparkleSpin + i * 60f).toDouble())
                                val dx = with(density) { (orbitRadius * kotlin.math.cos(rad)).toFloat().toDp() }
                                val dy = with(density) { (orbitRadius * kotlin.math.sin(rad)).toFloat().toDp() }
                                Text("✨", fontSize = 18.sp,
                                    modifier = Modifier.offset(x = dx, y = dy))
                            }
                            // Bouncy pop-in sticker token
                            Box(Modifier.size(150.dp)
                                .graphicsLayer { scaleX = popScale; scaleY = popScale }
                                .clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center) {
                                Text(badge.emoji, fontSize = 72.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = accent, textAlign = TextAlign.Center)
                        Text(badge.name, fontSize = 16.sp, color = Ink.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f))) {
                            Row(Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Lightbulb, "Fun Fact", tint = SunshineGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(badge.funFact, fontSize = 15.sp, lineHeight = 22.sp, color = Ink.copy(alpha = 0.85f))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onReturnToVillage,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Teal40),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Text("Back to Playroom")
                            }
                            if (onPlayGames != null) {
                                Button(
                                    onClick = onPlayGames,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SunshineGold,
                                        contentColor = OnGold,
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                ) {
                                    Icon(Icons.Default.SportsEsports, "Games", modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Play Game")
                                }
                            }
                            OutlinedButton(
                                onClick = onViewFieldGuide,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Text("View Field Guide")
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun badgeRevealAnimationEnabled(animatorDurationScale: Float): Boolean =
    animatorDurationScale > 0f

@Composable
fun WildlifeExpeditionProgressRow(progress: ChallengeProgress) {
    val subjects = listOf(
        Triple("English", progress.english, Icons.Default.MenuBook),
        Triple("Filipino", progress.filipino, Icons.Default.AutoStories),
        Triple("Math", progress.mathematics, Icons.Default.Calculate),
        Triple("Science", progress.science, Icons.Default.Science),
        Triple("AP", progress.makabansa, Icons.Default.Flag),
        Triple("GMRC", progress.gmrc, Icons.Default.Favorite),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${progress.completedCount}/3 adventures · ${progress.subjectCount}/2 learning areas",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = VillageTeal,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            subjects.forEach { (name, done, icon) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(40.dp).clip(CircleShape)
                        .background(if (done) SuccessGreen.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center) {
                        Icon(icon, name, tint = if (done) SuccessGreen else Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
                    }
                    Text(name, fontSize = 10.sp, color = if (done) SuccessGreen else Ink.copy(alpha = 0.3f))
                }
            }
        }
    }
}

/** Compatibility alias for existing callers and tests. */
@Composable
fun DailyChallengeProgressRow(progress: ChallengeProgress) = WildlifeExpeditionProgressRow(progress)
