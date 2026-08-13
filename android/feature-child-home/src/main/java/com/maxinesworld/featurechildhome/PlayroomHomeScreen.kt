package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.SanctuaryCatalog
import kotlin.math.roundToInt

// ─── Option 3 palette (design.md §8.1) ───
internal val PlayGoldTop = Color(0xFFFFD76E)
internal val PlayGoldMid = Color(0xFFFFB84D)
internal val PlayCoralBottom = Coral
internal val PlayTeal = VillageTeal
internal val PlayTealPressed = Color(0xFF06676A)
internal val PlayInk = Ink
internal val PlayInkDark = DeepNight
internal val PlayCream = Cream
internal val PlayWhite = White
internal val PlaySunshine = SunshineGold
internal val PlayCoral = Coral
internal val PlaySuccess = SuccessGreen
internal val PlayError = OnError
internal val PlayMuted = Color(0xFF4E5F66)

internal val SubjectAccent = mapOf(
    "mathematics" to SubjectColors.Mathematics.primary,
    "english" to SubjectColors.English.primary,
    "science" to SubjectColors.Science.primary,
    "filipino" to Color(0xFFD96555),
    "araling_panlipunan" to HeritageGold,
    "makabansa" to Color(0xFF8B5E34),
    "gmrc" to KindnessTealText,
)

internal val SubjectPale = mapOf(
    "mathematics" to SubjectColors.Mathematics.surface,
    "english" to SubjectColors.English.surface,
    "science" to SubjectColors.Science.surface,
    "filipino" to Color(0xFFFCEBE7),
    "araling_panlipunan" to Color(0xFFFFF3D7),
    "makabansa" to Color(0xFFF4EBDD),
    "gmrc" to Color(0xFFE5F7F5),
)

private fun subjectAccent(id: String): Color = SubjectAccent[id] ?: PlayTeal
private fun subjectPale(id: String): Color = SubjectPale[id] ?: Color(0xFFF2F2F0)

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
                    onTreatShopClick = onTreatShopClick,
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
                        onVideosClick = onVideosClick,
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
    onVideosClick: () -> Unit,
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
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    if (railBeside) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SubjectGrid(
                subjects = content.subjects,
                columns = columns,
                openingSubjectId = content.openingSubjectId,
                firstFocusId = firstAvailableId,
                firstFocusRequester = focusRequester,
                onSubjectClick = onSubjectClick,
                modifier = Modifier.weight(0.66f),
            )
            Column(Modifier.weight(0.34f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VideoLibraryCard(onClick = onVideosClick)
                TodayQuestCard(content.quest, questAction, Modifier.fillMaxWidth())
                SanctuaryPreview(content.sanctuary, Modifier.fillMaxWidth())
                WildlifeStickersPreview(
                    wildlifeStickers = content.wildlifeStickers,
                    onOpenCollection = onOpenCollection,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            VideoLibraryCard(onClick = onVideosClick)
            // Quest moves above the grid for medium/narrow widths (§7.1)
            TodayQuestCard(content.quest, questAction, Modifier.fillMaxWidth())
            SanctuaryPreview(content.sanctuary, Modifier.fillMaxWidth())
            WildlifeStickersPreview(
                wildlifeStickers = content.wildlifeStickers,
                onOpenCollection = onOpenCollection,
                modifier = Modifier.fillMaxWidth(),
            )
            SubjectGrid(
                subjects = content.subjects,
                columns = columns,
                openingSubjectId = content.openingSubjectId,
                firstFocusId = firstAvailableId,
                firstFocusRequester = focusRequester,
                onSubjectClick = onSubjectClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VideoLibraryCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
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
                Text("Video Lessons", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(
                    "Download and watch optional lessons offline",
                    color = PlayMuted,
                    fontSize = 13.sp,
                )
            }
            Text("Open", color = PlayTeal, fontWeight = FontWeight.ExtraBold)
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
    onTreatShopClick: () -> Unit = {},
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = onTreatShopClick,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Sanctuary Workshop"
                role = Role.Button
            },
        ) {
            Text("Sanctuary Workshop", fontWeight = FontWeight.ExtraBold)
        }
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
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
            "Milo's decorations",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF5C2E00),
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
                            fontSize = 13.sp,
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
                "PLAYROOM",
                fontSize = 10.sp, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, color = Color(0xFF7A3B00),
            )
        }
    }
}

