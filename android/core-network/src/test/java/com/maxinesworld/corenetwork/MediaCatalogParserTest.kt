package com.maxinesworld.corenetwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCatalogParserTest {

    @Test
    fun `parses a versioned media catalog`() {
        val catalog = MediaCatalogParser().parse(catalogJson())

        assertEquals(1, catalog.catalogVersion)
        assertEquals(1, catalog.media.size)
        assertEquals("kids-tagalog-07-colors", catalog.media.single().mediaId)
        assertEquals("Learn the colors in Tagalog", catalog.media.single().title)
        assertEquals(640, catalog.media.single().width)
        assertEquals(360, catalog.media.single().height)
    }

    @Test
    fun `parses an optional assessment block`() {
        val catalog = MediaCatalogParser().parse(assessmentCatalogJson())

        val assessment = catalog.media.single().assessment
        assertEquals(5, assessment?.questionCount)
        assertEquals("kids-tagalog-07-colors-q01", assessment?.items?.first()?.itemId)
        assertEquals("b", assessment?.items?.first()?.correctOptionIds?.single())
    }

    @Test
    fun `rejects assessment answer key outside options`() {
        val json = assessmentCatalogJson().replace(
            "\"correctOptionIds\": [\"b\"]",
            "\"correctOptionIds\": [\"z\"]",
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            MediaCatalogParser().parse(json)
        }

        assertTrue(error.message.orEmpty().contains("answer key", ignoreCase = true))
    }

    @Test
    fun `rejects duplicate media ids`() {
        val entry = mediaEntryJson()
        val json = catalogJson("$entry,$entry")

        val error = assertThrows(IllegalArgumentException::class.java) {
            MediaCatalogParser().parse(json)
        }

        assertTrue(error.message.orEmpty().contains("duplicate", ignoreCase = true))
    }

    @Test
    fun `rejects invalid sha256`() {
        val json = catalogJson().replace("${"a".repeat(64)}", "not-a-sha256")

        val error = assertThrows(IllegalArgumentException::class.java) {
            MediaCatalogParser().parse(json)
        }

        assertTrue(error.message.orEmpty().contains("sha256", ignoreCase = true))
    }

    @Test
    fun `rejects path traversal`() {
        val json = catalogJson().replace(
            "media/kids-tagalog-07-colors.mp4",
            "media/../private.mp4",
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            MediaCatalogParser().parse(json)
        }

        assertTrue(error.message.orEmpty().contains("path", ignoreCase = true))
    }

    private fun assessmentCatalogJson(): String = """
        {
          "catalogVersion": 1,
          "generatedAt": "2026-08-09T00:00:00+08:00",
          "media": [{
            "mediaId": "kids-tagalog-07-colors",
            "title": "Learn the colors in Tagalog",
            "file": "media/kids-tagalog-07-colors.mp4",
            "sha256": "${"a".repeat(64)}",
            "sizeBytes": 34084049,
            "durationSeconds": 762,
            "width": 640,
            "height": 360,
            "mimeType": "video/mp4",
            "releaseStatus": "PREVIEW",
            "licenseStatus": "PERSONAL_USE",
            "assessment": {
              "questionCount": 5,
              "passingCorrectCount": 4,
              "claimsMastery": false,
              "items": [
                ${assessmentItemsJson()}
              ]
            }
          }]
        }
    """.trimIndent()

    private fun assessmentItemsJson(): String = (1..5).joinToString(",\n") { sequence ->
        val id = sequence.toString().padStart(2, '0')
        """{
          "itemId": "kids-tagalog-07-colors-q$id",
          "sequence": $sequence,
          "type": "MULTIPLE_CHOICE",
          "prompt": "Which word means red? $sequence",
          "options": [
            {"id": "a", "text": "berde"},
            {"id": "b", "text": "pula"},
            {"id": "c", "text": "asul"},
            {"id": "d", "text": "dilaw"}
          ],
          "correctOptionIds": ["b"],
          "explanation": "Pula means red."
        }""".trimIndent()
    }

    private fun catalogJson(entries: String = mediaEntryJson()): String = """
        {
          "catalogVersion": 1,
          "generatedAt": "2026-08-09T00:00:00+08:00",
          "media": [$entries]
        }
    """.trimIndent()

    private fun mediaEntryJson(): String = """
        {
          "mediaId": "kids-tagalog-07-colors",
          "title": "Learn the colors in Tagalog",
          "file": "media/kids-tagalog-07-colors.mp4",
          "sha256": "${"a".repeat(64)}",
          "sizeBytes": 34084049,
          "durationSeconds": 762,
          "width": 640,
          "height": 360,
          "mimeType": "video/mp4",
          "releaseStatus": "PREVIEW",
          "licenseStatus": "PERSONAL_USE"
        }
    """.trimIndent()
}
