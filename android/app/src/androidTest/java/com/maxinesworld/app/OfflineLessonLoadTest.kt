package com.maxinesworld.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.corecontent.ActiveContentIndex
import com.maxinesworld.corecontent.ContentLessonLoader
import com.maxinesworld.corecontent.LessonLoader
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end offline lesson-loading contract (Phase 3):
 * every subject the Playroom can route to must resolve to a REAL lesson
 * from the bundled assets on a fresh install (no server, no synced content).
 */
@RunWith(AndroidJUnit4::class)
class OfflineLessonLoadTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    // All subjects reachable from the Playroom home islands.
    private val supportedSubjects = listOf(
        "english", "filipino", "mathematics", "science",
        "araling-panlipunan", "heritage-harbor", "makabansa", "gmrc"
    )

    @Test
    fun everyPlayroomSubjectLoadsOffline() = runBlocking {
        val activeIndex = ActiveContentIndex(context)
        val contentLoader = ContentLessonLoader(context, activeIndex)
        val legacyLoader = LessonLoader(context, activeIndex)

        val failures = supportedSubjects.mapNotNull { subject ->
            val lessonId = lessonIdForSubject(subject)
            if (lessonId == null) return@mapNotNull "no lessonId for '$subject'"
            val viaContent = contentLoader.loadLesson(lessonId)
            val viaLegacy = legacyLoader.loadLesson(lessonId)
            if (viaContent == null && viaLegacy == null) "$subject ($lessonId)" else null
        }

        assertTrue(
            "Subjects with NO loadable lesson offline: $failures",
            failures.isEmpty()
        )
    }

    @Test
    fun gmrcMapsToRealGmrcLesson() = runBlocking {
        // GMRC content converted from DepEd SLM source — the Kindness island
        // must open a REAL gmrc lesson (not an AP placeholder, not English).
        val activeIndex = ActiveContentIndex(context)
        val contentLoader = ContentLessonLoader(context, activeIndex)
        val legacyLoader = LessonLoader(context, activeIndex)

        val lessonId = lessonIdForSubject("gmrc")!!
        assertTrue("gmrc must map to gmrc content, got: $lessonId", lessonId.startsWith("gmrc"))

        val viaContent = contentLoader.loadLesson(lessonId)
        val viaLegacy = legacyLoader.loadLesson(lessonId)
        assertTrue("gmrc lesson must load offline", viaContent != null || viaLegacy != null)
    }

    /**
     * Assessment-delivery contract (adversarial review): the bundled content-pack
     * assessment blocks must parse into the shape toAssessmentStep() consumes —
     * options as [{id, text}], a keyed correctOptionIds, and an authored
     * explanation — for every subject a child can reach from the Playroom.
     * (Legacy lessons have no playable assessment items; they are out of scope.)
     */
    @Test
    fun everyPlayroomSubjectAssessmentIsConvertible() = runBlocking {
        val activeIndex = ActiveContentIndex(context)
        val contentLoader = ContentLessonLoader(context, activeIndex)

        val failures = supportedSubjects.mapNotNull { subject ->
            val lessonId = lessonIdForSubject(subject) ?: return@mapNotNull null
            val m1 = contentLoader.loadLesson(lessonId) ?: return@mapNotNull null
            val assessment = m1.assessment
            if (assessment == null || assessment.items.isEmpty()) {
                "$subject ($lessonId): no assessment items"
            } else {
                val badItems = assessment.items.filter { item ->
                    val opts = (item.options as? JsonArray)?.mapNotNull { el ->
                        (el as? JsonObject)?.get("text")
                    } ?: emptyList()
                    val ids = (item.options as? JsonArray)?.mapNotNull { el ->
                        (el as? JsonObject)?.get("id")?.jsonPrimitive?.content
                    } ?: emptyList()
                    opts.size < 2 ||
                        item.correctOptionIds.isEmpty() ||
                        item.correctOptionIds.first() !in ids ||
                        item.explanation.isBlank()
                }
                if (badItems.isNotEmpty()) "$subject ($lessonId): ${badItems.size} unconvertible items" else null
            }
        }

        assertTrue("Assessment contract violations: $failures", failures.isEmpty())
    }
}
