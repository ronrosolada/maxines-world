package com.maxinesworld.featurerewards

import androidx.room.withTransaction
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.InventoryEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/** Small, permanent sanctuary decorations purchased with lesson-earned tokens. */
data class TreatShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val iconKey: String,
    /** Kid-readable cosmetic line — these items never alter learning rewards. */
    val perk: String,
)

object TreatShopCatalog {
    val items: List<TreatShopItem> = listOf(
        TreatShopItem(
            id = "fish-treat-basket",
            name = "Fish Treat Basket",
            description = "A cheerful basket for Milo's sanctuary.",
            cost = 5,
            iconKey = "basket",
            perk = "Cosmetic decoration for Milo's home",
        ),
        TreatShopItem(
            id = "cozy-milo-cushion",
            name = "Cozy Milo Cushion",
            description = "A soft place for Milo to nap after exploring.",
            cost = 8,
            iconKey = "cushion",
            perk = "Cosmetic decoration for Milo's home",
        ),
        TreatShopItem(
            id = "starry-food-bowl",
            name = "Starry Food Bowl",
            description = "A shiny bowl for a thoughtful animal friend.",
            cost = 12,
            iconKey = "bowl",
            perk = "Cosmetic decoration for Milo's home",
        ),
    )

    fun byId(id: String): TreatShopItem? = items.firstOrNull { it.id == id }
}

enum class PurchaseResult {
    Purchased,
    AlreadyOwned,
    NotEnoughCoins,
}

@ViewModelScoped
class TreatShopRepository @Inject constructor(
    private val database: MaxinesDatabase,
    private val rewardDao: RewardDao,
    private val inventoryDao: InventoryDao,
) {
    suspend fun coinBalance(childId: String): Int =
        rewardDao.getTotalByType(childId, "COIN") ?: 0

    suspend fun ownedItemIds(childId: String): Set<String> =
        inventoryDao.getOwnedItemIds(childId).toSet()

    suspend fun purchase(childId: String, item: TreatShopItem): PurchaseResult =
        database.withTransaction {
            if (inventoryDao.owns(childId, item.id)) {
                return@withTransaction PurchaseResult.AlreadyOwned
            }

            val balance = rewardDao.getTotalByType(childId, "COIN") ?: 0
            if (balance < item.cost) {
                return@withTransaction PurchaseResult.NotEnoughCoins
            }

            val inventoryInserted = inventoryDao.insertIgnoring(
                InventoryEntity(
                    id = "treat-shop:$childId:${item.id}",
                    childId = childId,
                    itemId = item.id,
                )
            )
            if (inventoryInserted == 0L) {
                return@withTransaction PurchaseResult.AlreadyOwned
            }

            rewardDao.insert(
                RewardEntity(
                    id = "treat-shop:$childId:${item.id}:debit",
                    childId = childId,
                    type = "COIN",
                    amount = -item.cost,
                    metadata = "treat-shop:${item.id}",
                )
            )
            PurchaseResult.Purchased
        }
}
