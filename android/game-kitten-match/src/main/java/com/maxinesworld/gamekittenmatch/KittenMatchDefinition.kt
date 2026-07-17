package com.maxinesworld.gamekittenmatch

import com.maxinesworld.engineminigame.MiniGameDefinition
import com.maxinesworld.engineminigame.MiniGameMode

val KittenMatchDefinition = MiniGameDefinition(
    id = KittenMatchViewModel.GAME_ID,
    title = "Kitten Match",
    mode = MiniGameMode.LIGHT_REINFORCEMENT,
    estimatedRoundSeconds = 50,
    assetBundleId = "kitten-match-v1"
)
