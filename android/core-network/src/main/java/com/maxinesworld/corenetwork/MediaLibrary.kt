package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.ChildFacingMediaPolicy
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

/**
 * High-level coordinator for optional lesson media.
 * Supports primary LAN (10.10.10.33) and guest VLAN (10.10.20.33) fallback.
 * Implements local-first cached catalog access and fast local file verification.
 */
class MediaLibrary(
    private val catalogUrl: String,
    private val mediaBaseUrl: String,
    private val catalogClient: MediaCatalogClient,
    private val downloader: MediaDownloader,
    private val storage: MediaStorage,
) {
    @Volatile
    private var cachedCatalog: MediaCatalog? = null
    private var activeBaseUrl: String = mediaBaseUrl
    private val verifiedLocalFiles = ConcurrentHashMap<String, File>()

    /**
     * Fast local-first catalog retrieval: returns in-memory cached catalog immediately,
     * or reads and parses persisted disk catalog without network delays.
     */
    suspend fun getCatalog(): MediaCatalog {
        cachedCatalog?.let { return it }

        val cachedRaw = storage.readCatalog()
        if (cachedRaw != null) {
            try {
                val parsed = catalogClient.parse(cachedRaw)
                cachedCatalog = parsed
                return parsed
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // If local cached catalog is invalid or unparseable, fall through to remote refresh
            }
        }
        return refreshCatalog()
    }

    /**
     * Synchronous / non-blocking check to retrieve cached catalog if already available.
     */
    fun getCachedCatalog(): MediaCatalog? {
        if (cachedCatalog != null) return cachedCatalog
        val cachedRaw = storage.readCatalog() ?: return null
        return try {
            val parsed = catalogClient.parse(cachedRaw)
            cachedCatalog = parsed
            parsed
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshCatalog(): MediaCatalog {
        val candidateUrls = if (catalogUrl.contains("10.10.10.33")) {
            listOf(catalogUrl, catalogUrl.replace("10.10.10.33", "10.10.20.33")).distinct()
        } else {
            listOf(catalogUrl)
        }

        var lastError: Exception? = null
        for (url in candidateUrls) {
            try {
                val raw = catalogClient.fetchRaw(url)
                val parsed = catalogClient.parse(raw)
                runCatching { storage.writeCatalog(raw) }
                cachedCatalog = parsed
                activeBaseUrl = if (url.contains("10.10.20.33")) {
                    mediaBaseUrl.replace("10.10.10.33", "10.10.20.33")
                } else {
                    mediaBaseUrl
                }
                return parsed
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
            }
        }

        // Fallback to persisted disk catalog if remote is unreachable
        val cachedRaw = storage.readCatalog()
        if (cachedRaw != null) {
            try {
                val parsed = catalogClient.parse(cachedRaw)
                cachedCatalog = parsed
                return parsed
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }

        throw lastError ?: IOException("Failed to load media catalog from all candidate endpoints.")
    }

    /**
     * Child library, quest prefetch, and other child-facing playback paths
     * must use this entry point so PREVIEW / other-grade rows cannot be
     * fetched as curriculum media. [download] stays available for parent
     * review of the full LAN catalog.
     */
    suspend fun downloadChildFacing(mediaId: String): File {
        val catalog = getCatalog()
        val asset = catalog.media.firstOrNull { it.mediaId == mediaId }
            ?: throw IllegalArgumentException("Unknown mediaId: $mediaId")
        if (!ChildFacingMediaPolicy.isChildFacingCurriculum(asset)) {
            throw IllegalArgumentException("Media is not child-facing curriculum: $mediaId")
        }
        return download(mediaId)
    }

    suspend fun download(mediaId: String): File {
        val catalog = getCatalog()
        val asset = catalog.media.firstOrNull { it.mediaId == mediaId }
            ?: throw IllegalArgumentException("Unknown mediaId: $mediaId")

        val candidateBaseUrls = if (activeBaseUrl.contains("10.10.10.33")) {
            listOf(activeBaseUrl, activeBaseUrl.replace("10.10.10.33", "10.10.20.33")).distinct()
        } else {
            listOf(activeBaseUrl)
        }

        var lastError: Exception? = null
        for (base in candidateBaseUrls) {
            try {
                val file = downloader.download(base, asset)
                verifiedLocalFiles[mediaId] = file
                return file
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e
            }
        }
        throw lastError ?: IOException("Failed to download media $mediaId from all endpoints.")
    }

    fun isDownloaded(mediaId: String): Boolean {
        return localFile(mediaId) != null
    }

    fun localFile(mediaId: String): File? {
        verifiedLocalFiles[mediaId]?.let {
            if (it.isFile && it.length() > 0L) return it
            verifiedLocalFiles.remove(mediaId)
        }
        val file = storage.mediaFile(mediaId)
        return if (file.isFile && file.length() > 0L) {
            verifiedLocalFiles[mediaId] = file
            file
        } else null
    }

    fun isComplete(): Boolean {
        val catalog = cachedCatalog ?: return false
        return catalog.media.all { isDownloaded(it.mediaId) }
    }
}
