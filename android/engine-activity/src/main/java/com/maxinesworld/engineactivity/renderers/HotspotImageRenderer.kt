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

/**
 * Hotspot image: tappable regions overlaid on a placeholder image area.
 * Each region highlights on tap, shows success/correct feedback.
 * Options describe hotspot labels; correctIndex identifies the target region.
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

    val hotspots = step.options.ifEmpty { listOf("Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right") }
    val targetIndex = if (step.correctIndex in hotspots.indices) step.correctIndex else 0
    val hotspotPositions = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)

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
                text = if (step.imageAssets.isNotEmpty()) step.imageAssets.first() else "🖼️",
                style = MaterialTheme.typography.displayMedium
            )

            // Overlay hotspots in 4 corners
            hotspots.forEachIndexed { index, label ->
                val isTarget = index == targetIndex
                val isTapped = tappedRegion == index
                val bgColor by animateColorAsState(
                    targetValue = when {
                        result == true && isTapped -> SuccessGreen
                        result == false && isTapped -> ErrorRed
                        isTapped -> SunshineGold
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
                        .clickable(enabled = result == null) {
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
                        .semantics {
                            contentDescription = "Hotspot $label" +
                                if (result == true && isTapped) " — Correct!" else ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (result == true && isTapped) "✓" else "${index + 1}",
                        color = White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Labels row
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
