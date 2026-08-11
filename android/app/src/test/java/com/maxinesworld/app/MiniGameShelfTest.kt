package com.maxinesworld.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameShelfTest {

    private val games: List<EmbeddedMiniGame> = MiniGameCatalog.games

    @Test
    fun `shelf order preserves every game exactly once`() {
        val ordered = MiniGameShelf.shelfOrder(games)
        assertEquals(games.size, ordered.size)
        assertEquals(games.map { it.slug }.toSet(), ordered.map { it.slug }.toSet())
    }

    @Test
    fun `kid-friendly games come before the classic games`() {
        val ordered = MiniGameShelf.shelfOrder(games).map { it.slug }
        val lastKidFriendly = ordered.indexOfLast { it in MiniGameShelf.kidFriendlyFirstOrder }
        val firstClassic = ordered.indexOfFirst { it !in MiniGameShelf.kidFriendlyFirstOrder }
        assertTrue("kid games must precede classics", lastKidFriendly < firstClassic)
    }

    @Test
    fun `memory match leads the shelf and word games are at the back`() {
        val ordered = MiniGameShelf.shelfOrder(games).map { it.slug }
        assertEquals("memory-match", ordered.first())
        assertTrue(ordered.indexOf("wordle") > ordered.indexOf("memory-match"))
        assertTrue(ordered.indexOf("word-search") > ordered.indexOf("memory-match"))
        assertTrue(ordered.indexOf("solitaire") > ordered.indexOf("memory-match"))
    }

    @Test
    fun `kid friendly count only counts shelf leads`() {
        assertEquals(MiniGameShelf.kidFriendlyFirstOrder.size, MiniGameShelf.kidFriendlyCount(games))
    }
}
