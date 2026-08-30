package com.maxinesworld.engineactivity.renderers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.engineactivity.ActivityResult

/**
 * WRITING_PRODUCTION_V1 (CH-07 M2): the child builds a sentence from word
 * tiles, then self-marks a small writing checklist with the guide.
 *
 * No free-text grading — G3 production is assessed against the checklist,
 * consistent with the offline/privacy-first contract. Completion is
 * checklist-derived; the sentence order provides the learning feedback.
 */

/** Seeded display shuffle so retries and tests are deterministic. */
internal fun writingTileDisplayOrder(id: String, itemCount: Int): List<Int> =
    (0 until itemCount).shuffled(java.util.Random(id.hashCode().toLong()))

/** The sentence is correct when every tile is placed in its original order. */
internal fun writingSentenceIsCorrect(placed: List<Int>, itemCount: Int): Boolean =
    placed.size == itemCount && placed == (0 until itemCount).toList()

internal fun writingSubmitEnabled(placedCount: Int, itemCount: Int, submitted: Boolean): Boolean =
    !submitted && placedCount == itemCount

internal fun writingActionEnabled(
    placedCount: Int,
    itemCount: Int,
    submitted: Boolean,
    isCorrect: Boolean,
    attempts: Int,
): Boolean =
    (!submitted && placedCount == itemCount) ||
        (submitted && !isCorrect && attempts < 3)

internal fun writingUiText(language: String?, english: String, filipino: String): String =
    if (language?.startsWith("fil", ignoreCase = true) == true) filipino else english

internal fun writingActionLabel(
    placedCount: Int,
    itemCount: Int,
    submitted: Boolean,
    isCorrect: Boolean,
    attempts: Int,
    language: String? = null,
): String = when {
    submitted && isCorrect -> writingUiText(language, "Great job!", "Magaling!")
    submitted && attempts >= 3 -> writingUiText(language, "See the sentence →", "Tingnan ang pangungusap →")
    submitted -> writingUiText(language, "Try Again", "Subukan muli")
    placedCount < itemCount -> writingUiText(language, "Place all words ($placedCount/$itemCount)", "Ilagay ang lahat ng salita ($placedCount/$itemCount)")
    else -> writingUiText(language, "Check My Sentence", "Suriin ang aking pangungusap")
}

/** Every checklist item self-marked as done completes the production task. */
internal fun writingChecklistComplete(marks: List<Boolean>): Boolean =
    marks.isNotEmpty() && marks.all { it }

/** Both interactive halves require authored content to render safely. */
internal fun writingProductionContentIsRenderable(tileCount: Int, checklistCount: Int): Boolean =
    tileCount > 0 && checklistCount > 0

