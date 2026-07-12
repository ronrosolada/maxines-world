package com.maxinesworld.featureauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredesignsystem.theme.*

@Composable
fun ParentAuthScreen(
    onChildSelected: (String) -> Unit,
    viewModel: ParentAuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.currentScreen) {
        AuthScreen.LOADING -> LoadingScreen()
        AuthScreen.PIN_SETUP -> PinSetupScreen(state, viewModel)
        AuthScreen.PIN_LOGIN -> PinLoginScreen(state, viewModel)
        AuthScreen.CHILD_SELECT -> ChildSelectScreen(state, viewModel, onChildSelected)
        AuthScreen.CREATE_PROFILE -> CreateChildScreen(state, viewModel)
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Teal40)
    }
}

@Composable
private fun PinSetupScreen(state: AuthUiState, viewModel: ParentAuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🐱", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Welcome to Maxine's World!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Teal40
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Set up a PIN to keep the parent area secure.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onUpdateName,
            label = { Text("Your name") },
            leadingIcon = { Icon(Icons.Default.Person, "Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(16.dp))

        Text("Choose a PIN", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        PinDots(length = state.pinInput.length)
        Spacer(Modifier.height(16.dp))

        PinPad { digit -> viewModel.onPinDigit(digit) }
        Spacer(Modifier.height(16.dp))

        state.pinError?.let {
            Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = viewModel::onSetupPin,
            enabled = state.pinInput.length >= 4,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
        ) {
            Text("Set PIN", fontSize = 18.sp)
        }
    }
}

@Composable
private fun PinLoginScreen(state: AuthUiState, viewModel: ParentAuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Parent Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Teal40
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Enter your PIN to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        PinDots(length = state.pinInput.length)
        Spacer(Modifier.height(24.dp))

        PinPad { digit -> viewModel.onPinDigit(digit) }
        Spacer(Modifier.height(12.dp))

        // Delete button
        TextButton(onClick = viewModel::onPinDelete) {
            Text("Delete")
        }

        state.pinError?.let {
            Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Biometric option
        IconButton(onClick = { /* Triggers BiometricPrompt via Activity */ }) {
            Icon(
                Icons.Default.Fingerprint,
                "Use fingerprint",
                tint = Teal40,
                modifier = Modifier.size(48.dp)
            )
        }
        Text("Or use fingerprint", style = MaterialTheme.typography.labelSmall, color = Teal40)
    }
}

@Composable
private fun ChildSelectScreen(
    state: AuthUiState,
    viewModel: ParentAuthViewModel,
    onChildSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Who's learning today?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Teal40
        )
        Spacer(Modifier.height(24.dp))

        state.childProfiles.forEach { child ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onChildSelected(child.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Teal90),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("😺", fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(child.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            "Grade ${child.grade}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = viewModel::onShowCreateProfile,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("+ Add another child", fontSize = 16.sp)
        }
    }
}

@Composable
private fun CreateChildScreen(state: AuthUiState, viewModel: ParentAuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌟", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Create Child Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Teal40
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.newChildName,
            onValueChange = viewModel::onUpdateNewChildName,
            label = { Text("Child's name") },
            placeholder = { Text("Maxine") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.onCreateChild(state.newChildName) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
        ) {
            Text("Start Learning! 🚀", fontSize = 18.sp)
        }
    }
}

@Composable
fun PinDots(length: Int, maxLength: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(maxLength) { index ->
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < length) Teal40
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun PinPad(onDigit: (String) -> Unit) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "")
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        digits.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { digit ->
                    if (digit.isEmpty()) {
                        Spacer(Modifier.size(72.dp))
                    } else {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Teal90)
                                .clickable { onDigit(digit) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                digit,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Teal40
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
