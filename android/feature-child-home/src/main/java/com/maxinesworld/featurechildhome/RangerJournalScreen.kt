package com.maxinesworld.featurechildhome

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable fun RangerJournalScreen(childId:String,onBack:()->Unit){
 val context=LocalContext.current; val repo=remember(childId){RangerJournalRepository(FileRangerJournalStore(context))}; var filipino by remember{mutableStateOf(false)}; val journal=remember(childId){repo.journal(childId)}
 Scaffold(topBar={Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){FilledIconButton(onBack){Icon(Icons.Default.ArrowBack,"Back")}; Text(if(filipino)"Talaan ng Tanod-Gubat" else "Ranger Journal",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Row{IconButton({filipino=!filipino}){Icon(Icons.Default.Language,"Switch English and Filipino")};IconButton({val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,repo.exportText(childId,filipino))};context.startActivity(Intent.createChooser(send,"Print or export journal"))}){Icon(Icons.Default.Print,"Print or export journal")}}}}){pad->
  LazyColumn(Modifier.fillMaxSize().background(Color(0xFFFFF3D6)).padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
   item{Text(if(filipino)"Mga Larawang Polaroid" else "Polaroid Photo Gallery",style=MaterialTheme.typography.titleLarge)}
   items(journal.snapshots){s->Surface(shape=RoundedCornerShape(4.dp),shadowElevation=5.dp,color=Color.White,modifier=Modifier.semantics{contentDescription="${s.caption}, ${s.timeOfDay}, ${s.species.joinToString()}"}){Column(Modifier.padding(14.dp)){Box(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFB8D8B2)));Text(s.caption,fontWeight=FontWeight.Bold);Text("${DateFormat.getDateTimeInstance().format(Date(s.timestamp))} • ${s.timeOfDay}");Text(s.species.joinToString(" • "));Text(s.stickers.joinToString(" "))}}}
   item{Text(if(filipino)"Pasaporte ng Bakas-Paa" else "Animal Pawprint Passport",style=MaterialTheme.typography.titleLarge);Text(journal.stamps.joinToString("  "){"🐾 ${it.speciesName}"}.ifBlank{if(filipino)"Bisitahin ang mga kaibigang hayop!" else "Visit animal friends to earn stamps!"})}
   item{Text(if(filipino)"Mga Badge at Tala ni Milo" else "Milo's Field Ranger Badges & Notes",style=MaterialTheme.typography.titleLarge)}
   items(journal.badges){b->Card{Column(Modifier.padding(14.dp)){Text("★ "+if(filipino)b.titleFilipino else b.titleEnglish,fontWeight=FontWeight.Bold);Text(b.note)}}}
  }
 }
}
