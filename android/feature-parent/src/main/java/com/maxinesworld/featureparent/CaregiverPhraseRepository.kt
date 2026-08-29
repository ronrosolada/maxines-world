package com.maxinesworld.featureparent

import com.maxinesworld.coremodel.CaregiverPhraseCard
import com.maxinesworld.coremodel.CaregiverPhraseCategory
import java.time.LocalDate
import javax.inject.Inject

class CaregiverPhraseRepository @Inject constructor() {
    fun loadCards(): List<CaregiverPhraseCard> = ESSENTIAL_CARDS

    fun filterCards(
        query: String = "",
        category: CaregiverPhraseCategory? = null,
    ): List<CaregiverPhraseCard> {
        val term = query.trim()
        return ESSENTIAL_CARDS.filter { card ->
            (category == null || card.category == category) &&
                (term.isEmpty() || listOf(card.filipinoPhrase, card.englishCue, card.practicalTip)
                    .any { it.contains(term, ignoreCase = true) })
        }
    }
}

class CaregiverPracticeTracker {
    private val practicedByDate = mutableMapOf<LocalDate, MutableSet<String>>()

    fun isPracticed(cardId: String, date: LocalDate = LocalDate.now()): Boolean =
        cardId in practicedByDate[date].orEmpty()

    fun setPracticed(cardId: String, practiced: Boolean, date: LocalDate = LocalDate.now()) {
        val cards = practicedByDate.getOrPut(date) { mutableSetOf() }
        if (practiced) cards += cardId else cards -= cardId
    }
}

private fun card(id: String, category: CaregiverPhraseCategory, filipino: String, english: String, tip: String) =
    CaregiverPhraseCard(id, category, filipino, english, tip)

private val ESSENTIAL_CARDS = listOf(
    card("greeting-good-day", CaregiverPhraseCategory.GREETINGS, "Magandang araw", "Good day", "Use this cheerful greeting when Maxine wakes up."),
    card("greeting-good-morning", CaregiverPhraseCategory.GREETINGS, "Magandang umaga", "Good morning", "Say this together at breakfast."),
    card("greeting-how-are-you", CaregiverPhraseCategory.GREETINGS, "Kumusta ka?", "How are you?", "Ask after school and pause for an answer."),
    card("greeting-good-night", CaregiverPhraseCategory.GREETINGS, "Magandang gabi", "Good evening", "Use this as the evening routine begins."),
    card("basic-needs-water", CaregiverPhraseCategory.BASIC_NEEDS, "Pahingi po ng tubig", "Please may I have water", "Model this before handing over a drink."),
    card("basic-needs-food", CaregiverPhraseCategory.BASIC_NEEDS, "Gutom na po ako", "I am hungry", "Practice before snacks or meals."),
    card("basic-needs-help", CaregiverPhraseCategory.BASIC_NEEDS, "Tulungan mo po ako", "Please help me", "Prompt this when a task feels difficult."),
    card("basic-needs-bathroom", CaregiverPhraseCategory.BASIC_NEEDS, "Kailangan ko pong pumunta sa banyo", "I need to go to the bathroom", "Rehearse before leaving home."),
    card("praise-great", CaregiverPhraseCategory.PRAISES, "Ang galing mo!", "You did great!", "Celebrate effort after a home or school task."),
    card("praise-proud", CaregiverPhraseCategory.PRAISES, "Ipinagmamalaki kita", "I am proud of you", "Say what specific effort made you proud."),
    card("praise-good-job", CaregiverPhraseCategory.PRAISES, "Magaling!", "Well done!", "Use after Maxine tries, not only when correct."),
    card("praise-keep-going", CaregiverPhraseCategory.PRAISES, "Kaya mo iyan!", "You can do it!", "Encourage Maxine before another attempt."),
    card("routine-eat", CaregiverPhraseCategory.DAILY_ROUTINE, "Kain na tayo", "Let us eat", "Invite the family to the table with this phrase."),
    card("routine-bath", CaregiverPhraseCategory.DAILY_ROUTINE, "Maligo na tayo", "Let us take a bath", "Use as the consistent cue for bath time."),
    card("routine-clean", CaregiverPhraseCategory.DAILY_ROUTINE, "Magligpit na tayo", "Let us tidy up", "Say it while tidying together."),
    card("routine-sleep", CaregiverPhraseCategory.DAILY_ROUTINE, "Oras na para matulog", "It is time to sleep", "Use as the first cue in the bedtime routine."),
    card("routine-school", CaregiverPhraseCategory.DAILY_ROUTINE, "Handa ka na ba sa paaralan?", "Are you ready for school?", "Ask while checking the school bag together."),
    card("routine-thank-you", CaregiverPhraseCategory.DAILY_ROUTINE, "Salamat po", "Thank you", "Model this whenever someone helps at home."),
)
