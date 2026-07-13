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
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.maxinesworld.coredesignsystem.theme.*
import kotlin.math.roundToInt

// ─── Types ───
typealias SubjectId = String

sealed interface DestinationState {
    data object Available : DestinationState
    data object Recommended : DestinationState
    data object Completed : DestinationState
    data class Locked(val reason: String) : DestinationState
}

@Immutable
data class SubjectDestinationUiState(
    val id: SubjectId, val name: String, val subject: String,
    val color: Color, val progress: Float, val state: DestinationState,
)

@Immutable
data class DestinationAnchor(val centerX: Float, val centerY: Float)

@Immutable
data class AnimalPlacement(val x: Float, val y: Float, val targetW: Int, val animalRes: Int)

// ─── Canonical data ───

private val destinationAnchors = mapOf(
    "english" to DestinationAnchor(0.14f, 0.44f),
    "filipino" to DestinationAnchor(0.50f, 0.30f),
    "mathematics" to DestinationAnchor(0.84f, 0.36f),
    "science" to DestinationAnchor(0.14f, 0.72f),
    "history" to DestinationAnchor(0.50f, 0.72f),
    "gmrc" to DestinationAnchor(0.84f, 0.72f),
)

private val subjectMeta = mapOf(
    "english" to Triple("Story Tree", "English", StoryPurple),
    "filipino" to Triple("Bahay ng Kuwento", "Filipino", Coral),
    "mathematics" to Triple("Number Market", "Mathematics", SkyBlue),
    "science" to Triple("Discovery Lab", "Science", LeafGreen),
    "history" to Triple("Heritage Harbor", "Philippine History", Color(0xFFB87916)),
    "gmrc" to Triple("Kindness Corner", "GMRC", Color(0xFF087F83)),
)

private val endemicAnimals = listOf(
    AnimalPlacement(0.27f, 0.08f, 44, R.drawable.animal_philippine_eagle),
    AnimalPlacement(0.10f, 0.43f, 34, R.drawable.animal_philippine_tarsier),
    AnimalPlacement(0.73f, 0.39f, 44, R.drawable.animal_tamaraw),
    AnimalPlacement(0.08f, 0.63f, 34, R.drawable.animal_philippine_colugo),
    AnimalPlacement(0.29f, 0.84f, 42, R.drawable.animal_palawan_peacock_pheasant),
    AnimalPlacement(0.89f, 0.85f, 42, R.drawable.animal_visayan_warty_pig),
)

// ─── Root Screen ───

