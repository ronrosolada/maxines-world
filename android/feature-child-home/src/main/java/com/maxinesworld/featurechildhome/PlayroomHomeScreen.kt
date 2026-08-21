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
import androidx.compose.ui.semantics.heading
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
    const val SubjectGrid = "home_subjects"
    const val SubjectsHeading = "home_subjects_heading"

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

        // The active home uses a responsive vertical subject grid, not the
        // retired illustrated map or a horizontally clipped subject row.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_explore_subjects),
                modifier = Modifier
                    .testTag(PlayroomHomeTestTags.SubjectsHeading)
                    .semantics { heading() },
                color = PlayInk,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                lineHeight = 28.sp,
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
