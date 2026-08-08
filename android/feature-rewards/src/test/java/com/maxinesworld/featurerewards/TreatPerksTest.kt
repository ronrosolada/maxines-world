package com.maxinesworld.featurerewards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreatPerksTest {

    @Test
    fun noItemsMeansNoChange() {
        val out = TreatPerks.applyTo(
            starsEarned = 2,
            coinsEarned = 10,
            ownedItemIds = emptySet(),
            basketUsedToday = true,
        )
        assertEquals(2, out.stars)
        assertEquals(10, out.coins)
        assertFalse(out.basketDoubled)
    }

    @Test
    fun basketDoublesWhenNotUsedToday() {
        val out = TreatPerks.applyTo(
            starsEarned = 2,
            coinsEarned = 10,
            ownedItemIds = setOf(TreatPerks.BASKET_ID),
            basketUsedToday = false,
        )
        assertEquals(4, out.stars)
        assertEquals(10, out.coins)
        assertTrue(out.basketDoubled)
    }

    @Test
    fun basketDoesNotDoubleTwicePerDay() {
        val out = TreatPerks.applyTo(
            starsEarned = 3,
            coinsEarned = 10,
            ownedItemIds = setOf(TreatPerks.BASKET_ID),
            basketUsedToday = true,
        )
        assertEquals(3, out.stars)
        assertFalse(out.basketDoubled)
    }

    @Test
    fun cushionAddsOneStarEveryLesson() {
        val out = TreatPerks.applyTo(
            starsEarned = 1,
            coinsEarned = 0,
            ownedItemIds = setOf(TreatPerks.CUSHION_ID),
            basketUsedToday = true,
        )
        assertEquals(2, out.stars)
        assertEquals(0, out.coins)
    }

    @Test
    fun bowlAddsOneCoinEvenOnToughLessons() {
        val out = TreatPerks.applyTo(
            starsEarned = 1,
            coinsEarned = 0,
            ownedItemIds = setOf(TreatPerks.BOWL_ID),
            basketUsedToday = true,
        )
        assertEquals(1, out.stars)
        assertEquals(1, out.coins)
    }

    @Test
    fun allPerksStack() {
        val out = TreatPerks.applyTo(
            starsEarned = 2,
            coinsEarned = 10,
            ownedItemIds = setOf(TreatPerks.BASKET_ID, TreatPerks.CUSHION_ID, TreatPerks.BOWL_ID),
            basketUsedToday = false,
        )
        // 2 doubled to 4, then cushion adds 1 → 5; coins 10 + bowl 1 → 11
        assertEquals(5, out.stars)
        assertEquals(11, out.coins)
        assertTrue(out.basketDoubled)
    }

    @Test
    fun basketThenCushionOrderIsDeterministic() {
        // Doubling applies first, cushion bonus second — always the same order.
        val a = TreatPerks.applyTo(1, 0, setOf(TreatPerks.BASKET_ID, TreatPerks.CUSHION_ID), false)
        val b = TreatPerks.applyTo(1, 0, setOf(TreatPerks.CUSHION_ID, TreatPerks.BASKET_ID), false)
        assertEquals(a.stars, b.stars)
        assertEquals(3, a.stars)
    }
}
