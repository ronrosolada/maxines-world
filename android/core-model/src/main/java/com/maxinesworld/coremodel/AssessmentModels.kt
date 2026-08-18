package com.maxinesworld.coremodel

import kotlinx.serialization.Serializable

@Serializable
data class AssessmentCatalog(
    val schemaVersion: Int = 1,
    val packs: List<AssessmentPackMetadata> = emptyList(),
)

@Serializable
data class AssessmentPackMetadata(
    val id: String,
    val subjectId: String,
    val curriculum: String, // "ph", "sg", "us"
    val curriculumName: String,
    val flagEmoji: String,
    val title: String,
    val description: String,
    val badgeKey: String,
    val questionCount: Int = 10,
    val passingCount: Int = 8,
    val file: String,
)

@Serializable
data class AssessmentPack(
    val id: String,
    val subjectId: String,
    val curriculum: String,
    val curriculumName: String,
    val flagEmoji: String,
    val title: String,
    val description: String,
    val badgeKey: String,
    val items: List<AssessmentQuestionItem> = emptyList(),
)

@Serializable
data class AssessmentQuestionItem(
    val sequence: Int,
    val prompt: String,
    val options: List<AssessmentQuestionOption> = emptyList(),
    val correctOptionIds: List<String> = emptyList(),
    val explanation: String = "",
)

@Serializable
data class AssessmentQuestionOption(
    val id: String,
    val text: String,
)
