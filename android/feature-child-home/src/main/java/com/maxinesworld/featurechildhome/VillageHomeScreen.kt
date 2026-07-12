package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.R
import com.maxinesworld.coredesignsystem.WindowProfile
import com.maxinesworld.coredesignsystem.windowProfileForWidth
import com.maxinesworld.coredesignsystem.isWide
import com.maxinesworld.coredesignsystem.pageMargin
import com.maxinesworld.coredesignsystem.theme.*

// ─── Subject destination data ───

data class SubjectDestination(
    val id: String,
    val name: String,
    val guideName: String,
    val icon: ImageVector,
    val color: Color,
    val surfaceColor: Color,
    val characterIcon: ImageVector,
    val description: String
)

val villageSubjects = listOf(
    SubjectDestination("english", "Story Tree", "Mira",
        Icons.Default.MenuBook, StoryPurple, StoryPurple.copy(alpha = 0.08f),
        Icons.Default.MenuBook, "Read stories and discover new words"),
    SubjectDestination("mathematics", "Number Market", "Milo",
        Icons.Default.Calculate, SkyBlue, SkyBlue.copy(alpha = 0.08f),
        Icons.Default.Calculate, "Play with numbers and solve puzzles"),
    SubjectDestination("science", "Discovery Lab", "Niko",
        Icons.Default.Science, LeafGreen, LeafGreen.copy(alpha = 0.08f),
        Icons.Default.Science, "Explore, predict, and experiment"),
    SubjectDestination("filipino", "Bahay ng Kuwento", "Mira",
        Icons.Default.AutoStories, Coral, Coral.copy(alpha = 0.08f),
        Icons.Default.AutoStories, "Magbasa at magkuwento sa Filipino"),
    SubjectDestination("makabansa", "Heritage Harbor", "Lakan",
        Icons.Default.Flag, SunshineGold, SunshineGold.copy(alpha = 0.1f),
        Icons.Default.Flag, "Discover our culture and history"),
    SubjectDestination("gmrc", "Kindness Corner", "Duke",
        Icons.Default.Favorite, Coral, Coral.copy(alpha = 0.06f),
        Icons.Default.Favorite, "Learn good manners and right conduct")
)

data class DailyQuestInfo(
    val subjectIcon: ImageVector = Icons.Default.AutoAwesome,
    val title: String = "Today's Adventure",
    val description: String = "Read a story and learn 5 new words!",
    val completed: Int = 0,
    val total: Int = 5,
    val starReward: Int = 25,
    val coinReward: Int = 10,
    val estimatedMinutes: Int = 10
)

// ─── Main Village Home Screen ───

