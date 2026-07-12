package com.maxinesworld.engineminigame

import java.util.UUID

enum class MiniGameMode { PLAYFUL, LIGHT_REINFORCEMENT, CREATIVE }

data class MiniGameDefinition(
    val id: String,
    val title: String,
    val mode: MiniGameMode,
    val estimatedRoundSeconds: Int,
    val assetBundleId: String,
    val enabled: Boolean = true
) {
    init {
        require(id.isNotBlank())
        require(estimatedRoundSeconds in 20..120)
    }
}

data class MiniGameResult(
    val sessionId: String = UUID.randomUUID().toString(),
    val rewardBreakId: String,
    val gameId: String,
    val childId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val roundsCompleted: Int,
    val correctOrders: Int,
    val pawTokensEarned: Int,
    val collectibleId: String? = null
) {
    init {
        require(endedAtEpochMillis >= startedAtEpochMillis)
        require(roundsCompleted >= 0 && correctOrders >= 0 && pawTokensEarned >= 0)
    }

    /** Stable key: hosts must make result persistence idempotent with this value. */
    val idempotencyKey: String get() = "$rewardBreakId:$gameId"
}

interface MiniGameResultSink {
    /** Persist atomically; ignore a duplicate idempotencyKey. */
    suspend fun save(result: MiniGameResult)
}

class MiniGameRegistry(definitions: List<MiniGameDefinition>) {
    private val byId = definitions.associateBy { it.id }
    init { require(byId.size == definitions.size) { "Duplicate mini-game id" } }
    fun enabled(): List<MiniGameDefinition> = byId.values.filter { it.enabled }
    fun get(id: String): MiniGameDefinition? = byId[id]?.takeIf { it.enabled }
}
