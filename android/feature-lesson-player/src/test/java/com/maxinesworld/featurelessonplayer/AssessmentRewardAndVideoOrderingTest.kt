package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coremodel.MediaAsset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AssessmentRewardAndVideoOrderingTest {
    @Test
    fun `pass retake pass creates one assessment reward`() = runTest {
        val dao = mockk<RewardDao>()
        val stored = mutableListOf<RewardEntity>()
        coEvery { dao.getByChildAndMetadata(any(), any()) } answers {
            val childId = firstArg<String>()
            val metadata = secondArg<String>()
            stored.firstOrNull { it.childId == childId && it.metadata == metadata }
        }
        coEvery { dao.insertIgnoring(any()) } answers {
            stored += firstArg<RewardEntity>()
            1L
        }
        val reward = RewardEntity(
            id = "child-1:assessment_arena:stars:pack-1",
            childId = "child-1",
            type = "STARS",
            amount = 10,
            metadata = "assessment_arena_passed:pack-1",
        )

        repeat(3) { insertRewardIfAbsent(dao, reward) }

        assertEquals(1, stored.size)
        assertEquals(reward.metadata, stored.single().metadata)
        coVerify(exactly = 1) { dao.insertIgnoring(any()) }
    }

    @Test
    fun `video ordering uses grade quarter episode then title and passed items stay last`() {
        val assets = listOf(
            asset("g3-q1-e2", "Zebra", 3, 1, 2),
            asset("g2-q2-e1", "Quarter Two", 2, 2, 1),
            asset("g2-q1-e1-z", "Zeta", 2, 1, 1),
            asset("g2-q1-e1-a", "Alpha", 2, 1, 1),
            asset("g2-q1-e1-passed", "Passed", 2, 1, 1),
        )

        val ordered = sortMediaAssetsForLibrary(assets)
        assertEquals(
            listOf("g2-q1-e1-a", "g2-q1-e1-passed", "g2-q1-e1-z", "g2-q2-e1", "g3-q1-e2"),
            ordered.map { it.mediaId },
        )

        val (upcoming, completed) = partitionMediaAssetsForLibrary(
            assets,
            passedIds = setOf("g2-q1-e1-passed", "g3-q1-e2"),
        )
        assertEquals(listOf("g2-q1-e1-a", "g2-q1-e1-z", "g2-q2-e1"), upcoming.map { it.mediaId })
        assertEquals(listOf("g2-q1-e1-passed", "g3-q1-e2"), completed.map { it.mediaId })
    }

    private fun asset(id: String, title: String, grade: Int, quarter: Int, episode: Int) =
        MediaAsset(
            mediaId = id,
            title = title,
            file = "/media/$id.mp4",
            sha256 = "",
            sizeBytes = 1L,
            durationSeconds = 60,
            width = 1280,
            height = 720,
            gradeLevel = grade,
            quarter = quarter,
            episodeNumber = episode,
        )
}
