package com.maxinesworld.coremodel

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Returns the trailing run of consecutive local learning dates.
 *
 * The run starts at the most recent date in [localDates], matching the
 * dashboard's existing behavior: an older run separated by a gap does not
 * extend the current trailing run.
 */
fun longestLearningStreak(localDates: Set<LocalDate>): Int {
    if (localDates.isEmpty()) return 0

    var cursor = localDates.maxOrNull() ?: return 0
    var streak = 0
    while (cursor in localDates) {
        streak += 1
        if (cursor == LocalDate.MIN) break
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * Returns the live trailing learning streak for [today].
 *
 * Learning dates after [today] are ignored. A run is live only when its most
 * recent valid date is today or yesterday; otherwise it is stale and returns
 * zero.
 */
fun currentLearningStreak(localDates: Set<LocalDate>, today: LocalDate): Int {
    val datesThroughToday = localDates.filterNot { it.isAfter(today) }.toSet()
    if (datesThroughToday.isEmpty()) return 0

    val yesterday = if (today == LocalDate.MIN) today else today.minusDays(1)
    val mostRecent = datesThroughToday.maxOrNull() ?: return 0
    if (mostRecent.isBefore(yesterday)) return 0

    return longestLearningStreak(datesThroughToday)
}

/**
 * Converts positive epoch-millisecond timestamps to local calendar dates.
 *
 * Non-positive timestamps are invalid progress-record values and are ignored
 * at this raw-data adapter boundary. The streak calculations below consume
 * only validated [LocalDate] values.
 */
fun localLearningDates(timestamps: Iterable<Long>, zone: ZoneId): Set<LocalDate> = timestamps
    .asSequence()
    .filter { it > 0L }
    .mapNotNull { timestamp ->
        runCatching { Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate() }.getOrNull()
    }
    .toSet()
