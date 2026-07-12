package com.maxinesworld.featurechildhome

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.maxinesworld.coredesignsystem.theme.*

// ─── Subject slot display ───

data class DailySubjectSlot(
    val subjectId: String, val subjectName: String,
    val icon: ImageVector, val color: Color, val status: SubjectSlotStatus
)
enum class SubjectSlotStatus { NOT_STARTED, IN_PROGRESS, PASSED }

private val month1Subjects = listOf(
    Triple("english", "English", Icons.Default.MenuBook to StoryPurple),
    Triple("filipino", "Filipino", Icons.Default.AutoStories to Coral),
    Triple("mathematics", "Math", Icons.Default.Calculate to SkyBlue),
    Triple("science", "Science", Icons.Default.Science to LeafGreen),
    Triple("araling-panlipunan", "Araling Panlipunan", Icons.Default.Public to SunshineGold)
)

@Composable
fun DailyTrailPanel(
    currentDay: Int = 1,
    subjectStatuses: Map<String, SubjectSlotStatus> = emptyMap(),
    completedCount: Int = 0,
    onSubjectTap: (day: Int, subjectId: String) -> Unit,
    onDaySelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        // Day selector chips
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..20).forEach { day ->
                val isCurrent = day == currentDay
                FilterChip(
                    selected = isCurrent,
                    onClick = { onDaySelect(day) },
                    label = { Text("Day $day", fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VillageTeal,
                        selectedLabelColor = White
                    ),
                    modifier = Modifier.height(36.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Progress header
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VillageTeal.copy(alpha = 0.08f))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = SunshineGold, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Day $currentDay", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = VillageTeal)
                    Text("$completedCount of 5 subjects complete", fontSize = 14.sp, color = Ink.copy(alpha = 0.6f))
                }
                if (completedCount == 5) {
                    Text("🎉", fontSize = 24.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 5 subject slots
        month1Subjects.forEach { (id, name, pair) ->
            val (icon, color) = pair
            val status = subjectStatuses[id] ?: SubjectSlotStatus.NOT_STARTED
            val lessonId = "${id}-g3-m01-d${currentDay.toString().padStart(2, '0')}"

            SubjectSlotCard(
                subjectName = name, icon = icon, color = color,
                status = status, lessonId = lessonId,
                onClick = { onSubjectTap(currentDay, lessonId) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SubjectSlotCard(
    subjectName: String, icon: ImageVector, color: Color,
    status: SubjectSlotStatus, lessonId: String,
    onClick: () -> Unit, modifier: Modifier
) {
    Card(modifier.clickable { onClick() }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(
                when (status) {
                    SubjectSlotStatus.PASSED -> SuccessGreen.copy(alpha = 0.2f)
                    SubjectSlotStatus.IN_PROGRESS -> SunshineGold.copy(alpha = 0.2f)
                    SubjectSlotStatus.NOT_STARTED -> color.copy(alpha = 0.15f)
                }), contentAlignment = Alignment.Center) {
                when (status) {
                    SubjectSlotStatus.PASSED -> Icon(Icons.Default.Check, "Passed", tint = SuccessGreen, modifier = Modifier.size(22.dp))
                    SubjectSlotStatus.IN_PROGRESS -> Icon(icon, subjectName, tint = color, modifier = Modifier.size(22.dp))
                    SubjectSlotStatus.NOT_STARTED -> Icon(icon, subjectName, tint = color.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(subjectName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                Text(when (status) {
                    SubjectSlotStatus.PASSED -> "Completed ✓"
                    SubjectSlotStatus.IN_PROGRESS -> "In progress..."
                    SubjectSlotStatus.NOT_STARTED -> "Tap to start"
                }, fontSize = 13.sp, color = Ink.copy(alpha = 0.5f))
            }
            Icon(Icons.Default.ChevronRight, "Open", tint = color.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}
