package com.maxinesworld.featureauth

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class AuthImeLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingPinPadRequestsNameKeyboardDismissal() {
        val state = AuthUiState(
            isLoading = false,
            currentScreen = AuthScreen.PIN_SETUP,
        )
        var pinPadInteraction = false

        composeRule.setContent {
            MaxinesWorldTheme {
                PinSetupContent(
                    state = state,
                    onUpdateName = {},
                    onPinDigit = {},
                    onPinDelete = {},
                    onSetupPin = {},
                    onPinPadInteraction = { pinPadInteraction = true },
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.onNodeWithContentDescription("Digit 1").performClick()
        composeRule.waitForIdle()

        assertTrue(pinPadInteraction)
    }
}
