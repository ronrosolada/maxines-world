package com.maxinesworld.engineactivity

import com.maxinesworld.coremodel.ActivityStep
import com.maxinesworld.coremodel.MatchPair
import com.maxinesworld.coremodel.SortItem
import com.maxinesworld.engineactivity.renderers.HotspotProgress
import com.maxinesworld.engineactivity.renderers.hotspotBadgeOffset
import com.maxinesworld.engineactivity.renderers.hotspotGridColumns
import com.maxinesworld.engineactivity.renderers.lessonVisualAssetId
import com.maxinesworld.engineactivity.renderers.matchingTargetLabel
import com.maxinesworld.engineactivity.renderers.recordHotspotTargetTap
import org.junit.Assert.assertEquals
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These are pure-logic guards for the answer-key conventions the renderers
 * rely on. They do not instantiate Compose.
 */
class RendererContractTest {

    @Test
    fun `lesson visual uses the first authored asset and ignores blanks`() {
        val step = ActivityStep(
            id = "visual-1",
            type = "MULTIPLE_CHOICE_V1",
            imageAssets = listOf("", "english-g3-m01-d01-visual", "unused"),
        )

        assertEquals("english-g3-m01-d01-visual", lessonVisualAssetId(step))
    }

    @Test
    fun `lesson visual is absent when no authored asset exists`() {
        assertEquals(null, lessonVisualAssetId(ActivityStep(id = "visual-2", type = "MULTIPLE_CHOICE_V1")))
    }

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
    fun `shared match targets get distinct Milo labels`() {
        assertEquals("Milo says yes 1", matchingTargetLabel(0, "shows the skill", sharedLabel = true))
        assertEquals("shows the skill", matchingTargetLabel(1, "shows the skill", sharedLabel = false))
    }

    @Test
    fun `sequence display shuffle is stable for a given id`() {
        val id = "seq-1"
        val a = (0 until 3).shuffled(java.util.Random(id.hashCode().toLong()))
        val b = (0 until 3).shuffled(java.util.Random(id.hashCode().toLong()))
        assertEquals(a, b)
    }

    @Test
    fun `multi-target hotspot completes only after every unique target is visited`() {
        val first = recordHotspotTargetTap(HotspotProgress(), index = 0, targetCount = 3)
        val second = recordHotspotTargetTap(first, index = 1, targetCount = 3)
        val duplicate = recordHotspotTargetTap(second, index = 1, targetCount = 3)
        val complete = recordHotspotTargetTap(duplicate, index = 2, targetCount = 3)

        assertEquals(setOf(0), first.visited)
        assertTrue(!first.completed)
        assertEquals(setOf(0, 1), second.visited)
        assertEquals(second, duplicate)
        assertEquals(setOf(0, 1, 2), complete.visited)
        assertTrue(complete.completed)
        assertEquals(3, complete.attempts)
    }

    @Test
    fun `hotspot grid keeps five and eight targets in separate cells`() {
        assertEquals(2, hotspotGridColumns(4))
        assertEquals(3, hotspotGridColumns(5))
        assertEquals(3, hotspotGridColumns(8))
        assertEquals(4, hotspotGridColumns(10))
    }

    @Test
    fun `hotspot badges stay at the top start of their cells`() {
        val (x, y) = hotspotBadgeOffset(3, 2, 16.dp, 100.dp, 80.dp)
        assertEquals(124.dp, x)
        assertEquals(104.dp, y)
    }
}
