package com.maxinesworld.featurechildhome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ContentModuleLesson
import com.maxinesworld.corecontent.ModuleCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModuleLessonsState(
    val isLoading: Boolean = true,
    val moduleTitle: String = "",
    val lessons: List<ContentModuleLesson> = emptyList(),
    val error: String? = null
)

/**
 * Loads the lesson list for one module of a subject from the bundled
 * content pack.
 */
@HiltViewModel
class ModuleLessonsViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val subject: String = checkNotNull(savedStateHandle["subject"])
    private val moduleKey: String = checkNotNull(savedStateHandle["moduleKey"])

    private val _state = MutableStateFlow(ModuleLessonsState())
    val state: StateFlow<ModuleLessonsState> = _state.asStateFlow()

    init {
        val catalog = ModuleCatalog(application)
        viewModelScope.launch {
            val module = catalog.modulesFor(subject).firstOrNull { it.key == moduleKey }
            _state.value = if (module == null) {
                ModuleLessonsState(isLoading = false, error = "Module not found")
            } else {
                ModuleLessonsState(
                    isLoading = false,
                    moduleTitle = module.title,
                    lessons = module.lessons
                )
            }
        }
    }
}
