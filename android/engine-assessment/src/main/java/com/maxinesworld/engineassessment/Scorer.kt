package com.maxinesworld.engineassessment

import com.maxinesworld.coremodel.AssessmentBlock
import com.maxinesworld.engineactivity.ActivityResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scorer @Inject constructor() {

    fun computeAccuracy(results: List<ActivityResult>): Double {
        if (results.isEmpty()) return 0.0
        return results.count { it.correct }.toDouble() / results.size
    }

    fun hasPassed(results: List<ActivityResult>, threshold: Double): Boolean {
        return computeAccuracy(results) >= threshold
    }

    /**
     * Score only the authored assessment items. Invalid answer-key mappings
     * are a content-integrity failure and can never pass, even with a zero
     * authored threshold.
     */
    fun evaluateAssessment(
        results: List<ActivityResult>,
        assessment: AssessmentBlock,
    ): AssessmentEvaluation {
        val items = assessment.items
        val invalidItem = items.any { it.options.isEmpty() || it.correctIndex !in it.options.indices }
        val assessmentIds = items.map { it.id }.toSet()
        val scoredResults = results
            .asSequence()
            .filter { it.scored && it.activityId in assessmentIds }
            .distinctBy { it.activityId }
            .toList()
        val accuracy = computeAccuracy(scoredResults)
        val passed = !invalidItem &&
            scoredResults.size >= assessment.minQuestions &&
            accuracy >= assessment.passThreshold
        return AssessmentEvaluation(
            answeredQuestions = scoredResults.size,
            accuracy = accuracy,
            passed = passed,
            hasInvalidItem = invalidItem,
        )
    }
}

data class AssessmentEvaluation(
    val answeredQuestions: Int,
    val accuracy: Double,
    val passed: Boolean,
    val hasInvalidItem: Boolean,
)
