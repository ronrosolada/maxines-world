package com.maxinesworld.app

object MiniGameRoutes {
    const val REWARD_HUB = "reward/{childId}/{rewardBreakId}"
    const val CAT_CAFE = "reward/cat-cafe/{childId}/{rewardBreakId}/{durationMillis}"
    const val PARKOUR = "reward/parkour/{childId}/{rewardBreakId}/{durationMillis}"
    const val KITTEN_MATCH = "reward/kitten-match/{childId}/{rewardBreakId}/{durationMillis}"

    fun hub(childId: String, breakId: String) = "reward/$childId/$breakId"
    fun catCafe(childId: String, breakId: String, durationMillis: Long) =
        "reward/cat-cafe/$childId/$breakId/$durationMillis"
    fun parkour(childId: String, breakId: String, durationMillis: Long) =
        "reward/parkour/$childId/$breakId/$durationMillis"
    fun kittenMatch(childId: String, breakId: String, durationMillis: Long) =
        "reward/kitten-match/$childId/$breakId/$durationMillis"
}
