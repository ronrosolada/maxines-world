package com.maxinesworld.featurerewards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coredesignsystem.theme.Ink

@Composable
internal fun BadgePhotoArtwork(
    badge: CollectibleBadge,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    @Suppress("DiscouragedApi")
    val resourceId = remember(badge.photoAsset, context.packageName) {
        badge.photoAsset?.let { asset ->
            context.resources.getIdentifier(asset, "drawable", context.packageName)
        } ?: 0
    }

    if (resourceId != 0) {
        Image(
            painter = painterResource(resourceId),
            contentDescription = if (badge.photoKind == "official_stamp") {
                "Official stamp of ${badge.name}"
            } else {
                "Real-life photo of ${badge.name}"
            },
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp),
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = Ink.copy(alpha = 0.35f))
                Text(
                    "Real-life photo coming soon",
                    color = Ink.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

internal fun badgePhotoKindLabel(kind: String?): String = when (kind) {
    "museum_specimen_photo" -> "Museum specimen photo"
    "captive_photo" -> "Photo from a wildlife facility"
    "field_photo" -> "Field photo"
    "market_photo" -> "Market photo"
    "official_stamp" -> "Official Philippine stamp"
    else -> "Photo reference"
}
