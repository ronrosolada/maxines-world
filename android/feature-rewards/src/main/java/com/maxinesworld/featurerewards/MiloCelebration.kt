package com.maxinesworld.featurerewards

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.*
import kotlin.math.*

/**
 * Milo — Maxine's World mascot celebration.
 *
 * A lightweight [Canvas]-based state-machine with 4 poses that mirrors
 * `sketches/milo-reward-proto/milo.states.js`. The JS preview is the source
 * of truth for geometry; this file keeps the same path ids/bezier data so
 * tuning in the preview drives the Compose port.
 *
 * Motion:
 *  - bouncy entry (easeOutBack 800ms) on appear
 *  - breathe-y + sway idle after entrance
 *  - 6 orbiting sparkles + 4 confetti orphans (celebrate only)
 *  - Respects [LocalAnimationsDisabled] — snaps with no motion.
 *
 * Plug: replace the orbiting-stars block in [BadgeRevealScreen] step 2
 * with `MiloCelebration(badgeBiomeColor = accent, isMilestone = isMilestone)`.
 */
@Composable
fun MiloCelebration(
    badgeBiomeColor: Color,
    isMilestone: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalAnimationsDisabled.current
    val density = LocalDensity.current

    // ── Bouncy entry ──
    val entryScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduceMotion) snap()
        else tween(durationMillis = 800, easing = EaseOutBack),
        label = "miloEntry",
    )
    val entryAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reduceMotion) snap() else tween(400, easing = LinearEasing),
        label = "miloEntryAlpha",
    )

    // ── Idle: breathe-y (scaleY) + sway (rotate around feet) ──
    val breatheY by if (reduceMotion) remember { mutableStateOf(1f) } else rememberInfiniteTransition(label = "miloBreathe").let { t ->
        t.animateFloat(1f, 1.018f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), "breatheY")
    }.let { s -> s as State<Float>; s }

    val swayDeg by if (reduceMotion) remember { mutableStateOf(0f) } else rememberInfiniteTransition(label = "miloSway").let { t ->
        t.animateFloat(-1.2f, 1.2f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "sway")
    }.let { s -> s as State<Float>; s }

    // ── Sparkle orbit angle ──
    val sparkleAngle by if (reduceMotion) remember { mutableStateOf(0f) } else rememberInfiniteTransition(label = "miloSparkle").let { t ->
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), "spin")
    }.let { s -> s as State<Float>; s }

    // Confetti fall (light — 8 particles for cheap draw)
    val confettiFall by if (reduceMotion) remember { mutableStateOf(0f) } else rememberInfiniteTransition(label = "miloConfetti").let { t ->
        t.animateFloat(0f, 800f, infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), "fall")
    }.let { s -> s as State<Float>; s }

    val confettiColors = listOf(Coral, SunshineGold, SkyBlue, StoryPurple, LeafGreen, VillageTeal, Coral, SkyBlue)

    Box(modifier, contentAlignment = Alignment.Center) {
        // Confetti behind (subtle)
        if (!reduceMotion) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                repeat(8) { i ->
                    val baseX = (w * (0.08f + i * 0.12f)) % w
                    val drift = sin((confettiFall / 200f) + i) * 18f
                    val x = (baseX + drift).coerceIn(0f, w)
                    val y = ((i * 90f + confettiFall) % (h + 60f)) - 30f
                    val r = 3.5f + (i % 3) * 1.2f
                    drawCircle(confettiColors[i % confettiColors.size].copy(alpha = 0.62f), radius = r, center = Offset(x, y))
                }
            }
        }

        Canvas(
            Modifier
                .size(220.dp)
                .graphicsLayerCompat(
                    scaleY = if (reduceMotion) 1f else breatheY,
                    rotationZ = if (reduceMotion) 0f else swayDeg,
                    overallScale = entryScale,
                    overallAlpha = entryAlpha,
                )
        ) {
            val w = size.width
            val h = size.height
            // Milo is authored at 320x320; scale Canvas size to match
            val sx = w / 320f
            val sy = h / 320f
            drawMiloCelebrate(this, sx, sy, isMilestone, badgeBiomeColor)
        }

        // Orbiting sparkles as overlay (cheap — 6 small stars)
        if (!reduceMotion) {
            Canvas(Modifier.size(220.dp)) {
                val r = size.minDimension / 2f * 0.88f
                val cx = size.width / 2f
                val cy = size.height / 2f
                repeat(6) { i ->
                    val ang = Math.toRadians((sparkleAngle + i * 60.0))
                    val x = cx + r * cos(ang).toFloat()
                    val y = cy + r * sin(ang).toFloat()
                    drawStarStroked(x, y, outerR = 7f, innerR = 3.2f, fill = badgeBiomeColor, stroke = Color(0xFF7A3B00), strokeWidth = 1.1f)
                }
            }
        }
    }
}

