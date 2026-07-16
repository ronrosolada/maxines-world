package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.LocalReducedMotion

// ─── Color tokens ───
private val Teal = Color(0xFF087F83)
private val DarkTeal = Color(0xFF075F63)
private val MutedTeal = Color(0xFF34545A)
private val Gold = Color(0xFFF5A623)
private val Parchment = Color(0xFFF5F0E8)
private val WarmBg = Color(0xFFFFF5DD)
private val Ink = Color(0xFF173E38)

// ─── Subject-to-medallion drawable mapping ───
internal val subjectMedallionRes = mapOf(
    "english" to R.drawable.subject_english,
    "filipino" to R.drawable.subject_filipino,
    "mathematics" to R.drawable.subject_math,
    "science" to R.drawable.subject_science,
    "history" to R.drawable.subject_heritage,
    "gmrc" to R.drawable.subject_kindness,
)

// ─── Main entry point ───

@Composable
fun LivingVillageHomeScreen(
    state: VillageHomeState,
    onDestinationClick: (String) -> Unit,
    onMiraClick: () -> Unit,
    onDiscoveriesClick: () -> Unit,
    onCafeClick: () -> Unit,
    onPlaygroundClick: () -> Unit = {},
    onDismissPlaygroundUnlockCelebration: () -> Unit = {},
    onParentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val lvState = remember(state, reducedMotion) { state.toLivingVillage(reducedMotion) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val tablet = maxWidth >= 840.dp
        if (tablet) {
            TabletLivingVillage(lvState, onDestinationClick, onMiraClick, onCafeClick, onPlaygroundClick, onDiscoveriesClick, onParentsClick)
        } else {
            CompactLivingVillage(lvState, onDestinationClick, onMiraClick, onCafeClick, onPlaygroundClick, onParentsClick)
        }
    }
}

// ─── Tablet layout ───

