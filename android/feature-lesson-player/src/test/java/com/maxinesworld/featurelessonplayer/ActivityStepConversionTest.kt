package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.Month1Activity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStepConversionTest {

    private fun parse(raw: String): JsonElement = Json.parseToJsonElement(raw)

    private fun activity(type: String, contentJson: String, id: String = "a-01") =
        Month1Activity(
            activityId = id,
            sequence = 1,
            type = type,
            instruction = "Do the thing.",
            content = parse(contentJson)
        )

    @Test
    fun `multiple choice options and answer key are parsed`() {
        val step = toActivityStep(
            activity("MULTIPLE_CHOICE", """{"options":["A","B","C"],"correctIndex":0}""")
        )
        assertEquals("MULTIPLE_CHOICE_V1", step.type)
        assertEquals(listOf("A", "B", "C"), step.options)
        assertEquals(0, step.correctIndex)
        assertEquals("Do the thing.", step.question)
    }

    @Test
    fun `out of range correct index is rejected`() {
        val step = toActivityStep(
            activity("MULTIPLE_CHOICE", """{"options":["A","B"],"correctIndex":7}""")
        )
        assertEquals(-1, step.correctIndex)
    }

    @Test
    fun `sort activity yields two categories and all items`() {
        val step = toActivityStep(
            activity(
                "SORT_AND_CLASSIFY",
                """{"fits":["f1","f2","f3"],"doesNotFit":["d1","d2","d3"]}"""
            )
        )
        assertEquals(2, step.sortCategories.size)
        assertEquals(6, step.sortItems.size)
        assertEquals(3, step.sortItems.count { it.categoryIndex == 0 })
        assertEquals(3, step.sortItems.count { it.categoryIndex == 1 })
    }

    @Test
    fun `sort shuffle is stable for the same activity id`() {
        val json = """{"fits":["f1","f2","f3"],"doesNotFit":["d1","d2","d3"]}"""
        val a = toActivityStep(activity("SORT_AND_CLASSIFY", json, id = "same-id"))
        val b = toActivityStep(activity("SORT_AND_CLASSIFY", json, id = "same-id"))
        assertEquals(a.sortItems.map { it.label }, b.sortItems.map { it.label })
    }

    @Test
    fun `matching pairs are parsed`() {
        val step = toActivityStep(
            activity(
                "MATCHING_PAIRS",
                """{"pairs":[{"left":"L1","right":"R1"},{"left":"L2","right":"R2"}]}"""
            )
        )
        assertEquals(2, step.matchPairs.size)
        assertEquals("L1", step.matchPairs[0].left)
    }

    @Test
    fun `sequence steps are parsed`() {
        val step = toActivityStep(
            activity("SEQUENCE_BUILDER", """{"steps":["one","two","three"]}""")
        )
        assertEquals(listOf("one", "two", "three"), step.sequenceSteps)
    }

    @Test
    fun `hotspot examples are parsed`() {
        val step = toActivityStep(
            activity("HOTSPOT_IMAGE", """{"examples":["e1","e2"]}""")
        )
        assertEquals(listOf("e1", "e2"), step.hotspotExamples)
    }

    @Test
    fun `animated explanation uses string content as narration`() {
        val step = toActivityStep(
            activity("ANIMATED_EXPLANATION", "\"The body text.\"")
        )
        assertEquals("The body text.", step.narrationText)
    }

    @Test
    fun `malformed content degrades without throwing`() {
        val step = toActivityStep(
            activity("MULTIPLE_CHOICE", """{"options":"not-an-array"}""")
        )
        assertTrue(step.options.isEmpty())
        assertEquals(-1, step.correctIndex)
    }

    @Test
    fun `every raw type maps to a versioned renderer key`() {
        val expected = mapOf(
            "ANIMATED_EXPLANATION" to "ANIMATED_EXPLANATION_V1",
            "MULTIPLE_CHOICE" to "MULTIPLE_CHOICE_V1",
            "SORT_AND_CLASSIFY" to "SORT_AND_CLASSIFY_V1",
            "HOTSPOT_IMAGE" to "HOTSPOT_IMAGE_V1",
            "MATCHING_PAIRS" to "MATCHING_PAIRS_V1",
            "SEQUENCE_BUILDER" to "SEQUENCE_BUILDER_V1",
            "INTERACTIVE_SPEC" to "INTERACTIVE_SPEC_V1"
        )
        expected.forEach { (raw, versioned) -> assertEquals(versioned, rendererType(raw)) }
    }
}
