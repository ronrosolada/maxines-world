package com.maxinesworld.featurechildhome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Left upper panel: Mira portrait + greeting + mood line */
@Composable
fun GreetingPanel(
    childName: String,
    carnivalPhrase: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mira portrait: 56dp circular crop with 3dp warm-cream outline
        Image(
            painter = painterResource(R.drawable.mw_ic_profile),
            contentDescription = "Mira",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Hi, $childName!",
                color = Color(0xFF075F63),
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                maxLines = 1,
            )
            Text(
                carnivalPhrase,
                color = Color(0xFF34545A),
                fontSize = 14.sp,
                maxLines = 2,
            )
        }
    }
}
