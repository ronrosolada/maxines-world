package com.maxinesworld.engineactivity.renderers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.engineactivity.ActivityResult

/**
 * Type-safe dispatcher: routes each [ActivityStep.type] to its renderer.
 * Add new renderer branches here when adding capability types.
 */
@Composable
fun ActivityRenderer(
    step: ActivityStep,
    onResult: (ActivityResult) -> Unit,
    onHint: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (step.type) {
        "ANIMATED_EXPLANATION_V1" ->
            AnimatedExplanationRenderer(step, onResult, onHint, modifier)
        "MULTIPLE_CHOICE_V1" ->
            MultipleChoiceRenderer(step, onResult, onHint, modifier)
        "SORT_AND_CLASSIFY_V1" ->
            SortAndClassifyRenderer(step, onResult, onHint, modifier)
        "HOTSPOT_IMAGE_V1" ->
            HotspotImageRenderer(step, onResult, onHint, modifier)
        "MATCHING_PAIRS_V1" ->
            MatchingPairsRenderer(step, onResult, onHint, modifier)
        "SEQUENCE_BUILDER_V1" ->
            SequenceBuilderRenderer(step, onResult, onHint, modifier)
        "INTERACTIVE_SPEC_V1" ->
            InteractiveSpecRenderer(step, onResult, onHint, modifier)
        else -> throw UnsupportedActivityError(step.type)
    }
}

class UnsupportedActivityError(type: String) :
    IllegalStateException("Unsupported activity type: $type")
