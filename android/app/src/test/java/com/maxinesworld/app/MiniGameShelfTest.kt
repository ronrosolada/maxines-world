package com.maxinesworld.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameShelfTest {

    private val games: List<EmbeddedMiniGame> = MiniGameCatalog.games

    @Test
    fun `shelf only includes the 8-year-old allowlist`() {
        val ordered = MiniGameShelf.shelfOrder(games)
        assertEquals(MiniGameShelf.childAllowlist.size, ordered.size)
        assertEquals(MiniGameShelf.childAllowlist.toSet(), ordered.map { it.slug }.toSet())
        MiniGameShelf.hiddenFromChildShelf.forEach { slug ->
            assertFalse(ordered.any { it.slug == slug })
            assertFalse(MiniGameShelf.isChildFacing(slug))
        }
    }

    @Test
    fun `kid-friendly games come before the extra classics`() {
        val ordered = MiniGameShelf.shelfOrder(games).map { it.slug }
        val lastKidFriendly = ordered.indexOfLast { it in MiniGameShelf.kidFriendlyFirstOrder }
        val firstExtra = ordered.indexOfFirst { it in MiniGameShelf.additionalChildGames }
        assertTrue("kid games must precede extra classics", lastKidFriendly < firstExtra)
    }

    @Test
    fun `memory match leads the shelf and adult games are absent`() {
        val ordered = MiniGameShelf.shelfOrder(games).map { it.slug }
        assertEquals("memory-match", ordered.first())
        assertFalse(ordered.contains("wordle"))
        assertFalse(ordered.contains("sudoku"))
        assertFalse(ordered.contains("solitaire"))
        assertFalse(ordered.contains("yahtzee"))
    }

    @Test
    fun `kid friendly count only counts shelf leads`() {
        assertEquals(MiniGameShelf.kidFriendlyFirstOrder.size, MiniGameShelf.kidFriendlyCount(games))
    }
}
