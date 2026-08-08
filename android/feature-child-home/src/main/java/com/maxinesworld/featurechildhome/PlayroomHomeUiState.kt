package com.maxinesworld.featurechildhome

import androidx.annotation.DrawableRes

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
    val progressPercent: Int?,            // null = not started
    val availability: SubjectAvailability = SubjectAvailability.Available,
    val lockReason: String? = null,       // visible reason when locked
    val destination: String = id,         // pack subject key for navigation
)

@androidx.compose.runtime.Immutable
data class QuestUi(
    val task: String,
    val pawPrintsCompleted: Int,
    val pawPrintTotal: Int,
    val isComplete: Boolean = false,
    val recommendedSubjectId: String? = null, // null → “Choose a subject”
    val buttonLabel: String = "",             // Continue / Choose a subject / View reward
    val buttonAction: QuestAction = QuestAction.Continue,
)

enum class QuestAction { Continue, ChooseSubject, ViewReward }

@androidx.compose.runtime.Immutable
data class StickerUi(
    val id: String,
    val won: Boolean,
    @DrawableRes val iconRes: Int? = null,
)

@androidx.compose.runtime.Immutable
data class KeepsakeUi(
    val itemId: String,
    val emoji: String,
    val name: String,
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
        /** Treat Shop keepsakes this child owns — rendered on the home so a
         *  purchase always produces something visible. */
        val ownedKeepsakes: List<KeepsakeUi> = emptyList(),
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
