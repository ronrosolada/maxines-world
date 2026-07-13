package com.maxinesworld.coredesignsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.R

/**
 * Bamboo-themed surface assembled from standard PNG primitives per §26.2.
 * Sawali fill tiles inside, bamboo rails line the edges, and rattan corners sit above.
 * Native content rendered on top. No NinePatch dependency.
 */
@Composable
fun BambooSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    railThickness: Dp = 12.dp,
    cornerSize: Dp = 22.dp,
    subjectAccent: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val sawali: Painter = painterResource(R.drawable.fill_sawali)
    val railH: Painter = painterResource(R.drawable.rail_bamboo_horizontal)
    val railV: Painter = painterResource(R.drawable.rail_bamboo_vertical)
    val cornerTL: Painter = painterResource(R.drawable.corner_rattan_tl)
    val cornerTR: Painter = painterResource(R.drawable.corner_rattan_tr)
    val cornerBL: Painter = painterResource(R.drawable.corner_rattan_bl)
    val cornerBR: Painter = painterResource(R.drawable.corner_rattan_br)

    Surface(
        modifier = modifier.clearAndSetSemantics {},
        shape = shape,
        color = Color.Transparent,
    ) {
        Box {
            // Sawali fill
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Color(0xFFFFF7E8)) // Cream fallback
            ) {
                Image(sawali, null, Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop, alpha = 0.12f)
            }

            // Accent top rail
            if (subjectAccent != null) {
                Box(Modifier.fillMaxWidth().height(6.dp).align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(subjectAccent))
            }

            // Bamboo rails (decorative, exclude from semantics)
            // Top rail
            Image(railH, null,
                Modifier.fillMaxWidth().height(railThickness).align(Alignment.TopCenter),
                contentScale = ContentScale.FillWidth)

            // Bottom rail
            Image(railH, null,
                Modifier.fillMaxWidth().height(railThickness).align(Alignment.BottomCenter),
                contentScale = ContentScale.FillWidth)

            // Left rail
            Image(railV, null,
                Modifier.fillMaxHeight().width(railThickness).align(Alignment.CenterStart),
                contentScale = ContentScale.FillHeight)

            // Right rail
            Image(railV, null,
                Modifier.fillMaxHeight().width(railThickness).align(Alignment.CenterEnd),
                contentScale = ContentScale.FillHeight)

            // Rattan corners (above rails in z-order)
            Image(cornerTL, null,
                Modifier.size(cornerSize).align(Alignment.TopStart))

            Image(cornerTR, null,
                Modifier.size(cornerSize).align(Alignment.TopEnd))

            Image(cornerBL, null,
                Modifier.size(cornerSize).align(Alignment.BottomStart))

            Image(cornerBR, null,
                Modifier.size(cornerSize).align(Alignment.BottomEnd))

            // Native content
            Box(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}
