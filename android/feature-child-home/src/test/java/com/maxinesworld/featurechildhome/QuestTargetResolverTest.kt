package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.AssessmentPackMetadata
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
            isAssetDownloaded = { it.mediaId == "math-video" },
        )

        assertEquals(2, targets.size)
        assertEquals("math-video", targets[0].mediaId)
        assertEquals("Building Numbers", targets[0].title)
        assertEquals("mathematics", targets[0].subjectId)
        assertEquals(125, targets[0].durationSeconds)
        assertEquals("02:05", targets[0].durationLabel)
        assertTrue(targets[0].isCompleted)
        assertTrue(targets[0].isReadyOffline)
        assertFalse(targets[1].isCompleted)
        assertFalse(targets[1].isReadyOffline)
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

    @Test
    fun `arena ids resolve from bundled grade three pack metadata without video catalog`() {
        val pack = AssessmentPackMetadata(
            id = "science-g3-ph",
            subjectId = "science",
            curriculum = "ph",
            curriculumName = "Philippine DepEd",
            flagEmoji = "",
            title = "Grade 3 Science: Philippine DepEd",
            description = "",
            badgeKey = "badge_science_ph",
            file = "assessment-packs/science-g3-ph.json",
        )

        val targets = QuestTargetResolver.resolve(
            assigned = listOf("arena:science-g3-ph"),
            completed = setOf("arena:science-g3-ph"),
            assets = null,
            arenaPacks = listOf(pack),
        )

        assertEquals(1, targets.size)
        assertEquals(QuestTargetType.ARENA, targets.single().type)
        assertEquals("Grade 3 Science: Philippine DepEd", targets.single().title)
        assertEquals("science-g3-ph", targets.single().arenaPackId)
        assertTrue(targets.single().isCompleted)
    }

    @Test
    fun `preview and other-grade assigned videos do not resolve as quest targets`() {
        val targets = QuestTargetResolver.resolve(
            assigned = listOf("g3-released", "g3-preview", "g1-released"),
            completed = emptySet(),
            assets = listOf(
                asset("g3-released", "english", "Grade 3", 60),
                asset("g3-preview", "english", "Preview", 60).copy(releaseStatus = "PREVIEW"),
                asset("g1-released", "english", "Grade 1", 60).copy(gradeLevel = 1),
            ),
        )

        assertEquals(listOf("g3-released"), targets.map { it.mediaId })
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
        releaseStatus = "RELEASED",
    )
}
