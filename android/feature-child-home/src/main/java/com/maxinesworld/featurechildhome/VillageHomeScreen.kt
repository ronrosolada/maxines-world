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
    // §7 Responsive Layout: centered container with max width constraint.
    // Do NOT place fillMaxSize() before widthIn(max = 1440.dp) on the same content node.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 1440.dp).fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val screenW = maxWidth
                // Scale factor: 1.0 at 360dp phone, ~1.8 at 1524dp Xiaomi tablet
                val scale = (screenW / 600.dp).coerceIn(1.0f, 2.0f)
                val fontScale = scale.coerceIn(1.0f, 1.5f)
                val sceneH = (280.dp * scale).coerceAtLeast(280.dp)
                val buildW = (90.dp * scale).coerceIn(90.dp, 140.dp)
                val buildH = (80.dp * scale).coerceIn(80.dp, 120.dp)
                val questW = (180.dp * scale).coerceIn(180.dp, 260.dp)
                val isTablet = screenW > 840.dp

                Scaffold(
                    containerColor = Cream,
                    bottomBar = {
                        VillageFooterNav(onProfile, onAchievements, onBackpack, onParentGate, scale)
                    }
                ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ─── Header: Profile + Logo + Streak ───
                VillageHeader(childName, level, xp, xpMax, dayStreak, appVersion, onMenu, scale)

                // ─── Background Scene with Building PNGs ───
                Box(Modifier.fillMaxWidth().height(sceneH).clipToBounds()) {
                    // Reference background image
                    Image(painterResource(R.drawable.village_background), "Village",
                        Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth)
                    
                    // Daily Quest popup — floating top-left, above buildings
                    DailyQuestPopup(
                        completed = questCompleted, total = questTotal,
                        onClick = onDailyQuest,
                        scale = scale,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = (12.dp * scale).coerceAtLeast(12.dp), top = (8.dp * scale))
                            .width(questW)
                    )
                    
                    // Transparent tap zones over buildings in the bg image
                    // Story Tree (English) — left, mid-lower area (below popup)
                    Box(Modifier.fillMaxWidth(0.25f).fillMaxHeight(0.35f).align(Alignment.CenterStart)
                        .offset(y = 60.dp) // shift below popup
                        .clickable { onSubjectTap("english") })
                    // Bahay Kuwento (Filipino) — left, lower
                    Box(Modifier.fillMaxWidth(0.25f).fillMaxHeight(0.35f).align(Alignment.BottomStart)
                        .clickable { onSubjectTap("filipino") })
                    // Number Market (Math) — center
                    Box(Modifier.fillMaxWidth(0.3f).fillMaxHeight(0.7f).align(Alignment.Center)
                        .clickable { onSubjectTap("mathematics") })
                    // Discovery Lab (Science) — right upper
                    Box(Modifier.fillMaxWidth(0.25f).fillMaxHeight(0.6f).align(Alignment.CenterEnd)
                        .clickable { onSubjectTap("science") })
                    // Heritage Harbor (Makabansa) — right lower
                    Box(Modifier.fillMaxWidth(0.2f).fillMaxHeight(0.35f).align(Alignment.BottomEnd)
                        .clickable { onSubjectTap("makabansa") })
                    // Kindness Corner (GMRC) — bottom center-right
                    Box(Modifier.fillMaxWidth(0.15f).fillMaxHeight(0.3f).align(Alignment.BottomCenter)
                        .clickable { onSubjectTap("gmrc") })
                    
                    // Floating subject labels for buildings not prominent in bg art
                    SubjectLabel("Bahay Kuwento", "Filipino", Coral,
                        Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 48.dp),
                        onSubjectTap = onSubjectTap)
                    SubjectLabel("Heritage Harbor", "Makabansa", HeritageGold,
                        Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 40.dp),
                        onSubjectTap = onSubjectTap)
                    SubjectLabel("Kindness Corner", "GMRC", KindnessTeal,
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                        onSubjectTap = onSubjectTap)
                }

                Spacer(Modifier.height((12.dp * scale).coerceAtLeast(12.dp)))
            }
        }
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
    scale: Float = 1.0f,
    buildW: Dp = 90.dp,
    buildH: Dp = 80.dp,
    onTap: (String) -> Unit
) {
    val fontScale = scale.coerceIn(1.0f, 1.5f)
    val nodeW = (if (locked) 90.dp else 110.dp) * scale
    val lockW = (if (locked) 70.dp else buildW)
    val lockH = (if (locked) 55.dp else buildH)
    val tagTextSize = 10.sp * scale.coerceIn(1.0f, 1.4f)
    val todayTextSize = 8.sp * scale.coerceIn(1.0f, 1.4f)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = nodeW, max = nodeW * 1.5f)
            .clickable(enabled = !locked) { onTap(subjectId) }
    ) {
        // Building PNG — scaled to fit, bottom grounded
        Image(
            painterResource(buildingRes),
            "$label building",
            Modifier.size(lockW, lockH),
            contentScale = ContentScale.Fit
        )
        
        // Mini tag card
        Surface(
            shape = RoundedCornerShape((8.dp * scale).coerceIn(6.dp, 12.dp)),
            color = Cream,
            shadowElevation = 2.dp
        ) {
            Column(
                Modifier.padding(horizontal = (6.dp * scale).coerceIn(4.dp, 10.dp), vertical = (3.dp * scale).coerceIn(2.dp, 6.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isToday) {
                    Surface(shape = RoundedCornerShape(3.dp), color = SunshineGold.copy(alpha = 0.25f)) {
                        Text("TODAY", fontSize = todayTextSize, fontWeight = FontWeight.Bold,
                            color = Molasses,
                            modifier = Modifier.padding(horizontal = (3.dp * scale).coerceIn(2.dp, 5.dp), vertical = 1.dp))
                    }
                }
                Text(label, fontWeight = FontWeight.Bold, fontSize = tagTextSize,
                    color = if (locked) Ink.copy(alpha = 0.4f) else Ink,
                    maxLines = 1, textAlign = TextAlign.Center)
                
                if (locked) {
                    Icon(Icons.Default.Lock, "Locked", tint = Ink.copy(alpha = 0.3f),
                        modifier = Modifier.size((12.dp * scale).coerceIn(10.dp, 18.dp)))
                } else {
                    LinearProgressIndicator(
                        progress = { progress.toFloat() / total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(0.8f).height((3.dp * scale).coerceIn(2.dp, 5.dp)).clip(RoundedCornerShape(2.dp)),
                        color = color, trackColor = color.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

// ─── Floating Subject Label ───

@Composable
private fun SubjectLabel(
    label: String, subject: String, color: Color,
    modifier: Modifier, onSubjectTap: (String) -> Unit
) {
    val subjectId = when (subject) {
        "Filipino" -> "filipino"
        "Makabansa" -> "makabansa"
        "GMRC" -> "gmrc"
        else -> subject.lowercase()
    }
    Surface(
        modifier.clickable { onSubjectTap(subjectId) },
        shape = RoundedCornerShape(8.dp),
        color = Cream,
        shadowElevation = 3.dp
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Ink)
                Text(subject, fontSize = 9.sp, color = color)
            }
        }
    }
}

// ─── Header: Profile (left) + Logo (center) + Streak + Menu (right) ───

@Composable
private fun VillageHeader(
    name: String, level: Int, xp: Int, xpMax: Int,
    dayStreak: Int, appVersion: String, onMenu: () -> Unit,
    scale: Float = 1.0f
) {
    val fontScale = scale.coerceIn(1.0f, 1.5f)
    val headerPad = (12.dp * scale).coerceIn(10.dp, 20.dp)
    val avatarSize = (44.dp * scale).coerceIn(44.dp, 64.dp)
    val nameSize = 16.sp * fontScale
    val titleSize = 20.sp * fontScale
    val streakSize = 11.sp * fontScale
    
    Row(
        Modifier.fillMaxWidth().padding(horizontal = headerPad, vertical = (8.dp * scale).coerceIn(6.dp, 12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(avatarSize).clip(CircleShape).background(Coral)) {
                Image(painterResource(R.drawable.milo), "Avatar",
                    Modifier.fillMaxSize(0.9f).clip(CircleShape).align(Alignment.Center),
                    contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width((8.dp * scale).coerceIn(6.dp, 12.dp)))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = nameSize, color = Ink)
                    Spacer(Modifier.width((6.dp * scale).coerceIn(4.dp, 8.dp)))
                    Surface(shape = RoundedCornerShape(4.dp), color = SunshineGold.copy(alpha = 0.2f)) {
                        Text("Lv $level", fontSize = 11.sp * fontScale, fontWeight = FontWeight.Bold,
                            color = Molasses, modifier = Modifier.padding(horizontal = (6.dp * scale).coerceIn(4.dp, 8.dp), vertical = 1.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                LinearProgressIndicator(progress = { xp.toFloat() / xpMax.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(0.7f).height((6.dp * scale).coerceIn(4.dp, 8.dp)).clip(RoundedCornerShape(3.dp)),
                    color = VillageTeal, trackColor = VillageTeal.copy(alpha = 0.12f))
                Text("$xp/$xpMax", fontSize = 10.sp * fontScale, color = Ink.copy(alpha = 0.4f))
            }
        }

        // Center: Logo
        Text("Maxine's World", fontWeight = FontWeight.ExtraBold, fontSize = titleSize,
            color = VillageTeal, modifier = Modifier.padding(horizontal = (8.dp * scale).coerceIn(6.dp, 12.dp)))

        // Right: Streak + Menu
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = (100.dp * scale).coerceIn(80.dp, 140.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, "Streak", tint = Coral, modifier = Modifier.size((16.dp * scale).coerceIn(14.dp, 22.dp)))
                Text("$dayStreak-day", fontSize = streakSize, fontWeight = FontWeight.Bold, color = Coral,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Day Streak!", fontSize = 10.sp * fontScale, color = Ink.copy(alpha = 0.5f))
            Row { repeat(4) { Icon(Icons.Default.Star, null, tint = SunshineGold, modifier = Modifier.size((12.dp * scale).coerceIn(10.dp, 16.dp))) }
                Icon(Icons.Default.Star, null, tint = SunshineGold.copy(alpha = 0.2f), modifier = Modifier.size((12.dp * scale).coerceIn(10.dp, 16.dp))) }
        }

        // Menu button — §9 touch target: minimum 48dp
        IconButton(onClick = onMenu, modifier = Modifier.size((48.dp * scale).coerceIn(48.dp, 56.dp))) {
            Icon(Icons.Default.Menu, "Menu", tint = VillageTeal, modifier = Modifier.size((22.dp * scale).coerceIn(20.dp, 28.dp)))
        }
    }
}

// ─── Village Scene (background + buildings + characters) ───

// ─── Daily Quest Popup ───

@Composable
private fun DailyQuestPopup(completed: Int, total: Int, onClick: () -> Unit,
    scale: Float = 1.0f, modifier: Modifier) {
    val fontScale = scale.coerceIn(1.0f, 1.5f)
    val questPad = (16.dp * scale).coerceIn(12.dp, 24.dp)
    val questTextSize = 13.sp * fontScale
    val questBtnHeight = (56.dp * scale).coerceIn(56.dp, 64.dp)  // §9: 56dp minimum for primary child actions
    
    Card(modifier, shape = RoundedCornerShape((16.dp * scale).coerceIn(12.dp, 20.dp)),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(questPad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape((8.dp * scale).coerceIn(6.dp, 10.dp)), color = StoryPurple.copy(alpha = 0.15f)) {
                    Icon(Icons.Default.MenuBook, "Quest", tint = StoryPurple,
                        modifier = Modifier.padding((8.dp * scale).coerceIn(6.dp, 10.dp)).size((24.dp * scale).coerceIn(20.dp, 32.dp)))
                }
                Spacer(Modifier.width((8.dp * scale).coerceIn(6.dp, 12.dp)))
                Text("Daily Quest", fontWeight = FontWeight.Bold, fontSize = 14.sp * fontScale, color = Ink)
            }
            Spacer(Modifier.height((8.dp * scale).coerceIn(6.dp, 12.dp)))
            Text("Read a story with Mira and learn 5 new words!", fontSize = questTextSize,
                color = Ink.copy(alpha = 0.7f), lineHeight = 18.sp * fontScale)
            Spacer(Modifier.height((10.dp * scale).coerceIn(6.dp, 14.dp)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { completed.toFloat() / total },
                    modifier = Modifier.weight(1f).height((8.dp * scale).coerceIn(6.dp, 10.dp)).clip(RoundedCornerShape(4.dp)),
                    color = SuccessGreen, trackColor = SuccessGreen.copy(alpha = 0.15f))
                Spacer(Modifier.width((8.dp * scale).coerceIn(6.dp, 12.dp)))
                Text("$completed/$total", fontSize = 12.sp * fontScale, fontWeight = FontWeight.Bold, color = Ink)
            }
            Spacer(Modifier.height((12.dp * scale).coerceIn(8.dp, 16.dp)))
            MaxinesPrimaryButton(onClick = onClick, text = "Start", containerColor = SuccessGreen,
                modifier = Modifier.fillMaxWidth(), height = questBtnHeight)
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
    SubjectCardData("makabansa", "Heritage Harbor", "Makabansa", Icons.Default.Flag, HeritageGold, 0, 15, locked = true, lockLevel = 15)
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
                            color = Molasses, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
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
    onBackpack: () -> Unit, onParentGate: () -> Unit,
    scale: Float = 1.0f
) {
    val fontScale = scale.coerceIn(1.0f, 1.5f)
    NavigationBar(containerColor = White) {
        NavigationBarItem(selected = false, onClick = onProfile,
            icon = { Icon(Icons.Default.Person, "Profile", tint = VillageTeal,
                modifier = Modifier.size((24.dp * scale).coerceIn(20.dp, 28.dp))) },
            label = { Text("My Profile", fontSize = 11.sp * fontScale) })
        NavigationBarItem(selected = false, onClick = onAchievements,
            icon = { Icon(Icons.Default.EmojiEvents, "Achievements", tint = VillageTeal,
                modifier = Modifier.size((24.dp * scale).coerceIn(20.dp, 28.dp))) },
            label = { Text("Achievements", fontSize = 11.sp * fontScale) })
        NavigationBarItem(selected = false, onClick = onBackpack,
            icon = { Icon(Icons.Default.ShoppingBag, "Backpack", tint = VillageTeal,
                modifier = Modifier.size((24.dp * scale).coerceIn(20.dp, 28.dp))) },
            label = { Text("Backpack", fontSize = 11.sp * fontScale) })
        NavigationBarItem(selected = false, onClick = onParentGate,
            icon = { Icon(Icons.Default.Lock, "Parents", tint = VillageTeal,
                modifier = Modifier.size((24.dp * scale).coerceIn(20.dp, 28.dp))) },
            label = { Text("Parents", fontSize = 11.sp * fontScale) })
    }
}
