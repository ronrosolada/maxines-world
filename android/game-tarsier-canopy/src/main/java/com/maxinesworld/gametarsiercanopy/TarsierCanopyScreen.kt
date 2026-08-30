package com.maxinesworld.gametarsiercanopy

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxinesworld.engineminigame.RewardBreakClock
import kotlinx.coroutines.delay

private val Teal = Color(0xFF087F83)
private val Coral = Color(0xFFF47C6B)
private val Gold = Color(0xFFF5B82E)
private val Cream = Color(0xFFFFF7E8)
private val Ink = Color(0xFF183B4A)

private data class CanopyPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val trunk: Color,
    val leaf: Color,
    val vine: Color,
    val branch: Color,
    val ground: Color,
    val firefly: Color,
)

private fun paletteFor(courseId: String): CanopyPalette = when (courseId) {
    "canopy-moonlight" -> CanopyPalette(
        Color(0xFF1A2A5A), Color(0xFF3A4E8C), Color(0xFF25364F), Color(0xFF2E6B4F),
        Color(0xFF1F4A3A), Color(0xFF4A3A28), Color(0xFF22423A), Color(0xFFFFF59D),
    )
    "canopy-sunset" -> CanopyPalette(
        Color(0xFFFFB47A), Color(0xFFFFE0B2), Color(0xFF6E4F3A), Color(0xFFC98A4B),
        Color(0xFF7A5B3A), Color(0xFF8A5A3A), Color(0xFF6E4F3A), Color(0xFFFFD54F),
    )
    else -> CanopyPalette(
        Color(0xFFBFE8F5), Color(0xFFF7E8C0), Color(0xFF6B8E6B), Color(0xFF7FB069),
        Color(0xFF4E7A4A), Color(0xFF7A5B3A), Color(0xFF5B8C5A), Color(0xFFFFD54F),
    )
}

@Composable
fun TarsierCanopyScreen(
    childId: String,
    rewardBreakId: String,
    modifier: Modifier = Modifier,
    durationMillis: Long = RewardBreakClock.DEFAULT_DURATION_MILLIS,
    onExit: (TarsierResult) -> Unit,
    viewModel: TarsierCanopyViewModel = viewModel(
        factory = TarsierCanopyViewModelFactory(childId, rewardBreakId, durationMillis)
    ),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var exit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sounds = remember { TarsierCanopySoundPlayer(context.applicationContext) }
    val haptic = LocalHapticFeedback.current
    DisposableEffect(sounds) { onDispose { sounds.close() } }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler { exit = true }

    var lastFireflies by remember { mutableIntStateOf(0) }
    var lastBumps by remember { mutableIntStateOf(0) }
    var lastPhase by remember { mutableStateOf(ui.game.phase) }
    LaunchedEffect(ui.game.fireflies, ui.game.bumps, ui.game.phase) {
        if (ui.soundEnabled) {
            if (ui.game.fireflies > lastFireflies) sounds.firefly()
            if (ui.game.bumps > lastBumps) sounds.bump()
            if (lastPhase != CanopyPhase.ROUND_COMPLETE && ui.game.phase == CanopyPhase.ROUND_COMPLETE) sounds.finish()
        }
        if (ui.game.fireflies > lastFireflies) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (ui.game.bumps > lastBumps) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (lastPhase != CanopyPhase.ROUND_COMPLETE && ui.game.phase == CanopyPhase.ROUND_COMPLETE) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastFireflies = ui.game.fireflies
        lastBumps = ui.game.bumps
        lastPhase = ui.game.phase
    }
    LaunchedEffect(ui.breakExpired, ui.game.phase) {
        if (ui.breakExpired && ui.game.phase != CanopyPhase.RUNNING) {
            delay(1200)
            onExit(viewModel.result())
        }
    }

    Box(modifier.fillMaxSize().background(paletteFor(ui.game.course.id).skyTop)) {
        val wide = LocalConfiguration.current.screenWidthDp >= 840
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Stage(ui, viewModel, { if (ui.soundEnabled) sounds.hop() }, Modifier.weight(3f).fillMaxHeight())
                Side(ui, viewModel, { exit = true }, Modifier.widthIn(300.dp, 390.dp).fillMaxHeight())
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Header(ui, { exit = true })
                Stage(ui, viewModel, { if (ui.soundEnabled) sounds.hop() }, Modifier.weight(1f).fillMaxWidth())
            }
        }
        if (ui.paused) PauseOverlay(viewModel::resume)
        if (exit) ExitDialog({ exit = false }) { onExit(viewModel.result()) }
    }
}

