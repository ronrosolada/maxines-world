package com.maxinesworld.featurelessonplayer

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val rewardEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Fish-treat reward celebration. Plays once per unique rewardEventId.
 * Reduced motion shows static icon + text immediately.
 */
@Composable
fun FishTreatCelebration(
    amount: Int,
    rewardEventId: String,
    reducedMotion: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reducedMotion) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🐟", fontSize = 48.sp)
            Text("You earned $amount fish treats!", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        }
        return
    }

    val scale = remember { Animatable(0.7f) }
    val sparkleAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val bounce = remember { Animatable(1.0f) }

    LaunchedEffect(rewardEventId) {
        // 1. Fish treat icon scales in
        scale.animateTo(1.08f, tween(300, easing = rewardEasing))
        scale.animateTo(1.0f, spring(Spring.DampingRatioMediumBouncy))
        // 2. Sparkles appear
        sparkleAlpha.animateTo(1f, tween(200))
        kotlinx.coroutines.delay(300)
        sparkleAlpha.animateTo(0f, tween(300))
        // 3. Text fades in
        textAlpha.animateTo(1f, tween(400, easing = rewardEasing))
        // 4. Mira bounce
        bounce.animateTo(1.08f, tween(250, easing = rewardEasing))
        bounce.animateTo(1.0f, spring(Spring.DampingRatioMediumBouncy))
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.scale(scale.value)) {
            Text("🐟", fontSize = 48.sp)
        }
        // Sparkle particles (simplified)
        if (sparkleAlpha.value > 0.05f) {
            Text("✨ ✨ ✨", modifier = Modifier.alpha(sparkleAlpha.value), fontSize = 14.sp)
        }
        Text(
            "You earned $amount fish treats!",
            modifier = Modifier.alpha(textAlpha.value),
            color = Color(0xFF075F63),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
        )
    }
}
