package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.maxinesworld.coremodel.gamification.FishTreatPolicy
import android.util.Log

@Immutable
data class VillageDestinationV17(
    val id: String,
    val name: String,
    val subject: String,
    val progressText: String,
    @DrawableRes val iconRes: Int,
    val enabled: Boolean = true,
    val recommended: Boolean = false,
)

val defaultVillageDestinationsV17 = listOf(
    VillageDestinationV17("english", "Story Tree", "Reading", "42%", R.drawable.mw_ic_book),
    VillageDestinationV17("filipino", "Bahay ng Kuwento", "Filipino", "21%", R.drawable.mw_ic_language),
    VillageDestinationV17("mathematics", "Number Market", "Mathematics", "67%", R.drawable.mw_ic_math, recommended = true),
    VillageDestinationV17("science", "Discovery Lab", "Science", "17%", R.drawable.mw_ic_science),
    VillageDestinationV17("history", "Heritage Harbor", "Araling Panlipunan", "14%", R.drawable.mw_ic_history),
    VillageDestinationV17("gmrc", "Kindness Corner", "Values", "18%", R.drawable.mw_ic_heart),
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
    state: VillageHomeV17State,
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
            ExpandedVillageV17(
                state = state,
                sceneWidth = sceneWidth,
                sceneHeight = sceneHeight,
                onDestinationClick = onDestinationClick,
                onQuestClick = onQuestClick,
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onAvatarsClick = onAvatarsClick,
                onParentsClick = onParentsClick,
            )
        } else {
            CompactVillageV17(state, onDestinationClick, onQuestClick)
        }
    }
}

@Composable
private fun ExpandedVillageV17(
    state: VillageHomeV17State,
    sceneWidth: Dp,
    sceneHeight: Dp,
    onDestinationClick: (String) -> Unit,
    onQuestClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onAvatarsClick: () -> Unit,
    onParentsClick: () -> Unit,
) {
    Box(Modifier.size(sceneWidth, sceneHeight).clip(RoundedCornerShape(2.dp))) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
                Text("Loading...", color = Color.White, fontSize = 18.sp)
            }
            return@Box
        }
        Image(
            painter = painterResource(R.drawable.mw_village_scene_v17),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        NBox(NRect(.026f, .025f, .205f, .115f), sceneWidth, sceneHeight) {
            ProfileOverlayV17(state)
        }
        NBox(NRect(.027f, .162f, .205f, .112f), sceneWidth, sceneHeight) {
            QuestOverlayV17(state, onQuestClick)
        }
        RewardOverlayV17(state, sceneWidth, sceneHeight)
        // Mira request card
        if (state.showMiraRequest) {
            NBox(NRect(.30f, .45f, .40f, .11f), sceneWidth, sceneHeight) {
                MiraRequestOverlay { onDestinationClick("english") }
            }
        }
        // Café unlock progress
        if (!state.cafeUnlock.isPurchased && state.fishTreats > 0) {
            NBox(NRect(.70f, .05f, .28f, .06f), sceneWidth, sceneHeight) {
                CafeProgressOverlay(state.cafeUnlock)
            }
        }
        state.destinations.forEach { destination ->
            val bounds = destinationBounds[destination.id]
            if (bounds != null) {
                NBox(bounds, sceneWidth, sceneHeight) {
                    DestinationOverlayV17(destination, onDestinationClick)
                }
            } else {
                Log.w("VillageV17", "Unknown destination id: ${destination.id}")
            }
        }
        NBox(NRect(.108f, .858f, .805f, .085f), sceneWidth, sceneHeight) {
            NavigationOverlayV17(onHomeClick, onProgressClick, onAvatarsClick, onParentsClick)
        }
    }
}

@Composable
private fun NBox(rect: NRect, width: Dp, height: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = width * rect.x, y = height * rect.y)
            .size(width = width * rect.w, height = height * rect.h),
        content = content,
    )
}

@Composable
private fun ProfileOverlayV17(state: VillageHomeV17State) {
    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.mw_ic_profile), null, tint = Color(0xFFD15E7C), modifier = Modifier.size(42.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("Hi, ${state.childName}!", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            if (state.targetXp > 0) {
                Text("${state.currentXp} / ${state.targetXp} XP", color = Color(0xFF34545A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x5536545A))) {
                    Box(Modifier.fillMaxWidth((state.currentXp.toFloat() / state.targetXp).coerceIn(0f,1f)).fillMaxHeight().background(Color(0xFFF5A623)))
                }
            }
        }
    }
}

