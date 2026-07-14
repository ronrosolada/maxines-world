package com.maxinesworld.playground

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface LocalDayProvider {
    fun currentDayKey(): String
}

@Singleton
class SystemLocalDayProvider @Inject constructor(
    private val clock: Clock,
    private val zoneId: ZoneId,
) : LocalDayProvider {
    override fun currentDayKey(): String = LocalDate.now(clock.withZone(zoneId)).toString()
}
