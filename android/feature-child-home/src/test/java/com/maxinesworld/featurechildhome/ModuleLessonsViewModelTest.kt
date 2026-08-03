package com.maxinesworld.featurechildhome

import com.maxinesworld.corecontent.ContentModuleLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModuleLessonsViewModelTest {

    private fun lesson(id: String, day: Int = 1) = ContentModuleLesson(
        lessonId = id, title = "Lesson $id", day = day, estimatedMinutes = 10
    )

    private val lessons = listOf(
        lesson("filipino-g3-q1-w01-d01", day = 1),
        lesson("filipino-g3-q1-w01-d02", day = 2),
        lesson("filipino-g3-q1-w01-d03", day = 3),
    )

    @Test
    fun `nextLessonId returns first incomplete lesson in module order`() {
        assertEquals(
            "filipino-g3-q1-w01-d02",
            nextLessonId(lessons, setOf("filipino-g3-q1-w01-d01"))
        )
    }

    @Test
    fun `nextLessonId is null when everything is complete`() {
        assertNull(nextLessonId(lessons, lessons.map { it.lessonId }.toSet()))
    }

    @Test
    fun `nextLessonId is first lesson when nothing is complete`() {
        assertEquals("filipino-g3-q1-w01-d01", nextLessonId(lessons, emptySet()))
    }

    @Test
    fun `nextLessonId ignores completions outside the module`() {
        assertEquals(
            "filipino-g3-q1-w01-d01",
            nextLessonId(lessons, setOf("mathematics-g3-q1-w01-d01"))
        )
    }

    @Test
    fun `gap in the middle does not skip ahead`() {
        // Completed d01 and d03; d02 is still the resume point (module order wins).
        assertEquals(
            "filipino-g3-q1-w01-d02",
            nextLessonId(lessons, setOf("filipino-g3-q1-w01-d01", "filipino-g3-q1-w01-d03"))
        )
    }

    @Test
    fun `empty lesson list has no next lesson`() {
        assertNull(nextLessonId(emptyList(), emptySet()))
    }

    @Test
    fun `completed count derives from completion set intersection`() {
        val state = ModuleLessonsState(
            isLoading = false,
            lessons = lessons,
            completedLessonIds = setOf("filipino-g3-q1-w01-d01", "outside-lesson"),
            nextLessonId = "filipino-g3-q1-w01-d02"
        )
        assertEquals(2, state.completedCount) // includes IDs the child finished anywhere
        assertEquals(3, state.totalCount)
    }
}
