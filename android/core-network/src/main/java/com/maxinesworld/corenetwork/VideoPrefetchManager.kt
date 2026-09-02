package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.ChildFacingMediaPolicy
import com.maxinesworld.coremodel.MediaAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intelligent background prefetcher for video assets.
 * When called (e.g. at app launch or on home network), scans the catalog and downloads
 * the next uncompleted/unverified video lessons so playback starts instantly with 0ms buffering.
 */
@Singleton
class VideoPrefetchManager @Inject constructor(
    private val mediaLibrary: MediaLibrary,
    private val mediaStorage: MediaStorage,
) {
    suspend fun prefetchNextVideos(count: Int = 2): Int = withContext(Dispatchers.IO) {
        try {
            val catalog = mediaLibrary.getCatalog()
            val unverified = ChildFacingMediaPolicy.childFacing(catalog.media)
                .filter { !mediaStorage.isVerified(it) }
                .take(count)
            var successCount = 0
            for (asset in unverified) {
                val file = mediaLibrary.downloadChildFacing(asset.mediaId)
                if (file.exists()) {
                    successCount++
                }
            }
            successCount
        } catch (_: Exception) {
            0
        }
    }

    fun getStorageUsedBytes(): Long {
        return mediaStorage.totalStorageUsedBytes()
    }

    fun clearStorage(): Int {
        return mediaStorage.clearDownloadedMedia()
    }
}
