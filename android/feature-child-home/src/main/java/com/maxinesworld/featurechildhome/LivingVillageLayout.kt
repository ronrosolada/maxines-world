package com.maxinesworld.featurechildhome

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Converts design coordinates (3048×2032) into pixel positions within a container,
 * preserving aspect ratio via letterbox/pillarbox centering.
 */
data class DesignTransform(val scale: Float, val dx: Float, val dy: Float) {
    fun map(x: Float, y: Float) = Offset(dx + x * scale, dy + y * scale)
}

data class DesignPoint(val x: Float, val y: Float)

data class VillageUiMetrics(
    val scale: Float,
    val medallionSize: Dp,
    val selectedMedallionSize: Dp,
    val lessonLabelWidth: Dp,
    val oneLineLabelHeight: Dp,
    val twoLineLabelHeight: Dp,
    val lessonLabelFontSp: Float,
    val lessonLabelLineHeightSp: Float,
    val worldMarkerSize: Dp,
    val worldLabelWidth: Dp,
    val miraHeight: Dp,
    val questBookWidth: Dp,
)

fun villageUiMetrics(
    viewportWidthDp: Float,
    viewportHeightDp: Float,
): VillageUiMetrics {
    require(viewportWidthDp > 0f)
    require(viewportHeightDp > 0f)

    val shortSide = min(viewportWidthDp, viewportHeightDp)
    val scale = (shortSide / 720f).coerceIn(0.58f, 1.08f)

    return VillageUiMetrics(
        scale = scale,
        medallionSize = (88f * scale).dp,
        selectedMedallionSize = (104f * scale).dp,
        lessonLabelWidth = (156f * scale).coerceIn(104f, 168f).dp,
        oneLineLabelHeight = (32f * scale).coerceAtLeast(26f).dp,
        twoLineLabelHeight = (44f * scale).coerceAtLeast(36f).dp,
        lessonLabelFontSp = (13f * scale).coerceIn(11f, 14f),
        lessonLabelLineHeightSp = (15f * scale).coerceIn(13f, 17f),
        worldMarkerSize = (96f * scale).coerceIn(58f, 104f).dp,
        worldLabelWidth = (144f * scale).coerceIn(96f, 156f).dp,
        miraHeight = (224f * scale).coerceIn(176f, 224f).dp,
        questBookWidth = (360f * scale).coerceIn(292f, 360f).dp,
    )
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
