package com.maxinesworld.coredatabase

/**
 * Pure reward-break rules shared by entitlement creation and route/session code.
 *
 * Entitlements are created once for a child/day, start only when the child
 * chooses a game, and become unusable after consumption or expiry.
 */
object RewardBreakPolicy {
    const val DEFAULT_DURATION_MILLIS: Long = 5 * 60 * 1000L
    const val CREATED = "CREATED"
    const val ACTIVE = "ACTIVE"
    const val CONSUMED = "CONSUMED"

    fun dailyQuestCompletionId(childId: String, dayKey: String): String =
        "$childId:$dayKey"

    fun newEntitlement(
        id: String,
        childId: String,
        dailyQuestCompletionId: String,
        nowEpochMillis: Long,
        durationMillis: Long = DEFAULT_DURATION_MILLIS,
    ): RewardBreakEntitlementEntity = RewardBreakEntitlementEntity(
        id = id,
        childId = childId,
        dailyQuestCompletionId = dailyQuestCompletionId,
        durationMillis = durationMillis,
        remainingMillis = durationMillis,
        createdAtEpochMillis = nowEpochMillis,
        state = CREATED,
    )

    fun remainingAt(
        entitlement: RewardBreakEntitlementEntity,
        nowEpochMillis: Long,
    ): Long {
        if (entitlement.state == CONSUMED) return 0L
        if (entitlement.state != ACTIVE) return entitlement.remainingMillis.coerceAtLeast(0L)

        val startedAt = entitlement.startedAtEpochMillis ?: return entitlement.remainingMillis.coerceAtLeast(0L)
        val elapsed = (nowEpochMillis - startedAt).coerceAtLeast(0L)
        return (entitlement.remainingMillis - elapsed).coerceAtLeast(0L)
    }

    fun canStart(entitlement: RewardBreakEntitlementEntity): Boolean =
        entitlement.state == CREATED && entitlement.remainingMillis > 0L

    fun canUse(
        entitlement: RewardBreakEntitlementEntity,
        nowEpochMillis: Long,
    ): Boolean = when (entitlement.state) {
        CREATED -> entitlement.remainingMillis > 0L
        ACTIVE -> remainingAt(entitlement, nowEpochMillis) > 0L
        else -> false
    }
}
