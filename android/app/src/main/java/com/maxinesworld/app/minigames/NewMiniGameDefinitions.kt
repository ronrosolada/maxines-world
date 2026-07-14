package com.maxinesworld.app.minigames

import com.maxinesworld.engineminigame.MiniGameDefinition
import com.maxinesworld.gamefireflycatch.FireflyCatchDefinition
import com.maxinesworld.gamekittenmatch.KittenMatchDefinition
import com.maxinesworld.gamepawbeats.PawBeatsDefinition

/** Add these to the existing registry; do not replace existing Cat Cafe or Parkour definitions. */
val NewMiniGameDefinitions: List<MiniGameDefinition> = listOf(
    KittenMatchDefinition,
    FireflyCatchDefinition, // title is Firefly Garden; legacy ID retained for compatibility
    PawBeatsDefinition,
)
