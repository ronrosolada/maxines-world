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
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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

private const val MAX_QUICK_BITS_BYTES = 100L * 1024L * 1024L
private val QUICK_BITS_HOSTS = setOf("10.10.10.33", "10.10.20.33")
private val QUICK_BITS_ID = Regex("^[A-Za-z0-9][A-Za-z0-9_-]{2,80}$")
private val SHA256 = Regex("^[a-fA-F0-9]{64}$")

internal fun validateQuickBitsItem(item: QuickBitItem): List<String> {
    val errors = mutableListOf<String>()
    if (!item.id.matches(QUICK_BITS_ID)) errors += "invalid id"
    if (item.title.isBlank()) errors += "blank title"
    if (item.durationSeconds !in 1..900) errors += "duration outside 1..900 seconds"
    if (item.sizeBytes !in 1..MAX_QUICK_BITS_BYTES) errors += "size outside 1..100 MiB"

    fun validateUrl(value: String?, extension: String, label: String) {
        if (value == null) return
        val uri = runCatching { URI(value) }.getOrNull()
        val rawPath = uri?.rawPath.orEmpty()
        val decodedPath = runCatching {
            URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name())
        }.getOrNull().orEmpty()
        val pathSegmentsAreSafe = sequenceOf(rawPath, decodedPath).all { path ->
            path.split('/').none { segment -> segment == "." || segment == ".." } &&
                !path.contains("%2e", ignoreCase = true) &&
                !path.contains("%2f", ignoreCase = true) &&
                !path.contains("%5c", ignoreCase = true)
        }
        if (uri == null || uri.scheme !in setOf("http", "https") || uri.host !in QUICK_BITS_HOSTS ||
            uri.query != null || uri.fragment != null || !pathSegmentsAreSafe ||
            !uri.path.startsWith("/quickbits/") || !uri.path.endsWith(extension)
        ) {
            errors += "$label is outside the trusted Quick Bits path"
        }
    }

    validateUrl(item.videoUrl, ".mp4", "video URL")
    validateUrl(item.thumbnailUrl, ".jpg", "thumbnail URL")
    item.sha256?.let { if (!it.matches(SHA256)) errors += "invalid sha256" }
    return errors
}

internal fun validateQuickBitsCatalog(catalog: QuickBitsCatalog): List<String> {
    val errors = catalog.items.flatMap { item ->
        validateQuickBitsItem(item).map { "${item.id}: $it" }
    }.toMutableList()
    if (catalog.totalCount != catalog.items.size) errors += "totalCount does not match items"
    if (catalog.items.map { it.id }.toSet().size != catalog.items.size) errors += "duplicate item id"
    return errors
}

private fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

internal fun isQuickBitsFileValid(file: File, item: QuickBitItem): Boolean {
    if (!file.isFile || file.length() != item.sizeBytes) return false
    return item.sha256?.let { sha256Hex(file) == it.lowercase() } ?: true
}

@Singleton
class QuickBitsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
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
        if (!forceRefresh && cachedCatalog != null) return@withContext cachedCatalog!!
        if (!forceRefresh) {
            val bundled = loadBundledCatalog()
            cachedCatalog = bundled
            return@withContext bundled
        }

        runCatching {
            val request = Request.Builder()
                .url("http://10.10.10.33/quickbits/quickbits_catalog.json")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body?.string().orEmpty()
                val remote = json.decodeFromString<QuickBitsCatalog>(body)
                val errors = validateQuickBitsCatalog(remote)
                if (errors.isNotEmpty()) throw IOException("Invalid Quick Bits catalog: ${errors.take(3).joinToString()}")
                remote
            }
        }.onSuccess {
            cachedCatalog = it
        }.getOrElse {
            cachedCatalog ?: loadBundledCatalog().also { cachedCatalog = it }
        }
    }

    private fun loadBundledCatalog(): QuickBitsCatalog {
        val bundled = context.assets.open("quickbits_catalog.json").use { input ->
            json.decodeFromString<QuickBitsCatalog>(input.bufferedReader().readText())
        }
        val errors = validateQuickBitsCatalog(bundled)
        if (errors.isNotEmpty()) throw IOException("Invalid bundled Quick Bits catalog: ${errors.take(3).joinToString()}")
        return bundled
    }

    fun getLocalFile(item: QuickBitItem): File? {
        val file = File(storageDir, "${item.id}.mp4")
        return if (isQuickBitsFileValid(file, item)) file else null
    }

    fun isDownloaded(item: QuickBitItem): Boolean {
        val file = File(storageDir, "${item.id}.mp4")
        return isQuickBitsFileValid(file, item)
    }

    suspend fun downloadItem(item: QuickBitItem, onProgress: ((bytesDownloaded: Long, totalBytes: Long) -> Unit)? = null): File =
        withContext(Dispatchers.IO) {
            downloadSemaphore.withPermit {
                val itemErrors = validateQuickBitsItem(item)
                if (itemErrors.isNotEmpty()) {
                    throw IOException("Invalid Quick Bits item ${item.id}: ${itemErrors.joinToString()}")
                }
                val targetFile = File(storageDir, "${item.id}.mp4")
                if (isQuickBitsFileValid(targetFile, item)) {
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
                        val expectedBytes = item.sizeBytes
                        if (expectedBytes !in 1..MAX_QUICK_BITS_BYTES) {
                            throw IOException("Invalid expected size for ${item.id}")
                        }
                        if (body.contentLength() > MAX_QUICK_BITS_BYTES) {
                            throw IOException("Quick Bits response is too large for ${item.id}")
                        }

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                var downloaded = 0L

                                while (input.read(buffer).also { read = it } != -1) {
                                    downloaded += read
                                    if (downloaded > expectedBytes || downloaded > MAX_QUICK_BITS_BYTES) {
                                        throw IOException("Quick Bits response exceeded expected size for ${item.id}")
                                    }
                                    output.write(buffer, 0, read)
                                    onProgress?.invoke(downloaded, expectedBytes)
                                }
                            }
                        }

                        if (tempFile.length() != expectedBytes) {
                            tempFile.delete()
                            throw IOException("Downloaded size mismatch for ${item.id}")
                        }
                        item.sha256?.let { expectedHash ->
                            if (sha256Hex(tempFile) != expectedHash.lowercase()) {
                                tempFile.delete()
                                throw IOException("Downloaded hash mismatch for ${item.id}")
                            }
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
