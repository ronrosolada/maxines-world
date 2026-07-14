package com.maxinesworld.featurechildhome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Upper-right status rail: three bamboo pills */
@Composable
fun StatusRail(
    fishTreats: Int,
    completed: Int,
    assigned: Int,
    playgroundStatus: String,
    playgroundLabel: String,
    onPlaygroundClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        StatusPill(
            iconRes = R.drawable.mw_ic_coin,
            value = "$fishTreats",
            label = "Fish Treats",
            tint = Color(0xFFF5A623),
        )
        StatusPill(
            iconRes = R.drawable.mw_ic_progress,
            value = "$completed/$assigned",
            label = "Today",
            tint = Color(0xFF3A6B63),
        )
        PlaygroundPill(
            status = playgroundStatus,
            label = playgroundLabel,
            onClick = onPlaygroundClick,
        )
    }
}

@Composable
private fun StatusPill(
    iconRes: Int,
    value: String,
    label: String,
    tint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(Color(0xFFF5F0E8).copy(alpha = 0.9f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(iconRes), null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF075F63))
            Text(label, fontSize = 11.sp, color = Color(0xFF34545A))
        }
    }
}

@Composable
private fun PlaygroundPill(
    status: String,
    label: String,
    onClick: () -> Unit,
) {
    val iconRes = if (status == "Open") R.drawable.ic_playground_open else R.drawable.ic_playground_closed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(Color(0xFFF5F0E8).copy(alpha = 0.9f), RoundedCornerShape(10.dp))
            .semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(iconRes), null,
            tint = Color(0xFF3A6B63),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(status, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF075F63))
            Text(label, fontSize = 11.sp, color = Color(0xFF34545A))
        }
    }
}
