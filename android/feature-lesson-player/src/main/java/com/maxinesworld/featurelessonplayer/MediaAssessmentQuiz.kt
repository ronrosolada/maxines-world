package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.MediaAssessment

data class MediaAssessmentQuizState(
    val mediaId: String,
    val questionIndex: Int = 0,
    val selectedOptionId: String? = null,
    val submitted: Boolean = false,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    val isReplay: Boolean = false,
)

internal fun selectQuizOption(
    state: MediaAssessmentQuizState,
    optionId: String,
): MediaAssessmentQuizState {
    if (state.submitted || state.finished) return state
    return state.copy(selectedOptionId = optionId)
}

internal fun submitQuizAnswer(
    state: MediaAssessmentQuizState,
    assessment: MediaAssessment,
): MediaAssessmentQuizState {
    if (state.submitted || state.finished || state.selectedOptionId == null) return state
    val item = assessment.items.getOrNull(state.questionIndex) ?: return state
    val correct = state.selectedOptionId in item.correctOptionIds
    return state.copy(
        submitted = true,
        correctCount = state.correctCount + if (correct) 1 else 0,
    )
}

internal fun advanceQuiz(
    state: MediaAssessmentQuizState,
    assessment: MediaAssessment,
): MediaAssessmentQuizState {
    if (!state.submitted || state.finished) return state
    if (state.questionIndex >= assessment.items.lastIndex) {
        return state.copy(finished = true)
    }
    return state.copy(
        questionIndex = state.questionIndex + 1,
        selectedOptionId = null,
        submitted = false,
    )
}

internal fun restartQuiz(state: MediaAssessmentQuizState): MediaAssessmentQuizState =
    state.copy(
        questionIndex = 0,
        selectedOptionId = null,
        submitted = false,
        correctCount = 0,
        finished = false,
        isReplay = true,
    )
