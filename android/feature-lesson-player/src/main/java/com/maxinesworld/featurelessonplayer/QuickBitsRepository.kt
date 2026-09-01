package com.maxinesworld.featurelessonplayer

import android.content.Context
import com.maxinesworld.coremodel.QuickBitItem
import com.maxinesworld.coremodel.QuickBitsCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class BulkDownloadProgress(
    val isRunning: Boolean = false,
    val totalVideos: Int = 0,
    val completedVideos: Int = 0,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val currentItemTitle: String? = null,
    val error: String? = null,
)

@Singleton
class QuickBitsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val storageDir: File by lazy {
        File(context.filesDir, "quickbits").apply {
            if (!exists()) mkdirs()
        }
    }

    private val downloadMutex = Mutex()
    private val downloadSemaphore = Semaphore(2)

    private val _bulkProgress = MutableStateFlow(BulkDownloadProgress())
    val bulkProgress: StateFlow<BulkDownloadProgress> = _bulkProgress.asStateFlow()

    private var cachedCatalog: QuickBitsCatalog? = null

    suspend fun getCatalog(forceRefresh: Boolean = false): QuickBitsCatalog = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedCatalog != null) {
            return@withContext cachedCatalog!!
        }

        // Try fetching online catalog from Caddy server first, then fallback to bundled asset
        var catalog: QuickBitsCatalog? = null
        try {
            val request = Request.Builder()
                .url("https://10.10.10.33/quickbits/quickbits_catalog.json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        catalog = json.decodeFromString<QuickBitsCatalog>(body)
                    }
                }
            }
        } catch (_: Exception) {
            // Server might be offline or on different guest VLAN, fallback to asset
        }

        if (catalog == null) {
            try {
                context.assets.open("quickbits_catalog.json").use { input ->
                    val text = input.bufferedReader().readText()
                    catalog = json.decodeFromString<QuickBitsCatalog>(text)
                }
            } catch (e: Exception) {
                throw IOException("Failed to load Quick Bits catalog from server or assets: ${e.message}", e)
            }
        }

        val result = catalog ?: throw IOException("Quick Bits catalog is empty")
        cachedCatalog = result
        result
    }

    fun getLocalFile(item: QuickBitItem): File? {
        val file = File(storageDir, "${item.id}.mp4")
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun isDownloaded(item: QuickBitItem): Boolean {
        val file = File(storageDir, "${item.id}.mp4")
        return file.exists() && file.length() > 0L
    }

    suspend fun downloadItem(item: QuickBitItem, onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null): File =
        withContext(Dispatchers.IO) {
            downloadSemaphore.withPermit {
                val targetFile = File(storageDir, "${item.id}.mp4")
                if (targetFile.exists() && targetFile.length() > 0L) {
                    return@withContext targetFile
                }

                val tempFile = File(storageDir, "${item.id}.mp4.part")
                if (tempFile.exists()) tempFile.delete()

                val request = Request.Builder()
                    .url(item.videoUrl)
                    .build()

                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("Failed to download video ${item.id}: HTTP ${response.code}")
                        }
                        val body = response.body ?: throw IOException("Empty response body for ${item.id}")
                        val expectedBytes = if (item.sizeBytes > 0) item.sizeBytes else body.contentLength()

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                var downloaded = 0L

                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    onProgress?.invoke(downloaded, expectedBytes)
                                }
                            }
                        }

                        if (tempFile.length() <= 0L) {
                            tempFile.delete()
                            throw IOException("Downloaded file is empty for ${item.id}")
                        }

                        if (targetFile.exists()) targetFile.delete()
                        if (!tempFile.renameTo(targetFile)) {
                            throw IOException("Failed to move temporary file to destination for ${item.id}")
                        }
                        targetFile
                    }
                } catch (e: Exception) {
                    if (tempFile.exists()) tempFile.delete()
                    if (e is CancellationException) throw e
                    throw IOException("Download failed for ${item.title}: ${e.message}", e)
                }
            }
        }

    suspend fun downloadAll(items: List<QuickBitItem>) = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val toDownload = items.filter { !isDownloaded(it) }
            if (toDownload.isEmpty()) {
                _bulkProgress.value = BulkDownloadProgress(
                    isRunning = false,
                    totalVideos = items.size,
                    completedVideos = items.size,
                    totalBytes = items.sumOf { it.sizeBytes },
                    downloadedBytes = items.sumOf { it.sizeBytes }
                )
                return@withLock
            }

            val totalBytes = items.sumOf { it.sizeBytes }
            var currentDownloadedBytes = items.filter { isDownloaded(it) }.sumOf { it.sizeBytes }
            var completedCount = items.count { isDownloaded(it) }

            _bulkProgress.value = BulkDownloadProgress(
                isRunning = true,
                totalVideos = items.size,
                completedVideos = completedCount,
                totalBytes = totalBytes,
                downloadedBytes = currentDownloadedBytes,
                currentItemTitle = toDownload.firstOrNull()?.title
            )

            for (item in toDownload) {
                var itemPrevBytes = 0L
                try {
                    _bulkProgress.value = _bulkProgress.value.copy(
                        currentItemTitle = item.title
                    )
                    downloadItem(item) { bytesDownloaded, _ ->
                        val delta = bytesDownloaded - itemPrevBytes
                        itemPrevBytes = bytesDownloaded
                        currentDownloadedBytes += delta
                        _bulkProgress.value = _bulkProgress.value.copy(
                            downloadedBytes = currentDownloadedBytes
                        )
                    }
                    completedCount++
                    _bulkProgress.value = _bulkProgress.value.copy(
                        completedVideos = completedCount
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _bulkProgress.value = _bulkProgress.value.copy(
                        error = "Failed downloading: ${item.title}"
                    )
                }
            }

            _bulkProgress.value = _bulkProgress.value.copy(
                isRunning = false,
                currentItemTitle = null
            )
        }
    }

    suspend fun clearAllDownloads(): Boolean = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            var allSuccess = true
            storageDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.delete()) {
                    allSuccess = false
                }
            }
            _bulkProgress.value = BulkDownloadProgress()
            allSuccess
        }
    }

    fun getStorageMetrics(): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L
        storageDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".mp4") && file.length() > 0L) {
                count++
                totalBytes += file.length()
            }
        }
        return Pair(count, totalBytes)
    }
}
