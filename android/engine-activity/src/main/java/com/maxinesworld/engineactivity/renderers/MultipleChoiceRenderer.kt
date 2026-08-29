package com.maxinesworld.engineactivity.renderers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.childFacingCorrectFeedback
import com.maxinesworld.coremodel.childFacingIncorrectFeedback
import com.maxinesworld.coredesignsystem.components.AnswerCardState
import com.maxinesworld.coredesignsystem.components.MaxinesAnswerCard
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

/**
 * Multiple-choice question with correct/incorrect feedback and retry support.
 * Minimum 48dp touch targets, TalkBack content descriptions, and retry support.
 */
@Composable
fun MultipleChoiceRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
    onNarrationReplay: (String) -> Unit = {},
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var feedbackState by remember { mutableStateOf<Boolean?>(null) } // null=no feedback, true=correct, false=incorrect
    var submitted by remember { mutableStateOf(false) }

    val originalOptions = step.options
    if (originalOptions.isEmpty()) {
        Text(
            text = "This activity is unavailable.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(24.dp)
        )
        return
    }

    // Keep the content model's correctIndex tied to the original option list,
    // then present a stable per-lesson order so the first card is not always
    // the answer. Stable ordering avoids reshuffling during recomposition.
    val optionOrder = remember(step.id, originalOptions, step.correctIndex) {
        optionOrderFor(step.id, originalOptions.size, step.correctIndex)
    }
    val options = optionOrder.map(originalOptions::get)
    val displayedCorrectIndex = optionOrder.indexOf(step.correctIndex)
    val hasHint = step.hintText.isNotBlank()
    var hintVisible by remember(step.id) { mutableStateOf(false) }
    var hintsUsed by remember(step.id) { mutableIntStateOf(0) }
    var showNoSelectionHint by remember(step.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LessonVisual(step)

        Text(
            text = step.question,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Question: ${step.question}" }
        )

        if (hasReplayableNarration(step)) {
            TextButton(onClick = { onNarrationReplay(narrationPhrase(step)) }) {
                Icon(Icons.Default.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Replay Phrase")
            }
        }

        options.forEachIndexed { index, option ->
            val cardState = when {
                submitted && index == displayedCorrectIndex -> AnswerCardState.CORRECT
                submitted && index == selectedIndex && index != displayedCorrectIndex -> AnswerCardState.INCORRECT
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
                            if (submitted && index == displayedCorrectIndex) " — Correct" else ""
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
                    childFacingCorrectFeedback(step.feedback?.correct)
                else
                    childFacingIncorrectFeedback(step.feedback?.incorrect),
                style = MaterialTheme.typography.bodyMedium,
                color = if (feedbackState == true) SuccessGreenText else ReviewText,
                modifier = Modifier.semantics {
                    contentDescription = if (feedbackState == true) "Correct answer" else "Incorrect, try again"
                }
            )
        }

        if (shouldShowCorrection(submitted, feedbackState, attempts)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SunshineGold.copy(alpha = 0.16f),
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Let's look at this together",
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink,
                    )
                    options.getOrNull(displayedCorrectIndex)?.let { answer ->
                        Text(
                            "Correct answer: $answer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                        )
                    }
                    val guidance = step.hintText.ifBlank {
                        childFacingIncorrectFeedback(step.feedback?.incorrect)
                    }
                    if (guidance.isNotBlank()) {
                        Text(
                            guidance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        if (hintVisible) {
            Text(
                text = "Hint: ${step.hintText}",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
                modifier = Modifier.semantics { contentDescription = "Hint: ${step.hintText}" }
            )
        }

        if (showNoSelectionHint && !submitted && selectedIndex < 0) {
            Text(
                text = "Pick one answer to check",
                style = MaterialTheme.typography.bodyMedium,
                color = ReviewText,
                modifier = Modifier.semantics { contentDescription = "Pick one answer to check" }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasHint) {
                MaxinesPrimaryButton(
                    onClick = {
                        hintsUsed++
                        hintVisible = true
                        onHint()
                    },
                    text = "Hint",
                    containerColor = SunshineGold,
                    contentColor = OnGold,
                    enabled = !submitted || feedbackState == false,
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 48.dp)
                        .semantics { contentDescription = "Get a hint" }
                )
            }

            // Submit / Retry button
            MaxinesPrimaryButton(
                onClick = {
                    if (submitted) {
                        // Third failure: advance anyway so the child is never trapped.
                        if (feedbackState == false && attempts >= 3) {
                            onResult(multipleChoiceResult(step.id, false, attempts, hintsUsed,
                                System.currentTimeMillis() - startTime))
                        } else {
                            submitted = false
                            selectedIndex = -1
                            feedbackState = null
                            showNoSelectionHint = false
                        }
                    } else if (selectedIndex >= 0) {
                        attempts++
                        val correct = selectedIndex == displayedCorrectIndex
                        feedbackState = correct
                        submitted = true
                        showNoSelectionHint = false
                        if (correct) {
                            onResult(multipleChoiceResult(step.id, true, attempts, hintsUsed,
                                System.currentTimeMillis() - startTime))
                        }
                    } else {
                        showNoSelectionHint = true
                    }
                },
                text = when {
                    submitted && feedbackState == false && attempts >= 3 -> "Keep going →"
                    submitted && feedbackState == false -> "Retry"
                    else -> "Submit"
                },
                containerColor = if (submitted && feedbackState == false) Coral else VillageTeal,
                contentColor = if (submitted && feedbackState == false) OnCoral else White,
                enabled = true, // keep tappable so empty-tap can nudge the child (audit: silent no-op)
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

internal fun multipleChoiceResult(
    activityId: String,
    correct: Boolean,
    attempts: Int,
    hintsUsed: Int,
    responseTimeMs: Long,
): ActivityResult = ActivityResult(
    activityId = activityId,
    correct = correct,
    attempts = attempts,
    hintsUsed = hintsUsed,
    responseTimeMs = responseTimeMs,
)

internal fun shouldShowCorrection(
    submitted: Boolean,
    feedbackState: Boolean?,
    attempts: Int,
): Boolean = submitted && feedbackState == false && attempts >= 2

/**
 * Return a deterministic permutation for one lesson's option cards.
 * Invalid content remains non-crashing; the renderer will simply never mark
 * an option correct until the content is repaired.
 *
 * Public so the lesson player's assessment phase (feature-lesson-player)
 * presents its options under the same stable per-item order.
 */
fun optionOrderFor(stepId: String, optionCount: Int, correctIndex: Int): List<Int> {
    if (optionCount <= 1) return (0 until optionCount).toList()
    val order = (0 until optionCount).toMutableList()
    order.shuffle(java.util.Random(stepId.hashCode().toLong()))
    if (correctIndex !in 0 until optionCount) return order
    return order
}
