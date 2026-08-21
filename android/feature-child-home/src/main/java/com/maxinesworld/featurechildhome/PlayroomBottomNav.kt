package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardHeader
import com.maxinesworld.coredesignsystem.components.MaxinesQuestCardSurface
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featurerewards.SanctuaryCatalog
import kotlin.math.roundToInt


// ─── Bottom nav (design.md §13) ──────────────────────────────────────

@Composable
internal fun PlayroomBottomNav(
    onHomeClick: () -> Unit,
    onCollectionClick: () -> Unit,
    onParentsClick: () -> Unit,
    compact: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White, RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem("Home", isSelected = true, onClick = onHomeClick, compact = compact)
        NavItem(
            stringResource(R.string.nav_collection),
            isSelected = false,
            onClick = onCollectionClick,
            compact = compact,
            testTag = PlayroomHomeTestTags.Collection,
        )
        NavItem(
            stringResource(R.string.nav_parents),
            isSelected = false,
            onClick = onParentsClick,
            compact = compact,
            testTag = PlayroomHomeTestTags.Parents,
            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = PlayTeal, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    compact: Boolean,
    comingSoon: Boolean = false,
    testTag: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val enabled = onClick != null
    val interaction = remember { MutableInteractionSource() }
    val showComingSoon = comingSoon && !compact
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PlayTeal.copy(alpha = 0.14f) else Color.Transparent)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                if (isSelected) selected = true
                if (!enabled) disabled()
            }
            .then(if (isSelected) Modifier.testTag(PlayroomHomeTestTags.SelectedNavigation) else Modifier)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .focusable(enabled = enabled, interactionSource = interaction)
            .then(
                if (enabled) Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        } else {
            Text("●", color = if (isSelected) PlayTeal else if (enabled) PlayMuted else PlayMuted.copy(alpha = 0.5f), fontSize = 16.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (isSelected) PlayTeal else if (enabled) PlayMuted else PlayMuted.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp, // never below 14sp even compact (design.md §13.2)
            lineHeight = 18.sp,
            maxLines = 1,
        )
        if (showComingSoon) {
            Text(
                stringResource(R.string.home_coming_soon),
                color = PlayMuted.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp, lineHeight = 18.sp,
                maxLines = 1,
            )
        }
        // Active underline indicator
        Box(
            Modifier
                .padding(top = 3.dp)
                .width(if (isSelected) 20.dp else 0.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (isSelected) PlayTeal else Color.Transparent),
        )
    }
}
