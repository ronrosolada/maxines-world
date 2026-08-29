package com.maxinesworld.featurechildhome

import com.maxinesworld.corecontent.AssessmentRepository
import com.maxinesworld.coremodel.AssessmentPackMetadata
import com.maxinesworld.coremodel.MasteryRecord
import com.maxinesworld.coremodel.MasteryState
import com.maxinesworld.coremodel.MiloReviewQueueResolver
import com.maxinesworld.coremodel.VideoQuestPlanner
import com.maxinesworld.coremodel.CaregiverPhraseCard
import com.maxinesworld.coremodel.FilipinoProficiency
import com.maxinesworld.coredatabase.DailyQuestCompletionDao
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.DailyQuestCompletionEntity
import com.maxinesworld.coredatabase.DailyQuestSetDao
import com.maxinesworld.coredatabase.DailyQuestSetEntity
import com.maxinesworld.coredatabase.MasteryRecordDao
import com.maxinesworld.coredatabase.MasteryRecordEntity
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.corenetwork.MediaLibrary
import com.maxinesworld.featurerewards.DailyQuestRewardWriter
import com.maxinesworld.featureparent.CaregiverPhraseRepository
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val MAX_ARENA_SLOTS = 2
private const val HOME_PHRASE_PREFIX = "home-phrase:"

internal fun includeHomePracticeMission(
    proficiency: FilipinoProficiency,
    phraseCardIds: List<String>,
    regularQuestIds: List<String>,
    dayKey: String,
): List<String> {
    if (proficiency != FilipinoProficiency.BEGINNER || phraseCardIds.isEmpty()) return regularQuestIds
    val index = Math.floorMod(dayKey.hashCode(), phraseCardIds.size)
    return (listOf("$HOME_PHRASE_PREFIX${phraseCardIds[index]}") + regularQuestIds).distinct()
}

internal fun homePhraseRewardId(childId: String, dayKey: String, questId: String): String =
    "home-phrase-star:$childId:$dayKey:${questId.removePrefix(HOME_PHRASE_PREFIX)}"

internal fun injectDueSpacedReviews(
    records: List<MasteryRecordEntity>,
    regularQuestIds: List<String>,
    nowEpochMillis: Long = System.currentTimeMillis(),
    limit: Int = 3,
): List<String> {
    val modelRecords = records.map { record ->
        MasteryRecord(
            childId = record.childId,
            skillId = record.skillId,
            state = runCatching { MasteryState.valueOf(record.state) }.getOrDefault(MasteryState.PRACTICING),
            accuracy = record.accuracy,
            totalAttempts = record.totalAttempts,
            lastActivityAt = record.lastActivityAt,
            nextReviewAt = record.nextReviewAt,
        )
    }
    val reviewIds = MiloReviewQueueResolver.resolveDueItems(modelRecords, nowEpochMillis, limit)
        .map { it.id }
    return (reviewIds + regularQuestIds).distinct().take(limit)
}

/**
 * Prioritizes Foundations micro-lessons before native Grade 3 Filipino media targets
 * when a child profile is classified as BEGINNER in Filipino proficiency.
 */
internal fun prioritizeFilipinoFoundations(
    proficiency: com.maxinesworld.coremodel.FilipinoProficiency,
    foundationIds: List<String>,
    regularQuestIds: List<String>,
    passedQuestIds: Set<String>,
    limit: Int = 3,
): List<String> {
    if (proficiency != com.maxinesworld.coremodel.FilipinoProficiency.BEGINNER) {
        return regularQuestIds
    }
    val unpassedFoundations = foundationIds.filter { it !in passedQuestIds }
    if (unpassedFoundations.isEmpty()) {
        return regularQuestIds
    }
    val nonFilipinoRegular = regularQuestIds.filterNot { it.contains("filipino", ignoreCase = true) }
    val combined = (unpassedFoundations + nonFilipinoRegular).distinct()
    return combined.take(limit)
}


