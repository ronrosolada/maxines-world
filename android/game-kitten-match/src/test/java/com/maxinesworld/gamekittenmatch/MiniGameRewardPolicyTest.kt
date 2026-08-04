package com.maxinesworld.gamekittenmatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MiniGameRewardPolicyTest {
    @Test fun `mini game never grants progression rewards directly`() {
        assertEquals(0, MiniGameRewardPolicy.FISH_TREATS_EARNED)
        assertNull(MiniGameRewardPolicy.COLLECTIBLE_ID)
    }
}
