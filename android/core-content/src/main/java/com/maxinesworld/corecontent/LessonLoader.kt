package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.LessonManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadLesson(lessonId: String): LessonManifest? {
        val path = "content/ph-matatag/grade-3/$lessonId.json"
        return try {
            val stream = context.assets.open(path)
            val content = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<LessonManifest>(content)
        } catch (e: Exception) {
            null
        }
    }

    fun loadLessonFromFile(filePath: String): LessonManifest? {
        return try {
            val content = java.io.File(filePath).readText()
            json.decodeFromString<LessonManifest>(content)
        } catch (e: Exception) {
            null
        }
    }
}
