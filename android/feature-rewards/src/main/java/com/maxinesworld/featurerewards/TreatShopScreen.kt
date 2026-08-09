package com.maxinesworld.featurerewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TreatShopCream = Color(0xFFFFF7E8)
private val TreatShopInk = Color(0xFF183B4A)

data class TreatShopUiState(
    val coins: Int = 0,
    val ownedItemIds: Set<String> = emptySet(),
    val message: String? = null,
)

@HiltViewModel
class TreatShopViewModel @Inject constructor(
    private val repository: TreatShopRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TreatShopUiState())
    val state: StateFlow<TreatShopUiState> = _state.asStateFlow()

    fun load(childId: String) {
        viewModelScope.launch {
            _state.value = TreatShopUiState(
                coins = repository.coinBalance(childId),
                ownedItemIds = repository.ownedItemIds(childId),
            )
        }
    }

    fun purchase(childId: String, item: TreatShopItem) {
        viewModelScope.launch {
            val result = repository.purchase(childId, item)
            val coins = repository.coinBalance(childId)
            val owned = repository.ownedItemIds(childId)
            val message = when (result) {
                PurchaseResult.Purchased -> "${item.name} is now part of Milo's sanctuary."
                PurchaseResult.AlreadyOwned -> "You already have ${item.name}."
                PurchaseResult.NotEnoughCoins -> "You need more tokens for ${item.name}."
            }
            _state.value = TreatShopUiState(coins, owned, message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatShopScreen(
    childId: String,
    onBack: () -> Unit,
    viewModel: TreatShopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(childId) { viewModel.load(childId) }
    TreatShopContent(
        state = state,
        onBack = onBack,
        onPurchase = { item -> viewModel.purchase(childId, item) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatShopContent(
    state: TreatShopUiState,
    onBack: () -> Unit,
    onPurchase: (TreatShopItem) -> Unit,
) {
    Scaffold(
        containerColor = TreatShopCream,
        topBar = {
            TopAppBar(
                title = { Text("Milo's Sanctuary Workshop") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("‹", fontSize = 32.sp, color = TreatShopInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TreatShopCream),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Text(
                "Use tokens from your lessons to add a decoration to Milo's sanctuary.",
                style = MaterialTheme.typography.bodyLarge,
                color = TreatShopInk,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "${state.coins} sanctuary tokens",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = VillageTeal,
                modifier = Modifier.semantics {
                    contentDescription = "${state.coins} sanctuary tokens available"
                },
            )
            state.message?.let { message ->
                Text(
                    message,
                    color = VillageTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(TreatShopCatalog.items, key = { it.id }) { item ->
                    val owned = item.id in state.ownedItemIds
                    val affordable = state.coins >= item.cost
                    TreatShopItemCard(
                        item = item,
                        owned = owned,
                        affordable = affordable,
                        onPurchase = { onPurchase(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TreatShopItemCard(
    item: TreatShopItem,
    owned: Boolean,
    affordable: Boolean,
    onPurchase: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = treatIcon(item.iconKey),
                contentDescription = item.name,
                tint = VillageTeal,
                modifier = Modifier.size(56.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name, fontWeight = FontWeight.ExtraBold, color = TreatShopInk)
                Text(item.description, style = MaterialTheme.typography.bodyMedium, color = TreatShopInk)
                Text(item.perk, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VillageTeal)
                Text("${item.cost} tokens", color = TreatShopInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = onPurchase,
                enabled = !owned && affordable,
                colors = ButtonDefaults.buttonColors(containerColor = VillageTeal),
            ) {
                Text(if (owned) "Owned" else if (affordable) "Get it" else "Need more")
            }
        }
    }
}

private fun treatIcon(iconKey: String): ImageVector = when (iconKey) {
    "basket", "cushion", "bowl" -> Icons.Default.Pets
    else -> Icons.Default.Park
}
