package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.featurerewards.ChallengeProgress
import com.maxinesworld.featurerewards.DailyChallengeProgressRow

// ─── Village Home Screen ───

@Composable
fun VillageHomeScreen(
    childName: String = "Maxine",
    level: Int = 12,
    xp: Int = 660,
    xpMax: Int = 900,
    dayStreak: Int = 7,
    questCompleted: Int = 3,
    questTotal: Int = 5,
    badgeAwarder: BadgeAwarder? = null,
    childId: String = "",
    onSubjectTap: (String) -> Unit = {},
    onParentGate: () -> Unit = {},
    onAchievements: () -> Unit = {},
    onBackpack: () -> Unit = {},
    onDailyQuest: () -> Unit = {},
    onProfile: () -> Unit = {},
    onMenu: () -> Unit = {},
    appVersion: String = ""
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp

        Scaffold(
            containerColor = Cream,
            bottomBar = {
                VillageFooterNav(onProfile, onAchievements, onBackpack, onParentGate)
            }
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ─── Header: Profile + Logo + Streak ───
                VillageHeader(childName, level, xp, xpMax, dayStreak, appVersion, onMenu)

                // ─── Background Scene with Building PNGs ───
                Box(Modifier.fillMaxWidth().height(if (isWide) 420.dp else 320.dp).clipToBounds()) {
                    // Reference background image
                    Image(painterResource(R.drawable.village_background), "Village",
                        Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth)
                    
                    // Daily Quest popup — floating left
                    DailyQuestPopup(
                        completed = questCompleted, total = questTotal,
                        onClick = onDailyQuest,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp, top = 8.dp)
                            .width(if (isWide) 180.dp else 150.dp)
                    )
                    
                    // Buildings along the path — anchored to bottom edge
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        BuildingNode("english", "Story Tree", R.drawable.building_story_tree, 7, 12,
                            color = StoryPurple, onTap = onSubjectTap)
                        BuildingNode("filipino", "Bahay Kuwento", R.drawable.building_bahay, 4, 10,
                            color = Coral, onTap = onSubjectTap)
                        BuildingNode("mathematics", "Number Market", R.drawable.building_number_market, 8, 12,
                            color = SkyBlue, isToday = true, onTap = onSubjectTap)
                        BuildingNode("science", "Discovery Lab", R.drawable.building_discovery_lab, 5, 12,
                            color = LeafGreen, onTap = onSubjectTap)
                        BuildingNode("makabansa", "Heritage Harbor", R.drawable.building_heritage_harbor, 0, 15,
                            color = Color(0xFFB87916), locked = true, onTap = onSubjectTap)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ─── Building Node (PNG anchored to ground + tag card) ───

@Composable
private fun BuildingNode(
    subjectId: String,
    label: String,
    @DrawableRes buildingRes: Int,
    progress: Int, total: Int,
    color: Color = VillageTeal,
    isToday: Boolean = false,
    locked: Boolean = false,
    onTap: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (locked) 90.dp else 110.dp)
            .clickable(enabled = !locked) { onTap(subjectId) }
    ) {
        // Building PNG — scaled to fit, bottom grounded
        Image(
            painterResource(buildingRes),
            "$label building",
            Modifier
                .width(if (locked) 70.dp else 90.dp)
                .height(if (locked) 55.dp else 80.dp),
            contentScale = ContentScale.FillBounds
        )
        
        // Mini tag card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Cream,
            shadowElevation = 2.dp
        ) {
            Column(
                Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isToday) {
                    Surface(shape = RoundedCornerShape(3.dp), color = SunshineGold.copy(alpha = 0.25f)) {
                        Text("TODAY", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2100),
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                    }
                }
                Text(label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (locked) Ink.copy(alpha = 0.4f) else Ink,
                    maxLines = 1, textAlign = TextAlign.Center)
                
                if (locked) {
                    Icon(Icons.Default.Lock, "Locked", tint = Ink.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
                } else {
                    LinearProgressIndicator(
                        progress = { progress.toFloat() / total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = color, trackColor = color.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

// ─── Header: Profile (left) + Logo (center) + Streak + Menu (right) ───

@Composable
private fun VillageHeader(
    name: String, level: Int, xp: Int, xpMax: Int,
    dayStreak: Int, appVersion: String, onMenu: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Coral)) {
                Image(painterResource(R.drawable.milo), "Avatar",
                    Modifier.size(40.dp).clip(CircleShape).align(Alignment.Center),
                    contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = SunshineGold.copy(alpha = 0.2f)) {
                        Text("Lv $level", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2100), modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                LinearProgressIndicator(progress = { xp.toFloat() / xpMax.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = VillageTeal, trackColor = VillageTeal.copy(alpha = 0.12f))
                Text("$xp/$xp", fontSize = 10.sp, color = Ink.copy(alpha = 0.4f))
            }
        }

        // Center: Logo
        Text("Maxine's World", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
            color = VillageTeal, modifier = Modifier.padding(horizontal = 8.dp))

        // Right: Streak + Menu
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(100.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, "Streak", tint = Coral, modifier = Modifier.size(16.dp))
                Text("$dayStreak-day", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Coral, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Day Streak!", fontSize = 10.sp, color = Ink.copy(alpha = 0.5f))
            Row { repeat(4) { Icon(Icons.Default.Star, null, tint = SunshineGold, modifier = Modifier.size(12.dp)) }
                Icon(Icons.Default.Star, null, tint = SunshineGold.copy(alpha = 0.2f), modifier = Modifier.size(12.dp)) }
        }

        // Menu button
        IconButton(onClick = onMenu, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Menu, "Menu", tint = VillageTeal, modifier = Modifier.size(22.dp))
        }
    }
}

// ─── Village Scene (background + buildings + characters) ───

// ─── Daily Quest Popup ───

@Composable
private fun DailyQuestPopup(completed: Int, total: Int, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = StoryPurple.copy(alpha = 0.15f)) {
                    Icon(Icons.Default.MenuBook, "Quest", tint = StoryPurple,
                        modifier = Modifier.padding(8.dp).size(24.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Daily Quest", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
            }
            Spacer(Modifier.height(8.dp))
            Text("Read a story with Mira and learn 5 new words!", fontSize = 13.sp,
                color = Ink.copy(alpha = 0.7f), lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { completed.toFloat() / total },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = SuccessGreen, trackColor = SuccessGreen.copy(alpha = 0.15f))
                Spacer(Modifier.width(8.dp))
                Text("$completed/$total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Spacer(Modifier.height(12.dp))
            MaxinesPrimaryButton(onClick = onClick, text = "Start", containerColor = SuccessGreen,
                modifier = Modifier.fillMaxWidth(), height = 44.dp)
        }
    }
}

// ─── Subject Destination Card Row ───

data class SubjectCardData(
    val id: String, val label: String, val subject: String,
    val icon: ImageVector, val color: Color,
    val progress: Int, val total: Int,
    val locked: Boolean = false, val lockLevel: Int = 0,
    val isToday: Boolean = false
)

private val subjectCards = listOf(
    SubjectCardData("english", "Story Tree", "English", Icons.Default.MenuBook, StoryPurple, 7, 12),
    SubjectCardData("filipino", "Bahay Kuwento", "Filipino", Icons.Default.AutoStories, Coral, 4, 10),
    SubjectCardData("mathematics", "Number Market", "Mathematics", Icons.Default.Calculate, SkyBlue, 8, 12, isToday = true),
    SubjectCardData("science", "Discovery Lab", "Science", Icons.Default.Science, LeafGreen, 5, 12),
    SubjectCardData("makabansa", "Heritage Harbor", "Makabansa", Icons.Default.Flag, Color(0xFFB87916), 0, 15, locked = true, lockLevel = 15)
)

@Composable
private fun SubjectCardRow(onSubjectTap: (String) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(subjectCards.size) { index ->
            val card = subjectCards[index]
            SubjectCard(card, onSubjectTap)
        }
    }
}

@Composable
private fun SubjectCard(card: SubjectCardData, onTap: (String) -> Unit) {
    Card(
        Modifier.width(150.dp).clickable(enabled = !card.locked) { onTap(card.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (card.locked) White.copy(alpha = 0.7f) else White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = card.color.copy(alpha = 0.12f)) {
                    Icon(if (card.locked) Icons.Default.Lock else card.icon, null,
                        tint = if (card.locked) Ink.copy(alpha = 0.3f) else card.color,
                        modifier = Modifier.padding(6.dp).size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                if (card.isToday) {
                    Surface(shape = RoundedCornerShape(4.dp), color = SunshineGold.copy(alpha = 0.2f)) {
                        Text("TODAY", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2100), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(card.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (card.locked) Ink.copy(alpha = 0.4f) else Ink)
            Text(card.subject, fontSize = 11.sp, color = card.color)
            Spacer(Modifier.height(6.dp))
            if (card.locked) {
                Text("Reach Level ${card.lockLevel} to open", fontSize = 10.sp, color = Ink.copy(alpha = 0.45f))
            } else {
                LinearProgressIndicator(progress = { card.progress.toFloat() / card.total.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = card.color, trackColor = card.color.copy(alpha = 0.1f))
                Spacer(Modifier.height(2.dp))
                Text("${card.progress}/${card.total}", fontSize = 10.sp, color = Ink.copy(alpha = 0.4f))
            }
        }
    }
}

// ─── Footer Navigation ───

@Composable
private fun VillageFooterNav(
    onProfile: () -> Unit, onAchievements: () -> Unit,
    onBackpack: () -> Unit, onParentGate: () -> Unit
) {
    NavigationBar(containerColor = White) {
        NavigationBarItem(selected = false, onClick = onProfile,
            icon = { Icon(Icons.Default.Person, "Profile", tint = VillageTeal) },
            label = { Text("My Profile", fontSize = 11.sp) })
        NavigationBarItem(selected = false, onClick = onAchievements,
            icon = { Icon(Icons.Default.EmojiEvents, "Achievements", tint = VillageTeal) },
            label = { Text("Achievements", fontSize = 11.sp) })
        NavigationBarItem(selected = false, onClick = onBackpack,
            icon = { Icon(Icons.Default.ShoppingBag, "Backpack", tint = VillageTeal) },
            label = { Text("Backpack", fontSize = 11.sp) })
        NavigationBarItem(selected = false, onClick = onParentGate,
            icon = { Icon(Icons.Default.Lock, "Parents", tint = VillageTeal) },
            label = { Text("Parents", fontSize = 11.sp) })
    }
}
