package com.maxinesworld.app

object MiniGameRoutes {
    const val REWARD_HUB = "reward/{childId}/{rewardBreakId}"
    const val SOURCE_LIBRARY = "reward/source-games/{childId}/{rewardBreakId}"
    const val SOURCE_WEB_GAME = "reward/source-game/{childId}/{rewardBreakId}/{durationMillis}/{gameSlug}"
    const val CAT_CAFE = "reward/cat-cafe/{childId}/{rewardBreakId}/{durationMillis}"
    const val PARKOUR = "reward/parkour/{childId}/{rewardBreakId}/{durationMillis}"
    const val KITTEN_MATCH = "reward/kitten-match/{childId}/{rewardBreakId}/{durationMillis}"

    fun hub(childId: String, breakId: String) = "reward/$childId/$breakId"
    fun sourceLibrary(childId: String, breakId: String) =
        "reward/source-games/$childId/$breakId"
    fun sourceWebGame(childId: String, breakId: String, durationMillis: Long, gameSlug: String) =
        "reward/source-game/$childId/$breakId/$durationMillis/$gameSlug"
    fun catCafe(childId: String, breakId: String, durationMillis: Long) =
        "reward/cat-cafe/$childId/$breakId/$durationMillis"
    fun parkour(childId: String, breakId: String, durationMillis: Long) =
        "reward/parkour/$childId/$breakId/$durationMillis"
    fun kittenMatch(childId: String, breakId: String, durationMillis: Long) =
        "reward/kitten-match/$childId/$breakId/$durationMillis"
}