private val PlayHeritageGold = Color(0xFFB87916)

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
            color = Color(0xFF5C2E00),
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
            Text(label, color = PlayMuted, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
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
                    Modifier.fillMaxWidth().heightIn(min = 44.dp),
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
                                fontSize = 14.sp, lineHeight = 18.sp,
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
                    contentColor = Color(0xFFFFE9A8),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp),
                ) {
                    Text(
                        subject.lockReason ?: stringResource(R.string.home_locked),
                        fontSize = 14.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ─── This Week's Quest (design.md §11) ────────────────────────────────

@Composable
private fun TodayQuestCard(
    quest: QuestUi,
    onQuestAction: (QuestAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PlayCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header: teal→ink dark gradient
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(PlayTeal, PlayInkDark)))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PawGlyph(Color(0xFFFFE9A8), size = 20.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.home_today_quest),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp, lineHeight = 28.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }

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
                            quest.task,
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
                                fontSize = 14.sp, lineHeight = 18.sp,
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
                            if (quest.godModeEnabled) {
                                "Parent mode: Playground + all stickers and rewards unlocked"
                            } else if (quest.isComplete) {
                                "Reward earned: a sanctuary piece + 5-minute play break"
                            } else {
                                "Reward at 3/3: a sanctuary piece + 5-minute play break"
                            },
                            color = PlayInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
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
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape)
                                        .background(if (targetDone) PlaySunshine else PlayInk.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (targetDone) Text("✓", fontWeight = FontWeight.Black, color = PlayInkDark, fontSize = 14.sp)
                                    else Text(target.displaySubject.first().toString().uppercase(), fontWeight = FontWeight.Black, color = PlayMuted, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(target.displaySubject, fontSize = 11.sp, fontWeight = FontWeight.Black, color = PlayMuted, maxLines = 1)
                                    Text(displayTitle, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, color = PlayInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = PlayTeal,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics { role = Role.Button }
                        .clickable(role = Role.Button, onClick = { onQuestAction(quest.buttonAction) }),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            quest.buttonLabel,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp, lineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ─── Milo's Wildlife Sanctuary ───────────────────────────────────────

@Composable
private fun SanctuaryPreview(
    sanctuary: SanctuaryUi,
    modifier: Modifier = Modifier,
) {
    val progress = if (sanctuary.totalPieces > 0) {
        (sanctuary.earnedPieces.toFloat() / sanctuary.totalPieces).coerceIn(0f, 1f)
    } else {
        0f
    }
    val orderedPieces = SanctuaryCatalog.pieces
        .take(sanctuary.totalPieces.coerceAtLeast(0))
        .map { piece -> SanctuaryPieceUi(piece.id, piece.name, piece.description, piece.iconKey) }
    val boardCells = sanctuaryBoardCells(sanctuary, orderedPieces)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F6EE)),
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
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.home_sanctuary_subtitle),
                color = PlayInk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(10.dp))
            SanctuaryScene(sanctuary = sanctuary)
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
                color = Color(0xFFBFE5CC),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Milo's home board. ${sanctuary.earnedPieces} of ${sanctuary.totalPieces} places added."
                    },
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.home_sanctuary_board_title),
                        color = PlayInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                    boardCells.chunked(3).forEach { rowCells ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowCells.forEach { cell -> SanctuaryBoardCell(cell) }
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
                    modifier = Modifier.fillMaxWidth(),
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
                                fontSize = 12.sp,
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
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.home_sanctuary_complete),
                    color = PlayInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            // The earn hint describes how to add the NEXT place. Once the
            // sanctuary is complete (or god mode shows it complete) there is
            // nothing left to earn — showing both lines is contradictory.
            if (nextPiece != null) {
                Spacer(Modifier.height(9.dp))
                Text(
                    stringResource(R.string.home_sanctuary_earn_hint),
                    color = PlayMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun RowScope.SanctuaryBoardCell(cell: SanctuaryBoardCellUi) {
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
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 78.dp)
            .semantics {
                contentDescription = when {
                    cell.isEarned -> "${cell.piece.name}, added to Milo's home"
                    cell.isNext -> "${cell.piece.name}, next place to add"
                    else -> "Locked sanctuary place"
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
            Icon(
                if (cell.isEarned || cell.isNext) sanctuaryIcon(cell.piece.iconKey) else Icons.Default.Lock,
                contentDescription = null,
                tint = if (cell.isEarned || cell.isNext) PlayTeal else PlayMuted.copy(alpha = 0.58f),
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (cell.isEarned || cell.isNext) cell.piece.name else stringResource(R.string.home_sanctuary_locked_place),
                color = if (cell.isEarned || cell.isNext) PlayInk else PlayMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
                    fontSize = 14.sp, lineHeight = 18.sp,
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
                        fontSize = 14.sp, lineHeight = 20.sp,
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
                if (sticker.won) Brush.linearGradient(listOf(Color(0xFFFFF6D6), Color(0xFFFFE9A8)))
                else Brush.linearGradient(listOf(Color(0xFFF3EFE2), Color(0xFFECE7D8))),
                RoundedCornerShape(10.dp),
            )
            .border(2.dp, if (sticker.won) PlaySunshine else Color(0xFFD9C48F), RoundedCornerShape(10.dp))
            .semantics {
                contentDescription = if (sticker.won) "${sticker.id} collected sticker" else mystery
            },
        contentAlignment = Alignment.Center,
    ) {
        if (sticker.won) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = PlayTeal, modifier = Modifier.size(24.dp))
        } else {
            Text("?", color = Color(0xFF8A6A3A), fontWeight = FontWeight.Black, fontSize = 16.sp)
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
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
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
