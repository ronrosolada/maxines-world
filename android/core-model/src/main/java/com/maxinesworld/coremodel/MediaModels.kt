package com.maxinesworld.coremodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoCheckpointDocument(
    val schemaVersion: Int,
    val media: List<MediaCheckpoints> = emptyList(),
)

@Serializable
data class MediaCheckpoints(
    val mediaId: String,
    val checkpoints: List<VideoCheckpointItem> = emptyList(),
)

@Serializable
data class VideoCheckpointItem(
    val checkpointId: String,
    val positionMs: Long,
    val type: CheckpointType,
    val prompt: String,
    val options: List<CheckpointOption> = emptyList(),
    val correctOptionId: String,
    val feedbackLadder: CheckpointFeedbackLadder,
)

@Serializable
data class CheckpointOption(val id: String, val text: String)

@Serializable
data class CheckpointFeedbackLadder(
    @SerialName("hint1_clue") val hint1Clue: String,
    @SerialName("hint2_worked_example") val hint2WorkedExample: String,
    @SerialName("hint3_prereq_subquestion") val hint3PrereqSubquestion: String,
)

@Serializable
enum class CheckpointType { PREDICTION, QUICK_CHECK, HANDS_ON_PAPER, VOCAB_SPOTLIGHT }

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
