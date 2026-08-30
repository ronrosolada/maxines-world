package com.maxinesworld.featurelessonplayer

/** Layout contract for the feedback action that overlays the lesson viewport. */
internal object LessonFeedbackLayout {
    /** Space reserved below scrollable lesson content for the sticky feedback card. */
    const val StickyFeedbackHeightDp = 120

    fun bottomContentPaddingDp(showFeedback: Boolean): Int =
        if (showFeedback) StickyFeedbackHeightDp else 0
}
