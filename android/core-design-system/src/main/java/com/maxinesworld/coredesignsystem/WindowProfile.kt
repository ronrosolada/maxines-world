package com.maxinesworld.coredesignsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowProfile {
    COMPACT,
    MEDIUM,
    EXPANDED,
    LARGE_TABLET
}

fun windowProfileForWidth(width: Dp): WindowProfile = when {
    width < 600.dp -> WindowProfile.COMPACT
    width < 840.dp -> WindowProfile.MEDIUM
    width < 1200.dp -> WindowProfile.EXPANDED
    else -> WindowProfile.LARGE_TABLET
}

val WindowProfile.isWide: Boolean
    get() = this == WindowProfile.EXPANDED || this == WindowProfile.LARGE_TABLET

val WindowProfile.pageMargin: Dp
    get() = when (this) {
        WindowProfile.COMPACT -> 16.dp
        WindowProfile.MEDIUM -> 24.dp
        WindowProfile.EXPANDED, WindowProfile.LARGE_TABLET -> 32.dp
    }

val WindowProfile.primaryControlHeight: Dp
    get() = when (this) {
        WindowProfile.COMPACT -> 56.dp
        WindowProfile.MEDIUM, WindowProfile.EXPANDED, WindowProfile.LARGE_TABLET -> 64.dp
    }