@Composable
private fun TabletLivingVillage(
    state: LivingVillageHomeState,
    onSubject: (String) -> Unit,
    onMira: () -> Unit,
    onCafe: () -> Unit,
    onPlayground: () -> Unit,
    onDiscoveries: () -> Unit,
    onParents: () -> Unit,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
    ) {
        // Background village scene
        Image(
            painter = painterResource(id = R.drawable.mw_village_scene_v17),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        // Paw trail (decorative, below overlays)
        if (state.questSubjectId != null && !state.reducedMotion) {
            PawTrailOverlay(state, containerSize)
        }

        // Subject medallions on buildings
        LivingVillageDestinationLayer(
            destinations = state.destinations,
            activeSubjectId = state.questSubjectId,
            containerSize = containerSize,
            reducedMotion = state.reducedMotion,
            onClick = onSubject,
        )

        // Status rail — top-right
        LivingStatusRail(
            fishTreats = state.fishTreats,
            completed = state.completedQuests,
            total = state.totalQuests,
            playground = state.playground,
            onPlaygroundClick = onPlayground,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .widthIn(max = 180.dp),
        )

        // Mira + quest book — bottom-left
        MiraQuestOverlay(
            state = state,
            onContinue = onMira,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 72.dp),
        )

        // Cat Café world marker
        CatCafeMarker(
            containerSize = containerSize,
            onClick = onCafe,
        )

        // Playground gate marker
        PlaygroundMarker(
            playground = state.playground,
            containerSize = containerSize,
            onClick = onPlayground,
        )

        // Minimal bottom nav: Home, Discoveries, Parents
        MinimalHomeNav(
            onDiscoveries = onDiscoveries,
            onParents = onParents,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─── Compact (phone) layout ───

@Composable
private fun CompactLivingVillage(
    state: LivingVillageHomeState,
    onSubject: (String) -> Unit,
    onMira: () -> Unit,
    onCafe: () -> Unit,
    onPlayground: () -> Unit,
    onParents: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(WarmBg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Compact profile + status strip
        CompactStatusStrip(state, onPlayground)

        // Village viewport with medallions
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .onSizeChanged { containerSize = it },
        ) {
            Image(
                painter = painterResource(id = R.drawable.mw_village_scene_v17),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            LivingVillageDestinationLayer(
                destinations = state.destinations,
                activeSubjectId = state.questSubjectId,
                containerSize = containerSize,
                reducedMotion = state.reducedMotion,
                onClick = onSubject,
            )
            CatCafeMarker(containerSize = containerSize, onClick = onCafe)
            PlaygroundMarker(playground = state.playground, containerSize = containerSize, onClick = onPlayground)
        }

        // Mira quest sheet below viewport
        CompactMiraSheet(state, onMira)

        // Bottom nav
        MinimalHomeNav(
            onDiscoveries = {},
            onParents = onParents,
        )
    }
}

// ─── Subject medallion layer ───

@Composable
private fun LivingVillageDestinationLayer(
    destinations: List<SubjectDestinationUi>,
    activeSubjectId: String?,
    containerSize: IntSize,
    reducedMotion: Boolean,
    onClick: (String) -> Unit,
) {
    if (containerSize == IntSize.Zero) return
    val transform = remember(containerSize) { contentFitTransform(containerSize) }

    destinations.forEach { dest ->
        val anchor = subjectAnchors[dest.id] ?: return@forEach
        val pos = transform.map(anchor.x, anchor.y)
        val isActive = dest.id == activeSubjectId
        val medallionRes = subjectMedallionRes[dest.id] ?: R.drawable.subject_blank

        SubjectMedallion(
            imageRes = medallionRes,
            label = dest.label,
            progressText = dest.progressText,
            isActive = isActive,
            enabled = dest.enabled,
            reducedMotion = reducedMotion,
            onClick = { onClick(dest.id) },
            modifier = Modifier
                .offset { IntOffset(pos.x.toInt(), pos.y.toInt()) }
                .size(72.dp),
        )
    }
}

@Composable
private fun SubjectMedallion(
    imageRes: Int,
    label: String,
    progressText: String,
    isActive: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pressing by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressing) 0.96f else if (isActive) 1.06f else 1f,
        animationSpec = tween(120),
        label = "medallionScale",
    )
    val lanternAlpha = if (!reducedMotion && isActive) {
        val glow by rememberInfiniteTransition(label = "lantern").animateFloat(
            initialValue = 0.82f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
            label = "glow",
        )
        glow
    } else 1f

    val desc = "$label, $progressText${if (!enabled) ", opening soon" else ""}"
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = desc
                role = Role.Button
                if (!enabled) disabled()
            }
            .scale(pressScale)
            .alpha(lanternAlpha)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        // Label overlaid at bottom of medallion
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .background(Color(0x99000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

// ─── Status rail ───

@Composable
private fun LivingStatusRail(
    fishTreats: Long,
    completed: Int,
    total: Int,
    playground: PlaygroundUi,
    onPlaygroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Use the existing StatusRail composable, adapted
    val playgroundStatus = when (playground) {
        is PlaygroundUi.Open -> "Open"
        is PlaygroundUi.Locked -> "${playground.completed}/${playground.total}"
        is PlaygroundUi.Unavailable -> "—"
    }
    val playgroundLabel = when (playground) {
        is PlaygroundUi.Open -> "Playground"
        is PlaygroundUi.Locked -> "${playground.remaining} to go"
        is PlaygroundUi.Unavailable -> "Playground"
    }
    StatusRail(
        fishTreats = fishTreats.toInt(),
        completed = completed,
        assigned = total,
        playgroundStatus = playgroundStatus,
        playgroundLabel = playgroundLabel,
        onPlaygroundClick = onPlaygroundClick,
        modifier = modifier,
    )
}

// ─── Mira + quest book ───

@Composable
private fun MiraQuestOverlay(
    state: LivingVillageHomeState,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val miraBob by remember { mutableStateOf(0f) }
    if (!state.reducedMotion) {
        val bob by rememberInfiniteTransition(label = "miraBob").animateFloat(
            initialValue = 0f, targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "bob",
        )
    }

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Mira: ${state.questTitle}. ${state.questPrompt}"
                role = Role.Button
            }
            .clickable(role = Role.Button) { onContinue() },
        verticalAlignment = Alignment.Bottom,
    ) {
        // Mira character
        Image(
            painter = painterResource(
                if (state.questSubjectId != null) R.drawable.mira_point_right else R.drawable.mira_idle
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(176.dp)
                .graphicsLayer { translationY = if (state.reducedMotion) 0f else miraBob },
        )

        // Quest book panel
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(260.dp)
                .height(100.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.storybook_quest_panel),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.questTitle.ifEmpty { "Your Quest" },
                    color = DarkTeal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.questPrompt.ifEmpty { "Tap to continue" },
                    color = MutedTeal,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Paw trail ───

@Composable
private fun PawTrailOverlay(state: LivingVillageHomeState, containerSize: IntSize) {
    if (containerSize == IntSize.Zero) return
    val transform = remember(containerSize) { contentFitTransform(containerSize) }
    val targetAnchor = state.questSubjectId?.let { subjectAnchors[it] } ?: return
    val target = transform.map(targetAnchor.x, targetAnchor.y)

    // Simple paw prints along the path from Mira position toward target
    val miraPos = transform.map(200f, 1800f)
    val steps = 5
    for (i in 0 until steps) {
        val t = i.toFloat() / steps
        val x = miraPos.x + (target.x - miraPos.x) * t
        val y = miraPos.y + (target.y - miraPos.y) * t
        val pawAlpha = if (i == steps - 1) {
            // Last paw pulses
            val pulse by rememberInfiniteTransition(label = "pawPulse").animateFloat(
                initialValue = 0.82f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "pulse",
            )
            pulse
        } else 0.5f

        Image(
            painter = painterResource(R.drawable.paw_trail),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset { IntOffset(x.toInt() - 16, y.toInt() - 16) }
                .size(32.dp)
                .alpha(pawAlpha),
        )
    }
}

// ─── World destinations ───

@Composable
private fun CatCafeMarker(containerSize: IntSize, onClick: () -> Unit) {
    if (containerSize == IntSize.Zero) return
    val transform = remember(containerSize) { contentFitTransform(containerSize) }
    val anchor = subjectAnchors["cat-cafe"] ?: return
    val pos = transform.map(anchor.x, anchor.y)

    Image(
        painter = painterResource(R.drawable.cat_cafe_marker),
        contentDescription = "Cat Café",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset { IntOffset(pos.x.toInt() - 40, pos.y.toInt() - 40) }
            .size(80.dp)
            .semantics { contentDescription = "Cat Café"; role = Role.Button }
            .clickable(role = Role.Button) { onClick() },
    )
}

@Composable
private fun PlaygroundMarker(playground: PlaygroundUi, containerSize: IntSize, onClick: () -> Unit) {
    if (containerSize == IntSize.Zero) return
    val anchor = Offset(400f, 1850f) // Playground position in design coords
    val transform = remember(containerSize) { contentFitTransform(containerSize) }
    val pos = transform.map(anchor.x, anchor.y)

    val (imageRes, label) = when (playground) {
        is PlaygroundUi.Open -> R.drawable.playground_open to "Playground — Open"
        is PlaygroundUi.Locked -> R.drawable.playground_locked to "Playground — ${playground.remaining} to go"
        is PlaygroundUi.Unavailable -> R.drawable.playground_locked to "Playground"
    }

    Image(
        painter = painterResource(imageRes),
        contentDescription = label,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset { IntOffset(pos.x.toInt() - 36, pos.y.toInt() - 36) }
            .size(72.dp)
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(role = Role.Button) { onClick() },
    )
}

// ─── Minimal bottom nav ───

@Composable
private fun MinimalHomeNav(
    onDiscoveries: () -> Unit,
    onParents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.6f)
            .padding(bottom = 8.dp)
            .height(56.dp)
            .background(Color(0xCC075F63), RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("Home", isActive = true, onClick = {})
        NavItem("Discoveries", onClick = onDiscoveries)
        NavItem("Parents", onClick = onParents)
    }
}

@Composable
private fun RowScope.NavItem(label: String, isActive: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clickable(role = Role.Button) { onClick() }
            .semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = if (isActive) Gold else Color.White,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 12.sp,
        )
    }
}

// ─── Compact (phone) widgets ───

@Composable
private fun CompactStatusStrip(state: LivingVillageHomeState, onPlayground: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(DarkTeal)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Hi, ${state.childName}!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🐟 ${state.fishTreats}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("📋 ${state.completedQuests}/${state.totalQuests}", color = Color.White, fontSize = 14.sp)
            val pgText = when (state.playground) {
                is PlaygroundUi.Open -> "🎮 Open"
                is PlaygroundUi.Locked -> "🔒"
                is PlaygroundUi.Unavailable -> "—"
            }
            Text(
                pgText, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.clickable { onPlayground() },
            )
        }
    }
}

@Composable
private fun CompactMiraSheet(state: LivingVillageHomeState, onMira: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Parchment)
            .clickable(role = Role.Button) { onMira() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.mira_idle),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(state.questTitle, color = DarkTeal, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(state.questPrompt, color = MutedTeal, fontSize = 12.sp)
        }
    }
}
