package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MediaDownloaderTest {
    private lateinit var server: MockWebServer
    private lateinit var root: File

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        root = File.createTempFile("media-downloader-test", "").also {
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
    fun `downloads media to a partial file then promotes only after verification`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "video/mp4")
                .setBody("video-bytes"),
        )

        val asset = asset()
        val storage = MediaStorage(root)
        val file = MediaDownloader(OkHttpClient(), storage)
            .download(server.url("/").toString(), asset)

        assertEquals(storage.mediaFile(asset), file)
        assertTrue(file.isFile)
        assertTrue(storage.isVerified(asset))
        assertEquals("/media/kids-tagalog-07-colors.mp4", server.takeRequest().path)
    }

    @Test
    fun `resumes an existing partial file with a range request`() = runBlocking {
        val asset = asset()
        val storage = MediaStorage(root)
        storage.partialFile(asset).writeText("video-")
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 6-10/11")
                .setBody("bytes"),
        )

        val file = MediaDownloader(OkHttpClient(), storage)
            .download(server.url("/").toString(), asset)

        assertEquals("video-bytes", file.readText())
        assertEquals("bytes=6-", server.takeRequest().getHeader("Range"))
        assertTrue(storage.isVerified(asset))
    }

    private fun asset() = MediaAsset(
        mediaId = "kids-tagalog-07-colors",
        title = "Learn the colors in Tagalog",
        file = "media/kids-tagalog-07-colors.mp4",
        sha256 = "79fd615a866fe7f9eb4da8d9c41ab57e3bd48056df42fd2c13e4d461a87afbe3",
        sizeBytes = 11,
        durationSeconds = 762,
        width = 640,
        height = 360,
    )
}
