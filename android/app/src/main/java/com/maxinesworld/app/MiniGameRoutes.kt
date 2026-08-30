package com.maxinesworld.app

object MiniGameRoutes {
    const val REWARD_HUB = "reward/{childId}/{rewardBreakId}"
    const val SOURCE_LIBRARY = "reward/source-games/{childId}/{rewardBreakId}"
    const val SOURCE_WEB_GAME = "reward/source-game/{childId}/{rewardBreakId}/{durationMillis}/{gameSlug}"
    const val CAT_CAFE = "reward/cat-cafe/{childId}/{rewardBreakId}/{durationMillis}"
    const val PARKOUR = "reward/parkour/{childId}/{rewardBreakId}/{durationMillis}"
    const val KITTEN_MATCH = "reward/kitten-match/{childId}/{rewardBreakId}/{durationMillis}"
    const val TARSIER_CANOPY = "reward/tarsier-canopy/{childId}/{rewardBreakId}/{durationMillis}"

    private fun segment(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val code = byte.toInt() and 0xFF
            val character = code.toChar()
            if (
                code in 'A'.code..'Z'.code ||
                code in 'a'.code..'z'.code ||
                code in '0'.code..'9'.code ||
                character in "-_.!~*'()"
            ) {
                append(character)
            } else {
                append('%')
                append(HEX_DIGITS[code ushr 4])
                append(HEX_DIGITS[code and 0x0F])
            }
        }
    }

    private const val HEX_DIGITS = "0123456789ABCDEF"

    fun hub(childId: String, breakId: String) =
        "reward/${segment(childId)}/${segment(breakId)}"

    fun sourceLibrary(childId: String, breakId: String) =
        "reward/source-games/${segment(childId)}/${segment(breakId)}"

    fun sourceWebGame(childId: String, breakId: String, durationMillis: Long, gameSlug: String) =
        "reward/source-game/${segment(childId)}/${segment(breakId)}/$durationMillis/${segment(gameSlug)}"

    fun catCafe(childId: String, breakId: String, durationMillis: Long) =
        "reward/cat-cafe/${segment(childId)}/${segment(breakId)}/$durationMillis"

    fun parkour(childId: String, breakId: String, durationMillis: Long) =
        "reward/parkour/${segment(childId)}/${segment(breakId)}/$durationMillis"

    fun kittenMatch(childId: String, breakId: String, durationMillis: Long) =
        "reward/kitten-match/${segment(childId)}/${segment(breakId)}/$durationMillis"

    fun tarsierCanopy(childId: String, breakId: String, durationMillis: Long) =
        "reward/tarsier-canopy/${segment(childId)}/${segment(breakId)}/$durationMillis"
}
