package com.maxinesworld.featurechildhome

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.maxinesworld.coredesignsystem.components.BambooSurface
import com.maxinesworld.coredesignsystem.theme.*
import kotlin.math.roundToInt

// ─── Types ───

typealias SubjectId = String

@Immutable
data class NormalizedRect(val left: Float, val top: Float, val width: Float, val height: Float)

sealed interface DestinationState {
    data object Available : DestinationState
    data object Recommended : DestinationState
    data object Completed : DestinationState
    data class Locked(val reason: String) : DestinationState
}

@Immutable
data class SubjectDestinationUiState(
    val id: SubjectId, val name: String, val subject: String,
    val color: Color, val progress: Float,
    val state: DestinationState, val zone: NormalizedRect,
)

@Immutable
data class EndemicAnimal(
    val id: String, val subject: String, val x: Float, val y: Float,
    val targetW: Int, val animalRes: Int,
)

// ─── Master scene viewport ───

private const val REF_W = 1280f; private const val REF_H = 800f

private data class SceneViewport(val displayW: Float, val displayH: Float, val cropScale: Float, val cropOffsetX: Float) {
    fun toDpRect(r: NormalizedRect, density: Density): Rect {
        val xPx = (r.left * REF_W * cropScale - cropOffsetX)
        val yPx = (r.top * REF_H * cropScale)
        val wPx = (r.width * REF_W * cropScale)
        val hPx = (r.height * REF_H * cropScale)
        return Rect(xPx, yPx, xPx + wPx, yPx + hPx)
    }
}

private fun computeViewport(displayW: Float, displayH: Float): SceneViewport {
    val scale = maxOf(displayW / REF_W, displayH / REF_H)
    val cropW = REF_W * scale
    val offsetX = (cropW - displayW) / 2f
    return SceneViewport(displayW, displayH, scale, offsetX)
}

// ─── Canonical 6 destinations ───

private val landmarkZones = mapOf(
    "english" to NormalizedRect(0.04f, 0.23f, 0.28f, 0.27f),
    "filipino" to NormalizedRect(0.36f, 0.23f, 0.28f, 0.27f),
    "mathematics" to NormalizedRect(0.68f, 0.23f, 0.28f, 0.27f),
    "science" to NormalizedRect(0.04f, 0.55f, 0.28f, 0.27f),
    "history" to NormalizedRect(0.36f, 0.55f, 0.28f, 0.27f),
    "gmrc" to NormalizedRect(0.68f, 0.55f, 0.28f, 0.27f),
)

private val subjectMeta = mapOf(
    "english" to Triple("Story Tree", "English", StoryPurple),
    "filipino" to Triple("Bahay ng Kuwento", "Filipino", Coral),
    "mathematics" to Triple("Number Market", "Mathematics", SkyBlue),
    "science" to Triple("Discovery Lab", "Science", LeafGreen),
    "history" to Triple("Heritage Harbor", "Philippine History", Color(0xFFB87916)),
    "gmrc" to Triple("Kindness Corner", "GMRC", Color(0xFF087F83)),
)

// ─── Endemic animal placements ───

private val endemicAnimals = listOf(
    EndemicAnimal("philippine_eagle", "english", 0.265f, 0.075f, 44, R.drawable.animal_philippine_eagle),
    EndemicAnimal("philippine_tarsier", "filipino", 0.10f, 0.425f, 34, R.drawable.animal_philippine_tarsier),
    EndemicAnimal("tamaraw", "mathematics", 0.735f, 0.385f, 44, R.drawable.animal_tamaraw),
    EndemicAnimal("philippine_colugo", "science", 0.08f, 0.625f, 34, R.drawable.animal_philippine_colugo),
    EndemicAnimal("palawan_peacock_pheasant", "history", 0.285f, 0.835f, 42, R.drawable.animal_palawan_peacock_pheasant),
    EndemicAnimal("visayan_warty_pig", "gmrc", 0.895f, 0.845f, 42, R.drawable.animal_visayan_warty_pig),
)

