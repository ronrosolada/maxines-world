package com.maxinesworld.coremodel.gamification

import com.maxinesworld.coremodel.CollectibleBadge

/**
 * Pure badge eligibility evaluator. Receives catalog, metadata, attempt facts,
 * and existing ownership — must NOT write to Room or select a different badge.
 */
object WildlifeBadgeEvaluator {

    /**
     * Evaluate whether a wildlife badge should be awarded.
     */
    fun evaluate(
        catalog: List<CollectibleBadge>,
        lessonMetadata: WildlifeDiscoveryMetadata?,
        attemptFacts: AttemptQualification,
        alreadyCollectedBadgeIds: Set<String>,
    ): BadgeEvaluation {
        // 1. No mapping
        if (lessonMetadata == null) return BadgeEvaluation.NoMapping

        // 2. Invalid badge ID
        val badgeId = lessonMetadata.badgeId
        if (catalog.none { it.id == badgeId }) {
            return BadgeEvaluation.InvalidMapping(badgeId)
        }

        // 3. Already collected
        if (badgeId in alreadyCollectedBadgeIds) {
            return BadgeEvaluation.AlreadyCollected(badgeId)
        }

        // 4. Qualifying completion?
        if (!attemptFacts.isQualifying) return BadgeEvaluation.NoMapping

        // 5. Award
        return BadgeEvaluation.Award(
            badgeId = badgeId,
            sourceLessonId = attemptFacts.lessonId,
        )
    }
}

/**
 * Wildlife discovery metadata embedded in a lesson JSON.
 */
@kotlinx.serialization.Serializable
data class WildlifeDiscoveryMetadata(
    val badgeId: String,
    val trigger: String = "QUALIFYING_COMPLETION",
    val factActivityId: String? = null,
)

/**
 * Facts about a lesson attempt used to determine qualification.
 */
data class AttemptQualification(
    val lessonId: String,
    val attemptId: String,
    val requiredSequenceCompleted: Boolean,
    val isPreviewOrDemo: Boolean,
    val isParentTestMode: Boolean,
    val hasScoredInteraction: Boolean,
    val activityTypeIsNonScored: Boolean,
) {
    val isQualifying: Boolean
        get() = attemptId.isNotBlank()
                && lessonId.isNotBlank()
                && requiredSequenceCompleted
                && !isPreviewOrDemo
                && !isParentTestMode
                && (hasScoredInteraction || activityTypeIsNonScored)
}

/**
 * Result of wildlife badge evaluation.
 */
sealed interface BadgeEvaluation {
    /** No wildlife_discovery metadata on this lesson. */
    data object NoMapping : BadgeEvaluation

    /** Lesson references a badge ID not in the catalog. */
    data class InvalidMapping(val badgeId: String) : BadgeEvaluation

    /** Child already owns this badge. */
    data class AlreadyCollected(val badgeId: String) : BadgeEvaluation

    /** Badge should be awarded: (badgeId, source lesson). */
    data class Award(val badgeId: String, val sourceLessonId: String) : BadgeEvaluation
}
