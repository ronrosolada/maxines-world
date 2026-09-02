package com.maxinesworld.corenetwork

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class MediaLibraryTest {
    private lateinit var server: MockWebServer
    private lateinit var root: File

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        root = File.createTempFile("media-library-test", "").also {
            assertTrue(it.delete())
            assertTrue(it.mkdirs())
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
        root.deleteRecursively()
    }

    @Test
    fun `resolves a media id through the catalog before downloading`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(catalogJson()))
        server.enqueue(MockResponse().setResponseCode(200).setBody("video-bytes"))

        val client = OkHttpClient()
        val storage = MediaStorage(root)
        val library = MediaLibrary(
            catalogUrl = server.url("/media/catalog.json").toString(),
            mediaBaseUrl = server.url("/").toString(),
            catalogClient = MediaCatalogClient(client),
            downloader = MediaDownloader(client, storage),
            storage = storage,
        )

        val file = library.download("kids-tagalog-07-colors")

        assertEquals("video-bytes", file.readText())
        assertEquals("/media/catalog.json", server.takeRequest().path)
        assertEquals("/media/kids-tagalog-07-colors.mp4", server.takeRequest().path)
    }

    @Test
    fun `child-facing download rejects preview catalog rows`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(catalogJson(releaseStatus = "PREVIEW")))

        val library = library()
        try {
            library.downloadChildFacing("kids-tagalog-07-colors")
            fail("PREVIEW media must not download through the child-facing entry point")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("not child-facing curriculum"))
        }
        assertEquals("/media/catalog.json", server.takeRequest().path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `child-facing download fetches grade 3 released curriculum`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(catalogJson(releaseStatus = "RELEASED")))
        server.enqueue(MockResponse().setResponseCode(200).setBody("video-bytes"))

        val file = library().downloadChildFacing("kids-tagalog-07-colors")

        assertEquals("video-bytes", file.readText())
        assertEquals("/media/catalog.json", server.takeRequest().path)
        assertEquals("/media/kids-tagalog-07-colors.mp4", server.takeRequest().path)
    }

    @Test
    fun `uses the persisted catalog when a later refresh cannot reach the server`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(catalogJson()))
        server.enqueue(MockResponse().setResponseCode(503))

        val client = OkHttpClient()
        val storage = MediaStorage(root)
        val library = MediaLibrary(
            catalogUrl = server.url("/media/catalog.json").toString(),
            mediaBaseUrl = server.url("/").toString(),
            catalogClient = MediaCatalogClient(client),
            downloader = MediaDownloader(client, storage),
            storage = storage,
        )

        assertEquals(1, library.refreshCatalog().media.size)
        val cached = library.refreshCatalog()

        assertEquals(listOf("kids-tagalog-07-colors"), cached.media.map { it.mediaId })
        assertEquals("/media/catalog.json", server.takeRequest().path)
        assertEquals("/media/catalog.json", server.takeRequest().path)
    }

    @Test
    fun `uses the persisted catalog when a new library instance starts offline`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(catalogJson()))
        server.enqueue(MockResponse().setResponseCode(503))

        val client = OkHttpClient()
        val storage = MediaStorage(root)
        val firstLibrary = MediaLibrary(
            catalogUrl = server.url("/media/catalog.json").toString(),
            mediaBaseUrl = server.url("/").toString(),
            catalogClient = MediaCatalogClient(client),
            downloader = MediaDownloader(client, storage),
            storage = storage,
        )
        firstLibrary.refreshCatalog()

        val restartedLibrary = MediaLibrary(
            catalogUrl = server.url("/media/catalog.json").toString(),
            mediaBaseUrl = server.url("/").toString(),
            catalogClient = MediaCatalogClient(client),
            downloader = MediaDownloader(client, storage),
            storage = storage,
        )

        val cached = restartedLibrary.refreshCatalog()

        assertEquals(listOf("kids-tagalog-07-colors"), cached.media.map { it.mediaId })
        assertEquals("/media/catalog.json", server.takeRequest().path)
        assertEquals("/media/catalog.json", server.takeRequest().path)
    }

    private fun library(): MediaLibrary {
        val client = OkHttpClient()
        val storage = MediaStorage(root)
        return MediaLibrary(
            catalogUrl = server.url("/media/catalog.json").toString(),
            mediaBaseUrl = server.url("/").toString(),
            catalogClient = MediaCatalogClient(client),
            downloader = MediaDownloader(client, storage),
            storage = storage,
        )
    }

    private fun catalogJson(releaseStatus: String = "PREVIEW"): String = """
        {
          "catalogVersion": 1,
          "generatedAt": "2026-08-09T00:00:00+08:00",
          "media": [
            {
              "mediaId": "kids-tagalog-07-colors",
              "title": "Learn the colors in Tagalog",
              "file": "media/kids-tagalog-07-colors.mp4",
              "sha256": "79fd615a866fe7f9eb4da8d9c41ab57e3bd48056df42fd2c13e4d461a87afbe3",
              "sizeBytes": 11,
              "durationSeconds": 762,
              "width": 640,
              "height": 360,
              "subjectId": "filipino",
              "gradeLevel": 3,
              "mimeType": "video/mp4",
              "releaseStatus": "$releaseStatus",
              "licenseStatus": "PERSONAL_USE"
            }
          ]
        }
    """.trimIndent()
}