@Composable
fun VillageHomeScreen(
    childName: String = "Maxine", level: Int = 12, xp: Int = 660, xpMax: Int = 900,
    dayStreak: Int = 7, stars: Int = 1234, pawCoins: Int = 567,
    onSubjectTap: (String) -> Unit = {}, onParentGate: () -> Unit = {},
    onAchievements: (() -> Unit)? = null, onBackpack: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null, onMenu: (() -> Unit)? = null,
    onQuestClick: () -> Unit = {},
) {
    val destinations = landmarkZones.map { (id, zone) ->
        val (name, subject, color) = subjectMeta[id]!!
        val progress = when (id) {
            "english" -> 0.42f; "filipino" -> 0.25f; "mathematics" -> 0.67f
            "science" -> 0.33f; "history" -> 0.16f; else -> 0f
        }
        val state: DestinationState = when {
            id == "gmrc" -> DestinationState.Locked("Opening soon")
            id == "mathematics" -> DestinationState.Recommended
            progress >= 1f -> DestinationState.Completed
            progress > 0f -> DestinationState.Available
            else -> DestinationState.Available
        }
        SubjectDestinationUiState(id, name, subject, color, progress, state, zone)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp && maxHeight >= 600.dp
        if (isExpanded) {
            ExpandedVillageHome(destinations, childName, level, xp, xpMax, dayStreak, stars, pawCoins,
                onSubjectTap, onQuestClick, onParentGate, onAchievements, onBackpack, onProfile, onMenu)
        } else {
            CompactVillageHome(destinations, childName, level, xp, xpMax, dayStreak, stars, pawCoins,
                onSubjectTap, onQuestClick, onParentGate, onAchievements, onBackpack, onProfile)
        }
    }
}

// ─── Expanded (tablet) ───

