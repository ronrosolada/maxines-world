package com.maxinesworld.featurelessonplayer

import kotlin.random.Random

/**
 * Attempt-time multiple-choice helpers.
 *
 * Authored JSON keeps a stable option list for content contracts (#111 length-tells,
 * #113 first-option / slot-position). Runtime display order is a fresh Fisher–Yates
 * shuffle per presentation so a retry cannot memorize "the third button".
 * Scoring is by option id only — never visual index or on-screen letter.
 */
internal fun <T> shuffleMcOptions(
    options: List<T>,
    random: Random = Random.Default,
): List<T> {
    if (options.size <= 1) return options.toList()
    val copy = options.toMutableList()
    for (i in copy.lastIndex downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = copy[i]
        copy[i] = copy[j]
        copy[j] = tmp
    }
    return copy
}

/**
 * Fail closed: a missing, blank, or unpresented id is never correct.
 * A keyed id scores correct in any display slot.
 */
internal fun isKeyedMcChoiceCorrect(
    selectedOptionId: String?,
    presentedOptionIds: List<String>,
    correctOptionIds: List<String>,
): Boolean {
    if (selectedOptionId.isNullOrBlank()) return false
    if (presentedOptionIds.isEmpty()) return false
    if (selectedOptionId !in presentedOptionIds) return false
    return selectedOptionId in correctOptionIds
}

/** Visual slot letter (A, B, C, …) independent of authored option id. */
internal fun mcSlotLabel(index: Int): String = ('A' + index).toString()
