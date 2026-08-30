package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.VideoCheckpointDocument
import com.maxinesworld.coremodel.VideoCheckpointItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCheckpointRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cacheMutex = Mutex()
    @Volatile private var cachedDocument: VideoCheckpointDocument? = null

    suspend fun getDocument(): VideoCheckpointDocument = cachedDocument ?: cacheMutex.withLock {
        cachedDocument ?: withContext(Dispatchers.IO) {
            context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
                json.decodeFromString<VideoCheckpointDocument>(reader.readText())
            }
        }.also { cachedDocument = it }
    }

    suspend fun getCheckpoints(mediaId: String): List<VideoCheckpointItem> =
        getDocument().media.firstOrNull { it.mediaId == mediaId }
            ?.checkpoints
            ?.sortedBy { it.positionMs }
            .orEmpty()

    private companion object {
        const val ASSET_PATH = "content-pack/video-checkpoints.json"
    }
}
