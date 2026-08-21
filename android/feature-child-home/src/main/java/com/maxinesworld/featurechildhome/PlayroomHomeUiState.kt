package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes
import com.maxinesworld.coremodel.MediaAsset

/**
 * Option 3 Playroom Collections homepage state (design.md §14).
 *
 * The screen renders exactly six canonical subject cards in the order below.
 * Remote/DB data may update progress, lock state, and availability, but must
 * never rename, remove, duplicate, or reorder these definitions.
 *
 * Araling Panlipunan content ships under the Makabansa card: Makabansa is the
 * Matatag-curriculum successor of AP (product decision 2026-08-06, audit A1),
 * so the 20 legacy AP lessons appear inside the Makabansa collection.
 */
enum class SubjectAvailability {
    Available,
    Locked,
    OfflineAvailable,
    OfflineUnavailable,
    Error,
}

@androidx.compose.runtime.Immutable
data class SubjectCardUi(
    val id: String,                       // stable ID, never the visible label
    val formalName: String,
    val playfulName: String,
    @DrawableRes val illustrationRes: Int,
    val progressPercent: Int?,            // compatibility/internal calculations only
    /** Completed videos from the active media catalog; null means unavailable. */
    val completedVideos: Int? = null,
    /** Total videos from the active media catalog; null means unavailable. */
    val totalVideos: Int? = null,
    val availability: SubjectAvailability = SubjectAvailability.Available,
    val lockReason: String? = null,       // visible reason when locked
    val destination: String = id,         // pack subject key for navigation
)

/**
 * Returns the truthful video count for one subject. A null asset list means the
 * media catalog is unavailable; an empty subject match is a real 0-of-0 result.
 */
internal fun videoProgressForSubject(
    subjectId: String,
    assets: List<MediaAsset>?,
    passedMediaIds: Set<String>,
): Pair<Int, Int>? {
    if (assets == null) return null
    val subjectAssets = assets.filter { it.subjectId.equals(subjectId, ignoreCase = true) }
    return subjectAssets.count { it.mediaId in passedMediaIds } to subjectAssets.size
}

/**
 * Publishes the latest video progress on top of one stable home snapshot.
 * Non-video fields are intentionally copied unchanged from [baseContent].
 */
internal fun withVideoProgress(
    baseContent: PlayroomHomeUiState.Content,
    assets: List<MediaAsset>?,
    passedMediaIds: Set<String>,
): PlayroomHomeUiState.Content = baseContent.copy(
    subjects = baseContent.subjects.map { subject ->
        val progress = videoProgressForSubject(subject.destination, assets, passedMediaIds)
        subject.copy(
            completedVideos = progress?.first,
            totalVideos = progress?.second,
        )
    },
)

@androidx.compose.runtime.Immutable
data class QuestTargetUi(
    val lessonId: String,
    val title: String,
    val subject: String,
    val displaySubject: String,
    val moduleKey: String?,
    val isCompleted: Boolean,
)

enum class QuestTaskCopy {
    ParentMode,
    CompleteToday,
    IncompleteToday,
}

enum class QuestButtonLabel {
    OpenPlayground,
    OpenSanctuary,
    ChooseSubject,
    StartQuest,
    ContinueQuest,
    Start,
    Continue,
}

@androidx.compose.runtime.Immutable
data class QuestUi(
    val task: QuestTaskCopy,
    val pawPrintsCompleted: Int,
    val pawPrintTotal: Int,
    val isComplete: Boolean = false,
    val recommendedSubjectId: String? = null, // null → "Choose a subject"
    val buttonLabel: QuestButtonLabel = QuestButtonLabel.Continue,
    val buttonAction: QuestAction = QuestAction.Continue,
    val targets: List<QuestTargetUi> = emptyList(),
    val nextLessonId: String? = null,
    val godModeEnabled: Boolean = false,
    val sanctuaryComplete: Boolean = false,
    val playgroundUnlocked: Boolean = false,
)

enum class QuestAction { Continue, ChooseSubject, ViewReward, OpenLesson, OpenPlayground }