/** Persisted daily mission progress. IDs are opaque quest IDs, not lesson IDs. */
data class DailyQuestProgress(
    val dayKey: String,
    val assignedMediaIds: List<String>,
    val completedMediaIds: List<String>,
    val arenaPacks: List<AssessmentPackMetadata> = emptyList(),
    val homePhraseCards: List<CaregiverPhraseCard> = emptyList(),
) {
    /** Semantic alias for mixed video/Arena missions. */
    val assignedQuestIds: List<String> get() = assignedMediaIds
    val completedQuestIds: List<String> get() = completedMediaIds
    val completedCount: Int get() = completedMediaIds.size
    val totalCount: Int get() = assignedMediaIds.size
    val isComplete: Boolean get() = totalCount > 0 && completedCount >= totalCount
}

/**
 * Combines the planner's valid selection with deterministic frontier recovery.
 * A planner rejection may still leave one usable video when Arena slots exist;
 * that sparse video is intentionally not treated as a full planner selection.
 */
internal fun composeDailyQuestIds(
    plannerVideoIds: List<String>,
    frontier: List<VideoQuestPlanner.Candidate>,
    arenaIds: List<String>,
): List<String> {
    val assignedArenaIds = arenaIds.distinct().take(MAX_ARENA_SLOTS)
    val videoCount = 1 + (MAX_ARENA_SLOTS - assignedArenaIds.size)
    val orderedFallback = frontier
        .sortedWith(compareBy({ it.subjectId }, { it.mediaId }))
        .map { it.mediaId }
    val videoIds = if (plannerVideoIds.isEmpty()) {
        if (assignedArenaIds.isNotEmpty()) orderedFallback.take(1) else emptyList()
    } else {
        (plannerVideoIds + orderedFallback).distinct().take(videoCount)
    }
    return videoIds + assignedArenaIds
}

