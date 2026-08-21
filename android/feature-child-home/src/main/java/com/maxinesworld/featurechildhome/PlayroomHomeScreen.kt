package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardHeader
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardSurface
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.SanctuaryCatalog
import kotlin.math.roundToInt

// ─── Playroom aliases ────────────────────────────────────────────────
// Feature code uses the shared design-system tokens; these aliases preserve
// the concise names used throughout this screen.
internal val PlayGoldTop = PlayroomColors.GoldTop
internal val PlayGoldMid = PlayroomColors.GoldMid
internal val PlayCoralBottom = Coral
internal val PlayTeal = VillageTeal
internal val PlayTealPressed = PlayroomColors.TealPressed
internal val PlayInk = Ink
internal val PlayInkDark = DeepNight
internal val PlayCream = Cream
internal val PlayWhite = White
internal val PlaySunshine = SunshineGold
internal val PlayCoral = Coral
internal val PlaySuccess = SuccessGreen
internal val PlayError = OnError
internal val PlayMuted = PlayroomColors.Muted

internal val SubjectAccent = PlayroomColors.SubjectAccent
internal val SubjectPale = PlayroomColors.SubjectPale

/** Stable semantics hooks for child-home interaction and accessibility tests. */
internal object PlayroomHomeTestTags {
    const val TodayQuest = "home_today_quest"
    const val Streak = "home_streak"
    const val Collection = "home_collection"
    const val Parents = "home_parents"
    const val SelectedNavigation = "home_nav_selected"

    fun subject(id: String): String = "home_subject_$id"
}

// ─── Width classes (design.md §7.1) ───
private enum class HomeWidthClass { Wide, Medium, Narrow, Compact }

@Composable
private fun widthClassFor(maxWidth: Dp): HomeWidthClass = when {
    maxWidth >= 1100.dp -> HomeWidthClass.Wide
    maxWidth >= 840.dp -> HomeWidthClass.Medium
    maxWidth >= 600.dp -> HomeWidthClass.Narrow
    else -> HomeWidthClass.Compact
}

/** Maximum subject columns given width class + font scale (§7.1, §7.2). */
@Composable
private fun maxColumns(widthClass: HomeWidthClass, maxWidth: Dp): Int {
    val fontScale = LocalDensity.current.fontScale
    return when {
        fontScale >= 1.8f -> 1
        fontScale >= 1.3f -> 2
        else -> when (widthClass) {
            HomeWidthClass.Wide -> 3
            HomeWidthClass.Medium -> 2
            HomeWidthClass.Narrow -> if (maxWidth >= 720.dp) 2 else 1
            HomeWidthClass.Compact -> 1
        }
    }
}

/**
 * Option 3 Playroom Collections home (design.md). Stateless and previewable;
 * the route owns the ViewModel and navigation.
 */
