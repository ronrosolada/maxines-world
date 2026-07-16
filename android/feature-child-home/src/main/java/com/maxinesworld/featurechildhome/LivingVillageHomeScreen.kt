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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.LocalReducedMotion
import kotlin.math.roundToInt

// ─── Design tokens from handoff v2.0.0 ───
private val DESIGN_W = 3048
private val DESIGN_H = 2032
private val STATUS_RAIL_WIDTH = 340.dp
private val STATUS_RAIL_HEIGHT = 76.dp
private val BOTTOM_NAV_HEIGHT = 64.dp
private val BOTTOM_NAV_MAX_WIDTH = 500.dp

// ─── Colors ───
private val Teal = Color(0xFF087F83)
private val DarkTeal = Color(0xFF075F63)
private val MutedTeal = Color(0xFF34545A)
private val Gold = Color(0xFFF5A623)
private val Cream = Color(0xFFFFF5DD)
private val WarmBg = Color(0xFFFFF5DD)
private val Ink = Color(0xFF173E38)
private val RailValueColor = Color(0xFFFFF8E8)
private val RailLabelColor = Color(0xFFD4C8A0)

// ─── Medallion drawable map ───
internal val subjectMedallionRes = mapOf(
    "english" to R.drawable.subject_english,
    "filipino" to R.drawable.subject_filipino,
    "mathematics" to R.drawable.subject_math,
    "science" to R.drawable.subject_science,
    "history" to R.drawable.subject_heritage,
    "gmrc" to R.drawable.subject_kindness,
)

// ─── Mira pose ───
private fun miraPose(state: LivingVillageHomeState): Int = when {
    state.completedQuests >= state.totalQuests && state.totalQuests > 0 -> R.drawable.mira_celebrate
    state.questSubjectId != null -> R.drawable.mira_point_right
    else -> R.drawable.mira_idle
}

// ═══════════════════════════════════════════════════════════
// Main entry point
// ═══════════════════════════════════════════════════════════

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
        modifier = modifier.fillMaxSize().background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (maxWidth >= 840.dp) {
            TabletLivingVillage(lvState, onDestinationClick, onMiraClick,
                onCafeClick, onPlaygroundClick, onDiscoveriesClick, onParentsClick)
        } else {
            CompactLivingVillage(lvState, onDestinationClick, onMiraClick,
                onCafeClick, onPlaygroundClick, onParentsClick)
        }
    }
}

// ═══════════════════════
// Tablet layout
// ═══════════════════════

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
    val density = LocalDensity.current

    val uiMetrics = remember(containerSize, density) {
        if (containerSize == IntSize.Zero) {
            null
        } else {
            with(density) {
                villageUiMetrics(
                    viewportWidthDp = containerSize.width.toDp().value,
                    viewportHeightDp = containerSize.height.toDp().value,
                )
            }
        }
    }

    Box(Modifier.fillMaxSize().onSizeChanged { containerSize = it }) {
            // Layer 1: Clean village background — use Fit to match contentFitTransform coords
            Image(
                painter = painterResource(R.drawable.village_home_six_landmarks_master),
                contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            // Layer 2: Paw trail — always when quest target is known.
            val resolvedQuestAnchor = state.questSubjectId?.let { subjectAnchors[it] }
            if (resolvedQuestAnchor != null) {
                PawTrailLayer(
                    questSubjectId = state.questSubjectId!!,
                    containerSize = containerSize,
                    reducedMotion = state.reducedMotion,
                )
            }

            // Layer 3: Mira + storybook quest group (before medallions so science anchor is not covered)
            MiraQuestGroup(state, onMira, uiMetrics,
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = BOTTOM_NAV_HEIGHT + 12.dp))

            uiMetrics?.let { metrics ->
                // Layer 4: Subject medallions (above Mira so all 6 are visible)
                MedallionLayer(
                    destinations = state.destinations,
                    activeSubjectId = state.questSubjectId,
                    containerSize = containerSize,
                    reducedMotion = state.reducedMotion,
                    metrics = metrics,
                    onClick = onSubject,
                )

                // Layer 5: Cat Café + Playground (non-subject world destinations)
                WorldDestinationLayer(
                    state = state,
                    containerSize = containerSize,
                    metrics = metrics,
                    onCafe = onCafe,
                    onPlayground = onPlayground,
                )
            }

            // Layer 6: Wooden status rail
            WoodenStatusRail(
                fishTreats = state.fishTreats, completed = state.completedQuests,
                total = state.totalQuests, playground = state.playground,
                onPlaygroundClick = onPlayground,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)
                    .width(STATUS_RAIL_WIDTH).height(STATUS_RAIL_HEIGHT),
            )

            // Layer 7: Minimal bottom nav
            MinimalHomeNav(onDiscoveries, onParents,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    .widthIn(max = BOTTOM_NAV_MAX_WIDTH).height(BOTTOM_NAV_HEIGHT))
        }
}

