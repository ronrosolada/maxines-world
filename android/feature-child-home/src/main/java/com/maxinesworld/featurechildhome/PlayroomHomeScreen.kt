package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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

private fun subjectAccent(id: String): Color = SubjectAccent[id] ?: PlayTeal
private fun subjectPale(id: String): Color = SubjectPale[id] ?: PlayroomColors.FallbackSurface

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
                    .padding(bottom = if (fullWidth < 600.dp || fontScale >= 1.3f) 32.dp else 16.dp)
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
        // 30-Min Video Watch-to-Earn Quest Header
        WatchToEarnQuestCard(
            totalAccreditedSeconds = content.totalAccreditedSeconds,
            onOpenVideos = onVideosClick,
            modifier = Modifier.fillMaxWidth()
        )
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
    Surface(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .clickable(onClick = onClick)
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

// ─── Header (design.md §9) ───────────────────────────────────────────

@Composable
private fun PlayroomHeader(
    childName: String,
    offline: Boolean,
    wide: Boolean,
    starBalance: Int = 0,
    coinBalance: Int = 0,
    keepsakes: List<KeepsakeUi> = emptyList(),
) {
    if (wide) {
        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandBlock(Modifier.width(210.dp))
            GreetingBlock(childName, Modifier.widthIn(min = 250.dp).padding(horizontal = 16.dp))
            BalanceChips(starBalance, coinBalance, Modifier.weight(1f))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandBlock(Modifier.weight(1f))
            BalanceChips(starBalance, coinBalance)
        }
        GreetingBlock(childName, Modifier.fillMaxWidth().padding(top = 8.dp))
    }
    KeepsakesStrip(keepsakes)
    if (offline) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = PlayInkDark, contentColor = PlayWhite,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                stringResource(R.string.home_offline_chip),
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/** Child-visible learning stars and sanctuary tokens — earned work stays visible. */
@Composable
private fun BalanceChips(stars: Int, coins: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BalanceChip("stars", "★", stars, PlaySunshine)
        BalanceChip("sanctuary tokens", "●", coins, PlayTeal)
    }
}

/**
 * Owned Treat Shop keepsakes — the visible payoff for spending coins. Hidden
 * entirely until the child owns at least one item; every purchase then shows
 * up on the home so the reward loop always ends in something to see.
 */
@Composable
private fun KeepsakesStrip(keepsakes: List<KeepsakeUi>) {
    if (keepsakes.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.home_milo_decorations),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlayroomColors.KeepsakeHeading,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(keepsakes, key = { it.itemId }) { keepsake ->
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    shadowElevation = 1.dp,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Milo's keepsake: ${keepsake.name}"
                    },
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = keepsakeIcon(keepsake.iconKey),
                            contentDescription = null,
                            tint = PlayTeal,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            keepsake.name,
                            color = PlayInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceChip(label: String, icon: String, value: Int, tint: Color) {
    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$value $label"
        },
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = 0.85f),
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(icon, fontSize = 16.sp, color = tint, fontWeight = FontWeight.Black)
            Text(
                "$value",
                color = PlayInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun BrandBlock(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            PawGlyph(PlayHeritageGold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(R.string.home_brand),
                color = PlayInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp, lineHeight = 24.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.home_playroom_label),
                fontSize = 12.sp, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, color = PlayroomColors.BrandLabel,
            )
        }
    }
}

private val PlayHeritageGold = HeritageGold

@Composable
private fun GreetingBlock(childName: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            stringResource(
                if (childName.isBlank()) R.string.home_greeting_fallback
                else R.string.home_greeting, childName
            ),
            color = PlayInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp, lineHeight = 32.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(R.string.home_encouragement),
            color = PlayroomColors.KeepsakeHeading,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp, lineHeight = 21.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, color = PlayInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp)
            Text(label, color = PlayMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp)
        }
    }
}

// ─── Subject grid + card (design.md §10) ─────────────────────────────

