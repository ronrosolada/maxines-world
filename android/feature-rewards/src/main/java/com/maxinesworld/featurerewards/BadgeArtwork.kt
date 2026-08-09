package com.maxinesworld.featurerewards

import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.maxinesworld.coremodel.CollectibleBadge

/**
 * Renders the collected animal artwork, retaining a safe vector fallback for
 * milestone badges and any future catalog entry without an animal PNG.
 */
@Composable
internal fun BadgeArtwork(
    badge: CollectibleBadge,
    modifier: Modifier = Modifier,
    fallbackTint: Color,
) {
    val context = LocalContext.current
    @Suppress("DiscouragedApi")
    val resourceId = remember(badge.id, context.packageName) {
        context.resources.getIdentifier(
            badgeArtworkResourceName(badge.id),
            "drawable",
            context.packageName,
        )
    }

    if (resourceId != 0) {
        Image(
            painter = painterResource(resourceId),
            contentDescription = "${badge.name} sticker",
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        Icon(
            imageVector = Icons.Default.Pets,
            contentDescription = "${badge.name} sticker",
            tint = fallbackTint,
            modifier = modifier,
        )
    }
}

internal fun badgeArtworkResourceName(badgeId: String): String = "animal_$badgeId"
