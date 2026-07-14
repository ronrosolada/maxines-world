package com.maxinesworld.enginesync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.maxinesworld.coremodel.ContentCatalog
import com.maxinesworld.corecontent.ContentVerifier
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

@HiltWorker
class ContentSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val DEFAULT_CATALOG_URL = "https://ronrosolada.github.io/maxines-world-content/production/catalog.json"
        const val UNIQUE_WORK_NAME = "maxines_content_sync"
        private const val CONNECT_TIMEOUT = 15_000L
        private const val READ_TIMEOUT = 60_000L

        fun enqueue(context: Context, catalogUrl: String = DEFAULT_CATALOG_URL) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<ContentSyncWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf("catalogUrl" to catalogUrl))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val catalogUrl = inputData.getString("catalogUrl") ?: DEFAULT_CATALOG_URL
        try {
            // 1. Fetch catalog
            val catalogJson = fetchUrl(catalogUrl)
            val catalog = json.decodeFromString<ContentCatalog>(catalogJson)

            if (catalog.packages.isEmpty()) return@withContext Result.success()

            // 2. Process each package
            catalog.packages.forEach { pkg ->
                if (!RuntimeCapabilities.isCompatible(pkg.minimumAppVersion, 1, emptySet())) {
                    // Skip incompatible packages — don't download
                    return@forEach
                }
                // Check if already installed
                val installedDir = File(applicationContext.filesDir, "content/active/${pkg.packageId}/${pkg.version}")
                if (installedDir.exists() && installedDir.isDirectory) {
                    return@forEach // Already have this version
                }

                // 3. Download to staging
                val stagingDir = File(applicationContext.filesDir, "content/staging").also { it.mkdirs() }
                val stagingFile = File(stagingDir, "${pkg.packageId}-v${pkg.version}.zip")

                downloadFile(pkg.url, stagingFile)

                // 4. Verify checksum
                val checksumResult = ContentVerifier.verifyChecksum(stagingFile, pkg.sha256)
                if (!checksumResult.isSuccess) {
                    stagingFile.delete()
                    return@forEach
                }

                // 5. Verify size
                val sizeResult = ContentVerifier.verifySize(stagingFile, pkg.sizeBytes)
                if (!sizeResult.isSuccess) {
                    stagingFile.delete()
                    return@forEach
                }

                // 6. Extract to active
                val activeDir = File(applicationContext.filesDir, "content/active/${pkg.packageId}/${pkg.version}")
                activeDir.mkdirs()
                ContentVerifier.safeExtract(stagingFile, activeDir)

                // 7. Clean up staging
                stagingFile.delete()
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun fetchUrl(urlStr: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT.toInt()
        conn.readTimeout = READ_TIMEOUT.toInt()
        conn.requestMethod = "GET"
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }

    private fun downloadFile(urlStr: String, dest: File) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT.toInt()
        conn.readTimeout = READ_TIMEOUT.toInt()
        conn.requestMethod = "GET"
        conn.inputStream.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
        conn.disconnect()
    }
}

/** Compatibility check for sync — simplified version that doesn't require activity capabilities. */
private object RuntimeCapabilities {
    fun isCompatible(minAppVersion: Int, schema: Int, caps: Set<String>): Boolean =
        minAppVersion <= 9 && schema == 1
}
