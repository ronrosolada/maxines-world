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
import com.maxinesworld.engineactivity.LocalLessonUiLanguage
import com.maxinesworld.engineactivity.lessonUiStrings

@Composable
fun SortAndClassifyRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = lessonUiStrings(LocalLessonUiLanguage.current)
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableIntStateOf(-1) }
    var classified by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var submitted by remember { mutableStateOf(false) }
    var allCorrect by remember { mutableStateOf(false) }

    val options = step.options
    // Prefer explicit 2-bin classify (Fits / Does not fit + items).
    // correctIndex = number of items that belong to category 0 (prefix of items list).
    val categories: List<String>
    val items: List<String>
    val correctMapping: Map<Int, Int>
    if (options.size >= 3) {
        categories = options.take(2)
        items = options.drop(2)
        val cat0Count = if (step.correctIndex in 0..items.size) step.correctIndex else items.size / 2
        correctMapping = items.indices.associateWith { i -> if (i < cat0Count) 0 else 1 }
    } else {
        val catCount = maxOf(1, options.size / 2)
        categories = options.take(catCount)
        items = options.drop(catCount).ifEmpty { options }
        correctMapping = items.indices.associateWith { it % maxOf(1, categories.size) }
    }
    val prompt = step.question.ifBlank { step.narrationText }

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(prompt, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Sort: $prompt" })

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
                if (submitted) { classified = mutableMapOf(); submitted = false; allCorrect = false; attempts++ }
                else if (classified.size == items.size) {
                    attempts++; allCorrect = classified.all { (it, c) -> c == correctMapping[it] }; submitted = true
                    if (allCorrect) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                }
            }.semantics { contentDescription = if (submitted && !allCorrect) ui.tryAgain else ui.submit },
            contentAlignment = Alignment.Center) {
            Text(if (submitted) { if (allCorrect) ui.greatJob else ui.tryAgain } else ui.submit,
                color = White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(14.dp))
        }
    }
}
