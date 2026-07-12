package com.maxinesworld.featurechildhome

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.*

data class SubjectDestination(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val moduleCount: Int,
    val character: String  // emoji placeholder until we have vector art
)

val subjects = listOf(
    SubjectDestination("english", "Story Tree", Icons.Default.MenuBook, StoryTreeGreen, 12, "🐱"),
    SubjectDestination("filipino", "Bahay ng Kuwento", Icons.Default.AutoStories, BahayNgKuwentoBlue, 12, "🐱"),
    SubjectDestination("mathematics", "Number Market", Icons.Default.Calculate, NumberMarketRed, 12, "🐱"),
    SubjectDestination("science", "Discovery Lab", Icons.Default.Science, DiscoveryLabPurple, 12, "🐱"),
    SubjectDestination("philippine-history", "Heritage Harbor", Icons.Default.Flag, HeritageHarborBrown, 8, "🐱")
)

@Composable
fun VillageHomeScreen(
    childName: String = "Maxine",
    level: Int = 1,
    xp: Int = 150,
    xpMax: Int = 900,
    dayStreak: Int = 7,
    stars: Int = 0,
    coins: Int = 0,
    dailyQuest: DailyQuestState = DailyQuestState(),
    onSubjectTap: (String) -> Unit = {},
    onParentGate: () -> Unit = {},
    onProfile: () -> Unit = {},
    onAchievements: () -> Unit = {},
    onBackpack: () -> Unit = {}
) {
    Scaffold(
        topBar = { VillageTopBar(childName, level, xp, xpMax, dayStreak) },
        bottomBar = { VillageBottomBar(onProfile, onAchievements, onBackpack, onParentGate) }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Village greeting
                Text(
                    "Welcome to Maxine's World! 🌍",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Teal40
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick a place to start learning today!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // Village map with subject destinations
                VillageMap(subjects, onSubjectTap)
            }

            // Daily Quest sidebar
            DailyQuestPanel(dailyQuest, stars, coins)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VillageTopBar(
    name: String,
    level: Int,
    xp: Int,
    xpMax: Int,
    dayStreak: Int
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Orange40),
                    contentAlignment = Alignment.Center
                ) {
                    Text("😺", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Level $level", style = MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(
                        progress = { xp.toFloat() / xpMax },
                        modifier = Modifier.width(100.dp).height(6.dp),
                        color = EnergyGold,
                        trackColor = EnergyGold.copy(alpha = 0.3f)
                    )
                }
            }
        },
        actions = {
            // Day streak
            Badge(containerColor = Orange40) {
                Text("🔥 $dayStreak", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            // Stars
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐", fontSize = 16.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
    )
}

@Composable
private fun VillageBottomBar(
    onProfile: () -> Unit,
    onAchievements: () -> Unit,
    onBackpack: () -> Unit,
    onParentGate: () -> Unit
) {
    NavigationBar(containerColor = SurfaceContainer) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("My Profile") },
            selected = false,
            onClick = onProfile
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.EmojiEvents, "Achievements") },
            label = { Text("Achievements") },
            selected = false,
            onClick = onAchievements
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Backpack, "Backpack") },
            label = { Text("Backpack") },
            selected = false,
            onClick = onBackpack
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Lock, "Parents") },
            label = { Text("Parents") },
            selected = false,
            onClick = onParentGate
        )
    }
}

@Composable
fun VillageMap(
    subjects: List<SubjectDestination>,
    onSubjectTap: (String) -> Unit
) {
    // Village scene with path
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE8F5E9)) // Light green background
    ) {
        // Decorative path
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            // Winding path
            drawLine(
                color = Color(0xFFA5D6A7),
                start = Offset(size.width * 0.1f, size.height * 0.8f),
                end = Offset(size.width * 0.9f, size.height * 0.2f),
                strokeWidth = 12f,
                pathEffect = pathEffect
            )
        }

        // Subject destination cards placed around the map
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            subjects.forEach { subject ->
                SubjectCard(subject, Modifier.fillMaxWidth(), onSubjectTap)
            }
        }
    }
}

@Composable
fun SubjectCard(
    subject: SubjectDestination,
    modifier: Modifier = Modifier,
    onTap: (String) -> Unit
) {
    Card(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onTap(subject.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = subject.color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(subject.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    subject.icon,
                    contentDescription = subject.name,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    subject.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = subject.color
                )
                Text(
                    "${subject.moduleCount} modules · Grade 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(subject.character, fontSize = 24.sp)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = subject.color
            )
        }
    }
}

data class DailyQuestState(
    val title: String = "Read a story and learn 5 new words!",
    val completed: Int = 3,
    val total: Int = 5,
    val starReward: Int = 25,
    val coinReward: Int = 10
)

@Composable
fun DailyQuestPanel(quest: DailyQuestState, stars: Int, coins: Int) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .padding(start = 4.dp),
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
        color = SurfaceContainer,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎒", fontSize = 32.sp)
            Text(
                "Daily Quest",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Teal40
            )
            Spacer(Modifier.height(16.dp))

            Text(
                quest.title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            // Progress
            Text(
                "${quest.completed} / ${quest.total}",
                fontWeight = FontWeight.Bold,
                color = Teal40
            )
            LinearProgressIndicator(
                progress = { quest.completed.toFloat() / quest.total },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = SuccessGreen,
                trackColor = SuccessGreen.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(12.dp))

            // Rewards
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 20.sp)
                    Text("${quest.starReward}", fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🪙", fontSize = 20.sp)
                    Text("${quest.coinReward}", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Explore Every Day! 🌟",
                style = MaterialTheme.typography.labelSmall,
                color = Teal40,
                textAlign = TextAlign.Center
            )
        }
    }
}
