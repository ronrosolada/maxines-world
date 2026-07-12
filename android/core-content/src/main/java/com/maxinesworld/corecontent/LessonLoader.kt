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

    /**
     * Load a lesson by its ID (e.g., "eng-g3-m01-l01").
     * Tries multiple paths: old flat path, new subject-based path, and content-pack path.
     */
    fun loadLesson(lessonId: String): LessonManifest? {
        // Path 1: Old flat path — content/ph-matatag/grade-3/{lessonId}.json
        val flatPath = "content/ph-matatag/grade-3/$lessonId.json"
        tryLoad(flatPath)?.let { return it }

        // Path 2: Derived subject path — parse lesson ID to build path
        // e.g., "eng-g3-m01-l01" → "english/module-01/lesson-01.json"
        val derivedPath = deriveSubjectPath(lessonId)
        if (derivedPath != null) {
            tryLoad(derivedPath)?.let { return it }
        }

        // Path 3: Content pack path — content-packs/ph-grade3-v1/lessons/{filename}.json
        // Try with the lesson ID as filename
        val packPath = "content-packs/ph-grade3-v1/lessons/$lessonId.json"
        tryLoad(packPath)?.let { return it }

        return null
    }

    /**
     * Derive a subject-based path from a lesson ID.
     * "eng-g3-m01-l01" → "content/ph-matatag/grade-3/english/module-01/lesson-01.json"
     * "mkb-g3-m01-l01" → "content/ph-matatag/grade-3/makabansa/module-01/lesson-01.json"
     */
    private fun deriveSubjectPath(lessonId: String): String? {
        val parts = lessonId.split("-")
        if (parts.size < 4) return null

        val subjectCode = parts[0]
        val moduleNum = parts[2].removePrefix("m")  // "m01" → "01"
        val lessonNum = parts[3].removePrefix("l")  // "l01" → "01"

        val subjectDir = when (subjectCode) {
            "eng" -> "english"
            "fil" -> "filipino"
            "math" -> "mathematics"
            "sci" -> "science"
            "mkb" -> "makabansa"
            "hist" -> "makabansa"  // legacy
            "gmrc" -> "gmrc"
            else -> return null
        }

        return "content/ph-matatag/grade-3/$subjectDir/module-$moduleNum/lesson-$lessonNum.json"
    }

    fun loadLessonFromFile(filePath: String): LessonManifest? {
        return try {
            val content = java.io.File(filePath).readText()
            json.decodeFromString<LessonManifest>(content)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryLoad(path: String): LessonManifest? {
        return try {
            val stream = context.assets.open(path)
            val content = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<LessonManifest>(content)
        } catch (e: Exception) {
            null
        }
    }
}
