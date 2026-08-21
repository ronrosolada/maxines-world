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


// ─── Subject grid + card (design.md §10) ─────────────────────────────

private fun subjectAccent(id: String): Color = SubjectAccent[id] ?: PlayTeal
private fun subjectPale(id: String): Color = SubjectPale[id] ?: PlayroomColors.FallbackSurface

@Composable
internal fun SubjectGrid(
    subjects: List<SubjectCardUi>,
    columns: Int,
    openingSubjectId: String?,
    firstFocusId: String?,
    firstFocusRequester: FocusRequester,
    onSubjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag(PlayroomHomeTestTags.SubjectGrid),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
    val completedVideos = subject.completedVideos
    val totalVideos = subject.totalVideos
    val videoProgress = if (completedVideos != null && totalVideos != null) {
        completedVideos.coerceIn(0, totalVideos.coerceAtLeast(0)) to totalVideos.coerceAtLeast(0)
    } else {
        null
    }
    val progressFraction = videoProgress?.let { (completed, total) ->
        if (total > 0) completed.toFloat() / total.toFloat() else 0f
    } ?: 0f
    val progressLabel = when {
        videoProgress == null -> stringResource(R.string.home_progress_unavailable)
        videoProgress.second == 0 -> stringResource(R.string.home_no_videos)
        videoProgress.first >= videoProgress.second -> stringResource(
            R.string.home_video_progress_complete,
            videoProgress.first,
            videoProgress.second,
        )
        videoProgress.first == 0 -> stringResource(
            R.string.home_video_progress_not_started,
            videoProgress.first,
            videoProgress.second,
        )
        else -> stringResource(
            R.string.home_video_progress,
            videoProgress.first,
            videoProgress.second,
        )
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
            .testTag(PlayroomHomeTestTags.subject(subject.id))
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
                onClickLabel = "Open ${subject.formalName}",
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

                // Keep the locked explanation in normal layout flow. It must
                // remain readable beside the lock icon instead of covering
                // the illustration with an overlaid chip.
                if (subject.availability == SubjectAvailability.Locked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PlayInkDark)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = PlayroomColors.LockedSurfaceText,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                stringResource(R.string.home_locked),
                                color = PlayroomColors.LockedSurfaceText,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                            )
                            subject.lockReason
                                ?.takeIf { it.isNotBlank() }
                                ?.let { reason ->
                                    Text(
                                        stringResource(R.string.home_locked_reason_label, reason),
                                        color = PlayroomColors.LockedSurfaceText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                    )
                                }
                        }
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
                            if (videoProgress != null && videoProgress.first > 0 && videoProgress.second > 0) {
                                Box(
                                    Modifier.fillMaxWidth(progressFraction).fillMaxHeight()
                                        .background(accent, RoundedCornerShape(99.dp)),
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (videoProgress != null &&
                                videoProgress.second > 0 &&
                                videoProgress.first >= videoProgress.second
                            ) {
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

        }
    }
}
