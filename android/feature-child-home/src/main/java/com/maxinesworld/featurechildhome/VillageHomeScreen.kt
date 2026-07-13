package com.maxinesworld.featurechildhome

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.maxinesworld.coredesignsystem.theme.*

data class VillageBuilding(
    val id: String, val label: String, val subject: String,
    val drawableRes: Int, val color: Color,
    val progress: Int, val total: Int,
    val locked: Boolean = false, val isToday: Boolean = false
)

@Composable
fun VillageHomeScreen(
    childName: String = "Maxine", level: Int = 12, xp: Int = 660, xpMax: Int = 900,
    dayStreak: Int = 7, questCompleted: Int = 3, questTotal: Int = 5,
    childId: String = "",
    onSubjectTap: (String) -> Unit = {}, onParentGate: () -> Unit = {},
    onAchievements: () -> Unit = {}, onBackpack: () -> Unit = {},
    onDailyQuest: () -> Unit = {}, onProfile: () -> Unit = {},
    appVersion: String = ""
) {
    val buildings = listOf(
        VillageBuilding("english", "Story Tree", "English", R.drawable.building_story_tree, StoryPurple, 7, 12),
        VillageBuilding("filipino", "Filipino", "Filipino", R.drawable.building_bahay, Coral, 4, 10),
        VillageBuilding("mathematics", "Number Market", "Math", R.drawable.building_number_market, SkyBlue, 8, 12, isToday = true),
        VillageBuilding("science", "Discovery Lab", "Science", R.drawable.building_discovery_lab, LeafGreen, 5, 12),
        VillageBuilding("makabansa", "Heritage Harbor", "Makabansa", R.drawable.building_heritage_harbor, Color(0xFFB8862B), 0, 5, locked = true),
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Scaffold(containerColor = Cream,
            bottomBar = { FooterNav(onProfile, onAchievements, onBackpack, onParentGate) }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                // Scene box
                Box(Modifier.weight(1f)) {
                    // Backdrop
                    Image(painterResource(R.drawable.village_backdrop), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    // Scrims
                    Box(Modifier.fillMaxWidth().fillMaxHeight(0.25f).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color(0x4D0B2A36), Color.Transparent))))
                    Box(Modifier.fillMaxWidth().fillMaxHeight(0.30f).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x850B2A36)))))
                    // Building row
                    LazyRow(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(buildings.size) { i ->
                            val b = buildings[i]
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onSubjectTap(b.id) }.widthIn(min = 88.dp).padding(horizontal = 2.dp)
                            ) {
                                Image(painterResource(b.drawableRes), b.label, Modifier.width(80.dp).height(90.dp), contentScale = ContentScale.Fit)
                                Surface(shape = RoundedCornerShape(8.dp), color = Cream, shadowElevation = 2.dp) {
                                    Column(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (b.isToday) Text("★ TODAY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Molasses)
                                        Text(b.label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Ink)
                                        if (b.locked) Text("Level 15", fontSize = 9.sp, color = Ink.copy(alpha = 0.4f))
                                        else {
                                            LinearProgressIndicator(b.progress.toFloat() / b.total, Modifier.width(50.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                color = b.color, trackColor = b.color.copy(alpha = 0.12f))
                                            Text("${b.progress}/${b.total}", fontSize = 9.sp, color = b.color)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Profile card
                    Card(Modifier.align(Alignment.TopStart).padding(12.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(Cream), elevation = CardDefaults.cardElevation(4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(Coral))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(childName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                                Text("Lv $level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StoryPurple)
                                LinearProgressIndicator(xp.toFloat() / xpMax, Modifier.width(100.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = SunshineGold, trackColor = Color(0xFFE6D9C2))
                                Text("$xp/$xpMax", fontSize = 10.sp, color = Ink.copy(alpha = 0.5f))
                            }
                        }
                    }
                    // Streak pill
                    Card(Modifier.align(Alignment.TopEnd).padding(12.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(Cream), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, null, tint = Coral, modifier = Modifier.size(18.dp))
                                Text("$dayStreak-day streak", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                            }
                            Text("Keep it going!", fontSize = 11.sp, color = LeafGreen)
                        }
                    }
                }
                // DailyQuest banner below scene
                Spacer(Modifier.height(4.dp))
                Card(onClick = onDailyQuest, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Cream),
                    elevation = CardDefaults.cardElevation(3.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, null, tint = StoryPurple, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Read a story with Mira and learn 5 new words!",
                            fontSize = 12.sp, color = Ink, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("$questCompleted/$questTotal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LeafGreen)
                        Icon(Icons.Default.ChevronRight, null, tint = Ink.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterNav(
    onProfile: () -> Unit, onAchievements: () -> Unit,
    onBackpack: () -> Unit, onParentGate: () -> Unit
) {
    NavigationBar(containerColor = White) {
        NavigationBarItem(selected = false, onClick = onProfile,
            icon = { Icon(Icons.Default.Person, "Profile", tint = VillageTeal) }, label = { Text("My Profile", fontSize = 11.sp) })
        NavigationBarItem(selected = false, onClick = onAchievements,
            icon = { Icon(Icons.Default.EmojiEvents, "Achievements", tint = VillageTeal) }, label = { Text("Achievements", fontSize = 11.sp) })
        NavigationBarItem(selected = false, onClick = onBackpack,
            icon = { Icon(Icons.Default.ShoppingBag, "Backpack", tint = VillageTeal) }, label = { Text("Backpack", fontSize = 11.sp) })
        NavigationBarItem(selected = false, onClick = onParentGate,
            icon = { Icon(Icons.Default.Lock, "Parents", tint = VillageTeal) }, label = { Text("Parents", fontSize = 11.sp) })
    }
}
