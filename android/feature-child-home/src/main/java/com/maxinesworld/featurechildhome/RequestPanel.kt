package com.maxinesworld.featurechildhome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Left lower panel: quest progress + Continue CTA */
@Composable
fun RequestPanel(
    questProgressText: String,
    completed: Int,
    assigned: Int,
    onContinueQuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp),
    ) {
        Text(
            "Mira's request",
            color = Color(0xFF075F63).copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            requestSummary(completed, assigned),
            color = Color(0xFF075F63),
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        // Progress: "N of M quests complete" + paw markers
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$completed of $assigned quests complete",
                color = Color(0xFF34545A),
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(6.dp))
            repeat(assigned.coerceAtMost(3)) { i ->
                Text(
                    if (i < completed) "🐾" else "◦",
                    fontSize = 14.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = onContinueQuest,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(
                "Continue quest →",
                color = Color(0xFFF5A623),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

private fun requestSummary(completed: Int, assigned: Int): String {
    if (assigned == 0) return "No quests assigned yet"
    val remaining = assigned - completed
    return when {
        remaining <= 0 -> "All quests complete! 🎉"
        remaining == 1 -> "1 quest left — you're almost there!"
        else -> "$remaining quests left today"
    }
}
