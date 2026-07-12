package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyChallengeProgressRow

// ─── Village landscape colors ───
private val SkyTop = Color(0xFF87CEEB)
private val SkyBottom = Color(0xFFE8F4FD)
private val MountainFar = Color(0xFF9B8EC4)
private val MountainNear = Color(0xFF6B5B9E)
private val GrassGreen = Color(0xFF7BC67E)
private val GroundBrown = Color(0xFFD4A96A)
private val PathColor = Color(0xFFE8D5B7)
private val WaterBlue = Color(0xFF5B9BD5)
private val WoodBrown = Color(0xFF8B5E3C)
private val TreeGreen = Color(0xFF2E7D32)
private val RoofRed = Color(0xFFC44E3A)

// ─── Building shapes per subject ───
data class SubjectBuilding(
    val id: String, val name: String, val subtitle: String,
    val guideName: String, val characterRes: Int,
    val roofType: RoofType, val color: Color, val icon: ImageVector
)
enum class RoofType { TREE_OAK, MARKET_AWNING, DOME_GLASS, BAHAY_KUBO, FLAG_FORT, HEART_COTTAGE }

private val subjectBuildings = listOf(
    SubjectBuilding("english", "Story Tree", "English", "Mira", R.drawable.character_mira, RoofType.TREE_OAK, StoryPurple, Icons.Default.MenuBook),
    SubjectBuilding("mathematics", "Number Market", "Math", "Milo", R.drawable.character_milo, RoofType.MARKET_AWNING, SkyBlue, Icons.Default.Calculate),
    SubjectBuilding("science", "Discovery Lab", "Science", "Niko", R.drawable.character_niko, RoofType.DOME_GLASS, LeafGreen, Icons.Default.Science),
    SubjectBuilding("filipino", "Bahay ng Kuwento", "Filipino", "Mira", R.drawable.character_mira, RoofType.BAHAY_KUBO, Coral, Icons.Default.AutoStories),
    SubjectBuilding("makabansa", "Heritage Harbor", "Makabansa", "Lakan", R.drawable.character_lakan, RoofType.FLAG_FORT, SunshineGold, Icons.Default.Flag),
    SubjectBuilding("gmrc", "Kindness Corner", "GMRC", "Duke", R.drawable.character_duke, RoofType.HEART_COTTAGE, Coral, Icons.Default.Favorite)
)

// ─── Main Village Home Screen ───

