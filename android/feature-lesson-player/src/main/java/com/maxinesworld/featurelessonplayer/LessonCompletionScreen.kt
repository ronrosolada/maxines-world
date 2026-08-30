package com.maxinesworld.featurelessonplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.Coral
import com.maxinesworld.coredesignsystem.theme.HeritageGold
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.LeafGreen
import com.maxinesworld.coredesignsystem.theme.LocalAnimationsDisabled
import com.maxinesworld.coredesignsystem.theme.OnGold
import com.maxinesworld.coredesignsystem.theme.SkyBlue
import com.maxinesworld.coredesignsystem.theme.StoryPurple
import com.maxinesworld.coredesignsystem.theme.SuccessGreen
import com.maxinesworld.coredesignsystem.theme.SuccessGreenText
import com.maxinesworld.coredesignsystem.theme.SunshineGold
import com.maxinesworld.coredesignsystem.theme.Teal40
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import com.maxinesworld.featurerewards.LessonRewardPolicy
import com.maxinesworld.featurerewards.SanctuaryCatalog

/**
 * Completion/reward surface retained for the reward-break integration flow.
 * The old lesson-player route and its child-facing entry point were removed;
 * this surface is not a lesson navigator.
 */
@Composable
fun LessonCompleteScreen(state: LessonUiState, onComplete: () -> Unit, onPlayGames: () -> Unit = {}) {
    val scored = state.results.filter { it.scored }
    val correct = scored.count { it.correct }
    val total = scored.size
    val accuracy = if (total > 0) correct.toFloat() / total else 0f
    val calculatedReward = LessonRewardPolicy.forAccuracy(accuracy.toDouble())
    val starsEarned = state.starsEarned.takeIf { it > 0 } ?: calculatedReward.stars
    val coinsEarned = state.coinsEarned.takeIf { it > 0 } ?: calculatedReward.coins

    val reducedMotion = LocalAnimationsDisabled.current
    val confettiCount = if (reducedMotion) 0 else 6
    val confettiColors = if (!reducedMotion) {
        listOf(Coral, SunshineGold, SkyBlue, StoryPurple, LeafGreen, VillageTeal)
    } else {
        emptyList()
    }
    val particles = remember(reducedMotion) {
        List(confettiCount) {
            Offset((Math.random() * 1000).toFloat(), (-Math.random() * 800).toFloat())
        }
    }
    val confettiAnim = rememberConfettiProgress(enabled = !reducedMotion)

    Box(Modifier.fillMaxSize()) {
        if (!reducedMotion) {
            Canvas(Modifier.fillMaxSize()) {
                particles.forEachIndexed { i, pos ->
                    val y = (pos.y + confettiAnim + (i * 37)) % size.height
                    val x = (pos.x + kotlin.math.sin(confettiAnim / 200 + i) * 50) % size.width
                    drawCircle(
                        confettiColors[i % confettiColors.size].copy(alpha = 0.6f),
                        radius = (4 + (i % 5)).toFloat(),
                        center = Offset(x.toFloat(), y),
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            val lang = state.lesson?.languageOfInstruction
            Text(
                lessonUiText(lang, "Lesson Complete!", "Tapos na ang Aralin!"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Teal40,
            )
            Text(
                lessonUiText(lang, "You got $correct out of $total correct!", "Nakuha mo ang $correct sa $total!"),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "${(accuracy * 100).toInt()}%",
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = if (accuracy >= 0.8f) SuccessGreenText else HeritageGold,
            )

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SunshineGold.copy(alpha = 0.1f)),
            ) {
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, lessonUiText(lang, "Stars", "Mga bituin"), tint = SunshineGold, modifier = Modifier.height(28.dp))
                        Text(lessonUiText(lang, "+$starsEarned Learning Stars", "+$starsEarned Bituin sa Pagkatuto"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Pets, sanctuaryTokensDescription(lang), tint = VillageTeal, modifier = Modifier.height(28.dp))
                        Text(lessonUiText(lang, "+$coinsEarned Tokens", "+$coinsEarned Token"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            if (state.activityPawPrintsEarned > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    lessonUiText(lang, "You filled ${state.activityPawPrintsEarned} learning paw prints.", "Napunan mo ang ${state.activityPawPrintsEarned} bakas ng paa sa pagkatuto."),
                    color = Teal40,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }

            if (state.dailyQuestCompleted) {
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LeafGreen.copy(alpha = 0.16f)),
                ) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(lessonUiText(lang, "Daily Quest complete!", "Tapos na ang Pang-araw-araw na Hamon!"), fontWeight = FontWeight.ExtraBold, color = Teal40)
                        Text(
                            state.sanctuaryPieceId
                                ?.let { SanctuaryCatalog.byId(it)?.name }
                                ?.let { sanctuaryGainedDescription(lang, it) }
                                ?: lessonUiText(lang, "Milo's sanctuary gained a new piece.", "Nagkaroon ng bagong gamit ang santuwaryo ni Milo."),
                            textAlign = TextAlign.Center,
                            color = Ink.copy(alpha = 0.78f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            val rewardGateReduce = LocalAnimationsDisabled.current
            val rewardPop by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (state.rewardBreakId != null) 1f else 0.92f,
                animationSpec = if (rewardGateReduce) {
                    androidx.compose.animation.core.snap()
                } else {
                    androidx.compose.animation.core.spring(
                        dampingRatio = 0.62f,
                        stiffness = 520f,
                    )
                },
                label = "rewardGatePop",
            )
            MaxinesPrimaryButton(onClick = onComplete, text = lessonUiText(lang, "Continue", "Magpatuloy"), modifier = Modifier.fillMaxWidth())
            if (state.rewardBreakId != null) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().graphicsLayer(scaleX = rewardPop, scaleY = rewardPop)) {
                    Button(
                        onClick = onPlayGames,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunshineGold,
                            contentColor = OnGold,
                        ),
                    ) {
                        Icon(Icons.Default.SportsEsports, "Games", modifier = Modifier.height(22.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(lessonUiText(lang, "Play a Reward Game", "Maglaro ng Gantimpalang Laro"), fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
