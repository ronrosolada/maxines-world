package com.maxinesworld.gamekittenmatch

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GameLayoutMode {
    PhonePortrait,
    PhoneLandscape,
    TabletMedium,
    TabletExpanded
}

@Immutable
data class GameLayoutMetrics(
    val outerPadding: Dp,
    val sectionSpacing: Dp,
    val minimumControlSize: Dp,
    val preferredGameTargetSize: Dp
)

fun gameLayoutMode(maxWidth: Dp, maxHeight: Dp): GameLayoutMode = when {
    maxHeight < 480.dp && maxWidth > maxHeight -> GameLayoutMode.PhoneLandscape
    maxWidth < 600.dp -> GameLayoutMode.PhonePortrait
    maxWidth >= 840.dp && maxHeight >= 600.dp -> GameLayoutMode.TabletExpanded
    else -> GameLayoutMode.TabletMedium
}

fun GameLayoutMode.metrics(): GameLayoutMetrics = when (this) {
    GameLayoutMode.PhonePortrait,
    GameLayoutMode.PhoneLandscape -> GameLayoutMetrics(
        outerPadding = 8.dp,
        sectionSpacing = 8.dp,
        minimumControlSize = 48.dp,
        preferredGameTargetSize = 72.dp
    )
    GameLayoutMode.TabletMedium -> GameLayoutMetrics(
        outerPadding = 16.dp,
        sectionSpacing = 12.dp,
        minimumControlSize = 48.dp,
        preferredGameTargetSize = 96.dp
    )
    GameLayoutMode.TabletExpanded -> GameLayoutMetrics(
        outerPadding = 24.dp,
        sectionSpacing = 16.dp,
        minimumControlSize = 48.dp,
        preferredGameTargetSize = 112.dp
    )
}
