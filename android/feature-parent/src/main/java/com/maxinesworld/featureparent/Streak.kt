package com.maxinesworld.featureparent

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Longest run of consecutive days (in the caller's local timezone) with at
 * least one learning event. Counts backwards from the most recent day, so a
 * gap before today's run does not extend it.
 *
 * @param localDates distinct local dates as "YYYY-MM-DD" (deduplicated).
 * @return 0 when empty; otherwise the length of the trailing consecutive run.
 */
internal fun longestStreak(localDates: Set<String>): Int {
    if (localDates.isEmpty()) return 0
    val days = localDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (days.isEmpty()) return 0
    val distinct = days.toSet()
    var cursor = distinct.max()
    var streak = 0
    while (cursor in distinct) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * Local-date strings for a list of epoch-millis timestamps, in the device's
 * current timezone. SQL-side date bucketing is deliberately avoided — it
 * applies UTC, which shifts early-morning (pre-8am Manila) completions to
 * the previous day.
 */
internal fun localDatesFromEpochMillis(
    timestamps: List<Long>,
    zone: ZoneId = ZoneId.systemDefault()
): Set<String> = timestamps
    .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toString() }
    .toSet()
