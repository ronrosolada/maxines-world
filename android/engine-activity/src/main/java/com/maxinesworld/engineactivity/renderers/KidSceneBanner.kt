package com.maxinesworld.engineactivity.renderers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.VillageTeal

/**
 * Big kid-friendly scene banner driven by [imageAssets] emoji packs.
 * Soft bob animation unless [reducedMotion] is true.
 */
@Composable
fun KidSceneBanner(
    imageAssets: List<String>,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scene = imageAssets.firstOrNull { !it.startsWith("asset:") && it.isNotBlank() }
        ?: "🐱📚✨"

    val bobY = if (reducedMotion) {
        0f
    } else {
        val infinite = rememberInfiniteTransition(label = "kidScene")
        val bob by infinite.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bob"
        )
        bob
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        VillageTeal.copy(alpha = 0.18f),
                        SunshineGold.copy(alpha = 0.22f),
                        Color(0xFFE1BEE7).copy(alpha = 0.25f),
                    )
                )
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = scene,
            fontSize = 44.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = bobY.dp)
        )
    }
}

@Composable
fun CelebratePulse(
    active: Boolean,
    reducedMotion: Boolean = false,
    content: @Composable (Modifier) -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (active && !reducedMotion) 1.06f else 1f,
        animationSpec = tween(220),
        label = "celebrate"
    )
    content(Modifier.scale(scale))
}
