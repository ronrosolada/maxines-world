package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.playground.PlaygroundGateStatus

/**
 * One-shot celebration dialog displayed when the Playground transitions from
 * locked/in-progress to unlocked. Keyed by `shouldShow: Boolean` and consumed
 * via [onDismiss]; the ViewModel is responsible for preventing replays.
 *
 * Reduced-motion mode renders the final static state without bounce, particles,
 * or pulsing — the scale animation is skipped and TalkBack focus is stable.
 */
@Composable
fun PlaygroundUnlockCelebration(
    status: PlaygroundGateStatus,
    onEnter: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (reducedMotion) 1f else 1.05f,
        animationSpec = if (reducedMotion) tween(0) else tween(400),
        label = "celebration-scale",
    )

    AlertDialog(
        modifier = modifier.scale(scale),
        onDismissRequest = onEnter,
        title = {
            Text(
                text = "🎉 Playground Unlocked!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text(
                    text = "You completed all 3 quests today!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Time to play in the village playground.\nPick a game and have fun!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEnter) {
                Text(
                    text = "Let's Go!" + if (status == PlaygroundGateStatus.Locked) " 🚀" else " 🎮",
                    fontSize = 16.sp,
                )
            }
        },
    )
}
