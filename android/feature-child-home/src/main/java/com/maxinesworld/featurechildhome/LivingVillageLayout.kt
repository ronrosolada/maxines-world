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

data class DesignPoint(val x: Float, val y: Float)

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
 * Exactly six subject medallion anchors in design pixels (3048×2032).
 * Cat Café and Playground are NOT subject destinations.
 */
val subjectAnchors = mapOf(
    "english" to Offset(610f, 780f),
    "filipino" to Offset(1570f, 820f),
    "mathematics" to Offset(2380f, 820f),
    "science" to Offset(640f, 1460f),
    "history" to Offset(1510f, 1510f),
    "gmrc" to Offset(2360f, 1470f),
)

/** Non-subject world destination anchors */
val playgroundAnchor = DesignPoint(x = 340f, y = 1160f)
val catCafeAnchor = DesignPoint(x = 2690f, y = 1570f)
