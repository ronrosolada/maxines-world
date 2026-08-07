package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.AssessmentBlock
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.engineactivity.ActivityResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCompletionGateTest {
    private val lesson = LessonManifest(
        id = "lesson-1",
        schemaVersion = 1,
        subject = "english",
        moduleId = "module-1",
        title = "A lesson",
        objective = "Learn",
        guideCharacter = "Milo",
        estimatedMinutes = 5,
        steps = listOf(
            ActivityStep(id = "practice-1", type = "MULTIPLE_CHOICE_V1"),
            ActivityStep(id = "assessment-q1", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
            ActivityStep(id = "assessment-q2", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
            ActivityStep(id = "assessment-q3", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
            ActivityStep(id = "assessment-q4", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
        ),
        assessment = AssessmentBlock(
            passThreshold = 0.75,
            minQuestions = 4,
            items = listOf(
                ActivityStep(id = "assessment-q1", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
                ActivityStep(id = "assessment-q2", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
                ActivityStep(id = "assessment-q3", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
                ActivityStep(id = "assessment-q4", type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = 0),
            ),
        ),
    )

    private fun result(id: String, correct: Boolean) =
        ActivityResult(id, correct, attempts = 1, hintsUsed = 0, responseTimeMs = 0)

    @Test
    fun `assessment passes exactly at authored threshold`() {
        val results = listOf(
            result("practice-1", correct = false),
            result("assessment-q1", correct = true),
            result("assessment-q2", correct = true),
            result("assessment-q3", correct = true),
            result("assessment-q4", correct = false),
        )

        assertTrue(evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size).complete)
    }

    @Test
    fun `assessment fails immediately below authored threshold`() {
        val results = listOf(
            result("practice-1", correct = true),
            result("assessment-q1", correct = true),
            result("assessment-q2", correct = true),
            result("assessment-q3", correct = false),
            result("assessment-q4", correct = false),
        )

        assertFalse(evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size).complete)
    }

    @Test
    fun `missing required assessment result cannot complete even with perfect accuracy`() {
        val results = listOf(
            result("practice-1", correct = true),
            result("assessment-q1", correct = true),
            result("assessment-q2", correct = true),
            result("assessment-q3", correct = true),
        )

        assertFalse(evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size).complete)
    }

    @Test
    fun `practice correctness cannot compensate for failed assessment`() {
        val results = listOf(
            result("practice-1", correct = true),
            result("assessment-q1", correct = false),
            result("assessment-q2", correct = false),
            result("assessment-q3", correct = false),
            result("assessment-q4", correct = false),
        )

        assertFalse(evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size).complete)
    }

    @Test
    fun `practice failure does not prevent an independently passing assessment`() {
        val results = listOf(
            result("practice-1", correct = false),
            result("assessment-q1", correct = true),
            result("assessment-q2", correct = true),
            result("assessment-q3", correct = true),
            result("assessment-q4", correct = false),
        )

        assertTrue(evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size).complete)
    }

    @Test
    fun `practice results are excluded from the assessment verdict`() {
        val results = listOf(
            result("practice-1", correct = false).copy(scored = false),
            result("assessment-q1", correct = true).copy(scored = true),
            result("assessment-q2", correct = true).copy(scored = true),
            result("assessment-q3", correct = true).copy(scored = true),
            result("assessment-q4", correct = false).copy(scored = true),
        )

        // 3/4 = 0.75 is exactly at the authored threshold; the incorrect
        // practice answer must not drag the verdict down (spec CH-04).
        val decision = evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size)
        assertTrue(decision.complete)
        assertEquals(0.75, decision.assessment?.accuracy ?: -1.0, 0.001)
    }

    @Test
    fun `practice results alone yield no assessment accuracy or mastery`() {
        val results = listOf(
            result("practice-1", correct = true).copy(scored = false),
        )
        val decision = evaluateLessonCompletion(lesson, results, nextStep = lesson.steps.size)
        // Practice-only results can never complete a lesson: there is no
        // assessment verdict at all, so no accuracy and no mastery signal
        // can be derived (spec CH-04).
        assertFalse(decision.complete)
        assertNull(decision.assessment)
    }

    @Test
    fun `invalid assessment mapping fails closed`() {
        val invalidAssessment = lesson.assessment!!
        val invalidLesson = lesson.copy(
            assessment = invalidAssessment.copy(
                items = invalidAssessment.items.map { item ->
                    if (item.id == "assessment-q4") item.copy(correctIndex = -1) else item
                },
            ),
            steps = lesson.steps.map { step ->
                if (step.id == "assessment-q4") step.copy(correctIndex = -1) else step
            },
        )
        val results = listOf(
            result("practice-1", correct = true),
            result("assessment-q1", correct = true),
            result("assessment-q2", correct = true),
            result("assessment-q3", correct = true),
            result("assessment-q4", correct = true),
        )

        assertFalse(evaluateLessonCompletion(invalidLesson, results, nextStep = invalidLesson.steps.size).complete)
    }

    @Test
    fun `lesson without assessment keeps existing all-steps completion behavior`() {
        val practiceOnly = lesson.copy(
            assessment = null,
            steps = listOf(ActivityStep(id = "practice-1", type = "MULTIPLE_CHOICE_V1")),
        )

        assertTrue(
            evaluateLessonCompletion(
                practiceOnly,
                listOf(result("practice-1", correct = false)),
                nextStep = 1,
            ).complete,
        )
    }
}
