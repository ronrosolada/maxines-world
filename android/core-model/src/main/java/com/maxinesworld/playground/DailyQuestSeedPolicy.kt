package com.maxinesworld.playground

import java.security.MessageDigest

object DailyQuestSeedPolicy {
    const val QUESTS_PER_DAY = 3

    val candidates: List<String> = listOf(
        "subject:english",
        "subject:filipino",
        "subject:mathematics",
        "subject:science",
        "subject:makabansa",
    )

    fun assign(childId: String, dayKey: String): List<String> =
        candidates
            .sortedBy { sha256("$childId\u001f$dayKey\u001f$it") }
            .take(QUESTS_PER_DAY)
            .sorted()

    fun questSetHash(ids: Collection<String>): String =
        sha256(ids.toSortedSet().joinToString("\u001f"))

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