@Composable
private fun ExpandedVillageHome(
    destinations: List<SubjectDestinationUiState>,
    name: String, level: Int, xp: Int, xpMax: Int,
    streak: Int, stars: Int, coins: Int,
    onTap: (String) -> Unit, onQuest: () -> Unit,
    onParent: () -> Unit, onAchieve: (() -> Unit)?,
    onBack: (() -> Unit)?, onProf: (() -> Unit)?, onMenu: (() -> Unit)?
) {
    var displayW by remember { mutableStateOf(0f) }
    var displayH by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Scaffold(containerColor = Color.Transparent,
        bottomBar = { FloatingBottomNav(onParent, onAchieve, onBack, onProf) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).onSizeChanged { displayW = it.width.toFloat(); displayH = it.height.toFloat() }) {
            val vp = remember(displayW, displayH) { computeViewport(displayW, displayH) }

            // L1: Master scene
            Image(painterResource(R.drawable.village_home_six_landmarks_master), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.Center)

            // L2: Scrims
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.18f).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0x4D0B2A36), Color.Transparent))))
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.28f).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x700B2A36)))))

            // L3: Invisible hit targets (on top of scene, behind scrims)
            destinations.forEach { dest ->
                val rect = remember(dest.id, displayW, displayH) { vp.toDpRect(dest.zone, density) }
                val x = with(density) { rect.left.toDp() }
                val y = with(density) { rect.top.toDp() }
                val w = with(density) { rect.width.toDp() }
                val h = with(density) { rect.height.toDp() }

                Box(Modifier.offset(x, y).sizeIn(minWidth = w, minHeight = h)
                    .clickable(
                        enabled = dest.state !is DestinationState.Locked,
                        role = Role.Button,
                        onClick = { onTap(dest.id) }
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "${dest.name}, ${dest.subject}"
                        stateDescription = when (dest.state) {
                            is DestinationState.Recommended -> "Recommended today, ${(dest.progress * 100).roundToInt()}% complete"
                            is DestinationState.Locked -> "Locked, ${(dest.state as DestinationState.Locked).reason}"
                            else -> "${(dest.progress * 100).roundToInt()}% complete"
                        }
                    }
                )
            }

            // L4: Profile HUD
            ProfileHud(name, level, xp, xpMax, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 20.dp))

            // L4: Streak + currencies
            Row(Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardPill(Icons.Rounded.LocalFireDepartment, "$streak", Coral)
                RewardPill(Icons.Rounded.Star, formatCount(stars), SunshineGold)
                RewardPill(painterResource(R.drawable.ic_paw_coin), formatCount(coins), Color(0xFFC8A04A))
                if (onMenu != null) {
                    IconButton(onClick = onMenu, modifier = Modifier.size(48.dp).background(SkyBlue, RoundedCornerShape(16.dp))) {
                        Icon(Icons.Rounded.Menu, "Menu", tint = White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // L4: Collapsed Daily Quest pill
            QuestPill(onQuest, Modifier.align(Alignment.TopStart).padding(start = 24.dp, top = 110.dp))

            // L4.5: Native labels (must render ABOVE HUD)
            destinations.forEach { dest ->
                val rect = remember(dest.id, displayW, displayH) { vp.toDpRect(dest.zone, density) }
                val x = with(density) { rect.left.toDp() }
                val y = with(density) { rect.top.toDp() }
                val w = with(density) { rect.width.toDp() }
                val labelY = if (dest.zone.top < 0.50f) y + with(density) { rect.height.toDp() } + 40.dp else y + with(density) { rect.height.toDp() } + 4.dp
                DestinationLabel(dest, Modifier.offset(x = x, y = labelY).widthIn(max = w * 0.9f))
            }

            // L4.6: Endemic animals (render above labels and HUD)
            endemicAnimals.forEach { animal ->
                val ax = with(density) { (animal.x * vp.displayW).toDp() }
                val ay = with(density) { (animal.y * vp.displayH).toDp() }
                val aw = with(density) { (animal.targetW).dp }
                Image(painterResource(animal.animalRes), null,
                    Modifier.offset(ax, ay).size(aw),
                    contentScale = ContentScale.Fit,
                    alpha = 0.85f)
            }

            // TODAY focus glow
            val today = destinations.find { it.state is DestinationState.Recommended }
            if (today != null) {
                val rect = remember(today.id, displayW, displayH) { vp.toDpRect(today.zone, density) }
                Box(Modifier.offset(with(density) { rect.left.toDp() }, with(density) { rect.top.toDp() })
                    .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                    .drawBehind {
                        drawRoundRect(SunshineGold.copy(alpha = 0.08f),
                            cornerRadius = CornerRadius(24f, 24f))
                    })
            }
        }
    }
}

// ─── Compact layout ───

@Composable
private fun CompactVillageHome(
    destinations: List<SubjectDestinationUiState>,
    name: String, level: Int, xp: Int, xpMax: Int,
    streak: Int, stars: Int, coins: Int,
    onTap: (String) -> Unit, onQuest: () -> Unit,
    onParent: () -> Unit, onAchieve: (() -> Unit)?,
    onBack: (() -> Unit)?, onProf: (() -> Unit)?
) {
    Scaffold(containerColor = Cream,
        bottomBar = { FloatingBottomNav(onParent, onAchieve, onBack, onProf) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            // Hero crop
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                Image(painterResource(R.drawable.village_home_six_landmarks_master), null,
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.Center)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x4D0B2A36), Color.Transparent, Color(0x4D0B2A36)))))
                ProfileHud(name, level, xp, xpMax, Modifier.align(Alignment.TopStart).padding(16.dp))
                Row(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RewardPill(Icons.Rounded.LocalFireDepartment, "$streak", Coral)
                    RewardPill(Icons.Rounded.Star, formatCount(stars), SunshineGold)
                }
            }
            QuestPill(onQuest, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            // 2-column grid
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in destinations.chunked(2)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { dest ->
                            CompactCard(dest, Modifier.weight(1f), onTap)
                        }
                        if (row.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Destination Label (expanded) ───

@Composable
private fun DestinationLabel(dest: SubjectDestinationUiState, modifier: Modifier = Modifier) {
    BambooSurface(
        modifier = modifier,
        subjectAccent = dest.color,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dest.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dest.subject, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = dest.color)
                    Text(" · ", color = Ink.copy(alpha = 0.4f))
                    when (dest.state) {
                        is DestinationState.Locked -> {
                            Icon(Icons.Rounded.Lock, null, tint = Ink.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                            Text((dest.state as DestinationState.Locked).reason, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f))
                        }
                        else -> {
                            val pct = (dest.progress * 100).roundToInt()
                            Text("$pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = dest.color)
                            LinearProgressIndicator(dest.progress, Modifier.width(48.dp).height(4.dp).padding(start = 6.dp).clip(RoundedCornerShape(2.dp)),
                                color = dest.color, trackColor = dest.color.copy(alpha = 0.12f))
                        }
                    }
                }
            }
            if (dest.state is DestinationState.Recommended) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(50), color = SunshineGold.copy(alpha = 0.2f)) {
                    Text("TODAY", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF2B2100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ─── Compact Card ───

@Composable
private fun CompactCard(dest: SubjectDestinationUiState, modifier: Modifier, onTap: (String) -> Unit) {
    Card(
        onClick = { if (dest.state !is DestinationState.Locked) onTap(dest.id) },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = dest.color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = dest.color.copy(alpha = 0.15f)) {
                    Icon(painterResource(R.drawable.ic_book), null, tint = dest.color, modifier = Modifier.padding(8.dp).size(24.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(dest.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(dest.subject, fontSize = 11.sp, color = dest.color)
                }
                if (dest.state is DestinationState.Recommended) {
                    Surface(shape = RoundedCornerShape(50), color = SunshineGold.copy(alpha = 0.2f)) {
                        Text("TODAY", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF2B2100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (dest.state) {
                is DestinationState.Locked -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = Ink.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                        Text((dest.state as DestinationState.Locked).reason, fontSize = 11.sp, color = Ink.copy(alpha = 0.4f))
                    }
                }
                else -> {
                    LinearProgressIndicator(dest.progress, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = dest.color, trackColor = dest.color.copy(alpha = 0.1f))
                    Spacer(Modifier.height(2.dp))
                    Text("${(dest.progress * 100).roundToInt()}%", fontSize = 10.sp, color = dest.color)
                }
            }
        }
    }
}

// ─── Profile HUD ───

@Composable
private fun ProfileHud(name: String, level: Int, xp: Int, xpMax: Int, modifier: Modifier = Modifier) {
    BambooSurface(
        modifier = modifier.widthIn(180.dp, 260.dp),
        railThickness = 10.dp, cornerSize = 18.dp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Coral))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Hi, $name!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = SunshineGold.copy(alpha = 0.15f)) {
                        Text("Lv $level", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2100),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    LinearProgressIndicator(xp.toFloat() / xpMax, Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = SunshineGold, trackColor = Color(0xFFE6D9C2))
                }
                Text("$xp / $xpMax XP", fontSize = 10.sp, color = Ink.copy(alpha = 0.4f))
            }
        }
    }
}

