package com.maxinesworld.featureparent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class ParentGateState(
    val pinInput: String = "",
    val pinError: String? = null,
    val isAuthenticated: Boolean = false,
    val attempts: Int = 0
)

@HiltViewModel
class ParentGateViewModel @Inject constructor(
    private val authManager: ParentAuthManager
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(ParentGateState())
    val state: StateFlow<ParentGateState> = _state.asStateFlow()
    private var locked = false

    fun onPinDigit(digit: String) {
        if (locked) return
        _state.update {
            val newInput = (it.pinInput + digit).take(6)
            it.copy(pinInput = newInput, pinError = null)
        }
        if (_state.value.pinInput.length == 6) verifyPin()
    }

    fun onPinDelete() {
        if (locked) return
        _state.update { it.copy(pinInput = it.pinInput.dropLast(1), pinError = null) }
    }

    private fun verifyPin() {
        viewModelScope.launch {
            val pinHash = authManager.getPinHash()
            val input = _state.value.pinInput
            val newAttempts = _state.value.attempts + 1

            if (pinHash != null && authManager.verifyPin(input)) {
                _state.update { it.copy(isAuthenticated = true, pinInput = "", pinError = null) }
            } else {
                if (newAttempts >= 5) {
                    locked = true
                    _state.update { it.copy(pinInput = "", pinError = "Too many attempts. Please wait.", attempts = newAttempts) }
                } else {
                    _state.update { it.copy(pinInput = "", pinError = "Incorrect PIN. ${5 - newAttempts} tries left.", attempts = newAttempts) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentGateScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit,
    viewModel: ParentGateViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔒", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Enter Parent PIN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Teal40,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            PinDots(length = state.pinInput.length)
            Spacer(Modifier.height(24.dp))

            PinPad { digit -> viewModel.onPinDigit(digit) }
            Spacer(Modifier.height(12.dp))

            TextButton(onClick = viewModel::onPinDelete) {
                Text("Delete")
            }

            state.pinError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
