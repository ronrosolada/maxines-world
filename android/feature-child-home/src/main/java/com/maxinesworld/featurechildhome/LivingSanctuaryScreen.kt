package com.maxinesworld.featurechildhome

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.featurerewards.*
import java.util.Locale

@Composable
fun LivingSanctuaryRoute(onBack: () -> Unit, viewModel: LivingSanctuaryViewModel) {
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    LivingSanctuaryScreen(scene, onBack)
}

@Composable
fun LivingSanctuaryScreen(scene: SanctuaryScene, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalAnimationsDisabled.current
    val audio = remember { SanctuaryAudioEngine(context) }
    var filipino by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<WildlifeHabitatAffinity?>(null) }
    var miloMessage by remember { mutableStateOf<String?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) {}
        onDispose { tts?.shutdown(); audio.close() }
    }
    LaunchedEffect(Unit) { audio.ambientCue() }

    Box(Modifier.fillMaxSize().background(Color(0xFFCCE8C7))) {
        Image(painterResource(R.drawable.sanctuary_backdrop), null, Modifier.fillMaxSize().graphicsLayer { translationX = -8f }, contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .06f)))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            scene.residents.sortedBy { it.anchor.zIndex }.forEach { resident ->
                ResidentAnimal(resident, reduceMotion, Modifier
                    .offset(maxWidth * resident.anchor.x - 52.dp, maxHeight * resident.anchor.y - 52.dp)
                    .size((resident.anchor.scale * 420).dp)
                    .clickable {
                        selected = resident.species
                        audio.discovery()
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    })
            }
            MiloResident(reduceMotion, Modifier.align(Alignment.BottomStart).padding(start = 38.dp, bottom = 30.dp).size(150.dp),
                onPet = { audio.purr(); haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); miloMessage = if (filipino) "Prrrr! Salamat sa lambing!" else "Prrrr! Thank you for the pets!" },
                onTap = { miloMessage = if (scene.residents.isEmpty()) "Keep learning and wildlife friends will visit!" else "We have ${scene.residents.size} wildlife friends visiting today!" })
        }
        Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            FilledIconButton(onClick = onBack, modifier = Modifier.size(56.dp).semantics { contentDescription = "Exit sanctuary" }) { Icon(Icons.Default.ArrowBack, null) }
            FilledTonalButton(onClick = { filipino = !filipino }, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.Default.Language, null); Spacer(Modifier.width(8.dp)); Text(if (filipino) "Filipino" else "English", fontWeight = FontWeight.Bold)
            }
        }
        miloMessage?.let { Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp).widthIn(max = 420.dp), RoundedCornerShape(20.dp), color = Color.White.copy(.94f)) { Text(it, Modifier.padding(16.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold) } }
    }
    selected?.let { animal ->
        AlertDialog(onDismissRequest = { selected = null }, confirmButton = { Button(onClick = {
            val text = if (filipino) animal.factFilipino else animal.factEnglish
            val locale = if (filipino) Locale.Builder().setLanguage("fil").setRegion("PH").build() else Locale.US
            val languageResult = tts?.setLanguage(locale)
            if (languageResult != TextToSpeech.LANG_MISSING_DATA && languageResult != TextToSpeech.LANG_NOT_SUPPORTED) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, animal.badgeId)
        }) { Text(if (filipino) "Pakinggan" else "Listen") } }, dismissButton = { TextButton(onClick = { selected = null }) { Text(if (filipino) "Isara" else "Close") } },
            title = { Text(if (filipino) animal.nameFilipino else animal.nameEnglish) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (filipino) animal.habitatFilipino else animal.habitatEnglish, fontWeight = FontWeight.Bold); Text(if (filipino) animal.factFilipino else animal.factEnglish); Text(animal.signatureBehavior, style = MaterialTheme.typography.labelLarge) } })
    }
}

@Composable private fun ResidentAnimal(r: SanctuaryResident, reduced: Boolean, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "animalIdle")
    val motion by transition.animateFloat(0f, if (reduced) 0f else 4f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "idle")
    val drawable = when (r.species.badgeId) { "badge_philippine_eagle" -> R.drawable.animal_philippine_eagle; "badge_philippine_tarsier" -> R.drawable.animal_philippine_tarsier; "badge_tamaraw" -> R.drawable.animal_tamaraw; else -> R.drawable.animal_fish_sinarapan }
    Image(painterResource(drawable), r.species.nameEnglish, modifier.graphicsLayer { translationY = motion }, contentScale = ContentScale.Fit)
}

@Composable private fun MiloResident(reduced: Boolean, modifier: Modifier, onPet: () -> Unit, onTap: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "miloIdle")
    val breath by transition.animateFloat(1f, if (reduced) 1f else 1.035f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "breathing")
    var drag by remember { mutableFloatStateOf(0f) }
    Image(painterResource(R.drawable.character_milo), "Milo. Tap to talk or gently swipe over his head to pet him.", modifier.graphicsLayer { scaleX = breath; scaleY = breath }.clickable(onClick = onTap).pointerInput(Unit) { detectDragGestures(onDragEnd = { if (drag > 28f) onPet(); drag = 0f }) { _, amount -> drag += kotlin.math.abs(amount.x) + kotlin.math.abs(amount.y) } }, contentScale = ContentScale.Fit)
}
