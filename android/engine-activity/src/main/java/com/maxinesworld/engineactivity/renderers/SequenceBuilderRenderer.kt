package com.maxinesworld.engineactivity.renderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

internal fun sequenceActionEnabled(orderedCount: Int, itemCount: Int, submitted: Boolean): Boolean =
    submitted || orderedCount == itemCount

internal fun sequenceActionLabel(
    orderedCount: Int,
    itemCount: Int,
    submitted: Boolean,
    isCorrect: Boolean,
    attempts: Int,
): String = when {
    submitted && isCorrect -> "Great job!"
    submitted && attempts >= 3 -> "Keep going →"
    submitted -> "Try Again"
    orderedCount < itemCount -> "Place all cards ($orderedCount/$itemCount)"
    else -> "Submit"
}

internal fun sequenceActionDescription(
    orderedCount: Int,
    itemCount: Int,
    submitted: Boolean,
    isCorrect: Boolean,
    attempts: Int,
): String = sequenceActionLabel(orderedCount, itemCount, submitted, isCorrect, attempts)

@Composable
fun SequenceBuilderRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var ordered by remember { mutableStateOf(listOf<Int>()) }
    var submitted by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    val items: List<String> = step.sequenceSteps.ifEmpty { step.options }
    // Present tiles in a stable shuffled order; the correct answer remains the
    // original index order. Seeded so retries and tests are deterministic.
    val displayOrder = remember(step.id) {
        items.indices.shuffled(java.util.Random(step.id.hashCode().toLong()))
    }
    val available = displayOrder.filter { it !in ordered }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LessonVisual(step)

        Text(step.question.ifEmpty { "Arrange in order:" }, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Sequence: ${step.question}" })

        Text("Available:", style = MaterialTheme.typography.labelLarge, color = VillageTeal)
        available.forEach { idx ->
            Row(Modifier.fillMaxWidth().sizeIn(minHeight = 44.dp).clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainer)
                .clickable(enabled = !submitted) { ordered = ordered + idx }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { contentDescription = "Available: ${items[idx]} — tap to add" },
                verticalAlignment = Alignment.CenterVertically) {
                Text("${idx + 1}. ${items[idx]}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Your order:", style = MaterialTheme.typography.labelLarge, color = OnGold)
        ordered.forEachIndexed { pos, idx ->
            val bg by animateColorAsState(when {
                submitted && isCorrect -> SuccessGreen.copy(alpha = 0.15f)
                submitted && !isCorrect && pos != idx -> ErrorRed.copy(alpha = 0.1f)
                else -> SunshineGold.copy(alpha = 0.08f)
            }, label = "ord$idx")
            Row(Modifier.fillMaxWidth().sizeIn(minHeight = 44.dp).clip(RoundedCornerShape(10.dp)).background(bg)
                .clickable(enabled = !submitted) { ordered = ordered.filter { it != idx } }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .semantics { contentDescription = "Position ${pos + 1}: ${items[idx]} — tap to remove" },
                verticalAlignment = Alignment.CenterVertically) {
                Text("${pos + 1}. ${items[idx]}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                if (!submitted) Text("✕", color = OnCoral, style = MaterialTheme.typography.labelSmall)
            }
        }

        ActivityHint(step = step, onHint = onHint)

        Spacer(Modifier.height(24.dp))
        val actionEnabled = sequenceActionEnabled(ordered.size, items.size, submitted)
        val actionLabel = sequenceActionLabel(ordered.size, items.size, submitted, isCorrect, attempts)
        val actionDescription = sequenceActionDescription(ordered.size, items.size, submitted, isCorrect, attempts)
        Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    submitted && isCorrect -> SuccessGreen
                    !actionEnabled -> SurfaceContainer
                    else -> VillageTeal
                }
            )
            .clickable(enabled = actionEnabled) {
                if (submitted && !isCorrect) {
                    // Third failure: advance anyway so the child is never trapped.
                    if (attempts >= 3) {
                        onResult(ActivityResult(step.id, false, attempts, 0, System.currentTimeMillis() - startTime))
                    } else {
                        ordered = emptyList(); submitted = false; isCorrect = false
                    }
                } else if (!submitted && ordered.size == items.size) {
                    attempts++; isCorrect = ordered == items.indices.toList(); submitted = true
                    if (isCorrect) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                }
            }.semantics {
                contentDescription = actionDescription
                if (!actionEnabled) disabled()
            },
            contentAlignment = Alignment.Center) {
            Text(
                actionLabel,
                color = when {
                    submitted && isCorrect -> OnSuccess
                    !actionEnabled -> Teal40
                    else -> White
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
