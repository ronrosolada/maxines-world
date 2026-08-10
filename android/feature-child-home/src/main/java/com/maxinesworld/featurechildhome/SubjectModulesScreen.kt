package com.maxinesworld.featurechildhome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.corecontent.ContentModule
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.KindnessTealText
import com.maxinesworld.coredesignsystem.theme.VillageTeal

/**
 * Subject → Module list. Shows every module of a subject (e.g. math:
 * Module 1 + Quarter 1 · Week 1 … Quarter 4 · Week 9) before any lesson
 * is opened, so the child (and parent) can see and pick the curriculum
 * unit instead of being dropped straight into a single lesson.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectModulesScreen(
    subject: String,
    state: SubjectModulesState,
    onModuleClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val displayName = subjectDisplayName(subject)
    val headerColor = if (subject == "gmrc") KindnessTealText else VillageTeal
    val heroRes = subjectHeroResource(subject)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = headerColor)
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
                else -> {
                    val recommendedKey = state.modules.firstOrNull { it.lessons.isNotEmpty() }?.key
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            heroRes?.let { resource ->
                                Image(
                                    painter = painterResource(resource),
                                    contentDescription = "Kindness Corner learning place",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(156.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                "Start here, or choose another module.",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink
                            )
                            Text(
                                "${state.modules.size} modules · pick one to start learning",
                                fontSize = 14.sp, color = Ink
                            )
                        }
                        items(state.modules, key = { it.key }) { module ->
                            ModuleCard(
                                module = module,
                                recommended = module.key == recommendedKey,
                            ) { onModuleClick(module.key) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: ContentModule,
    recommended: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook, contentDescription = null,
                tint = VillageTeal, modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (recommended) {
                    Text(
                        "Start here!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = VillageTeal,
                    )
                }
                Text(module.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
                Text(
                    moduleCardSubtitle(module),
                    fontSize = 13.sp, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Text("▸", fontSize = 20.sp, color = VillageTeal)
        }
    }
}

/** Keep the module card descriptor stable when lesson skill names recur by design. */
internal fun moduleCardSubtitle(module: ContentModule): String =
    "${module.lessonCount} lessons"

/** Island/subject ID → friendly display name. */
fun subjectDisplayName(subject: String): String = when (subject) {
    "english" -> "Story Time"
    "mathematics" -> "Number Fun"
    "filipino" -> "Kwentuhan"
    "science" -> "Discovery"
    "heritage-harbor" -> "Heritage"
    "gmrc" -> "Kindness"
    "araling-panlipunan" -> "Araling Panlipunan"
    "makabansa" -> "Makabansa"
    else -> subject.replaceFirstChar { it.uppercase() }
}

private fun subjectHeroResource(subject: String): Int? = when (subject) {
    "gmrc" -> R.drawable.mw_location_kindness_corner
    else -> null
}

/** Normalize an island ID to the pack subject used in lesson IDs. */
fun subjectForPack(subject: String): String? = when (subject) {
    "heritage-harbor", "philippine-history", "araling_panlipunan" -> "araling-panlipunan"
    "makabansa", "mathematics", "english", "science", "filipino", "gmrc" -> subject
    else -> null
}
