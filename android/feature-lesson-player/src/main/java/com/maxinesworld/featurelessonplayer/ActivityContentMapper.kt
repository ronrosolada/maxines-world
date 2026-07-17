package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.Month1Activity
import com.maxinesworld.engineactivity.lessonUiStrings
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps rich Month1 activity `content` into the flat [ActivityStep] fields the renderers use.
 *
 * Encoding conventions:
 * - MULTIPLE_CHOICE: options[] + correctIndex
 * - SORT_AND_CLASSIFY: [cat0, cat1, item0, item1, ...] interleaved so item i → cat (i % 2)
 * - MATCHING_PAIRS: lefts then rights (same index = pair)
 * - SEQUENCE_BUILDER: steps in correct order
 * - HOTSPOT_IMAGE: labels; correctIndex=-1 means visit-all (renderer handles)
 * - ANIMATED_EXPLANATION: body text returned as narrationBody
 */
data class MappedActivityContent(
    val narrationText: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val imageAssets: List<String> = emptyList(),
)

fun mapActivityContent(
    act: Month1Activity,
    canonicalType: String,
    language: String?,
): MappedActivityContent {
    val instruction = act.instruction.trim()
    val content = act.content
    val strings = lessonUiStrings(language)
    val visual = content.extractVisualScene()
    val celebration = content.extractCelebration()
    val images = buildList {
        if (visual != null) add(visual)
        if (celebration != null) add(celebration)
        act.assetId?.let { add("asset:$it") }
    }

    return when (canonicalType) {
        "ANIMATED_EXPLANATION_V1" -> {
            val body = content.asStringBody() ?: instruction
            MappedActivityContent(
                narrationText = body,
                question = instruction,
                options = emptyList(),
                correctIndex = -1,
                imageAssets = images,
            )
        }
        "MULTIPLE_CHOICE_V1" -> {
            val obj = content.asObject()
            val options = obj.stringList("options")
                .ifEmpty { obj.stringList("choices") }
            val correctIndex = obj.intValue("correctIndex")
                ?: obj.correctIndexFromChoices()
                ?: 0
            MappedActivityContent(
                narrationText = instruction,
                question = instruction,
                options = options,
                correctIndex = correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
                imageAssets = images,
            )
        }
        "SORT_AND_CLASSIFY_V1" -> {
            val obj = content.asObject()
            val fits = obj.stringList("fits")
            val doesNotFit = obj.stringList("doesNotFit")
            val lower = instruction.lowercase()
            val cat0 = obj.stringValue("categoryFits")
                ?: when {
                    "pantangi" in lower || "pambalana" in lower -> "Pantangi"
                    else -> strings.categoryFits
                }
            val cat1 = obj.stringValue("categoryDoesNotFit")
                ?: when {
                    "pantangi" in lower || "pambalana" in lower -> "Pambalana"
                    else -> strings.categoryDoesNotFit
                }
            val items = (fits + doesNotFit).ifEmpty {
                obj.stringList("items").ifEmpty { obj.stringList("options") }
            }
            val cat0Count = if (fits.isNotEmpty() || doesNotFit.isNotEmpty()) fits.size else items.size / 2
            MappedActivityContent(
                narrationText = instruction,
                question = instruction,
                options = listOf(cat0, cat1) + items,
                correctIndex = cat0Count,
                imageAssets = images,
            )
        }
        "MATCHING_PAIRS_V1" -> {
            val obj = content.asObject()
            val pairs = obj?.get("pairs") as? JsonArray
            val lefts = mutableListOf<String>()
            val rights = mutableListOf<String>()
            if (pairs != null) {
                for (el in pairs) {
                    val p = el as? JsonObject ?: continue
                    lefts += p.stringValue("left").orEmpty()
                    rights += p.stringValue("right").orEmpty()
                }
            }
            MappedActivityContent(
                narrationText = instruction,
                question = instruction,
                options = lefts + rights,
                correctIndex = -1,
                imageAssets = images,
            )
        }
        "SEQUENCE_BUILDER_V1" -> {
            val obj = content.asObject()
            val steps = obj.stringList("steps").ifEmpty { obj.stringList("items") }
            MappedActivityContent(
                narrationText = instruction,
                question = instruction,
                options = steps,
                correctIndex = -1,
                imageAssets = images,
            )
        }
        "HOTSPOT_IMAGE_V1" -> {
            val obj = content.asObject()
            val targets = obj?.get("targets") as? JsonArray
            val labels = mutableListOf<String>()
            if (targets != null) {
                for (el in targets) {
                    val t = el as? JsonObject ?: continue
                    labels += t.stringValue("label")
                        ?: t.stringValue("id")
                        ?: "Target ${labels.size + 1}"
                }
            }
            if (labels.isEmpty()) {
                labels += obj.stringList("examples").map {
                    it.substringBefore("—").substringBefore("-").trim()
                }.filter { it.isNotBlank() }
            }
            MappedActivityContent(
                narrationText = instruction,
                question = instruction,
                options = labels.ifEmpty { listOf("1", "2", "3") },
                correctIndex = -1,
                imageAssets = images.ifEmpty { listOf("🖼️🌟🐱") },
            )
        }
        else -> MappedActivityContent(
            narrationText = instruction,
            question = instruction,
            options = emptyList(),
            correctIndex = -1,
            imageAssets = images,
        )
    }
}

