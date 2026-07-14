package com.maxinesworld.coremodel.gamification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ─── Deterministic Identity ───

fun deterministicUuid(source: String): UUID =
    UUID.nameUUIDFromBytes(source.toByteArray(StandardCharsets.UTF_8))

// ─── Completion Command & Receipt ───

data class CompleteAttemptCommand(
    val childId: String,
    val lessonId: String,
    val attemptId: String,
    val questId: String?,
    val completedAt: Instant,
    val localDate: LocalDate,
    val responses: List<ResponseRecord>,
    val previewMode: Boolean,
    val parentTestMode: Boolean,
)

data class ResponseRecord(
    val activityId: String,
    val score: Double,
    val isScored: Boolean,
)

data class CompletionReceipt(
    val attemptId: String,
    val wasAlreadyProcessed: Boolean,
    val lessonStars: Int,
    val fishTreatsGranted: Int,
    val rewardSourceKeys: List<String>,
    val badgesAwarded: List<String>,
    val questCompleted: Boolean,
    val masteryTransitions: List<MasteryTransition>,
)

data class MasteryTransition(
    val skillId: String,
    val from: String,
    val to: String,
)

// ─── Purchase ───

data class PurchaseResult(val itemId: String, val price: Int)

// ─── Grant types (Fish Treat Policy) ───

data class Grant(
    val sourceKey: String,
    val amount: Int,
)
