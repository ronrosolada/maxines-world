package com.maxinesworld.engineactivity.renderers

/** Supportive completion copy for ungraded Grade 3 speaking practice. */
internal fun voiceCompletionFeedback(language: String?): String =
    if (language?.startsWith("fil", ignoreCase = true) == true) "Ang galing!" else "Great speaking!"

/** Advances a dialogue without stepping past its final authored turn. */
internal fun nextDialogueTurn(currentTurn: Int, finalTurn: Int): Int =
    (currentTurn + 1).coerceAtMost(finalTurn)
