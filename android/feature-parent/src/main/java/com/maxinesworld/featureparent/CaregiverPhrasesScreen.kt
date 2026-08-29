package com.maxinesworld.featureparent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import com.maxinesworld.coremodel.CaregiverPhraseCategory

@Composable
internal fun CaregiverPhrasesDeck(
    repository: CaregiverPhraseRepository = remember { CaregiverPhraseRepository() },
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CaregiverPhraseCategory?>(null) }
    val practicedToday = remember { mutableStateMapOf<String, Boolean>() }
    val cards = repository.filterCards(query, category)

    Card(colors = CardDefaults.cardColors(containerColor = VillageTeal.copy(alpha = 0.06f)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Home Practice Phrases", fontWeight = FontWeight.Bold, color = Ink)
            Text("Try one Filipino phrase in a real family moment today.", color = Ink.copy(alpha = 0.7f))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search phrases") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") }) }
                items(CaregiverPhraseCategory.entries) { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option.displayName) },
                    )
                }
            }
            if (cards.isEmpty()) Text("No phrases match your search.", color = Ink.copy(alpha = 0.7f))
            cards.forEach { phrase ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(phrase.category.displayName, color = VillageTeal, fontWeight = FontWeight.SemiBold)
                        Text(phrase.filipinoPhrase, color = Ink, fontWeight = FontWeight.Bold)
                        Text(phrase.englishCue, color = Ink.copy(alpha = 0.75f))
                        Text("Try it: ${phrase.practicalTip}", color = Ink.copy(alpha = 0.65f))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Practiced today", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Switch(
                                checked = practicedToday[phrase.id] == true,
                                onCheckedChange = { practicedToday[phrase.id] = it },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
                                    contentDescription = "Mark ${phrase.filipinoPhrase} as practiced today"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