@ViewModelScoped
class DailyQuestManager @Inject constructor(
    private val mediaLibrary: MediaLibrary,
    private val assessmentRepository: AssessmentRepository,
    private val dailyQuestSetDao: DailyQuestSetDao,
    private val dailyQuestCompletionDao: DailyQuestCompletionDao,
    private val masteryRecordDao: MasteryRecordDao,
    private val rewardDao: RewardDao,
    private val dailyQuestRewardWriter: DailyQuestRewardWriter,
    private val caregiverPhraseRepository: CaregiverPhraseRepository,
    private val childProfileDao: ChildProfileDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureToday(
        childId: String,
        dayKey: String = LocalDate.now().toString(),
        passedMediaIds: Set<String> = emptySet(),
        availableMediaOverride: List<String>? = null,
        passedArenaPackIdsOverride: Set<String>? = null,
    ): DailyQuestProgress {
        val filipinoProficiency = runCatching {
            FilipinoProficiency.valueOf(childProfileDao.getById(childId)?.filipinoProficiency.orEmpty())
        }.getOrDefault(FilipinoProficiency.BEGINNER)
        val passedArenaPackIds = passedArenaPackIdsOverride ?: passedArenaPackIds(childId)
        val regularSelection = createSelection(childId, dayKey, passedMediaIds, passedArenaPackIds, availableMediaOverride)
        val selection = Selection(
            includeHomePracticeMission(
                proficiency = filipinoProficiency,
                phraseCardIds = caregiverPhraseRepository.loadCards().map { it.id },
                regularQuestIds = injectDueSpacedReviews(
                    records = masteryRecordDao.getByChild(childId),
                    regularQuestIds = regularSelection.ids,
                ),
                dayKey = dayKey,
            )
        )
        var set = dailyQuestSetDao.getByChildAndDay(childId, dayKey)
            ?: createSet(childId, dayKey, selection.ids)
        val persistedIds = parseIds(set.assignedQuestIds)
        val upgradedLegacyAssignment = persistedIds.any(::isLegacyLessonId)
        val needsBeginnerHomeMission = filipinoProficiency == FilipinoProficiency.BEGINNER &&
            persistedIds.none { it.startsWith(HOME_PHRASE_PREFIX) }
        if (upgradedLegacyAssignment || needsBeginnerHomeMission) {
            dailyQuestSetDao.updateAssignedQuestIds(
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = json.encodeToString(selection.ids),
            )
            set = checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
        }
        if (!upgradedLegacyAssignment && persistedIds.isEmpty() && selection.ids.isNotEmpty()) {
            // An unavailable catalog remains truthful, but an empty mission is
            // retryable when the bundled catalogs recover later the same day.
            dailyQuestSetDao.updateAssignedQuestIds(
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = json.encodeToString(selection.ids),
            )
            set = checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
        }
        val assigned = parseIds(set.assignedQuestIds)
        creditPassedQuestIds(childId, dayKey, assigned, passedMediaIds, passedArenaPackIds)
        dailyQuestRewardWriter.reconcile(childId, dayKey)
        val completed = dailyQuestCompletionDao
            .getCompletedQuestIds(childId, dayKey)
            .filter { it in assigned && (it.startsWith(HOME_PHRASE_PREFIX) || isPassedQuest(it, passedMediaIds, passedArenaPackIds)) }
        return DailyQuestProgress(
            dayKey = dayKey,
            assignedMediaIds = assigned,
            completedMediaIds = completed,
            arenaPacks = arenaPacksForAssigned(assigned),
            homePhraseCards = caregiverPhraseRepository.loadCards().filter { "$HOME_PHRASE_PREFIX${it.id}" in assigned },
        )
    }

    suspend fun markHomePhrasePracticed(childId: String, dayKey: String, questId: String) {
        require(questId.startsWith(HOME_PHRASE_PREFIX))
        val assigned = dailyQuestSetDao.getByChildAndDay(childId, dayKey)?.let { parseIds(it.assignedQuestIds) }.orEmpty()
        require(questId in assigned) { "Home phrase is not assigned for this day" }
        dailyQuestCompletionDao.insertIgnoring(
            DailyQuestCompletionEntity(
                id = "$childId:$dayKey:$questId",
                childId = childId,
                dayKey = dayKey,
                questId = questId,
                completionEventId = "home-practice:$childId:$dayKey:$questId",
            )
        )
        rewardDao.insertIgnoring(
            RewardEntity(
                id = homePhraseRewardId(childId, dayKey, questId),
                childId = childId,
                type = "STAR",
                subject = "filipino",
                amount = 1,
                metadata = "home_phrase_practiced:${questId.removePrefix(HOME_PHRASE_PREFIX)}",
            )
        )
        dailyQuestRewardWriter.reconcile(childId, dayKey)
    }

    private suspend fun createSet(
        childId: String,
        dayKey: String,
        selectedIds: List<String>,
    ): DailyQuestSetEntity {
        dailyQuestSetDao.insertIgnoring(
            DailyQuestSetEntity(
                id = "$childId:$dayKey",
                childId = childId,
                dayKey = dayKey,
                assignedQuestIds = json.encodeToString(selectedIds),
            )
        )
        return checkNotNull(dailyQuestSetDao.getByChildAndDay(childId, dayKey))
    }

    private data class Selection(
        val ids: List<String>,
    )

    private suspend fun createSelection(
        childId: String,
        dayKey: String,
        passedMediaIds: Set<String>,
        passedArenaPackIds: Set<String>,
        availableMediaOverride: List<String>?,
    ): Selection {
        val catalogMedia = runCatching { mediaLibrary.getCatalog().media }.getOrElse { emptyList() }
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
                        VideoQuestPlanner.Candidate(asset.mediaId, asset.subjectId, asset.durationSeconds)
                    }
            }
        val arenaPacks = runCatching { assessmentRepository.getCatalog().packs }
            .getOrElse { emptyList() }
            .filter { it.id.isNotBlank() && it.title.trim().startsWith(GRADE_THREE_PREFIX) }
            .filterNot { it.id in passedArenaPackIds }
        val arenaIds = arenaPacks.take(MAX_ARENA_SLOTS).map { "$ARENA_PREFIX${it.id}" }
        val plannerVideoIds = VideoQuestPlanner.select(childId, dayKey, frontier)
        val videoIds = composeDailyQuestIds(plannerVideoIds, frontier, arenaIds)
            .filterNot { it.startsWith(ARENA_PREFIX) }
        if (videoIds.isEmpty()) {
            // When video catalog/LAN is offline or unindexed, fall back to pure Assessment Arena daily missions
            val allArenaIds = arenaPacks.take(3).map { "$ARENA_PREFIX${it.id}" }
            if (allArenaIds.isNotEmpty()) {
                return Selection(allArenaIds)
            }
            return Selection(emptyList())
        }
        return Selection(videoIds + arenaIds)
    }

    private suspend fun passedArenaPackIds(childId: String): Set<String> =
        rewardDao.getByChild(childId)
            .asSequence()
            .mapNotNull { reward ->
                reward.metadata.takeIf { it.startsWith(ARENA_REWARD_PREFIX) }
                    ?.removePrefix(ARENA_REWARD_PREFIX)
            }
            .toSet()

    private suspend fun arenaPacksForAssigned(assigned: List<String>): List<AssessmentPackMetadata> {
        val ids = assigned.filter { it.startsWith(ARENA_PREFIX) }
            .map { it.removePrefix(ARENA_PREFIX) }
            .toSet()
        if (ids.isEmpty()) return emptyList()
        return runCatching { assessmentRepository.getCatalog().packs }
            .getOrElse { emptyList() }
            .filter { it.id in ids && it.title.trim().startsWith(GRADE_THREE_PREFIX) }
    }

    private suspend fun creditPassedQuestIds(
        childId: String,
        dayKey: String,
        assignedQuestIds: List<String>,
        passedMediaIds: Set<String>,
        passedArenaPackIds: Set<String>,
    ) {
        for (questId in assignedQuestIds.distinct()) {
            val passed = isPassedQuest(questId, passedMediaIds, passedArenaPackIds)
            if (!passed) continue
            dailyQuestCompletionDao.insertIgnoring(
                DailyQuestCompletionEntity(
                    id = "$childId:$dayKey:$questId",
                    childId = childId,
                    dayKey = dayKey,
                    questId = questId,
                    completionEventId = if (questId.startsWith(ARENA_PREFIX)) {
                        "arena-pass:$childId:$questId"
                    } else {
                        "video-pass:$childId:$questId"
                    },
                )
            )
        }
    }

    private fun isPassedQuest(
        questId: String,
        passedMediaIds: Set<String>,
        passedArenaPackIds: Set<String>,
    ): Boolean = if (questId.startsWith(ARENA_PREFIX)) {
        questId.removePrefix(ARENA_PREFIX) in passedArenaPackIds
    } else {
        questId in passedMediaIds
    }

    private fun parseIds(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }
            .getOrElse { raw.split('|').map(String::trim).filter(String::isNotEmpty) }

    private companion object {
        const val RELEASED = "RELEASED"
        const val ARENA_PREFIX = "arena:"
        const val ARENA_REWARD_PREFIX = "assessment_arena_passed:"
        const val GRADE_THREE_PREFIX = "Grade 3"
        val LEGACY_LESSON_ID = Regex(
            "^(?:[a-z-]+-g3-(?:m\\d+|q\\d-w\\d+)-d\\d+|(?:eng|fil|math|sci|mkb|gmrc)-g3-m\\d+-l\\d+)$",
        )

        fun isLegacyLessonId(id: String): Boolean = LEGACY_LESSON_ID.matches(id)
    }
}
