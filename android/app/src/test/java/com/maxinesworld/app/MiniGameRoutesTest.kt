package com.maxinesworld.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MiniGameRoutesTest {
    @Test
    fun `mini-game route patterns declare every navigation argument`() {
        assertEquals(
            "reward/{childId}/{rewardBreakId}",
            MiniGameRoutes.REWARD_HUB,
        )
        assertEquals(
            "reward/source-games/{childId}/{rewardBreakId}",
            MiniGameRoutes.SOURCE_LIBRARY,
        )
        assertEquals(
            "reward/source-game/{childId}/{rewardBreakId}/{durationMillis}/{gameSlug}",
            MiniGameRoutes.SOURCE_WEB_GAME,
        )
        assertEquals(
            "reward/cat-cafe/{childId}/{rewardBreakId}/{durationMillis}",
            MiniGameRoutes.CAT_CAFE,
        )
        assertEquals(
            "reward/parkour/{childId}/{rewardBreakId}/{durationMillis}",
            MiniGameRoutes.PARKOUR,
        )
        assertEquals(
            "reward/kitten-match/{childId}/{rewardBreakId}/{durationMillis}",
            MiniGameRoutes.KITTEN_MATCH,
        )
    }

    @Test
    fun `mini-game builders produce routes matching their patterns`() {
        val childId = "child-1"
        val breakId = "break-1"
        val durationMillis = 30_000L

        assertEquals(
            "reward/source-games/child-1/break-1",
            MiniGameRoutes.sourceLibrary(childId, breakId),
        )
        assertEquals(
            "reward/source-game/child-1/break-1/30000/word-search",
            MiniGameRoutes.sourceWebGame(childId, breakId, durationMillis, "word-search"),
        )
        assertEquals(
            "reward/cat-cafe/child-1/break-1/30000",
            MiniGameRoutes.catCafe(childId, breakId, durationMillis),
        )
        assertEquals(
            "reward/parkour/child-1/break-1/30000",
            MiniGameRoutes.parkour(childId, breakId, durationMillis),
        )
        assertEquals(
            "reward/kitten-match/child-1/break-1/30000",
            MiniGameRoutes.kittenMatch(childId, breakId, durationMillis),
        )
    }
}
