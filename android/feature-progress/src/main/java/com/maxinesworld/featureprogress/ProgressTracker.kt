package com.maxinesworld.featureprogress

import com.maxinesworld.coremodel.MasteryRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressTracker @Inject constructor() {

    fun getMasterySummary(childId: String): List<MasteryRecord> {
        // To be implemented in M6
        return emptyList()
    }
}
