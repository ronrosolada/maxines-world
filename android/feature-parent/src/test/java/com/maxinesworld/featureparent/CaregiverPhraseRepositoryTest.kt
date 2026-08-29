package com.maxinesworld.featureparent

import com.maxinesworld.coremodel.CaregiverPhraseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CaregiverPhraseRepositoryTest {
    private val repository = CaregiverPhraseRepository()

    @Test
    fun `loads an essential deck spanning every category`() {
        val cards = repository.loadCards()

        assertTrue(cards.size in 16..20)
        assertEquals(CaregiverPhraseCategory.entries.toSet(), cards.map { it.category }.toSet())
        assertTrue(cards.all { it.filipinoPhrase.isNotBlank() && it.englishCue.isNotBlank() && it.practicalTip.isNotBlank() })
        assertEquals(cards.size, cards.map { it.id }.distinct().size)
    }

    @Test
    fun `filters cards by category and case insensitive search across phrase and cue`() {
        val praises = repository.filterCards(category = CaregiverPhraseCategory.PRAISES)
        val water = repository.filterCards(query = "WATER")
        val filipino = repository.filterCards(query = "tubig")

        assertTrue(praises.isNotEmpty())
        assertTrue(praises.all { it.category == CaregiverPhraseCategory.PRAISES })
        assertEquals(water.map { it.id }, filipino.map { it.id })
        assertEquals(listOf("basic-needs-water"), water.map { it.id })
    }

    @Test
    fun `practice tracking is scoped to today and can be toggled off`() {
        val tracker = CaregiverPracticeTracker()
        val cardId = repository.loadCards().first().id
        val today = LocalDate.of(2026, 8, 29)

        tracker.setPracticed(cardId, true, today)
        assertTrue(tracker.isPracticed(cardId, today))
        assertFalse(tracker.isPracticed(cardId, today.plusDays(1)))

        tracker.setPracticed(cardId, false, today)
        assertFalse(tracker.isPracticed(cardId, today))
    }
}