@Composable
fun PlayroomHomeScreen(
    state: PlayroomHomeUiState,
    onSubjectClick: (String) -> Unit,
    onQuestAction: (QuestAction) -> Unit,
    onHomeClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onTreatShopClick: () -> Unit = {},
    onVideosClick: () -> Unit = {},
    onAssessmentsClick: () -> Unit = {},
    onQuickBitsClick: () -> Unit = onVideosClick,
    onParentsClick: () -> Unit,
    onOpenCollection: () -> Unit = onCollectionClick,
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PlayGoldTop, PlayGoldMid, PlayCoralBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val widthClass = widthClassFor(maxWidth)
        val columns = maxColumns(widthClass, maxWidth)
        val fontScale = LocalDensity.current.fontScale
        val fullWidth = maxWidth
        val showRailBeside = widthClass == HomeWidthClass.Wide

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (fullWidth >= 600.dp) 24.dp else 16.dp, vertical = 16.dp),
        ) {
            // The subject catalogue can be much taller than the viewport (and
            // becomes taller still with Android text scaling). Keep the nav
            // outside this scroll region so it remains reachable and visible.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PlayroomHeader(
                    childName = (state as? PlayroomHomeUiState.Content)?.childName.orEmpty(),
                    offline = (state as? PlayroomHomeUiState.Content)?.offline == true,
                    wide = widthClass == HomeWidthClass.Wide && fontScale < 1.3f,
                    starBalance = (state as? PlayroomHomeUiState.Content)?.starBalance ?: 0,
                    coinBalance = (state as? PlayroomHomeUiState.Content)?.coinBalance ?: 0,
                    keepsakes = (state as? PlayroomHomeUiState.Content)?.ownedKeepsakes.orEmpty(),
                )

                when (state) {
                    is PlayroomHomeUiState.Loading -> LoadingPlaceholders(columns = columns)
                    is PlayroomHomeUiState.Error -> ErrorCard(
                        message = state.message,
                        canRetry = state.canRetry,
                        onRetry = onRetry,
                        onBack = onBack,
                    )
                    is PlayroomHomeUiState.Content -> ContentLayout(
                        content = state,
                        columns = columns,
                        railBeside = showRailBeside,
                        onSubjectClick = onSubjectClick,
                        onQuestAction = onQuestAction,
                        onOpenCollection = onOpenCollection,
                        onTreatShopClick = onTreatShopClick,
                        onVideosClick = onVideosClick,
                        onAssessmentsClick = onAssessmentsClick,
                        onQuickBitsClick = onQuickBitsClick,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            PlayroomBottomNav(
                onHomeClick = onHomeClick,
                onCollectionClick = onCollectionClick,
                onParentsClick = onParentsClick,
                compact = widthClass == HomeWidthClass.Compact || fontScale >= 1.3f,
            )
        }
    }
}

// ─── Content layout ──────────────────────────────────────────────────

@Composable
private fun ContentLayout(
    content: PlayroomHomeUiState.Content,
    columns: Int,
    railBeside: Boolean,
    onSubjectClick: (String) -> Unit,
    onQuestAction: (QuestAction) -> Unit,
    onOpenCollection: () -> Unit,
    onTreatShopClick: () -> Unit,
    onVideosClick: () -> Unit,
    onAssessmentsClick: () -> Unit = {},
    onQuickBitsClick: () -> Unit,
) {
    // “Choose a subject” moves focus to the first available card (§11.4)
    val firstAvailableId = content.subjects.firstOrNull { it.isAvailable }?.id
    val focusRequester = remember { FocusRequester() }
    val questAction: (QuestAction) -> Unit = { action ->
        if (action == QuestAction.ChooseSubject && firstAvailableId != null) {
            focusRequester.requestFocus()
        } else {
            onQuestAction(action)
        }
    }

    if (content.staleBanner) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PlayCream,
            contentColor = PlayInk,
            border = BorderStroke(1.dp, PlaySunshine),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.home_stale_banner),
                fontSize = 15.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Today's Quest Card (Primary action)
        TodayQuestCard(content.quest, questAction, Modifier.fillMaxWidth())

        // Grade 3 Assessment Arena Entry Card
        AssessmentArenaBannerCard(
            onClick = onAssessmentsClick,
            modifier = Modifier.fillMaxWidth()
        )

        // Quick Bits Full-Width Feature Card
        QuickBitsHomeCard(
            onClick = onQuickBitsClick,
            modifier = Modifier.fillMaxWidth(),
        )

        // 6 Subject Cards front and center for curriculum entry
        SubjectGrid(
            subjects = content.subjects,
            columns = columns,
            openingSubjectId = content.openingSubjectId,
            firstFocusId = firstAvailableId,
            firstFocusRequester = focusRequester,
            onSubjectClick = onSubjectClick,
            modifier = Modifier.fillMaxWidth(),
        )

        // Rewards and Sanctuary
        WildlifeStickersPreview(
            wildlifeStickers = content.wildlifeStickers,
            onOpenCollection = onOpenCollection,
            modifier = Modifier.fillMaxWidth(),
        )
        SanctuaryPreview(
            sanctuary = content.sanctuary,
            questTotal = content.quest.pawPrintTotal,
            onTreatShopClick = onTreatShopClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun QuickBitsHomeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "QuickBitsCardPress"
    )

    Surface(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics {
                contentDescription = "Quick Bits video explorer. Watch 60 fun bite-sized science, space, animals, and math videos offline."
                role = Role.Button
            },
        shape = RoundedCornerShape(20.dp),
        color = PlayCream,
        contentColor = PlayInk,
        border = BorderStroke(1.5.dp, PlayTeal.copy(alpha = 0.45f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PlayTeal.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = PlayTeal,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Quick Bits",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = PlayInk,
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SunshineGold.copy(alpha = 0.3f),
                            modifier = Modifier.padding(2.dp),
                        ) {
                            Text(
                                "60 Videos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PlayInkDark,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Animals, Space, Science & Math educational shorts",
                        color = PlayMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PlayTeal,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    "Watch Shorts ▶",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoLibraryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "VideoLibraryCardPress"
    )

    Surface(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics {
                contentDescription = "Video lesson library. Open videos to download and watch offline."
                role = Role.Button
            },
        shape = RoundedCornerShape(18.dp),
        color = PlayCream,
        contentColor = PlayInk,
        border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = PlayTeal,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_video_lessons),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                Text(
                    stringResource(R.string.home_video_lessons_description),
                    color = PlayMuted,
                    fontSize = 15.sp,
                )
            }
            Text(stringResource(R.string.home_open), color = PlayTeal, fontWeight = FontWeight.ExtraBold)
        }
    }
}






