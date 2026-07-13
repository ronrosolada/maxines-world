package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF183B4A)
private val PaperTop = Color(0xFFFFF8E8)
private val PaperBottom = Color(0xFFF4E2BE)
private val Outline = Color(0xA6375662)
private val Teal = Color(0xFF087F83)
private val Gold = Color(0xFFF0AF28)

@Immutable
data class DestinationAnchor(val x: Float, val y: Float)

enum class DestinationStatus { AVAILABLE, RECOMMENDED, COMPLETED, LOCKED }

@Immutable
data class VillageDestinationUi(
    val id: String,
    val destinationName: String,
    val subjectName: String,
    val progressPercent: Int,
    val accent: Color,
    @DrawableRes val iconRes: Int,
    val anchor: DestinationAnchor,
    val status: DestinationStatus = DestinationStatus.AVAILABLE,
    val lockReason: String? = null,
)

@Immutable
data class VillageHomeUiState(
    val childName: String = "Maxine",
    val level: Int = 12,
    val currentXp: Int = 660,
    val targetXp: Int = 900,
    val streak: Int = 7,
    val stars: Int = 1200,
    val coins: Int = 567,
    val questTitle: String = "Read a story in Story Tree",
    val questProgress: String = "0 / 1",
    val destinations: List<VillageDestinationUi>,
)

@Composable
fun VillageHomeScreen(
    state: VillageHomeUiState,
    onDestinationClick: (String) -> Unit,
    onQuestClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onBackpackClick: () -> Unit,
    onParentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            StorybookBottomNavigation(
                onProfileClick = onProfileClick,
                onAchievementsClick = onAchievementsClick,
                onBackpackClick = onBackpackClick,
                onParentsClick = onParentsClick,
            )
        },
    ) { scaffoldPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .testTag("village_scene"),
        ) {
            Image(
                painter = painterResource(R.drawable.village_home_six_landmarks_master),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            DestinationOverlay(
                destinations = state.destinations,
                onDestinationClick = onDestinationClick,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlayerProfilePanel(state = state, onClick = onProfileClick)
                DailyQuestPanel(
                    title = state.questTitle,
                    progress = state.questProgress,
                    onClick = onQuestClick,
                )
            }

            RewardRail(
                streak = state.streak,
                stars = state.stars,
                coins = state.coins,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp),
            )
        }
    }
}

@Composable
private fun DestinationOverlay(
    destinations: List<VillageDestinationUi>,
    onDestinationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val plaqueWidth = 184.dp
        val plaqueHeight = 72.dp
        destinations.forEach { destination ->
            val maxX = (maxWidth - plaqueWidth - 8.dp).coerceAtLeast(8.dp)
            val maxY = (maxHeight - plaqueHeight - 8.dp).coerceAtLeast(8.dp)
            val x = (maxWidth * destination.anchor.x - plaqueWidth / 2)
                .coerceIn(8.dp, maxX)
            val y = (maxHeight * destination.anchor.y - plaqueHeight / 2)
                .coerceIn(8.dp, maxY)

            DestinationPlaque(
                destination = destination,
                onClick = { onDestinationClick(destination.id) },
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(plaqueWidth, plaqueHeight),
            )
        }
    }
}

