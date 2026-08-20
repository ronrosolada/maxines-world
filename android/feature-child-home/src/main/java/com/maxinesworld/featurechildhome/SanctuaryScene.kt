package com.maxinesworld.featurechildhome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.featurerewards.SanctuaryCatalog

/**
 * Scene placement for Milo's Wildlife Sanctuary (design.md §2).
 *
 * The sanctuary is a fixed 3-row meadow scene on the home card. Earned pieces
 * occupy their own slot; the next piece previews in a shaded outline slot.
 * Positions are fractions of the scene width/height so the layout can scale
 * across phones and tablets.
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

/** The single "next piece" preview slot, shown only when not all pieces are earned. */
internal fun nextSanctuarySlot(): SanctuarySlot = SanctuarySlot(
    pieceId = "sanctuary-next",
    xFraction = 0.64f,
    yFraction = 0.52f,
    sizeFraction = 0.20f,
    tint = SanctuarySceneColors.next,
)

/** Slot for a piece that has not been earned yet (not the preview slot). */
internal fun earnedSlotIds(visiblePieces: List<SanctuaryPieceUi>): Set<String> =
    visiblePieces.mapTo(mutableSetOf()) { it.id }

/** The 12 canonical piece ids in scene order (first earned → first in scene). */
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

/** Milo's spot in the scene — drawn on top of the meadow, front-left. */
internal val miloSanctuarySlot = SanctuarySlot(
    pieceId = "milo",
    xFraction = 0.17f,
    yFraction = 0.55f,
    sizeFraction = 0.34f,
    tint = Color(0xFFFFB84D),
)

/**
 * A living scene for Milo's Wildlife Sanctuary (design.md §2).
 *
 * The daily quest promise — "grow Milo's sanctuary" — finally becomes
 * something the child can see: each earned piece is placed in the meadow,
 * the next piece previews in an outline slot, and Milo stands guard.
 */
@Composable
fun SanctuaryScene(
    sanctuary: SanctuaryUi,
    onPieceClick: (SanctuaryPieceUi) -> Unit = {},
    onMiloClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val earnedIds = earnedSlotIds(sanctuary.visiblePieces)
    val visiblePieceMap = sanctuary.visiblePieces.associateBy { it.id }
    val allSlots = sanctuarySceneSlots()
    val nextSlot = nextSanctuarySlot()
    val showNext = sanctuary.nextPiece != null && earnedIds.size < sanctuary.totalPieces
    val reduceMotion = LocalAnimationsDisabled.current
    var miloBounced by remember { mutableStateOf(false) }
    var miloSpeechBubble by remember { mutableStateOf<String?>(null) }

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
        if (earnedIds.isNotEmpty()) {
            append("Placed: ")
            append(sanctuary.visiblePieces.joinToString { it.name })
            append(". ")
        }
        if (showNext && sanctuary.nextPiece != null) {
            append("Next piece: ${sanctuary.nextPiece.name}.")
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .semantics { contentDescription = sceneDescription },
    ) {
        val sceneWidth = maxWidth
        val sceneHeight = maxHeight

        // High-Fidelity Storybook Sanctuary Backdrop
        Image(
            painter = painterResource(R.drawable.sanctuary_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        fun slotSize(slot: SanctuarySlot) = sceneHeight * slot.sizeFraction

        allSlots.forEach { slot ->
            val earned = slot.pieceId in earnedIds
            val pieceUi = visiblePieceMap[slot.pieceId] ?: SanctuaryCatalog.byId(slot.pieceId)?.let {
                SanctuaryPieceUi(it.id, it.name, it.description, it.iconKey, it.residentWildlife, it.funFact)
            }
            val clickableModifier = if (earned && pieceUi != null) {
                Modifier.clickable { onPieceClick(pieceUi) }
            } else {
                Modifier
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
                            role = androidx.compose.ui.semantics.Role.Button
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

        if (showNext && sanctuary.nextPiece != null) {
            Box(
                Modifier
                    .size(slotSize(nextSlot))
                    .offset(
                        x = (sceneWidth * nextSlot.xFraction - slotSize(nextSlot) / 2)
                            .coerceIn(0.dp, sceneWidth),
                        y = (sceneHeight * nextSlot.yFraction - slotSize(nextSlot) / 2)
                            .coerceIn(0.dp, sceneHeight),
                    )
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
                    .border(2.dp, Color(0xFF8FAF9F), CircleShape)
                    .clickable { onPieceClick(sanctuary.nextPiece) }
                    .semantics {
                        role = androidx.compose.ui.semantics.Role.Button
                        contentDescription = "Next place: ${sanctuary.nextPiece.name}. Tap to preview."
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(sanctuary.nextPiece.iconKey)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                    )
                }
            }
        }

        // Milo, drawn last so he stands in front of the meadow.
        val idleOffset by if (reduceMotion) {
            remember { mutableStateOf(0f) }
        } else {
            rememberInfiniteTransition(label = "MiloIdleBreath").animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "MiloBob",
            )
        }
        val bounceScale by animateFloatAsState(
            targetValue = if (miloBounced) 1.22f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "MiloBounce"
        )

        Box(
            Modifier
                .size(slotSize(miloSanctuarySlot))
                .offset(
                    x = (sceneWidth * miloSanctuarySlot.xFraction - slotSize(miloSanctuarySlot) / 2)
                        .coerceIn(0.dp, sceneWidth),
                    y = (sceneHeight * miloSanctuarySlot.yFraction - slotSize(miloSanctuarySlot) / 2 + idleOffset.dp)
                        .coerceIn(0.dp, sceneHeight),
                )
                .graphicsLayer {
                    scaleX = bounceScale
                    scaleY = bounceScale
                }
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, Color.White, CircleShape)
                .clickable {
                    miloBounced = true
                    miloSpeechBubble = miloQuotes.random()
                    onMiloClick()
                }
                .semantics {
                    role = androidx.compose.ui.semantics.Role.Button
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

        // Milo Speech Bubble Overlay when tapped
        miloSpeechBubble?.let { quote ->
            LaunchedEffect(quote) {
                kotlinx.coroutines.delay(300)
                miloBounced = false
                kotlinx.coroutines.delay(2700)
                miloSpeechBubble = null
            }
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF087F83)),
                modifier = Modifier
                    .offset(x = 12.dp, y = 8.dp)
                    .widthIn(max = 240.dp)
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