// ─── Loading / error (design.md §15) ─────────────────────────────────

@Composable
private fun LoadingPlaceholders(columns: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(PlayCream),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = PlayTeal,
                    strokeWidth = 3.dp,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    stringResource(R.string.home_loading),
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    stringResource(R.string.home_loading_hint),
                    color = PlayMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    // Card placeholders preserve final geometry while the content loads.
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(columns) {
                    Box(
                        Modifier.weight(1f).height(160.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .semantics { disabled() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PlayCream),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = PlayInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canRetry) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PlayTeal, contentColor = Color.White,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onRetry),
                    ) {
                        Text(
                            stringResource(R.string.home_retry),
                            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White, contentColor = PlayInk,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onBack),
                ) {
                    Text(
                        stringResource(R.string.nav_home),
                        fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}


@Composable
private fun FlameGlyph(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val c = color
        drawCircle(c, radius = size.minDimension * .28f, center = Offset(size.width * .5f, size.height * .68f))
        drawCircle(c, radius = size.minDimension * .22f, center = Offset(size.width * .42f, size.height * .42f))
        drawCircle(c, radius = size.minDimension * .15f, center = Offset(size.width * .58f, size.height * .30f))
    }
}


@Composable
private fun WatchToEarnQuestCard(
    totalAccreditedSeconds: Int,
    onOpenVideos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = (totalAccreditedSeconds % 1800).toFloat() / 1800f
    val currentMins = (totalAccreditedSeconds % 1800) / 60
    val watchReduce = LocalAnimationsDisabled.current
    // Animated ring: draw progress with easeOutCubic; tick-pop handled by scale on gift box
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (watchReduce) snap() else tween(700, easing = com.maxinesworld.coredesignsystem.theme.DelightMotion.EaseOutCubic),
        label = "watchProgress",
    )
    val tickPop by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (watchReduce) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 720f),
        label = "watchTickPop",
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(PlayTeal.copy(alpha = 0.12f))
                        .graphicsLayer(scaleX = tickPop, scaleY = tickPop),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 28.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Watch-to-Earn Challenge",
                        color = PlayInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                    Text(
                        "1 Wildlife Sticker per 30 mins watched + passed quizzes",
                        color = PlayMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PlayTeal.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "$currentMins / 30 Mins",
                        color = PlayTeal,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    color = PlayTeal,
                    trackColor = PlayTeal.copy(alpha = 0.15f),
                    strokeWidth = 7.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = PlayTeal,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "$currentMins",
                        color = PlayInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Next Reward: 🦌 Tamaraw Habitat Sticker",
                    color = PlayInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                TextButton(onClick = onOpenVideos) {
                    Text("Browse Videos ▶", color = PlayTeal, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AssessmentArenaBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PlayroomColors.GoldTop),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, SunshineGold),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🏆", fontSize = 26.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Grade 3 Assessment Arena",
                            color = DeepNight,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("🇵🇭 🇸🇬 🇺🇸", fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Test your skills across all 6 subjects & earn +10 Stars!",
                        color = DeepNight.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VillageTeal,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    "Enter Arena ▶",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
