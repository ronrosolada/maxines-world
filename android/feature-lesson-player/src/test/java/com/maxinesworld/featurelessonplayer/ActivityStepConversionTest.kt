package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.AssessmentItem
import com.maxinesworld.coremodel.Month1Activity
import com.maxinesworld.coremodel.CompletionRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStepConversionTest {

    private fun parse(raw: String): JsonElement = Json.parseToJsonElement(raw)

    private fun activity(
        type: String,
        contentJson: String,
        id: String = "a-01",
        completionRule: CompletionRule? = null
    ) =
        Month1Activity(
            activityId = id,
            sequence = 1,
            type = type,
            instruction = "Do the thing.",
            content = parse(contentJson),
            completionRule = completionRule
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
    fun `authored hint text is carried into the renderer step`() {
        val step = toActivityStep(
            activity(
                "MULTIPLE_CHOICE",
                """{"options":["A","B"],"correctIndex":0,"hint":"Look at the first word."}"""
            )
        )
        assertEquals("Look at the first word.", step.hintText)
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
    fun `multi-target hotspot completion rule is carried into the renderer step`() {
        val step = toActivityStep(
            activity(
                "HOTSPOT_IMAGE",
                """{"examples":["e1","e2","e3"]}""",
                completionRule = CompletionRule("ALL_TARGETS_VISITED", targetCount = 3)
            )
        )

        assertEquals("ALL_TARGETS_VISITED", step.completionRule)
        assertEquals(3, step.completionTargetCount)
    }

    @Test
    fun `animated explanation uses string content as narration`() {
        val step = toActivityStep(
            activity("ANIMATED_EXPLANATION", "\"The body text.\"")
        )
        assertEquals("The body text.", step.narrationText)
    }

    @Test
    fun `duplicate narration is suppressed from lesson chrome`() {
        val instruction = "Match each example to the lesson idea."
        val step = ActivityStep(
            id = "duplicate-instruction",
            type = "MATCHING_PAIRS_V1",
            narrationText = instruction,
            question = instruction,
        )

        assertFalse(shouldShowNarrationCard(step))
    }

    @Test
    fun `distinct narration remains available as lesson context`() {
        val step = ActivityStep(
            id = "distinct-instruction",
            type = "MATCHING_PAIRS_V1",
            narrationText = "Milo found three examples in the garden.",
            question = "Match each example to the lesson idea.",
        )

        assertTrue(shouldShowNarrationCard(step))
    }

    // ─── Assessment delivery (adversarial review: authored checks were never played) ───

    private fun assessmentItem(
        id: String = "lesson-q01",
        prompt: String = "Alin ang simuno?",
        optionsJson: String = """[{"id":"a","text":"Si Ana"},{"id":"b","text":"ay nagbabasa"},{"id":"c","text":"Ang aklat"},{"id":"d","text":"Ang aso"}]""",
        correctIds: List<String> = listOf("a"),
        explanation: String = "Ang simuno ay si Ana."
    ) = AssessmentItem(
        itemId = id, sequence = 1, type = "MULTIPLE_CHOICE",
        prompt = prompt,
        options = parse(optionsJson),
        correctOptionIds = correctIds,
        explanation = explanation
    )

    @Test
    fun `assessment item becomes a playable scored multiple choice step`() {
        val step = toAssessmentStep(assessmentItem())

        assertEquals("ASSESSMENT_V1", step.type)
        assertEquals("assessment-lesson-q01", step.id)
        assertEquals("Alin ang simuno?", step.question)
        assertEquals(listOf("Si Ana", "ay nagbabasa", "Ang aklat", "Ang aso"), step.options)
        assertEquals(0, step.correctIndex)
        assertEquals("Ang simuno ay si Ana.", step.feedback?.correct)
        assertEquals("Ang simuno ay si Ana.", step.feedback?.incorrect)
    }

    @Test
    fun `assessment option order is preserved and key maps by option id`() {
        val step = toAssessmentStep(
            assessmentItem(
                optionsJson = """[{"id":"a","text":"Una"},{"id":"b","text":"Pangalawa"},{"id":"c","text":"Pangatlo"}]""",
                correctIds = listOf("c")
            )
        )
        assertEquals(listOf("Una", "Pangalawa", "Pangatlo"), step.options)
        assertEquals(2, step.correctIndex)
    }

    @Test
    fun `assessment with missing key or malformed options degrades safely`() {
        val noKey = toAssessmentStep(assessmentItem(correctIds = emptyList()))
        assertEquals(-1, noKey.correctIndex)
        assertEquals(4, noKey.options.size)

        val malformed = toAssessmentStep(assessmentItem(optionsJson = "\"not-an-array\""))
        assertEquals(emptyList<String>(), malformed.options)
        assertEquals(-1, malformed.correctIndex)
        // Explanation is still authored, so feedback stays subject-specific.
        assertEquals("Ang simuno ay si Ana.", malformed.feedback?.correct)
    }

    @Test
    fun `assessment item with blank explanation falls back to default feedback`() {
        val step = toAssessmentStep(assessmentItem(explanation = ""))
        assertEquals("Great job! 🎉", step.feedback?.correct)
        assertEquals("Let's try again! 💪", step.feedback?.incorrect)
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
