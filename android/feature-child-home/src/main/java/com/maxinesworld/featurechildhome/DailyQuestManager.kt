package com.maxinesworld.featurechildhome

import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.DailyQuestCompletionDao
import com.maxinesworld.coredatabase.DailyQuestCompletionEntity
import com.maxinesworld.coredatabase.DailyQuestSetDao
import com.maxinesworld.coredatabase.DailyQuestSetEntity
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DailyQuestProgress(
    val dayKey: String,
    val assignedQuestIds: List<String>,
    val completedQuestIds: List<String>,
) {
    val completedCount: Int get() = completedQuestIds.size
    val totalCount: Int get() = assignedQuestIds.size
    val isComplete: Boolean get() = totalCount > 0 && completedCount >= totalCount
}

object DailyQuestPlanner {
    internal fun questStartIndex(hash: Int, size: Int): Int {
        require(size > 0) { "Quest pool must not be empty" }
        // Preserve the existing stable rotation for every normal hash. The
        // only unsafe value for abs(Int) is Int.MIN_VALUE, whose absolute
        // value cannot be represented as an Int.
        val safeHash = if (hash == Int.MIN_VALUE) Int.MAX_VALUE else abs(hash)
        return safeHash % size
    }

    fun selectQuestIds(
        childId: String,
        dayKey: String,
        completedLessonIds: Set<String>,
        availableLessonIds: List<String>,
        count: Int = 3,
    ): List<String> {
        if (count <= 0 || availableLessonIds.isEmpty()) return emptyList()
        val uniqueAvailable = availableLessonIds.distinct()
        val unfinished = uniqueAvailable.filterNot(completedLessonIds::contains)
        // Only unfinished lessons may be daily targets. Padding with
        // already-completed lessons let a child recycle previous days'
        // completions into today's quest and mint rewards without new learning.
        if (unfinished.isEmpty()) return emptyList()
        val start = questStartIndex("$childId:$dayKey".hashCode(), unfinished.size)
        return (0 until minOf(count, unfinished.size))
            .map { offset -> unfinished[(start + offset) % unfinished.size] }
    }
}

@ViewModelScoped
class DailyQuestManager @Inject constructor(
    private val catalog: ModuleCatalog,
    private val lessonCompletionDao: LessonCompletionDao,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val dailyQuestRewardWriter: DailyQuestRewardWriter,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureToday(
        childId: String,
        dayKey: String = LocalDate.now().toString(),
        completedLessonIds: List<String>? = null,
        availableLessonIdsOverride: List<String>? = null,
    ): DailyQuestProgress {
        val completed = (completedLessonIds ?: lessonCompletionDao.observeDistinctLessonIds(childId).first()).toSet()
        val set = dailyQuestSetDao.getByChildAndDay(childId, dayKey)
            ?: createSet(childId, dayKey, completed, availableLessonIdsOverride)
        val assigned = parseIds(set.assignedQuestIds)
        // Never pre-fill today's quest from the child's GLOBAL completion set: a
        // lesson finished on a previous day must not complete today's quest or mint
        // today's sanctuary/break reward. Today's quest targets are credited only by
        // the lesson-completion path (DailyQuestRewardWriter), so a stale historical
        // completion cannot auto-complete a fresh day.
        dailyQuestRewardWriter.reconcile(childId, dayKey)
        val completedQuestIds = dailyQuestCompletionDao.getCompletedQuestIds(childId, dayKey)
        return DailyQuestProgress(dayKey, assigned, completedQuestIds)
    }

    private suspend fun createSet(
        childId: String,
        dayKey: String,
        completedLessonIds: Set<String>,
        availableLessonIdsOverride: List<String>?,
    ): DailyQuestSetEntity {
        val availableLessonIds = availableLessonIdsOverride ?: DAILY_QUEST_SUBJECTS
            .flatMap { subject -> catalog.modulesFor(subject).flatMap { it.lessons.map { lesson -> lesson.lessonId } } }
        val selected = DailyQuestPlanner.selectQuestIds(childId, dayKey, completedLessonIds, availableLessonIds)
        dailyQuestSetDao.insertIgnoring(
            DailyQuestSetEntity(
                id = "$childId:$dayKey",
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = json.encodeToString(selected),
            )
        )
        return checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
    }

    private fun parseIds(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrElse { raw.split('|').map(String::trim).filter(String::isNotEmpty) }

    companion object {
        private val DAILY_QUEST_SUBJECTS = listOf(
            "mathematics", "english", "science", "filipino",
            "araling-panlipunan", "makabansa", "gmrc",
        )
    }
}
