package com.maxinesworld.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonRouteTest {

    private fun lessonsDir(): File {
        val candidates = listOf(
            File("src/main/assets/content-pack/month-01/lessons"),
            File("app/src/main/assets/content-pack/month-01/lessons"),
            File("android/app/src/main/assets/content-pack/month-01/lessons")
        )
        return candidates.first { it.isDirectory }
    }

    private val subjects = listOf(
        "english", "filipino", "mathematics", "science",
        "araling-panlipunan", "philippine-history", "makabansa",
        "heritage-harbor", "gmrc", "unknown-subject"
    )

    @Test
    fun `every village destination routes to a lesson file that exists`() {
        val dir = lessonsDir()
        val broken = subjects.filter { subject ->
            !File(dir, "${lessonIdForSubject(subject)}.json").isFile
        }
        assertTrue("Subjects routing to nonexistent lessons: $broken", broken.isEmpty())
    }
}