private fun JsonElement?.extractVisualScene(): String? {
    val obj = asObject() ?: return null
    return obj.stringValue("visualScene") ?: obj.stringValue("sceneEmoji")
}

private fun JsonElement?.extractCelebration(): String? {
    val obj = asObject() ?: return null
    return obj.stringValue("celebrationEmoji")
}

private fun JsonElement?.asObject(): JsonObject? = when (this) {
    is JsonObject -> this
    else -> null
}

private fun JsonElement?.asStringBody(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.isNotBlank() }
    is JsonObject -> stringValue("text") ?: stringValue("body") ?: stringValue("explanation")
    else -> null
}

private fun JsonObject?.stringList(key: String): List<String> {
    if (this == null) return emptyList()
    val el = this[key] ?: return emptyList()
    return when (el) {
        is JsonArray -> el.mapNotNull {
            when (it) {
                is JsonPrimitive -> it.contentOrNull
                is JsonObject -> it.stringValue("text") ?: it.stringValue("label")
                else -> null
            }
        }.filter { it.isNotBlank() }
        is JsonPrimitive -> listOfNotNull(el.contentOrNull?.takeIf { it.isNotBlank() })
        else -> emptyList()
    }
}

private fun JsonObject?.stringValue(key: String): String? {
    if (this == null) return null
    val el = this[key] as? JsonPrimitive ?: return null
    return el.contentOrNull?.takeIf { it.isNotBlank() }
}

private fun JsonObject?.intValue(key: String): Int? {
    if (this == null) return null
    val el = this[key] as? JsonPrimitive ?: return null
    return el.intOrNull
}

/** Support assessment-style choices: [{text, correct:true}, ...] if embedded in content. */
private fun JsonObject?.correctIndexFromChoices(): Int? {
    if (this == null) return null
    val choices = this["choices"] as? JsonArray ?: return null
    choices.forEachIndexed { index, el ->
        val obj = el as? JsonObject ?: return@forEachIndexed
        val correct = (obj["correct"] as? JsonPrimitive)?.contentOrNull
        if (correct == "true") return index
        // boolean true without quotes
        if ((obj["correct"] as? JsonPrimitive)?.content == "true") return index
    }
    // also try boolean via toString
    choices.forEachIndexed { index, el ->
        val obj = el as? JsonObject ?: return@forEachIndexed
        val prim = obj["correct"] as? JsonPrimitive
        if (prim != null && prim.content.equals("true", ignoreCase = true)) return index
    }
    return null
}
