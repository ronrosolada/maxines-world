package com.maxinesworld.featurerewards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TreatPerksTest {
    @Test
    fun sanctuaryDecorationsNeverChangeLessonRewards() {
        val out = TreatPerks.applyTo(
            starsEarned = 2,
            coinsEarned = 1,
            ownedItemIds = setOf(TreatPerks.BASKET_ID, TreatPerks.CUSHION_ID, TreatPerks.BOWL_ID),
            basketUsedToday = false,
        )

        assertEquals(2, out.stars)
        assertEquals(1, out.coins)
        assertFalse(out.basketDoubled)
    }

    @Test
    fun lessonRewardPolicyAlwaysHasACompletionReward() {
        assertEquals(LessonReward(1, 1), LessonRewardPolicy.forAccuracy(0.0))
        assertEquals(LessonReward(2, 2), LessonRewardPolicy.forAccuracy(0.8))
        assertEquals(LessonReward(3, 2), LessonRewardPolicy.forAccuracy(0.95))
    }
}
