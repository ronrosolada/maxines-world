package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardHeader
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardSurface
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.SanctuaryCatalog
import kotlin.math.roundToInt


// ─── Milo's Wildlife Sanctuary ───────────────────────────────────────

@Composable
internal fun SanctuaryPreview(
    sanctuary: SanctuaryUi,
    questTotal: Int,
    onTreatShopClick: () -> Unit,
    onVisitSanctuary: () -> Unit = {},
    onOpenJournal: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedPieceForInspect by remember { mutableStateOf<SanctuaryPieceUi?>(null) }
    var miloTapCount by remember { mutableIntStateOf(0) }
    val progress = if (sanctuary.totalPieces > 0) {
        (sanctuary.earnedPieces.toFloat() / sanctuary.totalPieces).coerceIn(0f, 1f)
    } else {
        0f
    }
    val orderedPieces = SanctuaryCatalog.pieces
        .take(sanctuary.totalPieces.coerceAtLeast(0))
        .map { piece ->
            SanctuaryPieceUi(
                id = piece.id,
                name = piece.name,
                description = piece.description,
                iconKey = piece.iconKey,
                residentWildlife = piece.residentWildlife,
                funFact = piece.funFact,
            )
        }
    val boardCells = sanctuaryBoardCells(sanctuary, orderedPieces)
    val workshopLabel = stringResource(R.string.home_sanctuary_workshop)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PlayCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, PlaySunshine.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Park, contentDescription = null, tint = PlayTeal, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.home_sanctuary_title),
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(
                        R.string.home_sanctuary_piece_count,
                        sanctuary.earnedPieces,
                        sanctuary.totalPieces,
                    ),
                    color = PlayTeal,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.home_sanctuary_subtitle, questTotal),
                color = PlayInk,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(10.dp))
            SanctuaryScene(
                sanctuary = sanctuary,
                onPieceClick = { piece -> selectedPieceForInspect = piece },
                onMiloClick = { miloTapCount++ },
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = PlayTeal,
                trackColor = PlayTeal.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PlayroomColors.SanctuaryBoardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Milo's home board. ${sanctuary.earnedPieces} of ${sanctuary.totalPieces} places added. Tap any place to inspect."
                    },
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.home_sanctuary_board_title),
                            color = PlayInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            "Tap to inspect",
                            color = PlayTeal,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                    }
                    boardCells.chunked(3).forEach { rowCells ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowCells.forEach { cell ->
                                SanctuaryBoardCell(
                                    cell = cell,
                                    onClick = {
                                        if (cell.isEarned || cell.isNext) {
                                            selectedPieceForInspect = cell.piece
                                        }
                                    }
                                )
                            }
                            repeat(3 - rowCells.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            MaxinesPrimaryButton(
                text = "Visit Sanctuary / Bisitahin ang Santuwaryo",
                onClick = onVisitSanctuary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenJournal,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Default.MenuBook, null, tint = PlayTeal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ranger Journal / Talaan ng Ranger",
                    color = PlayInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            val nextPiece = sanctuary.nextPiece
            if (nextPiece != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedPieceForInspect = nextPiece },
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            sanctuaryIcon(nextPiece.iconKey),
                            contentDescription = null,
                            tint = PlayTeal,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.home_sanctuary_next_reward),
                                color = PlayTeal,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                nextPiece.name,
                                color = PlayInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                            Text(
                                nextPiece.description,
                                color = PlayMuted,
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.home_sanctuary_complete),
                    color = PlayInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            if (nextPiece != null) {
                Spacer(Modifier.height(9.dp))
                Text(
                    stringResource(R.string.home_sanctuary_earn_hint, questTotal),
                    color = PlayMuted,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
            }
            TextButton(
                onClick = onTreatShopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = workshopLabel
                        role = Role.Button
                    },
            ) {
                Text(
                    stringResource(R.string.home_sanctuary_workshop),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
            }
        }
    }

    selectedPieceForInspect?.let { piece ->
        val isEarned = sanctuary.visiblePieces.any { it.id == piece.id }
        SanctuaryPieceInspectionDialog(
            piece = piece,
            isEarned = isEarned,
            onDismiss = { selectedPieceForInspect = null },
        )
    }
}