@Composable
fun VillageHomeScreen(
    childName: String = "Maxine", level: Int = 12, xp: Int = 660, xpMax: Int = 900,
    dayStreak: Int = 7, stars: Int = 1234, pawCoins: Int = 567,
    onSubjectTap: (String) -> Unit = {}, onParentGate: () -> Unit = {},
    onAchievements: (() -> Unit)? = null, onBackpack: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null,
    onQuestClick: () -> Unit = {},
) {
    val destinations = destinationAnchors.map { (id, _) ->
        val (name, subject, color) = subjectMeta[id]!!
        val progress = when (id) {
            "english" -> 0.42f; "filipino" -> 0.25f; "mathematics" -> 0.67f
            "science" -> 0.33f; "history" -> 0.16f; else -> 0f
        }
        val state: DestinationState = when {
            id == "gmrc" -> DestinationState.Locked("Opening soon")
            id == "mathematics" -> DestinationState.Recommended
            progress >= 1f -> DestinationState.Completed
            else -> DestinationState.Available
        }
        SubjectDestinationUiState(id, name, subject, color, progress, state)
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { VillageBottomNav(onParentGate, onAchievements, onBackpack, onProfile) },
    ) { scaffoldPad ->
        Box(Modifier.fillMaxSize().padding(scaffoldPad).clipToBounds()) {
            // L0: Village scene
            Image(painterResource(R.drawable.village_home_six_landmarks_master), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.Center)

            // L0: Scrims
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.15f).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0x4D0B2A36), Color.Transparent))))

            var sceneW by remember { mutableStateOf(0) }; var sceneH by remember { mutableStateOf(0) }
            val density = LocalDensity.current

            // L0.5: Animals
            Box(Modifier.fillMaxSize().onSizeChanged { sceneW = it.width; sceneH = it.height }) {
                endemicAnimals.forEach { a ->
                    Image(painterResource(a.animalRes), null,
                        Modifier.offset(
                            x = with(density) { (a.x * sceneW.toFloat()).toDp() },
                            y = with(density) { (a.y * sceneH.toFloat()).toDp() },
                        ).size(with(density) { a.targetW.dp }),
                        contentScale = ContentScale.Fit, alpha = 0.8f)
                }
            }

            // L1: Hanging bamboo sign for Daily Quest
            val questAnchor = DestinationAnchor(0.14f, 0.16f)
            BambooSign(
                anchor = questAnchor, sceneW = sceneW, sceneH = sceneH, density = density,
                onClick = onQuestClick, enabled = true,
                accentColor = StoryPurple,
                contentDescription = "Daily Quest, read a story, 3 of 5, button",
            ) {
                Text("Daily Quest", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF183B4A))
                Text("Read & discover 5 words · 3/5", fontSize = 10.sp, color = Color(0xFF183B4A).copy(alpha = 0.55f), maxLines = 1)
            }

            // L1: Hanging bamboo signs for each destination
            destinations.forEach { dest ->
                val anchor = destinationAnchors[dest.id]!!
                val contentDesc = "${dest.name}, ${dest.subject}, ${(dest.progress * 100).roundToInt()}% complete" +
                    when (dest.state) {
                        is DestinationState.Recommended -> ", recommended today"
                        is DestinationState.Locked -> ", locked, ${(dest.state as DestinationState.Locked).reason}"
                        else -> ""
                    } + ", button"
                BambooSign(
                    anchor = anchor, sceneW = sceneW, sceneH = sceneH, density = density,
                    onClick = { onSubjectTap(dest.id) },
                    enabled = dest.state !is DestinationState.Locked,
                    accentColor = dest.color,
                    contentDescription = contentDesc,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dest.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF183B4A), maxLines = 1)
                            Text(dest.subject, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = dest.color)
                        }
                        if (dest.state is DestinationState.Recommended) {
                            Surface(shape = RoundedCornerShape(50), color = SunshineGold.copy(alpha = 0.2f)) {
                                Text("TODAY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2100),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        if (dest.state is DestinationState.Locked) {
                            Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = Color(0xFF183B4A).copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // L2: Profile HUD (compact top-left)
            Surface(
                Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp).widthIn(max = 260.dp),
                shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF7E8).copy(alpha = 0.94f), shadowElevation = 4.dp,
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Coral))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Hi, $childName!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF183B4A))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Lv $level", fontSize = 10.sp, color = SunshineGold, fontWeight = FontWeight.Bold)
                            Text(" · $xp / $xpMax XP", fontSize = 10.sp, color = Color(0xFF183B4A).copy(alpha = 0.4f))
                        }
                        LinearProgressIndicator((xp.toFloat() / xpMax).coerceIn(0f, 1f),
                            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = SunshineGold, trackColor = Color(0xFFE6D9C2))
                    }
                }
            }

            // L2: Currency badges (tiny, top-right)
            Row(Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BadgeChip(painterResource(R.drawable.ic_quest), "$dayStreak", Coral)
                BadgeChip(Icons.Rounded.Star, formatCount(stars), SunshineGold)
                BadgeChip(Icons.Rounded.CardGiftcard, formatCount(pawCoins), Color(0xFFC8A04A))
            }

            // L1.5: TODAY glow
            val todayDest = destinations.find { it.state is DestinationState.Recommended }
            if (todayDest != null) {
                val a = destinationAnchors[todayDest.id]!!
                val dx = with(density) { (a.centerX * sceneW.toFloat() - 78.dp.toPx()).toDp() }
                val dy = with(density) { (a.centerY * sceneH.toFloat() - 36.dp.toPx()).toDp() }
                Box(Modifier.offset(dx, dy).size(156.dp, 72.dp)
                    .drawBehind { drawRoundRect(SunshineGold.copy(alpha = 0.06f), cornerRadius = CornerRadius(16f)) })
            }
        }
    }
}

// ─── Hanging Bamboo Sign ───

