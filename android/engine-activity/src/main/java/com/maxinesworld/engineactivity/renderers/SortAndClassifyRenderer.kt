package com.maxinesworld.engineactivity.renderers

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

@Composable
fun SortAndClassifyRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableIntStateOf(-1) }
    var classified by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var submitted by remember { mutableStateOf(false) }
    var allCorrect by remember { mutableStateOf(false) }

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

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(step.question, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Sort: ${step.question}" })

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEachIndexed { ci, label ->
                val n = classified.count { it.value == ci }
                Box(Modifier.weight(1f).sizeIn(minHeight = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedItem >= 0 && !submitted) VillageTeal.copy(alpha = 0.12f) else SubjectColors.Science.surface)
                    .clickable(enabled = selectedItem >= 0 && !submitted) { classified = classified.toMutableMap().apply { put(selectedItem, ci) }; selectedItem = -1 }
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
                .clickable(enabled = !submitted && !placed) { selectedItem = ii }
                .padding(12.dp).sizeIn(minHeight = 48.dp)
                .semantics { contentDescription = if (placed) "$label — sorted" else "Item: $label" },
                verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (placed && !submitted) Text("✓", color = VillageTeal)
            }
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(if (submitted && allCorrect) SuccessGreen else VillageTeal)
            .clickable {
                if (submitted && !allCorrect) {
                    // Third failure: advance anyway so the child is never trapped.
                    if (attempts >= 3) {
                        onResult(ActivityResult(step.id, false, attempts, 0, System.currentTimeMillis() - startTime))
                    } else {
                        classified = mutableMapOf(); submitted = false
                    }
                } else if (!submitted && classified.size == items.size) {
                    attempts++
                    allCorrect = classified.all { (i, c) -> c == correctMapping[i] }
                    submitted = true
                    if (allCorrect) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                }
            }.semantics { contentDescription = if (submitted && !allCorrect) "Try again" else "Submit" },
            contentAlignment = Alignment.Center) {
            Text(
                if (submitted) {
                    when {
                        allCorrect -> "Great job! 🎉"
                        attempts >= 3 -> "Keep going →"
                        else -> "Try Again"
                    }
                } else "Submit",
                color = White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(14.dp))
        }
    }
}
