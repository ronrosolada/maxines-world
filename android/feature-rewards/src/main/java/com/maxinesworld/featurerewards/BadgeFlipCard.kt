package com.maxinesworld.featurerewards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.White
import com.maxinesworld.coremodel.CollectibleBadge

/**
 * A child-friendly coin-flip card: the sticker is the front, and the back
 * contains a real-life reference image, a short fact, and photo credit.
 */
@Composable
internal fun BadgeFlipCard(
    badge: CollectibleBadge,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalAnimationsDisabled.current
    var flipped by rememberSaveable(badge.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(550, easing = FastOutSlowInEasing),
        label = "badgeFlip",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val frontVisible = rotation < 90f

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            }
            .clickable {
                flipped = !flipped
            }
            .semantics {
                role = Role.Button
                contentDescription = if (flipped) {
                    "Real-life photo of ${badge.name}. Tap to show the sticker."
                } else {
                    "${badge.name} sticker. Tap to see a real-life photo."
                }
                stateDescription = if (flipped) "Showing real-life photo" else "Showing sticker"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (flipped) White else accentColor.copy(alpha = 0.1f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        if (frontVisible) {
            BadgeFlipFront(badge = badge, accentColor = accentColor)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
            ) {
                BadgeFlipBack(badge = badge, accentColor = accentColor)
            }
        }
    }
}

@Composable
private fun BadgeFlipFront(
    badge: CollectibleBadge,
    accentColor: Color,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BadgeArtwork(
            badge = badge,
            modifier = Modifier.size(112.dp),
            fallbackTint = accentColor,
        )
        Spacer(Modifier.height(8.dp))
        Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor)
        Text(badge.name, fontSize = 13.sp, color = Ink.copy(alpha = 0.65f))
        Spacer(Modifier.height(12.dp))
        Text(
            "Tap to see a real-life photo",
            fontSize = 12.sp,
            color = accentColor.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun BadgeFlipBack(
    badge: CollectibleBadge,
    accentColor: Color,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgePhotoArtwork(
            badge = badge,
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text("Fun fact", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SunshineGold)
        Spacer(Modifier.height(2.dp))
        Text(
            badge.funFact,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Ink.copy(alpha = 0.85f),
        )
        badge.photoCredit?.takeIf { it.isNotBlank() }?.let { credit ->
            Spacer(Modifier.height(8.dp))
            Text(
                "${badgePhotoKindLabel(badge.photoKind)} · Photo credit: $credit" +
                    (badge.photoLicense?.let { " · $it" } ?: ""),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = Ink.copy(alpha = 0.5f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap to see the sticker again",
            fontSize = 11.sp,
            color = accentColor.copy(alpha = 0.75f),
        )
    }
}
