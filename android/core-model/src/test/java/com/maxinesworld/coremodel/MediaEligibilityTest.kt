package com.maxinesworld.coremodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaEligibilityTest {
    private fun asset(subject: String, grade: Int, releaseStatus: String, license: String = "PERSONAL_USE") = MediaAsset(
        mediaId = "video-${subject.replace('-', '_')}-$grade-${releaseStatus.lowercase()}",
        title = "$subject Grade $grade video",
        file = "media/video.mp4",
        sha256 = "a".repeat(64),
        sizeBytes = 1,
        durationSeconds = 600,
        width = 640,
        height = 360,
        subjectId = subject,
        gradeLevel = grade,
        releaseStatus = releaseStatus,
        licenseStatus = license,
    )

    @Test
    fun `english math and science allow released grades three and four only`() {
        listOf("english", "mathematics", "science").forEach { subject ->
            assertTrue(asset(subject, 3, "RELEASED").isEligibleForCurriculumVideo())
            assertTrue(asset(subject, 4, "RELEASED").isEligibleForCurriculumVideo())
            assertTrue(asset(subject, 3, "PREVIEW").isEligibleForCurriculumVideo())
            assertTrue(asset(subject, 4, "PREVIEW").isEligibleForCurriculumVideo())
            assertFalse(asset(subject, 4, "PRODUCTION").isEligibleForCurriculumVideo())
            assertFalse(asset(subject, 2, "RELEASED").isEligibleForCurriculumVideo())
        }
    }

    @Test
    fun `filipino gmrc and makabansa allow released grades one through four`() {
        listOf("filipino", "gmrc", "makabansa").forEach { subject ->
            (1..4).forEach { grade ->
                assertTrue(asset(subject, grade, "RELEASED").isEligibleForCurriculumVideo())
                assertTrue(asset(subject, grade, "PREVIEW").isEligibleForCurriculumVideo())
            }
        }
    }

    @Test
    fun `preview personal use and released media are eligible but production is not`() {
        assertTrue(asset("mathematics", 3, "PREVIEW", "PERSONAL_USE").isEligibleForCurriculumVideo())
        assertFalse(asset("mathematics", 3, "PREVIEW", "UNKNOWN").isEligibleForCurriculumVideo())
        assertFalse(asset("mathematics", 3, "PRODUCTION").isEligibleForCurriculumVideo())
        assertFalse(asset("history", 3, "RELEASED").isEligibleForCurriculumVideo())
    }

    @Test
    fun `release status comparison is case insensitive`() {
        assertTrue(asset("english", 3, "released").isEligibleForCurriculumVideo())
    }
}