@Composable
private fun SubjectGrid(
    subjects: List<SubjectCardUi>,
    columns: Int,
    openingSubjectId: String?,
    firstFocusId: String?,
    firstFocusRequester: FocusRequester,
    onSubjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        subjects.chunked(columns).forEach { rowSubjects ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rowSubjects.forEach { subject ->
                    SubjectCard(
                        subject = subject,
                        opening = subject.id == openingSubjectId,
                        firstFocus = subject.id == firstFocusId,
                        firstFocusRequester = firstFocusRequester,
                        onClick = { onSubjectClick(subject.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowSubjects.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectCardUi,
    opening: Boolean,
    firstFocus: Boolean,
    firstFocusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = subjectAccent(subject.id)
    val pale = subjectPale(subject.id)
    val enabled = subject.isAvailable && !opening
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val progress = subject.progressPercent?.coerceIn(0, 100)
    val progressLabel = when {
        progress == null -> stringResource(R.string.home_not_started)
        progress >= 100 -> stringResource(R.string.home_complete)
        else -> "$progress% complete"
    }
    val spoken = buildString {
        append(subject.formalName).append(", ").append(subject.playfulName)
        // Progress is announced via stateDescription below — not repeated here
        // (design.md §10.4 / audit AC18, 2026-08-06).
        if (subject.availability == SubjectAvailability.Locked) {
            append(". ").append(stringResource(R.string.home_locked)).append(".")
            subject.lockReason?.let { append(" ").append(it).append(".") }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                role = Role.Button
                if (!enabled) disabled()
                stateDescription = progressLabel
            }
            .focusable(enabled = enabled, interactionSource = interaction)
            .then(if (firstFocus) Modifier.focusRequester(firstFocusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) PlayTeal else Color.Transparent,
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Top content row (baseline 132dp): illustration + text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(pale, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(subject.illustrationRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(88.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            subject.formalName,
                            color = PlayInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp, lineHeight = 22.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, // never ellipsize formal name
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subject.playfulName,
                            // Ink, not the subject accent: 5 of 6 accent colors
                            // fail 4.5:1 on white at 15sp (audit AC22,
                            // 2026-08-06); accents stay on icon/progress/arrow.
                            color = PlayInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, lineHeight = 20.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Bottom row: progress bar + arrow
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(pale, RoundedCornerShape(99.dp)),
                        ) {
                            if (progress != null && progress > 0) {
                                Box(
                                    Modifier.fillMaxWidth(progress / 100f).fillMaxHeight()
                                        .background(accent, RoundedCornerShape(99.dp)),
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (progress != null && progress >= 100) {
                                Icon(Icons.Filled.CheckCircle, null, tint = PlaySuccess, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                progressLabel,
                                color = PlayMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, lineHeight = 18.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (opening) pale else accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (opening) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accent,
                                strokeWidth = 2.5.dp,
                            )
                        } else if (enabled) {
                            Text("›", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        } else {
                            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Locked reason chip (visible, not low-opacity-only)
            if (subject.availability == SubjectAvailability.Locked) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = PlayInkDark,
                    contentColor = PlayroomColors.LockedSurfaceText,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp),
                ) {
                    Text(
                        subject.lockReason ?: stringResource(R.string.home_locked),
                        fontSize = 15.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ─── Today's Quest (design.md §11) ───────────────────────────────────

@Composable
private fun TodayQuestCard(
    quest: QuestUi,
    onQuestAction: (QuestAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val taskText = when (quest.task) {
        QuestTaskCopy.ParentMode -> stringResource(R.string.home_quest_task_parent)
        QuestTaskCopy.CompleteToday -> if (quest.sanctuaryComplete) {
            stringResource(R.string.home_sanctuary_complete)
        } else {
            stringResource(R.string.home_quest_task_complete)
        }
        QuestTaskCopy.IncompleteToday -> stringResource(
            R.string.home_quest_task_incomplete,
            quest.pawPrintTotal,
        )
    }
    val buttonText = when (quest.buttonLabel) {
        QuestButtonLabel.OpenPlayground -> stringResource(R.string.home_quest_open_playground)
        QuestButtonLabel.OpenSanctuary -> stringResource(R.string.home_quest_open_sanctuary)
        QuestButtonLabel.ChooseSubject -> stringResource(R.string.home_choose_subject)
        QuestButtonLabel.StartQuest -> stringResource(R.string.home_quest_start)
        QuestButtonLabel.ContinueQuest -> stringResource(R.string.home_quest_continue)
        QuestButtonLabel.Start -> stringResource(R.string.home_start)
        QuestButtonLabel.Continue -> stringResource(R.string.home_continue)
    }
    MaxinesQuestCardSurface(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            MaxinesQuestCardHeader(
                title = stringResource(R.string.home_today_quest),
                leadingContent = { PawGlyph(PlayroomColors.LockedSurfaceText, size = 20.dp) },
            )

            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.mw_mascot_guide),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(58.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            taskText,
                            color = PlayInk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp, lineHeight = 23.sp,
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(quest.pawPrintTotal) { i ->
                                    PawGlyph(
                                        if (i < quest.pawPrintsCompleted) PlaySunshine else PlayInk.copy(alpha = 0.18f),
                                        size = 18.dp,
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${quest.pawPrintsCompleted} of ${quest.pawPrintTotal}",
                                color = PlayMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, lineHeight = 18.sp,
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .semantics {
                            contentDescription = if (quest.godModeEnabled) {
                                "Parent mode: Playground and all rewards unlocked"
                            } else if (quest.sanctuaryComplete) {
                                "Reward: five minute play break; Milo's home is complete"
                            } else if (quest.isComplete) {
                                "Reward earned: sanctuary piece and five minute play break"
                            } else {
                                "Quest reward: one sanctuary piece and five minute play break"
                            }
                        },
                    color = if (quest.godModeEnabled || quest.isComplete) PlaySunshine.copy(alpha = 0.28f) else PlayTeal.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = if (quest.godModeEnabled || quest.isComplete) PlayInkDark else PlayTeal,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                quest.godModeEnabled -> stringResource(R.string.home_quest_reward_parent)
                                quest.sanctuaryComplete -> stringResource(R.string.home_quest_reward_sanctuary_complete)
                                quest.isComplete -> stringResource(R.string.home_quest_reward_earned)
                                else -> stringResource(
                                    R.string.home_quest_reward_pending,
                                    quest.pawPrintTotal,
                                )
                            },
                            color = PlayInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                if (quest.targets.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    // When daily targets share the same title (e.g. three
                    // sequential "Word Roots" lessons), an 8yo sees a dupe.
                    // Only then, show the week/day distinguisher so titles feel
                    // like Part 1/2/3 instead of a copy-paste bug.
                    val titleCounts = quest.targets.groupingBy { it.title }.eachCount()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quest.targets.forEach { target ->
                            val targetDone = target.isCompleted
                            val needsDisambiguation = (titleCounts[target.title] ?: 0) > 1
                            val displayTitle = if (needsDisambiguation && target.moduleKey != null) {
                                val suffix = com.maxinesworld.corecontent.questTargetDisambiguator(
                                    target.moduleKey, target.lessonId,
                                )
                                if (suffix != null) "${target.title} · $suffix" else target.title
                            } else target.title
                            val targetCd = if (targetDone) "Quest target done: $displayTitle"
                            else "Quest target: ${target.displaySubject}: $displayTitle"
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.72f))
                                    .clickable(role = Role.Button, onClick = { onQuestAction(QuestAction.OpenLesson) })
                                    .semantics { contentDescription = targetCd }
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape)
                                        .background(if (targetDone) PlaySunshine else PlayInk.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (targetDone) Text("✓", fontWeight = FontWeight.Black, color = PlayInkDark, fontSize = 14.sp)
                                    else Text(target.displaySubject.first().toString().uppercase(), fontWeight = FontWeight.Black, color = PlayMuted, fontSize = 15.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(target.displaySubject, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, color = PlayMuted, maxLines = 1)
                                    Text(displayTitle, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = PlayInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                if (targetDone) {
                                    Box(Modifier.size(22.dp).clip(CircleShape).background(PlaySunshine.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                                        Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PlayInkDark)
                                    }
                                } else {
                                    Text("›", fontSize = 18.sp, fontWeight = FontWeight.Black, color = PlayTeal)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                MaxinesPrimaryButton(
                    onClick = { onQuestAction(quest.buttonAction) },
                    text = buttonText,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                    cornerRadius = 18.dp,
                )
            }
        }
    }
}

// ─── Milo's Wildlife Sanctuary ───────────────────────────────────────

@Composable
private fun SanctuaryPreview(
    sanctuary: SanctuaryUi,
    questTotal: Int,
    onTreatShopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPieceForInspect by remember { mutableStateOf<SanctuaryPieceUi?>(null) }
    var selectedVisitorForInspect by remember { mutableStateOf<SanctuaryVisitorUi?>(null) }
    var miloTapCount by remember { mutableIntStateOf(0) }
    val progress = if (sanctuary.totalPieces > 0) {
        (sanctuary.earnedPieces.toFloat() / sanctuary.totalPieces).coerceIn(0f, 1f)
    } else {
        0f
    }
    val orderedPieces = SanctuaryCatalog.pieces
        .take(sanctuary.totalPieces.coerceAtLeast(0))
        .map { piece ->
            SanctuaryPieceUi(
                id = piece.id,
                name = piece.name,
                description = piece.description,
                iconKey = piece.iconKey,
                residentWildlife = piece.residentWildlife,
                funFact = piece.funFact,
            )
        }
    val boardCells = sanctuaryBoardCells(sanctuary, orderedPieces)
    val workshopLabel = stringResource(R.string.home_sanctuary_workshop)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PlayroomColors.SanctuarySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Park, contentDescription = null, tint = PlayTeal, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.home_sanctuary_title),
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(
                        R.string.home_sanctuary_piece_count,
                        sanctuary.earnedPieces,
                        sanctuary.totalPieces,
                    ),
                    color = PlayTeal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.home_sanctuary_subtitle, questTotal),
                color = PlayInk,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(10.dp))
            SanctuaryScene(
                sanctuary = sanctuary,
                onPieceClick = { piece -> selectedPieceForInspect = piece },
                onVisitorClick = { visitor -> selectedVisitorForInspect = visitor },
                onMiloClick = { miloTapCount++ },
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = PlayTeal,
                trackColor = PlayTeal.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PlayroomColors.SanctuaryBoardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Milo's home board. ${sanctuary.earnedPieces} of ${sanctuary.totalPieces} places added. Tap any place to inspect."
                    },
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.home_sanctuary_board_title),
                            color = PlayInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "Tap to inspect",
                            color = PlayTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                    }
                    boardCells.chunked(3).forEach { rowCells ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowCells.forEach { cell ->
                                SanctuaryBoardCell(
                                    cell = cell,
                                    onClick = {
                                        if (cell.isEarned || cell.isNext) {
                                            selectedPieceForInspect = cell.piece
                                        }
                                    }
                                )
                            }
                            repeat(3 - rowCells.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val nextPiece = sanctuary.nextPiece
            if (nextPiece != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedPieceForInspect = nextPiece },
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            sanctuaryIcon(nextPiece.iconKey),
                            contentDescription = null,
                            tint = PlayTeal,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.home_sanctuary_next_reward),
                                color = PlayTeal,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                nextPiece.name,
                                color = PlayInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                            Text(
                                nextPiece.description,
                                color = PlayMuted,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.home_sanctuary_complete),
                    color = PlayInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            if (nextPiece != null) {
                Spacer(Modifier.height(9.dp))
                Text(
                    stringResource(R.string.home_sanctuary_earn_hint, questTotal),
                    color = PlayMuted,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }
            TextButton(
                onClick = onTreatShopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = workshopLabel
                        role = Role.Button
                    },
            ) {
                Text(
                    stringResource(R.string.home_sanctuary_workshop),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }
        }
    }

    selectedPieceForInspect?.let { piece ->
        val isEarned = sanctuary.visiblePieces.any { it.id == piece.id }
        SanctuaryPieceInspectionDialog(
            piece = piece,
            isEarned = isEarned,
            onDismiss = { selectedPieceForInspect = null },
        )
    }

    selectedVisitorForInspect?.let { visitor ->
        SanctuaryVisitorInspectionDialog(
            visitor = visitor,
            onDismiss = { selectedVisitorForInspect = null },
        )
    }
}

@Composable
private fun RowScope.SanctuaryBoardCell(
    cell: SanctuaryBoardCellUi,
    onClick: () -> Unit = {},
) {
    val background = when {
        cell.isEarned -> Color.White.copy(alpha = 0.92f)
        cell.isNext -> PlaySunshine.copy(alpha = 0.78f)
        else -> Color.White.copy(alpha = 0.42f)
    }
    val borderColor = when {
        cell.isNext -> PlayTeal
        cell.isEarned -> PlayTeal.copy(alpha = 0.28f)
        else -> PlayMuted.copy(alpha = 0.18f)
    }
    val clickableModifier = if (cell.isEarned || cell.isNext) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(clickableModifier)
            .semantics {
                role = Role.Button
                contentDescription = when {
                    cell.isEarned -> "${cell.piece.name}, place added to Milo's home. Tap to inspect."
                    cell.isNext -> "Next reward: ${cell.piece.name}. Finish today's quest to unlock. Tap to preview."
                    else -> "${cell.piece.name}, locked place."
                }
                stateDescription = when {
                    cell.isEarned -> "Added"
                    cell.isNext -> "Next reward"
                    else -> "Locked"
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(if (cell.isNext) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (cell.isEarned || cell.isNext) PlayTeal.copy(alpha = 0.12f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (cell.isEarned || cell.isNext) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(cell.piece.iconKey)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = PlayMuted.copy(alpha = 0.58f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                if (cell.isEarned || cell.isNext) cell.piece.name else stringResource(R.string.home_sanctuary_locked_place),
                color = if (cell.isEarned || cell.isNext) PlayInk else PlayMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SanctuaryPieceInspectionDialog(
    piece: SanctuaryPieceUi,
    isEarned: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isEarned) PlayTeal.copy(alpha = 0.15f) else PlaySunshine.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(piece.iconKey)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
                Column {
                    Text(
                        piece.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlayInk,
                    )
                    Text(
                        if (isEarned) "Sanctuary Habitat • Unlocked" else "Next Habitat Unlock",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isEarned) PlayTeal else PlayroomColors.KeepsakeHeading,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    piece.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlayInk,
                    lineHeight = 20.sp,
                )

                if (piece.funFact.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PlayroomColors.SanctuarySurface,
                        border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Milo's Field Note",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PlayTeal
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                piece.funFact,
                                style = MaterialTheme.typography.bodySmall,
                                color = PlayInk,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (piece.residentWildlife.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Native Animals That Love This Place:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PlayroomColors.KeepsakeHeading
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            piece.residentWildlife.forEach { animal ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.2f)),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        "🐾 $animal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = PlayInk,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PlayTeal),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PlayCream,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun SanctuaryVisitorInspectionDialog(
    visitor: SanctuaryVisitorUi,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val drawableId = context.resources.getIdentifier(visitor.drawableResName, "drawable", context.packageName)
    val effectiveDrawable = if (drawableId != 0) drawableId else R.drawable.character_milo

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, PlayTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(effectiveDrawable),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }
                Column {
                    Text(
                        visitor.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlayInk,
                    )
                    Text(
                        "${visitor.localName} • ${visitor.nativeRegion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PlayTeal,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PlayroomColors.SanctuarySurface,
                    border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Maxine's Wildlife Field Fact",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PlayTeal
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            visitor.funFact,
                            style = MaterialTheme.typography.bodySmall,
                            color = PlayInk,
                            lineHeight = 18.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.2f))
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("FAVORITE TREAT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PlayMuted)
                            Text(visitor.favoriteTreat, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PlayInk)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.2f))
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ACTIVITY CYCLE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PlayMuted)
                            Text(if (visitor.isNocturnal) "🌙 Nocturnal" else "☀️ Diurnal", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PlayTeal)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PlayTeal),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Awesome!", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PlayCream,
        shape = RoundedCornerShape(20.dp),
    )
}

private fun sanctuaryIcon(iconKey: String): ImageVector = when (iconKey) {
    "tree", "garden", "meadow", "flower" -> Icons.Default.Park
    else -> Icons.Default.Pets
}

private fun keepsakeIcon(iconKey: String): ImageVector = when (iconKey) {
    "tree", "garden", "meadow", "flower", "park" -> Icons.Default.Park
    else -> Icons.Default.Pets
}

// ─── Wildlife stickers preview (design.md §12) ───────────────────────

@Composable
private fun WildlifeStickersPreview(
    wildlifeStickers: WildlifeStickersUi,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_sticker_book),
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp, lineHeight = 22.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.home_collected, wildlifeStickers.collectedCount, wildlifeStickers.totalCount),
                    color = PlayMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, lineHeight = 18.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(12.dp))
            // Show won stickers plus locked "mystery" placeholders, capped at
            // 7 visible slots (design.md §12.2 / audit gap 12, 2026-08-06).
            val wonStickers = wildlifeStickers.stickers.filter { it.won }
            val previewSlots: List<StickerUi> = if (wonStickers.isEmpty() && wildlifeStickers.totalCount == 0) {
                emptyList()
            } else {
                val shown = wonStickers.take(7)
                shown + List((7 - shown.size).coerceAtLeast(0)) { i ->
                    StickerUi(id = "mystery-$i", won = false)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (previewSlots.isEmpty()) {
                    Text(
                        stringResource(R.string.home_no_stickers),
                        color = PlayMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp, lineHeight = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(previewSlots) { sticker -> StickerSlot(sticker) }
                    }
                }
                TextButton(onClick = onOpenCollection) {
                    Text(stringResource(R.string.home_open_field_guide), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun StickerSlot(sticker: StickerUi) {
    val mystery = stringResource(R.string.home_mystery_sticker)
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
            .background(
                if (sticker.won) Brush.linearGradient(
                    listOf(PlayroomColors.StickerWonStart, PlayroomColors.StickerWonEnd),
                )
                else Brush.linearGradient(
                    listOf(PlayroomColors.StickerLockedStart, PlayroomColors.StickerLockedEnd),
                ),
                RoundedCornerShape(10.dp),
            )
            .border(
                2.dp,
                if (sticker.won) PlaySunshine else PlayroomColors.StickerLockedBorder,
                RoundedCornerShape(10.dp),
            )
            .semantics {
                contentDescription = if (sticker.won) "${sticker.id} collected sticker" else mystery
            },
        contentAlignment = Alignment.Center,
    ) {
        if (sticker.won) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = PlayTeal, modifier = Modifier.size(24.dp))
        } else {
            Text("?", color = PlayroomColors.StickerLockedText, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

// ─── Bottom nav (design.md §13) ──────────────────────────────────────

@Composable
private fun PlayroomBottomNav(
    onHomeClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onParentsClick: () -> Unit,
    compact: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White, RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem("Home", isSelected = true, onClick = onHomeClick, compact = compact)
        NavItem(
            stringResource(R.string.nav_collection),
            isSelected = false,
            onClick = onCollectionClick,
            compact = compact,
        )
        NavItem(
            stringResource(R.string.nav_parents),
            isSelected = false,
            onClick = onParentsClick,
            compact = compact,
            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = PlayTeal, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    compact: Boolean,
    comingSoon: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val enabled = onClick != null
    val interaction = remember { MutableInteractionSource() }
    val showComingSoon = comingSoon && !compact
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PlayTeal.copy(alpha = 0.14f) else Color.Transparent)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                if (isSelected) selected = true
                if (!enabled) disabled()
            }
            .focusable(enabled = enabled, interactionSource = interaction)
            .then(
                if (enabled) Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        } else {
            Text("●", color = if (isSelected) PlayTeal else if (enabled) PlayMuted else PlayMuted.copy(alpha = 0.5f), fontSize = 16.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (isSelected) PlayTeal else if (enabled) PlayMuted else PlayMuted.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp, // never below 14sp even compact (design.md §13.2)
            lineHeight = 18.sp,
            maxLines = 1,
        )
        if (showComingSoon) {
            Text(
                stringResource(R.string.home_coming_soon),
                color = PlayMuted.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp, lineHeight = 18.sp,
                maxLines = 1,
            )
        }
        // Active underline indicator
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(if (isSelected) 20.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (isSelected) PlayTeal else Color.Transparent),
        )
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

// ─── Glyphs ──────────────────────────────────────────────────────────

@Composable
private fun PawGlyph(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val c = color
        drawCircle(c, radius = size.toPx() * .30f, center = Offset(this.size.width / 2f, this.size.height * .72f))
        drawCircle(c, radius = size.toPx() * .16f, center = Offset(this.size.width * .32f, this.size.height * .40f))
        drawCircle(c, radius = size.toPx() * .16f, center = Offset(this.size.width * .68f, this.size.height * .40f))
        drawCircle(c, radius = size.toPx() * .13f, center = Offset(this.size.width * .18f, this.size.height * .58f))
        drawCircle(c, radius = size.toPx() * .13f, center = Offset(this.size.width * .82f, this.size.height * .58f))
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
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(PlayTeal.copy(alpha = 0.12f)),
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
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = PlayTeal,
                trackColor = PlayTeal.copy(alpha = 0.15f),
            )
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PlayroomColors.GoldTop),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, SunshineGold),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Grade 3 Assessment Arena",
                            color = DeepNight,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("🇵🇭 🇸🇬 🇺🇸", fontSize = 14.sp)
                    }
                    Text(
                        "Test your skills across all 6 subjects & earn +10 Stars!",
                        color = DeepNight.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VillageTeal,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    "Enter Arena ▶",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
