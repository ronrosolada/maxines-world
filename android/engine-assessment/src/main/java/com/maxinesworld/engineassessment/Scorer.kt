package com.maxinesworld.engineassessment

import com.maxinesworld.engineactivity.ActivityResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scorer @Inject constructor() {

    fun computeAccuracy(results: List<ActivityResult>): Double {
        if (results.isEmpty()) return 0.0
        return results.count { it.correct }.toDouble() / results.size
    }

    fun hasPassed(results: List<ActivityResult>, threshold: Double): Boolean {
        return computeAccuracy(results) >= threshold
    }
}
