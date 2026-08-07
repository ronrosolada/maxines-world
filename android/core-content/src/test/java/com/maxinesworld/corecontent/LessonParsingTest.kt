package com.maxinesworld.corecontent

import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Release-status guard for bundled lesson parsing (spec CH-02): a lesson
 * that is not `RELEASED` must resolve to `null` exactly as if it were
 * missing, so the lesson-load error and retry surface handles it and a
 * partially loaded lesson is never surfaced to the child.
 */
class LessonParsingTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun lessonJson(releaseStatus: String): String = """
        {
          "lessonId": "fixture-g3-m01-d01",
          "schemaVersion": 1,
          "grade": 3,
          "month": 1,
          "day": 1,
          "subject": "english",
          "title": "Fixture lesson",
          "objective": "Learn something",
          "estimatedMinutes": 5,
          "educatorValidated": true,
          "releaseStatus": "$releaseStatus",
          "activities": []
        }
    """.trimIndent()

    @Test
    fun `released lesson loads normally`() {
        assertNotNull(parseBundledLesson(lessonJson("RELEASED"), json))
    }

    @Test
    fun `unreleased lesson resolves to null`() {
        assertNull(parseBundledLesson(lessonJson("REQUIRES_EDUCATOR_REVIEW"), json))
        assertNull(parseBundledLesson(lessonJson(""), json))
    }

    @Test
    fun `malformed lesson resolves to null`() {
        assertNull(parseBundledLesson("{ definitely not json", json))
        assertNull(parseBundledLesson("", json))
    }
}
