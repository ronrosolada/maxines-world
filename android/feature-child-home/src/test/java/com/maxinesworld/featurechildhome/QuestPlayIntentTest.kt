package com.maxinesworld.featurechildhome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestPlayIntentTest {

    @Test
    fun `OpenVideoQuest with a video target requests play of that mediaId`() {
        val target = videoTarget(mediaId = "science-g3-01")
        val intent = QuestPlayRouter.intentForQuestAction(
            QuestAction.OpenVideoQuest,
            quest(targets = listOf(target), nextTargetId = target.mediaId),
        )

        assertEquals(
            QuestPlayIntent.PlayAssignedVideo(
                subjectId = "science",
                mediaId = "science-g3-01",
            ),
            intent,
        )
    }

    @Test
    fun `OpenVideoQuest with an arena target opens that pack`() {
        val target = arenaTarget()
        val intent = QuestPlayRouter.intentForQuestAction(
            QuestAction.OpenVideoQuest,
            quest(targets = listOf(target), nextTargetId = target.mediaId),
        )

        assertEquals(
            QuestPlayIntent.OpenArena(subjectId = "science", packId = "science-g3"),
            intent,
        )
    }

    @Test
    fun `OpenVideoQuest skips home-practice and does not invent a Filipino video`() {
        val home = homePracticeTarget()
        val video = videoTarget(mediaId = "english-g3-01", subjectId = "english")
        val intent = QuestPlayRouter.intentForQuestAction(
            QuestAction.OpenVideoQuest,
            quest(targets = listOf(home, video), nextTargetId = home.mediaId),
        )

        assertEquals(
            QuestPlayIntent.PlayAssignedVideo(subjectId = "english", mediaId = "english-g3-01"),
            intent,
        )
    }

    @Test
    fun `home-practice target click stays on home`() {
        assertEquals(
            QuestPlayIntent.StayOnHome,
            QuestPlayRouter.intentForTarget(homePracticeTarget()),
        )
    }

    @Test
    fun `video target click requests that mediaId`() {
        val target = videoTarget(mediaId = "math-g3-02", subjectId = "mathematics")
        assertEquals(
            QuestPlayIntent.PlayAssignedVideo(
                subjectId = "mathematics",
                mediaId = "math-g3-02",
            ),
            QuestPlayRouter.intentForTarget(target),
        )
    }

    @Test
    fun `arena target click opens Assessment Arena`() {
        assertEquals(
            QuestPlayIntent.OpenArena(subjectId = "science", packId = "science-g3"),
            QuestPlayRouter.intentForTarget(arenaTarget()),
        )
    }

    @Test
    fun `Continue without a named target opens the video shelf`() {
        assertEquals(
            QuestPlayIntent.OpenVideoShelf,
            QuestPlayRouter.intentForQuestAction(QuestAction.Continue, quest()),
        )
    }

    @Test
    fun `passed quest and retry actions stay off the video library`() {
        assertEquals(
            QuestPlayIntent.StayOnHome,
            QuestPlayRouter.intentForQuestAction(QuestAction.OpenPlayground, quest()),
        )
        assertEquals(
            QuestPlayIntent.StayOnHome,
            QuestPlayRouter.intentForQuestAction(QuestAction.ViewReward, quest()),
        )
        assertEquals(
            QuestPlayIntent.StayOnHome,
            QuestPlayRouter.intentForQuestAction(QuestAction.RetryMission, quest()),
        )
    }

    @Test
    fun `OpenVideoQuest with only home-practice stays on home`() {
        val home = homePracticeTarget()
        val intent = QuestPlayRouter.intentForQuestAction(
            QuestAction.OpenVideoQuest,
            quest(targets = listOf(home), nextTargetId = home.mediaId),
        )
        assertTrue(intent is QuestPlayIntent.StayOnHome)
    }

    private fun quest(
        targets: List<QuestTargetUi> = emptyList(),
        nextTargetId: String? = null,
    ) = QuestUi(
        task = QuestTaskCopy.IncompleteToday,
        pawPrintsCompleted = 0,
        pawPrintTotal = 3,
        buttonLabel = QuestButtonLabel.StartQuest,
        buttonAction = QuestAction.OpenVideoQuest,
        targets = targets,
        nextTargetId = nextTargetId,
    )

    private fun videoTarget(
        mediaId: String,
        subjectId: String = "science",
        isReadyOffline: Boolean = true,
    ) = QuestTargetUi(
        mediaId = mediaId,
        title = "Living Things",
        subjectId = subjectId,
        displaySubject = "Science",
        durationSeconds = 120,
        durationLabel = "02:00",
        isCompleted = false,
        type = QuestTargetType.VIDEO,
        isReadyOffline = isReadyOffline,
    )

    private fun arenaTarget() = QuestTargetUi(
        mediaId = "arena:science-g3",
        title = "Grade 3 Science Challenge",
        subjectId = "science",
        displaySubject = "Science",
        durationSeconds = 0,
        durationLabel = "",
        isCompleted = false,
        type = QuestTargetType.ARENA,
        arenaPackId = "science-g3",
        isReadyOffline = true,
    )

    private fun homePracticeTarget() = QuestTargetUi(
        mediaId = "home-phrase:greeting-good-morning",
        title = "Misyong Pantahanan: Magandang umaga",
        subjectId = "filipino",
        displaySubject = "Home Mission",
        durationSeconds = 0,
        durationLabel = "+1 STAR",
        isCompleted = false,
        type = QuestTargetType.HOME_PRACTICE,
        isReadyOffline = true,
    )
}
