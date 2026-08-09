package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Fetches the small media catalog; media bytes are downloaded separately. */
class MediaCatalogClient(
    private val client: OkHttpClient,
    private val parser: MediaCatalogParser = MediaCatalogParser(),
) {
    suspend fun fetch(url: String): MediaCatalog = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Media catalog request failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Media catalog response had no body")
            parser.parse(body.string())
        }
    }
}
