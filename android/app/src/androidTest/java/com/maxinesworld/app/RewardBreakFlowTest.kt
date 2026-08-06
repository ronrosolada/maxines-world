package com.maxinesworld.app

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxinesworld.coredatabase.ChildProfileEntity
import com.maxinesworld.coredatabase.MaxinesDatabase
import com.maxinesworld.coredatabase.ParentAccountEntity
import com.maxinesworld.coredatabase.RewardBreakPolicy
import com.maxinesworld.coremodel.LessonManifest
import com.maxinesworld.engineminigame.MiniGameResult
import com.maxinesworld.engineactivity.ActivityResult
import com.maxinesworld.featureauth.ParentAuthManager
import com.maxinesworld.featurelessonplayer.LessonCompleteScreen
import com.maxinesworld.featurelessonplayer.LessonUiState
import com.maxinesworld.gamecatcafe.CatCafeDashScreen
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Deterministic UI smoke coverage for the lesson-completion reward-break path.
 *
 * The lesson-completion screen, reward hub, and Cat Café screen are production
 * composables. The test host only replaces the app's route state so the test
 * does not depend on authored activity ordering or animation-sensitive taps.
 */
@RunWith(AndroidJUnit4::class)
class RewardBreakFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val childId = "ui-smoke-child"
    private val parentId = "ui-smoke-parent"
    private lateinit var scenario: androidx.test.core.app.ActivityScenario<MainActivity>
    private lateinit var database: MaxinesDatabase
    private lateinit var authManager: ParentAuthManager
    private lateinit var rewardBreakId: String

    @Before
    fun setUp() = runBlocking {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.maxinesworld.app.di.StartupCheckEntryPoint::class.java,
        )
        database = entryPoint.database()
        authManager = entryPoint.authManager()

        scenario = composeRule.activityRule.scenario
        scenario.onActivity { activity ->
            // Dispose the production NavGraph before seeding Room. Otherwise its startup
            // coroutine can race the deterministic database setup below.
            activity.setContent {
                MaxinesWorldTheme {}
            }
        }
        composeRule.waitForIdle()

        withContext(Dispatchers.IO) {
            database.clearAllTables()
            authManager.clearAll()
            database.parentAccountDao().upsert(
                ParentAccountEntity(
                    id = parentId,
                    displayName = "UI Smoke Parent",
                    pinHash = "test-only",
                )
            )
            database.childProfileDao().upsert(
                ChildProfileEntity(
                    id = childId,
                    parentId = parentId,
                    name = "Maxine",
                )
            )
            authManager.setPin("1234", "UI Smoke Parent")

            val dayKey = LocalDate.now(ZoneId.systemDefault()).toString()
            val dailyQuestCompletionId = RewardBreakPolicy.dailyQuestCompletionId(childId, dayKey)
            rewardBreakId = "ui-smoke-break-${UUID.randomUUID()}"
            database.rewardBreakDao().insertIgnoring(
                RewardBreakPolicy.newEntitlement(
                    id = rewardBreakId,
                    childId = childId,
                    dailyQuestCompletionId = dailyQuestCompletionId,
                    nowEpochMillis = System.currentTimeMillis(),
                )
            )
        }

        scenario.onActivity { activity ->
            activity.setContent {
                MaxinesWorldTheme {
                    RewardBreakFlowHost(
                        childId = childId,
                        rewardBreakId = rewardBreakId,
                        completionState = completedLessonState(rewardBreakId),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() = runBlocking {
        scenario.close()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            authManager.clearAll()
        }
    }

    @Test
    fun lessonCompletionOpensRewardGameAndVillageReturnConsumesBreak() {
        // Lesson completion UI exposes the reward-break action.
        waitForText("Lesson Complete!")
        onText("Play a Reward Game").performClick()

        // The hub loads the CREATED entitlement and starts it only on game choice.
        waitForText("Great work today!")
        waitForText("Cat Café Dash")
        val created = getBreak()
        assertEquals(RewardBreakPolicy.CREATED, created?.state)
        onText("Cat Café Dash").performClick()

        waitForDescription("Leave game")
        val active = getBreak()
        assertEquals(RewardBreakPolicy.ACTIVE, active?.state)
        onDescription("Leave game").performClick()
        waitForText("Leave the café?")
        onText("Save and leave").performClick()

        // Leaving the game saves its result but does not consume the break.
        waitForText("Return to Playroom")
        assertEquals(RewardBreakPolicy.ACTIVE, getBreak()?.state)
        assertEquals(1, runBlocking(Dispatchers.IO) {
            database.miniGameResultDao().getByChild(childId).size
        })

        // The hub BackHandler invokes the same finishBreak() path as Return to Playroom.
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForConsumedBreak()
        waitForText("Hi, Maxine!")
        val consumed = getBreak()
        assertEquals(RewardBreakPolicy.CONSUMED, consumed?.state)
        assertEquals(0L, consumed?.remainingMillis)
    }

    private fun getBreak() = runBlocking(Dispatchers.IO) {
        database.rewardBreakDao().getById(rewardBreakId)
    }

    private fun onText(text: String) = composeRule.onNodeWithText(text)

    private fun onDescription(description: String) =
        composeRule.onNodeWithContentDescription(description)

    private fun waitForText(text: String) {
        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForDescription(description: String) {
        composeRule.waitUntil(20_000) {
            composeRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForConsumedBreak() {
        composeRule.waitUntil(20_000) {
            getBreak()?.state == RewardBreakPolicy.CONSUMED
        }
    }

}

private enum class RewardBreakTestRoute {
    COMPLETION,
    HUB,
    CAT_CAFE,
    VILLAGE,
}

@Composable
private fun RewardBreakFlowHost(
    childId: String,
    rewardBreakId: String,
    completionState: LessonUiState,
) {
    var route by remember { mutableStateOf(RewardBreakTestRoute.COMPLETION) }
    var gameDurationMillis by remember { mutableLongStateOf(RewardBreakPolicy.DEFAULT_DURATION_MILLIS) }
    val scope = rememberCoroutineScope()
    val rewardBreakViewModel: RewardBreakViewModel = hiltViewModel()

    when (route) {
        RewardBreakTestRoute.COMPLETION -> LessonCompleteScreen(
            state = completionState,
            onComplete = { route = RewardBreakTestRoute.VILLAGE },
            onPlayGames = { route = RewardBreakTestRoute.HUB },
        )

        RewardBreakTestRoute.HUB -> RewardHubScreen(
            childId = childId,
            rewardBreakId = rewardBreakId,
            viewModel = rewardBreakViewModel,
            onPlayCatCafe = { durationMillis ->
                gameDurationMillis = durationMillis
                route = RewardBreakTestRoute.CAT_CAFE
            },
            onPlayParkour = { durationMillis ->
                gameDurationMillis = durationMillis
                route = RewardBreakTestRoute.CAT_CAFE
            },
            onPlayKittenMatch = { durationMillis ->
                gameDurationMillis = durationMillis
                route = RewardBreakTestRoute.CAT_CAFE
            },
            onOpenSourceGames = {},
            onReturnToVillage = { route = RewardBreakTestRoute.VILLAGE },
        )

        RewardBreakTestRoute.CAT_CAFE -> CatCafeDashScreen(
            childId = childId,
            rewardBreakId = rewardBreakId,
            durationMillis = gameDurationMillis,
            onExit = { result: MiniGameResult ->
                scope.launch {
                    rewardBreakViewModel.saveResult(result)
                    route = RewardBreakTestRoute.HUB
                }
            },
        )

        RewardBreakTestRoute.VILLAGE -> {
            androidx.compose.material3.Text("Hi, Maxine!")
        }
    }
}

private fun completedLessonState(rewardBreakId: String): LessonUiState = LessonUiState(
    isLoading = false,
    lesson = LessonManifest(
        id = "ui-smoke-lesson",
        schemaVersion = 1,
        subject = "GMRC",
        moduleId = "ui-smoke-module",
        title = "Tiwala sa Sarili",
        objective = "Practice a kind learning habit.",
        guideCharacter = "Milo",
        estimatedMinutes = 5,
        languageOfInstruction = "english",
    ),
    isComplete = true,
    results = listOf(
        ActivityResult(
            activityId = "ui-smoke-complete",
            correct = true,
            attempts = 1,
            hintsUsed = 0,
            responseTimeMs = 1_000L,
        )
    ),
    rewardBreakId = rewardBreakId,
)
