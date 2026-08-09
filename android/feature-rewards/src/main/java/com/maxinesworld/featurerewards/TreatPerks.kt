package com.maxinesworld.featurerewards

/**
 * Compatibility shell for older callers. Sanctuary decorations are cosmetic;
 * owning one never changes lesson rewards. New completion code uses
 * [LessonRewardPolicy] directly.
 */
data class PerkApplication(
    val stars: Int,
    val coins: Int,
    val basketDoubled: Boolean = false,
)

object TreatPerks {
    const val BASKET_ID = "fish-treat-basket"
    const val CUSHION_ID = "cozy-milo-cushion"
    const val BOWL_ID = "starry-food-bowl"

    @Deprecated("Sanctuary decorations are cosmetic and do not alter learning rewards.")
    fun applyTo(
        starsEarned: Int,
        coinsEarned: Int,
        ownedItemIds: Set<String>,
        basketUsedToday: Boolean,
    ): PerkApplication = PerkApplication(
        stars = starsEarned,
        coins = coinsEarned,
        basketDoubled = false,
    )
}
