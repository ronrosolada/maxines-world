package com.maxinesworld.featurelessonplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class McOptionShuffleTest {
    @Test
    fun `fisher yates follows injected random draws`() {
        // nextInt(4)=0, nextInt(3)=0, nextInt(2)=0
        // [a,b,c,d] -> swap 3,0 -> [d,b,c,a] -> swap 2,0 -> [c,b,d,a] -> swap 1,0 -> [b,c,d,a]
        val shuffled = shuffleMcOptions(
            listOf("a", "b", "c", "d"),
            QueueRandom(intArrayOf(0, 0, 0)),
        )
        assertEquals(listOf("b", "c", "d", "a"), shuffled)
    }

    @Test
    fun `shuffle is a permutation not a length sort`() {
        val options = listOf(
            "ok",
            "a much longer distractor",
            "mid size",
            "x",
        )
        val orders = (0..80).map { seed ->
            shuffleMcOptions(options, Random(seed.toLong()))
        }
        assertTrue(orders.toSet().size >= 2)
        assertTrue(
            "must not always put the longest choice first",
            orders.any { it.first() != "a much longer distractor" },
        )
        assertTrue(
            "must not always put the longest choice last",
            orders.any { it.last() != "a much longer distractor" },
        )
        orders.forEach { assertEquals(options.toSet(), it.toSet()) }
    }

    @Test
    fun `keyed first option is not always moved last`() {
        val authored = listOf("a", "b", "c", "d")
        val keySlots = IntArray(4)
        repeat(200) { seed ->
            val shuffled = shuffleMcOptions(authored, Random(seed.toLong()))
            keySlots[shuffled.indexOf("a")]++
        }
        assertTrue(
            "a first-authored key must be able to land in several slots, counts=${keySlots.toList()}",
            keySlots.count { it > 0 } >= 3,
        )
    }

    @Test
    fun `keyed id scores in every slot and distractor never does`() {
        val presented = listOf("d", "c", "b", "a")
        val correct = listOf("b")
        presented.forEach { id ->
            val scored = isKeyedMcChoiceCorrect(id, presented, correct)
            assertEquals(id == "b", scored)
        }
    }

    @Test
    fun `missing blank and unpresented ids fail closed`() {
        val presented = listOf("a", "b", "c", "d")
        val correct = listOf("b")
        assertFalse(isKeyedMcChoiceCorrect(null, presented, correct))
        assertFalse(isKeyedMcChoiceCorrect("", presented, correct))
        assertFalse(isKeyedMcChoiceCorrect("z", presented, correct))
        assertFalse(isKeyedMcChoiceCorrect("b", emptyList(), correct))
        assertFalse(isKeyedMcChoiceCorrect("b", listOf("a", "c", "d"), correct))
    }

    @Test
    fun `slot labels are visual letters not option ids`() {
        assertEquals("A", mcSlotLabel(0))
        assertEquals("B", mcSlotLabel(1))
        assertEquals("C", mcSlotLabel(2))
        assertEquals("D", mcSlotLabel(3))
    }

    private class QueueRandom(private val draws: IntArray) : Random() {
        private var index = 0
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextInt(until: Int): Int = draws[index++]
    }
}
