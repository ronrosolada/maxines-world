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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

@Composable
fun MatchingPairsRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selectedLeft by remember { mutableIntStateOf(-1) }
    var matched by remember { mutableStateOf(setOf<Int>()) }
    var mismatch by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val all = step.options
    val half = maxOf(1, all.size / 2)
    val left = all.take(half)
    val right = all.drop(half).ifEmpty { all }
    val n = minOf(left.size, right.size)

    LaunchedEffect(mismatch) {
        if (mismatch != null) { kotlinx.coroutines.delay(800); mismatch = null; selectedLeft = -1 }
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(step.question.ifEmpty { "Match the pairs!" }, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Match pairs: ${step.question}" })

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                left.forEachIndexed { i, label -> if (i < n) {
                    val bg by animateColorAsState(when {
                        i in matched -> SuccessGreen.copy(alpha = 0.2f)
                        mismatch?.first == i -> ErrorRed.copy(alpha = 0.2f)
                        selectedLeft == i -> VillageTeal.copy(alpha = 0.15f)
                        else -> SurfaceContainer
                    }, label = "L$i")
                    Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                        .clickable(enabled = i !in matched && mismatch == null) { selectedLeft = if (selectedLeft == i) -1 else i }
                        .padding(10.dp).semantics { contentDescription = "Left: $label${if (i in matched) " — matched" else ""}" },
                        contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                } }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                right.forEachIndexed { i, label -> if (i < n) {
                    val bg by animateColorAsState(when {
                        i in matched -> SuccessGreen.copy(alpha = 0.2f)
                        mismatch?.second == i -> ErrorRed.copy(alpha = 0.2f)
                        else -> SurfaceContainer
                    }, label = "R$i")
                    Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                        .clickable(enabled = i !in matched && selectedLeft >= 0 && mismatch == null) {
                            attempts++
                            if (selectedLeft == i) { matched = matched + i; selectedLeft = -1
                                if (matched.size == n) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                            } else mismatch = selectedLeft to i
                        }.padding(10.dp).semantics { contentDescription = "Right: $label${if (i in matched) " — matched" else ""}" },
                        contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                } }
            }
        }

        Text("Matched: ${matched.size} / $n", style = MaterialTheme.typography.labelMedium, color = VillageTeal,
            modifier = Modifier.semantics { contentDescription = "${matched.size} of $n matched" })
        Spacer(Modifier.weight(1f))
    }
}
