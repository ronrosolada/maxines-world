package com.maxinesworld.app

import com.maxinesworld.featurelessonplayer.AssessmentArenaRoute
import com.maxinesworld.featurelessonplayer.AssessmentArenaViewModel

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maxinesworld.coredatabase.ChildProfileDao
import com.maxinesworld.coredatabase.GodModeManager
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.featureauth.ParentAuthManager
import com.maxinesworld.featureauth.ParentAuthScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.featurechildhome.PlayroomHomeScreen
import com.maxinesworld.featurechildhome.PlayroomHomeViewModel
import com.maxinesworld.featurechildhome.PlayroomHomeUiState
import com.maxinesworld.featurechildhome.QuestAction
import com.maxinesworld.featurechildhome.LivingSanctuaryRoute
import com.maxinesworld.featurechildhome.LivingSanctuaryViewModel
import com.maxinesworld.featurelessonplayer.QuickBitsScreen
import com.maxinesworld.featurelessonplayer.QuickBitsViewModel
import com.maxinesworld.featurelessonplayer.VideoLibraryScreen
import com.maxinesworld.featurelessonplayer.VideoLibraryViewModel
import com.maxinesworld.featureparent.ParentDashboardScreen
import com.maxinesworld.featureparent.ParentGateScreen
import com.maxinesworld.featurerewards.WildlifeFieldGuideScreen
import com.maxinesworld.featurerewards.TreatShopScreen
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.engineminigame.MiniGameResult
import com.maxinesworld.gamecatcafe.CatCafeDashScreen
import com.maxinesworld.gamekittenmatch.KittenMatchScreen
import com.maxinesworld.gamepawprintparkour.PawprintParkourScreen
import com.maxinesworld.gamepawprintparkour.ParkourResult
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

object Routes {
    const val PARENT_AUTH = "parent_auth"
    const val CHILD_HOME = "child_home/{childId}"
    const val TREAT_SHOP = "treat_shop/{childId}"
    const val VIDEO_LIBRARY = "video_library/{childId}?subject={subject}"
    const val ASSESSMENT_ARENA = "assessment_arena/{childId}?subject={subject}&packId={packId}"
    const val QUICK_BITS = "quick_bits/{childId}"
    const val PARENT_DASHBOARD = "parent_dashboard/{childId}"
    const val PARENT_GATE = "parent_gate/{childId}"
    const val WILDLIFE_FIELD_GUIDE = "wildlife_field_guide/{childId}?badgeId={badgeId}"
    const val SANCTUARY = "sanctuary/{childId}"

    private fun segment(value: String): String = Uri.encode(value)

    fun childHome(childId: String) = "child_home/${segment(childId)}"
    fun sanctuary(childId: String) = "sanctuary/${segment(childId)}"
    fun treatShop(childId: String) = "treat_shop/${segment(childId)}"
    fun assessmentArena(childId: String, subject: String? = null, packId: String? = null) =
        "assessment_arena/${segment(childId)}?subject=${segment(subject.orEmpty())}&packId=${segment(packId.orEmpty())}"

    fun videoLibrary(childId: String, subject: String? = null) =
        "video_library/${segment(childId)}?subject=${segment(subject.orEmpty())}"

    fun quickBits(childId: String) = "quick_bits/${segment(childId)}"
    fun parentDashboard(childId: String) = "parent_dashboard/${segment(childId)}"
    fun parentGate(childId: String) = "parent_gate/${segment(childId)}"
    fun wildlifeFieldGuide(childId: String, badgeId: String? = null): String =
        "wildlife_field_guide/${segment(childId)}?badgeId=${segment(badgeId.orEmpty())}"
}

