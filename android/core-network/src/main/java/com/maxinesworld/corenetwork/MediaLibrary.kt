package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import java.io.File
import java.io.IOException

/**
 * High-level coordinator for optional lesson media.
 * Supports primary LAN (10.10.10.33) and guest VLAN (10.10.20.33) fallback.
 */
class MediaLibrary(
    private val catalogUrl: String,
    private val mediaBaseUrl: String,
    private val catalogClient: MediaCatalogClient,
    private val downloader: MediaDownloader,
    private val storage: MediaStorage,
) {
    private var cachedCatalog: MediaCatalog? = null
    private var activeBaseUrl: String = mediaBaseUrl

    suspend fun getCatalog(): MediaCatalog {
        cachedCatalog?.let { return it }

        val cachedRaw = storage.readCatalog()
        if (cachedRaw != null) {
            try {
                val parsed = catalogClient.parse(cachedRaw)
                cachedCatalog = parsed
                return parsed
            } catch (_: Exception) {
                // If local cached catalog is invalid or stale, refresh from remote
            }
        }
        return refreshCatalog()
    }

    suspend fun refreshCatalog(): MediaCatalog {
        val candidateUrls = listOf(
            catalogUrl,
            catalogUrl.replace("10.10.10.33", "10.10.20.33")
        ).distinct()

        var lastError: Exception? = null
        for (url in candidateUrls) {
            try {
                val raw = catalogClient.fetchRaw(url)
                val parsed = catalogClient.parse(raw)
                storage.writeCatalog(raw)
                cachedCatalog = parsed
                activeBaseUrl = if (url.contains("10.10.20.33")) "http://10.10.20.33" else "http://10.10.10.33"
                return parsed
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Failed to load media catalog from all candidate endpoints.")
    }

    suspend fun download(mediaId: String): File {
        val catalog = getCatalog()
        val asset = catalog.media.firstOrNull { it.mediaId == mediaId }
            ?: throw IllegalArgumentException("Unknown mediaId: $mediaId")

        val candidateBaseUrls = listOf(
            activeBaseUrl,
            if (activeBaseUrl.contains("10.10.10.33")) "http://10.10.20.33" else "http://10.10.10.33"
        ).distinct()

        var lastError: Exception? = null
        for (base in candidateBaseUrls) {
            try {
                return downloader.download(base, asset)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Failed to download media $mediaId from all endpoints.")
    }

    fun isDownloaded(mediaId: String): Boolean {
        val file = storage.mediaFile(mediaId)
        return file.isFile && file.length() > 0L
    }

    fun localFile(mediaId: String): File? {
        val file = storage.mediaFile(mediaId)
        return if (file.isFile && file.length() > 0L) file else null
    }

    fun isComplete(): Boolean {
        val catalog = cachedCatalog ?: return false
        return catalog.media.all { isDownloaded(it.mediaId) }
    }
}