// ══════════════════════
// Compact phone layout
// ══════════════════════

@Composable
private fun CompactLivingVillage(
    state: LivingVillageHomeState,
    onSubject: (String) -> Unit,
    onMira: () -> Unit,
    onCafe: () -> Unit,
    onPlayground: () -> Unit,
    onParents: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(WarmBg).verticalScroll(rememberScrollState())) {
        CompactStatusStrip(state, onPlayground)
        var vpSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current
        val uiMetrics = remember(vpSize, density) {
            if (vpSize == IntSize.Zero) {
                null
            } else {
                with(density) {
                    villageUiMetrics(
                        viewportWidthDp = vpSize.width.toDp().value,
                        viewportHeightDp = vpSize.height.toDp().value,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).onSizeChanged { vpSize = it }) {
            Image(painter = painterResource(R.drawable.village_home_six_landmarks_master), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            uiMetrics?.let { metrics ->
                MedallionLayer(state.destinations, state.questSubjectId, vpSize, state.reducedMotion, metrics, onSubject)
                WorldDestinationLayer(state, vpSize, metrics, onCafe, onPlayground)
            }
        }
        CompactMiraSheet(state, onMira)
        MinimalHomeNav({}, onParents, Modifier.height(BOTTOM_NAV_HEIGHT).padding(bottom = 8.dp))
    }
}

// ════════════════════════
// Medallion Layer
// ════════════════════════

@Composable
private fun MedallionLayer(
    destinations: List<SubjectDestinationUi>,
    activeSubjectId: String?,
    containerSize: IntSize,
    reducedMotion: Boolean,
    metrics: VillageUiMetrics,
    onClick: (String) -> Unit,
) {
    if (containerSize == IntSize.Zero) return
    val transform = remember(containerSize) {
        contentFitTransform(containerSize, IntSize(DESIGN_W, DESIGN_H))
    }
    val density = LocalDensity.current

    destinations.forEach { destination ->
        val anchor = subjectAnchors[destination.id] ?: return@forEach
        val imageRes = subjectMedallionRes[destination.id] ?: return@forEach
        val approvedLabel = approvedDestinationNames[destination.id] ?: return@forEach
        val position = transform.map(anchor.x, anchor.y)
        val isActive = destination.id == activeSubjectId

        val iconSize = if (isActive) {
            metrics.selectedMedallionSize
        } else {
            metrics.medallionSize
        }

        val groupWidthPx = with(density) {
            metrics.lessonLabelWidth.toPx()
        }

        val iconSizePx = with(density) {
            iconSize.toPx()
        }

        SubjectMedallion(
            imageRes = imageRes,
            label = approvedLabel,
            progressText = destination.progressText,
            isActive = isActive,
            enabled = destination.enabled,
            reducedMotion = reducedMotion,
            metrics = metrics,
            onClick = { onClick(destination.id) },
            modifier = Modifier.offset {
                IntOffset(
                    x = (position.x - groupWidthPx / 2f).roundToInt(),
                    y = (position.y - iconSizePx / 2f).roundToInt(),
                )
            }
                .testTag("living_village_subject_${destination.id}"),
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
    metrics: VillageUiMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive && !reducedMotion) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "lessonMedallionScale",
    )

    val glowAlpha = if (isActive && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "lessonMedallionGlow")
        val value by transition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "lessonMedallionGlowAlpha",
        )
        value
    } else {
        1f
    }

    val iconSize = if (isActive) {
        metrics.selectedMedallionSize
    } else {
        metrics.medallionSize
    }

    val accessibilityLabel = buildString {
        append(label)
        if (progressText.isNotBlank()) {
            append(", ")
            append(progressText)
        }
        if (!enabled) {
            append(", locked")
        }
    }

    Column(
        modifier = modifier
            .width(metrics.lessonLabelWidth)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
                role = Role.Button
                if (!enabled) disabled()
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = glowAlpha
                }
                .testTag("living_village_subject_icon_${label.lowercase().replace(" ", "_")}"),
        )

        Spacer(Modifier.height(2.dp))

        LessonDestinationPlaque(
            label = label,
            metrics = metrics,
        )
    }
}

@Composable
private fun LessonDestinationPlaque(
    label: String,
    metrics: VillageUiMetrics,
) {
    val usesTwoLines = label in setOf(
        "Bahay ng Kuwento",
        "Number Market",
        "Discovery Lab",
        "Heritage Harbor",
        "Kindness Corner",
    )

    val height = if (usesTwoLines) {
        metrics.twoLineLabelHeight
    } else {
        metrics.oneLineLabelHeight
    }

    Box(
        modifier = Modifier
            .width(metrics.lessonLabelWidth)
            .height(height)
            .testTag("living_village_subject_label_${label.lowercase().replace(" ", "_")}"),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.mw_subject_plaque),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.lessonLabelFontSp.sp,
            lineHeight = metrics.lessonLabelLineHeightSp.sp,
            textAlign = TextAlign.Center,
            maxLines = if (usesTwoLines) 2 else 1,
            softWrap = usesTwoLines,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
        )
    }
}

