package com.maxinesworld.coremodel

import kotlinx.serialization.Serializable

@Serializable
data class SkillGraph(
    val schemaVersion: Int = 1,
    val curriculum: String = "",
    val nodes: List<SkillNode> = emptyList(),
)

@Serializable
data class SkillNode(
    val id: String,
    val subjectId: String,
    val grade: Int,
    val title: String,
    val description: String = "",
    val strand: String = "",
    val depedCode: String = "",
    val prerequisites: List<String> = emptyList(),
    val remediationSkillId: String? = null,
    val equivalentTracks: EquivalentTracks = EquivalentTracks(),
)

@Serializable
data class EquivalentTracks(
    val singaporeMOE: String = "",
    val unitedStates: String = "",
)
