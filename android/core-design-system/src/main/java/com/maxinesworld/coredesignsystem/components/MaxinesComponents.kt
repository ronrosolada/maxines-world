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

@Composable
fun MaxinesAnswerCard(
    selected: Boolean,
    correct: Boolean? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = when {
        correct == true -> SuccessGreen.copy(alpha = 0.15f)
        correct == false -> ErrorRed.copy(alpha = 0.1f)
        selected -> Teal40.copy(alpha = 0.08f)
        else -> SurfaceContainer
    }
    Card(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (selected) 4.dp else 2.dp,
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
