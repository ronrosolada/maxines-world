package com.maxinesworld.engineactivity.renderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

@Composable
fun SortAndClassifyRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
    onAdvance: () -> Unit = {},
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableIntStateOf(-1) }
    var classified by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var submitted by remember { mutableStateOf(false) }
    var allCorrect by remember { mutableStateOf(false) }
    val animationsDisabled = LocalAnimationsDisabled.current

    // Typed fields are authoritative. The positional fallback below exists only
    // for legacy ActivitySteps that predate the typed model.
    val categories = step.sortCategories.ifEmpty {
        val n = maxOf(1, step.options.size / 2)
        step.options.take(n)
    }
    val items: List<String> = if (step.sortItems.isNotEmpty()) {
        step.sortItems.map { it.label }
    } else {
        step.options.drop(maxOf(1, step.options.size / 2)).ifEmpty { step.options }
    }
    val correctMapping: Map<Int, Int> = if (step.sortItems.isNotEmpty()) {
        step.sortItems.indices.associateWith { step.sortItems[it].categoryIndex }
    } else {
        items.indices.associateWith { it % maxOf(1, categories.size) }
    }
    val placedCount = classified.size
    val submitEnabled = submitted || placedCount == items.size
    val progressCopy = when {
        placedCount == 0 -> "Place all ${items.size} cards first"
        placedCount == items.size -> "All ${items.size} cards placed. Ready to check!"
        else -> {
            val remaining = items.size - placedCount
            "Place $remaining more ${if (remaining == 1) "card" else "cards"} to check"
        }
    }
    val progressDescription =
        "Sort progress: $placedCount of ${items.size} cards placed. $progressCopy"
    // Buckets glow while a card is selected — the only visual cue that the
    // interaction is tap-card-then-tap-box (#28). Keep it subtle but present.
    val bucketHighlight by animateColorAsState(
        targetValue = if (selectedItem >= 0 && !submitted) VillageTeal.copy(alpha = 0.18f)
        else SubjectColors.Science.surface,
        animationSpec = if (animationsDisabled) snap() else tween(180),
        label = "bucketGlow",
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LessonVisual(step)

        Text(step.question, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Sort: ${step.question}" })

        // Interaction hint — the tap-tap model is not discoverable by itself
        // and the natural drag gesture is not supported (#28).
        Text(
            "Tap a card, then tap a box",
            style = MaterialTheme.typography.labelLarge,
            color = VillageTeal,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { contentDescription = "Hint: tap a card, then tap a box" }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEachIndexed { ci, label ->
                val n = classified.count { it.value == ci }
                val enabled = selectedItem >= 0 && !submitted
                Box(Modifier.weight(1f).sizeIn(minHeight = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) bucketHighlight else SubjectColors.Science.surface)
                    .clickable(enabled = enabled, role = Role.Button) { classified = classified.toMutableMap().apply { put(selectedItem, ci) }; selectedItem = -1 }
                    .semantics { contentDescription = "Category $label ($n items)" },
                    contentAlignment = Alignment.Center) {
                    Text("$label ($n)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(8.dp))
                }
            }
        }

        items.forEachIndexed { ii, label ->
            val placed = classified.containsKey(ii)
            val sel = selectedItem == ii
            val bg = when {
                submitted && placed -> if (classified[ii] == correctMapping[ii]) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                sel -> VillageTeal.copy(alpha = 0.2f)
                placed -> SubjectColors.Science.surface
                else -> SurfaceContainer
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg)
                .clickable(enabled = !submitted && !placed, role = Role.Button) { selectedItem = ii }
                .padding(12.dp).sizeIn(minHeight = 48.dp)
                .semantics { contentDescription = if (placed) "$label — sorted" else "Item: $label" },
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (placed && !submitted) Text("✓", color = OnLeafGreen)
            }
        }

        ActivityHint(step = step, onHint = onHint)

        if (!submitted) {
            Text(
                progressCopy,
                style = MaterialTheme.typography.labelMedium,
                color = if (submitEnabled) VillageTeal else Teal40,
                modifier = Modifier.semantics { contentDescription = progressDescription },
            )
            if (items.size > 4 && placedCount < items.size) {
                Text(
                    "Scroll down to place all cards and check your work",
                    style = MaterialTheme.typography.labelSmall,
                    color = Teal40,
                    modifier = Modifier.padding(top = 4.dp)
                        .semantics { contentDescription = "Scroll down to place all cards and check your work" }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        // Success state: the celebration banner IS the next-step button.
        // Tapping it advances the lesson — a full-width green button that
        // does nothing was the #29 complaint; it must not happen again.
        Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    submitted && allCorrect -> SuccessGreen
                    !submitEnabled -> SurfaceContainer
                    else -> VillageTeal
                }
            )
            .clickable(enabled = submitEnabled, role = Role.Button) {
                when {
                    submitted && allCorrect -> onAdvance()
                    submitted && !allCorrect -> {
                        // Third failure: advance anyway so the child is never trapped.
                        if (attempts >= 3) {
                            onResult(ActivityResult(step.id, false, attempts, 0, System.currentTimeMillis() - startTime))
                        } else {
                            classified = retainCorrectSortPlacements(classified, correctMapping)
                            submitted = false
                        }
                    }
                    !submitted && classified.size == items.size -> {
                        attempts++
                        allCorrect = classified.all { (i, c) -> c == correctMapping[i] }
                        submitted = true
                        if (allCorrect) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                    }
                }
            }.semantics { contentDescription = if (submitted && allCorrect) "Great job, next step" else if (submitted && !allCorrect) "Try again" else "Submit" },
            contentAlignment = Alignment.Center) {
            Text(
                when {
                    submitted && allCorrect -> "Great job! Next →"
                    submitted && attempts >= 3 -> "Keep going →"
                    submitted -> "Try Again"
                    else -> "Submit"
                },
                color = when {
                    submitted && allCorrect -> OnSuccess
                    !submitEnabled -> Teal40
                    else -> White
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

/** Keep correct placements visible when a learner retries an imperfect sort. */
internal fun retainCorrectSortPlacements(
    classified: Map<Int, Int>,
    correctMapping: Map<Int, Int>,
): MutableMap<Int, Int> =
    classified.filterTo(mutableMapOf()) { (itemIndex, categoryIndex) ->
        correctMapping[itemIndex] == categoryIndex
    }
