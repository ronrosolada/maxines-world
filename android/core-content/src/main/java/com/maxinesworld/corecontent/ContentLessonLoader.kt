package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.DayManifest
import com.maxinesworld.coremodel.Month1Lesson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentLessonLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val lessonCache = ConcurrentHashMap<String, Month1Lesson>()
    private val manifestCache = ConcurrentHashMap<Int, DayManifest>()

    suspend fun loadLesson(lessonId: String): Month1Lesson? = withContext(Dispatchers.IO) {
        lessonCache.getOrPut(lessonId) {
            runCatching {
                // Try multiple path patterns for different lesson ID formats
                val raw = tryPath(lessonId)
                    ?: return@runCatching null
                json.decodeFromString<Month1Lesson>(raw)
            }.getOrNull()
        }
    }

    /** Try to load a lesson JSON from assets, trying multiple path formats. */
    private fun tryPath(lessonId: String): String? {
        // Format 1: Month-based (month-01) — english-g3-m01-d01
        try {
            return context.assets.open("content-pack/month-01/lessons/$lessonId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {}

        // Format 2: Quarter-week-based — e.g., english-g3-q2-w01-d01
        // Path: content-pack/grade-3/lessons/english-g3-q2-w01-d01.json
        try {
            return context.assets.open("content-pack/grade-3/lessons/$lessonId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {}

        // Format 3: Legacy ph-matatag path
        try {
            return context.assets.open("content/ph-matatag/grade-3/$lessonId.json")
                .bufferedReader().use { it.readText() }
        } catch (_: Exception) {}

        return null
    }

    suspend fun loadDayManifest(day: Int): DayManifest? = withContext(Dispatchers.IO) {
        require(day in 1..20)
        manifestCache.getOrPut(day) {
            runCatching {
                val padded = day.toString().padStart(2, '0')
                val raw = context.assets.open("content-pack/month-01/days/day-$padded.json")
                    .bufferedReader().use { it.readText() }
                json.decodeFromString<DayManifest>(raw)
            }.getOrNull()
        }
    }

    fun getAssetPath(assetId: String): String =
        "content-pack/month-01/assets/vectors/$assetId.svg"

    val subjectMapping = mapOf(
        "ENGLISH" to "english",
        "FILIPINO" to "filipino",
        "MATHEMATICS" to "mathematics",
        "SCIENCE" to "science",
        "ARALING_PANLIPUNAN" to "makabansa"
    )

    fun toAppSubject(apiSubject: String): String = subjectMapping[apiSubject] ?: apiSubject.lowercase()
}
