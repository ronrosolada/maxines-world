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
import com.maxinesworld.coredesignsystem.theme.Coral
import com.maxinesworld.coredesignsystem.theme.HeritageGold
import com.maxinesworld.coredesignsystem.theme.KindnessTeal
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.SkyBlue
import com.maxinesworld.coredesignsystem.theme.StoryPurple
import com.maxinesworld.coredesignsystem.theme.SubjectColors
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.SurfaceContainer
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import com.maxinesworld.coredesignsystem.theme.White
import com.maxinesworld.coremodel.ActivityStep
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * An offline, answer-neutral subject motif. It changes the visual rhythm of a
 * lesson without pretending to be a content-specific diagram or revealing an answer.
 * Authored lesson artwork takes precedence; this is used as a fallback only.
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

private data class ConceptPalette(
    val background: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
)

private fun paletteFor(kind: LessonVisualKind): ConceptPalette = when (kind) {
    LessonVisualKind.MATH -> ConceptPalette(
        background = SubjectColors.Mathematics.surface,
        primary = SubjectColors.Mathematics.primary,
        secondary = VillageTeal,
        accent = SunshineGold,
    )
    LessonVisualKind.LANGUAGE -> ConceptPalette(
        background = SubjectColors.English.surface,
        primary = SubjectColors.English.primary,
        secondary = VillageTeal,
        accent = Coral,
    )
    LessonVisualKind.SCIENCE -> ConceptPalette(
        background = SubjectColors.Science.surface,
        primary = SubjectColors.Science.primary,
        secondary = VillageTeal,
        accent = SunshineGold,
    )
    LessonVisualKind.COMMUNITY -> ConceptPalette(
        background = SubjectColors.History.surface,
        primary = HeritageGold,
        secondary = VillageTeal,
        accent = Coral,
    )
    LessonVisualKind.VALUES -> ConceptPalette(
        background = SubjectColors.Filipino.surface,
        primary = KindnessTeal,
        secondary = StoryPurple,
        accent = Coral,
    )
    LessonVisualKind.GENERAL -> ConceptPalette(
        background = SurfaceContainer,
        primary = VillageTeal,
        secondary = SkyBlue,
        accent = SunshineGold,
    )
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
    val palette = paletteFor(kind)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val animationsDisabled = LocalAnimationsDisabled.current
    var motionScale by remember { mutableFloatStateOf(animatorScale(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) motionScale = animatorScale(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val phase = if (motionAllowed && !animationsDisabled && motionScale > 0f) {
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
            .background(palette.background, RoundedCornerShape(20.dp))
            // The motif is decorative. The adjacent lesson text carries the full task.
            .clearAndSetSemantics { },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (kind) {
                LessonVisualKind.MATH -> drawMathMotif(phase, palette)
                LessonVisualKind.LANGUAGE -> drawLanguageMotif(phase, palette)
                LessonVisualKind.SCIENCE -> drawScienceMotif(phase, palette)
                LessonVisualKind.COMMUNITY -> drawCommunityMotif(phase, palette)
                LessonVisualKind.VALUES -> drawValuesMotif(phase, palette)
                LessonVisualKind.GENERAL -> drawTrailMotif(phase, palette)
            }
        }
    }
}

private fun DrawScope.designUnit(value: Float): Float = value * size.height / 148f

private fun wave(value: Float): Float = sin(value.toDouble()).toFloat()
private fun cosine(value: Float): Float = cos(value.toDouble()).toFloat()

private fun DrawScope.drawMathMotif(phase: Float, palette: ConceptPalette) {
    val colors = listOf(
        palette.primary,
        palette.secondary,
        palette.accent,
        palette.primary.copy(alpha = .75f),
    )
    repeat(4) { group ->
        val centerX = size.width * (.17f + group * .22f)
        val lift = wave((phase + group * .12f) * 2f * PI.toFloat()) * designUnit(4f)
        repeat(group + 1) { dot ->
            drawCircle(
                color = colors[group],
                radius = designUnit(11f),
                center = Offset(centerX, size.height * .72f - dot * designUnit(27f) + lift),
            )
        }
        drawLine(
            colors[group].copy(alpha = .35f),
            Offset(centerX - designUnit(28f), size.height * .83f),
            Offset(centerX + designUnit(28f), size.height * .83f),
            designUnit(5f),
        )
    }
}

private fun DrawScope.drawLanguageMotif(phase: Float, palette: ConceptPalette) {
    val centerX = size.width / 2f
    val top = size.height * .2f
    val page = Size(size.width * .28f, size.height * .58f)
    drawRoundRect(White, Offset(centerX - page.width, top), page, CornerRadius(designUnit(14f)))
    drawRoundRect(White, Offset(centerX, top), page, CornerRadius(designUnit(14f)))
    drawLine(palette.primary, Offset(centerX, top), Offset(centerX, top + page.height), designUnit(5f))
    repeat(3) { row ->
        val drift = wave((phase + row * .12f) * 2f * PI.toFloat()) * designUnit(5f)
        val rowY = top + designUnit(28f) + row * designUnit(26f)
        drawLine(
            palette.primary.copy(alpha = .55f),
            Offset(centerX - page.width + designUnit(20f) + drift, rowY),
            Offset(centerX - designUnit(18f), rowY),
            designUnit(5f),
        )
        drawLine(
            palette.secondary.copy(alpha = .55f),
            Offset(centerX + designUnit(18f), rowY),
            Offset(centerX + page.width - designUnit(20f) - drift, rowY),
            designUnit(5f),
        )
    }
}

private fun DrawScope.drawScienceMotif(phase: Float, palette: ConceptPalette) {
    val lens = Offset(size.width * .46f, size.height * .45f)
    drawCircle(White.copy(alpha = .72f), size.height * .23f, lens)
    drawCircle(palette.primary, size.height * .23f, lens, style = Stroke(designUnit(7f)))
    drawLine(
        palette.primary,
        lens + Offset(size.height * .17f, size.height * .17f),
        Offset(size.width * .7f, size.height * .82f),
        designUnit(12f),
    )
    val scanX = lens.x - designUnit(32f) + phase * designUnit(64f)
    drawLine(
        palette.accent.copy(alpha = .8f),
        Offset(scanX, lens.y - designUnit(34f)),
        Offset(scanX, lens.y + designUnit(34f)),
        designUnit(4f),
    )
    drawCircle(palette.secondary, designUnit(12f), lens - Offset(designUnit(28f), designUnit(4f)))
    drawRoundRect(
        palette.primary,
        lens + Offset(designUnit(8f), -designUnit(18f)),
        Size(designUnit(28f), designUnit(28f)),
        CornerRadius(designUnit(5f)),
    )
}

private fun DrawScope.drawCommunityMotif(phase: Float, palette: ConceptPalette) {
    val points = listOf(Offset(.22f, .68f), Offset(.5f, .28f), Offset(.78f, .68f))
    val blockColors = listOf(palette.accent, palette.secondary, palette.primary)
    points.forEachIndexed { index, point ->
        val center = Offset(size.width * point.x, size.height * point.y)
        val next = points[(index + 1) % points.size]
        drawLine(
            palette.primary.copy(alpha = .5f),
            center,
            Offset(size.width * next.x, size.height * next.y),
            designUnit(6f),
        )
        drawRoundRect(
            blockColors[index],
            center - Offset(designUnit(35f), designUnit(28f)),
            Size(designUnit(70f), designUnit(56f)),
            CornerRadius(designUnit(12f)),
        )
    }
    drawCircle(
        White.copy(alpha = .85f),
        designUnit(8f + 4f * wave(phase * 2f * PI.toFloat())),
        Offset(size.width * .5f, size.height * .58f),
    )
}

private fun DrawScope.drawValuesMotif(phase: Float, palette: ConceptPalette) {
    val center = Offset(size.width / 2f, size.height / 2f)
    repeat(5) { index ->
        val angle = index * 2f * PI.toFloat() / 5f - PI.toFloat() / 2f
        val person = center + Offset(cosine(angle) * size.width * .28f, wave(angle) * size.height * .3f)
        drawLine(palette.accent.copy(alpha = .35f), center, person, designUnit(5f))
        drawCircle(
            palette.primary,
            designUnit(17f + 2f * wave((phase + index * .1f) * 2f * PI.toFloat())),
            person,
        )
    }
    drawCircle(palette.secondary, designUnit(25f), center)
}

private fun DrawScope.drawTrailMotif(phase: Float, palette: ConceptPalette) {
    val path = Path().apply {
        moveTo(size.width * .1f, size.height * .72f)
        cubicTo(size.width * .32f, size.height * .18f, size.width * .62f, size.height * .88f, size.width * .9f, size.height * .3f)
    }
    drawPath(path, palette.primary.copy(alpha = .45f), style = Stroke(designUnit(8f)))
    drawCircle(
        palette.accent,
        designUnit(18f),
        Offset(size.width * (.12f + phase * .76f), size.height * (.64f - wave(phase * PI.toFloat()) * .2f)),
    )
}
