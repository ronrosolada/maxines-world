package com.maxinesworld.featurelessonplayer

/** A video assessment cannot be opened early for Tagalog media. */
internal fun canOpenMediaAssessment(
    item: VideoLibraryItemUi,
    watchedMediaIds: Set<String>,
): Boolean {
    if (item.localPath == null || item.asset.assessment == null) return false
    return !item.asset.isTagalogVideo() || item.asset.mediaId in watchedMediaIds
}

internal fun shouldHideMediaAssessment(
    item: VideoLibraryItemUi,
    watchedMediaIds: Set<String>,
): Boolean =
    item.localPath != null &&
        item.asset.assessment != null &&
        item.asset.isTagalogVideo() &&
        item.asset.mediaId !in watchedMediaIds

internal fun com.maxinesworld.coremodel.MediaAsset.isTagalogVideo(): Boolean =
    mediaId.startsWith("kids-tagalog-", ignoreCase = true) ||
        title.contains("tagalog", ignoreCase = true)
