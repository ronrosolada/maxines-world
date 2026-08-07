package com.maxinesworld.featureauth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
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

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler(enabled = state.currentScreen == AuthScreen.PIN_SETUP || state.currentScreen == AuthScreen.CREATE_PROFILE) {
        if (state.currentScreen == AuthScreen.CREATE_PROFILE && state.childProfiles.isNotEmpty()) {
            viewModel.onHideCreateProfile()
        } else {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        when (state.currentScreen) {
            AuthScreen.LOADING -> LoadingScreen()
            AuthScreen.PIN_SETUP -> PinSetupScreen(state, viewModel)
            AuthScreen.PIN_LOGIN -> PinLoginScreen(state, viewModel)
            AuthScreen.CHILD_SELECT -> ChildSelectScreen(state, viewModel, onChildSelected)
            AuthScreen.CREATE_PROFILE -> CreateChildScreen(state, viewModel)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Teal40)
    }
}

@Composable
internal fun PinSetupScreen(state: AuthUiState, viewModel: ParentAuthViewModel) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    PinSetupContent(
        state = state,
        onUpdateName = viewModel::onUpdateName,
        onPinDigit = viewModel::onPinDigit,
        onPinDelete = viewModel::onPinDelete,
        onSetupPin = viewModel::onSetupPin,
        onPinPadInteraction = {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PinSetupContent(
    state: AuthUiState,
    onUpdateName: (String) -> Unit,
    onPinDigit: (String) -> Unit,
    onPinDelete: () -> Unit,
    onSetupPin: () -> Unit,
    onPinPadInteraction: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val scrollState = rememberScrollState()
    val deleteRequester = remember { BringIntoViewRequester() }

    // When the name field opens the keyboard, Compose keeps focus on that field
    // and the scroll position near the top. The keypad's last row is then under
    // the IME. Bring the bottom control into the reduced viewport so the 0 key,
    // Delete, and the fixed Set PIN action remain reachable together.
    LaunchedEffect(imeVisible) {
        if (imeVisible) deleteRequester.bringIntoView()
    }

    // Apply IME insets to the whole screen, not only the footer. When the
    // name field opens the software keyboard, the scrollable content and the
    // action footer must share the reduced viewport; padding only the footer
    // can push that footer below the IME on large tablet layouts.
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (imeVisible) Arrangement.Top else Arrangement.Center,
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Teal40, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Welcome to Maxine's World!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Teal40,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Set up a PIN to keep the parent area secure.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = state.displayName,
                onValueChange = onUpdateName,
                label = { Text("Parent or guardian name (optional)") },
                leadingIcon = { Icon(Icons.Default.Person, "Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }),
            )
            Spacer(Modifier.height(16.dp))

            Text("Choose a 6-digit PIN", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            PinDots(length = state.pinInput.length)
            Text(
                "${state.pinInput.length} of 6 digits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            PinPad(
                onInteraction = onPinPadInteraction,
            ) { digit -> onPinDigit(digit) }
            Spacer(Modifier.height(12.dp))

            // Delete/backspace — a mis-tapped digit must be correctable without
            // restarting the app (adversarial UX review #31).
            TextButton(
                onClick = onPinDelete,
                enabled = state.pinInput.isNotEmpty(),
                modifier = Modifier.bringIntoViewRequester(deleteRequester),
            ) {
                Text("Delete")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.pinError?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = onSetupPin,
                enabled = state.pinInput.length == 6,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal40),
            ) {
                Text("Set PIN", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun PinLoginScreen(state: AuthUiState, viewModel: ParentAuthViewModel) {
    val locked = state.lockRemainingSeconds > 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = "Parent access",
            tint = Teal40,
            modifier = Modifier.size(64.dp),
        )
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
        Text(
            "${state.pinInput.length} of 6 digits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        PinPad(enabled = !locked) { digit -> viewModel.onPinDigit(digit) }
        Spacer(Modifier.height(12.dp))

        // Delete button
        TextButton(
            onClick = viewModel::onPinDelete,
            enabled = !locked && state.pinInput.isNotEmpty(),
        ) {
            Text("Delete")
        }

        if (locked) {
            Text(
                "Too many attempts. Try again in ${state.lockRemainingSeconds}s.",
                color = ErrorRed,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else state.pinError?.let {
            Text(it, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // ─── Biometric option removed — not yet functional ───
        // Per audit recommendation: never show a control that suggests protection
        // that does not exist. BiometricPrompt will be re-added when implemented.

        Spacer(Modifier.height(16.dp))
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
                        Icon(Icons.Default.Person, contentDescription = null, tint = Teal40, modifier = Modifier.size(28.dp))
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = SunshineGold, modifier = Modifier.size(64.dp))
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
            label = { Text("Child's name (required)") },
            placeholder = { Text("Type your child's name") },
            isError = state.childNameError != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            state.childNameError ?: "Your child's name — then we can begin!",
            color = if (state.childNameError != null) ErrorRed else Teal40.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.onCreateChild(state.newChildName) },
            enabled = state.newChildName.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
        ) {
            Text("Start Learning!", fontSize = 18.sp)
        }
    }
}

@Composable
fun PinDots(length: Int, maxLength: Int = 6) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics {
            contentDescription = "$length of $maxLength digits entered"
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        repeat(maxLength) { index ->
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < length) Teal40
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (index >= length) {
                            Modifier.border(1.dp, Teal40.copy(alpha = 0.4f), CircleShape)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@Composable
fun PinPad(
    enabled: Boolean = true,
    onInteraction: () -> Unit = {},
    onDigit: (String) -> Unit,
) {
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
                                .clickable(enabled = enabled, role = Role.Button) {
                                    onInteraction()
                                    onDigit(digit)
                                }
                                .semantics {
                                    contentDescription = "Digit $digit"
                                    role = Role.Button
                                    if (!enabled) disabled()
                                },
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
