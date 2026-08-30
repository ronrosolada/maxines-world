package com.maxinesworld.featurelessonplayer

internal data class ArenaCopy(
    val isFilipino: Boolean,
    val correctHeader: String,
    val clueHeader: String,
    val correctTtsPrefix: String,
    val clueTtsPrefix: String,
    val hintFallback: String,
    val checkAnswer: String,
    val nextQuestion: String,
    val finishQuiz: String,
    val retry: String,
    val reviewClues: String,
    val ttsLanguage: String,
)

internal fun isFilipinoArena(packIdOrLanguage: String?): Boolean {
    val value = packIdOrLanguage.orEmpty().lowercase()
    return listOf("filipino", "makabansa", "gmrc", "fil-ph", "tagalog").any(value::contains)
}

internal fun arenaCopy(packIdOrLanguage: String?): ArenaCopy {
    val filipino = isFilipinoArena(packIdOrLanguage)
    return if (filipino) ArenaCopy(
        true, "Tama! Napakagaling!", "Pahiwatig ni Milo:", "Tama! ", "Pahiwatig ni Milo: ",
        "Basahing mabuti ang bawat pagpipilian at alisin ang mga hindi tumutugma sa tanong.",
        "Tingnan ang Sagot", "Susunod na tanong", "Tapusin ang pagsusulit", "Muling subukan",
        "Balikan ang mga pahiwatig", "fil-PH",
    ) else ArenaCopy(
        false, "Correct! Awesome job!", "Milo's learning clue:", "Correct! ", "Milo's Learning Clue: ",
        "Read each choice carefully and eliminate options that do not match the question requirements!",
        "Check Answer", "Next question", "Finish quiz", "Try Again", "Review clues", "en-US",
    )
}

internal enum class ArenaSoundEffect { CORRECT, ENCOURAGEMENT, CELEBRATION }
internal fun arenaAnswerSound(isSubmitted: Boolean, isCorrect: Boolean): ArenaSoundEffect? =
    if (!isSubmitted) null else if (isCorrect) ArenaSoundEffect.CORRECT else ArenaSoundEffect.ENCOURAGEMENT

internal data class ArenaCompletionState(
    val message: String,
    val showRetry: Boolean,
    val showReviewClues: Boolean,
    val showMasteryRewards: Boolean,
    val soundEffect: ArenaSoundEffect,
)

internal fun arenaCompletionState(isPassed: Boolean, isFilipino: Boolean) = ArenaCompletionState(
    message = when {
        isPassed && isFilipino -> "Napakagaling! Ganap mo nang natutuhan ang hamon!"
        isPassed -> "Amazing work! You mastered this challenge!"
        isFilipino -> "Magaling na pagsisikap! Sa bawat subok, lalo kang gumagaling!"
        else -> "Great effort! Every try makes you stronger!"
    },
    showRetry = !isPassed,
    showReviewClues = !isPassed,
    showMasteryRewards = isPassed,
    soundEffect = if (isPassed) ArenaSoundEffect.CELEBRATION else ArenaSoundEffect.ENCOURAGEMENT,
)

internal fun sanctuaryTokensDescription(language: String?) =
    if (isFilipinoArena(language)) "Mga token ng santuwaryo" else "Sanctuary tokens"

internal fun sanctuaryGainedDescription(language: String?, piece: String) =
    if (isFilipinoArena(language)) "Nagkaroon ng bagong gamit ang santuwaryo ni Milo: $piece"
    else "Milo's sanctuary gained: $piece"
