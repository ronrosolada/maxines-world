package com.maxinesworld.engineactivity.renderers

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.maxinesworld.coremodel.ActivityStep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An offline, answer-neutral subject motif. It changes the visual rhythm of a
 * lesson without pretending to be a content-specific diagram or revealing an answer.
 */
enum class LessonVisualKind { MATH, LANGUAGE, SCIENCE, COMMUNITY, VALUES, GENERAL }

internal fun lessonVisualKind(step: ActivityStep): LessonVisualKind {
    val id = step.id.lowercase()
    return when {
        id.startsWith("mathematics-") || id.startsWith("math-") -> LessonVisualKind.MATH
        id.startsWith("english-") || id.startsWith("filipino-") -> LessonVisualKind.LANGUAGE
        id.startsWith("science-") -> LessonVisualKind.SCIENCE
        id.startsWith("gmrc-") -> LessonVisualKind.VALUES
        id.startsWith("makabansa-") || id.startsWith("araling-panlipunan-") -> LessonVisualKind.COMMUNITY
        else -> LessonVisualKind.GENERAL
    }
}

private fun animatorScale(context: android.content.Context): Float = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
}.getOrDefault(1f)

@Composable
fun LessonConceptVisual(
    step: ActivityStep,
    modifier: Modifier = Modifier,
    motionAllowed: Boolean = true,
) {
    val kind = remember(step.id) { lessonVisualKind(step) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var motionScale by remember { mutableFloatStateOf(animatorScale(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) motionScale = animatorScale(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val phase = if (motionAllowed && motionScale > 0f) {
        val transition = rememberInfiniteTransition(label = "lessonVisual")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (5000f * motionScale.coerceIn(.5f, 10f)).toInt(),
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "gentleMotion",
        )
        value
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .background(backgroundFor(kind), RoundedCornerShape(20.dp))
            // The motif is decorative. The adjacent lesson text carries the full task.
            .clearAndSetSemantics { },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (kind) {
                LessonVisualKind.MATH -> drawMathMotif(phase)
                LessonVisualKind.LANGUAGE -> drawLanguageMotif(phase)
                LessonVisualKind.SCIENCE -> drawScienceMotif(phase)
                LessonVisualKind.COMMUNITY -> drawCommunityMotif(phase)
                LessonVisualKind.VALUES -> drawValuesMotif(phase)
                LessonVisualKind.GENERAL -> drawTrailMotif(phase)
            }
        }
    }
}

private fun backgroundFor(kind: LessonVisualKind): Color = when (kind) {
    LessonVisualKind.MATH -> Color(0xFFFFF5D8)
    LessonVisualKind.LANGUAGE -> Color(0xFFF3ECFF)
    LessonVisualKind.SCIENCE -> Color(0xFFE8F7EE)
    LessonVisualKind.COMMUNITY -> Color(0xFFE8F4FA)
    LessonVisualKind.VALUES -> Color(0xFFFFECED)
    LessonVisualKind.GENERAL -> Color(0xFFEAF6F4)
}

private fun wave(value: Float): Float = sin(value.toDouble()).toFloat()
private fun cosine(value: Float): Float = cos(value.toDouble()).toFloat()

private fun DrawScope.drawMathMotif(phase: Float) {
    val colors = listOf(Color(0xFF8054B3), Color(0xFF2D8C82), Color(0xFFF2A33A), Color(0xFFE96B63))
    repeat(4) { group ->
        val centerX = size.width * (.17f + group * .22f)
        val lift = wave((phase + group * .12f) * 2f * PI.toFloat()) * 4f
        repeat(group + 1) { dot ->
            drawCircle(
                color = colors[group],
                radius = 11f,
                center = Offset(centerX, size.height * .72f - dot * 27f + lift),
            )
        }
        drawLine(colors[group].copy(alpha = .35f), Offset(centerX - 28f, size.height * .83f), Offset(centerX + 28f, size.height * .83f), 5f)
    }
}

private fun DrawScope.drawLanguageMotif(phase: Float) {
    val centerX = size.width / 2f
    val top = size.height * .2f
    val page = Size(size.width * .28f, size.height * .58f)
    drawRoundRect(Color.White, Offset(centerX - page.width, top), page, CornerRadius(14f))
    drawRoundRect(Color.White, Offset(centerX, top), page, CornerRadius(14f))
    drawLine(Color(0xFF8054B3), Offset(centerX, top), Offset(centerX, top + page.height), 5f)
    repeat(3) { row ->
        val drift = wave((phase + row * .12f) * 2f * PI.toFloat()) * 5f
        drawLine(Color(0xFF8054B3).copy(alpha = .55f), Offset(centerX - page.width + 20f + drift, top + 28f + row * 26f), Offset(centerX - 18f, top + 28f + row * 26f), 5f)
        drawLine(Color(0xFF2D8C82).copy(alpha = .55f), Offset(centerX + 18f, top + 28f + row * 26f), Offset(centerX + page.width - 20f - drift, top + 28f + row * 26f), 5f)
    }
}

private fun DrawScope.drawScienceMotif(phase: Float) {
    val lens = Offset(size.width * .46f, size.height * .45f)
    drawCircle(Color.White.copy(alpha = .72f), size.height * .23f, lens)
    drawCircle(Color(0xFF2D8C82), size.height * .23f, lens, style = Stroke(7f))
    drawLine(Color(0xFF2D8C82), lens + Offset(size.height * .17f, size.height * .17f), Offset(size.width * .7f, size.height * .82f), 12f)
    val scanX = lens.x - 32f + phase * 64f
    drawLine(Color(0xFFF2A33A).copy(alpha = .8f), Offset(scanX, lens.y - 34f), Offset(scanX, lens.y + 34f), 4f)
    drawCircle(Color(0xFFE96B63), 12f, lens - Offset(28f, 4f))
    drawRoundRect(Color(0xFF8054B3), lens + Offset(8f, -18f), Size(28f, 28f), CornerRadius(5f))
}

private fun DrawScope.drawCommunityMotif(phase: Float) {
    val points = listOf(Offset(.22f, .68f), Offset(.5f, .28f), Offset(.78f, .68f))
    points.forEachIndexed { index, point ->
        val center = Offset(size.width * point.x, size.height * point.y)
        val next = points[(index + 1) % points.size]
        drawLine(Color(0xFF2D8C82).copy(alpha = .5f), center, Offset(size.width * next.x, size.height * next.y), 6f)
        drawRoundRect(listOf(Color(0xFFE96B63), Color(0xFFF2A33A), Color(0xFF8054B3))[index], center - Offset(35f, 28f), Size(70f, 56f), CornerRadius(12f))
    }
    drawCircle(Color.White.copy(alpha = .85f), 8f + 4f * wave(phase * 2f * PI.toFloat()), Offset(size.width * .5f, size.height * .58f))
}

private fun DrawScope.drawValuesMotif(phase: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    repeat(5) { index ->
        val angle = index * 2f * PI.toFloat() / 5f - PI.toFloat() / 2f
        val person = center + Offset(cosine(angle) * size.width * .28f, wave(angle) * size.height * .3f)
        drawLine(Color(0xFFE96B63).copy(alpha = .35f), center, person, 5f)
        drawCircle(Color(0xFF8054B3), 17f + 2f * wave((phase + index * .1f) * 2f * PI.toFloat()), person)
    }
    drawCircle(Color(0xFFE96B63), 25f, center)
}

private fun DrawScope.drawTrailMotif(phase: Float) {
    val path = Path().apply {
        moveTo(size.width * .1f, size.height * .72f)
        cubicTo(size.width * .32f, size.height * .18f, size.width * .62f, size.height * .88f, size.width * .9f, size.height * .3f)
    }
    drawPath(path, Color(0xFF2D8C82).copy(alpha = .45f), style = Stroke(8f))
    drawCircle(Color(0xFFF2A33A), 18f, Offset(size.width * (.12f + phase * .76f), size.height * (.64f - wave(phase * PI.toFloat()) * .2f)))
}
