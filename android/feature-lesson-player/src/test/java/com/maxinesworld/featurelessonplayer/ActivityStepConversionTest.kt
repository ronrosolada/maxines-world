package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.AssessmentItem
import com.maxinesworld.coremodel.CompletionRule
import com.maxinesworld.coremodel.DEFAULT_INCORRECT_FEEDBACK
import com.maxinesworld.coremodel.Month1Activity
import com.maxinesworld.coremodel.Month1ActivityFeedback
import com.maxinesworld.coredesignsystem.components.AnswerCardState
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
        completionRule: CompletionRule? = null,
        instruction: String = "Do the thing.",
        assetId: String? = null,
        feedback: Month1ActivityFeedback? = null,
    ) =
        Month1Activity(
            activityId = id,
            sequence = 1,
            type = type,
            instruction = instruction,
            content = parse(contentJson),
            completionRule = completionRule,
            feedback = feedback,
            assetId = assetId,
        )

    @Test
    fun `multiple choice options and answer key are parsed`() {
        val step = toActivityStep(
            activity("MULTIPLE_CHOICE", """{"options":["A","B","C"],"correctIndex":0,"hint":"Look for the key idea."}""")
        )
        assertEquals("MULTIPLE_CHOICE_V1", step.type)
        assertEquals(listOf("A", "B", "C"), step.options)
        assertEquals(0, step.correctIndex)
        assertEquals("Do the thing.", step.question)
        assertEquals("Look for the key idea.", step.hintText)
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
    fun `video activity carries its media id and remains unscored`() {
        val step = toActivityStep(
            activity(
                "VIDEO",
                """{"mediaId":"kids-tagalog-07-colors"}""",
                instruction = "Watch the colors lesson.",
            )
        )

        assertEquals("VIDEO_V1", step.type)
        assertEquals("kids-tagalog-07-colors", step.mediaId)
        assertEquals("Watch the colors lesson.", step.question)
        assertFalse(step.scored)
    }

    @Test
    fun `out of range correct index is rejected`() {
        val step = toActivityStep(
            activity("MULTIPLE_CHOICE", """{"options":["A","B"],"correctIndex":7}""")
        )
        assertEquals(-1, step.correctIndex)
    }

    @Test
    fun `authored jargon retry is sanitized into child-facing feedback`() {
        val step = toActivityStep(
            activity(
                "MULTIPLE_CHOICE",
                """{"options":["A","B"],"correctIndex":0}""",
                feedback = Month1ActivityFeedback(
                    retry = "Read the skill rule again. B and C do not show the skill."
                ),
            )
        )

        assertFalse(step.feedback!!.incorrect.contains("show the skill", ignoreCase = true))
        assertTrue(step.feedback!!.incorrect.contains("show what we learned", ignoreCase = true))
    }

    @Test
    fun `missing authored retry names the correct answer instead of generic prompt`() {
        val step = toActivityStep(
            activity("MULTIPLE_CHOICE", """{"options":["A","B"],"correctIndex":0}""")
        )

        assertEquals("Not quite. The answer is \"A\".", step.feedback?.incorrect)
    }

    @Test
    fun `missing authored retry for Filipino names the correct answer in Filipino`() {
        val step = toActivityStep(
            activity(
                "MULTIPLE_CHOICE",
                """{"options":["A","B"],"correctIndex":1}""",
                id = "filipino-g3-q1-w01-d01-a04",
            )
        )

        assertEquals("Hindi pa tama. Ang sagot ay \"B\".", step.feedback?.incorrect)
    }

    @Test
    fun `authored jargon success feedback is sanitized before rendering`() {
        val step = toActivityStep(
            activity(
                "SORT_AND_CLASSIFY",
                """{"fits":["A"],"doesNotFit":["B"]}""",
                feedback = Month1ActivityFeedback(
                    correct = "Your groups follow the skill rule."
                ),
            )
        )

        assertEquals("Your groups match what we learned.", step.feedback?.correct)
    }

    @Test
    fun `sort activity yields two categories and all items`() {
        val step = toActivityStep(
            activity(
                "SORT_AND_CLASSIFY",
                """{"fits":["f1","f2","f3"],"doesNotFit":["d1","d2","d3"]}""",
                id = "filipino-g3-q1-w01-d01-a03"
            )
        )
        assertEquals(2, step.sortCategories.size)
        assertEquals(listOf("Angkop", "Hindi angkop"), step.sortCategories)
        assertEquals(6, step.sortItems.size)
        assertEquals(3, step.sortItems.count { it.categoryIndex == 0 })
        assertEquals(3, step.sortItems.count { it.categoryIndex == 1 })
        assertTrue(step.feedback?.incorrect?.contains("maling kahon") == true)
        // Practice activities are never scored; only assessment counts (CH-04).
        assertEquals(false, step.scored)
    }

    @Test
    fun `sort prompt uses the same vocabulary as its category labels`() {
        val step = toActivityStep(
            activity(
                "SORT_AND_CLASSIFY",
                """{"fits":["f1"],"doesNotFit":["d1"]}""",
                instruction = "Sort each card into “shows the skill” or “does not show the skill.”"
            )
        )

        assertEquals("Sort each card into “Fits” or “Does not fit.”", step.question)
        assertEquals(step.question, step.narrationText)
        assertTrue(step.question.contains(step.sortCategories[0]))
        assertTrue(step.question.contains(step.sortCategories[1]))
    }

    @Test
    fun `sort shuffle is stable for the same activity id`() {
        val json = """{"fits":["f1","f2","f3"],"doesNotFit":["d1","d2","d3"]}"""
        val a = toActivityStep(activity("SORT_AND_CLASSIFY", json, id = "same-id"))
        val b = toActivityStep(activity("SORT_AND_CLASSIFY", json, id = "same-id"))
        assertEquals(a.sortItems.map { it.label }, b.sortItems.map { it.label })
    }

    @Test
    fun `Filipino sort uses localized buckets and explains retry`() {
        val step = toActivityStep(
            activity(
                "SORT_AND_CLASSIFY",
                """{"fits":["f1"],"doesNotFit":["d1"]}""",
                id = "filipino-sort-01",
                instruction = "Piliin kung angkop o hindi angkop."
            )
        )
        assertEquals(listOf("Angkop", "Hindi angkop"), step.sortCategories)
        assertTrue(step.feedback?.incorrect.orEmpty().contains("maling kahon"))
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
    fun `activity asset id is carried into the renderer step`() {
        val step = toActivityStep(
            activity(
                "HOTSPOT_IMAGE",
                """{"examples":["a red flag"]}""",
                assetId = "english-g3-q1-w01-d01-visual",
            )
        )

        assertEquals(listOf("english-g3-q1-w01-d01-visual"), step.imageAssets)
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
        // Only authored assessment steps are scored (spec CH-04).
        assertEquals(true, step.scored)
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
        assertEquals("Great job!", step.feedback?.correct)
        assertEquals(DEFAULT_INCORRECT_FEEDBACK, step.feedback?.incorrect)
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
            "INTERACTIVE_SPEC" to "INTERACTIVE_SPEC_V1",
            "VIDEO" to "VIDEO_V1",
            "VOICE_RECORD" to "VOICE_RECORD_V1",
            "SCRIPTED_DIALOGUE" to "SCRIPTED_DIALOGUE_V1",
        )
        expected.forEach { (raw, versioned) -> assertEquals(versioned, rendererType(raw)) }
    }

    @Test
    fun `voice recording payload carries model phrase`() {
        val step = toActivityStep(activity("VOICE_RECORD", """{"targetPhrase":"Salamat po"}"""))
        assertEquals("VOICE_RECORD_V1", step.type)
        assertEquals("Salamat po", step.targetPhrase)
        assertFalse(step.scored)
    }

    @Test
    fun `scripted dialogue payload carries prompts and responses`() {
        val step = toActivityStep(activity(
            "SCRIPTED_DIALOGUE",
            """{"prompts":["Kamusta ka?","Saan ka pupunta?"],"responses":["Mabuti po!","Sa paaralan po!"]}""",
        ))
        assertEquals("SCRIPTED_DIALOGUE_V1", step.type)
        assertEquals(listOf("Kamusta ka?", "Saan ka pupunta?"), step.dialoguePrompts)
        assertEquals(listOf("Mabuti po!", "Sa paaralan po!"), step.dialogueResponses)
        assertFalse(step.scored)
    }

    @Test
    fun `unknown raw type fails closed instead of defaulting to explanation`() {
        assertEquals(null, rendererType("MYSTERY_ACTIVITY"))
        assertEquals(null, rendererType(""))
    }

    @Test
    fun `activity with unknown type is dropped during conversion`() {
        val steps = playableSteps(listOf(activity("MYSTERY_ACTIVITY", "\"body text\"")))
        assertTrue(steps.isEmpty())
    }

    @Test
    fun `mixed activities keep known types and drop unknown ones`() {
        val steps = playableSteps(
            listOf(
                activity("MULTIPLE_CHOICE", """{"options":["A","B"],"correctIndex":0}""", id = "a-01"),
                activity("MYSTERY_ACTIVITY", "\"body text\"", id = "a-02"),
                activity("ANIMATED_EXPLANATION", "\"Some story body\"", id = "a-03"),
            )
        )
        assertEquals(listOf("a-01", "a-03"), steps.map { it.id })
        assertEquals(listOf("MULTIPLE_CHOICE_V1", "ANIMATED_EXPLANATION_V1"), steps.map { it.type })
    }

    @Test
    fun `assessment answer state supports selection before submission`() {
        assertEquals(AnswerCardState.SELECTED, assessmentOptionState(1, 1, false, 2))
        assertEquals(AnswerCardState.IDLE, assessmentOptionState(0, 1, false, 2))
        assertEquals(AnswerCardState.CORRECT, assessmentOptionState(1, 1, true, 1))
        assertEquals(AnswerCardState.INCORRECT, assessmentOptionState(1, 1, true, 2))
        assertEquals(AnswerCardState.DISABLED, assessmentOptionState(0, 1, true, 2))
    }
}
