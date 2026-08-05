package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.engineassessment.AssessmentEvaluation
import com.maxinesworld.engineassessment.Scorer

internal data class LessonCompletionDecision(
    val complete: Boolean,
    val assessmentFailed: Boolean = false,
    val assessment: AssessmentEvaluation? = null,
)

/**
 * Completion is deliberately decided from authored assessment IDs, not from
 * the position of a result in the step list. Practice accuracy never enters
 * the assessment score.
 */
internal fun evaluateLessonCompletion(
    lesson: LessonManifest,
    results: List<ActivityResult>,
    nextStep: Int,
    scorer: Scorer = Scorer(),
): LessonCompletionDecision {
    val requiredIds = lesson.steps.map { it.id }.toSet()
    val completedIds = results.map { it.activityId }.toSet()
    val reachedEnd = nextStep >= lesson.steps.size
    if (!reachedEnd || !completedIds.containsAll(requiredIds)) {
        return LessonCompletionDecision(complete = false)
    }

    val assessment = lesson.assessment ?: return LessonCompletionDecision(complete = true)
    val evaluation = scorer.evaluateAssessment(results, assessment)
    return LessonCompletionDecision(
        complete = evaluation.passed,
        assessmentFailed = !evaluation.passed,
        assessment = evaluation,
    )
}
