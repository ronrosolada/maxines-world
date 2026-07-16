package com.maxinesworld.featurechildhome

import androidx.compose.runtime.Immutable
import com.maxinesworld.playground.PlaygroundGateState
import com.maxinesworld.playground.PlaygroundGateStatus

@Immutable
data class SubjectDestinationUi(
    val id: String,
    val label: String,
    val progressText: String,
    val enabled: Boolean = true,
)

sealed interface PlaygroundUi {
    data class Locked(val completed: Int, val total: Int, val remaining: Int) : PlaygroundUi
    data object Open : PlaygroundUi
    data object Unavailable : PlaygroundUi
}

@Immutable
data class LivingVillageHomeState(
    val childId: String,
    val childName: String,
    val fishTreats: Long,
    val completedQuests: Int,
    val totalQuests: Int,
    val playground: PlaygroundUi,
    val questTitle: String,
    val questPrompt: String,
    val questSubjectId: String?,
    val destinations: List<SubjectDestinationUi>,
    val reducedMotion: Boolean,
    val loading: Boolean = false,
)

sealed interface LivingVillageAction {
    data class OpenSubject(val subjectId: String) : LivingVillageAction
    data object ContinueQuest : LivingVillageAction
    data object OpenCatCafe : LivingVillageAction
    data object OpenPlayground : LivingVillageAction
    data object OpenDiscoveries : LivingVillageAction
    data object OpenParents : LivingVillageAction
}

fun VillageHomeState.toLivingVillage(reducedMotion: Boolean): LivingVillageHomeState {
    val questCompleted = questProgressText.split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 0
    val questTotal = questProgressText.split("/").lastOrNull()?.trim()?.toIntOrNull() ?: 3
    val playgroundUi = when {
        playground == null -> PlaygroundUi.Unavailable
        playground.status == PlaygroundGateStatus.Unlocked -> PlaygroundUi.Open
        playground.status == PlaygroundGateStatus.Locked -> PlaygroundUi.Locked(
            completed = playground.completed,
            total = playground.totalAssigned,
            remaining = playground.totalAssigned - playground.completed,
        )
        else -> PlaygroundUi.Unavailable // Loading, NoQuests, Error
    }
    val questSubject = if (showMiraRequest) "english" else null
    return LivingVillageHomeState(
        childId = "",
        childName = childName,
        fishTreats = fishTreats.toLong(),
        completedQuests = questCompleted,
        totalQuests = questTotal,
        playground = playgroundUi,
        questTitle = if (questText == "Complete 3 activities" && questCompleted >= questTotal) "All done!" else questText,
        questPrompt = if (questCompleted >= questTotal) "New requests are on the way" else "Tap to continue",
        questSubjectId = questSubject,
        destinations = destinations.map { d ->
            SubjectDestinationUi(
                id = d.id,
                label = d.name,
                progressText = d.progressText,
                enabled = d.enabled,
            )
        },
        reducedMotion = reducedMotion,
        loading = isLoading,
    )
}
