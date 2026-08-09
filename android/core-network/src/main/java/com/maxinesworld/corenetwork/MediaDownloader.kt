package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Downloads one media asset with resumable HTTP and post-download verification. */
class MediaDownloader(
    private val client: OkHttpClient,
    private val storage: MediaStorage,
) {
    suspend fun download(baseUrl: String, asset: MediaAsset): File = withContext(Dispatchers.IO) {
        if (storage.isVerified(asset)) return@withContext storage.mediaFile(asset)

        val destination = storage.mediaFile(asset)
        if (destination.exists()) destination.delete()

        val partial = storage.partialFile(asset)
        val existingBytes = partial.takeIf { it.isFile }?.length() ?: 0L
        val canResume = existingBytes in 1 until asset.sizeBytes
        val request = Request.Builder()
            .url(assetUrl(baseUrl, asset))
            .apply {
                if (canResume) header("Range", "bytes=$existingBytes-")
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Media download failed for ${asset.mediaId}: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Media response had no body: ${asset.mediaId}")
            val append = canResume && response.code == 206 && validContentRange(
                response.header("Content-Range"),
                existingBytes,
            )
            val startingBytes = if (append) existingBytes else 0L
            if (!append && partial.exists()) partial.delete()

            var totalBytes = startingBytes
            body.byteStream().use { input ->
                FileOutputStream(partial, append).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > asset.sizeBytes) {
                            throw IOException(
                                "Media response exceeded catalog size for ${asset.mediaId}",
                            )
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
        }

        try {
            storage.installVerified(partial, asset)
        } catch (error: IllegalArgumentException) {
            // A completed but corrupt file cannot be resumed safely.
            partial.delete()
            throw error
        }
    }

    private fun assetUrl(baseUrl: String, asset: MediaAsset): String =
        "${baseUrl.trimEnd('/')}/${asset.file}"

    private fun validContentRange(header: String?, expectedStart: Long): Boolean =
        header?.startsWith("bytes $expectedStart-") == true
}
