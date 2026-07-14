package com.maxinesworld.coremodel

import org.junit.Assert.*
import org.junit.Test

class SubjectTest {

    // ── All supported destinations ──

    @Test
    fun `english resolves correctly`() {
        val s = Subject.fromId("english")
        assertNotNull(s)
        assertEquals(Subject.ENGLISH, s)
        assertEquals("english-g3-m01-d01", s?.lessonId)
    }

    @Test
    fun `filipino resolves correctly`() {
        val s = Subject.fromId("filipino")
        assertNotNull(s)
        assertEquals(Subject.FILIPINO, s)
        assertEquals("filipino-g3-m01-d01", s?.lessonId)
    }

    @Test
    fun `mathematics resolves correctly`() {
        val s = Subject.fromId("mathematics")
        assertNotNull(s)
        assertEquals(Subject.MATHEMATICS, s)
        assertEquals("mathematics-g3-m01-d01", s?.lessonId)
    }

    @Test
    fun `science resolves correctly`() {
        val s = Subject.fromId("science")
        assertNotNull(s)
        assertEquals(Subject.SCIENCE, s)
        assertEquals("science-g3-m01-d01", s?.lessonId)
    }

    @Test
    fun `gmrc resolves correctly`() {
        val s = Subject.fromId("gmrc")
        assertNotNull(s)
        assertEquals(Subject.GMRC, s)
        assertEquals("gmrc-g3-m01-l01", s?.lessonId)
    }

    // ── History/Makabansa aliases ──

    @Test
    fun `makabansa resolves to MAKABANSA`() {
        val s = Subject.fromId("makabansa")
        assertNotNull(s)
        assertEquals(Subject.MAKABANSA, s)
        assertEquals("mkb-g3-m01-l01", s?.lessonId)
    }

    @Test
    fun `history resolves to MAKABANSA`() {
        val s = Subject.fromId("history")
        assertNotNull(s)
        assertEquals(Subject.MAKABANSA, s)
        assertEquals("mkb-g3-m01-l01", s?.lessonId)
    }

    @Test
    fun `philippine-history resolves to MAKABANSA`() {
        val s = Subject.fromId("philippine-history")
        assertNotNull(s)
        assertEquals(Subject.MAKABANSA, s)
        assertEquals("mkb-g3-m01-l01", s?.lessonId)
    }

    // ── Case insensitivity ──

    @Test
    fun `uppercase subject resolves correctly`() {
        assertEquals(Subject.ENGLISH, Subject.fromId("ENGLISH"))
    }

    @Test
    fun `mixed case with whitespace resolves correctly`() {
        assertEquals(Subject.ENGLISH, Subject.fromId("  English  "))
    }

    // ── Unknown / invalid ──

    @Test
    fun `unknown id returns null`() {
        assertNull(Subject.fromId("art"))
        assertNull(Subject.fromId("music"))
        assertNull(Subject.fromId("random-string"))
    }

    @Test
    fun `null id returns null`() {
        assertNull(Subject.fromId(null))
    }

    @Test
    fun `blank id returns null`() {
        assertNull(Subject.fromId(""))
        assertNull(Subject.fromId("   "))
    }

    // ── Never defaults to English ──

    @Test
    fun `unknown id does NOT default to English`() {
        val s = Subject.fromId("unknown-topic")
        assertNull(s)
        // Must not accidentally route unknown → English
    }

    @Test
    fun `made up id does NOT resolve to any subject`() {
        assertNull(Subject.fromId("pottery"))
        assertNull(Subject.fromId("xkcd"))
    }

    // ── All IDs are unique ──

    @Test
    fun `all subject ids are unique`() {
        val ids = Subject.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `all lesson ids are unique`() {
        val lessonIds = Subject.entries.map { it.lessonId }
        assertEquals(lessonIds.size, lessonIds.toSet().size)
    }
}
