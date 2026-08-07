package com.maxinesworld.featurelessonplayer

import com.maxinesworld.engineactivity.ActivityResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityResultTest {
    @Test
    fun `later successful result replaces an earlier retry result`() {
        val failed = ActivityResult("match-1", correct = false, attempts = 6, hintsUsed = 0, responseTimeMs = 100)
        val completed = ActivityResult("match-1", correct = true, attempts = 7, hintsUsed = 0, responseTimeMs = 200)

        val results = upsertActivityResult(listOf(failed), completed)

        assertEquals(listOf(completed), results)
    }

    @Test
    fun `different activities remain ordered when a result is appended`() {
        val first = ActivityResult("a-1", correct = true, attempts = 1, hintsUsed = 0, responseTimeMs = 10)
        val second = ActivityResult("a-2", correct = false, attempts = 1, hintsUsed = 0, responseTimeMs = 20)

        assertEquals(listOf(first, second), upsertActivityResult(listOf(first), second))
    }
}