@Composable
fun MaxinesNavGraph(navController: NavHostController) {
    var startDest by remember { mutableStateOf<String?>(null) }
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            navController.context.applicationContext,
            com.maxinesworld.app.di.StartupCheckEntryPoint::class.java
        )
    }

    LaunchedEffect(Unit) {
        val parentDao = entryPoint.parentAccountDao()
        val childDao = entryPoint.childProfileDao()
        val authManager = entryPoint.authManager()

        authManager.getPinHash()
        val parent = parentDao.getParent()
        val children = parent?.let { childDao.getByParent(it.id) } ?: emptyList()
        startDest = if (children.isNotEmpty()) {
            Routes.childHome(children.first().id)
        } else {
            Routes.PARENT_AUTH
        }
    }

    if (startDest == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDest!!
    ) {
        composable(Routes.PARENT_AUTH) {
            ParentAuthScreen(
                onChildSelected = { childId ->
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(Routes.PARENT_AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CHILD_HOME,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val homeViewModel: PlayroomHomeViewModel = hiltViewModel(backStackEntry)
            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
            PlayroomHomeScreen(
                state = homeState,
                onSubjectClick = { subjectId ->
                    if (homeViewModel.onSubjectSelected(subjectId)) {
                        navController.navigate(Routes.videoLibrary(childId, subjectId))
                        homeViewModel.onOpenFinished()
                    }
                },
                onQuestAction = { action ->
                    when (action) {
                        QuestAction.OpenVideoQuest -> {
                            val quest = (homeState as? PlayroomHomeUiState.Content)?.quest
                            val target = quest?.nextTargetId?.let { nextTargetId ->
                                quest.targets.firstOrNull { it.mediaId == nextTargetId }
                            }
                            if (target != null) {
                                if (target.type == com.maxinesworld.featurechildhome.QuestTargetType.ARENA) {
                                    navController.navigate(
                                        Routes.assessmentArena(childId, target.subjectId, target.arenaPackId),
                                    )
                                } else {
                                    navController.navigate(Routes.videoLibrary(childId, target.subjectId))
                                }
                            }
                        }
                        QuestAction.RetryMission -> homeViewModel.retry()
                        QuestAction.Continue -> {
                            navController.navigate(Routes.videoLibrary(childId))
                        }
                        QuestAction.ChooseSubject -> { /* focus move handled in screen */ }
                        QuestAction.ViewReward -> {
                            // The completed-quest reward is the 5-minute play break
                            // minted by DailyQuestRewardWriter under a deterministic
                            // id. Open THAT experience (the honest fulfillment of the
                            // "break + sanctuary piece" promise) rather than the token
                            // shop the old handler pointed to.
                            val dayKey = java.time.LocalDate.now().toString()
                            navController.navigate(
                                MiniGameRoutes.hub(childId, "reward-break:$childId:$dayKey")
                            )
                        }
                        QuestAction.OpenPlayground -> {
                            navController.navigate(
                                MiniGameRoutes.hub(childId, GodModeManager.GOD_MODE_REWARD_BREAK_ID)
                            )
                        }
                    }
                },
                onQuestTargetClick = { target ->
                    if (target.type == com.maxinesworld.featurechildhome.QuestTargetType.ARENA) {
                        navController.navigate(
                            Routes.assessmentArena(childId, target.subjectId, target.arenaPackId),
                        )
                    } else {
                        navController.navigate(Routes.videoLibrary(childId, target.subjectId))
                    }
                },
                onHomeClick = { /* Home is the current destination — no push */ },
                onCollectionClick = {
                    navController.navigate(Routes.wildlifeFieldGuide(childId))
                },
                onTreatShopClick = {
                    navController.navigate(Routes.treatShop(childId))
                },
                onQuickBitsClick = {
                    navController.navigate(Routes.quickBits(childId))
                },
                onVideosClick = {
                    navController.navigate(Routes.videoLibrary(childId))
                },
                onAssessmentsClick = {
                    navController.navigate(Routes.assessmentArena(childId))
                },
                onOpenCollection = {
                    navController.navigate(Routes.wildlifeFieldGuide(childId))
                },
                onVisitSanctuary = { navController.navigate(Routes.sanctuary(childId)) },
                onParentsClick = {
                    navController.navigate(Routes.parentGate(childId))
                },
                onRetry = homeViewModel::retry,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SANCTUARY,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val viewModel: LivingSanctuaryViewModel = hiltViewModel(backStackEntry)
            LivingSanctuaryRoute(
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }

                composable(
            route = Routes.ASSESSMENT_ARENA,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("subject") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("packId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            )
        ) { backStackEntry ->
            val arenaPackId = backStackEntry.arguments?.getString("packId")?.takeIf(String::isNotBlank)
            val arenaViewModel: AssessmentArenaViewModel = hiltViewModel(backStackEntry)
            LaunchedEffect(arenaPackId) {
                arenaPackId?.let(arenaViewModel::startQuiz)
            }
            AssessmentArenaRoute(
                onBack = { navController.popBackStack() },
                viewModel = arenaViewModel,
            )
        }

        composable(
            route = Routes.QUICK_BITS,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val viewModel: QuickBitsViewModel = hiltViewModel(backStackEntry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            QuickBitsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.loadCatalog(true) },
                onSelectCategory = viewModel::selectCategory,
                onPlayVideo = viewModel::playVideo,
                onStopPlaying = viewModel::stopPlaying,
                onDownloadSingle = viewModel::downloadSingle,
                onDownloadAll = viewModel::downloadAll,
                onClearDownloads = viewModel::clearAllDownloads,
            )
        }

        composable(
            route = Routes.VIDEO_LIBRARY,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("subject") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) {
            VideoLibraryScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.TREAT_SHOP,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            TreatShopScreen(
                childId = childId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PARENT_GATE,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            ParentGateScreen(
                onAuthenticated = {
                    navController.navigate(Routes.parentDashboard(childId)) {
                        popUpTo(Routes.PARENT_GATE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PARENT_DASHBOARD,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            ParentDashboardScreen(
                childId = childId,
                onBack = { navController.popBackStack() }
            )
        }

        // ─── Mini-Game Routes ───

        composable(
            route = MiniGameRoutes.REWARD_HUB,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            RewardHubScreen(
                childId = childId,
                rewardBreakId = breakId,
                onPlayCatCafe = { durationMillis ->
                    navController.navigate(MiniGameRoutes.catCafe(childId, breakId, durationMillis))
                },
                onPlayParkour = { durationMillis ->
                    navController.navigate(MiniGameRoutes.parkour(childId, breakId, durationMillis))
                },
                onPlayKittenMatch = { durationMillis ->
                    navController.navigate(MiniGameRoutes.kittenMatch(childId, breakId, durationMillis))
                },
                onPlaySourceGame = { gameSlug, durationMillis ->
                    navController.navigate(MiniGameRoutes.sourceWebGame(childId, breakId, durationMillis, gameSlug))
                },
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = MiniGameRoutes.SOURCE_LIBRARY,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            MiniGameLibraryScreen(
                childId = childId,
                rewardBreakId = breakId,
                onPlay = { gameSlug, durationMillis ->
                    navController.navigate(MiniGameRoutes.sourceWebGame(childId, breakId, durationMillis, gameSlug))
                },
                onPlayCatCafe = { durationMillis ->
                    navController.navigate(MiniGameRoutes.catCafe(childId, breakId, durationMillis))
                },
                onPlayParkour = { durationMillis ->
                    navController.navigate(MiniGameRoutes.parkour(childId, breakId, durationMillis))
                },
                onPlayKittenMatch = { durationMillis ->
                    navController.navigate(MiniGameRoutes.kittenMatch(childId, breakId, durationMillis))
                },
                onBack = { navController.popBackStack() },
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = MiniGameRoutes.SOURCE_WEB_GAME,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType },
                navArgument("durationMillis") { type = NavType.LongType },
                navArgument("gameSlug") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            val routeDuration = backStackEntry.arguments?.getLong("durationMillis") ?: return@composable
            val gameSlug = backStackEntry.arguments?.getString("gameSlug") ?: return@composable
            val sessionViewModel: RewardBreakViewModel = hiltViewModel(backStackEntry)
            val scope = rememberCoroutineScope()
            RewardBreakRouteGuard(
                childId = childId,
                rewardBreakId = breakId,
                viewModel = sessionViewModel,
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            ) { remainingMillis ->
                MiniGameWebScreen(
                    childId = childId,
                    rewardBreakId = breakId,
                    gameSlug = gameSlug,
                    durationMillis = minOf(routeDuration, remainingMillis),
                    onExit = { result ->
                        scope.launch {
                            sessionViewModel.saveResult(result)
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.childHome(childId)) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = MiniGameRoutes.CAT_CAFE,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType },
                navArgument("durationMillis") { type = NavType.LongType },
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            val routeDuration = backStackEntry.arguments?.getLong("durationMillis") ?: return@composable
            val sessionViewModel: RewardBreakViewModel = hiltViewModel(backStackEntry)
            val scope = rememberCoroutineScope()
            RewardBreakRouteGuard(
                childId = childId,
                rewardBreakId = breakId,
                viewModel = sessionViewModel,
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) { popUpTo(0) { inclusive = true } }
                },
            ) { remainingMillis ->
                CatCafeDashScreen(
                    childId = childId,
                    rewardBreakId = breakId,
                    durationMillis = minOf(routeDuration, remainingMillis),
                    onExit = { result: MiniGameResult ->
                        scope.launch {
                            sessionViewModel.saveResult(result)
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.childHome(childId)) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
        }

        composable(
            route = MiniGameRoutes.PARKOUR,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType },
                navArgument("durationMillis") { type = NavType.LongType },
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            val routeDuration = backStackEntry.arguments?.getLong("durationMillis") ?: return@composable
            val sessionViewModel: RewardBreakViewModel = hiltViewModel(backStackEntry)
            val scope = rememberCoroutineScope()
            RewardBreakRouteGuard(
                childId = childId,
                rewardBreakId = breakId,
                viewModel = sessionViewModel,
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) { popUpTo(0) { inclusive = true } }
                },
            ) { remainingMillis ->
                PawprintParkourScreen(
                    childId = childId,
                    rewardBreakId = breakId,
                    durationMillis = minOf(routeDuration, remainingMillis),
                    onExit = { result: ParkourResult ->
                        scope.launch {
                            sessionViewModel.saveResult(
                                MiniGameResult(
                                    rewardBreakId = result.rewardBreakId,
                                    gameId = "pawprint-parkour",
                                    childId = result.childId,
                                    startedAtEpochMillis = result.startedAtEpochMillis,
                                    endedAtEpochMillis = result.endedAtEpochMillis,
                                    roundsCompleted = result.roundsCompleted,
                                    correctOrders = result.tokensCollected,
                                    pawTokensEarned = result.pawTokensEarned,
                                    collectibleId = result.collectibleId,
                                )
                            )
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.childHome(childId)) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
        }

        composable(
            route = MiniGameRoutes.KITTEN_MATCH,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType },
                navArgument("durationMillis") { type = NavType.LongType },
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            val routeDuration = backStackEntry.arguments?.getLong("durationMillis") ?: return@composable
            val sessionViewModel: RewardBreakViewModel = hiltViewModel(backStackEntry)
            val scope = rememberCoroutineScope()
            RewardBreakRouteGuard(
                childId = childId,
                rewardBreakId = breakId,
                viewModel = sessionViewModel,
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) { popUpTo(0) { inclusive = true } }
                },
            ) { remainingMillis ->
                KittenMatchScreen(
                    childId = childId,
                    rewardBreakId = breakId,
                    durationMillis = minOf(routeDuration, remainingMillis),
                    onExit = { result: MiniGameResult ->
                        scope.launch {
                            sessionViewModel.saveResult(result)
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.childHome(childId)) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }
        }

        composable(
            route = Routes.WILDLIFE_FIELD_GUIDE,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("badgeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val badgeId = backStackEntry.arguments?.getString("badgeId")
            val badgeAwarder: BadgeAwarder = entryPoint.badgeAwarder()
            WildlifeFieldGuideScreen(
                childId = childId,
                badgeAwarder = badgeAwarder,
                initialBadgeId = badgeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Map a Playroom island/subject ID to a lesson ID.
 *
 * Returns null for unknown subjects — callers MUST handle null explicitly
 * (show an error state / refuse navigation). There is deliberately NO silent
 * fallback to English: an unknown subject must never open an unrelated lesson.
 *
 * GMRC (Kindness island) now maps to REAL GMRC content converted from the
 * DepEd Matatag SLM source (tools/convert_slm_to_pack.py) — previously it
 * routed to an Araling Panlipunan lesson because no playable GMRC content
 * existed. The heritage island keeps the legacy AP lesson.
 */
internal fun lessonIdForSubject(subject: String): String? = when (subject) {
    "english" -> "english-g3-m01-d01"
    "filipino" -> "filipino-g3-m01-d01"
    "mathematics" -> "mathematics-g3-m01-d01"
    "science" -> "science-g3-m01-d01"
    "araling-panlipunan", "philippine-history", "heritage-harbor" ->
        "araling-panlipunan-g3-m01-d01"
    "makabansa" -> "makabansa-g3-q1-w01-d01"
    "gmrc" -> "gmrc-g3-q1-w01-d01"
    else -> null
}
