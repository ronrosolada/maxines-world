package com.maxinesworld.featurechildhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SanctuaryVisitorCatalogTest {

    @Test
    fun `visitor registry contains native philippine animals with required metadata`() {
        val visitors = SanctuaryVisitorCatalog.allVisitors
        assertTrue(visitors.isNotEmpty())
        assertEquals(6, visitors.size)

        visitors.forEach { visitor ->
            assertTrue(visitor.name.isNotBlank())
            assertTrue(visitor.localName.isNotBlank())
            assertTrue(visitor.slotId.startsWith("sanctuary-"))
            assertTrue(visitor.funFact.isNotBlank())
            assertTrue(visitor.nativeRegion.isNotBlank())
        }
    }

    @Test
    fun `visitors unlock dynamically based on earned sanctuary habitat pieces`() {
        val emptyEarned = emptySet<String>()
        assertTrue(SanctuaryVisitorCatalog.getVisitorsForUnlockedPieces(emptyEarned).isEmpty())

        val treeEarned = setOf("sanctuary-tree")
        val treeVisitors = SanctuaryVisitorCatalog.getVisitorsForUnlockedPieces(treeEarned)
        assertEquals(1, treeVisitors.size)
        assertEquals("Philippine Tarsier", treeVisitors.first().name)
        assertTrue(treeVisitors.first().isNocturnal)

        val multipleEarned = setOf("sanctuary-tree", "sanctuary-lookout", "sanctuary-meadow")
        val multipleVisitors = SanctuaryVisitorCatalog.getVisitorsForUnlockedPieces(multipleEarned)
        assertEquals(3, multipleVisitors.size)
        assertTrue(multipleVisitors.any { it.name == "Philippine Eagle" })
        assertTrue(multipleVisitors.any { it.name == "Mindoro Tamaraw" })
    }
}
