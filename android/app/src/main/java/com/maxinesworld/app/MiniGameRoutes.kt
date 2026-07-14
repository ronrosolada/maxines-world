package com.maxinesworld.app

object MiniGameRoutes {
    const val REWARD_HUB = "reward/{childId}/{rewardBreakId}"
    const val CAT_CAFE = "reward/cat-cafe/{childId}/{rewardBreakId}"
    const val PARKOUR = "reward/parkour/{childId}/{rewardBreakId}"
    const val KITTEN_MATCH = "reward/kitten-match/{childId}/{rewardBreakId}"
    const val FIREFLY_GARDEN = "reward/firefly-garden/{childId}/{rewardBreakId}"
    const val PAW_BEATS = "reward/paw-beats/{childId}/{rewardBreakId}"

    fun hub(childId: String, breakId: String) = "reward/$childId/$breakId"
    fun catCafe(childId: String, breakId: String) = "reward/cat-cafe/$childId/$breakId"
    fun parkour(childId: String, breakId: String) = "reward/parkour/$childId/$breakId"
    fun kittenMatch(childId: String, breakId: String) = "reward/kitten-match/$childId/$breakId"
    fun fireflyGarden(childId: String, breakId: String) = "reward/firefly-garden/$childId/$breakId"
    fun pawBeats(childId: String, breakId: String) = "reward/paw-beats/$childId/$breakId"
}
