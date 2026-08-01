package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Playroom palette (from OD Playroom concept, bound to core Color.kt) ───
private val PlayTeal = Color(0xFF087F83)
private val PlayCoral = Color(0xFFF47C6B)
private val PlayGold = Color(0xFFF5B82E)
private val PlayLeaf = Color(0xFF57943B)
private val PlayPurple = Color(0xFF7653B5)
private val PlaySky = Color(0xFF218CC8)
private val PlayFilCoral = Color(0xFFD96555)
private val PlayHeritageGold = Color(0xFFB87916)
private val PlayGmrcTeal = Color(0xFF26A69A)
private val PlayInk = Color(0xFF183B4A)
private val PlayInkDark = Color(0xFF0B2A36)
private val PlayCream = Color(0xFFFFF7E8)
private val PlayMuted = Color(0xFF8A6A4A)

/** One activity island on the playroom grid. */
@Immutable
data class PlayroomIsland(
    val id: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    @DrawableRes val iconRes: Int,
    val locked: Boolean = false,
    val lockLevel: Int = 0,
)

/** Full-screen UI state for the Playroom home. */
@Immutable
data class PlayroomHomeState(
    val childName: String = "Maxine",
    val streak: Int = 12,
    val xp: Int = 240,
    val pawPrints: Int = 2,
    val pawPrintTotal: Int = 3,
    val stickerCollected: Int = 8,
    val stickerTotal: Int = 12,
    val questTitle: String = "Collect 3 paw prints today",
    val islands: List<PlayroomIsland> = defaultPlayroomIslands,
    @DrawableRes val stickerIcons: List<Int> = defaultPlayroomStickers,
)

val defaultPlayroomIslands = listOf(
    PlayroomIsland("english", "Story Time", "Read with Owly", PlayPurple, R.drawable.mw_ic_book),
    PlayroomIsland("mathematics", "Number Fun", "Count with Kiko", PlaySky, R.drawable.mw_ic_math),
    PlayroomIsland("filipino", "Kwentuhan", "Filipino stories & legends", PlayFilCoral, R.drawable.mw_ic_language),
    PlayroomIsland("science", "Discovery", "Explore with Tutti", PlayLeaf, R.drawable.mw_ic_science),
    PlayroomIsland("heritage-harbor", "Heritage", "Alamin ang Pilipinas", PlayHeritageGold, R.drawable.mw_ic_history),
    PlayroomIsland("gmrc", "Kindness", "Unlocks at Level 4", PlayGmrcTeal, R.drawable.mw_ic_heart, locked = true, lockLevel = 4),
)

val defaultPlayroomStickers = listOf(
    R.drawable.mw_ic_star, R.drawable.mw_ic_trophy, R.drawable.mw_ic_coin,
    R.drawable.mw_ic_book, R.drawable.mw_ic_math, R.drawable.mw_ic_language,
    R.drawable.mw_ic_science, R.drawable.mw_ic_heart,
)

/**
 * Playroom home screen — the OD Colorful design-system concept implemented in Compose.
 * Warm gold→coral canvas, activity islands, sticker strip, paw-print quest bar.
 */
@Composable
fun PlayroomHomeScreen(
    state: PlayroomHomeState,
    onDestinationClick: (String) -> Unit,
    onQuestClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onAvatarsClick: () -> Unit,
    onParentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFD76E), Color(0xFFFFB84D), PlayCoral))
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val expanded = maxHeight >= 780.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (expanded) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlayroomHeader(state, onParentsClick)
            PlayroomGreeting(state)
            PlayroomIslandGrid(state, onDestinationClick, islandHeight = if (expanded) 185.dp else 150.dp)
            PlayroomStickerStrip(state)
            PlayroomQuestBar(state, onQuestClick)
            PlayroomBottomNav(onHomeClick, onProgressClick, onAvatarsClick, onParentsClick)
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────

