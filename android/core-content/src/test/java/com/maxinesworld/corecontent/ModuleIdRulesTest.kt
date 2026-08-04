package com.maxinesworld.corecontent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModuleIdRulesTest {

    @Test
    fun `friendly lesson title strips schema suffix but keeps authored text`() {
        assertEquals("Shape Trail", friendlyLessonTitleOf("Shape Trail · Q1 W01 D01"))
        assertEquals("Market Day", friendlyLessonTitleOf("Market Day · M01 D20"))
        assertEquals("Already friendly", friendlyLessonTitleOf("Already friendly"))
    }

    // ─── moduleKeyFor ───────────────────────────────────────────────────

    @Test
    fun `legacy lesson maps to m01 module`() {
        assertEquals("m01", ModuleIdRules.moduleKeyFor("mathematics-g3-m01-d05"))
        assertEquals("m01", ModuleIdRules.moduleKeyFor("english-g3-m01-d01"))
        assertEquals("m01", ModuleIdRules.moduleKeyFor("araling-panlipunan-g3-m01-d20"))
    }

    @Test
    fun `slm lesson maps to quarter-week module`() {
        assertEquals("q2-w04", ModuleIdRules.moduleKeyFor("mathematics-g3-q2-w04-d03"))
        assertEquals("q1-w01", ModuleIdRules.moduleKeyFor("gmrc-g3-q1-w01-d01"))
        assertEquals("q4-w09", ModuleIdRules.moduleKeyFor("science-g3-q4-w09-d02"))
    }

    @Test
    fun `malformed lesson ids return null`() {
        assertNull(ModuleIdRules.moduleKeyFor("mathematics-g3-q2-d03"))        // no week
        assertNull(ModuleIdRules.moduleKeyFor("mathematics-g3-q2-w04"))        // no day
        assertNull(ModuleIdRules.moduleKeyFor("mathematics-g3-m01"))           // no day
        assertNull(ModuleIdRules.moduleKeyFor("not-a-lesson-id"))
    }

    // ─── dayFor ──────────────────────────────────────────────────────────

    @Test
    fun `day extracted from suffix`() {
        assertEquals(5, ModuleIdRules.dayFor("mathematics-g3-m01-d05"))
        assertEquals(3, ModuleIdRules.dayFor("mathematics-g3-q2-w04-d03"))
        assertEquals(0, ModuleIdRules.dayFor("mathematics-g3-m01"))
    }

    // ─── moduleTitle ─────────────────────────────────────────────────────

    @Test
    fun `legacy module titled Module 1`() {
        assertEquals("Module 1", ModuleIdRules.moduleTitle("m01"))
    }

    @Test
    fun `slm module titled Quarter Week`() {
        assertEquals("Quarter 2 · Week 4", ModuleIdRules.moduleTitle("q2-w04"))
        assertEquals("Quarter 4 · Week 9", ModuleIdRules.moduleTitle("q4-w09"))
    }

    // ─── moduleSortRank ──────────────────────────────────────────────────

    @Test
    fun `m01 sorts before all slm modules`() {
        assertEquals(0, ModuleIdRules.moduleSortRank("m01"))
        assertEquals(104, ModuleIdRules.moduleSortRank("q1-w04"))
        assertEquals(409, ModuleIdRules.moduleSortRank("q4-w09"))
        // legacy first, then quarter-major week-minor
        val sorted = listOf("q4-w09", "m01", "q1-w04", "q3-w05", "q2-w01")
            .sortedBy { ModuleIdRules.moduleSortRank(it) }
        assertEquals(listOf("m01", "q1-w04", "q2-w01", "q3-w05", "q4-w09"), sorted)
    }
}
