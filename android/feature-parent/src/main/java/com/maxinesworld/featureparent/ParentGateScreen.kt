package com.maxinesworld.featureparent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxinesworld.coredesignsystem.theme.*
import com.maxinesworld.featureauth.ParentAuthManager
import com.maxinesworld.featureauth.PinDots
import com.maxinesworld.featureauth.PinPad
import com.maxinesworld.featureauth.generateParentChallenge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class ParentGateState(
    val pinInput: String = "",
    val pinError: String? = null,
    val isAuthenticated: Boolean = false,
    val attempts: Int = 0,
    /** Epoch millis until PIN entry unlocks; 0 when not locked. */
    val lockedUntilEpochMillis: Long = 0L,
    /** Live whole seconds remaining in the current lockout. */
    val lockRemainingSeconds: Int = 0,
)

@HiltViewModel
class ParentGateViewModel @Inject constructor(
    private val authManager: ParentAuthManager
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(ParentGateState())
    val state: StateFlow<ParentGateState> = _state.asStateFlow()
    private var lockCountdownJob: Job? = null

    init {
        viewModelScope.launch {
            val lockedUntil = authManager.getLockedUntilEpochMillis()
            if (lockedUntil > System.currentTimeMillis()) {
                startLockCountdown(lockedUntil)
            }
        }
    }

    fun onPinDigit(digit: String) {
        if (isLockedNow() || _state.value.lockRemainingSeconds > 0) return
        _state.update {
            val newInput = (it.pinInput + digit).take(6)
            it.copy(pinInput = newInput, pinError = null)
        }
        if (_state.value.pinInput.length == 6) verifyPin()
    }

    fun onPinDelete() {
        if (isLockedNow() || _state.value.lockRemainingSeconds > 0) return
        _state.update { it.copy(pinInput = it.pinInput.dropLast(1), pinError = null) }
    }

    private fun isLockedNow(): Boolean {
        val lockedUntil = _state.value.lockedUntilEpochMillis
        return lockedUntil > System.currentTimeMillis()
    }

    private var verificationInFlight = false

    private fun verifyPin() {
        if (verificationInFlight) return
        verificationInFlight = true
        viewModelScope.launch {
            try {
                authManager.getPinHash()
                val input = _state.value.pinInput
                val now = System.currentTimeMillis()

                // Persisted lockout from a previous session must still apply.
                val persistedLockedUntil = authManager.getLockedUntilEpochMillis()
                if (persistedLockedUntil > now) {
                    _state.update {
                        it.copy(
                            pinInput = "",
                            lockedUntilEpochMillis = persistedLockedUntil,
                            lockRemainingSeconds = parentGateRemainingSeconds(persistedLockedUntil, now),
                            pinError = "Too many attempts."
                        )
                    }
                    startLockCountdown(persistedLockedUntil)
                    return@launch
                }

                if (authManager.verifyPin(input)) {
                    authManager.resetFailedAttempts()
                    _state.update {
                        it.copy(
                            isAuthenticated = true,
                            pinInput = "",
                            pinError = null,
                            lockedUntilEpochMillis = 0L,
                            lockRemainingSeconds = 0,
                        )
                    }
                } else {
                    val newLockedUntil = authManager.recordFailedAttempt(now)
                    val attempts = authManager.getFailedAttempts()
                    _state.update {
                        if (newLockedUntil > 0) {
                            it.copy(
                                pinInput = "",
                                attempts = attempts,
                                lockedUntilEpochMillis = newLockedUntil,
                                lockRemainingSeconds = parentGateRemainingSeconds(newLockedUntil, now),
                                pinError = "Too many attempts."
                            )
                        } else {
                            val left = ParentAuthManager.MAX_ATTEMPTS_BEFORE_LOCK - attempts
                            it.copy(
                                pinInput = "",
                                attempts = attempts,
                                lockedUntilEpochMillis = 0L,
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
                lockRemainingSeconds = parentGateRemainingSeconds(
                    lockedUntilEpochMillis,
                    System.currentTimeMillis(),
                ),
            )
        }
        lockCountdownJob = viewModelScope.launch {
            while (isActive) {
                val remaining = parentGateRemainingSeconds(
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

    /**
     * Resets parent PIN safely without clearing child data in Room.
     */
    fun onResetPin(onResetComplete: () -> Unit) {
        viewModelScope.launch {
            lockCountdownJob?.cancel()
            authManager.resetPinOnly()
            _state.update {
                it.copy(
                    pinInput = "",
                    pinError = null,
                    attempts = 0,
                    lockedUntilEpochMillis = 0L,
                    lockRemainingSeconds = 0,
                )
            }
            onResetComplete()
        }
    }

    override fun onCleared() {
        lockCountdownJob?.cancel()
        super.onCleared()
    }
}

internal fun parentGateRemainingSeconds(lockedUntilEpochMillis: Long, nowEpochMillis: Long): Int =
    if (lockedUntilEpochMillis <= nowEpochMillis) {
        0
    } else {
        ((lockedUntilEpochMillis - nowEpochMillis) / 1_000L + 1L).toInt()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentGateScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit,
    viewModel: ParentGateViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val locked = state.lockRemainingSeconds > 0
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onAuthenticated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Access") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Cream)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Parent PIN",
                tint = Teal40,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Enter Parent PIN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Teal40,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            PinDots(length = state.pinInput.length)
            Spacer(Modifier.height(20.dp))

            PinPad(enabled = !locked) { digit -> viewModel.onPinDigit(digit) }
            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = viewModel::onPinDelete,
                enabled = !locked && state.pinInput.isNotEmpty(),
            ) {
                Text("Delete")
            }

            if (locked) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SunshineGold.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SunshineGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Please take a short pause (${state.lockRemainingSeconds}s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Teal40,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Keypad will unlock automatically in ${state.lockRemainingSeconds} seconds, or answer a quick math question below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Kusang magbubukas pagkatapos ng ${state.lockRemainingSeconds} segundo, o sagutan ang simpleng tanong sa ibaba.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else state.pinError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Forgot PIN or restore default using parent math challenge" },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (locked) Teal40 else SurfaceContainer, contentColor = if (locked) White else Teal40)
            ) {
                Text(
                    if (locked) "Bypass lockout with quick math question" else "Forgot PIN? Answer quick math question",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showResetDialog) {
        val challenge = remember { generateParentChallenge() }
        var answerInput by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("Restore Default Parent PIN", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "To restore the default parent PIN without losing Maxine's learning progress and stickers, please answer the parent verification question:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Teal90.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            challenge.questionPromptEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Teal40,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = {
                            answerInput = it
                            errorText = null
                        },
                        label = { Text("Answer") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorText != null) {
                        Text(errorText!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (challenge.verify(answerInput)) {
                            showResetDialog = false
                            viewModel.onResetPin {
                                onBack()
                            }
                        } else {
                            errorText = "Incorrect answer. Please try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal40)
                ) {
                    Text("Verify & Restore PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
