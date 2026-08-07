package com.maxinesworld.engineactivity.renderers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.OnGold
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coremodel.ActivityStep

/** Displays authored hint text and invokes the lesson narration callback. */
@Composable
internal fun ActivityHint(
    step: ActivityStep,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (step.hintText.isBlank()) return

    var hintVisible by remember(step.id) { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        MaxinesPrimaryButton(
            onClick = {
                hintVisible = true
                onHint()
            },
            text = if (hintVisible) "Read hint again" else "Hint",
            containerColor = SunshineGold,
            contentColor = OnGold,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp)
                .semantics { contentDescription = "Get a hint" },
        )
        if (hintVisible) {
            Text(
                text = "Hint: ${step.hintText}",
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .semantics { contentDescription = "Hint: ${step.hintText}" },
            )
        }
    }
}
