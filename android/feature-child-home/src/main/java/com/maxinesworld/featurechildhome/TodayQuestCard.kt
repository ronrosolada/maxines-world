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


// ─── Today's Quest (design.md §11) ───────────────────────────────────

@Composable
internal fun TodayQuestCard(
    quest: QuestUi,
    onQuestAction: (QuestAction) -> Unit,
    onQuestTargetClick: (QuestTargetUi) -> Unit,
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
        QuestTaskCopy.Unavailable -> stringResource(R.string.home_quest_unavailable)
    }
    val buttonText = when (quest.buttonLabel) {
        QuestButtonLabel.OpenPlayground -> stringResource(R.string.home_quest_open_playground)
        QuestButtonLabel.OpenSanctuary -> stringResource(R.string.home_quest_open_sanctuary)
        QuestButtonLabel.ChooseSubject -> stringResource(R.string.home_choose_subject)
        QuestButtonLabel.StartQuest -> stringResource(R.string.home_quest_start)
        QuestButtonLabel.ContinueQuest -> stringResource(R.string.home_quest_continue)
        QuestButtonLabel.Start -> stringResource(R.string.home_start)
        QuestButtonLabel.Continue -> stringResource(R.string.home_continue)
        QuestButtonLabel.Retry -> stringResource(R.string.home_retry)
    }
    MaxinesQuestCardSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PlayroomHomeTestTags.TodayQuest)
            .clickable(
                role = Role.Button,
                onClickLabel = "Open today's mission",
                onClick = { onQuestAction(quest.buttonAction) },
            )
            .semantics {
                contentDescription = "Today's mission. ${quest.pawPrintsCompleted} of ${quest.pawPrintTotal} complete"
                stateDescription = "${quest.pawPrintsCompleted} of ${quest.pawPrintTotal} complete"
            },
    ) {
        Column(Modifier.fillMaxWidth()) {
            MaxinesQuestCardHeader(
                title = stringResource(R.string.home_today_quest),
                leadingContent = { PawGlyph(PlayroomColors.LockedSurfaceText, size = 20.dp) },
            )

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.mw_mascot_guide),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(52.dp),
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
                        // Animated paw bar: each newly-earned paw pops (scale) as pawPrintsCompleted grows.
                        // Reduced-motion: snap, no pop. Idle twinkle on complete handled below.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(quest.pawPrintTotal) { i ->
                                    val filled = i < quest.pawPrintsCompleted
                                    val reduceMotionQuest = LocalAnimationsDisabled.current
                                    val pawScale by animateFloatAsState(
                                        targetValue = if (filled) 1f else 0.95f,
                                        animationSpec = if (reduceMotionQuest) snap() else tween(
                                            durationMillis = 240,
                                            easing = FastOutSlowInEasing,
                                        ),
                                        label = "pawScale$i",
                                    )
                                    // Only pop the paw that just filled: stagger hint via index
                                    Box(Modifier.graphicsLayer(scaleX = if (!reduceMotionQuest && filled) pawScale else 1f, scaleY = if (!reduceMotionQuest && filled) pawScale else 1f)) {
                                        PawGlyph(
                                            if (filled) PlayTeal else PlayInk.copy(alpha = 0.18f),
                                            size = 18.dp,
                                        )
                                    }
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

                // P1 fix: quest banner no longer infinite pulse; single gentle pop on complete, then still (avoids motion sickness on home).
                val bannerReduce = LocalAnimationsDisabled.current
                val bannerScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = if (bannerReduce) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "questBanner",
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = bannerScale, scaleY = bannerScale)
                        .clip(RoundedCornerShape(14.dp))
                        .semantics {
                            contentDescription = if (quest.godModeEnabled) {
                                "Parent mode: Playground and all rewards unlocked"
                            } else if (quest.sanctuaryComplete) {
                                "Reward: one five minute play break; Milo's home is complete"
                            } else if (quest.isComplete) {
                                "Reward earned: sanctuary piece and one five minute play break"
                            } else if (quest.task == QuestTaskCopy.Unavailable) {
                                "Video mission unavailable until the video catalog is ready"
                            } else {
                                "Quest reward: one sanctuary piece and one five minute play break"
                            }
                        },
                    color = if (quest.godModeEnabled || quest.isComplete) PlaySunshine.copy(alpha = 0.28f) else PlayTeal.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // P2 fix: gift pop now keyed to isComplete so it fires once on completion
                        val questCompleteReduce = LocalAnimationsDisabled.current
                        val giftScaleState = remember(quest.isComplete) { androidx.compose.animation.core.Animatable(if (quest.isComplete && !questCompleteReduce) 0.92f else 1f) }
                        LaunchedEffect(quest.isComplete) {
                            if (!questCompleteReduce && quest.isComplete) {
                                giftScaleState.snapTo(0.92f)
                                giftScaleState.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f))
                            } else giftScaleState.snapTo(1f)
                        }
                        Box(Modifier.graphicsLayer(scaleX = giftScaleState.value, scaleY = giftScaleState.value)) {
                            Icon(
                                Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = if (quest.godModeEnabled || quest.isComplete) PlayInkDark else PlayTeal,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                quest.godModeEnabled -> stringResource(R.string.home_quest_reward_parent)
                                quest.sanctuaryComplete -> stringResource(R.string.home_quest_reward_sanctuary_complete)
                                quest.isComplete -> stringResource(R.string.home_quest_reward_earned)
                                quest.task == QuestTaskCopy.Unavailable -> stringResource(R.string.home_quest_reward_unavailable)
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quest.targets.forEach { target ->
                            val targetDone = target.isCompleted
                            val displayTitle = target.title
                            val targetCd = if (target.type == QuestTargetType.ARENA) {
                                if (targetDone) "Quiz target done: $displayTitle" else "Quiz target: $displayTitle"
                            } else if (targetDone) {
                                "Quest target done: $displayTitle · ${target.durationLabel}"
                            } else {
                                "Quest target: ${target.displaySubject}: $displayTitle · ${target.durationLabel}"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.72f))
                                    .clickable(role = Role.Button, onClick = { onQuestTargetClick(target) })
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            displayTitle,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PlayInk,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        if (target.isReadyOffline && !targetDone) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = PlaySuccess.copy(alpha = 0.15f),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        Icons.Default.Pets,
                                                        contentDescription = "Available offline",
                                                        tint = SuccessGreenText,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        "Offline",
                                                        color = SuccessGreenText,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (target.type == QuestTargetType.ARENA) PlaySunshine.copy(alpha = 0.2f) else PlayTeal.copy(alpha = 0.12f),
                                        ) {
                                            Text(
                                                if (target.type == QuestTargetType.ARENA) "Quiz" else target.durationLabel,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                color = if (target.type == QuestTargetType.ARENA) PlayInkDark else PlayTeal,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
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
