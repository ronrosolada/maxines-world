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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineactivity.LocalLessonUiLanguage
import com.maxinesworld.engineactivity.lessonUiStrings

@Composable
fun SequenceBuilderRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = lessonUiStrings(LocalLessonUiLanguage.current)
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var ordered by remember { mutableStateOf(listOf<Int>()) }
    var submitted by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    val items = step.options.ifEmpty { listOf("1", "2", "3", "4") }
    val available = items.indices.filter { it !in ordered }
    val prompt = step.question.ifBlank { step.narrationText }.ifBlank { ui.arrangeInOrder }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(prompt, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Sequence: $prompt" })

        Text(ui.available, style = MaterialTheme.typography.labelLarge, color = VillageTeal)
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

        Text(ui.yourOrder, style = MaterialTheme.typography.labelLarge, color = SunshineGold)
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
                if (!submitted) Text("✕", color = Coral, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(16.dp))
            .background(if (submitted && isCorrect) SuccessGreen else VillageTeal)
            .clickable {
                if (submitted) { ordered = emptyList(); submitted = false; isCorrect = false; attempts++ }
                else if (ordered.size == items.size) {
                    attempts++; isCorrect = ordered == items.indices.toList(); submitted = true
                    if (isCorrect) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                }
            }.semantics { contentDescription = if (submitted && !isCorrect) ui.tryAgain else ui.submit },
            contentAlignment = Alignment.Center) {
            Text(when {
                submitted && isCorrect -> ui.greatJob
                submitted -> ui.tryAgain
                ordered.size < items.size -> "${ui.selectAll} (${ordered.size}/${items.size})"
                else -> ui.submit
            }, color = White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(14.dp))
        }
    }
}
