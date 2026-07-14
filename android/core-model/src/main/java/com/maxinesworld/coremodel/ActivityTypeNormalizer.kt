package com.maxinesworld.coremodel

import java.util.Locale

enum class ActivityType(val wireName: String) {
    ANIMATED_EXPLANATION("ANIMATED_EXPLANATION_V1"),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE_V1"),
    SORT_AND_CLASSIFY("SORT_AND_CLASSIFY_V1"),
    HOTSPOT_IMAGE("HOTSPOT_IMAGE_V1"),
    MATCHING_PAIRS("MATCHING_PAIRS_V1"),
    SEQUENCE_BUILDER("SEQUENCE_BUILDER_V1"),
    INTERACTIVE_SPEC("INTERACTIVE_SPEC_V1");

    companion object {
        fun parse(raw: String): ActivityType? {
            val key = raw.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .uppercase(Locale.ROOT)

            return when (key) {
                "ANIMATED_EXPLANATION", "ANIMATED_EXPLANATION_V1" -> ANIMATED_EXPLANATION
                "MULTIPLE_CHOICE", "MULTIPLE_CHOICE_V1",
                "STORY_COMPREHENSION", "PREDICTION_OBSERVATION_EXPLANATION" -> MULTIPLE_CHOICE
                "SORT_AND_CLASSIFY", "SORT_AND_CLASSIFY_V1", "TIMELINE_BUILDER" -> SORT_AND_CLASSIFY
                "HOTSPOT_IMAGE", "HOTSPOT_IMAGE_V1" -> HOTSPOT_IMAGE
                "MATCHING_PAIRS", "MATCHING_PAIRS_V1" -> MATCHING_PAIRS
                "SEQUENCE_BUILDER", "SEQUENCE_BUILDER_V1" -> SEQUENCE_BUILDER
                "INTERACTIVE_SPEC", "INTERACTIVE_SPEC_V1" -> INTERACTIVE_SPEC
                else -> null
            }
        }
    }
}

fun canonicalActivityType(raw: String): String? = ActivityType.parse(raw)?.wireName
