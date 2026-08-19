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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

/** Friendly labels for the shared-category targets used by legacy match activities. */
internal fun matchingTargetLabel(index: Int, authoredLabel: String, sharedLabel: Boolean): String =
    if (sharedLabel) "Milo says yes ${index + 1}" else authoredLabel

internal const val MATCHING_MISMATCH_MESSAGE =
    "Not this pair yet. Compare the two ideas and try again."

internal fun matchingPairIsCorrect(
    right: List<String>,
    selectedLeft: Int,
    selectedRight: Int,
): Boolean = selectedLeft in right.indices &&
    selectedRight in right.indices &&
    right[selectedRight] == right[selectedLeft]

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
    var matchedLeft by remember { mutableStateOf(setOf<Int>()) }
    var matchedRight by remember { mutableStateOf(setOf<Int>()) }
    var mismatch by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val animationsDisabled = LocalAnimationsDisabled.current

    val left: List<String> = if (step.matchPairs.isNotEmpty()) step.matchPairs.map { it.left }
        else step.options.filterIndexed { i, _ -> i % 2 == 0 }
    val right: List<String> = if (step.matchPairs.isNotEmpty()) step.matchPairs.map { it.right }
        else step.options.filterIndexed { i, _ -> i % 2 == 1 }
    val n = minOf(left.size, right.size)
    val sharedRightLabel = right.take(n).distinct().size == 1
    val matchingHint = if (sharedRightLabel) {
        "Tap an example, then tap a Milo says yes box"
    } else {
        "Tap an example, then tap its match"
    }
    // Positional matching checks index alignment (right[i] == right[selectedLeft]).
    // Content authors must keep left/right lists aligned; identical right labels act as a shared category.

    LaunchedEffect(mismatch) {
        if (mismatch != null) { kotlinx.coroutines.delay(800); mismatch = null; selectedLeft = -1 }
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LessonVisual(step)

        Text(step.question.ifEmpty { "Match the pairs!" }, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Match pairs: ${step.question}" })

        // Interaction hint — tap a left card, then tap its match on the right.
        Text(
            matchingHint,
            style = MaterialTheme.typography.labelLarge,
            color = VillageTeal,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { contentDescription = "Hint: $matchingHint" }
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                left.forEachIndexed { i, label -> if (i < n) {
                    val bg by animateColorAsState(when {
                        i in matchedLeft -> SuccessGreen.copy(alpha = 0.2f)
                        mismatch?.first == i -> ErrorRed.copy(alpha = 0.2f)
                        selectedLeft == i -> VillageTeal.copy(alpha = 0.15f)
                        else -> SurfaceContainer
                    }, animationSpec = if (animationsDisabled) snap() else tween(180), label = "L$i")
                    Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                        .clickable(enabled = i !in matchedLeft && mismatch == null, role = Role.Button) { selectedLeft = if (selectedLeft == i) -1 else i }
                        .padding(10.dp).semantics { contentDescription = "Left: $label${if (i in matchedLeft) " — matched" else ""}" },
                        contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                } }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                right.forEachIndexed { i, label -> if (i < n) {
                    val displayLabel = matchingTargetLabel(i, label, sharedRightLabel)
                    val bg by animateColorAsState(when {
                        i in matchedRight -> SuccessGreen.copy(alpha = 0.2f)
                        mismatch?.second == i -> ErrorRed.copy(alpha = 0.2f)
                        else -> SurfaceContainer
                    }, animationSpec = if (animationsDisabled) snap() else tween(180), label = "R$i")
                    Box(Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                        .clickable(enabled = i !in matchedRight && selectedLeft >= 0 && mismatch == null, role = Role.Button) {
                            attempts++
                            // Matching is positional: content authors write
                            // pairs[i] = (left[i], right[i]) and both columns
                            // render in authored order, so the tapped right
                            // item is the correct partner iff it equals the
                            // right item at the selected left's index.
                            // All-identical right labels ("shows the skill")
                            // therefore accept any pairing — a classification
                            // rendered as matching. (RendererContractTest
                            // pins this semantic.)
                            val isMatch = matchingPairIsCorrect(right, selectedLeft, i)
                            if (isMatch) {
                                matchedLeft = matchedLeft + selectedLeft
                                matchedRight = matchedRight + i
                                selectedLeft = -1
                                if (matchedLeft.size == n) onResult(ActivityResult(step.id, true, attempts, 0, System.currentTimeMillis() - startTime))
                            } else {
                                mismatch = selectedLeft to i
                                if (attempts >= 6) {
                                    onResult(ActivityResult(step.id, false, attempts, 0, System.currentTimeMillis() - startTime))
                                }
                            }
                        }.padding(10.dp).semantics { contentDescription = "Right: $displayLabel${if (i in matchedRight) " — matched" else ""}" },
                        contentAlignment = Alignment.Center) {
                        Text(displayLabel, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                } }
            }
        }

        mismatch?.let {
            Text(
                MATCHING_MISMATCH_MESSAGE,
                color = OnCoral,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.semantics {
                    contentDescription = MATCHING_MISMATCH_MESSAGE
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }

        ActivityHint(step = step, onHint = onHint)

        Text("Matched: ${matchedLeft.size} / $n", style = MaterialTheme.typography.labelMedium, color = VillageTeal,
            modifier = Modifier.semantics { contentDescription = "${matchedLeft.size} of $n matched" })
        Spacer(Modifier.height(24.dp))
    }
}
