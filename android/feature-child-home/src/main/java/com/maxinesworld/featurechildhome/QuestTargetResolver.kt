package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.AssessmentPackMetadata
import com.maxinesworld.coremodel.CaregiverPhraseCard

/** Resolves persisted daily mission media IDs into child-facing video targets. */
object QuestTargetResolver {

    fun resolve(
        assigned: List<String>,
        completed: Set<String>,
        assets: List<MediaAsset>?,
        arenaPacks: List<AssessmentPackMetadata> = emptyList(),
        homePhraseCards: List<CaregiverPhraseCard> = emptyList(),
        isAssetDownloaded: (MediaAsset) -> Boolean = { false },
    ): List<QuestTargetUi> {
        if (assigned.isEmpty()) return emptyList()
        val assetById = assets.orEmpty().associateBy { it.mediaId }
        val arenaById = arenaPacks.associateBy { it.id }
        val homePhraseById = homePhraseCards.associateBy { it.id }
        return assigned.distinct().mapNotNull { mediaId ->
            if (mediaId.startsWith(HOME_PHRASE_PREFIX)) {
                val card = homePhraseById[mediaId.removePrefix(HOME_PHRASE_PREFIX)] ?: return@mapNotNull null
                return@mapNotNull QuestTargetUi(
                    mediaId = mediaId,
                    title = "Misyong Pantahanan: ${card.filipinoPhrase}",
                    subjectId = "filipino",
                    displaySubject = "Home Mission • ${card.englishCue}",
                    durationSeconds = 0,
                    durationLabel = "+1 STAR",
                    isCompleted = mediaId in completed,
                    type = QuestTargetType.HOME_PRACTICE,
                    isReadyOffline = true,
                )
            }
            if (mediaId.startsWith(ARENA_PREFIX)) {
                val packId = mediaId.removePrefix(ARENA_PREFIX)
                val pack = arenaById[packId] ?: return@mapNotNull null
                return@mapNotNull QuestTargetUi(
                    mediaId = mediaId,
                    title = pack.title,
                    subjectId = pack.subjectId,
                    displaySubject = subjectDisplayName(pack.subjectId),
                    durationSeconds = 0,
                    durationLabel = "",
                    isCompleted = mediaId in completed,
                    type = QuestTargetType.ARENA,
                    arenaPackId = pack.id,
                    isReadyOffline = true,
                )
            }
            if (assets == null) return@mapNotNull null
            val asset = assetById[mediaId] ?: return@mapNotNull null
            if (asset.releaseStatus != RELEASED) return@mapNotNull null
            val subjectId = asset.subjectId.takeIf(String::isNotBlank) ?: return@mapNotNull null
            QuestTargetUi(
                mediaId = mediaId,
                title = asset.title,
                subjectId = subjectId,
                displaySubject = subjectDisplayName(subjectId),
                durationSeconds = asset.durationSeconds,
                durationLabel = formatDuration(asset.durationSeconds),
                isCompleted = mediaId in completed,
                isReadyOffline = isAssetDownloaded(asset),
            )
        }
    }

    internal fun formatDuration(durationSeconds: Int): String {
        val safeSeconds = durationSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(safeSeconds / 60, safeSeconds % 60)
    }

    private const val RELEASED = "RELEASED"
    private const val ARENA_PREFIX = "arena:"
    private const val HOME_PHRASE_PREFIX = "home-phrase:"
}
