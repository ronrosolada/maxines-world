package com.maxinesworld.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.corecontent.ActiveContentIndex
import com.maxinesworld.corecontent.ContentLessonLoader
import com.maxinesworld.corecontent.LessonLoader
import kotlinx.coroutines.runBlocking
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
        "araling-panlipunan", "heritage-harbor", "gmrc"
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
    fun gmrcRoutesToPlayableLessonPerKnownGapPolicy() = runBlocking {
        // KNOWN GAP (documented in MaxinesNavGraph): no playable GMRC content
        // exists yet, so gmrc routes to an AP lesson. The guarantee we test:
        // it must load SOMETHING playable offline and never silently open English.
        val activeIndex = ActiveContentIndex(context)
        val contentLoader = ContentLessonLoader(context, activeIndex)
        val legacyLoader = LessonLoader(context, activeIndex)

        val lessonId = lessonIdForSubject("gmrc")!!
        assertTrue("gmrc must not silently map to English", lessonId != "english-g3-m01-d01")

        val viaContent = contentLoader.loadLesson(lessonId)
        val viaLegacy = legacyLoader.loadLesson(lessonId)
        assertTrue("gmrc-mapped lesson must load offline", viaContent != null || viaLegacy != null)
    }
}
