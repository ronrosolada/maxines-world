package com.maxinesworld.featurechildhome

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.DailyQuestCompletionEntity
import com.maxinesworld.coredatabase.LessonCompletionEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyQuestManagerTest {
    private lateinit var database: MaxinesDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MaxinesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dailySetIsSeededOnceAndCompletionIsRestartSafe() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val available = listOf("math-1", "english-1", "science-1", "filipino-1")
        val manager = manager(context)

        val first = manager.ensureToday("child-1", "2026-08-04", emptyList(), available)
        assertEquals(3, first.totalCount)
        assertNotNull(database.dailyQuestSetDao().getByChildAndDay("child-1", "2026-08-04"))

        val completed = first.assignedQuestIds.first()
        database.lessonCompletionDao().insertIgnoring(
            LessonCompletionEntity(
                id = "completion-1",
                childId = "child-1",
                lessonId = completed,
                attemptId = "attempt-1",
                accuracy = 1.0,
            )
        )
        database.dailyQuestCompletionDao().insertIgnoring(
            DailyQuestCompletionEntity(
                id = "child-1:2026-08-04:$completed",
                childId = "child-1",
                dayKey = "2026-08-04",
                questId = completed,
                completionEventId = "lesson-completion:child-1:$completed",
            )
        )

        val afterCompletion = manager.ensureToday("child-1", "2026-08-04", listOf(completed), available)
        val afterRestart = manager(context).ensureToday("child-1", "2026-08-04", listOf(completed), available)

        assertEquals(first.assignedQuestIds, afterCompletion.assignedQuestIds)
        assertEquals(1, afterCompletion.completedCount)
        assertEquals(afterCompletion, afterRestart)
    }

    private fun manager(context: android.content.Context) = DailyQuestManager(
        catalog = ModuleCatalog(context),
        lessonCompletionDao = database.lessonCompletionDao(),
        dailyQuestSetDao = database.dailyQuestSetDao(),
        dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
        dailyQuestRewardWriter = DailyQuestRewardWriter(
            database = database,
            dailyQuestSetDao = database.dailyQuestSetDao(),
            dailyQuestCompletionDao = database.dailyQuestCompletionDao(),
            rewardDao = database.rewardDao(),
            rewardBreakDao = database.rewardBreakDao(),
            playgroundUnlockReceiptDao = database.playgroundUnlockReceiptDao(),
        ),
    )
}
