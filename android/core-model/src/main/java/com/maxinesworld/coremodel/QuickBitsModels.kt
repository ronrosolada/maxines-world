package com.maxinesworld.coremodel

import kotlinx.serialization.Serializable

@Serializable
data class QuickBitsCatalog(
    val version: Int = 1,
    val section: String = "Quick Bits",
    val targetAge: String = "8-10",
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val totalSizeMb: Double = 0.0,
    val categories: List<String> = emptyList(),
    val items: List<QuickBitItem> = emptyList(),
)

@Serializable
data class QuickBitItem(
    val id: String,
    val videoId: String = "",
    val title: String,
    val channel: String = "Educational",
    val category: String,
    val durationSeconds: Int,
    val sizeBytes: Long = 0L,
    val sizeMb: Double = 0.0,
    val resolution: String = "854x480",
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val sha256: String? = null,
)
