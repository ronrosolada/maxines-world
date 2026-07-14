package com.maxinesworld.featurelessonplayer

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// ─── Shared Press Feedback (duplicated from village for module isolation) ───

val lessonSpringBounce = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

fun Modifier.lessonPressScale(): Modifier = composed {
    var isPressing by remember { mutableStateOf(false) }
    val s by animateFloatAsState(
        targetValue = if (isPressing) 0.96f else 1.0f,
        animationSpec = if (isPressing) tween(120) else lessonSpringBounce,
        label = "lessonPress",
    )
    this.scale(s).pointerInput(Unit) {
        detectTapGestures(onPress = {
            isPressing = true; tryAwaitRelease(); isPressing = false
        })
    }
}

// ─── Mira Reactions ───

@Composable
fun rememberMiraActivityEntryScale(reducedMotion: Boolean): Float {
    if (reducedMotion) return 1.0f
    val anim = remember { Animatable(0.94f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        anim.animateTo(1.0f, tween(250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
    }
    return anim.value
}

@Composable
fun rememberMiraCorrectTilt(reducedMotion: Boolean, trigger: Boolean): Float {
    if (reducedMotion || !trigger) return 0f
    val anim = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        anim.animateTo(-5f, tween(200))
        anim.animateTo(5f, tween(200))
        anim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
    }
    return anim.value
}

@Composable
fun rememberMiraRetryBob(reducedMotion: Boolean, trigger: Boolean): Float {
    if (reducedMotion || !trigger) return 0f
    val anim = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        anim.animateTo(-3f, tween(175))
        anim.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
    }
    return anim.value
}

@Composable
fun rememberMiraCompletionBounce(reducedMotion: Boolean, trigger: Boolean): Float {
    if (reducedMotion || !trigger) return 1.0f
    val anim = remember { Animatable(1.0f) }
    androidx.compose.runtime.LaunchedEffect(trigger) {
        anim.animateTo(1.08f, tween(250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        anim.animateTo(1.0f, spring(Spring.DampingRatioMediumBouncy))
    }
    return anim.value
}

// ─── Narration Waves ───

@Composable
fun rememberNarrationWaveAlpha(reducedMotion: Boolean, isPlaying: Boolean): Float {
    if (reducedMotion) return if (isPlaying) 0.3f else 0f
    val target = if (isPlaying) 1.0f else 0f
    val alpha by animateFloatAsState(target, tween(300), label = "waveAlpha")
    return alpha
}

@Composable
fun rememberNarrationWaveScale(reducedMotion: Boolean, isPlaying: Boolean): Float {
    if (reducedMotion) return 1.0f
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val s by infiniteTransition.animateFloat(
        1.0f, 1.15f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "waveScale"
    )
    return if (isPlaying) s else 1.0f
}
