package com.maxinesworld.featurechildhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.corecontent.ContentModuleLesson
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.SkyBlue
import com.maxinesworld.coredesignsystem.theme.SuccessGreen
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.VillageTeal

/**
 * Module → Lesson list. Shows the lessons inside one module; tapping a
 * lesson opens the lesson player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleLessonsScreen(
    moduleTitle: String,
    state: ModuleLessonsState,
    onLessonClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(moduleTitle, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyBlue)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = VillageTeal)
                state.error != null -> Text(
                    state.error, Modifier.align(Alignment.Center).padding(24.dp),
                    color = Ink, fontSize = 16.sp
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Lessons in this module",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink
                        )
                        Text(
                            if (state.completedCount == 0) {
                                "${state.lessons.size} lessons · tap one to play"
                            } else {
                                "${state.completedCount} of ${state.lessons.size} complete" +
                                    if (state.nextLessonId != null) " · keep going!" else " · module done! 🎉"
                            },
                            fontSize = 14.sp, color = Ink.copy(alpha = 0.6f)
                        )
                    }
                    items(state.lessons, key = { it.lessonId }) { lesson ->
                        LessonRow(
                            lesson = lesson,
                            isCompleted = lesson.lessonId in state.completedLessonIds,
                            isNext = lesson.lessonId == state.nextLessonId,
                            onClick = { onLessonClick(lesson.lessonId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: ContentModuleLesson,
    isCompleted: Boolean,
    isNext: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        isCompleted -> SuccessGreen.copy(alpha = 0.08f)
        isNext -> SunshineGold.copy(alpha = 0.12f)
        else -> Color.White
    }
    val borderColor = if (isNext) SunshineGold else Color.Transparent
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isNext) 2.dp else 0.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isCompleted -> SuccessGreen.copy(alpha = 0.15f)
                            isNext -> SunshineGold.copy(alpha = 0.25f)
                            else -> VillageTeal.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isCompleted) "✓" else lesson.day.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = when {
                        isCompleted -> SuccessGreen
                        isNext -> SunshineGold
                        else -> VillageTeal
                    }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(lesson.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        isCompleted -> "Completed"
                        isNext -> "Up next — tap to continue"
                        else -> "~${lesson.estimatedMinutes} min"
                    },
                    fontSize = 12.sp,
                    color = Ink.copy(alpha = 0.5f)
                )
            }
            Icon(
                if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isCompleted) SuccessGreen else SkyBlue
            )
        }
    }
}