@Composable
private fun QuestOverlayV17(state: VillageHomeV17State, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.mw_ic_quest), null, tint = Color(0xFF7653B5), modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("Daily Quest", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(state.questText, color = Color(0xFF34545A), fontSize = 10.sp, maxLines = 1)
            Text(state.questProgressText, color = Color(0xFF7653B5), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RewardOverlayV17(state: VillageHomeV17State, w: Dp, h: Dp) {
    val items = listOf(
        Triple(NRect(.912f,.030f,.070f,.050f), R.drawable.mw_ic_coin, state.fishTreats.toString()),
    )
    if (state.hasNewDiscovery) {
        val discoveryItems = items + Triple(NRect(.830f,.030f,.070f,.050f), R.drawable.mw_ic_star, "NEW")
        discoveryItems.forEach { (r, icon, value) ->
            NBox(r,w,h) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(painterResource(icon), null, tint = Color(0xFFB87916), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(value, color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
        return
    }
    items.forEach { (r, icon, value) ->
        NBox(r,w,h) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(painterResource(icon), null, tint = Color(0xFFB87916), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(value, color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DestinationOverlayV17(destination: VillageDestinationV17, onClick: (String) -> Unit) {
    val description = buildString {
        append(destination.name); append(", "); append(destination.subject); append(", ")
        append(if (destination.enabled) "${destination.progressText} complete" else "opening soon")
        if (destination.recommended) append(", recommended today")
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
                if (!destination.enabled) disabled()
            }
            .clickable(enabled = destination.enabled, role = Role.Button) { onClick(destination.id) }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (destination.recommended) {
                Text("TODAY", color = Color(0xFFB87916), fontWeight = FontWeight.Black, fontSize = 8.sp)
            }
            Text(destination.name, color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 2)
            Text(if (destination.enabled) "${destination.subject} · ${destination.progressText}" else "${destination.subject} · Opening soon", color = Color(0xFF34545A), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun NavigationOverlayV17(home:()->Unit, progress:()->Unit, avatars:()->Unit, parents:()->Unit) {
    val items = listOf(
        Triple("Home", R.drawable.mw_ic_home, home),
        Triple("Progress", R.drawable.mw_ic_progress, progress),
        Triple("Avatars", R.drawable.mw_ic_avatars, avatars),
        Triple("Parents", R.drawable.mw_ic_lock, parents),
    )
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, icon, action) ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClick = action),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(painterResource(icon), null, tint = Color(0xFF075F63), modifier = Modifier.size(24.dp))
                Text(label, color = Color(0xFF075F63), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CompactVillageV17(state: VillageHomeV17State, onDestinationClick: (String)->Unit, onQuestClick:()->Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF5DD)).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
    ) {
        Image(painterResource(R.drawable.mw_village_scene_v17), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1.8f))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hi, ${state.childName}!", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFF2DEB6)).clickable(onClick=onQuestClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.mw_ic_quest), null, tint=Color(0xFF7653B5)); Spacer(Modifier.width(12.dp))
                Column { Text("Daily Quest", fontWeight=FontWeight.ExtraBold, color=Color(0xFF075F63)); Text(state.questText, color=Color(0xFF34545A)) }
            }
            state.destinations.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { d ->
                        Column(
                            Modifier.weight(1f).heightIn(min=112.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF2DEB6))
                                .clickable(enabled=d.enabled){onDestinationClick(d.id)}.padding(14.dp),
                            verticalArrangement=Arrangement.Center,
                        ) {
                            Icon(painterResource(d.iconRes), null, tint=Color(0xFF075F63), modifier=Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp)); Text(d.name, color=Color(0xFF075F63), fontWeight=FontWeight.ExtraBold, fontSize=16.sp)
                            Text(if(d.enabled) "${d.subject} · ${d.progressText}" else "${d.subject} · Opening soon", color=Color(0xFF34545A), fontSize=12.sp)
                        }
                    }
                    if(pair.size==1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiraRequestOverlay(onClick: () -> Unit) {
    val description = "Mira needs help finishing a story. Tap to start an English lesson."
    Row(
        Modifier.fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF5DD))
            .semantics(mergeDescendants = true) {
                contentDescription = description; role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.mw_ic_profile), null, tint = Color(0xFFD15E7C), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Mira needs help!", color = Color(0xFF075F63), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Text("Can you help finish a story?", color = Color(0xFF34545A), fontSize = 10.sp, maxLines = 1)
        }
        Icon(painterResource(R.drawable.mw_ic_book), null, tint = Color(0xFFF5A623), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun CafeProgressOverlay(state: CafeUnlockState) {
    val progressPct = (state.progress.toFloat() / state.requiredTreats).coerceIn(0f,1f)
    val label = if (state.isUnlocked) "Café unlocked!" else "🍪 ${state.progress}/${state.requiredTreats}"
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(label, color = Color(0xFF075F63), fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
        if (!state.isUnlocked) {
            Spacer(Modifier.height(2.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x5536545A))) {
                Box(Modifier.fillMaxWidth(progressPct).fillMaxHeight().background(Color(0xFFF5A623)))
            }
        }
    }
}
