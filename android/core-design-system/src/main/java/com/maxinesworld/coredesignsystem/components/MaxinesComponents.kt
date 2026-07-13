package com.maxinesworld.coredesignsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.*

// ─── Utility ───

/** Darken a color by the given factor (0..1). Default 0.7 = 30% darker. */
private fun Color.darker(factor: Float = 0.7f): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)

// ─── Tactile Primary Button (§24.3, §4) ───

/**
 * Tactile child button with chunky bottom shadow per design v2 §24.3 / §4.
 *
 * Shadow: a darker tint of [containerColor] drawn **below** the button
 * (y+5dp, blur 0). On press the button translates down 4dp, the shadow
 * shrinks to y+1dp, and everything springs back.
 *
 * Label is Baloo 2 weight 700‑800 (via [AppDisplayFont]).
 * Height: 56‑64 dp (§9).
 */
@Composable
fun MaxinesPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Teal40,
    contentColor: Color = Color.White,
    height: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Shadow offset below the button: 5dp → 1dp on press
    val shadowOffsetY by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 5.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f)
    )

    // Button vertical translation: 0dp → 4dp on press
    val buttonOffsetY by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f)
    )

    // Shadow alpha: full → slightly faded on press
    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.45f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f)
    )

    val shadowColor = containerColor.darker(0.65f) // darker tint of button color

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(height)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        color = shadowColor.copy(alpha = shadowAlpha),
                        topLeft = Offset(0f, shadowOffsetY.toPx()),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                }
            }
            .offset(y = buttonOffsetY),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

// ─── Answer Card ───

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

// ─── Daily Quest Compact Banner (§7.4) ───

/**
 * Daily Quest compact banner — one tappable strip per design §7.4.
 *
 * Layout: [icon] quest-text … [progress bar + count] ▸ chevron
 *
 * Uses Cream surface with 2dp resting elevation.
 * **No Start button, no duplicate heading.**
 */
@Composable
fun MaxinesQuestCard(
    /** Stable subject ID (english, mathematics, ...) for color token. */
    subjectId: String,
    /** Short quest description (e.g. "Read a story with Mira"). */
    questText: String,
    /** Progress so far (0..total). */
    completed: Int,
    /** Total steps for this quest. */
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = subjectColorFor(subjectId)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Cream),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = subjectColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = subjectColor,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Quest text + progress
            Column(Modifier.weight(1f)) {
                Text(
                    text = questText,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / total.coerceAtLeast(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SuccessGreen,
                        trackColor = SuccessGreen.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$completed/$total",
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open quest",
                tint = subjectColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Map a stable subject ID to its primary subject color token. */
private fun subjectColorFor(subjectId: String): Color = when (subjectId) {
    "english"     -> StoryPurple
    "filipino"    -> Coral
    "mathematics" -> SkyBlue
    "science"     -> LeafGreen
    "makabansa"   -> HeritageGold
    "gmrc"        -> KindnessTeal
    else          -> VillageTeal
}
