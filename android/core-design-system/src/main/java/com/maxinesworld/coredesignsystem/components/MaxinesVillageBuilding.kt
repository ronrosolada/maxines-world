package com.maxinesworld.coredesignsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.*

/**
 * Data class for a village building destination card.
 * §3: destination cards — each building has a subject, progress, and unlock state.
 */
data class VillageBuildingData(
    val id: String,
    val label: String,
    val subject: String,
    val color: Color,
    val progress: Int,
    val total: Int,
    val locked: Boolean = false,
    val lockLevel: Int = 0,
    val isToday: Boolean = false
)

/**
 * §3 / §7.3: Village building destination card composable.
 *
 * Each building is a tappable button (minimum 88×88dp touch target) with:
 * - **Bottom-anchored building PNG** — the illustrated building sits at the base
 * - **Doorstep progress pill** — cream surface with colored progress bar + "8/12" count
 * - **Locked state** — gray pill with lock icon + "Level N" text
 * - **Today's focus** — 18dp raised elevation + gold ★ TODAY ribbon above the pill
 * - **Tactile press** — lifts 4dp on press with spring animation
 *
 * Uses theme tokens exclusively from [Color.kt]; does NOT refactor Theme.kt.
 *
 * @param buildingImage Painter for the building PNG (caller resolves via painterResource).
 * @param building Data model with label, subject, color, progress, locked state.
 * @param onClick Callback when the unlocked building is tapped (receives building.id).
 * @param modifier Optional modifier for the card container.
 * @param buildingWidth Width of the building PNG area.
 * @param buildingHeight Height of the building PNG area.
 */
@Composable
fun MaxinesVillageBuilding(
    buildingImage: Painter,
    building: VillageBuildingData,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    buildingWidth: Dp = 110.dp,
    buildingHeight: Dp = 100.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardElevation by animateDpAsState(
        targetValue = when {
            isPressed -> 22.dp          // 18dp TODAY base + 4dp press lift
            building.isToday -> 18.dp   // Today's focus: raised
            else -> 2.dp                // Default: lightly raised
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

    val pressLift by animateDpAsState(
        targetValue = if (isPressed) (-4).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
    )

    Card(
        modifier = modifier
            .offset(y = pressLift)
            .requiredSizeIn(minWidth = 88.dp, minHeight = 88.dp)
            .clickable(
                enabled = !building.locked,
                interactionSource = interactionSource,
                indication = null  // no ripple; tactile lift is the affordance
            ) { onClick(building.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            // ── Building PNG — bottom-anchored ──
            Box(
                modifier = Modifier
                    .size(buildingWidth, buildingHeight),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    painter = buildingImage,
                    contentDescription = "${building.label} building",
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.Unspecified  // preserve original PNG colors
                )
            }

            // ── TODAY focus ribbon ──
            if (building.isToday) {
                TodayFocusRibbon()
            }

            // ── Doorstep pill ──
            if (building.locked) {
                LockedDoorstepPill(lockLevel = building.lockLevel)
            } else {
                ProgressDoorstepPill(
                    progress = building.progress,
                    total = building.total,
                    color = building.color,
                    label = building.label,
                    subject = building.subject
                )
            }
        }
    }
}

// ─── Sub-components ─────────────────────────────────────────────

/**
 * Gold ★ TODAY ribbon that sits between the building PNG and the doorstep pill.
 * Only shown when the building is today's focus destination.
 */
@Composable
private fun TodayFocusRibbon() {
    Surface(
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        color = SunshineGold,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "★",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "TODAY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }
    }
}

/**
 * Cream-colored doorstep progress pill with colored bar and count (e.g. "8/12").
 * §7.3: progress pills — cream surface, subject-colored bar, progress fraction.
 */
@Composable
private fun ProgressDoorstepPill(
    progress: Int,
    total: Int,
    color: Color,
    label: String,
    subject: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Cream,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subject,
                fontSize = 9.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            // Colored progress bar + count
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / total.coerceAtLeast(1) },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.12f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$progress/$total",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            }
        }
    }
}

/**
 * Gray locked doorstep pill with lock icon + level requirement text.
 */
@Composable
private fun LockedDoorstepPill(lockLevel: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Ink.copy(alpha = 0.08f),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Ink.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Level $lockLevel",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Ink.copy(alpha = 0.5f)
            )
        }
    }
}
