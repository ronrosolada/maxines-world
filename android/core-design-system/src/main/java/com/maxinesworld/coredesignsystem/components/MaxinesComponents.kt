package com.maxinesworld.coredesignsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.theme.*

/**
 * Tactile child button with chunky bottom shadow per design v2 §24.3.
 * The shadow creates a "pressable" affordance — the button looks
 * like it can be physically pushed down.
 */
@Composable
fun MaxinesPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Teal40,
    contentColor: Color = Color.White,
    shadowColor: Color = containerColor,
    height: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(height)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(text)
    }
}

/**
 * Explicit card state for answer cards, replacing the old nullable Boolean pattern.
 * Design v3 §12.4: use explicit enum states (IDLE, SELECTED, CORRECT, INCORRECT, DISABLED).
 */
enum class AnswerCardState {
    IDLE,
    SELECTED,
    CORRECT,
    INCORRECT,
    DISABLED
}

@Composable
fun MaxinesAnswerCard(
    state: AnswerCardState = AnswerCardState.IDLE,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = when (state) {
        AnswerCardState.CORRECT -> SuccessGreen.copy(alpha = 0.15f)
        AnswerCardState.INCORRECT -> ErrorRed.copy(alpha = 0.1f)
        AnswerCardState.SELECTED -> Teal40.copy(alpha = 0.08f)
        AnswerCardState.DISABLED -> SurfaceContainer.copy(alpha = 0.5f)
        AnswerCardState.IDLE -> SurfaceContainer
    }
    Card(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = when (state) {
                    AnswerCardState.SELECTED, AnswerCardState.CORRECT, AnswerCardState.INCORRECT -> 4.dp
                    else -> 2.dp
                },
                shape = RoundedCornerShape(16.dp),
                ambientColor = bgColor,
                spotColor = bgColor
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        content()
    }
}

/**
 * Backward-compatible overload accepting the old Boolean-based params.
 * Maps to [AnswerCardState] and delegates to the primary signature.
 */
@Composable
@Deprecated("Use AnswerCardState overload instead", ReplaceWith("MaxinesAnswerCard(state = AnswerCardState.IDLE, onClick = onClick, modifier = modifier) { content() }"))
fun MaxinesAnswerCard(
    selected: Boolean,
    correct: Boolean? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = when {
        correct == true -> AnswerCardState.CORRECT
        correct == false -> AnswerCardState.INCORRECT
        selected -> AnswerCardState.SELECTED
        else -> AnswerCardState.IDLE
    }
    MaxinesAnswerCard(state = state, onClick = onClick, modifier = modifier, content = content)
}
