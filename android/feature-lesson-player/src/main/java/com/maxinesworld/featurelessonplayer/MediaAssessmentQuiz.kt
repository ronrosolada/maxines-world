package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAssessmentOption
import kotlin.random.Random

/** Stable quiz state kept independent from the quieter assessment presentation layer. */
data class MediaAssessmentQuizState(
    val mediaId: String,
    val questionIndex: Int = 0,
    val selectedOptionId: String? = null,
    val submitted: Boolean = false,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    val isReplay: Boolean = false,
    /** Per-attempt display order. Empty until the current item is presented. */
    val displayedOptions: List<MediaAssessmentOption> = emptyList(),
)

internal fun beginMediaAssessmentQuiz(
    mediaId: String,
    assessment: MediaAssessment,
    isReplay: Boolean = false,
    random: Random = Random.Default,
): MediaAssessmentQuizState = presentQuizItem(
    MediaAssessmentQuizState(mediaId = mediaId, isReplay = isReplay),
    assessment,
    random,
)

internal fun presentQuizItem(
    state: MediaAssessmentQuizState,
    assessment: MediaAssessment,
    random: Random = Random.Default,
): MediaAssessmentQuizState {
    val item = assessment.items.getOrNull(state.questionIndex) ?: return state.copy(
        displayedOptions = emptyList(),
        selectedOptionId = null,
        submitted = false,
    )
    return state.copy(
        displayedOptions = shuffleMcOptions(item.options, random),
        selectedOptionId = null,
        submitted = false,
    )
}

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
    val correct = isKeyedMcChoiceCorrect(
        selectedOptionId = state.selectedOptionId,
        presentedOptionIds = state.displayedOptions.map { it.id },
        correctOptionIds = item.correctOptionIds,
    )
    return state.copy(
        submitted = true,
        correctCount = state.correctCount + if (correct) 1 else 0,
    )
}

internal fun advanceQuiz(
    state: MediaAssessmentQuizState,
    assessment: MediaAssessment,
    random: Random = Random.Default,
): MediaAssessmentQuizState {
    if (!state.submitted || state.finished) return state
    if (state.questionIndex >= assessment.items.lastIndex) {
        return state.copy(finished = true)
    }
    return presentQuizItem(
        state.copy(
            questionIndex = state.questionIndex + 1,
            selectedOptionId = null,
            submitted = false,
        ),
        assessment,
        random,
    )
}

internal fun restartQuiz(
    state: MediaAssessmentQuizState,
    assessment: MediaAssessment,
    random: Random = Random.Default,
): MediaAssessmentQuizState = presentQuizItem(
    state.copy(
        questionIndex = 0,
        selectedOptionId = null,
        submitted = false,
        correctCount = 0,
        finished = false,
        isReplay = true,
        displayedOptions = emptyList(),
    ),
    assessment,
    random,
)
