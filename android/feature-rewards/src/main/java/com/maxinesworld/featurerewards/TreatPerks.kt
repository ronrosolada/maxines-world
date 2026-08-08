package com.maxinesworld.featurerewards

/**
 * Treat Shop perks — the real, functional payoff for spending coins.
 *
 * Each owned item changes what a lesson completion grants:
 *  - Fish Treat Basket : double stars on the first completed lesson each day
 *  - Cozy Milo Cushion : every lesson grants 1 extra star (cozy bonus)
 *  - Starry Food Bowl  : every lesson grants 1 extra coin (bowl fills up)
 *
 * Pure and deterministic so the math is unit-testable; the repository owns
 * persistence (inventory check + once-per-day ledger row) and calls this.
 */
data class PerkApplication(
    val stars: Int,
    val coins: Int,
    /** True when the Fish Treat Basket doubled this lesson's stars. */
    val basketDoubled: Boolean,
)

object TreatPerks {
    const val BASKET_ID = "fish-treat-basket"
    const val CUSHION_ID = "cozy-milo-cushion"
    const val BOWL_ID = "starry-food-bowl"

    /**
     * @param ownedItemIds  item ids the child owns (inventory)
     * @param basketUsedToday true when the once-per-day doubling was already
     *        consumed today (or the child does not own the basket)
     */
    fun applyTo(
        starsEarned: Int,
        coinsEarned: Int,
        ownedItemIds: Set<String>,
        basketUsedToday: Boolean,
    ): PerkApplication {
        var stars = starsEarned
        var coins = coinsEarned
        var basketDoubled = false

        if (BASKET_ID in ownedItemIds && !basketUsedToday) {
            stars *= 2
            basketDoubled = true
        }
        if (CUSHION_ID in ownedItemIds) {
            stars += 1
        }
        if (BOWL_ID in ownedItemIds) {
            coins += 1
        }
        return PerkApplication(stars = stars, coins = coins, basketDoubled = basketDoubled)
    }
}
