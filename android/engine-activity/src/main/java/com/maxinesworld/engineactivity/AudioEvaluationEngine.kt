package com.maxinesworld.engineactivity

import java.text.Normalizer
import kotlin.math.max

/** Supportive, offline-only feedback for beginner speaking practice. */
enum class AudioFeedbackState {
    EXCELLENT,
    GOOD_EFFORT,
    TRY_AGAIN,
}

data class AudioEvaluation(
    val state: AudioFeedbackState,
    val message: String,
    /** Speaking practice never traps the child on the current step. */
    val mayContinue: Boolean = true,
    /** Local voice practice is formative and never reduces a score. */
    val penalized: Boolean = false,
)

class AudioEvaluationEngine {
    fun evaluateTranscript(targetPhrase: String, spokenPhrase: String): AudioEvaluation {
        val targetTokens = phoneticTokens(targetPhrase)
        val spokenTokens = phoneticTokens(spokenPhrase)
        if (targetTokens.isEmpty() || spokenTokens.isEmpty()) return feedback(AudioFeedbackState.TRY_AGAIN)

        val distance = levenshtein(targetTokens.joinToString(" "), spokenTokens.joinToString(" "))
        val longest = max(targetTokens.joinToString(" ").length, spokenTokens.joinToString(" ").length)
        val similarity = 1.0 - distance.toDouble() / longest.coerceAtLeast(1)
        return feedback(
            when {
                similarity >= 0.90 -> AudioFeedbackState.EXCELLENT
                similarity >= 0.55 -> AudioFeedbackState.GOOD_EFFORT
                else -> AudioFeedbackState.TRY_AGAIN
            },
        )
    }

    fun evaluateAudio(durationMs: Long, energyEnvelope: List<Double>): AudioEvaluation {
        val activeSamples = energyEnvelope.count { it >= MIN_ACTIVE_ENERGY }
        val activeRatio = activeSamples.toDouble() / energyEnvelope.size.coerceAtLeast(1)
        return feedback(
            when {
                durationMs >= EXCELLENT_DURATION_MS && activeRatio >= 0.5 -> AudioFeedbackState.EXCELLENT
                durationMs >= MINIMUM_DURATION_MS && activeSamples > 0 -> AudioFeedbackState.GOOD_EFFORT
                else -> AudioFeedbackState.TRY_AGAIN
            },
        )
    }

    fun feedback(state: AudioFeedbackState): AudioEvaluation = AudioEvaluation(
        state = state,
        message = when (state) {
            AudioFeedbackState.EXCELLENT -> "Napakagaling!"
            AudioFeedbackState.GOOD_EFFORT -> "Magandang subok! Subukan ulitin"
            AudioFeedbackState.TRY_AGAIN -> "Pakinggan nating muli"
        },
    )

    private fun phoneticTokens(text: String): List<String> = Normalizer
        .normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .trim()
        .split("\\s+".toRegex())
        .filter(String::isNotBlank)
        .map { token ->
            token
                .replace("ph", "f")
                .replace("v", "b")
                .replace("z", "s")
                .replace("d", "t")
                .replace("g", "k")
                .replace("(.)\\1+".toRegex(), "$1")
        }

    private fun levenshtein(left: String, right: String): Int {
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (left[i] == right[j]) 0 else 1,
                )
            }
            previous = current
        }
        return previous[right.length]
    }

    private companion object {
        const val MIN_ACTIVE_ENERGY = 0.05
        const val MINIMUM_DURATION_MS = 500L
        const val EXCELLENT_DURATION_MS = 1_000L
    }
}
