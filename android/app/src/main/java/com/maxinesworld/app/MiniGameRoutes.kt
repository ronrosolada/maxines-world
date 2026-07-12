package com.maxinesworld.app

object MiniGameRoutes {
    const val REWARD_HUB = "reward/{childId}/{rewardBreakId}"
    const val CAT_CAFE = "reward/cat-cafe/{childId}/{rewardBreakId}"
    const val PARKOUR = "reward/parkour/{childId}/{rewardBreakId}"

    fun hub(childId: String, breakId: String) = "reward/$childId/$breakId"
    fun catCafe(childId: String, breakId: String) = "reward/cat-cafe/$childId/$breakId"
    fun parkour(childId: String, breakId: String) = "reward/parkour/$childId/$breakId"
}
