package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.VideoQuestPlanner
import com.maxinesworld.coredatabase.DailyQuestCompletionDao
import com.maxinesworld.coredatabase.DailyQuestCompletionEntity
import com.maxinesworld.coredatabase.DailyQuestSetDao
import com.maxinesworld.coredatabase.DailyQuestSetEntity
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persisted daily mission progress. IDs are opaque media IDs, not lesson IDs. */
data class DailyQuestProgress(
    val dayKey: String,
    val assignedMediaIds: List<String>,
    val completedMediaIds: List<String>,
) {
    val completedCount: Int get() = completedMediaIds.size
    val totalCount: Int get() = assignedMediaIds.size
    val isComplete: Boolean get() = totalCount > 0 && completedCount >= totalCount
}

@ViewModelScoped
class DailyQuestManager @Inject constructor(
    private val mediaLibrary: MediaLibrary,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val dailyQuestRewardWriter: DailyQuestRewardWriter,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureToday(
        childId: String,
        dayKey: String = LocalDate.now().toString(),
        passedMediaIds: Set<String> = emptySet(),
        availableMediaOverride: List<String>? = null,
    ): DailyQuestProgress {
        var set = dailyQuestSetDao.getByChildAndDay(childId, dayKey)
            ?: createSet(childId, dayKey, passedMediaIds, availableMediaOverride)
        val persistedIds = parseIds(set.assignedQuestIds)
        val upgradedLegacyAssignment = persistedIds.any(::isLegacyLessonId)
        if (upgradedLegacyAssignment) {
            // The table predates video missions and has no assignment-kind
            // column. Treat recognizable lesson IDs as stale legacy data and
            // replace the whole assignment before either displaying or
            // rewarding it. An empty replacement is still truthful and can be
            // retried on a later refresh.
            dailyQuestSetDao.updateAssignedQuestIds(
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = json.encodeToString(
                    createSelection(childId, dayKey, passedMediaIds, availableMediaOverride),
                ),
            )
            set = checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
        }
        if (!upgradedLegacyAssignment && parseIds(set.assignedQuestIds).isEmpty()) {
            // An unavailable catalog must remain truthful, but an empty mission
            // is retryable when the catalog recovers later the same day.
            val retry = createSelection(childId, dayKey, passedMediaIds, availableMediaOverride)
            if (retry.isNotEmpty()) {
                dailyQuestSetDao.updateAssignedQuestIds(
                    childId = childId,
                    dayKey = dayKey,
                    assignedQuestIds = json.encodeToString(retry),
                )
                set = checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
            }
        }
        val assigned = parseIds(set.assignedQuestIds)
        creditPassedMediaIds(childId, dayKey, assigned, passedMediaIds)
        // The reward writer remains the only daily-mission reward minter. It
        // reconciles from the persisted set/completions and is idempotent.
        dailyQuestRewardWriter.reconcile(childId, dayKey)
        val completedMediaIds = dailyQuestCompletionDao
            .getCompletedQuestIds(childId, dayKey)
            .filter { it in assigned && it in passedMediaIds }
        return DailyQuestProgress(dayKey, assigned, completedMediaIds)
    }

    private suspend fun createSet(
        childId: String,
        dayKey: String,
        passedMediaIds: Set<String>,
        availableMediaOverride: List<String>?,
    ): DailyQuestSetEntity {
        val selected = createSelection(childId, dayKey, passedMediaIds, availableMediaOverride)

        dailyQuestSetDao.insertIgnoring(
            DailyQuestSetEntity(
                id = "$childId:$dayKey",
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = json.encodeToString(selected),
            )
        )
        // insertIgnoring handles a concurrent creator; always read the durable
        // row rather than returning an in-memory candidate set.
        return checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
    }

    private suspend fun createSelection(
        childId: String,
        dayKey: String,
        passedMediaIds: Set<String>,
        availableMediaOverride: List<String>?,
    ): List<String> {
        val catalogMedia = runCatching { mediaLibrary.getCatalog().media }
            .getOrElse { emptyList() }
        val eligibleMedia = catalogMedia
            .asSequence()
            .filter { asset ->
                asset.gradeLevel == 3 &&
                    asset.releaseStatus == RELEASED &&
                    asset.subjectId.isNotBlank() &&
                    asset.durationSeconds > 0
            }
            .filter { asset -> availableMediaOverride == null || asset.mediaId in availableMediaOverride }
            .toList()

        val frontier = eligibleMedia
            .groupBy { it.subjectId }
            .values
            .mapNotNull { subjectMedia ->
                subjectMedia
                    .sortedWith(compareBy({ it.episodeNumber }, { it.mediaId }))
                    .firstOrNull { it.mediaId !in passedMediaIds }
                    ?.let { asset ->
                        VideoQuestPlanner.Candidate(
                            mediaId = asset.mediaId,
                            subjectId = asset.subjectId,
                            durationSeconds = asset.durationSeconds,
                        )
                    }
            }
        val selected = VideoQuestPlanner.select(childId, dayKey, frontier)
        return selected
    }

    private suspend fun creditPassedMediaIds(
        childId: String,
        dayKey: String,
        assignedMediaIds: List<String>,
        passedMediaIds: Set<String>,
    ) {
        assignedMediaIds
            .filter { it in passedMediaIds }
            .distinct()
            .forEach { mediaId ->
                dailyQuestCompletionDao.insertIgnoring(
                    DailyQuestCompletionEntity(
                        id = "$childId:$dayKey:$mediaId",
                        childId = childId,
                        dayKey = dayKey,
                        questId = mediaId,
                        completionEventId = "video-pass:$childId:$mediaId",
                    )
                )
            }
    }

    private fun parseIds(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrElse { raw.split('|').map(String::trim).filter(String::isNotEmpty) }

    private companion object {
        const val RELEASED = "RELEASED"
        val LEGACY_LESSON_ID = Regex(
            "^(?:[a-z-]+-g3-(?:m\\d+|q\\d-w\\d+)-d\\d+|(?:eng|fil|math|sci|mkb|gmrc)-g3-m\\d+-l\\d+)$",
        )

        fun isLegacyLessonId(id: String): Boolean = LEGACY_LESSON_ID.matches(id)
    }
}