@androidx.compose.runtime.Immutable
data class StickerUi(
    val id: String,
    val won: Boolean,
    @DrawableRes val iconRes: Int? = null,
)

@androidx.compose.runtime.Immutable
data class KeepsakeUi(
    val itemId: String,
    val name: String,
    val iconKey: String = "keepsake",
)

@androidx.compose.runtime.Immutable
data class SanctuaryPieceUi(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
    val residentWildlife: List<String> = emptyList(),
    val funFact: String = "",
)

@androidx.compose.runtime.Immutable
data class SanctuaryUi(
    val earnedPieces: Int = 0,
    val visiblePieces: List<SanctuaryPieceUi> = emptyList(),
    val nextPiece: SanctuaryPieceUi? = null,
    val totalPieces: Int = 12,
)

@androidx.compose.runtime.Immutable
data class WildlifeStickersUi(
    val collectedCount: Int,
    val totalCount: Int,
    val stickers: List<StickerUi> = emptyList(),
)

/**
 * Sealed screen state: one shape per top-level situation. Independent
 * booleans (isLoading/isError) are deliberately avoided.
 */
sealed interface PlayroomHomeUiState {
    data object Loading : PlayroomHomeUiState

    data class Content(
        val childName: String,
        val subjects: List<SubjectCardUi>,
        val quest: QuestUi,
        val wildlifeStickers: WildlifeStickersUi,
        val offline: Boolean = false,
        val openingSubjectId: String? = null,
        val staleBanner: Boolean = false,
        /** Child-visible currency — earned stars/coins must be seen and
         *  counted by the kid, not just the Parent Dashboard (#35). */
        val starBalance: Int = 0,
        val coinBalance: Int = 0,
        val totalAccreditedSeconds: Int = 0,
        /** Treat Shop keepsakes this child owns — rendered on the home so a
         *  purchase always produces something visible. */
        val ownedKeepsakes: List<KeepsakeUi> = emptyList(),
        /** Daily Quest rewards build a persistent, child-visible sanctuary. */
        val sanctuary: SanctuaryUi = SanctuaryUi(),
    ) : PlayroomHomeUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : PlayroomHomeUiState
}

/** The six canonical subjects in fixed order (design.md §10.1). */
val canonicalSubjects: List<SubjectCardUi> = listOf(
    SubjectCardUi(
        id = "mathematics", formalName = "Mathematics", playfulName = "Number Fun",
        illustrationRes = R.drawable.mw_subject_math_number_fun,
        progressPercent = null, destination = "mathematics",
    ),
    SubjectCardUi(
        id = "english", formalName = "English", playfulName = "Story Time",
        illustrationRes = R.drawable.mw_subject_english_story_time,
        progressPercent = null, destination = "english",
    ),
    SubjectCardUi(
        id = "science", formalName = "Science", playfulName = "Discovery",
        illustrationRes = R.drawable.mw_subject_science_discovery,
        progressPercent = null, destination = "science",
    ),
    SubjectCardUi(
        id = "filipino", formalName = "Filipino", playfulName = "Kwentuhan",
        illustrationRes = R.drawable.mw_subject_filipino_kwentuhan,
        progressPercent = null, destination = "filipino",
    ),
    // Makabansa (Matatag successor of Araling Panlipunan) — the legacy AP
    // lessons are folded into this collection; see ModuleCatalog.
    SubjectCardUi(
        id = "makabansa", formalName = "Makabansa", playfulName = "Bayan at Kultura",
        illustrationRes = R.drawable.mw_subject_ap_heritage,
        progressPercent = null, destination = "makabansa",
    ),
    SubjectCardUi(
        id = "gmrc", formalName = "GMRC", playfulName = "Kindness",
        illustrationRes = R.drawable.mw_subject_gmrc_kindness,
        progressPercent = null, destination = "gmrc",
    ),
)

/** The subset of subjects a learner can currently open (not locked). */
val SubjectCardUi.isAvailable: Boolean
    get() = availability == SubjectAvailability.Available ||
        availability == SubjectAvailability.OfflineAvailable
