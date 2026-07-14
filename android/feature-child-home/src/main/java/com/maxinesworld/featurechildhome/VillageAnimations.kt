package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput

// ─── Motion Tokens ───

object MotionTokens {
    const val KEY_PRESS = 120
    const val FAST_STATE = 180
    const val ENTRANCE = 300
    const val PROGRESS = 500
    const val CELEBRATION = 900
    const val AMBIENT_HALF = 1800

    val friendlyEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
}

val springBounce = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

// ─── Shared Press Feedback ───

fun Modifier.pressScale(): Modifier = composed {
    var isPressing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressing) 0.96f else 1.0f,
        animationSpec = if (isPressing) tween(MotionTokens.KEY_PRESS) else springBounce,
        label = "pressScale",
    )
    this.scale(scale).pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                isPressing = true
                tryAwaitRelease()
                isPressing = false
            }
        )
    }
}

// ─── Mira Ambient Bob ───

@Composable
fun rememberMiraBobOffset(reducedMotion: Boolean): Float {
    if (reducedMotion) return 0f
    val infiniteTransition = rememberInfiniteTransition(label = "miraBob")
    val bob by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(MotionTokens.AMBIENT_HALF, easing = MotionTokens.friendlyEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "miraBobY",
    )
    return bob * 4f
}

// ─── Help Mira Pulse ───

@Composable
fun rememberHelpMiraPulse(reducedMotion: Boolean): Float {
    if (reducedMotion) return 1.0f
    val pulse = remember { Animatable(1.0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000L)
        pulse.animateTo(1.04f, tween(225, easing = MotionTokens.friendlyEasing))
        pulse.animateTo(1.0f, tween(225, easing = MotionTokens.friendlyEasing))
    }
    return pulse.value
}

// ─── Lantern Glow ───

@Composable
fun rememberLanternGlow(reducedMotion: Boolean): Float {
    if (reducedMotion) return 1.0f
    val infiniteTransition = rememberInfiniteTransition(label = "lantern")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = MotionTokens.friendlyEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lanternGlow",
    )
    return glow
}
