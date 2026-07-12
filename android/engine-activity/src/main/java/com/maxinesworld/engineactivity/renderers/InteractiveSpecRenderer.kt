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

@Composable
fun InteractiveSpecRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(-1) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var revealed by remember { mutableStateOf(setOf<Int>()) }

    val labels = step.options.ifEmpty { listOf("Part A", "Part B", "Part C", "Part D") }
    val target = if (step.correctIndex in labels.indices) step.correctIndex else 0
    val positions = listOf(Alignment.TopCenter, Alignment.BottomCenter, Alignment.CenterStart, Alignment.CenterEnd)

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(step.question.ifEmpty { "Tap the correct label" }, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Diagram: ${step.question}" })

        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(16.dp))
            .background(SkyBlue.copy(alpha = 0.06f)).border(2.dp, VillageTeal.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {

            Box(Modifier.size(100.dp).clip(CircleShape).background(VillageTeal.copy(alpha = 0.08f))
                .border(2.dp, VillageTeal.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
                Text(step.narrationText.take(16).ifEmpty { "Diagram" }, style = MaterialTheme.typography.labelMedium, color = VillageTeal)
            }

            labels.forEachIndexed { i, label ->
                val bg by animateColorAsState(when {
                    result == true && selected == i -> SuccessGreen
                    result == false && selected == i -> ErrorRed
                    selected == i -> SunshineGold
                    i in revealed -> VillageTeal.copy(alpha = 0.15f)
                    else -> VillageTeal.copy(alpha = 0.7f)
                }, label = "spec$i")
                Box(Modifier.align(positions.getOrElse(i) { Alignment.Center }).padding(6.dp)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                    .clickable(enabled = result == null) {
                        selected = i; revealed = revealed + i; attempts++
                        val ok = i == target; result = ok
                        onResult(ActivityResult(step.id, ok, attempts, 0, System.currentTimeMillis() - startTime))
                    }.semantics { contentDescription = "Label $label${if (i in revealed) " — revealed" else ""}" },
                    contentAlignment = Alignment.Center) {
                    Text(if (result == true && selected == i) "✓" else label.take(12),
                        color = if (result == true && selected == i) White else Ink,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            labels.forEachIndexed { i, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.semantics { contentDescription = "$label" })
                    if (i in revealed) Text("✓ explored", style = MaterialTheme.typography.labelSmall, color = VillageTeal)
                }
            }
        }

        if (result != null) Text(
            if (result == true) step.feedback?.correct ?: "Correct! 🎉" else step.feedback?.incorrect ?: "Not quite!",
            color = if (result == true) SuccessGreen else ErrorRed, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { contentDescription = if (result == true) "Correct" else "Incorrect" })
    }
}
