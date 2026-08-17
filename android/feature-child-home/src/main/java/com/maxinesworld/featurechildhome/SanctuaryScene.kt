package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.featurerewards.SanctuaryCatalog
import java.util.Calendar

/**
 * Scene placement for Milo's Wildlife Sanctuary (design.md §2).
 */
data class SanctuarySlot(
    val pieceId: String,
    val xFraction: Float,
    val yFraction: Float,
    val sizeFraction: Float,
    val tint: Color,
)

/** 12 canonical sanctuary pieces with their deterministic scene positions. */
internal fun sanctuarySceneSlots(): List<SanctuarySlot> = listOf(
    SanctuarySlot("sanctuary-meadow", 0.50f, 0.62f, 0.34f, SanctuarySceneColors.meadow),
    SanctuarySlot("sanctuary-pond", 0.22f, 0.58f, 0.26f, SanctuarySceneColors.pond),
    SanctuarySlot("sanctuary-tree", 0.78f, 0.42f, 0.30f, SanctuarySceneColors.tree),
    SanctuarySlot("sanctuary-nest", 0.14f, 0.30f, 0.22f, SanctuarySceneColors.nest),
    SanctuarySlot("sanctuary-garden", 0.86f, 0.70f, 0.26f, SanctuarySceneColors.garden),
    SanctuarySlot("sanctuary-path", 0.50f, 0.86f, 0.34f, SanctuarySceneColors.path),
    SanctuarySlot("sanctuary-shelter", 0.30f, 0.40f, 0.26f, SanctuarySceneColors.shelter),
    SanctuarySlot("sanctuary-butterfly", 0.66f, 0.22f, 0.18f, SanctuarySceneColors.butterfly),
    SanctuarySlot("sanctuary-lookout", 0.52f, 0.22f, 0.22f, SanctuarySceneColors.lookout),
    SanctuarySlot("sanctuary-reading-nest", 0.36f, 0.74f, 0.24f, SanctuarySceneColors.reading),
    SanctuarySlot("sanctuary-flower-bed", 0.12f, 0.74f, 0.22f, SanctuarySceneColors.flower),
    SanctuarySlot("sanctuary-wildlife-sign", 0.90f, 0.30f, 0.20f, SanctuarySceneColors.sign),
)

internal fun nextSanctuarySlot(): SanctuarySlot = SanctuarySlot(
    pieceId = "sanctuary-next",
    xFraction = 0.64f,
    yFraction = 0.52f,
    sizeFraction = 0.20f,
    tint = SanctuarySceneColors.next,
)

internal fun earnedSlotIds(visiblePieces: List<SanctuaryPieceUi>): Set<String> =
    visiblePieces.mapTo(mutableSetOf()) { it.id }

internal val sanctuaryPieceOrder: List<String> = listOf(
    "sanctuary-meadow",
    "sanctuary-pond",
    "sanctuary-tree",
    "sanctuary-nest",
    "sanctuary-garden",
    "sanctuary-path",
    "sanctuary-shelter",
    "sanctuary-butterfly",
    "sanctuary-lookout",
    "sanctuary-reading-nest",
    "sanctuary-flower-bed",
    "sanctuary-wildlife-sign",
)

internal object SanctuarySceneColors {
    val meadow = Color(0xFF7BC47F)
    val pond = Color(0xFF4FA8D9)
    val tree = Color(0xFF5B8C4E)
    val nest = Color(0xFFB98A4E)
    val garden = Color(0xFFE07B54)
    val path = Color(0xFFC9A26B)
    val shelter = Color(0xFF8B6F47)
    val butterfly = Color(0xFFE06B8B)
    val lookout = Color(0xFF6B5B95)
    val reading = Color(0xFF4C9A8C)
    val flower = Color(0xFFE8A23D)
    val sign = Color(0xFF7A6A52)
    val next = Color(0xFF9FB8AD)
}

internal val miloSanctuarySlot = SanctuarySlot(
    pieceId = "milo",
    xFraction = 0.17f,
    yFraction = 0.55f,
    sizeFraction = 0.34f,
    tint = Color(0xFFFFB84D),
)

/**
 * Living Sanctuary Scene with 24-hour Dynamic Ambient shift, Visiting Philippine Wildlife,
 * tap-to-feed mechanics, and audio storytelling inspections.
 */
