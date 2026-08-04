package com.maxinesworld.featurerewards

import androidx.room.withTransaction
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.InventoryEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.UUID
import javax.inject.Inject

/** Small, permanent keepsakes that make lesson-earned coins useful to a child. */
data class TreatShopItem(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val cost: Int,
)

object TreatShopCatalog {
    val items: List<TreatShopItem> = listOf(
        TreatShopItem(
            id = "fish-treat-basket",
            name = "Fish Treat Basket",
            description = "A tasty snack for Milo.",
            emoji = "🐟",
            cost = 5,
        ),
        TreatShopItem(
            id = "cozy-milo-cushion",
            name = "Cozy Milo Cushion",
            description = "A soft place for Milo to nap.",
            emoji = "🛏️",
            cost = 8,
        ),
        TreatShopItem(
            id = "starry-food-bowl",
            name = "Starry Food Bowl",
            description = "A shiny bowl for a star learner.",
            emoji = "🥣",
            cost = 12,
        ),
    )
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
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    itemId = item.id,
                )
            )
            if (inventoryInserted == 0L) {
                return@withTransaction PurchaseResult.AlreadyOwned
            }

            rewardDao.insert(
                RewardEntity(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    type = "COIN",
                    amount = -item.cost,
                    metadata = "treat-shop:${item.id}",
                )
            )
            PurchaseResult.Purchased
        }
}
