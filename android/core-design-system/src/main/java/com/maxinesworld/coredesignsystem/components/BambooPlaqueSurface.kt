package com.maxinesworld.coredesignsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.R

/**
 * Uniform 176×72dp bamboo signpost plaque per correction spec v1.4.
 * Assembled from standard PNG primitives — no NinePatch dependency.
 */
@Composable
fun BambooPlaqueSurface(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sawali: Painter = painterResource(R.drawable.fill_sawali)
    val railH: Painter = painterResource(R.drawable.rail_bamboo_horizontal)
    val railV: Painter = painterResource(R.drawable.rail_bamboo_vertical)
    val cTL: Painter = painterResource(R.drawable.corner_rattan_tl)
    val cTR: Painter = painterResource(R.drawable.corner_rattan_tr)
    val cBL: Painter = painterResource(R.drawable.corner_rattan_bl)
    val cBR: Painter = painterResource(R.drawable.corner_rattan_br)

    Box(
        modifier = modifier
            .size(width = 176.dp, height = 72.dp)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                if (!enabled) disabled()
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        // Sawali fill center
        Image(
            sawali, null, modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 7.dp, vertical = 7.dp),
            contentScale = ContentScale.Crop, alpha = 0.14f,
        )

        // Bamboo rails
        Image(railH, null, Modifier.fillMaxWidth().height(12.dp).align(Alignment.TopCenter), contentScale = ContentScale.FillWidth)
        Image(railH, null, Modifier.fillMaxWidth().height(12.dp).align(Alignment.BottomCenter), contentScale = ContentScale.FillWidth)
        Image(railV, null, Modifier.fillMaxHeight().width(12.dp).align(Alignment.CenterStart), contentScale = ContentScale.FillHeight)
        Image(railV, null, Modifier.fillMaxHeight().width(12.dp).align(Alignment.CenterEnd), contentScale = ContentScale.FillHeight)

        // Rattan corners
        Image(cTL, null, Modifier.size(22.dp).align(Alignment.TopStart))
        Image(cTR, null, Modifier.size(22.dp).align(Alignment.TopEnd))
        Image(cBL, null, Modifier.size(22.dp).align(Alignment.BottomStart))
        Image(cBR, null, Modifier.size(22.dp).align(Alignment.BottomEnd))

        // Content
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 16.dp, top = 11.dp, end = 14.dp, bottom = 10.dp),
            content = content,
        )
    }
}
