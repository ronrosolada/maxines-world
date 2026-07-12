package com.maxinesworld.engineactivity

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.maxinesworld.coremodel.ActivityStep

data class ActivityResult(
    val activityId: String,
    val correct: Boolean,
    val attempts: Int,
    val hintsUsed: Int,
    val responseTimeMs: Long
)

typealias ActivityEngine = @Composable (
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier
) -> Unit
