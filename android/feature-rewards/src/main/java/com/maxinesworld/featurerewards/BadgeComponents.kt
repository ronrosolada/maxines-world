package com.maxinesworld.featurerewards

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coremodel.BadgeBiome
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coredesignsystem.theme.*

/**
 * Displays a single collectible badge card — either collected (full color) or locked (silhouette).
 */
@Composable
fun BadgeCard(
    badge: CollectibleBadge,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val biome = BadgeBiome.fromId(badge.biome)
    val accentColor = Color(biome.colorHex)

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isCollected) accentColor.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isCollected) 4.dp else 0.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge icon area
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (badge.isCollected) accentColor.copy(alpha = 0.2f)
                        else Color.Black.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (badge.isCollected) {
                    Text(badge.emoji, fontSize = 32.sp)
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Black.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                badge.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (badge.isCollected) accentColor else Ink.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2
            )
            Text(
                badge.name,
                fontSize = 11.sp,
                color = if (badge.isCollected) Ink.copy(alpha = 0.6f) else Ink.copy(alpha = 0.3f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Expanded badge detail — shows the fun fact and collection status.
 */
@Composable
fun BadgeDetailSheet(
    badge: CollectibleBadge,
    onDismiss: () -> Unit
) {
    val biome = BadgeBiome.fromId(badge.biome)
    val accentColor = Color(biome.colorHex)

    Card(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Close button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Ink.copy(alpha = 0.5f))
                }
            }

            // Badge icon
            Box(
                Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.emoji, fontSize = 48.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Title and name
            Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = accentColor, textAlign = TextAlign.Center)
            Text(badge.name, fontSize = 16.sp, color = Ink.copy(alpha = 0.6f))

            Spacer(Modifier.height(12.dp))

            // Fun fact
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, "Fun Fact", tint = SunshineGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(badge.funFact, fontSize = 15.sp, color = Ink.copy(alpha = 0.85f), lineHeight = 22.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Biome info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Park, "Biome", tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${biome.displayName} • ${biome.description}", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(12.dp))

            // Collect button or status
            if (badge.isCollected) {
                AssistChip(
                    onClick = {},
                    label = { Text("Collected!", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Star, null, tint = SunshineGold, modifier = Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SunshineGold.copy(alpha = 0.15f))
                )
            } else {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.04f))
                ) {
                    Text(
                        "Complete more lessons to unlock this badge!",
                        Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = Ink.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Badge biome section — shows a horizontal row of badge cards with a biome header.
 */
@Composable
fun BadgeBiomeSection(
    biome: BadgeBiome,
    badges: List<CollectibleBadge>,
    collectedCount: Int,
    totalCount: Int,
    onBadgeClick: (CollectibleBadge) -> Unit
) {
    val accentColor = Color(biome.colorHex)

    Column(Modifier.padding(vertical = 12.dp)) {
        // Biome header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Park, biome.displayName, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(biome.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = accentColor, modifier = Modifier.weight(1f))
            Text("$collectedCount/$totalCount", fontSize = 14.sp, color = if (collectedCount == totalCount) SunshineGold else Ink.copy(alpha = 0.4f))
        }

        Spacer(Modifier.height(8.dp))

        // Badge grid
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { badge ->
                BadgeCard(
                    badge = badge,
                    modifier = Modifier.width(110.dp),
                    onClick = { onBadgeClick(badge) }
                )
            }
        }
    }
}
