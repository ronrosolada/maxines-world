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

/**
 * Small, permanent keepsakes that make lesson-earned coins useful to a child.
 * Each item has its own emoji artwork (rendered large in the shop and as a
 * keepsake chip on the Playroom home), so buying something always changes
 * what the child sees.
 */
data class TreatShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val emoji: String,
    /** Kid-readable perk line — what owning this item does for lessons. */
    val perk: String,
)

object TreatShopCatalog {
    val items: List<TreatShopItem> = listOf(
        TreatShopItem(
            id = "fish-treat-basket",
            name = "Fish Treat Basket",
            description = "A tasty snack for Milo.",
            cost = 5,
            emoji = "🧺",
            perk = "Perk: double stars on your first lesson each day!",
        ),
        TreatShopItem(
            id = "cozy-milo-cushion",
            name = "Cozy Milo Cushion",
            description = "A soft place for Milo to nap.",
            cost = 8,
            emoji = "🛋️",
            perk = "Perk: every lesson gives 1 extra star",
        ),
        TreatShopItem(
            id = "starry-food-bowl",
            name = "Starry Food Bowl",
            description = "A shiny bowl for a star learner.",
            cost = 12,
            emoji = "🥣",
            perk = "Perk: every lesson fills the bowl with 1 coin",
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
