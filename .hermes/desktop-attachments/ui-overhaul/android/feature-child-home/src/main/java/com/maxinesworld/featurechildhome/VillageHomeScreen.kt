package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.theme.Coral
import com.maxinesworld.coredesignsystem.theme.Cream
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.LeafGreen
import com.maxinesworld.coredesignsystem.theme.SkyBlue
import com.maxinesworld.coredesignsystem.theme.StoryPurple
import com.maxinesworld.coredesignsystem.theme.SuccessGreen
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.VillageTeal

private val GmrcTeal = Color(0xFF0B8585)
private val HistoryGold = Color(0xFFB87916)
private val ChildSurface = Color(0xFFFFFBF2)

/** One authoritative destination model. Do not maintain a second subject list. */
data class SubjectDestination(
    val id: String,
    val name: String,
    val subject: String,
    @DrawableRes val buildingRes: Int,
    @DrawableRes val guideRes: Int,
    val color: Color,
    val progress: Float,
    val available: Boolean = true,
    val recommended: Boolean = false,
)

private val villageDestinations = listOf(
    SubjectDestination(
        id = "english",
        name = "Story Tree",
        subject = "English",
        buildingRes = R.drawable.location_story_tree,
        guideRes = R.drawable.character_mira,
        color = StoryPurple,
        progress = 0.42f,
        recommended = true,
    ),
    SubjectDestination(
        id = "filipino",
        name = "Bahay ng Kuwento",
        subject = "Filipino",
        buildingRes = R.drawable.location_bahay_kuwento,
        guideRes = R.drawable.character_mira,
        color = Coral,
        progress = 0.25f,
    ),
    SubjectDestination(
        id = "mathematics",
        name = "Number Market",
        subject = "Mathematics",
        buildingRes = R.drawable.location_number_market,
        guideRes = R.drawable.character_milo,
        color = SkyBlue,
        progress = 0.58f,
    ),
    SubjectDestination(
        id = "science",
        name = "Discovery Lab",
        subject = "Science",
        buildingRes = R.drawable.location_discovery_lab,
        guideRes = R.drawable.character_niko,
        color = LeafGreen,
        progress = 0.33f,
    ),
    SubjectDestination(
        id = "philippine-history",
        name = "Heritage Harbor",
        subject = "Makabansa",
        buildingRes = R.drawable.location_heritage_harbor,
        guideRes = R.drawable.character_lakan,
        color = HistoryGold,
        progress = 0.16f,
    ),
    SubjectDestination(
        id = "gmrc",
        name = "Kindness Corner",
        subject = "GMRC",
        buildingRes = R.drawable.location_kindness_corner,
        guideRes = R.drawable.character_duke,
        color = GmrcTeal,
        progress = 0f,
        available = false,
    ),
)

data class DailyQuestState(
    val title: String = "Read a story and discover 5 new words",
    val subjectId: String = "english",
    val completed: Int = 3,
    val total: Int = 5,
    val starReward: Int = 25,
    val coinReward: Int = 10,
    val minutes: Int = 8,
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
    onSubjectTap: (String) -> Unit,
    onParentGate: () -> Unit,
    onProfile: (() -> Unit)? = null,
    onAchievements: (() -> Unit)? = null,
    onBackpack: (() -> Unit)? = null,
) {
    Scaffold(
        containerColor = Cream,
        topBar = {
            PlayerHud(
                name = childName,
                level = level,
                xp = xp,
                xpMax = xpMax,
                dayStreak = dayStreak,
                stars = stars,
                coins = coins,
                onProfile = onProfile,
            )
        },
        bottomBar = {
            VillageNavigation(
                onProfile = onProfile,
                onAchievements = onAchievements,
                onBackpack = onBackpack,
                onParentGate = onParentGate,
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Cream),
        ) {
            val wide = maxWidth >= 700.dp
            if (wide) {
                WideVillageHome(
                    quest = dailyQuest,
                    onSubjectTap = onSubjectTap,
                )
            } else {
                CompactVillageHome(
                    quest = dailyQuest,
                    onSubjectTap = onSubjectTap,
                )
            }
        }
    }
}

@Composable
private fun WideVillageHome(
    quest: DailyQuestState,
    onSubjectTap: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        VillageScene(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1440.dp)
                .aspectRatio(16f / 9f)
                .heightIn(min = 480.dp),
            onSubjectTap = onSubjectTap,
        )
        DailyQuestCard(
            quest = quest,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .widthIn(max = 610.dp),
            onStart = { onSubjectTap(quest.subjectId) },
        )
    }
}

@Composable
private fun CompactVillageHome(
    quest: DailyQuestState,
    onSubjectTap: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Choose today’s adventure",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
        DailyQuestCard(
            quest = quest,
            modifier = Modifier.fillMaxWidth(),
            onStart = { onSubjectTap(quest.subjectId) },
        )
        villageDestinations.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { destination ->
                    CompactDestinationCard(
                        destination = destination,
                        modifier = Modifier.weight(1f),
                        onTap = onSubjectTap,
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun VillageScene(
    modifier: Modifier = Modifier,
    onSubjectTap: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF9EDB7B)),
    ) {
        Image(
            painter = painterResource(R.drawable.village_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(0.24f))
            villageDestinations.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.31f)
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { destination ->
                        DestinationMarker(
                            destination = destination,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onTap = onSubjectTap,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(0.14f))
        }
    }
}

