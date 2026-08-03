package com.maxinesworld.featurechildhome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ContentModuleLesson
import com.maxinesworld.corecontent.ModuleCatalog
import com.maxinesworld.coredatabase.LessonCompletionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModuleLessonsState(
    val isLoading: Boolean = true,
    val moduleTitle: String = "",
    val lessons: List<ContentModuleLesson> = emptyList(),
    val error: String? = null,
    /** Lesson IDs the child has already completed (persisted completions). */
    val completedLessonIds: Set<String> = emptySet(),
    /** First incomplete lesson in module order — the recommended next tap. */
    val nextLessonId: String? = null,
) {
    val completedCount: Int get() = completedLessonIds.size
    val totalCount: Int get() = lessons.size
}

/**
 * First lesson in module order that the child has not yet completed, or null
 * when every lesson is complete. Pure function — unit-testable.
 */
internal fun nextLessonId(lessons: List<ContentModuleLesson>, completed: Set<String>): String? =
    lessons.firstOrNull { it.lessonId !in completed }?.lessonId

/**
 * Loads the lesson list for one module of a subject from the bundled
 * content pack and layers persisted completion state on top so the list
 * can show finished lessons, the current lesson, and a resume point.
 */
@HiltViewModel
class ModuleLessonsViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val lessonCompletionDao: LessonCompletionDao,
) : AndroidViewModel(application) {

    private val subject: String = checkNotNull(savedStateHandle["subject"])
    private val moduleKey: String = checkNotNull(savedStateHandle["moduleKey"])
    private val childId: String = checkNotNull(savedStateHandle["childId"])

    private val _state = MutableStateFlow(ModuleLessonsState())
    val state: StateFlow<ModuleLessonsState> = _state.asStateFlow()

    init {
        val catalog = ModuleCatalog(application)
        viewModelScope.launch {
            val module = catalog.modulesFor(subject).firstOrNull { it.key == moduleKey }
            if (module == null) {
                _state.value = ModuleLessonsState(isLoading = false, error = "Module not found")
                return@launch
            }
            _state.value = ModuleLessonsState(
                isLoading = false,
                moduleTitle = module.title,
                lessons = module.lessons
            )
            // Layer completion state; recompute resume point on every change.
            lessonCompletionDao.observeDistinctLessonIds(childId).collect { completed ->
                _state.update {
                    it.copy(
                        completedLessonIds = completed.toSet(),
                        nextLessonId = nextLessonId(it.lessons, completed.toSet())
                    )
                }
            }
        }
    }
}
