package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class MediaCacheManagerTest {
    private lateinit var root: File
    private lateinit var storage: MediaStorage

    @Before
    fun setUp() {
        root = createTempDir(prefix = "media-cache-manager-")
        storage = MediaStorage(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `resolve returns verified local file without network`() {
        val bytes = "cached-video".toByteArray()
        val asset = asset("cached-video", bytes)
        install(asset, bytes)

        val result = MediaCacheManager(storage, maxStorageBytes = 100).resolve(asset)

        assertTrue(result is MediaCacheResolution.Local)
        assertEquals(storage.mediaFile(asset), (result as MediaCacheResolution.Local).file)
    }

    @Test
    fun `resolve falls back to remote url when cache is absent and online`() {
        val asset = asset("remote-video", "remote".toByteArray())
        val manager = MediaCacheManager(storage, maxStorageBytes = 100)

        val result = manager.resolve(asset, isOnline = true, mediaBaseUrl = "https://10.10.10.33/content/media/")

        assertEquals(
            MediaCacheResolution.Remote("https://10.10.10.33/content/media/video.mp4"),
            result,
        )
    }

    @Test
    fun `resolve reports unavailable when cache is absent and offline`() {
        val asset = asset("offline-video", "offline".toByteArray())

        assertEquals(
            MediaCacheResolution.Unavailable,
            MediaCacheManager(storage, 100).resolve(asset, isOnline = false),
        )
    }

    @Test
    fun `reserve evicts oldest verified files until storage limit is satisfied`() {
        val oldBytes = ByteArray(6) { 1 }
        val newBytes = ByteArray(6) { 2 }
        val old = asset("old-video", oldBytes)
        val recent = asset("recent-video", newBytes)
        install(old, oldBytes).setLastModified(1_000)
        install(recent, newBytes).setLastModified(2_000)

        val manager = MediaCacheManager(storage, maxStorageBytes = 10)
        manager.reserve(4, protectedMediaIds = setOf("recent-video"))

        assertNull(manager.localFile(old))
        assertTrue(manager.localFile(recent)?.isFile == true)
        assertTrue(storage.totalStorageUsedBytes() + 4 <= 10)
    }

    private fun install(asset: MediaAsset, bytes: ByteArray): File {
        val partial = storage.partialFile(asset)
        partial.writeBytes(bytes)
        return storage.installVerified(partial, asset)
    }

    private fun asset(id: String, bytes: ByteArray) = MediaAsset(
        mediaId = id,
        title = id,
        file = "video.mp4",
        sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) },
        sizeBytes = bytes.size.toLong(),
        durationSeconds = 1,
        width = 640,
        height = 360,
    )
}
