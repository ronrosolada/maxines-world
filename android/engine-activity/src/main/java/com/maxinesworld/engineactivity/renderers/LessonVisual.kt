package com.maxinesworld.engineactivity.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep

/** Returns the first non-empty authored SVG asset for an activity. */
internal fun lessonVisualAssetId(step: ActivityStep): String? =
    step.imageAssets.firstOrNull { it.isNotBlank() }

/**
 * Shared authored visual layer for lesson activities.
 *
 * The accessible text and interaction remain the source of truth. If the SVG
 * is absent or cannot be opened, this composable contributes no visible
 * content instead of breaking the activity.
 */
@Composable
fun LessonVisual(
    step: ActivityStep,
    modifier: Modifier = Modifier,
) {
    val assetId = lessonVisualAssetId(step) ?: return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AssetSvgPreview(
            assetId = assetId,
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .aspectRatio(640f / 360f)
                .clip(RoundedCornerShape(16.dp))
                .semantics {
                    contentDescription = "Lesson picture"
                },
        )
    }
}
