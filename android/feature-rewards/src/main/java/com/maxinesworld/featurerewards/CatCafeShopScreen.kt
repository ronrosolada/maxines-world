package com.maxinesworld.featurerewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatCafeShopScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatCafeShopViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle one-shot purchase status via Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.purchaseStatus) {
        when (val s = state.purchaseStatus) {
            is PurchaseUiStatus.Purchased -> snackbarHostState.showSnackbar("${s.itemName} purchased! 🎁")
            is PurchaseUiStatus.AlreadyOwned -> snackbarHostState.showSnackbar("You already own ${s.itemName}")
            is PurchaseUiStatus.InsufficientFunds -> snackbarHostState.showSnackbar("Need ${s.price} fish treats, have ${s.balance}")
            is PurchaseUiStatus.Error -> snackbarHostState.showSnackbar(s.message)
            null -> {}
        }
        viewModel.clearPurchaseStatus()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🐱 Cat Café") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Home") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F0E8),
                ),
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingCafe()
            state.error != null -> ErrorCafe(message = state.error!!, onRetry = viewModel::retry)
            state.menuItems.isEmpty() -> EmptyCafe(onBack = onBack)
            else -> CafeMenu(
                state = state,
                onPurchase = viewModel::purchase,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun LoadingCafe() {
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F0E8)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF3A6B63))
    }
}

@Composable
private fun ErrorCafe(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F0E8)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oops! Something went wrong.", color = Color(0xFFB00020), fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Try Again") }
        }
    }
}

@Composable
private fun EmptyCafe(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F0E8)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("The café is empty right now. 🐾", fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("Back to Village") }
        }
    }
}

@Composable
private fun CafeMenu(
    state: CatCafeShopUiState,
    onPurchase: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0E8))
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Balance header
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3A6B63)),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "🐟 Your Fish Treats",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "${state.balance}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Menu items
        items(state.menuItems) { item ->
            CafeItemCard(item = item, onPurchase = onPurchase)
        }
    }
}

@Composable
private fun CafeItemCard(
    item: CatCafeShopItem,
    onPurchase: (String) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth().testTag("cafe_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.owned) Color(0xFFE8F5E9) else Color.White,
        ),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("${item.price} fish treats", fontSize = 14.sp, color = Color.Gray)
            }
            when {
                item.owned -> Text("Owned ✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                else -> Button(
                    onClick = { onPurchase(item.id) },
                    enabled = item.affordable,
                    modifier = Modifier.testTag("purchase_${item.id}"),
                ) {
                    Text("Buy")
                }
            }
        }
    }
}