@Composable
fun WritingProductionRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
    language: String? = null,
) {
    val startTime = remember { System.currentTimeMillis() }
    var attempts by remember { mutableIntStateOf(0) }
    var placed by remember { mutableStateOf(listOf<Int>()) }
    var submitted by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var checklistMarks by remember { mutableStateOf(listOf<Boolean>()) }
    var finished by remember { mutableStateOf(false) }
    val animationsDisabled = LocalAnimationsDisabled.current

    val tiles: List<String> = step.writingTiles.ifEmpty { step.options }
    val checklist: List<String> = step.writingChecklist
    val displayOrder = remember(step.id) {
        writingTileDisplayOrder(step.id, tiles.size)
    }
    val available = displayOrder.filter { it !in placed }
    val showChecklist = submitted && (isCorrect || attempts >= 3)

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LessonVisual(step)

        Text(
            step.question.ifEmpty {
                writingUiText(language, "Build the sentence:", "Buuin ang pangungusap:")
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                contentDescription = writingUiText(language, "Writing: ${step.question}", "Pagsulat: ${step.question}")
            },
        )

        Text(writingUiText(language, "Words:", "Mga salita:"), style = MaterialTheme.typography.labelLarge, color = VillageTeal)
        available.forEach { idx ->
            Row(
                Modifier.fillMaxWidth().sizeIn(minHeight = 44.dp).clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainer)
                    .clickable(enabled = !submitted && !finished, role = Role.Button) { placed = placed + idx }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics {
                        contentDescription = writingUiText(
                            language,
                            "Word: ${tiles[idx]} — tap to add",
                            "Salita: ${tiles[idx]} — pindutin para idagdag",
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tiles[idx], style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(writingUiText(language, "My sentence:", "Aking pangungusap:"), style = MaterialTheme.typography.labelLarge, color = OnGold)
        placed.forEachIndexed { pos, idx ->
            val bg by animateColorAsState(
                when {
                    submitted && isCorrect -> SuccessGreen.copy(alpha = 0.15f)
                    submitted && !isCorrect && pos != idx -> ErrorRed.copy(alpha = 0.1f)
                    else -> SunshineGold.copy(alpha = 0.08f)
                },
                animationSpec = if (animationsDisabled) snap() else tween(180),
                label = "wp$idx",
            )
            Row(
                Modifier.fillMaxWidth().sizeIn(minHeight = 44.dp).clip(RoundedCornerShape(10.dp)).background(bg)
                    .clickable(enabled = !submitted && !finished, role = Role.Button) { placed = placed.filter { it != idx } }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics {
                        contentDescription = writingUiText(
                            language,
                            "Position ${pos + 1}: ${tiles[idx]} — tap to remove",
                            "Puwesto ${pos + 1}: ${tiles[idx]} — pindutin para alisin",
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tiles[idx], style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                if (!submitted) Text("✕", color = OnCoral, style = MaterialTheme.typography.labelSmall)
            }
        }

        ActivityHint(step = step, onHint = onHint)

        if (showChecklist) {
            Text(writingUiText(language, "Check your own writing:", "Suriin ang iyong pagsulat:"), style = MaterialTheme.typography.labelLarge, color = VillageTeal)
            checklist.forEachIndexed { index, item ->
                val marked = checklistMarks.getOrNull(index) == true
                Row(
                    Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (marked) SuccessGreen.copy(alpha = 0.15f) else SurfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        if (marked) writingUiText(language, "Yes, I did! ✓", "Oo, ginawa ko! ✓")
                        else writingUiText(language, "Not yet", "Hindi pa"),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (marked) SuccessGreenText else OnCoral,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !finished, role = Role.Button) {
                                checklistMarks = checklistMarks.toMutableList().also { list ->
                                    while (list.size <= index) list += false
                                    list[index] = !list[index]
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .semantics {
                                contentDescription = if (marked) {
                                    writingUiText(language, "$item — marked done", "$item — namarkahan na")
                                } else {
                                    writingUiText(language, "$item — mark as done", "$item — markahan bilang tapos")
                                }
                            },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (!showChecklist) {
            val retryEnabled = submitted && !isCorrect && attempts < 3
            val submitEnabled = writingActionEnabled(
                placedCount = placed.size,
                itemCount = tiles.size,
                submitted = submitted,
                isCorrect = isCorrect,
                attempts = attempts,
            )
            val actionLabel = writingActionLabel(placed.size, tiles.size, submitted, isCorrect, attempts, language)
            Box(
                Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            submitted && isCorrect -> SuccessGreen
                            !submitEnabled -> SurfaceContainer
                            else -> VillageTeal
                        },
                    )
                    .clickable(enabled = submitEnabled, role = Role.Button) {
                        if (retryEnabled) {
                            submitted = false
                            placed = emptyList()
                        } else {
                            submitted = true
                            isCorrect = writingSentenceIsCorrect(placed, tiles.size)
                            if (!isCorrect) attempts += 1
                        }
                    }
                    .padding(vertical = 12.dp)
                    .semantics { contentDescription = actionLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = MaterialTheme.typography.titleSmall, color = Ink)
            }
        } else {
            val checklistDone = writingChecklistComplete(checklistMarks)
            Box(
                Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (checklistDone) SuccessGreen else SurfaceContainer)
                    .clickable(enabled = checklistDone && !finished, role = Role.Button) {
                        finished = true
                        onResult(
                            ActivityResult(
                                activityId = step.id,
                                correct = isCorrect,
                                attempts = attempts,
                                hintsUsed = 0,
                                responseTimeMs = System.currentTimeMillis() - startTime,
                                scored = false,
                            ),
                        )
                    }
                    .padding(vertical = 12.dp)
                    .semantics {
                        contentDescription = if (checklistDone) {
                            writingUiText(language, "All done", "Tapos na")
                        } else {
                            writingUiText(language, "Mark every check item first", "Markahan muna ang lahat ng tsek")
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (checklistDone) writingUiText(language, "All done! 🎉", "Tapos na! 🎉")
                    else writingUiText(language, "Mark every check item first", "Markahan muna ang lahat ng tsek"),
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                )
            }
        }
    }
}
