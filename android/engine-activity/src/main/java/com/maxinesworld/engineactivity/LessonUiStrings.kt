package com.maxinesworld.engineactivity

import androidx.compose.runtime.staticCompositionLocalOf

/** UI chrome language for the in-lesson player (not curriculum content language). */
val LocalLessonUiLanguage = staticCompositionLocalOf { "en-PH" }

data class LessonUiStrings(
    val readAlong: String,
    val continueLabel: String,
    val tapSpeaker: String,
    val readingAloud: String,
    val voiceUnavailable: String,
    val voiceUnavailableBanner: String,
    val hint: String,
    val submit: String,
    val retry: String,
    val tryAgain: String,
    val next: String,
    val tryNext: String,
    val matchPairs: String,
    val matchedProgress: String,
    val arrangeInOrder: String,
    val available: String,
    val yourOrder: String,
    val selectAll: String,
    val greatJob: String,
    val tapRegion: String,
    val getHint: String,
    val submitAnswer: String,
    val continueNext: String,
    val categoryFits: String,
    val categoryDoesNotFit: String,
)

private val EN = LessonUiStrings(
    readAlong = "Read Along",
    continueLabel = "Continue",
    tapSpeaker = "Tap speaker to listen",
    readingAloud = "Reading aloud...",
    voiceUnavailable = "Voice not available",
    voiceUnavailableBanner = "Voice not available on this device — please read along instead.",
    hint = "Hint",
    submit = "Submit",
    retry = "Retry",
    tryAgain = "Try again",
    next = "Next",
    tryNext = "Try Next",
    matchPairs = "Match the pairs!",
    matchedProgress = "Matched",
    arrangeInOrder = "Arrange in order:",
    available = "Available:",
    yourOrder = "Your order:",
    selectAll = "Select all",
    greatJob = "Great job! 🎉",
    tapRegion = "Tap the correct region",
    getHint = "Get a hint",
    submitAnswer = "Submit answer",
    continueNext = "Continue to next activity",
    categoryFits = "Fits",
    categoryDoesNotFit = "Does not fit",
)

private val FIL = LessonUiStrings(
    readAlong = "Basahin Natin",
    continueLabel = "Susunod",
    tapSpeaker = "Pindutin ang speaker para makinig",
    readingAloud = "Binabasa nang malakas...",
    voiceUnavailable = "Walang boses sa device",
    voiceUnavailableBanner = "Walang Filipino voice sa device na ito — basahin na lang nang tahimik.",
    hint = "Pahiwatig",
    submit = "Ipasa",
    retry = "Subukan muli",
    tryAgain = "Subukan muli",
    next = "Susunod",
    tryNext = "Susunod",
    matchPairs = "Itugma ang magkapares!",
    matchedProgress = "Natugma",
    arrangeInOrder = "Ayusin sa tamang pagkakasunod:",
    available = "Mga hakbang:",
    yourOrder = "Iyong ayos:",
    selectAll = "Piliin lahat",
    greatJob = "Magaling! 🎉",
    tapRegion = "Pindutin ang tamang bahagi",
    getHint = "Kumuha ng pahiwatig",
    submitAnswer = "Ipasa ang sagot",
    continueNext = "Magpatuloy sa susunod na gawain",
    categoryFits = "Angkop",
    categoryDoesNotFit = "Hindi angkop",
)

fun lessonUiStrings(language: String?): LessonUiStrings {
    val lang = language?.lowercase().orEmpty()
    return if (lang.startsWith("fil") || lang.startsWith("tl")) FIL else EN
}