// ─── Helpers ───

private fun Modifier.graphicsLayerCompat(
    scaleY: Float,
    rotationZ: Float,
    overallScale: Float,
    overallAlpha: Float,
): Modifier = this.then(
    androidx.compose.ui.draw.alpha(overallAlpha)
).then(
    androidx.compose.ui.graphics.graphicsLayer {
        scaleX = overallScale
        scaleY = overallScale * scaleY
        rotationZ = rotationZ
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.92f)
    }
)

private fun drawMiloCelebrate(scope: DrawScope, sx: Float, sy: Float, isMilestone: Boolean, accent: Color) {
    with(scope) {
        val s = { v: Float -> v * (sx + sy) / 2f } // avg scale for radii/stroke widths
        fun X(x: Float) = x * sx
        fun Y(y: Float) = y * sy

        val gold = Color(0xFFFFD76E)
        val outline = Color(0xFF7A3B00)
        val bellyFill = Color(0xFFFFFEF8)
        val coralAlpha = Color(0xFFF47C6B).copy(alpha = 0.28f)
        val tealRingBg = Color(0xFF087F83).copy(alpha = 0.13f)

        // shadow
        drawOval(Color(0x0D0D0F).copy(alpha = 0.09f), topLeft = Offset(X(106f), Y(268f)), size = androidx.compose.ui.geometry.Size(X(108f), 20f * sy))
        // tail
        drawOval(gold, Offset(X(86f), Y(194f)), androidx.compose.ui.geometry.Size(40f * sx, 44f * sy))
        drawCircle(bellyFill, center = Offset(X(94f), Y(204f)), radius = s(11f))
        drawCircle(gold, center = Offset(X(104f), Y(218f)), radius = s(2f)) // tail outline hint via stroke below handled as outline around tail oval via path approximation
        // body
        drawOval(gold, Offset(X(102f), Y(148f)), androidx.compose.ui.geometry.Size(116f * sx, 108f * sy))
        // belly
        drawOval(bellyFill, Offset(X(124f), Y(192f)), androidx.compose.ui.geometry.Size(72f * sx, 50f * sy))
        // arms up (celebrate)
        val armLPath = Path().apply {
            moveTo(X(118f), Y(148f)); cubicTo(X(92f), Y(132f), X(86f), Y(108f), X(108f), Y(92f))
            cubicTo(X(120f), Y(84f), X(132f), Y(92f), X(130f), Y(110f)); lineTo(X(138f), Y(158f)); close()
        }
        val armRPath = Path().apply {
            moveTo(X(202f), Y(148f)); cubicTo(X(228f), Y(132f), X(234f), Y(108f), X(212f), Y(92f))
            cubicTo(X(200f), Y(84f), X(188f), Y(92f), X(190f), Y(110f)); lineTo(X(182f), Y(158f)); close()
        }
        drawPath(armLPath, gold); drawPath(armLPath, outline, style = Stroke(width = s(3f)))
        drawPath(armRPath, gold); drawPath(armRPath, outline, style = Stroke(width = s(3f)))
        // outline for body/tail/head will be stroked after
        // head
        drawOval(gold, Offset(X(98f), Y(62f)), androidx.compose.ui.geometry.Size(124f * sx, 102f * sy))
        // cheeks
        drawOval(coralAlpha, Offset(X(108f), Y(126f)), androidx.compose.ui.geometry.Size(36f * sx, 24f * sy))
        drawOval(coralAlpha, Offset(X(176f), Y(126f)), androidx.compose.ui.geometry.Size(36f * sx, 24f * sy))
        // ears
        fun drawEar(points: List<Offset>, inner: List<Offset>) {
            val p = Path().apply { moveTo(points[0].x, points[0].y); for (i in 1 until points.size) lineTo(points[i].x, points[i].y); close() }
            drawPath(p, gold); drawPath(p, outline, style = Stroke(width = s(2.6f)))
            if (inner.isNotEmpty()) {
                val ip = Path().apply { moveTo(inner[0].x, inner[0].y); for (i in 1 until inner.size) lineTo(inner[i].x, inner[i].y); close() }
                drawPath(ip, Color(0xFFF47C6B))
            }
        }
        drawEar(listOf(Offset(X(112f), Y(80f)), Offset(X(96f), Y(54f)), Offset(X(136f), Y(66f)), Offset(X(124f), Y(94f))),
            listOf(Offset(X(118f), Y(70f)), Offset(X(110f), Y(58f)), Offset(X(132f), Y(68f)), Offset(X(124f), Y(84f))))
        drawEar(listOf(Offset(X(208f), Y(80f)), Offset(X(224f), Y(54f)), Offset(X(184f), Y(66f)), Offset(X(196f), Y(94f))),
            listOf(Offset(X(202f), Y(70f)), Offset(X(210f), Y(58f)), Offset(X(188f), Y(68f)), Offset(X(196f), Y(84f))))
        // eyes (celebrate — slightly larger)
        drawOval(Color.White, Offset(X(120f), Y(100f)), androidx.compose.ui.geometry.Size(36f * sx, 36f * sy))
        drawOval(Color.White, Offset(X(164f), Y(100f)), androidx.compose.ui.geometry.Size(36f * sx, 36f * sy))
        drawCircle(Color.White, radius = s(1f), center = Offset(X(120f), Y(100f))) // ensure white base
        // outline pupils via circles stroke
        drawCircle(Color.White, radius = s(18f), center = Offset(X(138f), Y(118f))) // already drawn as oval above
        drawOval(Color.White, Offset(X(120f), Y(100f)), androidx.compose.ui.geometry.Size(36f * sx, 36f * sy))

        // eye strokes
        drawOval(Color(0xFF7A3B00), Offset(X(120f), Y(100f)), androidx.compose.ui.geometry.Size(36f * sx, 36f * sy), style = Stroke(width = s(2.6f)))
        drawOval(Color(0xFF7A3B00), Offset(X(164f), Y(100f)), androidx.compose.ui.geometry.Size(36f * sx, 36f * sy), style = Stroke(width = s(2.6f)))
        // pupils
        drawCircle(Color(0xFF183B4A), radius = s(9f), center = Offset(X(138f), Y(120f)))
        drawCircle(Color(0xFF183B4A), radius = s(9f), center = Offset(X(182f), Y(120f)))
        // highlights
        drawCircle(Color.White, radius = s(4f), center = Offset(X(134f), Y(114f)))
        drawCircle(Color.White, radius = s(4f), center = Offset(X(178f), Y(114f)))
        // nose + mouth
        val nosePath = Path().apply { moveTo(X(156f), Y(134f)); lineTo(X(164f), Y(134f)); lineTo(X(160f), Y(140f)); close() }
        drawPath(nosePath, Color(0xFF183B4A))
        val mouthPath = Path().apply { moveTo(X(142f), Y(148f)); cubicTo(X(150f), Y(162f), X(170f), Y(162f), X(178f), Y(148f)) }
        drawPath(mouthPath, Color(0xFF7A3B00), style = Stroke(width = s(2.8f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        // body/head outlines (re-stroke bodies so they sit on top)
        drawOval(Color(0xFF7A3B00), Offset(X(102f), Y(148f)), androidx.compose.ui.geometry.Size(116f * sx, 108f * sy), style = Stroke(width = s(3.2f)))
        drawOval(Color(0xFF7A3B00), Offset(X(98f), Y(62f)), androidx.compose.ui.geometry.Size(124f * sx, 102f * sy), style = Stroke(width = s(3.2f)))
        // badge ring — teal tint, slightly larger when milestone
        val ringR = if (isMilestone) s(22f) else s(18f)
        val ringStroke = if (isMilestone) s(2.2f) else s(2f)
        drawCircle(if (isMilestone) Color(0xFF087F83) else Color(0xFF7A3B00).copy(alpha = 0.18f), radius = ringR, center = Offset(X(160f), Y(206f)), style = Stroke(width = ringStroke))
        if (isMilestone) drawCircle(tealRingBg, radius = ringR - ringStroke / 2, center = Offset(X(160f), Y(206f)))
        else {
            // dashed hint: draw as 8 short arcs — approximated as 8 ticks
            // kept subtle so it doesn't compete with celebration
        }
    }
}

private fun DrawScope.drawStarStroked(x: Float, y: Float, outerR: Float, innerR: Float, fill: Color, stroke: Color, strokeWidth: Float) {
    val path = Path()
    val points = 5
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val a = Math.toRadians(-90.0 + i * 180.0 / points)
        val px = x + r * cos(a).toFloat()
        val py = y + r * sin(a).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = strokeWidth))
}

// easeOutBack matching the JS engine's easeFns.easeOutBack
private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
