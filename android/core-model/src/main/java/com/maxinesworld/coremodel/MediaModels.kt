package com.maxinesworld.coremodel

import kotlinx.serialization.Serializable

/** Versioned, optional media catalog served independently from lesson content. */
@Serializable
data class MediaCatalog(
    val catalogVersion: Int,
    val generatedAt: String,
    val media: List<MediaAsset> = emptyList(),
)

/** A downloadable, immutable media asset. */
@Serializable
data class MediaAsset(
    val mediaId: String,
    val title: String,
    val file: String,
    val sha256: String,
    val sizeBytes: Long,
    val durationSeconds: Int,
    val width: Int,
    val height: Int,
    val subjectId: String = "mathematics",
    val gradeLevel: Int = 3,
    val quarter: Int = 1,
    val episodeNumber: Int = 1,
    val mimeType: String = "video/mp4",
    val releaseStatus: String = "PREVIEW",
    val licenseStatus: String = "UNKNOWN",
    /** Optional comprehension check; it never gates lesson completion. */
    val assessment: MediaAssessment? = null,
)

/** Only explicitly released curriculum media for a supported subject/grade may enter the video UI. */
fun MediaAsset.isEligibleForCurriculumVideo(): Boolean {
    val normalizedSubject = subjectId
        .lowercase()
        .replace('_', '-')
    val gradeAllowed = when (normalizedSubject) {
        "english", "mathematics", "science" -> gradeLevel in 3..4
        "filipino", "gmrc", "makabansa", "araling-panlipunan" -> gradeLevel in 1..4
        else -> false
    }
    val statusAllowed = releaseStatus.equals("RELEASED", ignoreCase = true) ||
        (releaseStatus.equals("PREVIEW", ignoreCase = true) &&
            licenseStatus.equals("PERSONAL_USE", ignoreCase = true))
    return gradeAllowed && statusAllowed
}

/** A short, child-facing comprehension check attached to one media asset. */
@Serializable
data class MediaAssessment(
    val questionCount: Int,
    val passingCorrectCount: Int,
    val claimsMastery: Boolean = false,
    val items: List<MediaAssessmentItem> = emptyList(),
)

@Serializable
data class MediaAssessmentItem(
    val itemId: String,
    val sequence: Int,
    val type: String = "MULTIPLE_CHOICE",
    val prompt: String,
    val options: List<MediaAssessmentOption>,
    val correctOptionIds: List<String>,
    val explanation: String,
)

@Serializable
data class MediaAssessmentOption(
    val id: String,
    val text: String,
)

/** A lesson-level reference to an optional media asset. */
@Serializable
data class MediaReference(
    val mediaId: String,
    val purpose: String = "supplementary",
    val required: Boolean = false,
    val caption: String = "",
)
