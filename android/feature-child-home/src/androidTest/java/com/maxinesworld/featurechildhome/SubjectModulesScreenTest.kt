package com.maxinesworld.featurechildhome

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.maxinesworld.corecontent.ContentModule
import com.maxinesworld.corecontent.ContentModuleLesson
import org.junit.Rule
import org.junit.Test

class SubjectModulesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun firstModuleIsMarkedAsTheChildFriendlyStartingPoint() {
        val modules = listOf(
            ContentModule(
                key = "m01",
                title = "Milo's Equal-Groups Market",
                lessons = listOf(ContentModuleLesson("math-g3-m01-d01", "Market Day", 1)),
            ),
            ContentModule(
                key = "q1-w1",
                title = "Quarter 1 · Week 1",
                lessons = listOf(ContentModuleLesson("math-g3-q1-w01-d01", "Shape Trail", 1)),
            ),
        )

        composeRule.setContent {
            SubjectModulesScreen(
                subject = "mathematics",
                state = SubjectModulesState(isLoading = false, modules = modules),
                onModuleClick = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Start here, or choose another module.").assertIsDisplayed()
        composeRule.onNodeWithText("Start here!").assertIsDisplayed()
        composeRule.onAllNodesWithText("Start here!").assertCountEquals(1)
    }
}
