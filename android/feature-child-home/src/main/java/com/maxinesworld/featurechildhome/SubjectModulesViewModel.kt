package com.maxinesworld.featurechildhome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.maxinesworld.corecontent.ContentModule
import com.maxinesworld.corecontent.ModuleCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectModulesState(
    val isLoading: Boolean = true,
    val modules: List<ContentModule> = emptyList(),
    val error: String? = null
)

/**
 * Loads the module list for one subject from the bundled content pack
 * (subject → modules → lessons → lesson player).
 */
@HiltViewModel
class SubjectModulesViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val subject: String = checkNotNull(savedStateHandle["subject"])

    private val _state = MutableStateFlow(SubjectModulesState())
    val state: StateFlow<SubjectModulesState> = _state.asStateFlow()

    init {
        val catalog = ModuleCatalog(application)
        viewModelScope.launch {
            val modules = catalog.modulesFor(subject)
            _state.value = if (modules.isEmpty()) {
                SubjectModulesState(isLoading = false, error = "No modules found for $subject")
            } else {
                SubjectModulesState(isLoading = false, modules = modules)
            }
        }
    }
}