@Composable
fun VillageHomeScreen(
    childId: String = "unknown",
    badgeAwarder: BadgeAwarder? = null,
    childName: String = "Maxine",
    level: Int = 1,
    dayStreak: Int = 7,
    stars: Int = 0,
    xp: Int = 660,
    xpMax: Int = 900,
    questCompleted: Int = 3,
    questTotal: Int = 5,
    onSubjectTap: (String) -> Unit = {},
    onParentGate: () -> Unit = {},
    onAchievements: () -> Unit = {},
    onBackpack: () -> Unit = {},
    onDailyQuest: () -> Unit = {}
) {
    // ─── Load daily challenge progress ───
    val challengeProgress by produceState(ChallengeProgress()) {
        badgeAwarder?.let { value = it.getTodayProgress(childId) }
    }
    val badgeCount by produceState(0) {
        badgeAwarder?.let { value = it.getCollectedCount(childId) }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp
        Scaffold(
            containerColor = Cream,
            topBar = { VillageTopBar(childName, level, xp, xpMax, dayStreak) },
            bottomBar = { VillageBottomBar(onAchievements, onBackpack, onParentGate, badgeCount) }
        ) { innerPadding ->
            Column(Modifier.padding(innerPadding)) {
                // ─── Daily Challenge Progress ───
                if (badgeAwarder != null) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Daily Challenge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = VillageTeal
                            )
                            Spacer(Modifier.height(12.dp))
                            DailyChallengeProgressRow(challengeProgress)
                            if (challengeProgress.completedCount == 5) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "🎉 Badge earned!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (isWide) {
                    Row(Modifier.fillMaxSize().weight(1f)) {
                        DailyQuestPanel(questCompleted, questTotal, onDailyQuest, Modifier.width(220.dp).fillMaxHeight())
                        VillageLandscape(onSubjectTap, Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        VillageLandscape(onSubjectTap, Modifier.fillMaxWidth().height(500.dp))
                        DailyQuestPanel(questCompleted, questTotal, onDailyQuest, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

// ─── Hero / Village Landscape (scrollable) ───

@Composable
private fun VillageLandscape(onSubjectTap: (String) -> Unit, modifier: Modifier) {
    val scrollState = rememberScrollState()
    Box(modifier) {
        // Background: sky + mountains + ground
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Sky gradient
            drawRect(brush = Brush.verticalGradient(listOf(SkyTop, SkyBottom, Color(0xFFD4E8C2))), topLeft = Offset(0f, 0f), size = Size(w, h * 0.55f))
            // Far mountains
            drawPath(Path().apply {
                moveTo(0f, h * 0.45f); lineTo(w * 0.12f, h * 0.32f); lineTo(w * 0.28f, h * 0.38f)
                lineTo(w * 0.35f, h * 0.25f); lineTo(w * 0.48f, h * 0.33f); lineTo(w * 0.55f, h * 0.28f)
                lineTo(w * 0.65f, h * 0.35f); lineTo(w * 0.78f, h * 0.22f); lineTo(w * 0.90f, h * 0.30f)
                lineTo(w, h * 0.38f); lineTo(w, h * 0.55f); lineTo(0f, h * 0.55f); close()
            }, color = MountainFar.let { it.copy(alpha = 0.4f) })
            // Near mountains
            drawPath(Path().apply {
                moveTo(0f, h * 0.48f); lineTo(w * 0.15f, h * 0.36f); lineTo(w * 0.30f, h * 0.42f)
                lineTo(w * 0.50f, h * 0.32f); lineTo(w * 0.70f, h * 0.38f); lineTo(w * 0.85f, h * 0.28f)
                lineTo(w, h * 0.40f); lineTo(w, h * 0.55f); lineTo(0f, h * 0.55f); close()
            }, color = MountainNear.let { it.copy(alpha = 0.6f) })
            // Ground
            drawRect(GrassGreen.copy(alpha = 0.6f), topLeft = Offset(0f, h * 0.55f), size = Size(w, h * 0.35f))
            drawRect(GrassGreen, topLeft = Offset(0f, h * 0.70f), size = Size(w, h * 0.20f))
            drawRect(WaterBlue.copy(alpha = 0.3f), topLeft = Offset(w * 0.65f, h * 0.80f), size = Size(w * 0.35f, h * 0.15f))
            // Path
            drawPath(Path().apply {
                moveTo(w * 0.05f, h * 0.90f); lineTo(w * 0.25f, h * 0.82f); lineTo(w * 0.50f, h * 0.78f)
                lineTo(w * 0.75f, h * 0.75f); lineTo(w * 0.95f, h * 0.70f)
            }, color = PathColor, style = Stroke(width = 28f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))))
        }
        // Buildings + characters overlaid on landscape
        Row(
            Modifier.fillMaxSize().horizontalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subjectBuildings.forEach { building ->
                VillageBuilding(building, onSubjectTap)
            }
        }
    }
}

// ─── Village Building (subject destination) ───

@Composable
private fun VillageBuilding(building: SubjectBuilding, onTap: (String) -> Unit) {
    val bob by rememberInfiniteTransition(label = "bob").animateFloat(0f, 3f, animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutCubic), RepeatMode.Reverse), label = "bob")

    Column(
        modifier = Modifier.width(180.dp).clickable { onTap(building.id) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Character
        Image(
            painter = painterResource(building.characterRes), contentDescription = building.guideName,
            modifier = Modifier.size(96.dp).offset(y = bob.dp), contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(4.dp))
        // Building shape
        BuildingShape(building.roofType, building.color, Modifier.width(160.dp).height(140.dp))
        // Sign
        Card(Modifier.width(160.dp).offset(y = (-12).dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = WoodBrown), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(building.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(building.icon, null, tint = building.color, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(building.subtitle, fontSize = 11.sp, color = White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ─── Building shape renderer ───

@Composable
private fun BuildingShape(type: RoofType, accent: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        when (type) {
            RoofType.TREE_OAK -> {
                // Tree canopy
                drawCircle(accent.copy(alpha = 0.3f), radius = w * 0.4f, center = Offset(w * 0.5f, h * 0.35f))
                drawCircle(TreeGreen, radius = w * 0.35f, center = Offset(w * 0.35f, h * 0.4f))
                drawCircle(TreeGreen.copy(alpha = 0.8f), radius = w * 0.32f, center = Offset(w * 0.60f, h * 0.35f))
                // Trunk
                drawRect(WoodBrown, topLeft = Offset(w * 0.35f, h * 0.55f), size = Size(w * 0.3f, h * 0.45f))
                // Door
                drawRoundRect(TreeGreen.copy(alpha = 0.5f), topLeft = Offset(w * 0.40f, h * 0.70f), size = Size(w * 0.20f, h * 0.25f), cornerRadius = CornerRadius(4f))
            }
            RoofType.MARKET_AWNING -> {
                // Base
                drawRoundRect(WoodBrown.copy(alpha = 0.6f), topLeft = Offset(w * 0.15f, h * 0.30f), size = Size(w * 0.70f, h * 0.70f), cornerRadius = CornerRadius(8f))
                // Awning stripes
                repeat(5) { i ->
                    drawRect(if (i % 2 == 0) RoofRed else White, topLeft = Offset(w * 0.12f, h * 0.22f + i * h * 0.06f), size = Size(w * 0.76f, h * 0.06f))
                }
                // Counter
                drawRect(WoodBrown, topLeft = Offset(w * 0.10f, h * 0.55f), size = Size(w * 0.80f, h * 0.08f))
            }
            RoofType.DOME_GLASS -> {
                // Dome roof
                drawCircle(accent.copy(alpha = 0.2f), radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.25f))
                drawArc(accent.copy(alpha = 0.4f), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.12f, 0f), size = Size(w * 0.76f, h * 0.6f))
                // Base
                drawRect(accent.copy(alpha = 0.3f), topLeft = Offset(w * 0.15f, h * 0.30f), size = Size(w * 0.70f, h * 0.70f))
            }
            RoofType.BAHAY_KUBO -> {
                // Thatched roof
                val roof = Path().apply {
                    moveTo(w * 0.1f, h * 0.45f); lineTo(w * 0.5f, h * 0.05f); lineTo(w * 0.9f, h * 0.45f); close()
                }
                drawPath(roof, WoodBrown)
                // Bamboo walls
                drawRect(WoodBrown.copy(alpha = 0.4f), topLeft = Offset(w * 0.15f, h * 0.45f), size = Size(w * 0.70f, h * 0.55f))
            }
            RoofType.FLAG_FORT -> {
                // Fort walls
                drawRect(accent.copy(alpha = 0.4f), topLeft = Offset(w * 0.10f, h * 0.20f), size = Size(w * 0.80f, h * 0.80f))
                // Crenellations
                repeat(4) { i ->
                    drawRect(accent.copy(alpha = 0.4f), topLeft = Offset(w * (0.10f + i * 0.20f), h * 0.08f), size = Size(w * 0.12f, h * 0.12f))
                }
                // Flag
                drawLine(White, Offset(w * 0.70f, h * 0.08f), Offset(w * 0.70f, h * 0.45f), strokeWidth = 3f)
                drawRect(accent, topLeft = Offset(w * 0.70f, h * 0.05f), size = Size(w * 0.18f, h * 0.12f))
            }
            RoofType.HEART_COTTAGE -> {
                // Heart-shaped roof
                drawCircle(Coral.copy(alpha = 0.5f), radius = w * 0.2f, center = Offset(w * 0.35f, h * 0.2f))
                drawCircle(Coral.copy(alpha = 0.5f), radius = w * 0.2f, center = Offset(w * 0.65f, h * 0.2f))
                drawPath(Path().apply {
                    moveTo(w * 0.15f, h * 0.25f); lineTo(w * 0.5f, h * 0.50f); lineTo(w * 0.85f, h * 0.25f); close()
                }, Coral.copy(alpha = 0.5f))
                // Cottage walls
                drawRect(Coral.copy(alpha = 0.2f), topLeft = Offset(w * 0.15f, h * 0.25f), size = Size(w * 0.70f, h * 0.75f))
            }
        }
    }
}

// ─── Daily Quest Panel ───

@Composable
private fun DailyQuestPanel(completed: Int, total: Int, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier.padding(8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = SunshineGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Daily Quest", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            }
            Spacer(Modifier.height(16.dp))
            Image(painterResource(R.drawable.character_milo), "Quest", Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(12.dp))
            Text("Read a story and learn\n5 new words!", fontSize = 15.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = 0.8f), lineHeight = 22.sp)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { completed.toFloat() / total.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = SuccessGreen, trackColor = SuccessGreen.copy(alpha = 0.15f))
            Spacer(Modifier.height(4.dp))
            Text("$completed / $total", fontSize = 13.sp, color = Ink.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = SunshineGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("25", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SunshineGold) }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Toll, null, tint = SunshineGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("10", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SunshineGold) }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClick, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal40), modifier = Modifier.height(48.dp).fillMaxWidth()) { Text("Start", fontSize = 16.sp) }
            Spacer(Modifier.height(8.dp))
            Text("Explore Every Day!", fontSize = 12.sp, color = VillageTeal, fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Top Bar ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VillageTopBar(name: String, level: Int, xp: Int, xpMax: Int, dayStreak: Int) {
    TopAppBar(
        title = { Text("Maxine's World", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = VillageTeal) },
        actions = {
            if (dayStreak > 0) {
                AssistChip(onClick = {}, label = { Row { Icon(Icons.Default.LocalFireDepartment, null, tint = Coral, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("$dayStreak") } }, colors = AssistChipDefaults.assistChipColors(containerColor = SunshineGold.copy(alpha = 0.15f)))
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(Coral), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.character_milo), name, Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop) }
            Spacer(Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
    )
}

// ─── Bottom Nav ───

@Composable
private fun VillageBottomBar(
    onAchievements: () -> Unit = {}, onBackpack: () -> Unit = {}, onParentGate: () -> Unit = {},
    badgeCount: Int = 0
) {
    NavigationBar(containerColor = White) {
        NavigationBarItem(
            selected = false,
            onClick = onAchievements,
            icon = {
                Box {
                    Icon(Icons.Default.EmojiEvents, "Achievements", tint = VillageTeal)
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = Coral,
                            contentColor = White,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp)
                        ) {
                            Text(
                                "$badgeCount/50",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            },
            label = { Text("Achievements", fontSize = 12.sp) }
        )
        NavigationBarItem(selected = false, onClick = onBackpack, icon = { Icon(Icons.Default.ShoppingBag, "Backpack", tint = VillageTeal) }, label = { Text("Backpack", fontSize = 12.sp) })
        NavigationBarItem(selected = false, onClick = onParentGate, icon = { Icon(Icons.Default.People, "Parents", tint = VillageTeal) }, label = { Text("Parents", fontSize = 12.sp) })
    }
}
