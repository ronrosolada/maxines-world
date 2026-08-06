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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val childNameError: String? = null,
    val currentScreen: AuthScreen = AuthScreen.LOADING,
    /** Consecutive failed PIN attempts (survives process restarts). */
    val failedAttempts: Int = 0,
    /** Epoch millis until PIN entry unlocks; 0 when not locked. */
    val lockedUntilEpochMillis: Long = 0L,
    /** Live whole seconds remaining in the current lockout. */
    val lockRemainingSeconds: Int = 0,
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
    private var lockCountdownJob: Job? = null

    init {
        viewModelScope.launch {
            val pinHash = authManager.getPinHash()
            val parent = parentAccountDao.getParent()
            val children = parent?.let { childProfileDao.getByParent(it.id) } ?: emptyList()

            val lockedUntil = authManager.getLockedUntilEpochMillis()
            if (lockedUntil > System.currentTimeMillis()) {
                _state.update {
                    it.copy(
                        lockedUntilEpochMillis = lockedUntil,
                        lockRemainingSeconds = lockRemainingSeconds(lockedUntil, System.currentTimeMillis()),
                    )
                }
                startLockCountdown(lockedUntil)
            }

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
        if (_state.value.lockRemainingSeconds > 0) return
        _state.update {
            val newInput = (it.pinInput + digit).take(6)
            it.copy(pinInput = newInput, pinError = null)
        }
        // Auto-verify only during PIN login, not during setup
        if (_state.value.pinInput.length == 6 && _state.value.currentScreen == AuthScreen.PIN_LOGIN) {
            verifyPin()
        }
    }

    fun onPinDelete() {
        if (_state.value.lockRemainingSeconds > 0) return
        _state.update { it.copy(pinInput = it.pinInput.dropLast(1), pinError = null) }
    }

    private var verificationInFlight = false

    private fun verifyPin() {
        if (verificationInFlight) return
        verificationInFlight = true
        viewModelScope.launch {
            try {
                val pinHash = authManager.getPinHash()
                val input = _state.value.pinInput
                val now = System.currentTimeMillis()
                val lockedUntil = authManager.getLockedUntilEpochMillis()

                if (lockedUntil > now) {
                    // Lockout still active: reject even a correct PIN.
                    _state.update {
                        it.copy(
                            pinInput = "",
                            lockedUntilEpochMillis = lockedUntil,
                            lockRemainingSeconds = lockRemainingSeconds(lockedUntil, now),
                            pinError = "Too many attempts."
                        )
                    }
                    startLockCountdown(lockedUntil)
                    return@launch
                }

                if (pinHash != null && authManager.verifyPin(input)) {
                    authManager.resetFailedAttempts()
                    _state.update { it.copy(failedAttempts = 0, lockedUntilEpochMillis = 0L) }
                    onAuthenticated()
                } else {
                    val newLockedUntil = authManager.recordFailedAttempt(now)
                    val attempts = authManager.getFailedAttempts()
                    _state.update {
                        if (newLockedUntil > 0) {
                            it.copy(
                                pinInput = "",
                                failedAttempts = attempts,
                                lockedUntilEpochMillis = newLockedUntil,
                                lockRemainingSeconds = lockRemainingSeconds(newLockedUntil, now),
                                pinError = "Too many attempts."
                            )
                        } else {
                            val left = ParentAuthManager.MAX_ATTEMPTS_BEFORE_LOCK - attempts
                            it.copy(
                                pinInput = "",
                                failedAttempts = attempts,
                                pinError = "Incorrect PIN. $left attempt${if (left == 1) "" else "s"} left."
                            )
                        }
                    }
                    if (newLockedUntil > 0) startLockCountdown(newLockedUntil)
                }
            } finally {
                verificationInFlight = false
            }
        }
    }

    private fun startLockCountdown(lockedUntilEpochMillis: Long) {
        lockCountdownJob?.cancel()
        _state.update {
            it.copy(
                lockedUntilEpochMillis = lockedUntilEpochMillis,
                lockRemainingSeconds = lockRemainingSeconds(lockedUntilEpochMillis, System.currentTimeMillis()),
            )
        }
        lockCountdownJob = viewModelScope.launch {
            while (isActive) {
                val remaining = lockRemainingSeconds(
                    lockedUntilEpochMillis,
                    System.currentTimeMillis(),
                )
                if (remaining == 0) {
                    _state.update {
                        it.copy(
                            lockedUntilEpochMillis = 0L,
                            lockRemainingSeconds = 0,
                            pinError = null,
                        )
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        lockedUntilEpochMillis = lockedUntilEpochMillis,
                        lockRemainingSeconds = remaining,
                    )
                }
                delay(500)
            }
        }
    }

    override fun onCleared() {
        lockCountdownJob?.cancel()
        super.onCleared()
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

            // Single-row invariant (audit F1, 2026-08-06): a fresh UUID here
            // could INSERT a second parent row while DataStore was temporarily
            // empty, leaving child data bound to the old row. Reuse the
            // existing row's id when present (REPLACE upserts it), otherwise
            // the constant "parent" id makes REPLACE a true single-row upsert.
            val existing = parentAccountDao.getParent()
            val parent = ParentAccountEntity(
                id = existing?.id ?: "parent",
                displayName = name,
                pinHash = "" // no longer stored in Room — DataStore is the single source
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
            // No silent "Maxine" fallback — an empty name is an error, not a
            // license to create a ghost profile named after the developer's
            // daughter. (Adversarial UX review #27.)
            if (name.isBlank()) {
                _state.update { it.copy(childNameError = "Please type your child's name first.") }
                return@launch
            }
            val parent = parentAccountDao.getParent() ?: return@launch
            val child = ChildProfileEntity(
                id = UUID.randomUUID().toString(),
                parentId = parent.id,
                name = name.trim()
            )
            childProfileDao.upsert(child)
            _state.update {
                it.copy(
                    childProfiles = it.childProfiles + child,
                    selectedChildId = child.id,
                    showCreateProfile = false,
                    newChildName = "",
                    childNameError = null,
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
        _state.update {
            it.copy(
                showCreateProfile = true,
                childNameError = null,
                currentScreen = AuthScreen.CREATE_PROFILE
            )
        }
    }

    fun onHideCreateProfile() {
        _state.update {
            it.copy(
                showCreateProfile = false,
                childNameError = null,
                newChildName = "",
                // Return to the picker when profiles exist; otherwise fall
                // back to the parent gate (fresh install has no picker yet).
                currentScreen = if (it.childProfiles.isNotEmpty()) AuthScreen.CHILD_SELECT
                else AuthScreen.PIN_LOGIN
            )
        }
    }
}

internal fun lockRemainingSeconds(lockedUntilEpochMillis: Long, nowEpochMillis: Long): Int =
    if (lockedUntilEpochMillis <= nowEpochMillis) {
        0
    } else {
        ((lockedUntilEpochMillis - nowEpochMillis) / 1_000L + 1L).toInt()
    }
