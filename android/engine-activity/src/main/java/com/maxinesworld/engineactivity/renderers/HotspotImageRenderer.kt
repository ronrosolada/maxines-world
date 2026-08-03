package com.maxinesworld.engineactivity.renderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

internal data class HotspotProgress(
    val visited: Set<Int> = emptySet(),
    val attempts: Int = 0,
    val completed: Boolean = false
)

/** Records one unique target visit for an ALL_TARGETS_VISITED hotspot activity. */
internal fun recordHotspotTargetTap(
    progress: HotspotProgress,
    index: Int,
    targetCount: Int
): HotspotProgress {
    if (progress.completed || index < 0 || index in progress.visited) return progress
    val visited = progress.visited + index
    return progress.copy(
        visited = visited,
        attempts = progress.attempts + 1,
        completed = visited.size >= targetCount.coerceAtLeast(1)
    )
}

internal fun hotspotGridColumns(hotspotCount: Int): Int = when {
    hotspotCount <= 1 -> 1
    hotspotCount <= 4 -> 2
    hotspotCount <= 9 -> 3
    else -> 4
}

/**
 * Hotspot image: tappable regions overlaid on a placeholder image area.
 * Single-answer activities finish on one correct tap. ALL_TARGETS_VISITED
 * activities remain interactive until every unique target has been visited.
 */
@Composable
fun HotspotImageRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var tappedRegion by remember { mutableIntStateOf(-1) }
    var result by remember { mutableStateOf<Boolean?>(null) } // null = unanswered
    var hotspotProgress by remember { mutableStateOf(HotspotProgress()) }

    val hotspots = step.hotspotExamples
        .ifEmpty { step.options }
        .ifEmpty { listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right") }
    val allTargetsRequired = step.completionRule == "ALL_TARGETS_VISITED"
    val targetCount = step.completionTargetCount
        .takeIf { it > 0 }
        ?.coerceAtMost(hotspots.size)
        ?: hotspots.size
    val targetIndex = if (step.correctIndex in hotspots.indices) step.correctIndex else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = step.question.ifEmpty { "Tap the correct region" },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Hotspot: ${step.question}" }
        )

        // Responsive example board with hotspot overlay. The content pack's
        // example strings are the accessible/text fallback for the optional
        // artwork asset, so never leave the board visually empty.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StoryPurple.copy(alpha = 0.08f))
                .border(2.dp, VillageTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.TopStart
        ) {
            val columns = hotspotGridColumns(hotspots.size)
            val rows = (hotspots.size + columns - 1) / columns
            val boardPadding = 16.dp
            val cellWidth = (maxWidth - boardPadding * 2) / columns
            val cellHeight = (maxHeight - boardPadding * 2) / rows
            val hotspotSize = 48.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(boardPadding),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                hotspots.chunked(columns).forEach { rowHotspots ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowHotspots.forEach { label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Cream.copy(alpha = 0.82f))
                                    .border(1.dp, VillageTeal.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(40.dp, 12.dp, 12.dp, 12.dp)
                                )
                            }
                        }
                        repeat(columns - rowHotspots.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            hotspots.forEachIndexed { index, label ->
                val isTarget = index == targetIndex
                val isVisited = index in hotspotProgress.visited
                val isTapped = if (allTargetsRequired) isVisited else tappedRegion == index
                val bgColor by animateColorAsState(
                    targetValue = when {
                        result == true && isTapped -> SuccessGreen
                        result == false && isTapped -> ErrorRed
                        isVisited -> SunshineGold
                        else -> VillageTeal.copy(alpha = 0.6f)
                    },
                    label = "hotspot$index"
                )

                Box(
                    modifier = Modifier
                        .offset(
                            x = boardPadding + cellWidth * (index % columns).toFloat() + (cellWidth - hotspotSize) / 2,
                            y = boardPadding + cellHeight * (index / columns).toFloat() + 8.dp
                        )
                        .size(hotspotSize)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(enabled = result == null && (!allTargetsRequired || !isVisited)) {
                            if (allTargetsRequired) {
                                val nextProgress = recordHotspotTargetTap(hotspotProgress, index, targetCount)
                                hotspotProgress = nextProgress
                                if (nextProgress.completed) {
                                    result = true
                                    onResult(
                                        ActivityResult(
                                            activityId = step.id,
                                            correct = true,
                                            attempts = nextProgress.attempts,
                                            hintsUsed = 0,
                                            responseTimeMs = System.currentTimeMillis() - startTime
                                        )
                                    )
                                }
                            } else {
                                tappedRegion = index
                                attempts++
                                val correct = isTarget
                                result = correct
                                onResult(
                                    ActivityResult(
                                        activityId = step.id,
                                        correct = correct,
                                        attempts = attempts,
                                        hintsUsed = 0,
                                        responseTimeMs = System.currentTimeMillis() - startTime
                                    )
                                )
                            }
                        }
                        .semantics {
                            contentDescription = "Hotspot $label" +
                                when {
                                    result == true && isTapped -> " — Correct!"
                                    isVisited -> " — Visited"
                                    else -> ""
                                }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isVisited) "✓" else "${index + 1}",
                        color = White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Labels grid; keep longer content usable when a lesson has more than four targets.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hotspots.chunked(2).forEachIndexed { rowIndex, rowHotspots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowHotspots.forEachIndexed { columnIndex, label ->
                        val index = rowIndex * 2 + columnIndex
                        Text(
                            text = "${index + 1}. $label",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Region ${index + 1}: $label" }
                        )
                    }
                    if (rowHotspots.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (result != null) {
            Text(
                text = if (result == true) step.feedback?.correct ?: "Great job! 🎉"
                else step.feedback?.incorrect ?: "Let's try again! 💪",
                color = if (result == true) SuccessGreen else ErrorRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics {
                    contentDescription = if (result == true) "Correct answer" else "Incorrect"
                }
            )
        }
    }
}
