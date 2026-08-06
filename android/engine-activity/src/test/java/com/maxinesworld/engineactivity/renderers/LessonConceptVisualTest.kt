package com.maxinesworld.engineactivity.renderers

import com.maxinesworld.coremodel.ActivityStep
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonConceptVisualTest {
    private fun step(id: String) = ActivityStep(
        id = id,
        type = "ANIMATED_EXPLANATION_V1",
    )

    @Test
    fun `each learning area receives its own answer-neutral motif`() {
        assertEquals(LessonVisualKind.MATH, lessonVisualKind(step("mathematics-g3-q1-w01-d01-a01")))
        assertEquals(LessonVisualKind.LANGUAGE, lessonVisualKind(step("english-g3-q1-w01-d01-a01")))
        assertEquals(LessonVisualKind.LANGUAGE, lessonVisualKind(step("filipino-g3-q1-w01-d01-a01")))
        assertEquals(LessonVisualKind.SCIENCE, lessonVisualKind(step("science-g3-q1-w01-d01-a01")))
        assertEquals(LessonVisualKind.VALUES, lessonVisualKind(step("gmrc-g3-q1-w01-d01-a01")))
        assertEquals(LessonVisualKind.COMMUNITY, lessonVisualKind(step("makabansa-g3-q1-w01-d01-a01")))
        assertEquals(LessonVisualKind.COMMUNITY, lessonVisualKind(step("araling-panlipunan-g3-m01-d01-a01")))
    }

    @Test
    fun `short legacy math id is supported`() {
        assertEquals(
            LessonVisualKind.MATH,
            lessonVisualKind(step("math-g3-m01-d01-a01")),
        )
    }

    @Test
    fun `unknown content uses the neutral learning trail`() {
        assertEquals(
            LessonVisualKind.GENERAL,
            lessonVisualKind(step("custom-g3-q1-w01-d01-a01")),
        )
    }
}
