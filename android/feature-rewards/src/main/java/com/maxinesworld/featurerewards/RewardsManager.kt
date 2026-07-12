package com.maxinesworld.featurerewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredesignsystem.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

data class RewardsUiState(
    val totalStars: Int = 0,
    val totalCoins: Int = 0,
    val villageEnergy: Int = 0,
    val badges: List<String> = emptyList(),
    val recentRewards: List<RewardEntity> = emptyList()
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardDao: RewardDao
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(RewardsUiState())
    val state: StateFlow<RewardsUiState> = _state.asStateFlow()

    fun loadRewards(childId: String) {
        viewModelScope.launch {
            val stars = rewardDao.getTotalByType(childId, "STAR") ?: 0
            val coins = rewardDao.getTotalByType(childId, "COIN") ?: 0
            _state.update {
                it.copy(totalStars = stars, totalCoins = coins)
            }
        }
    }

    fun grantReward(
        childId: String,
        type: String,
        amount: Int = 1,
        subject: String = "",
        metadata: String = ""
    ) {
        viewModelScope.launch {
            val reward = RewardEntity(
                id = UUID.randomUUID().toString(),
                childId = childId,
                type = type,
                subject = subject,
                amount = amount,
                metadata = metadata
            )
            rewardDao.insert(reward)

            val stars = rewardDao.getTotalByType(childId, "STAR") ?: 0
            val coins = rewardDao.getTotalByType(childId, "COIN") ?: 0
            _state.update {
                it.copy(
                    totalStars = stars,
                    totalCoins = coins,
                    recentRewards = listOf(reward) + it.recentRewards.take(9)
                )
            }
        }
    }
}

@Composable
fun RewardsScreen(
    childId: String,
    viewModel: RewardsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(childId) { viewModel.loadRewards(childId) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏆", fontSize = 48.sp)
        Text("My Rewards", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Teal40)
        Spacer(Modifier.height(16.dp))

        // Balance row
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            RewardBalance("⭐", state.totalStars, "Stars")
            RewardBalance("🪙", state.totalCoins, "Coins")
            RewardBalance("⚡", state.villageEnergy, "Energy")
        }
        Spacer(Modifier.height(20.dp))

        // Badges
        Text("Badges", fontWeight = FontWeight.Bold, color = Teal40)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(8) { index ->
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (index < 3) EnergyGold else EnergyGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        listOf("🐾", "📚", "⭐", "🔒", "🔒", "🔒", "🔒", "🔒")[index],
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardBalance(emoji: String, amount: Int, label: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Text("$amount", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