@Composable
private fun PlayroomHeader(state: PlayroomHomeState, onParentsClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(24.dp)) {
                    // simple paw print
                    val c = Color(0xFFB87916)
                    drawCircle(c, radius = size.minDimension * .30f, center = Offset(size.width / 2f, size.height * .72f))
                    drawCircle(c, radius = size.minDimension * .16f, center = Offset(size.width * .32f, size.height * .40f))
                    drawCircle(c, radius = size.minDimension * .16f, center = Offset(size.width * .68f, size.height * .40f))
                    drawCircle(c, radius = size.minDimension * .13f, center = Offset(size.width * .18f, size.height * .58f))
                    drawCircle(c, radius = size.minDimension * .13f, center = Offset(size.width * .82f, size.height * .58f))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Maxine's World",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    "VILLAGE · PLAYROOM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color(0xFF5C2E00),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlayroomPill(icon = R.drawable.mw_ic_star, iconTint = PlayCoral, value = "${state.streak}-day streak")
            PlayroomPill(icon = R.drawable.mw_ic_trophy, iconTint = PlayTeal, value = "${state.xp} XP")
        }
    }
}

@Composable
private fun PlayroomPill(@DrawableRes icon: Int, iconTint: Color, value: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White, RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, color = PlayInk, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
    }
}

// ─── Greeting ────────────────────────────────────────────────────────

@Composable
private fun PlayroomGreeting(state: PlayroomHomeState) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Kumusta, ${state.childName}!", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF5C2E00))
            Text(
                "Let's play & learn!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                "Choose an island below and start today's adventure.",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5C2E00),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SurfaceSpeechBubble("Tara, let's explore an island today!")
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Color.White, CircleShape)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.character_milo),
                    contentDescription = "Milo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
    }
}

@Composable
private fun SurfaceSpeechBubble(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
            .background(Color.White, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, color = PlayInk, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 2)
    }
}

// ─── Island grid ─────────────────────────────────────────────────────

