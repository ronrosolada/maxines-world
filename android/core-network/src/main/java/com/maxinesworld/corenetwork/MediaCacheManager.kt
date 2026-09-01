package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import java.io.File

sealed interface MediaCacheResolution {
    data class Local(val file: File) : MediaCacheResolution
    data class Remote(val url: String) : MediaCacheResolution
    data object Unavailable : MediaCacheResolution
}

/** Resolves optional media local-first and enforces its storage budget. */
class MediaCacheManager(
    private val storage: MediaStorage,
    private val maxStorageBytes: Long,
) {
    init {
        require(maxStorageBytes >= 0L) { "maxStorageBytes must not be negative" }
    }

    fun resolve(
        asset: MediaAsset,
        isOnline: Boolean = true,
        mediaBaseUrl: String = DEFAULT_MEDIA_BASE_URL,
    ): MediaCacheResolution {
        localFile(asset)?.let { return MediaCacheResolution.Local(it) }
        return if (isOnline) {
            MediaCacheResolution.Remote("${mediaBaseUrl.trimEnd('/')}/${asset.file}")
        } else {
            MediaCacheResolution.Unavailable
        }
    }

    fun localFile(asset: MediaAsset): File? =
        storage.mediaFile(asset).takeIf { storage.isVerified(asset) }

    fun reserve(requiredBytes: Long, protectedMediaIds: Set<String> = emptySet()) {
        require(requiredBytes >= 0L) { "requiredBytes must not be negative" }

        val candidates = storage.downloadedMediaFiles()
            .asSequence()
            .filterNot { it.name.removeSuffix(MEDIA_FILE_SUFFIX) in protectedMediaIds }
            .sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })
            .iterator()

        while (storage.totalStorageUsedBytes() > maxStorageBytes - requiredBytes && candidates.hasNext()) {
            candidates.next().delete()
        }
    }

    private companion object {
        const val DEFAULT_MEDIA_BASE_URL = "https://10.10.10.33/content/media/"
        const val MEDIA_FILE_SUFFIX = ".mp4"
    }
}