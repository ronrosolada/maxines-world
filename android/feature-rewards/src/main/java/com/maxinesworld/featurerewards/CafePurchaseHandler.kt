package com.maxinesworld.featurerewards

import androidx.room.withTransaction
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.InventoryEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.RewardLedgerDao
import com.maxinesworld.coredatabase.RewardLedgerEntity
import com.maxinesworld.coremodel.gamification.deterministicUuid
import javax.inject.Inject
import javax.inject.Singleton

// ─── Cat Café Items ───

data class CafeItem(
    val id: String,
    val name: String,
    val price: Int
)

val CAT_CAFE_MENU: Map<String, CafeItem> = listOf(
    CafeItem("cat_bed_cozy", "Cozy Cat Bed", 50),
    CafeItem("yarn_ball_red", "Red Yarn Ball", 30),
    CafeItem("fish_toy", "Flutter Fish Toy", 40),
    CafeItem("cat_tower_small", "Small Cat Tower", 100),
    CafeItem("bowl_premium", "Premium Food Bowl", 60),
    CafeItem("collar_bell", "Bell Collar", 25),
    CafeItem("scratching_post", "Scratching Post", 70),
    CafeItem("tunnel_play", "Play Tunnel", 80)
).associateBy { it.id }

// ─── Purchase Result ───

sealed class PurchaseResult {
    data class AlreadyOwned(val itemName: String) : PurchaseResult()
    data class InsufficientFunds(val itemName: String, val price: Int, val balance: Int) : PurchaseResult()
    data class Purchased(val itemName: String, val price: Int) : PurchaseResult()
    data class ItemNotFound(val itemId: String) : PurchaseResult()
}

// ─── Purchase Handler ───

@Singleton
class CafePurchaseHandler @Inject constructor(
    private val database: MaxinesDatabase
) {
    private val rewardLedgerDao: RewardLedgerDao get() = database.rewardLedgerDao()
    private val inventoryDao: InventoryDao get() = database.inventoryDao()

    suspend fun purchaseItem(childId: String, itemId: String): PurchaseResult {
        return database.withTransaction {
            // 1. Already owned?
            if (inventoryDao.owns(childId, itemId)) {
                val item = CAT_CAFE_MENU[itemId]
                return@withTransaction PurchaseResult.AlreadyOwned(item?.name ?: itemId)
            }

            // 2. Get item from menu
            val cafeItem = CAT_CAFE_MENU[itemId]
                ?: return@withTransaction PurchaseResult.ItemNotFound(itemId)

            // 3. Check balance
            val balance = rewardLedgerDao.fishTreatBalance(childId)
            if (balance < cafeItem.price) {
                return@withTransaction PurchaseResult.InsufficientFunds(
                    itemName = cafeItem.name,
                    price = cafeItem.price,
                    balance = balance
                )
            }

            // 4. Insert debit ledger row (idempotent via unique index on child_id + source_key)
            val sourceKey = "purchase:$childId:$itemId"
            val ledgerId = deterministicUuid(sourceKey).toString()
            rewardLedgerDao.insertIgnoring(
                RewardLedgerEntity(
                    id = ledgerId,
                    childId = childId,
                    currency = "FISH_TREAT",
                    amount = -cafeItem.price,
                    sourceKey = sourceKey,
                    createdAtEpoch = System.currentTimeMillis()
                )
            )

            // 5. Insert inventory row (idempotent via unique index on child_id + item_id)
            val inventoryId = deterministicUuid("$childId:$itemId").toString()
            inventoryDao.insertIgnoring(
                InventoryEntity(
                    id = inventoryId,
                    childId = childId,
                    itemId = itemId,
                    acquiredAtEpoch = System.currentTimeMillis()
                )
            )

            PurchaseResult.Purchased(itemName = cafeItem.name, price = cafeItem.price)
        }
    }
}
