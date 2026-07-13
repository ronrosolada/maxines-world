package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

private val Ink = Color(0xFF123E45)
private val English = Color(0xFF7653B5)
private val Filipino = Color(0xFFD95D57)
private val Math = Color(0xFF2189C5)
private val Science = Color(0xFF4C9638)
private val History = Color(0xFFB66D0A)
private val Gmrc = Color(0xFF087F83)
private val Gold = Color(0xFFF0AF28)

data class VillageDestinationV16(
    val id: String, val destination: String, val subject: String,
    val progressText: String, val accent: Color, val centerX: Float, val centerY: Float,
    val enabled: Boolean = true, val today: Boolean = false,
)

val DefaultDestinationsV16 = listOf(
    VillageDestinationV16("english", "Story Tree", "Reading", "42%", English, .17f, .43f),
    VillageDestinationV16("filipino", "Bahay ng Kuwento", "Filipino", "21%", Filipino, .54f, .45f),
    VillageDestinationV16("mathematics", "Number Market", "Math", "67%", Math, .86f, .43f, today = true),
    VillageDestinationV16("science", "Discovery Lab", "Science", "17%", Science, .17f, .68f),
    VillageDestinationV16("history", "Heritage Harbor", "Araling Panlipunan", "14%", History, .50f, .75f),
    VillageDestinationV16("gmrc", "Kindness Corner", "Values", "18%", Gmrc, .83f, .75f),
)

@Composable
fun VillageChromeV16(
    modifier: Modifier = Modifier,
    destinations: List<VillageDestinationV16> = DefaultDestinationsV16,
    onDestinationClick: (String) -> Unit = {},
    childName: String = "Maxine",
    level: Int = 12, currentXp: Int = 660, targetXp: Int = 900,
    streak: Int = 7, stars: Int = 1200, coins: Int = 567,
    questTitle: String = "Read a story in Story Tree",
    questProgress: String = "0 / 1",
    onQuestClick: () -> Unit = {},
    onAchievementsClick: () -> Unit = {},
    onParentsClick: () -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val expanded = maxWidth >= 840.dp
        if (!expanded) {
            CompactVillageChromeV16(destinations, onDestinationClick, childName, level, currentXp, targetXp, streak, stars, coins, questTitle, questProgress, onQuestClick, onAchievementsClick, onParentsClick)
            return@BoxWithConstraints
        }
        Box(Modifier.fillMaxSize()) {
            destinations.forEach { item ->
                SubjectPlaqueV16(item = item, onClick = { onDestinationClick(item.id) },
                    modifier = Modifier.sceneAnchor(item.centerX, item.centerY, 196.dp, 84.dp))
            }
            ArtPanelV16(R.drawable.mw_profile_panel, Modifier.offset(20.dp, 20.dp).size(280.dp, 104.dp)) {
                ProfileContentV16(childName, level, currentXp, targetXp)
            }
            ArtPanelV16(R.drawable.mw_quest_panel, Modifier.offset(20.dp, 132.dp).size(340.dp, 104.dp)) {
                QuestContentV16(questTitle, questProgress, onQuestClick)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RewardChipV16(R.drawable.ic_flame, "$streak", Color(0xFFF47C6B))
                RewardChipV16(R.drawable.ic_star, if (stars >= 1000) "${stars / 1000}.${(stars % 1000) / 100}k" else "$stars", Gold)
                RewardChipV16(R.drawable.ic_coin, "$coins", Color(0xFFB87916))
            }
            ArtPanelV16(R.drawable.mw_bottom_nav, Modifier.align(Alignment.BottomCenter).padding(horizontal = 40.dp).padding(bottom = 10.dp).fillMaxWidth().height(84.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    NavItemV16("Profile", R.drawable.ic_profile, {})
                    NavItemV16("Achievements", R.drawable.ic_achievements, onAchievementsClick)
                    NavItemV16("Backpack", R.drawable.ic_backpack, {})
                    NavItemV16("Parents", R.drawable.ic_parent, onParentsClick)
                }
            }
        }
    }
}

@Composable
private fun ProfileContentV16(name: String, level: Int, xp: Int, xpMax: Int) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFF47C6B)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_profile), null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("Hi, $name!", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text("Lv $level   $xp / $xpMax XP", color = Ink.copy(alpha = .72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { (xp.toFloat() / xpMax).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Gold, trackColor = Ink.copy(alpha = .10f))
        }
    }
}

