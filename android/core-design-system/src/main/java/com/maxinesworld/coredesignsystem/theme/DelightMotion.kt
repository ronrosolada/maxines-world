package com.maxinesworld.coredesignsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * Shared motion tokens for Maxine's World delight.
 *
 * Keep in core-design-system so lesson + rewards + home all share one
 * easing/duration source (mirrors `milo.states.js` / delight-lab.html).
 * Tuned to match the svg-character-animator engine's presets.
 */
object DelightMotion {
    // Easings — same curves as the JS engine
    val EaseOutCubic: Easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
    val EaseOutBack: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val EaseInOutQuad: Easing = CubicBezierEasing(0.455f, 0.03f, 0.515f, 0.955f)

    // Durations (ms)
    const val QuickPopMs = 420
    const val BouncyMs = 800
    const val ConfettiMs = 3000
    const val SparkleMs = 4000

    // Specs — ready to pass to animate*AsState
    fun bouncySpec() = tween<Float>(durationMillis = BouncyMs, easing = EaseOutBack)
    fun quickSpec() = tween<Float>(durationMillis = QuickPopMs, easing = EaseOutCubic)
    fun popSpring() = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    fun confettiTween() = tween<Float>(durationMillis = ConfettiMs, easing = LinearEasing)
    fun sparkleTween() = tween<Float>(durationMillis = SparkleMs, easing = LinearEasing)
    fun breatheTween(ms: Int = 1600) = tween<Float>(durationMillis = ms, easing = FastOutSlowInEasing)

    /** Guard: 0 animator scale = snap, like [animationsEnabledForScale] */
    fun isReducedMotion(animationsDisabled: Boolean): Boolean = animationsDisabled
}