@Composable
private fun Stage(
    ui: TarsierUiState,
    vm: TarsierCanopyViewModel,
    onHopSound: () -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Cream.copy(alpha = 0.94f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(courseTitle(ui.game.course), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            LinearProgressIndicator(
                progress = { ui.game.progress },
                Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = Gold,
                trackColor = Color.White,
            )
            val feedbackText = feedbackLabel(ui.game.feedback)
            Text(
                feedbackText,
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .semantics {
                        contentDescription = feedbackText
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            CanopyView(ui.game, Modifier.weight(1f).fillMaxWidth())
            when (ui.game.phase) {
                CanopyPhase.READY -> Button(
                    { vm.start() },
                    Modifier.heightIn(min = 64.dp).fillMaxWidth(0.65f),
                    colors = ButtonDefaults.buttonColors(Teal),
                ) {
                    Icon(Icons.Default.PlayArrow, stringResource(R.string.tarsier_start))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tarsier_start), fontSize = 21.sp)
                }
                CanopyPhase.RUNNING -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        { vm.shortHop(); onHopSound() },
                        Modifier.heightIn(min = 64.dp).weight(1f),
                        colors = ButtonDefaults.buttonColors(Coral),
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.tarsier_hop))
                        Text(" ${stringResource(R.string.tarsier_hop)}", fontSize = 21.sp)
                    }
                    OutlinedButton(
                        { vm.longHop(); onHopSound() },
                        Modifier.heightIn(min = 64.dp).weight(1f),
                    ) {
                        Icon(Icons.Default.North, stringResource(R.string.tarsier_big_hop))
                        Text(" ${stringResource(R.string.tarsier_big_hop)}", fontSize = 20.sp)
                    }
                }
                CanopyPhase.ROUND_COMPLETE -> Button(
                    { vm.nextCourse() },
                    enabled = !ui.breakExpired,
                    modifier = Modifier.heightIn(min = 64.dp).fillMaxWidth(0.65f),
                    colors = ButtonDefaults.buttonColors(Teal),
                ) {
                    Text(
                        if (ui.breakExpired) stringResource(R.string.tarsier_break_complete)
                        else stringResource(R.string.tarsier_next_round),
                        fontSize = 21.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CanopyView(s: TarsierState, modifier: Modifier) {
    val palette = paletteFor(s.course.id)
    Box(modifier = modifier.clip(RoundedCornerShape(22.dp)).background(palette.skyTop)) {
        val viewStart = (s.x - 5f).coerceAtLeast(0f)
        val span = 16f

        val pulse = if (s.reducedMotion) 0.85f else {
            val transition = rememberInfiniteTransition(label = "fireflyPulse")
            transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "fireflyPulseValue",
            ).value
        }

        Canvas(Modifier.fillMaxSize().semantics {
            contentDescription = "Tarsier Canopy. ${s.fireflies} fireflies collected. Round ${s.roundsCompleted + 1}."
        }) {
            val widthPx = size.width
            val heightPx = size.height
            val groundY = heightPx * 0.92f
            val px: (Float) -> Float = { ux -> widthPx * ((ux - viewStart) / span) }
            val py: (Float) -> Float = { elevation -> groundY - (elevation * heightPx * 0.42f).coerceAtLeast(-heightPx * 0.08f) }

            // Sky
            drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom)))
            // Distant trunks (slow parallax, two rows)
            val trunkParallax = viewStart * 0.5f
            drawTrunkRow(this, px, py, palette, size.height, trunkParallax)
            drawTrunkRow(this, px, py, palette, size.height, trunkParallax + 1.4f)

            // Vines
            s.course.vines
                .filter { !s.passedVineIds.contains(it.id) && it.x in viewStart..(viewStart + span) }
                .forEach { v ->
                    val x = px(v.x)
                    val path = Path().apply {
                        moveTo(x, -size.height * 0.05f)
                        quadraticBezierTo(x + size.width * 0.03f, size.height * 0.22f, x, size.height * 0.30f)
                    }
                    drawPath(path, palette.vine, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height * 0.02f))
                    // knot at the swingable tangle height
                    drawCircle(palette.vine, radius = size.height * 0.03f, center = Offset(x, size.height * 0.30f))
                    drawCircle(palette.leaf, radius = size.height * 0.018f, center = Offset(x + size.width * 0.02f, size.height * 0.31f))
                }

            // Fireflies
            s.course.fireflies
                .filter { !s.collectedFireflyIds.contains(it.id) && it.x in viewStart..(viewStart + span) }
                .forEach { f ->
                    val fx = px(f.x)
                    val fy = py(f.height)
                    drawCircle(palette.firefly.copy(alpha = 0.28f * pulse), radius = size.height * 0.045f, center = Offset(fx, fy))
                    drawCircle(palette.firefly, radius = size.height * 0.016f, center = Offset(fx, fy))
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = size.height * 0.005f, center = Offset(fx - size.width * 0.006f, fy - size.height * 0.006f))
                }

            // Ground
            drawRoundRect(
                color = palette.ground,
                topLeft = Offset(-size.width * 0.05f, groundY),
                size = androidx.compose.ui.geometry.Size(size.width * 1.1f, size.height * 0.15f),
                cornerRadius = CornerRadius(size.height * 0.03f),
            )
            repeat(9) { i ->
                val gx = (i / 9f) * size.width
                drawLine(
                    color = palette.leaf.copy(alpha = 0.7f),
                    start = Offset(gx, groundY - size.height * 0.004f),
                    end = Offset(gx + size.width * 0.008f, groundY - size.height * 0.035f),
                    strokeWidth = size.height * 0.008f,
                )
            }

            // Tarsier
            val feetY = py(s.y).coerceIn(-size.height * 0.1f, size.height * 0.98f)
            drawTarsier(centerX = px(s.x), feetY = feetY, size = size.height * 0.24f)
        }
    }
}

