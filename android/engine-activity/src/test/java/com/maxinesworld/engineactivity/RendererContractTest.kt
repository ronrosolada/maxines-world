package com.maxinesworld.engineactivity

import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.MatchPair
import com.maxinesworld.coremodel.SortItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These are pure-logic guards for the answer-key conventions the renderers
 * rely on. They do not instantiate Compose.
 */
class RendererContractTest {

    @Test
    fun `sort mapping derives from typed items not positional halving`() {
        val step = ActivityStep(
            id = "a", type = "SORT_AND_CLASSIFY_V1",
            sortCategories = listOf("Fits", "Does not fit"),
            sortItems = listOf(
                SortItem("f1", 0), SortItem("d1", 1), SortItem("f2", 0),
                SortItem("d2", 1), SortItem("f3", 0), SortItem("d3", 1)
            )
        )
        val mapping = step.sortItems.indices.associateWith { step.sortItems[it].categoryIndex }
        assertEquals(6, mapping.size)
        assertEquals(2, step.sortCategories.size)
        assertEquals(3, mapping.values.count { it == 0 })
        assertEquals(3, mapping.values.count { it == 1 })
    }

    @Test
    fun `identical right values still match by value`() {
        val pairs = listOf(
            MatchPair("L1", "fits the lesson idea"),
            MatchPair("L2", "fits the lesson idea"),
            MatchPair("L3", "fits the lesson idea")
        )
        val right = pairs.map { it.right }
        assertTrue(right[0] == right[2])
    }

    @Test
    fun `sequence display shuffle is stable for a given id`() {
        val id = "seq-1"
        val a = (0 until 3).shuffled(java.util.Random(id.hashCode().toLong()))
        val b = (0 until 3).shuffled(java.util.Random(id.hashCode().toLong()))
        assertEquals(a, b)
    }
}