@Composable
fun DestinationPlaque(
    destination: VillageDestinationUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = destination.status != DestinationStatus.LOCKED
    val stateText = when (destination.status) {
        DestinationStatus.RECOMMENDED -> "recommended today"
        DestinationStatus.COMPLETED -> "completed"
        DestinationStatus.LOCKED -> destination.lockReason ?: "locked"
        DestinationStatus.AVAILABLE -> "available"
    }
    val spoken = "${destination.destinationName}, ${destination.subjectName}, " +
        "${destination.progressPercent} percent complete, $stateText"

    Box(
        modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Ink.copy(alpha = .18f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(PaperTop, PaperBottom)))
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                if (!enabled) disabled()
            }
            .testTag("destination_${destination.id}"),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 7.dp)
                .width(6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(destination.accent),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
                tint = destination.accent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    destination.destinationName,
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (destination.status == DestinationStatus.LOCKED)
                        destination.lockReason ?: "Opening soon"
                    else "${destination.subjectName} · ${destination.progressPercent}%",
                    color = destination.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
        if (destination.status == DestinationStatus.RECOMMENDED) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                color = Gold,
                contentColor = Ink,
                shape = RoundedCornerShape(99.dp),
            ) {
                Text("TODAY", fontSize = 8.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun PlayerProfilePanel(state: VillageHomeUiState, onClick: () -> Unit) {
    StorybookPanel(
        modifier = Modifier
            .width(270.dp)
            .height(90.dp)
            .clickable(onClick = onClick)
            .testTag("profile_panel"),
    ) {
        Row(
            Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFF47C6B)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_profile), null, tint = Color.White,
                    modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("Hi, ${state.childName}!", color = Ink, fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("Lv ${state.level}   ${state.currentXp} / ${state.targetXp} XP",
                    color = Ink.copy(alpha = .72f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(
                    progress = { (state.currentXp.toFloat() / state.targetXp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Gold,
                    trackColor = Ink.copy(alpha = .10f),
                )
            }
        }
    }
}

@Composable
private fun DailyQuestPanel(title: String, progress: String, onClick: () -> Unit) {
    StorybookPanel(
        modifier = Modifier
            .width(380.dp)
            .height(84.dp)
            .testTag("daily_quest"),
    ) {
        Row(
            Modifier.fillMaxSize().padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFFEDE2FA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_quest), null,
                    tint = Color(0xFF7653B5), modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Daily Quest", color = Ink, fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text(title, color = Ink.copy(alpha = .72f), fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(progress, color = Color(0xFF7653B5), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Continue", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StorybookPanel(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .shadow(5.dp, RoundedCornerShape(18.dp), ambientColor = Ink.copy(alpha = .15f))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(PaperTop, PaperBottom)))
            .border(1.dp, Outline, RoundedCornerShape(18.dp)),
        content = content,
    )
}

@Composable
private fun RewardRail(streak: Int, stars: Int, coins: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RewardChip(R.drawable.ic_flame, streak.toString(), Color(0xFFF47C6B))
        RewardChip(R.drawable.ic_star, if (stars >= 1000) "${stars / 1000}.${(stars % 1000) / 100}k" else stars.toString(), Gold)
        RewardChip(R.drawable.ic_coin, coins.toString(), Color(0xFFB87916))
    }
}

@Composable
private fun RewardChip(@DrawableRes icon: Int, value: String, tint: Color) {
    Surface(
        color = PaperTop.copy(alpha = .96f),
        contentColor = Ink,
        shape = RoundedCornerShape(99.dp),
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
    ) {
        Row(
            Modifier.height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StorybookBottomNavigation(
    onProfileClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    onBackpackClick: () -> Unit,
    onParentsClick: () -> Unit,
) {
    NavigationBar(
        containerColor = PaperTop,
        contentColor = Ink,
        tonalElevation = 5.dp,
        modifier = Modifier.height(72.dp),
    ) {
        val items = listOf(
            Triple("Profile", R.drawable.ic_profile, onProfileClick),
            Triple("Achievements", R.drawable.ic_achievements, onAchievementsClick),
            Triple("Backpack", R.drawable.ic_backpack, onBackpackClick),
            Triple("Parents", R.drawable.ic_parent, onParentsClick),
        )
        items.forEach { (label, icon, click) ->
            NavigationBarItem(
                selected = false,
                onClick = click,
                icon = { Icon(painterResource(icon), null) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = Teal,
                    unselectedTextColor = Ink,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

fun defaultVillageDestinations(): List<VillageDestinationUi> = listOf(
    VillageDestinationUi("english", "Story Tree", "English", 42, Color(0xFF7653B5), R.drawable.ic_subject_english, DestinationAnchor(.15f, .47f)),
    VillageDestinationUi("filipino", "Bahay ng Kuwento", "Filipino", 25, Color(0xFFF47C6B), R.drawable.ic_subject_filipino, DestinationAnchor(.50f, .42f)),
    VillageDestinationUi("mathematics", "Number Market", "Mathematics", 67, Color(0xFF3C9DDB), R.drawable.ic_subject_math, DestinationAnchor(.84f, .40f), DestinationStatus.RECOMMENDED),
    VillageDestinationUi("science", "Discovery Lab", "Science", 33, Color(0xFF66A83E), R.drawable.ic_subject_science, DestinationAnchor(.15f, .73f)),
    VillageDestinationUi("history", "Heritage Harbor", "Philippine History", 16, Color(0xFFB87916), R.drawable.ic_subject_history, DestinationAnchor(.50f, .72f)),
    VillageDestinationUi("gmrc", "Kindness Corner", "GMRC", 0, Color(0xFF087F83), R.drawable.ic_subject_gmrc, DestinationAnchor(.84f, .72f), DestinationStatus.LOCKED, "Opening soon"),
)