// ═══════════════════
// Wooden Status Rail
// ═══════════════════

@Composable
private fun WoodenStatusRail(
    fishTreats: Long, completed: Int, total: Int,
    playground: PlaygroundUi, onPlaygroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Image(painter = painterResource(R.drawable.status_rail), contentDescription = null,
            contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$fishTreats", color = RailValueColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Fish Treats", color = RailLabelColor, fontSize = 10.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$completed/$total", color = RailValueColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Today", color = RailLabelColor, fontSize = 10.sp)
            }
            val (s, d) = when (playground) {
                is PlaygroundUi.Open -> "Open" to "Playground"
                is PlaygroundUi.Locked -> "${playground.completed}/${playground.total}" to "${playground.remaining} left"
                is PlaygroundUi.Unavailable -> "—" to "Playground"
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(role = Role.Button) { onPlaygroundClick() }
                    .semantics(mergeDescendants = true) { contentDescription = "Playground $s"; role = Role.Button },
            ) {
                Text(s, color = RailValueColor, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(d, color = RailLabelColor, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════
// Mira + Quest — one group
// ═══════════════════════════

@Composable
private fun MiraQuestGroup(
    state: LivingVillageHomeState, onContinue: () -> Unit,
    metrics: VillageUiMetrics?,
    modifier: Modifier = Modifier,
) {
    val bobOffset = if (state.reducedMotion) 0f else {
        val bob by rememberInfiniteTransition().animateFloat(
            initialValue = 0f, targetValue = 4f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "miraBob",
        )
        bob
    }
    val poseRes = miraPose(state)

    val miraHeight = metrics?.miraHeight ?: 224.dp
    val questBookWidth = metrics?.questBookWidth ?: 360.dp

    Row(
        modifier = modifier.clickable(role = Role.Button) { onContinue() }
            .semantics(mergeDescendants = true) {
                contentDescription = "Mira: ${state.questTitle}. ${state.questPrompt}"; role = Role.Button
            },
        verticalAlignment = Alignment.Bottom,
    ) {
        Image(painter = painterResource(poseRes), contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.height(miraHeight).graphicsLayer { translationY = bobOffset })

        Spacer(Modifier.width(2.dp))

        // Storybook quest panel with corrected insets
        Box(Modifier.width(questBookWidth).height(miraHeight * 0.52f)) {
            Image(painter = painterResource(R.drawable.storybook_quest_panel), contentDescription = null,
                contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()
                .padding(start = 32.dp, top = 24.dp, end = 32.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.questTitle.ifEmpty { "Your Quest" }, color = DarkTeal,
                    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                Spacer(Modifier.height(4.dp))
                Text(state.questPrompt.ifEmpty { "Tap to continue" }, color = MutedTeal,
                    fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
                if (state.totalQuests > 0) {
                    Spacer(Modifier.height(12.dp))
                    val prog = state.completedQuests.toFloat() / state.totalQuests
                    Box(Modifier.fillMaxWidth(0.65f).height(4.dp)
                        .background(Color(0x33075F63), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(prog.coerceIn(0f, 1f)).fillMaxHeight()
                            .background(Gold, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
    }
}

// ═══════════════════
// Paw Trail (deterministic)
// ═══════════════════

@Composable
private fun PawTrailLayer(
    questSubjectId: String,
    containerSize: IntSize,
    reducedMotion: Boolean,
) {
    if (containerSize == IntSize.Zero) return
    val targetAnchor = subjectAnchors[questSubjectId] ?: return
    val transform = remember(containerSize) { contentFitTransform(containerSize, IntSize(DESIGN_W, DESIGN_H)) }
    val target = transform.map(targetAnchor.x, targetAnchor.y)
    val miraPos = transform.map(200f, 1800f)

    // Exactly six paws; last print pulses unless reduced motion.
    Box(Modifier.testTag("living_village_paw_trail")) {
        repeat(6) { i ->
            val t = i.toFloat() / 5f
            val x = miraPos.x + (target.x - miraPos.x) * t
            val y = miraPos.y + (target.y - miraPos.y) * t
            val isLast = i == 5
            val alpha = if (isLast && !reducedMotion) {
                val pulse by rememberInfiniteTransition().animateFloat(
                    initialValue = 0.82f, targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse",
                )
                pulse
            } else {
                0.45f + t * 0.25f
            }

            Image(
                painter = painterResource(R.drawable.paw_trail),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset((x - 22).toInt(), (y - 22).toInt()) }
                    .size(44.dp)
                    .alpha(alpha)
                    .testTag("living_village_paw_${i + 1}"),
            )
        }
    }
}

// ═══════════════════════
// World Destinations
// ═══════════════════════

@Composable
private fun WorldDestinationLayer(
    state: LivingVillageHomeState, containerSize: IntSize,
    metrics: VillageUiMetrics,
    onCafe: () -> Unit, onPlayground: () -> Unit,
) {
    if (containerSize == IntSize.Zero) return
    val transform = remember(containerSize) { contentFitTransform(containerSize, IntSize(DESIGN_W, DESIGN_H)) }
    val density = LocalDensity.current

    // Cat Café — separate anchor
    val cafePos = transform.map(catCafeAnchor.x, catCafeAnchor.y)
    val cafeMarkerSizePx = with(density) { metrics.worldMarkerSize.toPx() }
    val cafeLabelWidthPx = with(density) { metrics.worldLabelWidth.toPx() }
    Column(
        Modifier.offset {
            IntOffset(
                x = (cafePos.x - cafeMarkerSizePx / 2f).roundToInt(),
                y = (cafePos.y - cafeMarkerSizePx / 2f).roundToInt(),
            )
        }
            .semantics(mergeDescendants = true) { contentDescription = "Cat Café"; role = Role.Button }
            .clickable(role = Role.Button) { onCafe() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(R.drawable.cat_cafe_marker), contentDescription = "Cat Café",
            contentScale = ContentScale.Fit, modifier = Modifier.size(metrics.worldMarkerSize))
        Spacer(Modifier.height(4.dp))
        Text("Cat Café", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(metrics.worldLabelWidth)
                .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp))
    }

    // Playground — separate anchor
    val pgPos = transform.map(playgroundAnchor.x, playgroundAnchor.y)
    val pgMarkerSizePx = with(density) { metrics.worldMarkerSize.toPx() }
    val (pgRes, pgDesc) = when (state.playground) {
        is PlaygroundUi.Open -> R.drawable.playground_open to "Playground — Open"
        is PlaygroundUi.Locked -> R.drawable.playground_locked to "Playground — ${state.playground.remaining} to go"
        is PlaygroundUi.Unavailable -> R.drawable.playground_locked to "Playground"
    }
    Column(
        Modifier.offset {
            IntOffset(
                x = (pgPos.x - pgMarkerSizePx / 2f).roundToInt(),
                y = (pgPos.y - pgMarkerSizePx / 2f).roundToInt(),
            )
        }
            .semantics(mergeDescendants = true) { contentDescription = pgDesc; role = Role.Button }
            .clickable(role = Role.Button) { onPlayground() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painter = painterResource(pgRes), contentDescription = pgDesc, contentScale = ContentScale.Fit, modifier = Modifier.size(metrics.worldMarkerSize))
        Spacer(Modifier.height(4.dp))
        Text("Playground", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(metrics.worldLabelWidth)
                .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

// ═══════════════════
// Bottom Nav
// ═══════════════════

@Composable
private fun MinimalHomeNav(onDiscoveries: () -> Unit, onParents: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.background(Color(0xDD075F63), RoundedCornerShape(32.dp)).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        NavLabel("Home", isActive = true) {}
        NavLabel("Discoveries", onClick = onDiscoveries)
        NavLabel("Parents", onClick = onParents)
    }
}

@Composable
private fun RowScope.NavLabel(label: String, isActive: Boolean = false, onClick: () -> Unit) {
    Text(label, color = if (isActive) Gold else Color.White,
        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 14.sp,
        modifier = Modifier.weight(1f).clickable(role = Role.Button) { onClick() }
            .semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button }
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Center)
}

// ════════════════════════
// Compact phone variants
// ════════════════════════

@Composable
private fun CompactStatusStrip(state: LivingVillageHomeState, onPlayground: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(DarkTeal).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Hi, ${state.childName}!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("\uD83D\uDC1F ${state.fishTreats}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("\uD83D\uDCCB ${state.completedQuests}/${state.totalQuests}", color = Color.White, fontSize = 14.sp)
            Text(if (state.playground is PlaygroundUi.Open) "\uD83C\uDFAE" else "\uD83D\uDD12",
                color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { onPlayground() })
        }
    }
}

@Composable
private fun CompactMiraSheet(state: LivingVillageHomeState, onMira: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Cream).clickable(role = Role.Button) { onMira() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painter = painterResource(R.drawable.mira_idle), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(state.questTitle, color = DarkTeal, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(state.questPrompt, color = MutedTeal, fontSize = 12.sp)
        }
    }
}
