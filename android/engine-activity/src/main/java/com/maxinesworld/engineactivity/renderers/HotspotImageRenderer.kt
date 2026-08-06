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
import androidx.compose.ui.unit.Dp
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

/** Keeps the badge in the cell's top-start corner, away from centered labels. */
internal fun hotspotBadgeOffset(
    index: Int,
    columns: Int,
    boardPadding: Dp,
    cellWidth: Dp,
    cellHeight: Dp,
): Pair<Dp, Dp> {
    val safeColumns = columns.coerceAtLeast(1)
    val safeIndex = index.coerceAtLeast(0)
    return (
        boardPadding + cellWidth * (safeIndex % safeColumns) + 8.dp
    ) to (
        boardPadding + cellHeight * (safeIndex / safeColumns) + 8.dp
    )
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

    fun handleHotspotTap(index: Int) {
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
            val correct = index == targetIndex
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

        // The authored SVG is optional; the hotspot controls remain usable when
        // the asset is absent or unavailable.
        LessonVisual(step)
        // Give the learner a static concept model before asking them to inspect examples.
        // It is decorative/supportive only: all answer information remains in text.
        LessonConceptVisual(step, motionAllowed = false)
        // Responsive example board with hotspot controls. The content pack's
        // example strings are the accessible/text interaction layer, so never
        // leave the activity without usable controls if an image is unavailable.
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
                hotspots.chunked(columns).forEachIndexed { rowIndex, rowHotspots ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowHotspots.forEachIndexed { columnIndex, label ->
                            val index = rowIndex * columns + columnIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Cream.copy(alpha = 0.82f))
                                    .border(1.dp, VillageTeal.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = result == null && (!allTargetsRequired || index !in hotspotProgress.visited)) {
                                        handleHotspotTap(index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 56.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                                )
                            }
                        }
                        repeat(columns - rowHotspots.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            hotspots.forEachIndexed { index, label ->
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

                val (badgeX, badgeY) = hotspotBadgeOffset(
                    index = index,
                    columns = columns,
                    boardPadding = boardPadding,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                )
                Box(
                    modifier = Modifier
                        .offset(
                            x = badgeX,
                            y = badgeY,
                        )
                        .size(hotspotSize)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(enabled = result == null && (!allTargetsRequired || !isVisited)) {
                            handleHotspotTap(index)
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
                        color = when {
                            result == true && isTapped -> OnSuccess
                            result == false && isTapped -> OnError
                            isVisited -> OnGold
                            else -> White
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

    }
}
