package com.maxinesworld.coremodel

enum class CaregiverPhraseCategory(val displayName: String) {
    GREETINGS("Greetings"),
    BASIC_NEEDS("Basic Needs"),
    PRAISES("Praises"),
    DAILY_ROUTINE("Daily Routine"),
}

data class CaregiverPhraseCard(
    val id: String,
    val category: CaregiverPhraseCategory,
    val filipinoPhrase: String,
    val englishCue: String,
    val practicalTip: String,
)
