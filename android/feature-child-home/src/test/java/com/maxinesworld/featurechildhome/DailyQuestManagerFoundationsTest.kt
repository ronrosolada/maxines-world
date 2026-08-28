package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.FilipinoProficiency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyQuestManagerFoundationsTest {

    @Test
    fun `beginner receives foundations before grade three Filipino targets`() {
        val selected = prioritizeFilipinoFoundations(
            proficiency = FilipinoProficiency.BEGINNER,
            foundationIds = listOf("filipino-foundations-01", "filipino-foundations-02"),
            regularQuestIds = listOf("filipino-g3-video-01", "science-video-01"),
            passedQuestIds = emptySet(),
            limit = 3,
        )

        assertEquals(
            listOf("filipino-foundations-01", "filipino-foundations-02", "science-video-01"),
            selected,
        )
        assertTrue("native Grade 3 Filipino must wait behind unfinished foundations", "filipino-g3-video-01" !in selected)
    }

    @Test
    fun `completed foundations are skipped in canonical order`() {
        val selected = prioritizeFilipinoFoundations(
            proficiency = FilipinoProficiency.BEGINNER,
            foundationIds = listOf(
                "filipino-foundations-01",
                "filipino-foundations-02",
                "filipino-foundations-03",
                "filipino-foundations-04",
            ),
            regularQuestIds = listOf("filipino-g3-video-01"),
            passedQuestIds = setOf("filipino-foundations-01", "filipino-foundations-03"),
            limit = 3,
        )

        assertEquals(
            listOf("filipino-foundations-02", "filipino-foundations-04"),
            selected,
        )
    }

    @Test
    fun `intermediate keeps normal daily quest routing`() {
        val regular = listOf("filipino-g3-video-01", "science-video-01", "arena:filipino-g3")

        assertEquals(
            regular,
            prioritizeFilipinoFoundations(
                proficiency = FilipinoProficiency.INTERMEDIATE,
                foundationIds = listOf("filipino-foundations-01"),
                regularQuestIds = regular,
                passedQuestIds = emptySet(),
                limit = 3,
            ),
        )
    }

    @Test
    fun `advanced keeps normal daily quest routing`() {
        val regular = listOf("filipino-g3-video-01", "science-video-01")

        assertEquals(
            regular,
            prioritizeFilipinoFoundations(
                proficiency = FilipinoProficiency.ADVANCED,
                foundationIds = listOf("filipino-foundations-01"),
                regularQuestIds = regular,
                passedQuestIds = emptySet(),
                limit = 3,
            ),
        )
    }

    @Test
    fun `beginner with all foundations complete resumes normal routing`() {
        val foundations = listOf("filipino-foundations-01", "filipino-foundations-02")
        val regular = listOf("filipino-g3-video-01", "science-video-01")

        assertEquals(
            regular,
            prioritizeFilipinoFoundations(
                proficiency = FilipinoProficiency.BEGINNER,
                foundationIds = foundations,
                regularQuestIds = regular,
                passedQuestIds = foundations.toSet(),
                limit = 3,
            ),
        )
    }
}
