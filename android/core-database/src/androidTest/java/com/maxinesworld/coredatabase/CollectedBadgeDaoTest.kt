package com.maxinesworld.coredatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies sticker award + revoke behavior against a real Room instance. */
@RunWith(AndroidJUnit4::class)
class CollectedBadgeDaoTest {
    private lateinit var db: MaxinesDatabase
    private lateinit var dao: CollectedBadgeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.collectedBadgeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun badge(childId: String, badgeId: String) = CollectedBadgeEntity(
        id = "${childId}_$badgeId",
        childId = childId,
        badgeId = badgeId,
        biome = "forest",
        earnedDate = "parent_awarded",
    )

    @Test
    fun deleteByChildAndBadgeIdRemovesOnlyThatBadge() = runBlocking {
        dao.insert(badge("child_1", "badge_tarsier"))
        dao.insert(badge("child_1", "badge_eagle"))
        dao.insert(badge("child_2", "badge_tarsier"))

        val deleted = dao.deleteByChildAndBadgeId("child_1", "badge_tarsier")

        assertEquals(1, deleted)
        assertEquals(listOf("badge_eagle"), dao.getAllByChild("child_1").map { it.badgeId })
        assertTrue(dao.countByChild("child_2") == 1) // other children untouched
    }

    @Test
    fun deleteByChildAndBadgeIdReturnsZeroForUnknownBadge() = runBlocking {
        dao.insert(badge("child_1", "badge_tarsier"))
        assertEquals(0, dao.deleteByChildAndBadgeId("child_1", "badge_unknown"))
    }

    @Test
    fun observeBadgeIdsByChildReflectsInsertAndDelete(): Unit = runBlocking {
        val observed = mutableListOf<List<String>>()
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val job = scope.launch {
            dao.observeBadgeIdsByChild("child_1").collect { observed.add(it) }
        }
        try {
            awaitUntil(5_000) { observed.isNotEmpty() } // initial emission
            dao.insert(badge("child_1", "badge_tarsier"))
            awaitUntil(5_000) { observed.lastOrNull()?.contains("badge_tarsier") == true }

            dao.deleteByChildAndBadgeId("child_1", "badge_tarsier")
            awaitUntil(5_000) { observed.lastOrNull()?.isEmpty() == true }
        } finally {
            job.cancel()
            scope.cancel()
        }
    }

    /** Room invalidation is asynchronous; poll until [condition] holds or [timeoutMs] elapses. */
    private inline fun awaitUntil(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw AssertionError("Condition not met in ${timeoutMs}ms")
            Thread.sleep(50)
        }
    }
}
