package com.maxinesworld.featurerewards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.RewardLedgerDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatCafeShopUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val childId: String = "",
    val balance: Int = 0,
    val menuItems: List<CatCafeShopItem> = emptyList(),
    val purchaseStatus: PurchaseUiStatus? = null,
)

data class CatCafeShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val owned: Boolean,
    val affordable: Boolean = false,
)

sealed class PurchaseUiStatus {
    data class Purchased(val itemName: String) : PurchaseUiStatus()
    data class AlreadyOwned(val itemName: String) : PurchaseUiStatus()
    data class InsufficientFunds(val itemName: String, val price: Int, val balance: Int) : PurchaseUiStatus()
    data class Error(val message: String) : PurchaseUiStatus()
}

@HiltViewModel
class CatCafeShopViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rewardLedgerDao: RewardLedgerDao,
    private val inventoryDao: InventoryDao,
    private val purchaseHandler: CafePurchaseHandler,
) : ViewModel() {

    private val childId: String = savedStateHandle["childId"] ?: error("childId missing")

    private val _state = MutableStateFlow(CatCafeShopUiState(childId = childId))
    val state: StateFlow<CatCafeShopUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, purchaseStatus = null) }
            try {
                val balance = rewardLedgerDao.fishTreatBalance(childId)
                val menuItems = CAT_CAFE_MENU.values.map { item ->
                    val owned = inventoryDao.owns(childId, item.id)
                    CatCafeShopItem(
                        id = item.id,
                        name = item.name,
                        price = item.price,
                        owned = owned,
                        affordable = !owned && balance >= item.price,
                    )
                }
                _state.update { it.copy(loading = false, balance = balance, menuItems = menuItems) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not load café") }
            }
        }
    }

    fun purchase(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(purchaseStatus = null) }
            val result = purchaseHandler.purchaseItem(childId, itemId)
            _state.update { current ->
                val status = when (result) {
                    is PurchaseResult.Purchased -> PurchaseUiStatus.Purchased(result.itemName)
                    is PurchaseResult.AlreadyOwned -> PurchaseUiStatus.AlreadyOwned(result.itemName)
                    is PurchaseResult.InsufficientFunds -> PurchaseUiStatus.InsufficientFunds(
                        itemName = result.itemName, price = result.price, balance = result.balance
                    )
                    is PurchaseResult.ItemNotFound -> PurchaseUiStatus.Error("Item not found")
                }
                current.copy(purchaseStatus = status)
            }
            // Reload to reflect new balance + ownership
            load()
        }
    }

    fun clearPurchaseStatus() {
        _state.update { it.copy(purchaseStatus = null) }
    }

    fun retry() { load() }
}
