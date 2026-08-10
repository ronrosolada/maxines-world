package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaAssessment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAssessmentGateTest {
    @Test
    fun `Tagalog assessment stays hidden until video is watched`() {
        val item = item(title = "Learn body words in Tagalog", mediaId = "kids-tagalog-01-body")

        assertFalse(canOpenMediaAssessment(item, watchedMediaIds = emptySet()))
        assertTrue(canOpenMediaAssessment(item, watchedMediaIds = setOf(item.asset.mediaId)))
    }

    @Test
    fun `assessment still requires a downloaded video`() {
        val item = item(title = "Learn body words in Tagalog", mediaId = "kids-tagalog-01-body", localPath = null)

        assertFalse(canOpenMediaAssessment(item, watchedMediaIds = setOf(item.asset.mediaId)))
    }

    @Test
    fun `non Tagalog assessment keeps existing downloaded behavior`() {
        val item = item(title = "Animal sounds", mediaId = "kids-english-01-animals")

        assertTrue(canOpenMediaAssessment(item, watchedMediaIds = emptySet()))
    }

    private fun item(title: String, mediaId: String, localPath: String? = "/tmp/$mediaId.mp4") =
        VideoLibraryItemUi(
            asset = MediaAsset(
                mediaId = mediaId,
                title = title,
                file = "media/$mediaId.mp4",
                sha256 = "a".repeat(64),
                sizeBytes = 1,
                durationSeconds = 60,
                width = 640,
                height = 360,
                assessment = MediaAssessment(
                    questionCount = 1,
                    passingCorrectCount = 1,
                    items = emptyList(),
                ),
            ),
            localPath = localPath,
        )
}
