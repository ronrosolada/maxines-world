package com.maxinesworld.featurelessonplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.AssessmentRepository
import com.maxinesworld.coredatabase.RewardDao
import com.maxinesworld.coredatabase.RewardEntity
import com.maxinesworld.coremodel.AssessmentPack
import com.maxinesworld.coremodel.AssessmentPackMetadata
import com.maxinesworld.coremodel.AssessmentQuestionItem
import com.maxinesworld.coremodel.AssessmentQuestionOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

data class AssessmentArenaUiState(
    val isLoading: Boolean = true,
    val selectedSubjectId: String = "mathematics",
    val selectedCurriculum: String = "ph",
    val packs: List<AssessmentPackMetadata> = emptyList(),
    val currentPack: AssessmentPack? = null,
    val activeQuiz: ActiveAssessmentQuizState? = null,
    val passedPackIds: Set<String> = emptySet(),
    val totalStarsEarned: Int = 0,
    val totalTokensEarned: Int = 0,
    val showCelebrationDialog: Boolean = false,
)

data class ActiveAssessmentQuizState(
    val packId: String,
    val items: List<AssessmentQuestionItem>,
    val currentIndex: Int = 0,
    val selectedOptionId: String? = null,
    val isAnswerSubmitted: Boolean = false,
    val isCorrect: Boolean = false,
    val isHintVisible: Boolean = false,
    val correctCount: Int = 0,
    val isFinished: Boolean = false,
    val isPassed: Boolean = false,
    val earnedStars: Int = 0,
    val earnedTokens: Int = 0,
    /** Per-attempt display order for the current item. Authored JSON order stays on [items]. */
    val displayedOptions: List<AssessmentQuestionOption> = emptyList(),
)

