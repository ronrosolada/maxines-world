package com.maxinesworld.gamecatcafe

import com.maxinesworld.engineminigame.MiniGameDefinition
import com.maxinesworld.engineminigame.MiniGameMode

val CatCafeDashDefinition = MiniGameDefinition(
    id = CatCafeViewModel.GAME_ID,
    title = "Cat Café Dash",
    mode = MiniGameMode.LIGHT_REINFORCEMENT,
    estimatedRoundSeconds = 55,
    assetBundleId = "cat-cafe-v1"
)
