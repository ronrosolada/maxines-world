package com.maxinesworld.engineassessment

import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.AssessmentBlock
import com.maxinesworld.engineactivity.ActivityResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerAssessmentTest {
    private fun item(id: String, correctIndex: Int = 0) =
        ActivityStep(id = id, type = "ASSESSMENT_V1", options = listOf("A", "B"), correctIndex = correctIndex)

    private fun result(id: String, correct: Boolean) =
        ActivityResult(id, correct, attempts = 1, hintsUsed = 0, responseTimeMs = 0)

    @Test
    fun `assessment pass includes exactly threshold and excludes practice`() {
        val assessment = AssessmentBlock(
            passThreshold = 0.75,
            minQuestions = 4,
            items = listOf(item("q1"), item("q2"), item("q3"), item("q4")),
        )
        val results = listOf(
            result("practice", true),
            result("q1", true), result("q2", true), result("q3", true), result("q4", false),
        )

        assertTrue(Scorer().evaluateAssessment(results, assessment).passed)
    }

    @Test
    fun `assessment below threshold fails`() {
        val assessment = AssessmentBlock(
            passThreshold = 0.75,
            minQuestions = 4,
            items = listOf(item("q1"), item("q2"), item("q3"), item("q4")),
        )

        assertFalse(
            Scorer().evaluateAssessment(
                listOf(result("q1", true), result("q2", true), result("q3", false), result("q4", false)),
                assessment,
            ).passed,
        )
    }

    @Test
    fun `missing assessment result fails minimum question requirement`() {
        val assessment = AssessmentBlock(
            passThreshold = 0.0,
            minQuestions = 4,
            items = listOf(item("q1"), item("q2"), item("q3"), item("q4")),
        )

        assertFalse(
            Scorer().evaluateAssessment(
                listOf(result("q1", true), result("q2", true), result("q3", true)),
                assessment,
            ).passed,
        )
    }

    @Test
    fun `invalid correct mapping fails closed`() {
        val assessment = AssessmentBlock(
            passThreshold = 0.0,
            minQuestions = 1,
            items = listOf(item("q1", correctIndex = -1)),
        )

        assertFalse(
            Scorer().evaluateAssessment(listOf(result("q1", true)), assessment).passed,
        )
    }
}