private fun drawTrunkRow(scope: DrawScope, px: (Float) -> Float, py: (Float) -> Float, palette: CanopyPalette, heightPx: Float, parallaxOffset: Float) {
    val spacing = 1.3f
    val startTrunk = (parallaxOffset % spacing) - spacing
    var tx = startTrunk
    while (tx < 1.4f) {
        val screenX = (tx + spacing) / spacing
        scope.drawRoundRect(
            color = palette.trunk.copy(alpha = 0.32f),
            topLeft = Offset(screenX * scope.size.width - scope.size.width * 0.075f, -heightPx * 0.05f),
            size = androidx.compose.ui.geometry.Size(scope.size.width * 0.15f, heightPx * 1.12f),
            cornerRadius = CornerRadius(scope.size.width * 0.02f),
        )
        tx += spacing
    }
}

private fun DrawScope.drawTarsier(centerX: Float, feetY: Float, size: Float) {
    val fur = Color(0xFFD9A066)
    val furDark = Color(0xFFB07B45)
    val eye = Color(0xFF1F2937)
    val eyeHighlight = Color(0xFFFFFFFF)
    val blush = Color(0xFFF2B8A0)

    // Tail: curl up behind the body
    val tail = Path().apply {
        moveTo(centerX - size * 0.22f, feetY - size * 0.22f)
        cubicTo(
            centerX - size * 0.42f, feetY - size * 0.30f,
            centerX - size * 0.50f, feetY - size * 0.62f,
            centerX - size * 0.34f, feetY - size * 0.70f,
        )
    }
    drawPath(tail, furDark, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size * 0.09f))

    // Body
    drawOval(
        fur,
        topLeft = Offset(centerX - size * 0.26f, feetY - size * 0.48f),
        size = androidx.compose.ui.geometry.Size(size * 0.52f, size * 0.48f),
    )
    // Belly
    drawOval(
        fur.copy(alpha = 0.55f),
        topLeft = Offset(centerX - size * 0.16f, feetY - size * 0.40f),
        size = androidx.compose.ui.geometry.Size(size * 0.32f, size * 0.34f),
    )

    // Head (big, tarsier-style)
    val headCenter = Offset(centerX + size * 0.10f, feetY - size * 0.62f)
    drawCircle(fur, radius = size * 0.26f, center = headCenter)

    // Ears with inner tufts
    drawCircle(fur, radius = size * 0.12f, center = Offset(headCenter.x - size * 0.18f, headCenter.y - size * 0.16f))
    drawCircle(fur, radius = size * 0.12f, center = Offset(headCenter.x + size * 0.14f, headCenter.y - size * 0.20f))
    drawCircle(furDark.copy(alpha = 0.6f), radius = size * 0.06f, center = Offset(headCenter.x - size * 0.18f, headCenter.y - size * 0.16f))
    drawCircle(furDark.copy(alpha = 0.6f), radius = size * 0.06f, center = Offset(headCenter.x + size * 0.14f, headCenter.y - size * 0.20f))

    // Huge eyes — the tarsier's signature
    val eyeOffsetY = -size * 0.02f
    drawCircle(eye, radius = size * 0.115f, center = Offset(headCenter.x - size * 0.10f, headCenter.y + eyeOffsetY))
    drawCircle(eye, radius = size * 0.115f, center = Offset(headCenter.x + size * 0.10f, headCenter.y + eyeOffsetY))
    drawCircle(eyeHighlight, radius = size * 0.04f, center = Offset(headCenter.x - size * 0.08f, headCenter.y + eyeOffsetY - size * 0.03f))
    drawCircle(eyeHighlight, radius = size * 0.04f, center = Offset(headCenter.x + size * 0.12f, headCenter.y + eyeOffsetY - size * 0.03f))

    // Nose + blush
    drawCircle(furDark, radius = size * 0.025f, center = Offset(headCenter.x, headCenter.y + size * 0.09f))
    drawCircle(blush, radius = size * 0.045f, center = Offset(headCenter.x - size * 0.17f, headCenter.y + size * 0.10f))
    drawCircle(blush, radius = size * 0.045f, center = Offset(headCenter.x + size * 0.17f, headCenter.y + size * 0.10f))

    // Tiny arms
    drawLine(
        furDark, Offset(centerX - size * 0.14f, feetY - size * 0.40f),
        Offset(centerX - size * 0.24f, feetY - size * 0.24f), strokeWidth = size * 0.07f,
    )
    drawLine(
        furDark, Offset(centerX + size * 0.14f, feetY - size * 0.40f),
        Offset(centerX + size * 0.24f, feetY - size * 0.24f), strokeWidth = size * 0.07f,
    )
    // Legs
    drawCircle(furDark, radius = size * 0.08f, center = Offset(centerX - size * 0.14f, feetY - size * 0.05f))
    drawCircle(furDark, radius = size * 0.08f, center = Offset(centerX + size * 0.14f, feetY - size * 0.05f))
}

