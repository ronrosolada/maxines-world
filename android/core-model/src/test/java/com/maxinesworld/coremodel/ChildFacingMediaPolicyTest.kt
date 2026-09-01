package com.maxinesworld.coremodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildFacingMediaPolicyTest {

    @Test
    fun `grade 3 released videos with a subject and duration are child-facing`() {
        assertTrue(ChildFacingMediaPolicy.isChildFacingCurriculum(asset()))
    }

    @Test
    fun `preview other-grade blank-subject and zero-duration videos are hidden`() {
        assertFalse(ChildFacingMediaPolicy.isChildFacingCurriculum(asset(releaseStatus = "PREVIEW")))
        assertFalse(ChildFacingMediaPolicy.isChildFacingCurriculum(asset(gradeLevel = 1)))
        assertFalse(ChildFacingMediaPolicy.isChildFacingCurriculum(asset(gradeLevel = 2)))
        assertFalse(ChildFacingMediaPolicy.isChildFacingCurriculum(asset(gradeLevel = 4)))
        assertFalse(ChildFacingMediaPolicy.isChildFacingCurriculum(asset(subjectId = "")))
        assertFalse(ChildFacingMediaPolicy.isChildFacingCurriculum(asset(durationSeconds = 0)))
    }

    @Test
    fun `childFacing keeps only grade 3 released curriculum videos`() {
        val kept = ChildFacingMediaPolicy.childFacing(
            listOf(
                asset(mediaId = "g3-released"),
                asset(mediaId = "g3-preview", releaseStatus = "PREVIEW"),
                asset(mediaId = "g1-released", gradeLevel = 1),
                asset(mediaId = "g4-preview", gradeLevel = 4, releaseStatus = "PREVIEW"),
            )
        )
        assertEquals(listOf("g3-released"), kept.map { it.mediaId })
    }

    private fun asset(
        mediaId: String = "yt-test",
        gradeLevel: Int = 3,
        releaseStatus: String = "RELEASED",
        subjectId: String = "english",
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
        subjectId = subjectId,
        gradeLevel = gradeLevel,
        releaseStatus = releaseStatus,
    )
}
