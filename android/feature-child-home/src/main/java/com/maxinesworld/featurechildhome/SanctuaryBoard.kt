package com.maxinesworld.featurechildhome

/** One place on Milo's home-building board. */
data class SanctuaryBoardCellUi(
    val piece: SanctuaryPieceUi,
    val isEarned: Boolean,
    val isNext: Boolean,
)

/**
 * Orders the visible sanctuary as a child-facing board: earned places first,
 * then the next unlock, then the remaining locked places.
 */
internal fun sanctuaryBoardCells(
    sanctuary: SanctuaryUi,
    orderedPieces: List<SanctuaryPieceUi>,
): List<SanctuaryBoardCellUi> {
    val pieces = orderedPieces.take(sanctuary.totalPieces.coerceAtLeast(0))
    val earnedCount = sanctuary.earnedPieces.coerceIn(0, pieces.size)
    val visibleById = sanctuary.visiblePieces.associateBy { it.id }

    return pieces.mapIndexed { index, catalogPiece ->
        SanctuaryBoardCellUi(
            piece = visibleById[catalogPiece.id] ?: catalogPiece,
            isEarned = index < earnedCount,
            isNext = index == earnedCount && sanctuary.nextPiece != null,
        )
    }
}