@Composable
private fun Side(ui: TarsierUiState, vm: TarsierCanopyViewModel, onExit: () -> Unit, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Teal.copy(alpha = 0.96f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.tarsier_title), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text(time(ui.remainingMillis), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { (ui.remainingMillis.toFloat() / ui.durationMillis).coerceIn(0f, 1f) },
                Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = Gold,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.tarsier_fireflies, ui.game.fireflies),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.tarsier_rounds, ui.game.roundsCompleted),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 17.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ToggleIcon(ui.game.assistedMode, Icons.Default.AccessibilityNew, stringResource(R.string.tarsier_toggle_assist), vm::toggleAssist)
                ToggleIcon(ui.game.reducedMotion, Icons.Default.MotionPhotosOff, stringResource(R.string.tarsier_toggle_calm), vm::toggleReducedMotion)
                ToggleIcon(ui.soundEnabled, if (ui.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, stringResource(R.string.tarsier_toggle_sound), vm::toggleSound)
                ToggleIcon(false, Icons.Default.Pause, stringResource(R.string.tarsier_toggle_pause), vm::pause)
                ToggleIcon(false, Icons.Default.ExitToApp, stringResource(R.string.tarsier_toggle_leave), onExit)
            }
            if (ui.breakExpired) {
                Text(
                    stringResource(R.string.tarsier_expired_notice),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleIcon(active: Boolean, icon: ImageVector, label: String, click: () -> Unit) {
    IconButton(
        onClick = click,
        modifier = Modifier
            .size(54.dp)
            .background(if (active) Gold else Color.White.copy(alpha = 0.16f), CircleShape),
    ) {
        Icon(icon, label, tint = if (active) Ink else Color.White)
    }
}

@Composable
private fun Header(ui: TarsierUiState, onExit: () -> Unit) {
    Surface(color = Teal, shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.tarsier_title),
                Modifier.weight(1f),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(time(ui.remainingMillis), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, stringResource(R.string.tarsier_toggle_leave), tint = Color.White) }
        }
    }
}

