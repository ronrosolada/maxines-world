package com.maxinesworld.gamepawbeats

import com.maxinesworld.engineminigame.MiniGameDefinition
import com.maxinesworld.engineminigame.MiniGameMode

val PawBeatsDefinition = MiniGameDefinition(
    id = PawBeatsViewModel.GAME_ID,
    title = "Paw Beats",
    mode = MiniGameMode.CREATIVE,
    estimatedRoundSeconds = 40,
    assetBundleId = "paw-beats-v1"
)
