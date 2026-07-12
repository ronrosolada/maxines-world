package com.maxinesworld.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.*

@Composable
fun RewardHubScreen(
    childId: String,
    rewardBreakId: String,
    onPlayCatCafe: () -> Unit,
    onPlayParkour: () -> Unit,
    onReturnToVillage: () -> Unit
) {
    var remainingMillis by rememberSaveable { mutableStateOf(300_000L) }
    val breakExpired = remainingMillis <= 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Great work today!",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = VillageTeal,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You've earned a ${remainingMillis / 60_000}-minute reward break. Choose a game!",
            fontSize = 18.sp,
            color = Ink.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
        ) {
            GameCard(
                title = "Cat Café Dash",
                icon = Icons.Default.Restaurant,
                color = Coral,
                description = "Serve yummy food to animal friends!",
                enabled = !breakExpired,
                onClick = onPlayCatCafe
            )
            GameCard(
                title = "Pawprint Parkour",
                icon = Icons.Default.DirectionsRun,
                color = SkyBlue,
                description = "Jump and run with Milo!",
                enabled = !breakExpired,
                onClick = onPlayParkour
            )
        }

        Spacer(Modifier.height(32.dp))

        val minutesLeft = remainingMillis / 60_000
        val secondsLeft = (remainingMillis % 60_000) / 1000
        Text(
            "${minutesLeft}:${secondsLeft.toString().padStart(2, '0')} remaining",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (breakExpired) Coral else VillageTeal
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onReturnToVillage,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.Home, "Village")
            Spacer(Modifier.width(8.dp))
            Text("Return to Village", fontSize = 18.sp)
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { if (enabled) onClick() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = color, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                fontSize = 15.sp,
                color = Ink.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
