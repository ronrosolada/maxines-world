package com.maxinesworld.featurelessonplayer

import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coremodel.MediaAsset

/** Insert one reward for one child/source metadata key, even across retries. */
internal suspend fun insertRewardIfAbsent(rewardDao: RewardDao, reward: RewardEntity) {
    if (rewardDao.getByChildAndMetadata(reward.childId, reward.metadata) == null) {
        rewardDao.insertIgnoring(reward)
    }
}

/** Stable catalog ordering shared by the Video Hub and its unit tests. */
internal fun sortMediaAssetsForLibrary(assets: List<MediaAsset>): List<MediaAsset> =
    assets.sortedWith(
        compareBy<MediaAsset>(
            { it.gradeLevel },
            { it.quarter },
            { it.episodeNumber },
            { it.title.lowercase() },
        )
    )

/** Keeps passed assets in a separate, bottom section while preserving order. */
internal fun partitionMediaAssetsForLibrary(
    assets: List<MediaAsset>,
    passedIds: Set<String>,
): Pair<List<MediaAsset>, List<MediaAsset>> {
    val sorted = sortMediaAssetsForLibrary(assets)
    val upcoming = sorted.filterNot { it.mediaId in passedIds }
    val completed = sorted.filter { it.mediaId in passedIds }
    return upcoming to completed
}
