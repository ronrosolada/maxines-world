package com.maxinesworld.featurerewards

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.RewardEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreatShopPurchaseTest {
    private lateinit var database: MaxinesDatabase

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun purchaseDebitsCoinsAndPersistsInventory() = runBlocking {
        val childId = "child-shop"
        database.rewardDao().insert(
            RewardEntity(
                id = "earned-coins",
                childId = childId,
                type = "COIN",
                amount = 10,
            )
        )

        val item = TreatShopCatalog.items.first()
        val result = TreatShopRepository(
            database = database,
            rewardDao = database.rewardDao(),
            inventoryDao = database.inventoryDao(),
        ).purchase(childId, item)

        assertEquals(PurchaseResult.Purchased, result)
        assertEquals(10 - item.cost, database.rewardDao().getTotalByType(childId, "COIN"))
        assertTrue(database.inventoryDao().owns(childId, item.id))
    }

    @Test
    fun purchaseWithoutEnoughCoinsDoesNotDebitOrGrantItem() = runBlocking {
        val childId = "child-no-coins"
        val item = TreatShopCatalog.items.first()
        val result = TreatShopRepository(database, database.rewardDao(), database.inventoryDao())
            .purchase(childId, item)

        assertEquals(PurchaseResult.NotEnoughCoins, result)
        assertEquals(0, database.rewardDao().getTotalByType(childId, "COIN") ?: 0)
        assertFalse(database.inventoryDao().owns(childId, item.id))
    }

    @Test
    fun duplicatePurchaseDoesNotChargeTwice() = runBlocking {
        val childId = "child-duplicate"
        val item = TreatShopCatalog.items.first()
        database.rewardDao().insert(
            RewardEntity(
                id = "duplicate-coins",
                childId = childId,
                type = "COIN",
                amount = 10,
            )
        )
        val repository = TreatShopRepository(database, database.rewardDao(), database.inventoryDao())

        assertEquals(PurchaseResult.Purchased, repository.purchase(childId, item))
        assertEquals(PurchaseResult.AlreadyOwned, repository.purchase(childId, item))
        assertEquals(10 - item.cost, database.rewardDao().getTotalByType(childId, "COIN"))
    }
}
