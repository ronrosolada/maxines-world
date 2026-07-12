package com.maxinesworld.featurerewards

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.maxinesworld.coremodel.BadgeBiome
import com.maxinesworld.coremodel.CollectibleBadge
import com.maxinesworld.coredesignsystem.theme.*

/**
 * Pokémon-style badge card — silhouette until earned, full color when collected.
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
        modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isCollected) accentColor.copy(alpha = 0.1f)
                else Color.Black.copy(alpha = 0.04f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isCollected) 4.dp else 0.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Badge token
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (badge.isCollected) {
                    // Earned: colorful + scalloped glow
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(accentColor.copy(alpha = 0.2f), radius = size.minDimension / 2)
                        drawCircle(accentColor.copy(alpha = 0.08f), radius = size.minDimension / 2.3f)
                    }
                    Text(badge.emoji, fontSize = 32.sp)
                } else {
                    // Locked: dark silhouette token — no emoji
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2
                        drawCircle(Color.Black.copy(alpha = 0.2f), radius = r * 0.9f)
                        drawCircle(Color.Black.copy(alpha = 0.35f), radius = r, style = Stroke(width = 2.5f))
                        // Mystery paw shape
                        val paw = Path().apply {
                            moveTo(r, r * 0.4f)
                            // Main pad
                            addOval(Rect(r * 0.55f, r * 0.5f, r * 1.45f, r * 1.3f))
                        }
                        drawPath(paw, Color.Black.copy(alpha = 0.12f))
                    }
                    Icon(Icons.Default.Lock, "Undiscovered", tint = Color.Black.copy(alpha = 0.18f),
                        modifier = Modifier.size(20.dp).align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(8.dp))

            if (badge.isCollected) {
                // Earned: show title and name
                Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor,
                    textAlign = TextAlign.Center, lineHeight = 16.sp, maxLines = 2)
                Text(badge.name, fontSize = 11.sp, color = Ink.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center, maxLines = 1)
            } else {
                // Locked: "Undiscovered"
                Text("???", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center)
                Text("Undiscovered", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.15f),
                    textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Expanded badge detail — only accessible for earned badges.
 */
@Composable
fun BadgeDetailSheet(badge: CollectibleBadge, onDismiss: () -> Unit) {
    val biome = BadgeBiome.fromId(badge.biome)
    val accentColor = Color(biome.colorHex)

    Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = Ink.copy(alpha = 0.5f)) }
            }
            Box(Modifier.size(100.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                if (badge.isCollected) Text(badge.emoji, fontSize = 48.sp)
                else Icon(Icons.Default.Lock, "Locked", tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (badge.isCollected) {
                Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = accentColor, textAlign = TextAlign.Center)
                Text(badge.name, fontSize = 16.sp, color = Ink.copy(alpha = 0.6f))
            } else {
                Text("Undiscovered Animal", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black.copy(alpha = 0.3f))
                Text("Complete the Daily Challenge to reveal!", fontSize = 14.sp, color = Ink.copy(alpha = 0.4f))
            }
            Spacer(Modifier.height(12.dp))
            if (badge.isCollected) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Lightbulb, "Fun Fact", tint = SunshineGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(badge.funFact, fontSize = 15.sp, color = Ink.copy(alpha = 0.85f), lineHeight = 22.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${biome.displayName} • ${biome.description}", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                AssistChip(onClick = {}, label = { Text("Collected!", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Star, null, tint = SunshineGold, modifier = Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SunshineGold.copy(alpha = 0.15f)))
            } else {
                Text("Complete one module in each subject today to earn this badge.", fontSize = 14.sp,
                    color = Ink.copy(alpha = 0.5f), textAlign = TextAlign.Center)
            }
        }
    }
}
