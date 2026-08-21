package com.maxinesworld.featureparent

import com.maxinesworld.coremodel.localLearningDates
import java.time.LocalDate
import java.time.ZoneId

/**
 * Compatibility adapter for parent-only callers that still consume date
 * strings. Streak calculation itself lives in core-model so child and parent
 * surfaces share the same definition.
 */
internal fun localDatesFromEpochMillis(
    timestamps: List<Long>,
    zone: ZoneId = ZoneId.systemDefault(),
): Set<String> = localLearningDates(timestamps, zone)
    .map(LocalDate::toString)
    .toSet()
