package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.MediaAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestTargetResolverTest {
    @Test
    fun `media assets map to title subject duration and passed state`() {
        val assets = listOf(
            asset("math-video", "mathematics", "Building Numbers", 125),
            asset("english-video", "english", "Picture Detective", 60),
        )

        val targets = QuestTargetResolver.resolve(
            assigned = listOf("math-video", "english-video", "math-video"),
            completed = setOf("math-video"),
            assets = assets,
        )

        assertEquals(2, targets.size)
        assertEquals("math-video", targets[0].mediaId)
        assertEquals("Building Numbers", targets[0].title)
        assertEquals("mathematics", targets[0].subjectId)
        assertEquals(125, targets[0].durationSeconds)
        assertEquals("02:05", targets[0].durationLabel)
        assertTrue(targets[0].isCompleted)
        assertFalse(targets[1].isCompleted)
    }

    @Test
    fun `missing catalog renders no targets instead of lesson fallback`() {
        assertTrue(
            QuestTargetResolver.resolve(
                assigned = listOf("video-1"),
                completed = emptySet(),
                assets = null,
            ).isEmpty(),
        )
    }

    private fun asset(mediaId: String, subject: String, title: String, seconds: Int) = MediaAsset(
        mediaId = mediaId,
        title = title,
        file = "$mediaId.mp4",
        sha256 = "",
        sizeBytes = 1L,
        durationSeconds = seconds,
        width = 1,
        height = 1,
        subjectId = subject,
    )
}
