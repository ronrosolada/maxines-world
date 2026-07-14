package com.maxinesworld.gamefireflycatch

import com.maxinesworld.engineminigame.MiniGameDefinition
import com.maxinesworld.engineminigame.MiniGameMode

val FireflyCatchDefinition = MiniGameDefinition(
    id = FireflyCatchViewModel.GAME_ID,
    title = "Firefly Garden",
    mode = MiniGameMode.PLAYFUL,
    estimatedRoundSeconds = 45,
    assetBundleId = "firefly-catch-v1"
)
