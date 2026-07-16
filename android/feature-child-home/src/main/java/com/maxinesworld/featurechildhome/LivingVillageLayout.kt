package com.maxinesworld.featurechildhome

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * Converts design coordinates (3048×2032) into pixel positions within a container,
 * preserving aspect ratio via letterbox/pillarbox centering.
 */
data class DesignTransform(val scale: Float, val dx: Float, val dy: Float) {
    fun map(x: Float, y: Float) = Offset(dx + x * scale, dy + y * scale)
}

fun contentFitTransform(container: IntSize, design: IntSize = IntSize(3048, 2032)): DesignTransform {
    val scale = min(
        container.width / design.width.toFloat(),
        container.height / design.height.toFloat()
    )
    return DesignTransform(
        scale = scale,
        dx = (container.width - design.width * scale) / 2f,
        dy = (container.height - design.height * scale) / 2f,
    )
}

/**
 * Subject medallion anchors in design pixels (3048×2032 reference canvas).
 * Calibrated to attach each medallion to the correct building in the village scene.
 */
val subjectAnchors = mapOf(
    "english" to Offset(610f, 780f),
    "filipino" to Offset(1570f, 820f),
    "mathematics" to Offset(2380f, 820f),
    "science" to Offset(640f, 1460f),
    "history" to Offset(1510f, 1510f),
    "gmrc" to Offset(2360f, 1470f),
    "cat-cafe" to Offset(2700f, 1750f),
)
