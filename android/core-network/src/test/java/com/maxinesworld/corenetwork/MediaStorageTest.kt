package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MediaStorageTest {

    @Test
    fun `promotes a verified partial file into the private media store`() {
        val root = tempDirectory()
        val storage = MediaStorage(root)
        val asset = asset(sizeBytes = 11, sha256 = "79fd615a866fe7f9eb4da8d9c41ab57e3bd48056df42fd2c13e4d461a87afbe3")
        val partial = storage.partialFile(asset)
        partial.writeText("video-bytes")

        val installed = storage.installVerified(partial, asset)

        assertEquals(storage.mediaFile(asset), installed)
        assertTrue(installed.isFile)
        assertFalse(partial.exists())
        assertTrue(storage.isVerified(asset))
        root.deleteRecursively()
    }

    @Test
    fun `rejects a file whose size or hash does not match the catalog`() {
        val root = tempDirectory()
        val storage = MediaStorage(root)
        val asset = asset(sizeBytes = 12, sha256 = "79fd615a866fe7f9eb4da8d9c41ab57e3bd48056df42fd2c13e4d461a87afbe3")
        val partial = storage.partialFile(asset)
        partial.writeText("video-bytes")

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            storage.installVerified(partial, asset)
        }

        assertTrue(error.message.orEmpty().contains("size", ignoreCase = true))
        assertFalse(storage.mediaFile(asset).exists())
        root.deleteRecursively()
    }

    private fun asset(sizeBytes: Long, sha256: String) = MediaAsset(
        mediaId = "kids-tagalog-07-colors",
        title = "Learn the colors in Tagalog",
        file = "media/kids-tagalog-07-colors.mp4",
        sha256 = sha256,
        sizeBytes = sizeBytes,
        durationSeconds = 762,
        width = 640,
        height = 360,
    )

    private fun tempDirectory(): File = File.createTempFile("media-storage-test", "").also {
        assertTrue(it.delete())
        assertTrue(it.mkdirs())
    }
}
