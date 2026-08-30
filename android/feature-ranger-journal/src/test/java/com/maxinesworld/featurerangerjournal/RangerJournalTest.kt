package com.maxinesworld.featurerangerjournal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class RangerJournalTest {

    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var dispatcher: TestDispatcher

    @Before
    fun setUp() {
        scheduler = TestCoroutineScheduler()
        dispatcher = StandardTestDispatcher(scheduler)
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── JournalStore ─────────────────────────────────────────────────

    @Test
    fun `store keeps entries per child newest first with idempotent add`() = runTest(scheduler) {
        val store = InMemoryJournalStore()
        store.add(JournalEntry("a", "c1", "tarsier", 10L))
        store.add(JournalEntry("b", "c1", "eagle", 20L))
        store.add(JournalEntry("a", "c1", "tarsier", 10L)) // duplicate id is ignored
        store.add(JournalEntry("c", "c2", "tamaraw", 5L))

        val c1 = store.entries("c1")
        assertEquals(2, c1.size)
        assertEquals(listOf("b", "a"), c1.map { it.id })
        assertEquals(listOf("c"), store.entries("c2").map { it.id })

        store.delete("c1", "a")
        assertEquals(listOf("b"), store.entries("c1").map { it.id })

        store.setFavorite("c1", "b", true)
        assertTrue(store.entries("c1").single().isFavorite)
    }

    // ─── RangerJournalViewModel ───────────────────────────────────────

    @Test
    fun `refresh loads persisted entries newest first`() = runTest(scheduler) {
        val store = InMemoryJournalStore()
        store.add(JournalEntry("a", "c1", "tarsier", 10L))
        store.add(JournalEntry("b", "c1", "eagle", 20L))

        val vm = RangerJournalViewModel("c1", store)
        advanceUntilIdle()

        assertEquals(listOf("b", "a"), vm.state.value.entries.map { it.id })
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `addEntry inserts trimmed caption entry at top and persists`() = runTest(scheduler) {
        val store = InMemoryJournalStore()
        val ids = ArrayDeque(listOf("n1", "n2"))
        var now = 100L
        val vm = RangerJournalViewModel("c1", store, clock = { now }, idFactory = { ids.removeFirst() })
        advanceUntilIdle()

        vm.addEntry("tarsier", "  A happy day  ")
        advanceUntilIdle()
        val first = vm.state.value
        assertEquals(1, first.entries.size)
        assertEquals("A happy day", first.entries.single().caption)
        assertEquals("n1", first.lastSavedId)

        now = 200L
        vm.addEntry("pawikan", "")
        advanceUntilIdle()
        val second = vm.state.value
        assertEquals(2, second.entries.size)
        assertEquals(listOf("n2", "n1"), second.entries.map { it.id })
        assertEquals(2, store.entries("c1").size)
        assertEquals("pawikan", second.entries.first().sceneId)
    }

    @Test
    fun `delete removes entry and toggleFavorite flips and persists`() = runTest(scheduler) {
        val store = InMemoryJournalStore()
        val vm = RangerJournalViewModel("c1", store, idFactory = { "e1" })
        advanceUntilIdle()

        vm.addEntry("tarsier", "")
        advanceUntilIdle()
        vm.toggleFavorite("e1")
        advanceUntilIdle()
        assertTrue(vm.state.value.entries.single().isFavorite)
        assertTrue(store.entries("c1").single().isFavorite)

        vm.delete("e1")
        advanceUntilIdle()
        assertTrue(vm.state.value.entries.isEmpty())
        assertTrue(store.entries("c1").isEmpty())
    }

    // ─── RangerJournalExporter ────────────────────────────────────────

    @Test
    fun `export markdown includes scenes dates captions and favorites newest first`() {
        val markdown = RangerJournalExporter.exportMarkdown(
            entries = listOf(
                JournalEntry("a", "c1", "tarsier", 1_600_000_000_000L, caption = "Saw a tarsier!", isFavorite = true),
                JournalEntry("b", "c1", "eagle", 1_500_000_000_000L),
            ),
            sceneName = { id -> if (id == "tarsier") "Tarsier Forest" else "Eagle Sky" },
            locale = Locale.US,
        )

        assertTrue(markdown.startsWith("# Ranger Journal"))
        assertTrue(markdown.contains("Total photos: 2"))
        assertTrue(markdown.contains("## Tarsier Forest"))
        assertTrue(markdown.contains("Note: Saw a tarsier!"))
        assertTrue(markdown.contains("Favorite: yes"))
        assertTrue(markdown.contains("Taken: "))
        // Newest entry is listed first.
        assertTrue(markdown.indexOf("Tarsier Forest") < markdown.indexOf("Eagle Sky"))
    }

    @Test
    fun `export handles an empty journal`() {
        val markdown = RangerJournalExporter.exportMarkdown(emptyList(), { it })
        assertTrue(markdown.contains("Total photos: 0"))
        assertFalse(markdown.contains("## "))
    }

    @Test
    fun `export filename embeds child id and timestamp`() {
        val name = RangerJournalExporter.exportFilename("c1", 123L)
        assertTrue(name.startsWith("ranger-journal-c1-123"))
        assertTrue(name.endsWith(".md"))
    }
}