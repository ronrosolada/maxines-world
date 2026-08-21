package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardHeader
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardSurface
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.SanctuaryCatalog
import kotlin.math.roundToInt


// ─── Header (design.md §9) ───────────────────────────────────────────

private fun keepsakeIcon(iconKey: String): ImageVector = when (iconKey) {
    "tree", "garden", "meadow", "flower", "park" -> Icons.Default.Park
    else -> Icons.Default.Pets
}

@Composable
internal fun PlayroomHeader(
    childName: String,
    offline: Boolean,
    wide: Boolean,
    starBalance: Int = 0,
    coinBalance: Int = 0,
    streakDays: Int? = null,
    keepsakes: List<KeepsakeUi> = emptyList(),
) {
    if (wide) {
        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandBlock(Modifier.width(210.dp))
            GreetingBlock(childName, Modifier.widthIn(min = 250.dp).padding(horizontal = 16.dp))
            BalanceChips(starBalance, coinBalance, Modifier.weight(1f))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandBlock(Modifier.weight(1f))
            BalanceChips(starBalance, coinBalance)
        }
        GreetingBlock(childName, Modifier.fillMaxWidth().padding(top = 8.dp))
    }
    KeepsakesStrip(keepsakes)
    streakDays?.let { LearningStreakCard(it) }
    if (offline) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = PlayInkDark, contentColor = PlayWhite,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                stringResource(R.string.home_offline_chip),
                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/** True only when a positive streak may use the optional celebratory pop. */
internal fun shouldCelebrateStreak(streakDays: Int, animationsDisabled: Boolean): Boolean =
    streakDays > 0 && !animationsDisabled

/** Child-facing, informational learning-day progress; it never gates or rewards. */
@Composable
internal fun LearningStreakCard(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val days = streakDays.coerceAtLeast(0)
    val animationsDisabled = LocalAnimationsDisabled.current
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val cardScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(days, animationsDisabled) {
        if (shouldCelebrateStreak(days, animationsDisabled)) {
            cardScale.snapTo(0.96f)
            cardScale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            cardScale.snapTo(1f)
        }
    }

    val title = if (days > 0) {
        pluralStringResource(R.plurals.home_streak_days_learning, days, days)
    } else {
        stringResource(R.string.home_streak_zero_title)
    }
    val explanation = stringResource(R.string.home_streak_explanation)
    val accessibleLabel = if (days > 0) {
        "$title. $explanation"
    } else {
        "${stringResource(R.string.home_streak_no_learning_days)} $title. $explanation"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = cardScale.value, scaleY = cardScale.value)
            .testTag(PlayroomHomeTestTags.Streak)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.home_streak_tap_label),
                onClick = { showDetails = true },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleLabel
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlayCream.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PlayCoral.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = PlayCoral,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    explanation,
                    color = PlayMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
            }
            Text(
                "›",
                color = PlayTeal,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(stringResource(R.string.home_streak_dialog_title)) },
            text = {
                Text(
                    if (days > 0) {
                        pluralStringResource(R.plurals.home_streak_dialog_positive, days, days)
                    } else {
                        stringResource(R.string.home_streak_dialog_zero)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text(stringResource(R.string.home_streak_dialog_dismiss))
                }
            },
        )
    }
}

/** Child-visible learning stars and sanctuary tokens — earned work stays visible. */
@Composable
private fun BalanceChips(stars: Int, coins: Int, modifier: Modifier = Modifier) {
    val reduceMotion = LocalAnimationsDisabled.current
    // P2 fix: odometer pop keyed to value change so it actually animates; was always 1f->1f.
    // Animate on stars/coins change via keyed Animatable
    val balScale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(stars, coins) {
        if (reduceMotion) {
            balScale.snapTo(1f)
        } else {
            balScale.snapTo(0.88f)
            balScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Row(modifier.graphicsLayer(scaleX = balScale.value, scaleY = balScale.value), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BalanceChip("stars", "★", stars, PlaySunshine)
        BalanceChip("sanctuary tokens", "●", coins, PlayTeal)
    }
}

/**
 * Owned Treat Shop keepsakes — the visible payoff for spending coins. Hidden
 * entirely until the child owns at least one item; every purchase then shows
 * up on the home so the reward loop always ends in something to see.
 */
@Composable
private fun KeepsakesStrip(keepsakes: List<KeepsakeUi>) {
    if (keepsakes.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.home_milo_decorations),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlayroomColors.KeepsakeHeading,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(keepsakes, key = { it.itemId }) { keepsake ->
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    shadowElevation = 1.dp,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Milo's keepsake: ${keepsake.name}"
                    },
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = keepsakeIcon(keepsake.iconKey),
                            contentDescription = null,
                            tint = PlayTeal,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            keepsake.name,
                            color = PlayInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceChip(label: String, icon: String, value: Int, tint: Color) {
    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$value $label"
        },
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = 0.85f),
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(icon, fontSize = 16.sp, color = tint, fontWeight = FontWeight.Black)
            Text(
                "$value",
                color = PlayInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun BrandBlock(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            PawGlyph(PlayHeritageGold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(R.string.home_brand),
                color = PlayInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp, lineHeight = 24.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.home_playroom_label),
                fontSize = 12.sp, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, color = PlayroomColors.BrandLabel,
            )
        }
    }
}

private val PlayHeritageGold = HeritageGold

@Composable
private fun GreetingBlock(childName: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            stringResource(
                if (childName.isBlank()) R.string.home_greeting_fallback
                else R.string.home_greeting, childName
            ),
            color = PlayInk,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp, lineHeight = 32.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(R.string.home_encouragement),
            color = PlayroomColors.KeepsakeHeading,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp, lineHeight = 21.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, color = PlayInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 24.sp)
            Text(label, color = PlayMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 18.sp)
        }
    }
}
// ─── Glyphs ──────────────────────────────────────────────────────────

@Composable
internal fun PawGlyph(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val c = color
        drawCircle(c, radius = size.toPx() * .30f, center = Offset(this.size.width / 2f, this.size.height * .72f))
        drawCircle(c, radius = size.toPx() * .16f, center = Offset(this.size.width * .32f, this.size.height * .40f))
        drawCircle(c, radius = size.toPx() * .16f, center = Offset(this.size.width * .68f, this.size.height * .40f))
        drawCircle(c, radius = size.toPx() * .13f, center = Offset(this.size.width * .18f, this.size.height * .58f))
        drawCircle(c, radius = size.toPx() * .13f, center = Offset(this.size.width * .82f, this.size.height * .58f))
    }
}
