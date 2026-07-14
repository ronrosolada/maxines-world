package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log

@Immutable
data class VillageDestination(
    val id: String,
    val name: String,
    val subject: String,
    val progressText: String,
    @DrawableRes val iconRes: Int,
    val enabled: Boolean = true,
    val recommended: Boolean = false,
)

val defaultDestinations = listOf(
    VillageDestination("english", "Story Tree", "Reading", "42%", R.drawable.mw_ic_book),
    VillageDestination("filipino", "Bahay ng Kuwento", "Filipino", "21%", R.drawable.mw_ic_language),
    VillageDestination("mathematics", "Number Market", "Mathematics", "67%", R.drawable.mw_ic_math, recommended = true),
    VillageDestination("science", "Discovery Lab", "Science", "17%", R.drawable.mw_ic_science),
    VillageDestination("history", "Heritage Harbor", "Araling Panlipunan", "14%", R.drawable.mw_ic_history),
    VillageDestination("gmrc", "Kindness Corner", "Values", "18%", R.drawable.mw_ic_heart),
)

private data class NRect(val x: Float, val y: Float, val w: Float, val h: Float)

private val destinationBounds = mapOf(
    "english" to NRect(.116f, .305f, .126f, .116f),
    "filipino" to NRect(.515f, .355f, .116f, .135f),
    "mathematics" to NRect(.823f, .350f, .164f, .112f),
    "science" to NRect(.075f, .605f, .135f, .120f),
    "history" to NRect(.425f, .686f, .143f, .122f),
    "gmrc" to NRect(.750f, .700f, .135f, .118f),
)

@Composable
fun VillageHomeV17Screen(
    state: VillageHomeState,
    onDestinationClick: (String) -> Unit,
    onMiraClick: () -> Unit,
    onDiscoveriesClick: () -> Unit,
    onCafeClick: () -> Unit,
    onParentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF173E38))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        if (maxWidth >= 840.dp) {
            val ratio = 1.5f
            val sceneWidth: Dp
            val sceneHeight: Dp
            if (maxWidth / maxHeight > ratio) {
                sceneHeight = maxHeight
                sceneWidth = sceneHeight * ratio
            } else {
                sceneWidth = maxWidth
                sceneHeight = sceneWidth / ratio
            }
            ExpandedVillage(
                state = state,
                sceneWidth = sceneWidth,
                sceneHeight = sceneHeight,
                onDestinationClick = onDestinationClick,
                onMiraClick = onMiraClick,
                onDiscoveriesClick = onDiscoveriesClick,
                onCafeClick = onCafeClick,
                onParentsClick = onParentsClick,
            )
        } else {
            CompactVillage(state, onDestinationClick, onMiraClick, onDiscoveriesClick, onCafeClick, onParentsClick)
        }
    }
}

@Composable
private fun ExpandedVillage(
    state: VillageHomeState,
    sceneWidth: Dp, sceneHeight: Dp,
    onDestinationClick: (String) -> Unit,
    onMiraClick: () -> Unit,
    onDiscoveriesClick: () -> Unit,
    onCafeClick: () -> Unit,
    onParentsClick: () -> Unit,
) {
    Box(Modifier.size(sceneWidth, sceneHeight).clip(RoundedCornerShape(2.dp))) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.White, fontSize = 18.sp)
            }
            return@Box
        }
        Image(painterResource(R.drawable.mw_village_scene_v17), null,
            contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())

        // Compact header bar
        NBox(NRect(.010f, .010f, .220f, .060f), sceneWidth, sceneHeight) { CompactHeader(state) }
        // Mira request bubble
        if (state.showMiraRequest) {
            NBox(NRect(.240f, .350f, .280f, .075f), sceneWidth, sceneHeight) {
                MiraBubble(onMiraClick)
            }
        }
        // Destination hotspots
        state.destinations.forEach { dest ->
            val bounds = destinationBounds[dest.id]
            if (bounds != null) {
                NBox(bounds, sceneWidth, sceneHeight) { DestinationHotspot(dest, onDestinationClick) }
            } else { Log.w("Village", "Unknown dest: ${dest.id}") }
        }
        // Bottom nav
        NBox(NRect(.108f, .858f, .805f, .085f), sceneWidth, sceneHeight) {
            BottomNav(onDiscoveriesClick, onCafeClick, onParentsClick)
        }
    }
}

// ── Compact Header ──

