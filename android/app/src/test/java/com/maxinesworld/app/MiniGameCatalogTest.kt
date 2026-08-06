package com.maxinesworld.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MiniGameCatalogTest {
    @Test
    fun `catalog contains the curated offline source games`() {
        assertEquals(29, MiniGameCatalog.games.size)
        assertEquals("Word Search", MiniGameCatalog.find("word-search")?.title)
        assertNotNull(MiniGameCatalog.find("2048"))
    }

    @Test
    fun `unsafe source themes are not exposed to the child catalog`() {
        assertNull(MiniGameCatalog.find("blackjack"))
        assertNull(MiniGameCatalog.find("hangman"))
        assertNull(MiniGameCatalog.find("minesweeper"))
        assertNull(MiniGameCatalog.find("space-invaders"))
    }

    @Test
    fun `source games have stable zero reward result ids`() {
        val game = checkNotNull(MiniGameCatalog.find("word-search"))
        assertEquals("source-mini-word-search", game.gameId)
    }
}