@Composable
fun SanctuaryScene(
    sanctuary: SanctuaryUi,
    onPieceClick: (SanctuaryPieceUi) -> Unit = {},
    onVisitorClick: (SanctuaryVisitorUi) -> Unit = {},
    onMiloClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val earnedIds = earnedSlotIds(sanctuary.visiblePieces)
    val visiblePieceMap = sanctuary.visiblePieces.associateBy { it.id }
    val allSlots = sanctuarySceneSlots()
    val nextSlot = nextSanctuarySlot()
    val showNext = sanctuary.nextPiece != null && earnedIds.size < sanctuary.totalPieces
    val reduceMotion = LocalAnimationsDisabled.current

    // Day/Night State (auto-defaults based on tablet hour 7pm-6am or manual toggle)
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    var isNightMode by remember { mutableStateOf(currentHour >= 19 || currentHour < 6) }

    var miloBounced by remember { mutableStateOf(false) }
    var miloSpeechBubble by remember { mutableStateOf<String?>(null) }
    var fedVisitorId by remember { mutableStateOf<String?>(null) }
    var selectedTreat by remember { mutableStateOf<String?>("Sweet Fig") }

    val miloScale by animateFloatAsState(
        targetValue = if (miloBounced) 1.25f else 1.0f,
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "miloBounce",
    )

    val miloQuotes = remember {
        listOf(
            "Salamat sa pag-aaral! Milo loves our sanctuary!",
            "Look how peaceful our nature home is!",
            "Great job today! Every lesson helps our wildlife friends!",
            "Milo is happy to protect Philippine animals with you!",
        )
    }

    val sceneDescription = buildString {
        append("Milo's Wildlife Sanctuary. ")
        append(if (isNightMode) "Nighttime mode. " else "Daytime mode. ")
        if (earnedIds.isNotEmpty()) {
            append("Placed: ")
            append(sanctuary.visiblePieces.joinToString { it.name })
            append(". ")
        }
        if (sanctuary.visitors.isNotEmpty()) {
            append("Visiting wildlife: ")
            append(sanctuary.visitors.joinToString { it.name })
            append(". ")
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .semantics { contentDescription = sceneDescription },
    ) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight

        // Backdrop Base
        Image(
            painter = painterResource(R.drawable.sanctuary_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Dynamic Night Mode Overlay
        if (isNightMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xDD0F172A),
                                Color(0xBB1E293B),
                                Color(0x99064E3B),
                            )
                        )
                    )
            )

            // Night Moon Orb
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = sceneWidth - 54.dp, y = 14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF08A))
            )
        }

        // Mode Toggle Button (☀️ / 🌙)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isNightMode) Color(0xFF1E1B4B) else Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, if (isNightMode) Color(0xFF6366F1) else Color(0xFFCBD5E1)),
            modifier = Modifier
                .offset(x = 12.dp, y = 12.dp)
                .clickable { isNightMode = !isNightMode }
                .semantics {
                    role = Role.Button
                    contentDescription = if (isNightMode) "Switch to Day mode" else "Switch to Night mode"
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isNightMode) Icons.Default.Bedtime else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = if (isNightMode) Color(0xFFFDE047) else Color(0xFFEA580C),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isNightMode) "Cozy Night" else "Sunny Day",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) Color(0xFFE0E7FF) else Color(0xFF0F172A)
                )
            }
        }

        fun slotSize(slot: SanctuarySlot) = sceneHeight * slot.sizeFraction

        // Render Habitat Slots
        allSlots.forEach { slot ->
            val earned = slot.pieceId in earnedIds
            val pieceUi = visiblePieceMap[slot.pieceId] ?: SanctuaryCatalog.byId(slot.pieceId)?.let {
                SanctuaryPieceUi(it.id, it.name, it.description, it.iconKey, it.residentWildlife, it.funFact)
            }
            val clickableModifier = if (earned && pieceUi != null) {
                Modifier.clickable { onPieceClick(pieceUi) }
            } else {
                Modifier.clickable {
                    miloBounced = true
                    miloSpeechBubble = "Finish today's quest to unlock this habitat for visiting wildlife!"
                }
            }

            Box(
                Modifier
                    .size(slotSize(slot))
                    .offset(
                        x = (sceneWidth * slot.xFraction - slotSize(slot) / 2)
                            .coerceIn(0.dp, sceneWidth),
                        y = (sceneHeight * slot.yFraction - slotSize(slot) / 2)
                            .coerceIn(0.dp, sceneHeight),
                    )
                    .clip(CircleShape)
                    .background(if (earned) slot.tint else slot.tint.copy(alpha = 0.18f))
                    .border(2.dp, slot.tint.copy(alpha = 0.55f), CircleShape)
                    .then(clickableModifier)
                    .semantics {
                        if (earned && pieceUi != null) {
                            role = Role.Button
                            contentDescription = "${pieceUi.name} in sanctuary. Tap to inspect."
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (earned) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(slot.pieceId)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                    )
                } else {
                    Text("?", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Black, fontSize = (slotSize(slot) * 0.45f).value.sp)
                }
            }
        }

        // Visiting Wildlife Layer
        sanctuary.visitors.forEach { visitor ->
            val slot = allSlots.firstOrNull { it.pieceId == visitor.slotId }
            if (slot != null) {
                val isAwake = if (isNightMode) visitor.isNocturnal else !visitor.isNocturnal
                val drawableId = context.resources.getIdentifier(visitor.drawableResName, "drawable", context.packageName)
                val effectiveDrawable = if (drawableId != 0) drawableId else R.drawable.character_milo
                val isFed = fedVisitorId == visitor.id

                val visitorX = (sceneWidth * slot.xFraction - 24.dp).coerceIn(0.dp, sceneWidth - 56.dp)
                val visitorY = (sceneHeight * slot.yFraction - 36.dp).coerceIn(0.dp, sceneHeight - 64.dp)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = visitorX, y = visitorY)
                        .clickable {
                            if (selectedTreat != null) {
                                fedVisitorId = visitor.id
                            }
                            onVisitorClick(visitor)
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = "${visitor.name}. ${if (isAwake) "Active" else "Resting"}. Tap to feed or inspect."
                        }
                ) {
                    if (isFed) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text("❤️ Fed!", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else if (!isAwake) {
                        Text("Zzz...", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(visitor.localName, color = Color(0xFF0F172A), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }

                    Image(
                        painter = painterResource(effectiveDrawable),
                        contentDescription = visitor.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        }

        // Milo Mascot Guard
        Box(
            Modifier
                .size(slotSize(miloSanctuarySlot))
                .offset(
                    x = (sceneWidth * miloSanctuarySlot.xFraction - slotSize(miloSanctuarySlot) / 2)
                        .coerceIn(0.dp, sceneWidth),
                    y = (sceneHeight * miloSanctuarySlot.yFraction - slotSize(miloSanctuarySlot) / 2)
                        .coerceIn(0.dp, sceneHeight),
                )
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Color.White, CircleShape)
                .clickable {
                    miloBounced = true
                    miloSpeechBubble = miloQuotes.random()
                    onMiloClick()
                }
                .semantics {
                    role = Role.Button
                    contentDescription = "Milo, the sanctuary cat. Tap for an encouraging word."
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.character_milo),
                contentDescription = "Milo, the sanctuary cat",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }

        // Milo Speech Bubble Overlay
        miloSpeechBubble?.let { quote ->
            LaunchedEffect(quote) {
                kotlinx.coroutines.delay(300)
                miloBounced = false
                kotlinx.coroutines.delay(2700)
                miloSpeechBubble = null
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFF087F83)),
                modifier = Modifier
                    .offset(x = 12.dp, y = 50.dp)
                    .widthIn(max = 220.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = "🐾 \"$quote\"",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF183B4A),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Tap-to-Feed Toolbar
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🧺 Treats:", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF475569))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTreat == "Sweet Fig") Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (selectedTreat == "Sweet Fig") Color(0xFFF59E0B) else Color.Transparent),
                    modifier = Modifier.clickable { selectedTreat = "Sweet Fig" }
                ) {
                    Text("🍓 Fig", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTreat == "Jungle Leaf") Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (selectedTreat == "Jungle Leaf") Color(0xFF10B981) else Color.Transparent),
                    modifier = Modifier.clickable { selectedTreat = "Jungle Leaf" }
                ) {
                    Text("🌿 Leaf", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Text("Tap animal to feed ✨", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
            }
        }
    }
}

internal fun sanctuaryPieceDrawable(pieceId: String): Int = when (pieceId.removePrefix("sanctuary-")) {
    "meadow" -> R.drawable.sanctuary_piece_meadow
    "pond" -> R.drawable.sanctuary_piece_pond
    "tree" -> R.drawable.sanctuary_piece_tree
    "nest" -> R.drawable.sanctuary_piece_nest
    "garden" -> R.drawable.sanctuary_piece_garden
    "path" -> R.drawable.sanctuary_piece_path
    "shelter" -> R.drawable.sanctuary_piece_shelter
    "butterfly" -> R.drawable.sanctuary_piece_butterfly
    "lookout" -> R.drawable.sanctuary_piece_lookout
    "reading", "reading-nest" -> R.drawable.sanctuary_piece_reading_nest
    "flower", "flower-bed" -> R.drawable.sanctuary_piece_flower_bed
    "sign", "wildlife-sign" -> R.drawable.sanctuary_piece_wildlife_sign
    else -> R.drawable.sanctuary_piece_meadow
}