@Composable
private fun PauseOverlay(resume: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Pause, null, Modifier.size(54.dp), tint = Teal)
                Text(stringResource(R.string.tarsier_paused), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Button(onClick = resume) { Text(stringResource(R.string.tarsier_continue), fontSize = 19.sp) }
            }
        }
    }
}

@Composable
private fun ExitDialog(stay: () -> Unit, leave: () -> Unit) {
    AlertDialog(
        onDismissRequest = stay,
        title = { Text(stringResource(R.string.tarsier_leave_title)) },
        text = { Text(stringResource(R.string.tarsier_leave_body)) },
        confirmButton = { Button(onClick = leave) { Text(stringResource(R.string.tarsier_save_and_leave)) } },
        dismissButton = { OutlinedButton(onClick = stay) { Text(stringResource(R.string.tarsier_keep_playing)) } },
    )
}

@Composable
private fun courseTitle(course: CanopyCourse): String = when (course.id) {
    "canopy-moonlight" -> stringResource(R.string.tarsier_course_moonlight)
    "canopy-sunset" -> stringResource(R.string.tarsier_course_sunset)
    else -> stringResource(R.string.tarsier_course_morning)
}

@Composable
private fun feedbackLabel(feedback: CanopyFeedback): String = when (feedback) {
    CanopyFeedback.WELCOME -> stringResource(R.string.tarsier_feedback_welcome)
    CanopyFeedback.START -> stringResource(R.string.tarsier_feedback_start)
    CanopyFeedback.HOP -> stringResource(R.string.tarsier_feedback_hop)
    CanopyFeedback.BIG_HOP -> stringResource(R.string.tarsier_feedback_big_hop)
    CanopyFeedback.FIREFLY -> stringResource(R.string.tarsier_feedback_firefly)
    CanopyFeedback.BUMP -> stringResource(R.string.tarsier_feedback_bump)
    CanopyFeedback.GREAT_JUMP -> stringResource(R.string.tarsier_feedback_great_jump)
    CanopyFeedback.ROUND_COMPLETE -> stringResource(R.string.tarsier_feedback_round_complete)
    CanopyFeedback.NEXT_READY -> stringResource(R.string.tarsier_feedback_next_ready)
    CanopyFeedback.ASSISTED_ON -> stringResource(R.string.tarsier_feedback_assist_on)
    CanopyFeedback.ASSISTED_OFF -> stringResource(R.string.tarsier_feedback_assist_off)
    CanopyFeedback.CALM_ON -> stringResource(R.string.tarsier_feedback_calm_on)
    CanopyFeedback.CALM_OFF -> stringResource(R.string.tarsier_feedback_calm_off)
}

private fun time(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}