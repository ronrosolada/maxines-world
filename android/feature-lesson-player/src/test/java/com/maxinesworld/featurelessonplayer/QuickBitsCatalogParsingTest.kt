package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.QuickBitItem
import com.maxinesworld.coremodel.QuickBitsCatalog
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class QuickBitsCatalogParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `parse sample catalog json correctly`() {
        val sampleJson = """
        {
          "version": 1,
          "section": "Quick Bits",
          "targetAge": "8-10",
          "totalCount": 2,
          "totalSizeBytes": 25000000,
          "totalSizeMb": 23.84,
          "categories": ["animals", "space"],
          "items": [
            {
              "id": "qb_animals_5yHeg5hnq4Q",
              "videoId": "5yHeg5hnq4Q",
              "title": "Reindeer Fun Facts! 🦌",
              "channel": "Nat Geo Kids",
              "category": "animals",
              "durationSeconds": 75,
              "sizeBytes": 10000000,
              "sizeMb": 9.54,
              "resolution": "854x480",
              "videoUrl": "http://10.10.10.33/quickbits/qb_animals_5yHeg5hnq4Q.mp4",
              "thumbnailUrl": "http://10.10.10.33/quickbits/qb_animals_5yHeg5hnq4Q.jpg"
            },
            {
              "id": "qb_space_0MzWiKjSr9I",
              "videoId": "0MzWiKjSr9I",
              "title": "What Is a Nebula in Deep Space?",
              "channel": "NASA Space Place",
              "category": "space",
              "durationSeconds": 169,
              "sizeBytes": 15000000,
              "sizeMb": 14.30,
              "resolution": "854x480",
              "videoUrl": "http://10.10.10.33/quickbits/qb_space_0MzWiKjSr9I.mp4",
              "thumbnailUrl": "http://10.10.10.33/quickbits/qb_space_0MzWiKjSr9I.jpg"
            }
          ]
        }
        """.trimIndent()

        val catalog = json.decodeFromString<QuickBitsCatalog>(sampleJson)
        assertEquals(1, catalog.version)
        assertEquals("Quick Bits", catalog.section)
        assertEquals(2, catalog.items.size)
        assertEquals("qb_animals_5yHeg5hnq4Q", catalog.items[0].id)
        assertEquals("animals", catalog.items[0].category)
        assertEquals(75, catalog.items[0].durationSeconds)
        assertEquals("Nat Geo Kids", catalog.items[0].channel)
        assertEquals("space", catalog.items[1].category)
        assertEquals(15000000L, catalog.items[1].sizeBytes)
    }

    @Test
    fun `quick bits accepts only bounded trusted LAN media paths`() {
        val valid = QuickBitItem(
            id = "qb_animals_valid",
            title = "Animals",
            category = "animals",
            durationSeconds = 75,
            sizeBytes = 10_000_000,
            videoUrl = "http://10.10.10.33/quickbits/qb_animals_valid.mp4",
            thumbnailUrl = "http://10.10.10.33/quickbits/qb_animals_valid.jpg",
        )
        assertTrue(validateQuickBitsItem(valid).isEmpty())
        assertFalse(
            validateQuickBitsItem(valid.copy(videoUrl = "https://example.com/video.mp4")).isEmpty()
        )
        assertFalse(
            validateQuickBitsItem(valid.copy(sizeBytes = 101L * 1024L * 1024L)).isEmpty()
        )
        assertFalse(
            validateQuickBitsItem(valid.copy(videoUrl = "http://10.10.10.33/other/video.mp4")).isEmpty()
        )
    }


    @Test
    fun `quick bits rejects traversal paths including encoded traversal`() {
        val valid = QuickBitItem(
            id = "qb_animals_valid",
            title = "Animals",
            category = "animals",
            durationSeconds = 75,
            sizeBytes = 10_000_000,
            videoUrl = "http://10.10.10.33/quickbits/qb_animals_valid.mp4",
            thumbnailUrl = "http://10.10.10.33/quickbits/qb_animals_valid.jpg",
        )
        assertFalse(validateQuickBitsItem(valid.copy(videoUrl = "http://10.10.10.33/quickbits/../private.mp4")).isEmpty())
        assertFalse(validateQuickBitsItem(valid.copy(videoUrl = "http://10.10.10.33/quickbits/%2e%2e/private.mp4")).isEmpty())
    }

    @Test
    fun `cached quick bits require exact size and matching hash`() {
        val file = File.createTempFile("quick-bits", ".mp4")
        try {
            file.writeText("abc")
            val item = QuickBitItem(
                id = "qb_animals_valid",
                title = "Animals",
                category = "animals",
                durationSeconds = 75,
                sizeBytes = 3,
                videoUrl = "http://10.10.10.33/quickbits/qb_animals_valid.mp4",
                sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            )
            assertTrue(isQuickBitsFileValid(file, item))
            file.writeText("abd")
            assertFalse(isQuickBitsFileValid(file, item))
        } finally {
            file.delete()
        }
    }


    @Test
    fun `category filtering works as expected`() {
        val items = listOf(
            QuickBitItem(id = "1", title = "Lion", category = "animals", durationSeconds = 30, videoUrl = "url1"),
            QuickBitItem(id = "2", title = "Stars", category = "space", durationSeconds = 40, videoUrl = "url2"),
            QuickBitItem(id = "3", title = "Math", category = "math", durationSeconds = 50, videoUrl = "url3")
        )

        val animals = items.filter { it.category.equals("animals", ignoreCase = true) }
        val space = items.filter { it.category.equals("space", ignoreCase = true) }
        val all = items

        assertEquals(1, animals.size)
        assertEquals("Lion", animals[0].title)
        assertEquals(1, space.size)
        assertEquals(3, all.size)
    }
}
