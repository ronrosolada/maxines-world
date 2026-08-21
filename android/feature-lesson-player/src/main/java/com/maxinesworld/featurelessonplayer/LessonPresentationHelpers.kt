package com.maxinesworld.featurelessonplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.maxinesworld.coredesignsystem.components.AnswerCardState
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.Teal40
import com.maxinesworld.coremodel.ActivityStep

/** Minimal lesson-chrome localization retained for completion and renderer contracts. */
internal fun lessonUiText(language: String?, en: String, fil: String): String =
    if (language?.lowercase()?.startsWith("fil") == true) fil else en

/** Returns the localized fallback message shown when lesson narration is unavailable. */
internal fun ttsUnavailableMessage(language: String?): String =
    lessonUiText(
        language,
        "Voice not available on this device — please read along instead.",
        "Walang boses sa device na ito — basahin na lang natin ang teksto.",
    )

/** Returns true only when narration adds context beyond the activity instruction. */
internal fun shouldShowNarrationCard(step: ActivityStep): Boolean =
    step.type != "ANIMATED_EXPLANATION_V1" &&
        step.narrationText.isNotBlank() &&
        step.narrationText.trim() != step.question.trim()

internal fun assessmentOptionState(
    index: Int,
    selectedIndex: Int?,
    answered: Boolean,
    correctIndex: Int,
): AnswerCardState = when {
    answered && index == selectedIndex && index == correctIndex -> AnswerCardState.CORRECT
    answered && index == selectedIndex -> AnswerCardState.INCORRECT
    answered -> AnswerCardState.DISABLED
    index == selectedIndex -> AnswerCardState.SELECTED
    else -> AnswerCardState.IDLE
}

/** Small narration control retained for the standalone narration contract test. */
@Composable
fun NarrationControlRow(
    narrationEnabled: Boolean,
    ttsSpeaking: Boolean,
    onToggle: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
            Icon(
                if (narrationEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                if (narrationEnabled) "Turn narration off" else "Turn narration on",
                tint = if (narrationEnabled) Teal40 else Ink.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(
            enabled = narrationEnabled,
            onClick = onReplay,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                if (ttsSpeaking) Icons.Default.Stop else Icons.Default.Replay,
                if (ttsSpeaking) "Stop narration" else "Replay narration",
                tint = if (ttsSpeaking) Ink else Teal40,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
internal fun rememberConfettiProgress(enabled: Boolean): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "confetti")
    return transition.animateFloat(
        0f,
        800f,
        infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "fall",
    ).value
}

internal fun confettiAnimationEnabled(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f
