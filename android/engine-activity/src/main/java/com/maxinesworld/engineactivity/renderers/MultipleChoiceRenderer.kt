package com.maxinesworld.engineactivity.renderers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.components.AnswerCardState
import com.maxinesworld.coredesignsystem.components.MaxinesAnswerCard
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

/**
 * Multiple-choice question with correct/incorrect feedback and retry support.
 * Minimum 48dp touch targets, TalkBack content descriptions, reduced-motion aware.
 */
@Composable
fun MultipleChoiceRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var feedbackState by remember { mutableStateOf<Boolean?>(null) } // null=no feedback, true=correct, false=incorrect
    var submitted by remember { mutableStateOf(false) }

    val options = step.options.ifEmpty { listOf("A", "B", "C", "D") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = step.question,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Question: ${step.question}" }
        )

        options.forEachIndexed { index, option ->
            val cardState = when {
                submitted && index == step.correctIndex -> AnswerCardState.CORRECT
                submitted && index == selectedIndex && index != step.correctIndex -> AnswerCardState.INCORRECT
                index == selectedIndex -> AnswerCardState.SELECTED
                else -> AnswerCardState.IDLE
            }
            MaxinesAnswerCard(
                state = cardState,
                onClick = {
                    if (!submitted) selectedIndex = index
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 56.dp)
                    .semantics {
                        contentDescription = "Option ${index + 1}: $option" +
                            if (submitted && index == step.correctIndex) " — Correct" else ""
                    }
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Feedback text
        if (submitted && feedbackState != null) {
            Text(
                text = if (feedbackState == true)
                    step.feedback?.correct ?: "Great job! 🎉"
                else
                    step.feedback?.incorrect ?: "Let's try again! 💪",
                style = MaterialTheme.typography.bodyMedium,
                color = if (feedbackState == true) SuccessGreen else ErrorRed,
                modifier = Modifier.semantics {
                    contentDescription = if (feedbackState == true) "Correct answer" else "Incorrect, try again"
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hint button
            MaxinesPrimaryButton(
                onClick = {
                    onHint()
                    attempts++
                },
                text = "Hint",
                containerColor = SunshineGold,
                enabled = !submitted,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp)
                    .semantics { contentDescription = "Get a hint" }
            )

            // Submit / Retry button
            MaxinesPrimaryButton(
                onClick = {
                    if (submitted) {
                        // Retry: reset state
                        submitted = false
                        selectedIndex = -1
                        feedbackState = null
                    } else if (selectedIndex >= 0) {
                        attempts++
                        val correct = selectedIndex == step.correctIndex
                        feedbackState = correct
                        submitted = true
                        if (correct) {
                            onResult(
                                ActivityResult(
                                    activityId = step.id,
                                    correct = true,
                                    attempts = attempts,
                                    hintsUsed = 0,
                                    responseTimeMs = System.currentTimeMillis() - startTime
                                )
                            )
                        }
                    }
                },
                text = if (submitted && feedbackState == false) "Retry" else "Submit",
                containerColor = if (submitted && feedbackState == false) Coral else VillageTeal,
                enabled = selectedIndex >= 0 || (submitted && feedbackState == false),
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp)
                    .semantics {
                        contentDescription = if (submitted && feedbackState == false) "Retry" else "Submit answer"
                    }
            )
        }
    }
}
