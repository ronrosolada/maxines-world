package com.maxinesworld.featureauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.coredatabase.ParentAccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true,
    val hasPin: Boolean = false,
    val displayName: String = "",
    val pinInput: String = "",
    val pinError: String? = null,
    val isAuthenticated: Boolean = false,
    val childProfiles: List<ChildProfileEntity> = emptyList(),
    val selectedChildId: String? = null,
    val showCreateProfile: Boolean = false,
    val newChildName: String = "",
    val currentScreen: AuthScreen = AuthScreen.LOADING
)

enum class AuthScreen {
    LOADING, PIN_SETUP, PIN_LOGIN, CHILD_SELECT, CREATE_PROFILE
}

@HiltViewModel
class ParentAuthViewModel @Inject constructor(
    private val authManager: ParentAuthManager,
    private val parentAccountDao: ParentAccountDao,
    private val childProfileDao: ChildProfileDao
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val pinHash = authManager.getPinHash()
            val parent = parentAccountDao.getParent()
            val children = parent?.let { childProfileDao.getByParent(it.id) } ?: emptyList()

            authManager.displayName.collect { name ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        hasPin = pinHash != null,
                        displayName = name ?: parent?.displayName ?: "",
                        childProfiles = children,
                        currentScreen = when {
                            pinHash == null -> AuthScreen.PIN_SETUP
                            children.isEmpty() -> AuthScreen.CREATE_PROFILE
                            else -> AuthScreen.PIN_LOGIN
                        }
                    )
                }
            }
        }
    }

    fun onPinDigit(digit: String) {
        _state.update {
            val newInput = (it.pinInput + digit).take(6)
            it.copy(pinInput = newInput, pinError = null)
        }
        if (_state.value.pinInput.length == 6) {
            verifyPin()
        }
    }

    fun onPinDelete() {
        _state.update { it.copy(pinInput = it.pinInput.dropLast(1), pinError = null) }
    }

    private fun verifyPin() {
        viewModelScope.launch {
            val pinHash = authManager.getPinHash()
            val input = _state.value.pinInput

            if (pinHash != null && authManager.verifyPin(input, pinHash)) {
                onAuthenticated()
            } else {
                _state.update { it.copy(pinInput = "", pinError = "Incorrect PIN. Try again.") }
            }
        }
    }

    fun onSetupPin() {
        viewModelScope.launch {
            val pin = _state.value.pinInput
            if (pin.length != 6) {
                _state.update { it.copy(pinError = "PIN must be exactly 6 digits") }
                return@launch
            }
            val name = _state.value.displayName.ifBlank { "Parent" }
            authManager.setPin(pin, name)

            val parent = ParentAccountEntity(
                id = UUID.randomUUID().toString(),
                displayName = name,
                pinHash = authManager.hashPin(pin)
            )
            parentAccountDao.upsert(parent)
            _state.update {
                it.copy(
                    hasPin = true,
                    currentScreen = AuthScreen.CREATE_PROFILE
                )
            }
        }
    }

    fun onAuthenticated() {
        viewModelScope.launch {
            val parent = parentAccountDao.getParent()
            val children = parent?.let { childProfileDao.getByParent(it.id) } ?: emptyList()
            _state.update {
                it.copy(
                    isAuthenticated = true,
                    childProfiles = children,
                    pinInput = "",
                    pinError = null,
                    currentScreen = if (children.isEmpty()) AuthScreen.CREATE_PROFILE
                    else AuthScreen.CHILD_SELECT
                )
            }
        }
    }

    fun onCreateChild(name: String) {
        viewModelScope.launch {
            val parent = parentAccountDao.getParent() ?: return@launch
            val child = ChildProfileEntity(
                id = UUID.randomUUID().toString(),
                parentId = parent.id,
                name = name.ifBlank { "Maxine" }
            )
            childProfileDao.upsert(child)
            _state.update {
                it.copy(
                    childProfiles = it.childProfiles + child,
                    selectedChildId = child.id,
                    showCreateProfile = false,
                    newChildName = "",
                    currentScreen = AuthScreen.CHILD_SELECT
                )
            }
        }
    }

    fun onSelectChild(childId: String) {
        _state.update { it.copy(selectedChildId = childId) }
    }

    fun onUpdateName(name: String) {
        _state.update { it.copy(displayName = name) }
    }

    fun onUpdateNewChildName(name: String) {
        _state.update { it.copy(newChildName = name) }
    }

    fun onShowCreateProfile() {
        _state.update { it.copy(showCreateProfile = true) }
    }

    fun onHideCreateProfile() {
        _state.update { it.copy(showCreateProfile = false) }
    }
}
