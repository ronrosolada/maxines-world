package com.maxinesworld.coremodel.gamification

import org.junit.Assert.assertEquals
import org.junit.Test

class FishTreatPolicyTest {
    @Test fun incompleteAttemptEarnsNothing() = assertEquals(0, FishTreatPolicy.amount(LessonRewardInput(false, true, true)))
    @Test fun completionEarnsThree() = assertEquals(3, FishTreatPolicy.amount(LessonRewardInput(true, false, false)))
    @Test fun improvementEarnsOneBonus() = assertEquals(4, FishTreatPolicy.amount(LessonRewardInput(true, true, false)))
    @Test fun masteryEarnsTwoBonus() = assertEquals(5, FishTreatPolicy.amount(LessonRewardInput(true, false, true)))
    @Test fun allBonusesAreCappedAtSix() = assertEquals(6, FishTreatPolicy.amount(LessonRewardInput(true, true, true)))
}
