package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Application-facing media facade. The lesson player knows only stable media
 * ids; catalog lookup, network access, verification, and private storage stay
 * behind this class.
 */
class MediaLibrary(
    private val catalogUrl: String,
    private val mediaBaseUrl: String,
    private val catalogClient: MediaCatalogClient,
    private val downloader: MediaDownloader,
    private val storage: MediaStorage,
) {
    private val catalogMutex = Mutex()
    @Volatile
    private var catalog: List<MediaAsset>? = null

    suspend fun download(mediaId: String): File {
        val asset = asset(mediaId)
            ?: throw IllegalArgumentException("Media asset not found in catalog: $mediaId")
        return downloader.download(mediaBaseUrl, asset)
    }

    suspend fun asset(mediaId: String): MediaAsset? =
        loadCatalog().firstOrNull { it.mediaId == mediaId }

    /** Final files are only created by MediaStorage after integrity checks. */
    fun localFile(mediaId: String): File? =
        storage.mediaFile(mediaId).takeIf { it.isFile && it.length() > 0L }

    suspend fun refreshCatalog(): List<MediaAsset> {
        val loaded = loadCatalogWithFallback()
        catalog = loaded.media
        return loaded.media
    }

    private suspend fun loadCatalog(): List<MediaAsset> {
        catalog?.let { return it }
        return catalogMutex.withLock {
            catalog ?: loadCatalogWithFallback().media.also { catalog = it }
        }
    }

    private suspend fun loadCatalogWithFallback() = try {
        val raw = catalogClient.fetchRaw(catalogUrl)
        val fresh = catalogClient.parse(raw)
        // A cache write must never make a healthy online catalog unusable.
        runCatching { storage.writeCatalog(raw) }
        fresh
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        val cached = storage.readCatalog()
            ?.let { raw -> runCatching { catalogClient.parse(raw) }.getOrNull() }
        cached ?: throw error
    }
}
