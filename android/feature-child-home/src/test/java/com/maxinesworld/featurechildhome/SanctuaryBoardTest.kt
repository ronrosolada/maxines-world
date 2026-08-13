package com.maxinesworld.featurechildhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SanctuaryBoardTest {
    private val pieces = listOf(
        SanctuaryPieceUi("meadow", "Sunny Meadow", "A bright place.", "meadow"),
        SanctuaryPieceUi("pond", "Little Pond", "A quiet pond.", "pond"),
        SanctuaryPieceUi("tree", "Story Tree", "A shady tree.", "tree"),
        SanctuaryPieceUi("nest", "Bird Nest", "A cozy home.", "nest"),
    )

    @Test
    fun `empty sanctuary shows first place as next and the rest locked`() {
        val cells = sanctuaryBoardCells(SanctuaryUi(totalPieces = pieces.size, nextPiece = pieces.first()), pieces)

        assertEquals(4, cells.size)
        assertTrue(cells.first().isNext)
        assertFalse(cells.first().isEarned)
        assertEquals("Sunny Meadow", cells.first().piece.name)
        assertTrue(cells.drop(1).all { !it.isEarned && !it.isNext })
    }

    @Test
    fun `earned places are shown before the next place`() {
        val sanctuary = SanctuaryUi(
            earnedPieces = 2,
            visiblePieces = pieces.take(2),
            nextPiece = pieces[2],
            totalPieces = pieces.size,
        )

        val cells = sanctuaryBoardCells(sanctuary, pieces)

        assertEquals(listOf(true, true, false, false), cells.map { it.isEarned })
        assertEquals(listOf(false, false, true, false), cells.map { it.isNext })
        assertEquals("Story Tree", cells[2].piece.name)
    }

    @Test
    fun `complete sanctuary has no next place`() {
        val sanctuary = SanctuaryUi(
            earnedPieces = pieces.size,
            visiblePieces = pieces,
            nextPiece = null,
            totalPieces = pieces.size,
        )

        val cells = sanctuaryBoardCells(sanctuary, pieces)

        assertTrue(cells.all { it.isEarned })
        assertTrue(cells.none { it.isNext })
        assertNull(cells.firstOrNull { it.isNext })
    }
}
