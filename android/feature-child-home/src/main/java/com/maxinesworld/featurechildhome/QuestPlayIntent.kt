package com.maxinesworld.featurechildhome

/**
 * Child-facing destination for Today's mission Start / Continue / target taps.
 *
 * VIDEO targets must name the assigned mediaId so the library plays that
 * lesson instead of dumping Maxine on the subject shelf. Arena stays Arena.
 * Home-practice is caregiver-led and must never open the video library.
 */
sealed class QuestPlayIntent {
    data class PlayAssignedVideo(
        val subjectId: String,
        val mediaId: String,
    ) : QuestPlayIntent()

    data class OpenArena(
        val subjectId: String,
        val packId: String?,
    ) : QuestPlayIntent()

    data object OpenVideoShelf : QuestPlayIntent()

    data object StayOnHome : QuestPlayIntent()
}

object QuestPlayRouter {
    fun intentForQuestAction(action: QuestAction, quest: QuestUi?): QuestPlayIntent {
        return when (action) {
            QuestAction.OpenVideoQuest -> intentForNextPlayableTarget(quest)
            QuestAction.Continue -> QuestPlayIntent.OpenVideoShelf
            QuestAction.RetryMission,
            QuestAction.ChooseSubject,
            QuestAction.ViewReward,
            QuestAction.OpenPlayground,
            -> QuestPlayIntent.StayOnHome
        }
    }

    fun intentForTarget(target: QuestTargetUi): QuestPlayIntent = when (target.type) {
        QuestTargetType.VIDEO -> QuestPlayIntent.PlayAssignedVideo(
            subjectId = target.subjectId,
            mediaId = target.mediaId,
        )
        QuestTargetType.ARENA -> QuestPlayIntent.OpenArena(
            subjectId = target.subjectId,
            packId = target.arenaPackId,
        )
        QuestTargetType.HOME_PRACTICE -> QuestPlayIntent.StayOnHome
    }

    /**
     * Start / Continue resolves the next incomplete child-playable target.
     * Home-practice rows stay on home; they are not a Filipino video stand-in.
     */
    private fun intentForNextPlayableTarget(quest: QuestUi?): QuestPlayIntent {
        val targets = quest?.targets.orEmpty()
        val nextId = quest?.nextTargetId
        val named = nextId?.let { id -> targets.firstOrNull { it.mediaId == id } }
        val playable = when {
            named != null && named.type != QuestTargetType.HOME_PRACTICE -> named
            else -> targets.firstOrNull { !it.isCompleted && it.type != QuestTargetType.HOME_PRACTICE }
        } ?: return QuestPlayIntent.StayOnHome
        return intentForTarget(playable)
    }
}