@Composable
private fun PlayroomIslandGrid(
    state: PlayroomHomeState,
    onDestinationClick: (String) -> Unit,
    islandHeight: Dp = 150.dp,
) {
    state.islands.chunked(3).forEach { rowIslands ->
        Row(
            Modifier.fillMaxWidth().height(islandHeight),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            rowIslands.forEach { island ->
                PlayroomIslandCard(island, Modifier.weight(1f), onDestinationClick)
            }
            repeat(3 - rowIslands.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun PlayroomIslandCard(
    island: PlayroomIsland,
    modifier: Modifier = Modifier,
    onDestinationClick: (String) -> Unit,
) {
    val enabled = !island.locked
    val spoken = "${island.title}, ${island.subtitle}" +
        if (island.locked) ", locked until level ${island.lockLevel}" else ", tap to play"
    Card(
        modifier = modifier
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                role = Role.Button
                if (!enabled) disabled()
            }
            .clickable(enabled = enabled, role = Role.Button) { onDestinationClick(island.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight()) {
            if (island.locked) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = PlayInkDark,
                    contentColor = Color(0xFFFFE9A8),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp),
                ) {
                    Text("🔒 Lv ${island.lockLevel}", fontSize = 9.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(
                        if (island.locked) Color(0xFFEFECE3) else island.color.copy(alpha = .14f),
                        RoundedCornerShape(15.dp),
                    ).border(2.dp, if (island.locked) Color(0xFFE2DDD0) else island.color, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(island.iconRes),
                        null,
                        tint = if (island.locked) Color(0xFFB4AFA4) else island.color,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    island.title,
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    island.subtitle,
                    color = if (island.locked) Color(0xFF9AA0A3) else PlayMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (island.locked) Color(0xFFE8E3D4) else island.color,
                    contentColor = if (island.locked) Color(0xFF8AA0A7) else Color.White,
                    modifier = Modifier.heightIn(min = 34.dp),
                ) {
                    Text(
                        if (island.locked) "Locked" else "▶ Play",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

// ─── Sticker strip ───────────────────────────────────────────────────

@Composable
private fun PlayroomStickerStrip(state: PlayroomHomeState) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF9EA), RoundedCornerShape(20.dp))
            .border(2.dp, Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(110.dp)) {
            Text("🏅 Sticker book", color = Color(0xFF8A5B10), fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(
                "${state.stickerCollected} of ${state.stickerTotal} collected",
                color = Color(0xFF8A5B10), fontWeight = FontWeight.Bold, fontSize = 10.5.sp,
            )
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.stickerIcons.forEach { iconRes ->
                StickerSlot(iconRes, won = true)
            }
            repeat((state.stickerTotal - state.stickerCollected).coerceAtLeast(0)) {
                StickerSlot(null, won = false)
            }
        }
    }
}

@Composable
private fun StickerSlot(@DrawableRes iconRes: Int?, won: Boolean) {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            .background(
                if (won) Brush.linearGradient(listOf(Color(0xFFFFF6D6), Color(0xFFFFE9A8)))
                else Brush.linearGradient(listOf(Color(0xFFF6EFDC), Color(0xFFF1EAD8))),
                RoundedCornerShape(12.dp),
            )
            .border(2.dp, if (won) PlayGold else Color(0xFFD9C48F), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (won && iconRes != null) {
            Icon(painterResource(iconRes), null, tint = Color(0xFFB87916), modifier = Modifier.size(20.dp))
        } else {
            Text("?", color = Color(0xFFC9B273), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

// ─── Quest bar ───────────────────────────────────────────────────────

@Composable
private fun PlayroomQuestBar(state: PlayroomHomeState, onQuestClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlayInkDark, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .clickable(role = Role.Button, onClick = onQuestClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(26.dp)) {
            drawCircle(Color(0xFFFFE9A8), radius = size.minDimension * .30f, center = Offset(size.width / 2f, size.height * .72f))
            drawCircle(Color(0xFFFFE9A8), radius = size.minDimension * .16f, center = Offset(size.width * .32f, size.height * .40f))
            drawCircle(Color(0xFFFFE9A8), radius = size.minDimension * .16f, center = Offset(size.width * .68f, size.height * .40f))
            drawCircle(Color(0xFFFFE9A8), radius = size.minDimension * .13f, center = Offset(size.width * .18f, size.height * .58f))
            drawCircle(Color(0xFFFFE9A8), radius = size.minDimension * .13f, center = Offset(size.width * .82f, size.height * .58f))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(state.questTitle, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.5.sp)
            Text(
                "${state.pawPrints} of ${state.pawPrintTotal} collected",
                color = Color(0xFF9FD8CF), fontWeight = FontWeight.Bold, fontSize = 11.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(state.pawPrintTotal) { i ->
                Box(
                    Modifier.size(14.dp).clip(CircleShape)
                        .background(
                            if (i < state.pawPrints) PlayGold else Color(0x336DFFA),
                            CircleShape,
                        )
                        .border(
                            2.dp,
                            if (i < state.pawPrints) PlayGold else Color(0xFF3D5A63),
                            CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PlayTeal,
            contentColor = Color.White,
            modifier = Modifier.clickable(role = Role.Button, onClick = onQuestClick),
        ) {
            Text(
                "Start!",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            )
        }
    }
}

// ─── Bottom nav ──────────────────────────────────────────────────────

@Composable
private fun PlayroomBottomNav(
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onAvatarsClick: () -> Unit,
    onParentsClick: () -> Unit,
) {
    val items = listOf(
        Triple("Home", R.drawable.mw_ic_home, onHomeClick),
        Triple("Progress", R.drawable.mw_ic_progress, onProgressClick),
        Triple("Avatars", R.drawable.mw_ic_avatars, onAvatarsClick),
        Triple("Parents", R.drawable.mw_ic_lock, onParentsClick),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White, RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { (label, icon, action) ->
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = action),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(painterResource(icon), null, tint = PlayTeal, modifier = Modifier.size(22.dp))
                Text(label, color = PlayInk, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
            }
        }
    }
}
