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
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineactivity.LocalLessonUiLanguage
import com.maxinesworld.engineactivity.lessonUiStrings

/**
 * Hotspot image: tappable regions overlaid on a placeholder image area.
 * - correctIndex in range: single-target mode (must tap that region)
 * - correctIndex == -1: visit-all mode (tap every region to complete)
 */
@Composable
fun HotspotImageRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = lessonUiStrings(LocalLessonUiLanguage.current)
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var visited by remember { mutableStateOf(setOf<Int>()) }
    var lastTapped by remember { mutableIntStateOf(-1) }
    var completed by remember { mutableStateOf(false) }
    var lastWrong by remember { mutableStateOf(false) }

    val hotspots = step.options.ifEmpty { listOf("1", "2", "3", "4") }
    val visitAll = step.correctIndex !in hotspots.indices
    val targetIndex = if (visitAll) -1 else step.correctIndex
    val hotspotPositions = listOf(
        Alignment.TopStart, Alignment.TopEnd,
        Alignment.BottomStart, Alignment.BottomEnd, Alignment.Center
    )
    val prompt = step.question.ifBlank { step.narrationText }.ifBlank { ui.tapRegion }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = "Hotspot: $prompt" }
            )

            // Prefer emoji scene from content; keep tappable regions overlay style
            if (step.imageAssets.any { !it.startsWith("asset:") }) {
                KidSceneBanner(imageAssets = step.imageAssets)
            }

            // Image placeholder with hotspot overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StoryPurple.copy(alpha = 0.08f))
                    .border(2.dp, VillageTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.imageAssets.firstOrNull { !it.startsWith("asset:") } ?: "🖼️",
                    style = MaterialTheme.typography.displayMedium
                )
            hotspots.forEachIndexed { index, label ->
                val isVisited = index in visited
                val isLast = lastTapped == index
                val bgColor by animateColorAsState(
                    targetValue = when {
                        completed && isVisited -> SuccessGreen
                        lastWrong && isLast -> ErrorRed
                        isVisited -> SuccessGreen.copy(alpha = 0.7f)
                        isLast -> SunshineGold
                        else -> VillageTeal.copy(alpha = 0.6f)
                    },
                    label = "hotspot$index"
                )

                Box(
                    modifier = Modifier
                        .align(hotspotPositions.getOrElse(index) { Alignment.Center })
                        .padding(12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(enabled = !completed) {
                            lastTapped = index
                            attempts++
                            if (visitAll) {
                                lastWrong = false
                                val next = visited + index
                                visited = next
                                if (next.size >= hotspots.size) {
                                    completed = true
                                    onResult(
                                        ActivityResult(
                                            activityId = step.id,
                                            correct = true,
                                            attempts = attempts,
                                            hintsUsed = 0,
                                            responseTimeMs = System.currentTimeMillis() - startTime
                                        )
                                    )
                                }
                            } else {
                                val correct = index == targetIndex
                                lastWrong = !correct
                                if (correct) {
                                    visited = setOf(index)
                                    completed = true
                                    onResult(
                                        ActivityResult(
                                            activityId = step.id,
                                            correct = true,
                                            attempts = attempts,
                                            hintsUsed = 0,
                                            responseTimeMs = System.currentTimeMillis() - startTime
                                        )
                                    )
                                }
                            }
                        }
                        .semantics {
                            contentDescription = "Hotspot $label" +
                                if (isVisited) " — visited" else ""
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            hotspots.forEachIndexed { index, label ->
                Text(
                    text = "${index + 1}. $label",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics { contentDescription = "Region ${index + 1}: $label" }
                )
            }
        }

        if (visitAll && !completed) {
            Text(
                text = "${ui.matchedProgress}: ${visited.size} / ${hotspots.size}",
                style = MaterialTheme.typography.labelMedium,
                color = VillageTeal
            )
        }

        if (completed || lastWrong) {
            Text(
                text = when {
                    completed -> step.feedback?.correct ?: ui.greatJob
                    else -> step.feedback?.incorrect ?: ui.tryAgain
                },
                color = if (completed) SuccessGreen else ErrorRed,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
