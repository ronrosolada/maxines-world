package com.maxinesworld.corenetwork

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of a self-update attempt.
 */
sealed interface AppUpdateResult {
    /** APK downloaded, SHA-256 verified, and newer than the installed build — safe to install. */
    data class ReadyToInstall(val apkFile: File) : AppUpdateResult

    /** Download succeeded but the candidate was rejected before reaching the installer. */
    data class Rejected(val reason: String) : AppUpdateResult

    /** No endpoint could be reached or the download was unusable. */
    data class Failed(val reason: String) : AppUpdateResult
}

/**
 * Downloads and verifies self-hosted APK updates (DreamNAS LAN / guest-VLAN).
 *
 * Security model:
 *  - The downloaded APK is verified against a **pinned** expected SHA-256 when the caller
 *    supplies one, otherwise against a `<apk-url>.sha256` sidecar served next to the APK.
 *  - If neither checksum source is available the update is REJECTED (fail-closed): plain-HTTP
 *    LAN downloads are MITM-able, so an unverified APK must never reach PackageInstaller.
 *  - A version-code monotonicity check prevents downgrade installs; release versionCode is
 *    git-derived and NOT monotonic across divergent branches.
 *
 * Presentation concerns (progress/status callbacks and the installer handoff) stay out of
 * Compose code so the dashboard screen can remain purely presentational.
 */
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Downloads the update APK from the first reachable endpoint, verifies its integrity,
     * and returns [AppUpdateResult.ReadyToInstall] only for a verified newer build.
     *
     * @param candidateEndpoints absolute APK URLs tried in order until one succeeds.
     * @param pinnedSha256Hex optional trusted SHA-256 of the expected APK. When null, a
     *   `.sha256` sidecar is fetched from the same server instead. When both are unavailable
     *   the result is [AppUpdateResult.Rejected].
     * @param onStatus human-readable progress messages for the UI.
     * @param onProgress fractional download progress in `[0, 1]`.
     */
    suspend fun downloadVerifiedApk(
        candidateEndpoints: List<String>,
        pinnedSha256Hex: String? = null,
        onStatus: suspend (String) -> Unit = {},
        onProgress: suspend (Float) -> Unit = {},
    ): AppUpdateResult = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        var downloadedFile: File? = null
        var servedFromUrl: String? = null

        for (endpoint in candidateEndpoints) {
            try {
                onStatus("Downloading APK from $endpoint...")
                val file = downloadApk(endpoint, onProgress)
                downloadedFile = file
                servedFromUrl = endpoint
                break
            } catch (e: Exception) {
                lastError = e
            }
        }

        val apkFile = downloadedFile
        if (apkFile == null || !apkFile.exists() || apkFile.length() <= 0L) {
            return@withContext AppUpdateResult.Failed(
                "Failed to download update: ${lastError?.localizedMessage ?: "Unknown error"}"
            )
        }

        // ── Integrity verification (fail-closed) ──────────────────────────────────────────
        val actualSha256 = sha256Hex(apkFile)
        val expectedSha256 = pinnedSha256Hex?.trim()?.lowercase()
            ?: fetchSidecarChecksum(servedFromUrl!!, onStatus)

        if (expectedSha256.isNullOrBlank()) {
            apkFile.delete()
            return@withContext AppUpdateResult.Rejected(
                "Update rejected: no checksum available to verify the download. " +
                    "Provide a pinned SHA-256 or host an .sha256 sidecar next to the APK."
            )
        }
        // Sidecars commonly carry "<hex>  <filename>"; compare on the leading hash token.
        val expectedToken = expectedSha256.split(whitespace).firstOrNull()?.lowercase().orEmpty()
        if (!expectedToken.matches(SHA256_PATTERN)) {
            apkFile.delete()
            return@withContext AppUpdateResult.Rejected(
                "Update rejected: checksum is not a 64-character SHA-256 value."
            )
        }
        if (expectedToken != actualSha256) {
            apkFile.delete()
            return@withContext AppUpdateResult.Rejected(
                "Update rejected: SHA-256 mismatch (expected $expectedToken, got $actualSha256)."
            )
        }

        // ── Version monotonicity check ───────────────────────────────────────────────────
        // Reject a stale/tampered/incompatible candidate BEFORE handing it to the installer:
        // versionCode is git-derived and NOT monotonic across divergent release branches,
        // so a newer tag can carry a lower versionCode and a downgrade install would fail
        // or wipe the DB.
        @Suppress("DEPRECATION")
        val installedCode = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrNull() ?: 0
        @Suppress("DEPRECATION")
        val candidate = runCatching {
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        }.getOrNull()

        val candidateCode = candidate?.versionCode ?: Int.MIN_VALUE
        return@withContext when {
            candidate == null -> {
                apkFile.delete()
                AppUpdateResult.Rejected(
                    "Update rejected: APK could not be read (invalid or corrupt package)."
                )
            }
            candidateCode <= installedCode -> {
                apkFile.delete()
                AppUpdateResult.Rejected(
                    "Update rejected: candidate v$candidateCode is not newer than installed v$installedCode."
                )
            }
            else -> AppUpdateResult.ReadyToInstall(apkFile)
        }
    }

    /**
     * Hands a verified APK to the system package installer via FileProvider.
     * Safe to call from any thread; the activity launch is posted to the main looper.
     */
    fun install(apkFile: File) {
        postToMain {
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not launch installer: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Streams [endpoint] into the cache dir, reporting fractional progress. */
    private suspend fun downloadApk(endpoint: String, onProgress: suspend (Float) -> Unit): File {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode} from $endpoint")
            }
            val contentLength = conn.contentLength.toLong()
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val apkFile = File(cacheDir, UPDATE_APK_FILENAME)
            if (apkFile.exists()) apkFile.delete()

            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    var totalBytes = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        totalBytes += read
                        if (contentLength > 0) {
                            onProgress((totalBytes.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            return apkFile
        } finally {
            conn.disconnect()
        }
    }

    /** Fetches `<apk-url>.sha256` from the same host; returns null when unavailable. */
    private suspend fun fetchSidecarChecksum(
        apkUrl: String,
        onStatus: suspend (String) -> Unit,
    ): String? = runCatching {
        withContext(Dispatchers.IO) {
            val url = URL("$apkUrl.sha256")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
            }
            try {
                conn.connect()
                if (conn.responseCode !in 200..299) return@withContext null
                onStatus("Verifying download against server checksum...")
                conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } finally {
                conn.disconnect()
            }
        }
    }.getOrNull()

    private fun postToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else Handler(Looper.getMainLooper()).post(block)
    }

    companion object {
        private const val UPDATE_APK_FILENAME = "maxines_world_update.apk"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val DOWNLOAD_BUFFER_BYTES = 8_192

        private val whitespace = Regex("\\s+")
        private val SHA256_PATTERN = Regex("^[a-f0-9]{64}$")

        /** Lowercase hex SHA-256 of [file], streamed in 8 KiB chunks. */
        fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