@Composable
fun VillageHomeScreen(
    childName: String = "Maxine",
    level: Int = 1,
    dayStreak: Int = 0,
    stars: Int = 0,
    dailyQuest: DailyQuestInfo = DailyQuestInfo(),
    onSubjectTap: (String) -> Unit = {},
    onParentGate: () -> Unit = {},
    onProfile: () -> Unit = {},
    onAchievements: () -> Unit = {},
    onBackpack: () -> Unit = {},
    onDailyQuest: () -> Unit = {}
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val profile = windowProfileForWidth(maxWidth)
        val isWide = profile.isWide

        Scaffold(
            containerColor = White,
            topBar = { VillageTopBar(childName, level, dayStreak, stars) },
            bottomBar = { VillageBottomBar(onProfile, onAchievements, onBackpack, onParentGate) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = profile.pageMargin, vertical = 24.dp)
                    .widthIn(max = 1440.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── Hero Banner ───
                if (isWide) {
                    HeroSectionWide(childName, dailyQuest, onDailyQuest, stars)
                } else {
                    HeroSectionCompact(childName, dailyQuest, onDailyQuest)
                }

                Spacer(Modifier.height(32.dp))

                // ─── Subject Destinations Grid ───
                Text(
                    "Where do you want to go today?",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isWide) 32.sp else 26.sp,
                    color = Ink,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                val columns = when (profile) {
                    WindowProfile.LARGE_TABLET -> 3
                    WindowProfile.EXPANDED, WindowProfile.MEDIUM -> 2
                    else -> 1
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    villageSubjects.chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { subject ->
                                SubjectBuildingCard(
                                    subject = subject,
                                    modifier = Modifier.weight(1f),
                                    isWide = isWide,
                                    onTap = onSubjectTap
                                )
                            }
                            repeat(columns - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                PawPrintDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    "Learn. Explore. Grow.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VillageTeal.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Hero Section (Wide / Tablet) ───

@Composable
private fun HeroSectionWide(
    childName: String,
    quest: DailyQuestInfo,
    onDailyQuest: () -> Unit,
    stars: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Background gradient
            Canvas(Modifier.fillMaxWidth().height(280.dp)) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SkyBlue.copy(alpha = 0.15f),
                            VillageTeal.copy(alpha = 0.06f),
                            Cream
                        )
                    )
                )
                // Decorative clouds
                drawCircle(SkyBlue.copy(alpha = 0.07f), radius = 60f, center = Offset(100f, 80f))
                drawCircle(SkyBlue.copy(alpha = 0.05f), radius = 90f, center = Offset(300f, 50f))
                drawCircle(White.copy(alpha = 0.4f), radius = 50f, center = Offset(250f, 110f))
                // Ground
                drawRect(
                    LeafGreen.copy(alpha = 0.08f),
                    topLeft = Offset(0f, 240f),
                    size = androidx.compose.ui.geometry.Size(size.width, 40f)
                )
            }
            // Content
            Row(Modifier.padding(32.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Magandang araw, $childName!",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your animal friends have prepared today's learning adventure.",
                        fontSize = 20.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(24.dp))
                    DailyQuestMiniCard(quest, onDailyQuest)
                }
                // Milo mascot
                Box(contentAlignment = Alignment.BottomCenter) {
                    Image(
                        painter = painterResource(R.drawable.milo_hero),
                        contentDescription = "Milo",
                        modifier = Modifier.size(150.dp).offset(y = 10.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

// ─── Hero Section (Compact / Phone) ───

@Composable
private fun HeroSectionCompact(
    childName: String,
    quest: DailyQuestInfo,
    onDailyQuest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            Canvas(Modifier.fillMaxWidth().height(240.dp)) {
                drawRect(brush = Brush.verticalGradient(colors = listOf(SkyBlue.copy(alpha = 0.12f),VillageTeal.copy(alpha = 0.04f),Cream)))
                drawCircle(SkyBlue.copy(alpha = 0.06f), radius = 50f, center = Offset(80f, 60f))
                drawCircle(White.copy(alpha = 0.3f), radius = 35f, center = Offset(200f, 80f))
            }
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.milo_hero), "Milo", modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Hi, $childName!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Ready to learn today?", fontSize = 18.sp, color = Ink.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                DailyQuestMiniCard(quest, onDailyQuest)
            }
        }
    }
}

// ─── Daily Quest Mini Card ───

@Composable
private fun DailyQuestMiniCard(quest: DailyQuestInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(SunshineGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(quest.subjectIcon, "Quest", tint = SunshineGold, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(quest.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
                Text(quest.description, fontSize = 15.sp, color = Ink.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { quest.completed.toFloat() / quest.total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = SuccessGreen,
                    trackColor = SuccessGreen.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Star, "Stars", tint = SunshineGold, modifier = Modifier.size(16.dp))
                    Text(" ${quest.starReward}", fontSize = 13.sp, color = SunshineGold, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.Toll, "Coins", tint = SunshineGold, modifier = Modifier.size(16.dp))
                    Text(" ${quest.coinReward}", fontSize = 13.sp, color = SunshineGold, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, "Time", tint = Ink.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${quest.estimatedMinutes} min", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, "Start", tint = VillageTeal)
        }
    }
}

// ─── Subject Building Card ───

@Composable
private fun SubjectBuildingCard(
    subject: SubjectDestination,
    modifier: Modifier = Modifier,
    isWide: Boolean = true,
    onTap: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 4f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse),
        label = "bob"
    )

    Card(
        modifier = modifier.clickable { onTap(subject.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = subject.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(if (isWide) 24.dp else 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Character / building icon with bob animation
            Box(
                modifier = Modifier.offset(y = bobOffset.dp)
                    .size(if (isWide) 80.dp else 72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(subject.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(subject.characterIcon, subject.guideName, tint = subject.color, modifier = Modifier.size(if (isWide) 36.dp else 30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(subject.name, fontWeight = FontWeight.Bold, fontSize = if (isWide) 22.sp else 20.sp, color = subject.color, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Guide: ${subject.guideName}", fontSize = if (isWide) 15.sp else 14.sp, color = Ink.copy(alpha = 0.5f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            PathDots(color = subject.color)
            Spacer(Modifier.height(12.dp))
            Text(subject.description, fontSize = if (isWide) 15.sp else 14.sp, color = Ink.copy(alpha = 0.6f), textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
    }
}

// ─── Top Bar ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VillageTopBar(name: String, level: Int, dayStreak: Int, stars: Int) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.milo_hero), "Logo", modifier = Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(8.dp))
                Text("Maxine's World", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = VillageTeal)
            }
        },
        actions = {
            if (dayStreak > 0) {
                AssistChip(
                    onClick = {},
                    label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocalFireDepartment, "Streak", tint = Coral, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("$dayStreak", fontSize = 13.sp) } },
                    colors = AssistChipDefaults.assistChipColors(containerColor = SunshineGold.copy(alpha = 0.15f))
                )
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(Coral), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, name, tint = White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
    )
}

// ─── Bottom Nav ───

@Composable
private fun VillageBottomBar(
    onProfile: () -> Unit,
    onAchievements: () -> Unit,
    onBackpack: () -> Unit,
    onParentGate: () -> Unit
) {
    NavigationBar(containerColor = SurfaceContainer) {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, "Village") }, label = { Text("Village") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = VillageTeal))
        NavigationBarItem(selected = false, onClick = onAchievements, icon = { Icon(Icons.Default.EmojiEvents, "Achievements") }, label = { Text("Achievements") })
        NavigationBarItem(selected = false, onClick = onBackpack, icon = { Icon(Icons.Default.Backpack, "Backpack") }, label = { Text("Backpack") })
        NavigationBarItem(selected = false, onClick = onParentGate, icon = { Icon(Icons.Default.Shield, "Parents") }, label = { Text("Parents") })
    }
}

// ─── Path Dots ───

@Composable
private fun PathDots(color: Color) {
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val dotSpacing = 12f; val dotRadius = 3f
        val totalDots = ((size.width / dotSpacing).toInt()).coerceAtMost(8)
        for (i in 0 until totalDots) {
            drawCircle(color = color.copy(alpha = 0.3f - i * 0.02f), radius = dotRadius, center = Offset(size.width / 2 - (totalDots - 1) * dotSpacing / 2 + i * dotSpacing, 4f))
        }
    }
}

// ─── Paw Print Divider ───

@Composable
private fun PawPrintDivider() {
    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
        val pawSpacing = 40f
        val pawCount = (size.width / pawSpacing).toInt()
        val startX = (size.width - pawCount * pawSpacing) / 2 + pawSpacing / 2
        for (i in 0 until pawCount) {
            val x = startX + i * pawSpacing
            drawCircle(VillageTeal.copy(alpha = 0.2f), radius = 5f, center = Offset(x, 8f))
            drawCircle(VillageTeal.copy(alpha = 0.2f), radius = 3f, center = Offset(x - 6f, 4f))
            drawCircle(VillageTeal.copy(alpha = 0.2f), radius = 3f, center = Offset(x + 6f, 4f))
        }
    }
}