@Composable
private fun RowScope.SanctuaryBoardCell(
    cell: SanctuaryBoardCellUi,
    onClick: () -> Unit = {},
) {
    val background = when {
        cell.isEarned -> Color.White.copy(alpha = 0.92f)
        cell.isNext -> PlaySunshine.copy(alpha = 0.78f)
        else -> Color.White.copy(alpha = 0.42f)
    }
    val borderColor = when {
        cell.isNext -> PlayTeal
        cell.isEarned -> PlayTeal.copy(alpha = 0.28f)
        else -> PlayMuted.copy(alpha = 0.18f)
    }
    val clickableModifier = if (cell.isEarned || cell.isNext) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(clickableModifier)
            .semantics {
                role = Role.Button
                contentDescription = when {
                    cell.isEarned -> "${cell.piece.name}, place added to Milo's home. Tap to inspect."
                    cell.isNext -> "Next reward: ${cell.piece.name}. Finish today's quest to unlock. Tap to preview."
                    else -> "${cell.piece.name}, locked place."
                }
                stateDescription = when {
                    cell.isEarned -> "Added"
                    cell.isNext -> "Next reward"
                    else -> "Locked"
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(if (cell.isNext) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (cell.isEarned || cell.isNext) PlayTeal.copy(alpha = 0.12f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (cell.isEarned || cell.isNext) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(cell.piece.iconKey)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = PlayMuted.copy(alpha = 0.58f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                if (cell.isEarned || cell.isNext) cell.piece.name else stringResource(R.string.home_sanctuary_locked_place),
                color = if (cell.isEarned || cell.isNext) PlayInk else PlayMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SanctuaryPieceInspectionDialog(
    piece: SanctuaryPieceUi,
    isEarned: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isEarned) PlayTeal.copy(alpha = 0.15f) else PlaySunshine.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(sanctuaryPieceDrawable(piece.iconKey)),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
                Column {
                    Text(
                        piece.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlayInk,
                    )
                    Text(
                        if (isEarned) "Sanctuary Habitat • Unlocked" else "Next Habitat Unlock",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isEarned) PlayTeal else PlayroomColors.KeepsakeHeading,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    piece.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlayInk,
                    lineHeight = 20.sp,
                )

                if (piece.funFact.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PlayroomColors.SanctuarySurface,
                        border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Milo's Field Note",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = PlayTeal
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                piece.funFact,
                                style = MaterialTheme.typography.bodySmall,
                                color = PlayInk,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (piece.residentWildlife.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Native Animals That Love This Place:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PlayroomColors.KeepsakeHeading
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            piece.residentWildlife.forEach { animal ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, PlayTeal.copy(alpha = 0.2f)),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        animal,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = PlayInk,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PlayTeal),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PlayCream,
        shape = RoundedCornerShape(20.dp),
    )
}

private fun sanctuaryIcon(iconKey: String): ImageVector = when (iconKey) {
    "tree", "garden", "meadow", "flower" -> Icons.Default.Park
    else -> Icons.Default.Pets
}

// ─── Wildlife stickers preview (design.md §12) ───────────────────────

@Composable
internal fun WildlifeStickersPreview(
    wildlifeStickers: WildlifeStickersUi,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PlayCream,
        border = BorderStroke(1.dp, PlaySunshine.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_sticker_book),
                    color = PlayInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp, lineHeight = 22.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.home_collected, wildlifeStickers.collectedCount, wildlifeStickers.totalCount),
                    color = PlayMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, lineHeight = 18.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(12.dp))
            // Show won stickers plus locked "mystery" placeholders, capped at
            // 7 visible slots (design.md §12.2 / audit gap 12, 2026-08-06).
            val wonStickers = wildlifeStickers.stickers.filter { it.won }
            val previewSlots: List<StickerUi> = if (wonStickers.isEmpty() && wildlifeStickers.totalCount == 0) {
                emptyList()
            } else {
                val shown = wonStickers.take(7)
                shown + List((7 - shown.size).coerceAtLeast(0)) { i ->
                    StickerUi(id = "mystery-$i", won = false)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (previewSlots.isEmpty()) {
                    Text(
                        stringResource(R.string.home_no_stickers),
                        color = PlayMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp, lineHeight = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(previewSlots) { sticker -> StickerSlot(sticker) }
                    }
                }
                TextButton(onClick = onOpenCollection) {
                    Text(stringResource(R.string.home_open_field_guide), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun StickerSlot(sticker: StickerUi) {
    val mystery = stringResource(R.string.home_mystery_sticker)
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
            .background(
                if (sticker.won) Brush.linearGradient(
                    listOf(PlayroomColors.StickerWonStart, PlayroomColors.StickerWonEnd),
                )
                else Brush.linearGradient(
                    listOf(PlayroomColors.StickerLockedStart, PlayroomColors.StickerLockedEnd),
                ),
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (sticker.won) PlaySunshine else PlayroomColors.StickerLockedBorder,
                RoundedCornerShape(10.dp),
            )
            .semantics {
                contentDescription = if (sticker.won) "${sticker.id} collected sticker" else mystery
            },
        contentAlignment = Alignment.Center,
    ) {
        if (sticker.won) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = PlayTeal, modifier = Modifier.size(24.dp))
        } else {
            Text("?", color = PlayroomColors.StickerLockedText, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}
