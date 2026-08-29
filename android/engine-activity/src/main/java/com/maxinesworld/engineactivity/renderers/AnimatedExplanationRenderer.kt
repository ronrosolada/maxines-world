package com.maxinesworld.engineactivity.renderers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import com.maxinesworld.engineactivity.ActivityResult

/**
 * VIEW_AND_ACKNOWLEDGE rule: shows instruction text with a Continue button.
 * Always returns correct=true, scored=false — this is an unscored intro/explanation step.
 */
@Composable
fun AnimatedExplanationRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
    onNarrationReplay: (String) -> Unit = {},
) {
    val startTime = remember { System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        LessonVisual(step)

        Text(
            text = narrationPhrase(step),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics {
                contentDescription = "Instruction: ${step.narrationText.ifEmpty { step.question }}"
            }
        )

        TextButton(onClick = { onNarrationReplay(narrationPhrase(step)) }) {
            Icon(Icons.Default.Replay, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Replay Phrase")
        }

        ActivityHint(step = step, onHint = onHint)

        Spacer(modifier = Modifier.height(24.dp))

        MaxinesPrimaryButton(
            onClick = {
                onResult(
                    ActivityResult(
                        activityId = step.id,
                        correct = true,
                        attempts = 1,
                        hintsUsed = 0,
                        responseTimeMs = System.currentTimeMillis() - startTime,
                        scored = false
                    )
                )
            },
            text = "Continue",
            containerColor = VillageTeal,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 56.dp)
                .semantics { contentDescription = "Continue to next activity" }
        )
    }
}

internal fun narrationPhrase(step: ActivityStep): String =
    step.narrationText.ifBlank { step.question }

internal fun hasReplayableNarration(step: ActivityStep): Boolean =
    step.narrationText.isNotBlank()