@Composable
private fun QuestContentV16(title: String, progress: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFEDE2FA)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_quest), null, tint = Color(0xFF7653B5), modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Daily Quest", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = Ink.copy(alpha = .72f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(progress, color = Color(0xFF7653B5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087F83)),
            shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
            modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Continue", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NavItemV16(label: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(painterResource(icon), null, tint = Color(0xFF087F83), modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = Ink)
    }
}

@Composable
private fun RewardChipV16(@DrawableRes icon: Int, value: String, tint: Color) {
    Surface(color = Color(0xFFFFF8E8).copy(alpha = .96f), shape = RoundedCornerShape(99.dp), shadowElevation = 3.dp) {
        Row(Modifier.height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Ink)
        }
    }
}

@Composable
private fun SubjectPlaqueV16(item: VillageDestinationV16, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.semantics(mergeDescendants = true) {
        contentDescription = "${item.destination}, ${item.subject}, ${item.progressText}${if (item.today) ", recommended today" else ""}"
        if (!item.enabled) disabled()
    }.clickable(enabled = item.enabled, onClick = onClick)) {
        Image(painterResource(R.drawable.mw_subject_plaque), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        Row(Modifier.fillMaxSize().padding(start = 30.dp, end = 22.dp, top = 22.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(42.dp)) {
                Canvas(Modifier.fillMaxSize()) { drawRoundRect(item.accent, cornerRadius = CornerRadius(5f, 5f)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(item.destination, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, lineHeight = 17.sp)
                Text("${item.subject} · ${item.progressText}", color = item.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (item.today) Text("TODAY", color = Color(0xFF7A4B00), fontSize = 8.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ArtPanelV16(@DrawableRes res: Int, modifier: Modifier, content: @Composable BoxScope.() -> Unit) = Box(modifier) {
    Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    Box(Modifier.fillMaxSize().padding(22.dp), content = content)
}

private fun Modifier.sceneAnchor(cx: Float, cy: Float, w: Dp, h: Dp) = this.then(Modifier.layout { measurable, constraints ->
    val p = measurable.measure(Constraints.fixed(w.roundToPx(), h.roundToPx()))
    val x = (constraints.maxWidth * cx - p.width / 2).roundToInt().coerceIn(8, constraints.maxWidth - p.width - 8)
    val y = (constraints.maxHeight * cy - p.height / 2).roundToInt().coerceIn(8, constraints.maxHeight - p.height - 96)
    layout(constraints.maxWidth, constraints.maxHeight) { p.placeRelative(x, y) }
})

@Composable
private fun CompactVillageChromeV16(
    destinations: List<VillageDestinationV16>, onClick: (String) -> Unit,
    childName: String, level: Int, xp: Int, xpMax: Int,
    streak: Int, stars: Int, coins: Int,
    questTitle: String, questProgress: String,
    onQuestClick: () -> Unit, onAchievementsClick: () -> Unit, onParentsClick: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ArtPanelV16(R.drawable.mw_profile_panel, Modifier.fillMaxWidth().aspectRatio(2.69f)) { ProfileContentV16(childName, level, xp, xpMax) } }
        item { ArtPanelV16(R.drawable.mw_quest_panel, Modifier.fillMaxWidth().aspectRatio(3.27f)) { QuestContentV16(questTitle, questProgress, onQuestClick) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { RewardChipV16(R.drawable.ic_flame, "$streak", Color(0xFFF47C6B)); RewardChipV16(R.drawable.ic_star, if (stars >= 1000) "${stars / 1000}.${(stars % 1000) / 100}k" else "$stars", Gold); RewardChipV16(R.drawable.ic_coin, "$coins", Color(0xFFB87916)) } }
        items(destinations.size) { i -> SubjectPlaqueV16(destinations[i], { onClick(destinations[i].id) }, Modifier.fillMaxWidth().heightIn(min = 84.dp)) }
        item { ArtPanelV16(R.drawable.mw_bottom_nav, Modifier.fillMaxWidth().height(84.dp)) { Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { NavItemV16("Profile", R.drawable.ic_profile, {}); NavItemV16("Achievements", R.drawable.ic_achievements, onAchievementsClick); NavItemV16("Backpack", R.drawable.ic_backpack, {}); NavItemV16("Parents", R.drawable.ic_parent, onParentsClick) } } }
    }
}
