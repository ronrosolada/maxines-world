package com.maxinesworld.featurerangerjournal

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Shared canvas artwork for the four sanctuary scenes. */
@Composable
fun JournalSceneArtwork(scene: JournalScene, modifier: Modifier) {
    Canvas(modifier) {
        drawScene(scene)
    }
}

internal fun DrawScope.drawScene(scene: JournalScene) {
    val skyTop = Color(scene.skyTop)
    val skyBottom = Color(scene.skyBottom)
    val ground = Color(scene.ground)
    val accent = Color(scene.accent)
    val w = size.width
    val h = size.height

    drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

    when (scene.id) {
        "eagle" -> {
            // Sun
            drawCircle(Color(0xFFFFE082).copy(alpha = 0.9f), radius = h * 0.16f, center = Offset(w * 0.72f, h * 0.22f))
            // Mountains
            val mountains = Path().apply {
                moveTo(0f, h * 0.72f)
                lineTo(w * 0.25f, h * 0.42f)
                lineTo(w * 0.45f, h * 0.66f)
                lineTo(w * 0.62f, h * 0.48f)
                lineTo(w * 0.82f, h * 0.70f)
                lineTo(w * 1.05f, h * 0.58f)
                lineTo(w * 1.05f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(mountains, Color(0xFF7E9C7E))
            // Eagle silhouette
            val eagle = Path().apply {
                moveTo(w * 0.5f, h * 0.30f)
                lineTo(w * 0.34f, h * 0.26f)
                lineTo(w * 0.42f, h * 0.40f)
                lineTo(w * 0.28f, h * 0.52f)
                lineTo(w * 0.44f, h * 0.50f)
                lineTo(w * 0.5f, h * 0.62f)
                lineTo(w * 0.56f, h * 0.50f)
                lineTo(w * 0.72f, h * 0.52f)
                lineTo(w * 0.58f, h * 0.40f)
                lineTo(w * 0.66f, h * 0.26f)
                lineTo(w * 0.5f, h * 0.30f)
                close()
            }
            drawPath(eagle, Color(0xFF37474F))
        }
        "tamaraw" -> {
            // Rolling hills
            drawOval(
                Color(0xFF9CCC65).copy(alpha = 0.7f),
                topLeft = Offset(-w * 0.1f, h * 0.62f),
                size = Size(w * 0.7f, h * 0.45f),
            )
            // Tamaraw silhouette (body + horns)
            val body = Path().apply {
                moveTo(w * 0.30f, h * 0.74f)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        Offset(w * 0.30f, h * 0.58f),
                        Size(w * 0.40f, h * 0.26f)
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false,
                )
                lineTo(w * 0.70f, h * 0.74f)
                lineTo(w * 0.66f, h * 0.84f)
                lineTo(w * 0.62f, h * 0.80f)
                lineTo(w * 0.58f, h * 0.84f)
                lineTo(w * 0.54f, h * 0.78f)
                lineTo(w * 0.46f, h * 0.78f)
                lineTo(w * 0.42f, h * 0.84f)
                lineTo(w * 0.38f, h * 0.80f)
                lineTo(w * 0.34f, h * 0.84f)
                lineTo(w * 0.30f, h * 0.74f)
                close()
            }
            drawPath(body, Color(0xFF4E3B2E))
            // Head + horns
            drawCircle(Color(0xFF4E3B2E), radius = h * 0.05f, center = Offset(w * 0.42f, h * 0.62f))
            val hornL = Path().apply {
                moveTo(w * 0.40f, h * 0.60f)
                quadraticBezierTo(w * 0.36f, h * 0.52f, w * 0.42f, h * 0.48f)
                quadraticBezierTo(w * 0.46f, h * 0.52f, w * 0.44f, h * 0.58f)
                close()
            }
            val hornR = Path().apply {
                moveTo(w * 0.44f, h * 0.58f)
                quadraticBezierTo(w * 0.48f, h * 0.50f, w * 0.54f, h * 0.52f)
                quadraticBezierTo(w * 0.52f, h * 0.58f, w * 0.46f, h * 0.60f)
                close()
            }
            drawPath(hornL, Color(0xFFE8DCC8))
            drawPath(hornR, Color(0xFFE8DCC8))
        }
        "pawikan" -> {
            // Sea
            drawRect(ground.copy(alpha = 0.5f), topLeft = Offset(0f, h * 0.55f), size = Size(w, h * 0.45f))
            // Waves
            repeat(3) { i ->
                val y = h * (0.58f + i * 0.08f)
                val path = Path().apply {
                    moveTo(0f, y)
                    repeat(6) { k ->
                        quadraticBezierTo(
                            w * (k * 2 + 1) / 12f, y - h * 0.03f,
                            w * (k + 1) / 6f, y,
                        )
                    }
                }
                drawPath(path, Color(0xFFFFFFFF).copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = h * 0.012f))
            }
            // Turtle silhouette
            drawOval(
                Color(0xFF2E7D5B),
                topLeft = Offset(w * 0.38f, h * 0.52f),
                size = Size(w * 0.24f, h * 0.16f),
            )
            drawCircle(Color(0xFF2E7D5B), radius = h * 0.075f, center = Offset(w * 0.40f, h * 0.62f))
            drawCircle(Color(0xFFF4E8D0), radius = h * 0.02f, center = Offset(w * 0.40f, h * 0.60f))
            // Flippers
            drawLine(Color(0xFF2E7D5B), Offset(w * 0.36f, h * 0.66f), Offset(w * 0.28f, h * 0.72f), strokeWidth = h * 0.03f)
            drawLine(Color(0xFF2E7D5B), Offset(w * 0.64f, h * 0.66f), Offset(w * 0.72f, h * 0.72f), strokeWidth = h * 0.03f)
        }
        else -> { // tarsier forest
            // Trunk
            drawRoundRect(
                Color(0xFF7A5B3A),
                topLeft = Offset(w * 0.46f, h * 0.10f),
                size = Size(w * 0.08f, h * 0.80f),
                cornerRadius = CornerRadius(w * 0.02f),
            )
            // Canopy clusters
            listOf(
                Offset(w * 0.30f, h * 0.18f),
                Offset(w * 0.55f, h * 0.10f),
                Offset(w * 0.75f, h * 0.22f),
            ).forEach { center ->
                drawCircle(ground.copy(alpha = 0.85f), radius = h * 0.14f, center = center)
                drawCircle(ground.copy(alpha = 0.5f), radius = h * 0.09f, center = center + Offset(-h * 0.06f, h * 0.03f))
            }
            // Ground band
            drawRect(Color(0xFF4E7A4A), topLeft = Offset(0f, h * 0.86f), size = Size(w, h * 0.14f))
            // Tarsier on the branch
            val branchY = h * 0.62f
            drawRoundRect(
                accent,
                topLeft = Offset(w * 0.36f, branchY),
                size = Size(w * 0.30f, h * 0.03f),
                cornerRadius = CornerRadius(h * 0.02f),
            )
            drawCircle(accent, radius = h * 0.055f, center = Offset(w * 0.44f, branchY - h * 0.03f))
            drawCircle(accent, radius = h * 0.04f, center = Offset(w * 0.50f, branchY - h * 0.05f))
            drawCircle(Color(0xFF1F2937), radius = h * 0.016f, center = Offset(w * 0.435f, branchY - h * 0.055f))
            drawCircle(Color(0xFF1F2937), radius = h * 0.016f, center = Offset(w * 0.465f, branchY - h * 0.055f))
            drawCircle(Color.White, radius = h * 0.005f, center = Offset(w * 0.44f, branchY - h * 0.06f))
            drawCircle(Color.White, radius = h * 0.005f, center = Offset(w * 0.47f, branchY - h * 0.06f))
            val tail = Path().apply {
                moveTo(w * 0.42f, branchY - h * 0.03f)
                quadraticBezierTo(w * 0.30f, branchY - h * 0.02f, w * 0.32f, branchY - h * 0.10f)
            }
            drawPath(tail, accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = h * 0.012f))
        }
    }

    // Ground base + vignette for depth
    drawRect(ground.copy(alpha = 0.6f), topLeft = Offset(0f, h * 0.92f), size = Size(w, h * 0.08f))
}