// ─── Reward Pill ───

@Composable
private fun RewardPill(icon: Any, text: String, tint: Color) {
    Surface(shape = RoundedCornerShape(50), color = Cream.copy(alpha = 0.94f), shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            when (icon) {
                is ImageVector -> Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
                is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
        }
    }
}

// ─── Quest Pill ───

@Composable
private fun QuestPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Cream.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = StoryPurple.copy(alpha = 0.12f)) {
                Icon(painterResource(R.drawable.ic_quest), null, tint = StoryPurple,
                    modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily Quest", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
                Text("Read a story and discover 5 new words · 3/5", fontSize = 11.sp,
                    color = Ink.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(50), color = LeafGreen.copy(alpha = 0.12f)) {
                Text("Continue", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LeafGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

// ─── Bottom Nav ───

@Composable
private fun FloatingBottomNav(
    onParent: () -> Unit, onAchieve: (() -> Unit)?,
    onBack: (() -> Unit)?, onProf: (() -> Unit)?
) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp), color = Cream.copy(alpha = 0.97f),
        shadowElevation = 8.dp, tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            NavItem(onProf ?: {}, Icons.Rounded.Person, "Profile", enabled = onProf != null)
            NavItem(onAchieve ?: {}, Icons.Rounded.EmojiEvents, "Achievements", enabled = onAchieve != null)
            NavItem(onBack ?: {}, Icons.Rounded.Backpack, "Backpack", enabled = onBack != null)
            NavItem(onParent, Icons.Rounded.Lock, "Parents", enabled = true)
        }
    }
}

@Composable
private fun NavItem(onClick: () -> Unit, icon: ImageVector, label: String, enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.role = Role.Button; contentDescription = label; if (!enabled) stateDescription = "Unavailable" }
            .padding(8.dp).sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (enabled) VillageTeal else Ink.copy(alpha = 0.25f), modifier = Modifier.size(26.dp))
        Text(label, fontSize = 10.sp, color = if (enabled) Ink else Ink.copy(alpha = 0.25f))
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1000 -> "${n / 1000}.${(n % 1000) / 100}k"
    else -> "$n"
}
