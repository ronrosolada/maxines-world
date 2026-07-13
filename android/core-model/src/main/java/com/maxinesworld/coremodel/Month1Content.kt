package com.maxinesworld.coremodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A lesson from the Month 1 content package. Fields match the JSON schema exactly. */
@Serializable
data class Month1Lesson(
    val lessonId: String,
    val schemaVersion: Int,
    val grade: Int,
    val month: Int,
    val day: Int,
    val subject: String,
    val title: String,
    val objective: String,
    val estimatedMinutes: Int,
    val educatorValidated: Boolean,
    val releaseStatus: String,
    val qualifiesForDailyBadge: Boolean = true,
    val alignmentStatus: String = "",
    val language: String = "en-PH",
    val introduction: String = "",
    val vocabulary: List<VocabTerm> = emptyList(),
    val activities: List<Month1Activity> = emptyList(),
    val assessment: Month1Assessment? = null
)

@Serializable data class VocabTerm(val term: String, val definition: String)

@Serializable
data class Month1Activity(
    val activityId: String,
    val sequence: Int,
    val type: String,
    val instruction: String,
    val content: kotlinx.serialization.json.JsonElement? = null,
    val required: Boolean = true,
    val completionRule: CompletionRule? = null,
    val feedback: Month1ActivityFeedback? = null,
    val assetId: String? = null,
    @SerialName("accessibilityAlternative") val accessibilityAlternative: String? = null
)

@Serializable data class CompletionRule(val type: String, val targetCount: Int? = null)

@Serializable
data class Month1ActivityFeedback(val correct: String = "", val retry: String = "")

@Serializable
data class Month1Assessment(
    val purpose: String,
    val itemCount: Int,
    val passingCorrectCount: Int,
    val claimsMastery: Boolean = false,
    val items: List<AssessmentItem> = emptyList()
)

@Serializable
data class AssessmentItem(
    val itemId: String = "",
    val sequence: Int = 0,
    val type: String = "",
    val prompt: String = "",
    val options: kotlinx.serialization.json.JsonElement? = null,
    val correctOptionIds: List<String> = emptyList(),
    val explanation: String = ""
)

/** Daily manifest linking 5 qualifying lessons to a badge position. */
@Serializable
data class DayManifest(
    val dayId: String,
    val sequence: Int,
    val badgePosition: Int,
    val qualifyingLessonIds: List<String>,
    val requiredPassedSubjects: Int = 5,
    val passingCorrectCountPerLesson: Int = 4,
    val assessmentItemCountPerLesson: Int = 5,
    val badgeAwardLimit: Int = 1,
    val sameLocalDateRequired: Boolean = true
)

/** Content catalog from the NAS server. */
@Serializable
data class ContentCatalog(
    val catalogVersion: Int,
    val generatedAt: String,
    val packages: List<RemotePackage> = emptyList()
)

@Serializable
data class RemotePackage(
    val packageId: String, val version: Int, val url: String,
    val sha256: String, val sizeBytes: Long, val minimumAppVersion: Int,
    val educatorValidated: Boolean, val releaseStatus: String
)
