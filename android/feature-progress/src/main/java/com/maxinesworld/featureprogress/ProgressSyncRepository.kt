package com.maxinesworld.featureprogress

import com.maxinesworld.coredatabase.CollectedBadgeDao
import com.maxinesworld.coredatabase.CollectedBadgeEntity
import com.maxinesworld.coredatabase.InventoryDao
import com.maxinesworld.coredatabase.InventoryEntity
import com.maxinesworld.coredatabase.LessonCompletionDao
import com.maxinesworld.coredatabase.LessonCompletionEntity
import com.maxinesworld.coredatabase.ProgressEventDao
import com.maxinesworld.coredatabase.ProgressEventEntity
import com.maxinesworld.coredatabase.RewardLedgerDao
import com.maxinesworld.coredatabase.RewardLedgerEntity
import com.maxinesworld.coredatabase.VideoWatchLedgerDao
import com.maxinesworld.coredatabase.VideoWatchLedgerEntity
import com.maxinesworld.corenetwork.ApiClient
import com.maxinesworld.corenetwork.CollectedBadgeDto
import com.maxinesworld.corenetwork.InventoryDto
import com.maxinesworld.corenetwork.LessonCompletionDto
import com.maxinesworld.corenetwork.ProgressEventDto
import com.maxinesworld.corenetwork.RewardLedgerDto
import com.maxinesworld.corenetwork.SyncApiService
import com.maxinesworld.corenetwork.SyncPullResponse
import com.maxinesworld.corenetwork.SyncPushRequest
import com.maxinesworld.corenetwork.VideoWatchLedgerDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    data class Success(val pushedCount: Int, val pulledCount: Int) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}

@Singleton
class ProgressSyncRepository @Inject constructor(
    private val progressEventDao: ProgressEventDao,
    private val videoWatchLedgerDao: VideoWatchLedgerDao,
    private val lessonCompletionDao: LessonCompletionDao,
    private val collectedBadgeDao: CollectedBadgeDao,
    private val rewardLedgerDao: RewardLedgerDao,
    private val inventoryDao: InventoryDao,
    private val apiClient: ApiClient,
) {
    private val syncService: SyncApiService by lazy {
        apiClient.createSyncService()
    }

    suspend fun sync(childId: String, lastSyncTimestamp: Long = 0L): SyncResult = withContext(Dispatchers.IO) {
        try {
            // 1. Gather all local data for push
            val pendingProgress = progressEventDao.getPendingSyncByChild(childId)
            val videoRecords = videoWatchLedgerDao.getAllByChild(childId)
            val recentLessons = lessonCompletionDao.getRecentByChild(childId, limit = 500)
            val badges = collectedBadgeDao.getAllByChild(childId)
            val rewards = rewardLedgerDao.getAllByChild(childId)
            val inventory = inventoryDao.getAllByChild(childId)

            val pushReq = SyncPushRequest(
                childId = childId,
                progressEvents = pendingProgress.map { it.toDto() },
                videoWatchRecords = videoRecords.map { it.toDto() },
                lessonCompletions = recentLessons.map { it.toDto() },
                collectedBadges = badges.map { it.toDto() },
                rewardLedgers = rewards.map { it.toDto() },
                inventoryItems = inventory.map { it.toDto() }
            )

            // 2. Push local state to server
            val pushRes = syncService.pushSync(pushReq)
            if (!pushRes.success) {
                return@withContext SyncResult.Error("Push sync returned unsuccessful status")
            }

            // Mark pushed progress events as SYNCED
            for (pe in pendingProgress) {
                progressEventDao.updateSyncStatus(pe.id, "SYNCED")
            }

            // 3. Pull delta state from server
            val pullRes = syncService.pullSync(childId = childId, sinceEpochMillis = lastSyncTimestamp)
            val pulledCount = mergePulledData(pullRes)

            SyncResult.Success(pushedCount = pushRes.processedCount, pulledCount = pulledCount)
        } catch (e: Exception) {
            SyncResult.Error("Sync failed: ${e.message}", e)
        }
    }

    private suspend fun mergePulledData(pull: SyncPullResponse): Int {
        var count = 0

        // 1. Progress events
        for (pe in pull.progressEvents) {
            progressEventDao.insertIgnoring(pe.toEntity(status = "SYNCED"))
            count++
        }

        // 2. Video Watch Ledger (Additive merge)
        for (vw in pull.videoWatchRecords) {
            val existing = videoWatchLedgerDao.getEntry(vw.childId, vw.mediaId)
            if (existing != null) {
                val mergedAccredited = maxOf(existing.accreditedSeconds, vw.accreditedSeconds)
                val mergedQuizPassed = existing.quizPassed || vw.quizPassed
                val mergedBestScore = maxOf(existing.bestQuizScore, vw.bestQuizScore)
                val localFirst = existing.firstPassedAtEpochMillis
                val remoteFirst = vw.firstPassedAtEpochMillis
                val firstPassed = when {
                    localFirst != null && remoteFirst != null -> minOf(localFirst, remoteFirst)
                    else -> localFirst ?: remoteFirst
                }
                val lastWatched = maxOf(existing.lastWatchedAtEpochMillis, vw.lastWatchedAtEpochMillis)

                videoWatchLedgerDao.insertOrUpdate(
                    existing.copy(
                        accreditedSeconds = mergedAccredited,
                        quizPassed = mergedQuizPassed,
                        bestQuizScore = mergedBestScore,
                        firstPassedAtEpochMillis = firstPassed,
                        lastWatchedAtEpochMillis = lastWatched
                    )
                )
            } else {
                videoWatchLedgerDao.insertOrUpdate(vw.toEntity())
            }
            count++
        }

        // 3. Lesson Completions
        for (lc in pull.lessonCompletions) {
            lessonCompletionDao.insertIgnoring(lc.toEntity())
            count++
        }

        // 4. Badges
        for (b in pull.collectedBadges) {
            collectedBadgeDao.insert(b.toEntity())
            count++
        }

        // 5. Rewards
        for (r in pull.rewardLedgers) {
            rewardLedgerDao.insertIgnoring(r.toEntity())
            count++
        }

        // 6. Inventory
        for (inv in pull.inventoryItems) {
            inventoryDao.insertIgnoring(inv.toEntity())
            count++
        }

        return count
    }
}