@Composable
private fun BambooSign(
    anchor: DestinationAnchor, sceneW: Int, sceneH: Int, density: Density,
    onClick: () -> Unit, enabled: Boolean,
    accentColor: Color, contentDescription: String,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxWPx = sceneW.toFloat(); val maxHPx = sceneH.toFloat()
        val signW = 156.dp; val signH = 56.dp
        val tabH = 12.dp; val railW = 10.dp; val cornerSz = 18.dp
        val pl = LocalDensity.current

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    if (sceneW <= 0 || sceneH <= 0) {
                        return@layout layout(0, 0) {}
                    }
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val cx = (maxWPx * anchor.centerX - placeable.width / 2f).roundToInt()
                        .coerceIn(8.dp.toPx().roundToInt(), (maxWPx - placeable.width - 8.dp.toPx()).roundToInt())
                    val cy = (maxHPx * anchor.centerY - placeable.height / 2f).roundToInt()
                        .coerceIn(8.dp.toPx().roundToInt(), (maxHPx - placeable.height - 8.dp.toPx()).roundToInt())
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(cx, cy) }
                }
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                    if (!enabled) disabled()
                }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        ) {
            // Hanging tab
            Box(Modifier.width(20.dp).height(tabH).align(Alignment.TopCenter).offset(y = -tabH + 2.dp)
                .background(Color(0xFFD4A574), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))

            // Sign body
            Box(
                Modifier
                    .size(signW, signH)
                    .background(Color(0xFFFFF7E8), RoundedCornerShape(18.dp)),
            ) {
                // Sawali
                Image(painterResource(R.drawable.fill_sawali), null,
                    Modifier.matchParentSize().padding(4.dp), contentScale = ContentScale.Crop, alpha = 0.10f)

                // Accent top rail
                Box(Modifier.fillMaxWidth().height(5.dp).align(Alignment.TopCenter).background(accentColor.copy(alpha = 0.7f),
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)))

                // Bamboo rails
                Image(painterResource(R.drawable.rail_bamboo_horizontal), null,
                    Modifier.fillMaxWidth().height(railW).align(Alignment.TopCenter), contentScale = ContentScale.FillWidth)
                Image(painterResource(R.drawable.rail_bamboo_horizontal), null,
                    Modifier.fillMaxWidth().height(railW).align(Alignment.BottomCenter), contentScale = ContentScale.FillWidth)
                Image(painterResource(R.drawable.rail_bamboo_vertical), null,
                    Modifier.fillMaxHeight().width(railW).align(Alignment.CenterStart), contentScale = ContentScale.FillHeight)
                Image(painterResource(R.drawable.rail_bamboo_vertical), null,
                    Modifier.fillMaxHeight().width(railW).align(Alignment.CenterEnd), contentScale = ContentScale.FillHeight)

                // Rattan corners
                val c = cornerSz
                listOf(
                    R.drawable.corner_rattan_tl to Alignment.TopStart,
                    R.drawable.corner_rattan_tr to Alignment.TopEnd,
                    R.drawable.corner_rattan_bl to Alignment.BottomStart,
                    R.drawable.corner_rattan_br to Alignment.BottomEnd,
                ).forEach { (res, align) ->
                    Image(painterResource(res), null, Modifier.size(c).align(align))
                }

                // Content
                Box(Modifier.matchParentSize().padding(start = 14.dp, top = 8.dp, end = 12.dp, bottom = 6.dp)) {
                    content()
                }
            }
        }
    }
}

// ─── Badge Chip ───

@Composable
private fun BadgeChip(icon: Any, text: String, tint: Color) {
    Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFF7E8).copy(alpha = 0.94f), shadowElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            when (icon) {
                is ImageVector -> Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
                is androidx.compose.ui.graphics.painter.Painter -> Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(2.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF183B4A))
        }
    }
}

// ─── Bottom Nav ───

@Composable
private fun VillageBottomNav(
    onParent: () -> Unit, onAchieve: (() -> Unit)?,
    onBack: (() -> Unit)?, onProf: (() -> Unit)?,
) {
    // Bamboo-framed nav
    Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 10.dp).height(56.dp)
        .background(Color(0xFFFFF7E8), RoundedCornerShape(20.dp))) {
        // Bamboo rails
        Image(painterResource(R.drawable.rail_bamboo_horizontal), null, Modifier.fillMaxWidth().height(10.dp).align(Alignment.TopCenter), contentScale = ContentScale.FillWidth)
        Image(painterResource(R.drawable.rail_bamboo_horizontal), null, Modifier.fillMaxWidth().height(10.dp).align(Alignment.BottomCenter), contentScale = ContentScale.FillWidth)
        Image(painterResource(R.drawable.rail_bamboo_vertical), null, Modifier.fillMaxHeight().width(10.dp).align(Alignment.CenterStart), contentScale = ContentScale.FillHeight)
        Image(painterResource(R.drawable.rail_bamboo_vertical), null, Modifier.fillMaxHeight().width(10.dp).align(Alignment.CenterEnd), contentScale = ContentScale.FillHeight)
        // Corners
        listOf(
            R.drawable.corner_rattan_tl to Alignment.TopStart,
            R.drawable.corner_rattan_tr to Alignment.TopEnd,
            R.drawable.corner_rattan_bl to Alignment.BottomStart,
            R.drawable.corner_rattan_br to Alignment.BottomEnd,
        ).forEach { (r, a) -> Image(painterResource(r), null, Modifier.size(18.dp).align(a)) }

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            NavBtn(onProf ?: {}, Icons.Rounded.Person, "Profile", onProf != null)
            NavBtn(onAchieve ?: {}, Icons.Rounded.EmojiEvents, "Achieve", onAchieve != null)
            NavBtn(onBack ?: {}, Icons.Rounded.Backpack, "Backpack", onBack != null)
            NavBtn(onParent, Icons.Rounded.Lock, "Parents", true)
        }
    }
}

@Composable
private fun NavBtn(onClick: () -> Unit, icon: ImageVector, label: String, enabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label; if (!enabled) stateDescription = "Unavailable" }
            .width(56.dp).height(48.dp).wrapContentSize(Alignment.Center),
        verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (enabled) VillageTeal else Color(0xFF183B4A).copy(alpha = 0.25f), modifier = Modifier.size(22.dp))
        Text(label, fontSize = 9.sp, color = if (enabled) Color(0xFF183B4A) else Color(0xFF183B4A).copy(alpha = 0.25f))
    }
}

// ─── Utils ───

private fun formatCount(n: Int): String = when {
    n >= 1000 -> "${n / 1000}.${(n % 1000) / 100}k"
    else -> "$n"
}
