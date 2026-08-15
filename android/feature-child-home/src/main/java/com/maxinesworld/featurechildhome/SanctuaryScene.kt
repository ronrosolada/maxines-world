package com.maxinesworld.featurechildhome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    modifier: Modifier = Modifier,
) {
    val earnedIds = earnedSlotIds(sanctuary.visiblePieces)
    val allSlots = sanctuarySceneSlots()
    val nextSlot = nextSanctuarySlot()
    val showNext = sanctuary.nextPiece != null && earnedIds.size < sanctuary.totalPieces
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
                    .border(2.dp, slot.tint.copy(alpha = 0.55f), CircleShape),
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

        if (showNext) {
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
                    .border(2.dp, Color(0xFF8FAF9F), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(sanctuary.nextPiece?.iconKey ?: "next")),
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
                .border(3.dp, Color.White, CircleShape),
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
