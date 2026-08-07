package com.maxinesworld.featureauth

import android.graphics.Insets
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthImeLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

        composeRule.onNodeWithText("Set PIN").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performClick()
        composeRule.onNodeWithContentDescription("Digit 1").performClick()
        composeRule.waitForIdle()

        assertTrue(pinPadInteraction)
    }

    @Test
    fun pinSetupIdentifiesParentName() {
        composeRule.setContent {
            MaxinesWorldTheme {
                PinSetupContent(
                    state = AuthUiState(isLoading = false, currentScreen = AuthScreen.PIN_SETUP),
                    onUpdateName = {},
                    onPinDigit = {},
                    onPinDelete = {},
                    onSetupPin = {},
                    onPinPadInteraction = {},
                )
            }
        }

        composeRule.onNodeWithText("Parent or guardian name (optional)").assertIsDisplayed()
    }

    @Test
    fun pinDotsAnnounceEnteredDigitCount() {
        composeRule.setContent {
            MaxinesWorldTheme { PinDots(length = 3) }
        }

        composeRule
            .onNodeWithContentDescription("3 of 6 digits entered")
            .assertIsDisplayed()
    }

    @Test
    fun pinSetupKeepsKeypadActionsAboveOpenIme() {
        composeRule.setContent {
            MaxinesWorldTheme {
                PinSetupContent(
                    state = AuthUiState(isLoading = false, currentScreen = AuthScreen.PIN_SETUP),
                    onUpdateName = {},
                    onPinDigit = {},
                    onPinDelete = {},
                    onSetupPin = {},
                    onPinPadInteraction = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performClick()
        val fakeImeBottom = 640
        composeRule.runOnIdle {
            val fakeImeInsets = WindowInsets.Builder()
                .setInsets(
                    WindowInsets.Type.ime(),
                    Insets.of(0, 0, 0, fakeImeBottom),
                )
                .setVisible(WindowInsets.Type.ime(), true)
                .build()
            composeRule.activity.window.decorView.dispatchApplyWindowInsets(fakeImeInsets)
        }
        composeRule.waitForIdle()
        val imeTop = composeRule.activity.window.decorView.height - fakeImeBottom

        composeRule.onNodeWithContentDescription("Digit 0").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        val setPin = composeRule.onNodeWithText("Set PIN")
        setPin.assertIsDisplayed()

        val setPinBottom = setPin.fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(
            "Set PIN bottom=$setPinBottom must be above IME top=$imeTop",
            setPinBottom <= imeTop.toFloat(),
        )
    }
}
