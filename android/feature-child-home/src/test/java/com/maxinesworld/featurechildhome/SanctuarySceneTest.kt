package com.maxinesworld.featurechildhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SanctuarySceneTest {

    @Test
    fun `scene has one slot per sanctuary catalog piece plus the next preview`() {
        val slots = sanctuarySceneSlots()
        assertEquals(12, slots.size)
        assertEquals(12, slots.map { it.pieceId }.toSet().size)

        val next = nextSanctuarySlot()
        assertEquals("sanctuary-next", next.pieceId)
        assertTrue(slots.none { it.pieceId == next.pieceId })
    }

    @Test
    fun `piece order matches the reward catalog so the meadow is always first`() {
        assertEquals("sanctuary-meadow", sanctuaryPieceOrder.first())
        assertEquals("sanctuary-wildlife-sign", sanctuaryPieceOrder.last())
        assertEquals(12, sanctuaryPieceOrder.size)
        assertEquals(sanctuaryPieceOrder.toSet(), sanctuarySceneSlots().map { it.pieceId }.toSet())
    }

    @Test
    fun `every slot stays inside the scene bounds`() {
        sanctuarySceneSlots().forEach { slot ->
            assertTrue("x out of bounds for ${slot.pieceId}", slot.xFraction in 0.05f..0.95f)
            assertTrue("y out of bounds for ${slot.pieceId}", slot.yFraction in 0.05f..0.95f)
            assertTrue("size out of bounds for ${slot.pieceId}", slot.sizeFraction in 0.12f..0.5f)
        }
    }

    @Test
    fun `earned slot ids reflect only the visible pieces`() {
        val earned = earnedSlotIds(
            listOf(
                SanctuaryPieceUi("sanctuary-meadow", "Sunny Meadow", "A bright place.", "meadow"),
                SanctuaryPieceUi("sanctuary-pond", "Little Pond", "A quiet pond.", "pond"),
            ),
        )
        assertEquals(setOf("sanctuary-meadow", "sanctuary-pond"), earned)
        assertEquals(emptySet<String>(), earnedSlotIds(emptyList()))
    }
}
