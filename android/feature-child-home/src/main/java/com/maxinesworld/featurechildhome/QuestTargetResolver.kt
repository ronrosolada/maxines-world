package com.maxinesworld.featurechildhome

import com.maxinesworld.coremodel.MediaAsset

/** Resolves persisted daily mission media IDs into child-facing video targets. */
object QuestTargetResolver {

    fun resolve(
        assigned: List<String>,
        completed: Set<String>,
        assets: List<MediaAsset>?,
    ): List<QuestTargetUi> {
        if (assigned.isEmpty() || assets == null) return emptyList()
        val assetById = assets.associateBy { it.mediaId }
        return assigned.distinct().mapNotNull { mediaId ->
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
            )
        }
    }

    internal fun formatDuration(durationSeconds: Int): String {
        val safeSeconds = durationSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(safeSeconds / 60, safeSeconds % 60)
    }

    private const val RELEASED = "RELEASED"
}