@Composable
private fun CompactHeader(state: VillageHomeState) {
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.mw_ic_profile), null, tint = Color(0xFFD15E7C), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(6.dp))
        Text("Hi, ${state.childName}", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        // Fish treat counter
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.mw_ic_coin), "Fish treats", tint = Color(0xFFF5A623), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text("${state.fishTreats}", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

// ── Mira Bubble ──

@Composable
private fun MiraBubble(onClick: () -> Unit) {
    val desc = "Mira needs help finishing a story. Tap to help."
    Row(Modifier.fillMaxSize()
        .clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF5DD))
        .semantics(mergeDescendants = true) { contentDescription = desc; role = Role.Button }
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.mw_ic_profile), null, tint = Color(0xFFD15E7C), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text("Mira needs help!", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Text("Can you help finish a story?", color = Color(0xFF34545A), fontSize = 9.sp, maxLines = 1)
        }
        Text("Help Mira", color = Color(0xFFF5A623), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

// ── Destination Hotspot ──

@Composable
private fun DestinationHotspot(dest: VillageDestination, onClick: (String) -> Unit) {
    val desc = "${dest.name}, ${dest.subject}, ${dest.progressText}" +
            if (dest.recommended) ", recommended today" else ""
    Box(Modifier.fillMaxSize()
        .semantics(mergeDescendants = true) { contentDescription = desc; role = Role.Button; if (!dest.enabled) disabled() }
        .clickable(enabled = dest.enabled, role = Role.Button) { onClick(dest.id) }
        .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (dest.recommended) Text("TODAY", color = Color(0xFFB87916), fontWeight = FontWeight.Black, fontSize = 7.sp)
            Text(dest.name, color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                textAlign = TextAlign.Center, maxLines = 2)
            Text(if (dest.enabled) "${dest.subject} · ${dest.progressText}" else "${dest.subject} · soon",
                color = Color(0xFF34545A), fontWeight = FontWeight.Bold, fontSize = 9.sp,
                textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ── Bottom Nav ──

@Composable
private fun BottomNav(onDiscoveries: () -> Unit, onCafe: () -> Unit, onParents: () -> Unit) {
    data class NavItem(val label: String, val icon: Int, val action: () -> Unit)
    val items = listOf(
        NavItem("Home", R.drawable.mw_ic_home, {}),
        NavItem("Discoveries", R.drawable.mw_ic_progress, onDiscoveries),
        NavItem("Cat Café", R.drawable.mw_ic_avatars, onCafe),
        NavItem("Parents", R.drawable.mw_ic_lock, onParents),
    )
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { item ->
            Column(Modifier.weight(1f).fillMaxHeight()
                .clickable(role = Role.Button, onClick = item.action),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(painterResource(item.icon), item.label, tint = Color(0xFF075F63), modifier = Modifier.size(22.dp))
                Text(item.label, color = Color(0xFF075F63), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

// ── Compact Layout ──

@Composable
private fun CompactVillage(
    state: VillageHomeState,
    onDestinationClick: (String) -> Unit,
    onMiraClick: () -> Unit,
    onDiscoveriesClick: () -> Unit,
    onCafeClick: () -> Unit,
    onParentsClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFFFF5DD))
        .verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Image(painterResource(R.drawable.mw_village_scene_v17), null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1.8f))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactHeader(state)
            if (state.showMiraRequest) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF2DEB6))
                    .clickable(onClick = onMiraClick).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.mw_ic_profile), null, tint = Color(0xFFD15E7C), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Mira needs help!", fontWeight = FontWeight.ExtraBold, color = Color(0xFF075F63))
                        Text("Can you help finish a story?", color = Color(0xFF34545A), fontSize = 12.sp)
                    }
                    Text("Help Mira →", color = Color(0xFFF5A623), fontWeight = FontWeight.Bold)
                }
            }
            state.destinations.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { d ->
                        Column(Modifier.weight(1f).heightIn(min = 100.dp)
                            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF2DEB6))
                            .clickable(enabled = d.enabled) { onDestinationClick(d.id) }.padding(12.dp),
                            verticalArrangement = Arrangement.Center) {
                            Icon(painterResource(d.iconRes), null, tint = Color(0xFF075F63), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(d.name, color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(if (d.enabled) "${d.subject} · ${d.progressText}" else "${d.subject} · soon",
                                color = Color(0xFF34545A), fontSize = 11.sp)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            BottomNav(onDiscoveriesClick, onCafeClick, onParentsClick)
        }
    }
}

@Composable
private fun NBox(rect: NRect, width: Dp, height: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.offset(x = width * rect.x, y = height * rect.y).size(width = width * rect.w, height = height * rect.h), content = content)
}
