package com.maxinesworld.coredatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the stable reward key used to prevent replay farming. */
@RunWith(AndroidJUnit4::class)
class RewardDaoTest {
    private lateinit var db: MaxinesDatabase
    private lateinit var dao: RewardDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.rewardDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun firstCompletionRewardKeyIsIdempotent() = runBlocking {
        val reward = RewardEntity(
            id = "lesson-first:child_1:english-g3-m01-d01:STAR",
            childId = "child_1",
            type = "STAR",
            subject = "english",
            amount = 5,
            metadata = "lesson-first:child_1:english-g3-m01-d01",
        )

        assertEquals(1L, dao.insertIgnoring(reward))
        assertEquals(-1L, dao.insertIgnoring(reward))
        assertEquals(5, dao.getTotalByType("child_1", "STAR"))
    }
}