@HiltViewModel
class AssessmentArenaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val assessmentRepository: AssessmentRepository,
    private val rewardDao: RewardDao,
) : ViewModel() {

    val childId: String = savedStateHandle["childId"] ?: "default_child"
    private val initialSubject: String = savedStateHandle.get<String>("subject")?.takeIf { it.isNotBlank() } ?: "mathematics"

    /** Overridable in JVM tests so two presentations can be shown to differ. */
    internal var optionRandom: Random = Random.Default

    private val _state = MutableStateFlow(
        AssessmentArenaUiState(selectedSubjectId = initialSubject)
    )
    val state: StateFlow<AssessmentArenaUiState> = _state.asStateFlow()

    init {
        loadCatalog()
        observeRewards()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val catalog = assessmentRepository.getCatalog()
            _state.update {
                it.copy(
                    isLoading = false,
                    packs = catalog.packs,
                    selectedSubjectId = initialSubject.ifBlank { "mathematics" },
                )
            }
        }
    }

    private fun observeRewards() {
        val actualChildId = childId.ifBlank { "default_child" }
        viewModelScope.launch {
            rewardDao.observeByChild(actualChildId).collect { rewards ->
                val passed = rewards
                    .filter { it.metadata.startsWith("assessment_arena_passed:") }
                    .map { it.metadata.removePrefix("assessment_arena_passed:") }
                    .toSet()
                _state.update { it.copy(passedPackIds = passed) }
            }
        }
    }

    fun selectSubject(subjectId: String) {
        _state.update { it.copy(selectedSubjectId = subjectId, currentPack = null, activeQuiz = null) }
    }

    fun selectCurriculum(curriculum: String) {
        _state.update { it.copy(selectedCurriculum = curriculum) }
    }

    fun startQuiz(packId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val pack = assessmentRepository.getPack(packId)
            if (pack != null && pack.items.isNotEmpty()) {
                val quiz = presentArenaItem(
                    ActiveAssessmentQuizState(
                        packId = pack.id,
                        items = pack.items,
                        currentIndex = 0,
                        selectedOptionId = null,
                        isAnswerSubmitted = false,
                        isCorrect = false,
                        correctCount = 0,
                        isFinished = false,
                        isPassed = false,
                    ),
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        currentPack = pack,
                        activeQuiz = quiz,
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    
    fun toggleHint() {
        val quiz = _state.value.activeQuiz ?: return
        _state.update {
            it.copy(activeQuiz = quiz.copy(isHintVisible = !quiz.isHintVisible))
        }
    }

    fun selectOption(optionId: String) {
        val quiz = _state.value.activeQuiz ?: return
        if (quiz.isAnswerSubmitted) return
        _state.update {
            it.copy(activeQuiz = quiz.copy(selectedOptionId = optionId))
        }
    }

    fun submitAnswer() {
        val quiz = _state.value.activeQuiz ?: return
        if (quiz.selectedOptionId == null || quiz.isAnswerSubmitted) return
        val currentItem = quiz.items.getOrNull(quiz.currentIndex) ?: return
        val isCorrect = isKeyedMcChoiceCorrect(
            selectedOptionId = quiz.selectedOptionId,
            presentedOptionIds = quiz.displayedOptions.map { it.id },
            correctOptionIds = currentItem.correctOptionIds,
        )
        val newCorrectCount = if (isCorrect) quiz.correctCount + 1 else quiz.correctCount

        _state.update {
            it.copy(
                activeQuiz = quiz.copy(
                    isAnswerSubmitted = true,
                    isCorrect = isCorrect,
                    correctCount = newCorrectCount,
                )
            )
        }
    }

    fun nextQuestion() {
        val quiz = _state.value.activeQuiz ?: return
        if (!quiz.isAnswerSubmitted) return

        val nextIndex = quiz.currentIndex + 1
        if (nextIndex < quiz.items.size) {
            _state.update {
                it.copy(
                    activeQuiz = presentArenaItem(
                        quiz.copy(
                            currentIndex = nextIndex,
                            selectedOptionId = null,
                            isAnswerSubmitted = false,
                            isCorrect = false,
                            isHintVisible = false,
                        ),
                    ),
                )
            }
        } else {
            // Quiz finished!
            val passed = quiz.correctCount >= 8 // >= 80%
            val starsAwarded = if (passed) {
                if (quiz.correctCount == 10) 15 else 10
            } else 0
            val tokensAwarded = if (passed) 2 else 0

            if (passed) {
                awardRewards(quiz.packId, starsAwarded, tokensAwarded)
            }

            _state.update {
                it.copy(
                    activeQuiz = quiz.copy(
                        isFinished = true,
                        isPassed = passed,
                        earnedStars = starsAwarded,
                        earnedTokens = tokensAwarded,
                    ),
                    showCelebrationDialog = passed,
                )
            }
        }
    }

    private fun awardRewards(packId: String, stars: Int, tokens: Int) {
        val actualChildId = childId.ifBlank { "default_child" }
        val subject = _state.value.selectedSubjectId
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            // Deterministic ids + IGNORE make an arena pack's stars/coins a ONE-TIME
            // reward — Retake after a pass is reward-free (no replay farming), and
            // the canonical "STAR" type is what the balances actually read.
            if (stars > 0) {
                rewardDao.insertIgnoring(
                    RewardEntity(
                        id = "assessment-arena:$actualChildId:$packId:STAR",
                        childId = actualChildId,
                        type = "STAR",
                        subject = subject,
                        amount = stars,
                        earnedAt = now,
                        metadata = "assessment_arena_passed:$packId",
                    )
                )
            }
            if (tokens > 0) {
                rewardDao.insertIgnoring(
                    RewardEntity(
                        id = "assessment-arena:$actualChildId:$packId:COIN",
                        childId = actualChildId,
                        type = "COIN",
                        subject = subject,
                        amount = tokens,
                        earnedAt = now,
                        metadata = "assessment_arena_passed:$packId",
                    )
                )
            }
        }
    }

    fun dismissCelebration() {
        _state.update { it.copy(showCelebrationDialog = false) }
    }

    fun exitQuiz() {
        _state.update { it.copy(activeQuiz = null, currentPack = null, showCelebrationDialog = false) }
    }

    fun restartQuiz() {
        val packId = _state.value.activeQuiz?.packId ?: return
        startQuiz(packId)
    }

    private fun presentArenaItem(quiz: ActiveAssessmentQuizState): ActiveAssessmentQuizState {
        val item = quiz.items.getOrNull(quiz.currentIndex)
        return quiz.copy(
            displayedOptions = if (item == null) emptyList() else shuffleMcOptions(item.options, optionRandom),
            selectedOptionId = null,
            isAnswerSubmitted = false,
        )
    }
}