// ─── Entity / DTO Converters ──────────────────────────────────────────────────

fun ProgressEventEntity.toDto() = ProgressEventDto(
    id = id,
    childId = childId,
    skillId = skillId,
    lessonId = lessonId,
    activityId = activityId,
    eventType = eventType,
    accuracy = accuracy,
    attempts = attempts,
    hintsUsed = hintsUsed,
    responseTimeMs = responseTimeMs,
    timestamp = timestamp
)

fun ProgressEventDto.toEntity(status: String = "PENDING") = ProgressEventEntity(
    id = id,
    childId = childId,
    skillId = skillId,
    lessonId = lessonId,
    activityId = activityId,
    eventType = eventType,
    accuracy = accuracy,
    attempts = attempts,
    hintsUsed = hintsUsed,
    responseTimeMs = responseTimeMs,
    timestamp = timestamp,
    syncStatus = status
)

fun VideoWatchLedgerEntity.toDto() = VideoWatchLedgerDto(
    id = id,
    childId = childId,
    mediaId = mediaId,
    subjectId = subjectId,
    accreditedSeconds = accreditedSeconds,
    quizPassed = quizPassed,
    bestQuizScore = bestQuizScore,
    firstPassedAtEpochMillis = firstPassedAtEpochMillis,
    lastWatchedAtEpochMillis = lastWatchedAtEpochMillis
)

fun VideoWatchLedgerDto.toEntity() = VideoWatchLedgerEntity(
    id = id,
    childId = childId,
    mediaId = mediaId,
    subjectId = subjectId,
    accreditedSeconds = accreditedSeconds,
    quizPassed = quizPassed,
    bestQuizScore = bestQuizScore,
    firstPassedAtEpochMillis = firstPassedAtEpochMillis,
    lastWatchedAtEpochMillis = lastWatchedAtEpochMillis
)

fun LessonCompletionEntity.toDto() = LessonCompletionDto(
    id = id,
    childId = childId,
    lessonId = lessonId,
    attemptId = attemptId,
    accuracy = accuracy,
    passedOnFirstAttempt = passedOnFirstAttempt,
    completedAtEpochMillis = completedAtEpochMillis
)

fun LessonCompletionDto.toEntity() = LessonCompletionEntity(
    id = id,
    childId = childId,
    lessonId = lessonId,
    attemptId = attemptId,
    accuracy = accuracy,
    passedOnFirstAttempt = passedOnFirstAttempt,
    completedAtEpochMillis = completedAtEpochMillis
)

fun CollectedBadgeEntity.toDto() = CollectedBadgeDto(
    id = id,
    childId = childId,
    badgeId = badgeId,
    biome = biome,
    earnedDate = earnedDate,
    earnedAtEpochMillis = earnedAtEpochMillis
)

fun CollectedBadgeDto.toEntity() = CollectedBadgeEntity(
    id = id,
    childId = childId,
    badgeId = badgeId,
    biome = biome,
    earnedDate = earnedDate,
    earnedAtEpochMillis = earnedAtEpochMillis
)

fun RewardLedgerEntity.toDto() = RewardLedgerDto(
    id = id,
    childId = childId,
    amount = amount,
    sourceKey = sourceKey,
    occurredAtEpochMillis = occurredAtEpochMillis
)

fun RewardLedgerDto.toEntity() = RewardLedgerEntity(
    id = id,
    childId = childId,
    amount = amount,
    sourceKey = sourceKey,
    occurredAtEpochMillis = occurredAtEpochMillis
)

fun InventoryEntity.toDto() = InventoryDto(
    id = id,
    childId = childId,
    itemId = itemId,
    acquiredAtEpochMillis = acquiredAtEpochMillis
)

fun InventoryDto.toEntity() = InventoryEntity(
    id = id,
    childId = childId,
    itemId = itemId,
    acquiredAtEpochMillis = acquiredAtEpochMillis
)
