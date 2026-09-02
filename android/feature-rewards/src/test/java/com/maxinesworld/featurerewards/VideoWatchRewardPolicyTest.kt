package com.maxinesworld.featurerewards

import com.maxinesworld.coremodel.MediaAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoWatchRewardPolicyTest {

    @Test
    fun `only grade 3 released curriculum watches are creditable`() {
        assertTrue(VideoWatchRewardPolicy.shouldCreditCurriculumWatch(asset()))
        assertFalse(VideoWatchRewardPolicy.shouldCreditCurriculumWatch(asset(releaseStatus = "PREVIEW")))
        assertFalse(VideoWatchRewardPolicy.shouldCreditCurriculumWatch(asset(gradeLevel = 1)))
        assertFalse(VideoWatchRewardPolicy.shouldCreditCurriculumWatch(asset(gradeLevel = 4)))
    }

    @Test
    fun `accredited seconds ignore preview and other-grade ledger rows`() {
        val catalog = listOf(
            asset(mediaId = "g3-released", durationSeconds = 120),
            asset(mediaId = "g3-preview", releaseStatus = "PREVIEW", durationSeconds = 1800),
            asset(mediaId = "g1-released", gradeLevel = 1, durationSeconds = 900),
        )
        val seconds = VideoWatchRewardPolicy.accreditedSecondsForChildFacing(
            passedEntries = listOf(
                "g3-released" to 120,
                "g3-preview" to 1800,
                "g1-released" to 900,
                "unknown-retired" to 600,
            ),
            catalog = catalog,
        )
        assertEquals(120, seconds)
        assertEquals(0, VideoWatchRewardPolicy.calculateEarnedStickers(seconds))
    }

    @Test
    fun `preview ledger time cannot push a child past a sticker threshold`() {
        val catalog = listOf(asset(mediaId = "g3-released", durationSeconds = 100))
        val seconds = VideoWatchRewardPolicy.accreditedSecondsForChildFacing(
            passedEntries = listOf(
                "g3-preview" to 1800,
                "g3-released" to 100,
            ),
            catalog = catalog,
        )
        assertEquals(100, seconds)
        assertEquals(0, VideoWatchRewardPolicy.calculateEarnedStickers(seconds))
    }

    private fun asset(
        mediaId: String = "yt-test",
        gradeLevel: Int = 3,
        releaseStatus: String = "RELEASED",
        durationSeconds: Int = 60,
    ) = MediaAsset(
        mediaId = mediaId,
        title = mediaId,
        file = "media/$mediaId.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1L,
        durationSeconds = durationSeconds,
        width = 1,
        height = 1,
        subjectId = "english",
        gradeLevel = gradeLevel,
        releaseStatus = releaseStatus,
    )
}
