package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val character: String,
    val description: String
)

val villageSubjects = listOf(
    SubjectDestination("english", "Story Tree", "Mira",
        Icons.Default.MenuBook, StoryPurple, StoryPurple.copy(alpha = 0.08f),
        "🐱💜", "Read stories and discover new words"),
    SubjectDestination("mathematics", "Number Market", "Milo",
        Icons.Default.Calculate, SkyBlue, SkyBlue.copy(alpha = 0.08f),
        "🐱🧡", "Play with numbers and solve puzzles"),
    SubjectDestination("science", "Discovery Lab", "Niko",
        Icons.Default.Science, LeafGreen, LeafGreen.copy(alpha = 0.08f),
        "🐱🩶", "Explore, predict, and experiment"),
    SubjectDestination("filipino", "Bahay ng Kuwento", "Mira",
        Icons.Default.AutoStories, Coral, Coral.copy(alpha = 0.08f),
        "🐱💜", "Magbasa at magkuwento sa Filipino"),
    SubjectDestination("makabansa", "Heritage Harbor", "Lakan",
        Icons.Default.Flag, SunshineGold, SunshineGold.copy(alpha = 0.1f),
        "🐱🇵🇭", "Discover our culture and history"),
    SubjectDestination("gmrc", "Kindness Corner", "Duke",
        Icons.Default.Favorite, Coral, Coral.copy(alpha = 0.06f),
        "🐕💙", "Learn good manners and right conduct")
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
            containerColor = Cream,
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
                // ─── Hero Section ───
                if (isWide) {
                    HeroSectionWide(childName, dailyQuest, onDailyQuest, profile, stars)
                } else {
                    HeroSectionCompact(childName, dailyQuest, onDailyQuest, profile)
                }

                Spacer(Modifier.height(32.dp))

                // ─── Subject Destinations Grid ───
                Text(
                    "Where do you want to go today?",
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isWide) 30.sp else 24.sp,
                    color = Ink,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                val columns = when (profile) {
                    WindowProfile.LARGE_TABLET -> 3
                    WindowProfile.EXPANDED, WindowProfile.MEDIUM -> 2
                    else -> 1
                }

                // Build subject grid manually for mixed-sized cards
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
                            // Fill remaining slots
                            repeat(columns - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ─── Bottom decoration ───
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
    profile: WindowProfile,
    stars: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcome & Quest card — 68% width per spec
        Card(
            modifier = Modifier.weight(0.68f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = VillageTeal.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(Modifier.padding(32.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Magandang araw, $childName! ☀️",
                        fontSize = 32.sp,
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
                    // Daily Quest mini-card inside hero
                    DailyQuestMiniCard(quest, onDailyQuest)
                }
                // Character mascot
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(VillageTeal.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐱🧡", fontSize = 56.sp)
                }
            }
        }

        // Side info panel — 32% width per spec
        Column(
            modifier = Modifier.weight(0.32f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard("🔥", "${quest.completed}/${quest.total}", "Daily Quest Progress", VillageTeal)
            StatCard("⭐", "$stars", "Stars Earned", SunshineGold)
            StatCard("🏠", "6", "Village Places", LeafGreen)
        }
    }
}

// ─── Hero Section (Compact / Phone) ───

@Composable
private fun HeroSectionCompact(
    childName: String,
    quest: DailyQuestInfo,
    onDailyQuest: () -> Unit,
    profile: WindowProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VillageTeal.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🐱🧡", fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Hi, $childName! ☀️",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        "Ready to learn today?",
                        fontSize = 18.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            DailyQuestMiniCard(quest, onDailyQuest)
        }
    }
}

// ─── Daily Quest Mini Card ───

@Composable
private fun DailyQuestMiniCard(quest: DailyQuestInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SunshineGold.copy(alpha = 0.15f)),
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
                    Text("⭐ ${quest.starReward}", fontSize = 13.sp, color = SunshineGold, fontWeight = FontWeight.Medium)
                    Text("🪙 ${quest.coinReward}", fontSize = 13.sp, color = SunshineGold, fontWeight = FontWeight.Medium)
                    Text("⏱ ${quest.estimatedMinutes} min", fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
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
    // Gentle bobbing animation for the character
    val infiniteTransition = rememberInfiniteTransition(label = "bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    Card(
        modifier = modifier
            .clickable { onTap(subject.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = subject.surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.padding(if (isWide) 24.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Building-like icon area
            Box(
                modifier = Modifier
                    .offset(y = bobOffset.dp)
                    .size(if (isWide) 72.dp else 64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(subject.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(subject.character, fontSize = if (isWide) 36.sp else 30.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Subject name
            Text(
                subject.name,
                fontWeight = FontWeight.Bold,
                fontSize = if (isWide) 20.sp else 18.sp,
                color = subject.color,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))

            // Guide name
            Text(
                "Guide: ${subject.guideName}",
                fontSize = if (isWide) 14.sp else 13.sp,
                color = Ink.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            // Decorative path indicator
            PathDots(color = subject.color)
            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                subject.description,
                fontSize = if (isWide) 15.sp else 14.sp,
                color = Ink.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─── Stat Card ───

@Composable
private fun StatCard(emoji: String, value: String, label: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
                Text(label, fontSize = 13.sp, color = color.copy(alpha = 0.7f))
            }
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
                // Logo
                Text("🐾", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Maxine's World",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = VillageTeal
                )
            }
        },
        actions = {
            // Day streak
            if (dayStreak > 0) {
                AssistChip(
                    onClick = {},
                    label = { Text("🔥 $dayStreak", fontSize = 13.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SunshineGold.copy(alpha = 0.15f)
                    )
                )
                Spacer(Modifier.width(8.dp))
            }
            // Child avatar + name
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Coral),
                contentAlignment = Alignment.Center
            ) {
                Text("😺", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)
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
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, "Village") },
            label = { Text("Village", fontSize = 12.sp) },
            selected = true,
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.EmojiEvents, "Achievements") },
            label = { Text("Achievements", fontSize = 12.sp) },
            selected = false,
            onClick = onAchievements
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Backpack, "Backpack") },
            label = { Text("Backpack", fontSize = 12.sp) },
            selected = false,
            onClick = onBackpack
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Lock, "Parents") },
            label = { Text("Parents", fontSize = 12.sp) },
            selected = false,
            onClick = onParentGate
        )
    }
}

// ─── Decorative Elements ───

@Composable
private fun PawPrintDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.width(60.dp).height(1.dp)) {
            drawLine(VillageTeal.copy(alpha = 0.2f), Offset.Zero, Offset(size.width, 0f), 1f)
        }
        Spacer(Modifier.width(8.dp))
        Text("🐾", fontSize = 14.sp, color = VillageTeal.copy(alpha = 0.3f))
        Spacer(Modifier.width(8.dp))
        Canvas(Modifier.width(60.dp).height(1.dp)) {
            drawLine(VillageTeal.copy(alpha = 0.2f), Offset.Zero, Offset(size.width, 0f), 1f)
        }
    }
}

@Composable
private fun PathDots(color: Color, count: Int = 5) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i % 2 == 0) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (i < count / 2) 0.3f else 0.1f))
            )
        }
    }
}
