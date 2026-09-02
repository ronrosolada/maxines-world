package com.maxinesworld.coremodel

/**
 * Child-facing Grade 3 curriculum filter.
 *
 * The LAN catalog contains Grade 1–4 videos. Daily quests, the video library,
 * home progress, and prefetch must only present Grade 3 videos that are
 * `RELEASED`. Preview and other-grade rows stay in the catalog for future
 * review; they are not core curriculum.
 *
 * License status is independent: `PERSONAL_USE` remains an honest LAN-only
 * household license and is never rewritten to a public-distribution grant.
 */
object ChildFacingMediaPolicy {
    const val TARGET_GRADE = 3
    const val RELEASED = "RELEASED"

    fun isChildFacingCurriculum(asset: MediaAsset): Boolean =
        asset.gradeLevel == TARGET_GRADE &&
            asset.releaseStatus == RELEASED &&
            asset.subjectId.isNotBlank() &&
            asset.durationSeconds > 0

    fun childFacing(assets: List<MediaAsset>): List<MediaAsset> =
        assets.filter(::isChildFacingCurriculum)

    fun childFacingMediaIds(assets: List<MediaAsset>): Set<String> =
        childFacing(assets).mapTo(mutableSetOf()) { it.mediaId }
}