@Composable
private fun DestinationMarker(
    destination: SubjectDestination,
    modifier: Modifier = Modifier,
    onTap: (String) -> Unit,
) {
    val state = when {
        !destination.available -> "Locked. Adventure opening soon"
        destination.progress >= 1f -> "Completed"
        destination.progress > 0f -> "${(destination.progress * 100).toInt()} percent complete"
        else -> "Ready to begin"
    }
    Box(
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                enabled = destination.available,
                role = Role.Button,
                onClick = { onTap(destination.id) },
            )
            .semantics {
                role = Role.Button
                contentDescription = "${destination.name}, ${destination.subject}"
                stateDescription = state
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painter = painterResource(destination.buildingRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxHeight(0.86f)
                .fillMaxWidth(0.9f)
                .align(Alignment.TopCenter),
        )
        Image(
            painter = painterResource(destination.guideRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 38.dp)
                .size(58.dp),
        )
        DestinationLabel(destination = destination)
    }
}

@Composable
private fun DestinationLabel(destination: SubjectDestination) {
    Surface(
        color = ChildSurface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 6.dp,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .heightIn(min = 58.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!destination.available) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (destination.available) {
                    "${destination.subject} · ${(destination.progress * 100).toInt()}%"
                } else {
                    "${destination.subject} · Opening soon"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = destination.color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactDestinationCard(
    destination: SubjectDestination,
    modifier: Modifier,
    onTap: (String) -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 220.dp)
            .clickable(
                enabled = destination.available,
                role = Role.Button,
                onClick = { onTap(destination.id) },
            )
            .semantics {
                role = Role.Button
                contentDescription = "${destination.name}, ${destination.subject}"
                stateDescription = if (destination.available) {
                    "${(destination.progress * 100).toInt()} percent complete"
                } else {
                    "Locked. Adventure opening soon"
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = destination.color.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
            ) {
                Image(
                    painter = painterResource(destination.buildingRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(destination.guideRes),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp),
                )
            }
            Text(
                text = destination.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                text = if (destination.available) destination.subject else "${destination.subject} · Opening soon",
                style = MaterialTheme.typography.labelSmall,
                color = destination.color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DailyQuestCard(
    quest: DailyQuestState,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
) {
    val safeTotal = quest.total.coerceAtLeast(1)
    val progress = (quest.completed.toFloat() / safeTotal).coerceIn(0f, 1f)
    Surface(
        modifier = modifier,
        color = ChildSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                color = StoryPurple.copy(alpha = 0.12f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.character_mira),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(72.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "Daily Quest · ${quest.minutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = VillageTeal,
                )
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = SuccessGreen,
                    trackColor = SuccessGreen.copy(alpha = 0.18f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RewardLabel(Icons.Rounded.Star, quest.starReward, SunshineGold)
                    RewardLabel(Icons.Rounded.Paid, quest.coinReward, HistoryGold)
                    Text(
                        text = "${quest.completed}/${quest.total} steps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                }
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .height(60.dp)
                    .widthIn(min = 116.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VillageTeal,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (quest.completed > 0) "Continue" else "Start",
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun RewardLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
        )
    }
}

@Composable
private fun PlayerHud(
    name: String,
    level: Int,
    xp: Int,
    xpMax: Int,
    dayStreak: Int,
    stars: Int,
    coins: Int,
    onProfile: (() -> Unit)?,
) {
    Surface(color = ChildSurface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .requiredSize(58.dp)
                    .clickable(enabled = onProfile != null, role = Role.Button) { onProfile?.invoke() }
                    .semantics {
                        role = Role.Button
                        contentDescription = "$name profile"
                        stateDescription = if (onProfile == null) "Coming soon" else "Open profile"
                    },
                shape = CircleShape,
                color = Coral.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(3.dp, SunshineGold),
            ) {
                Image(
                    painter = painterResource(R.drawable.character_milo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(2.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hi, $name!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = VillageTeal,
                    )
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { (xp.toFloat() / xpMax.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .widthIn(max = 160.dp)
                            .weight(1f, fill = false)
                            .height(7.dp)
                            .clip(CircleShape),
                        color = StoryPurple,
                        trackColor = StoryPurple.copy(alpha = 0.16f),
                    )
                }
            }
            HudChip(Icons.Rounded.LocalFireDepartment, dayStreak, Coral, "day streak")
            HudChip(Icons.Rounded.Star, stars, SunshineGold, "stars")
            HudChip(Icons.Rounded.Paid, coins, HistoryGold, "paw coins")
        }
    }
}

@Composable
private fun HudChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    color: Color,
    description: String,
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.semantics { contentDescription = "$value $description" },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(value.toString(), fontWeight = FontWeight.ExtraBold, color = Ink)
        }
    }
}

@Composable
private fun VillageNavigation(
    onProfile: (() -> Unit)?,
    onAchievements: (() -> Unit)?,
    onBackpack: (() -> Unit)?,
    onParentGate: () -> Unit,
) {
    NavigationBar(containerColor = ChildSurface, tonalElevation = 4.dp) {
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = false,
            enabled = onProfile != null,
            onClick = { onProfile?.invoke() },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null) },
            label = { Text("Achievements") },
            selected = false,
            enabled = onAchievements != null,
            onClick = { onAchievements?.invoke() },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.Backpack, contentDescription = null) },
            label = { Text("Backpack") },
            selected = false,
            enabled = onBackpack != null,
            onClick = { onBackpack?.invoke() },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Rounded.Shield, contentDescription = null) },
            label = { Text("Parents") },
            selected = false,
            onClick = onParentGate,
        )
    }
}
