package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import java.io.File
import java.io.FileInputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Private on-device storage for optional media. Files become visible to the
 * player only after size and SHA-256 verification, then an atomic promotion.
 */
class MediaStorage(
    private val root: File,
) {
    init {
        require(root.isDirectory || root.mkdirs()) {
            "Cannot create media storage directory: ${root.absolutePath}"
        }
    }

    fun mediaFile(asset: MediaAsset): File {
        return mediaFile(asset.mediaId)
    }

    fun mediaFile(mediaId: String): File {
        validateMediaId(mediaId)
        return File(root, "$mediaId.mp4")
    }

    fun partialFile(asset: MediaAsset): File {
        validateMediaId(asset.mediaId)
        return File(root, "${asset.mediaId}.mp4.part")
    }

    fun isVerified(asset: MediaAsset): Boolean {
        val file = mediaFile(asset)
        return file.isFile && file.length() == asset.sizeBytes && sha256(file) == asset.sha256
    }

    fun installVerified(partial: File, asset: MediaAsset): File {
        require(partial.isFile) { "Media partial file does not exist: ${partial.absolutePath}" }
        require(partial.length() == asset.sizeBytes) {
            "Media size mismatch for ${asset.mediaId}: expected ${asset.sizeBytes}, got ${partial.length()}"
        }
        require(sha256(partial) == asset.sha256) {
            "Media SHA-256 mismatch for ${asset.mediaId}"
        }

        val destination = mediaFile(asset)
        try {
            Files.move(
                partial.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                partial.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        return destination
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val MEDIA_ID_PATTERN = Regex("^[a-z0-9][a-z0-9-]{2,63}$")

        fun validateMediaId(mediaId: String) {
            require(mediaId.matches(MEDIA_ID_PATTERN)) { "Unsafe mediaId: $mediaId" }
        }
    }
}
