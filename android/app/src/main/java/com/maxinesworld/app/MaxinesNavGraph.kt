package com.maxinesworld.app

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
import com.maxinesworld.coredatabase.ParentAccountDao
import com.maxinesworld.featureauth.ParentAuthManager
import com.maxinesworld.featureauth.ParentAuthScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxinesworld.featurechildhome.PlayroomHomeScreen
import com.maxinesworld.featurechildhome.PlayroomHomeViewModel
import com.maxinesworld.featurechildhome.PlayroomHomeUiState
import com.maxinesworld.featurechildhome.QuestAction
import com.maxinesworld.featurechildhome.SubjectModulesScreen
import com.maxinesworld.featurechildhome.SubjectModulesViewModel
import com.maxinesworld.featurechildhome.ModuleLessonsScreen
import com.maxinesworld.featurechildhome.ModuleLessonsViewModel
import com.maxinesworld.featurechildhome.subjectForPack
import com.maxinesworld.featurelessonplayer.LessonPlayerScreen
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
    const val SUBJECT_MODULES = "subject_modules/{childId}/{subject}"
    const val MODULE_LESSONS = "module_lessons/{childId}/{subject}/{moduleKey}"
    const val LESSON_PLAYER = "lesson_player/{childId}/{lessonId}"
    const val VIDEO_LIBRARY = "video_library/{childId}"
    const val PARENT_DASHBOARD = "parent_dashboard/{childId}"
    const val PARENT_GATE = "parent_gate/{childId}"
    const val WILDLIFE_FIELD_GUIDE = "wildlife_field_guide/{childId}?badgeId={badgeId}"

    private fun segment(value: String): String = Uri.encode(value)

    fun childHome(childId: String) = "child_home/${segment(childId)}"
    fun treatShop(childId: String) = "treat_shop/${segment(childId)}"
    fun subjectModules(childId: String, subject: String) =
        "subject_modules/${segment(childId)}/${segment(subject)}"
    fun moduleLessons(childId: String, subject: String, moduleKey: String) =
        "module_lessons/${segment(childId)}/${segment(subject)}/${segment(moduleKey)}"
    fun lessonPlayer(childId: String, lessonId: String) =
        "lesson_player/${segment(childId)}/${segment(lessonId)}"
    fun videoLibrary(childId: String) = "video_library/${segment(childId)}"
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

        val hasPin = authManager.getPinHash() != null
        if (!hasPin) {
            startDest = Routes.PARENT_AUTH
        } else {
            val parent = parentDao.getParent()
            val children = parent?.let { childDao.getByParent(it.id) } ?: emptyList()
            startDest = if (children.isNotEmpty()) {
                Routes.childHome(children.first().id)
            } else {
                Routes.PARENT_AUTH
            }
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
            val badgeAwarder: BadgeAwarder = entryPoint.badgeAwarder()
            val homeViewModel: PlayroomHomeViewModel = hiltViewModel(backStackEntry)
            val homeState by homeViewModel.state.collectAsStateWithLifecycle()
            PlayroomHomeScreen(
                state = homeState,
                onSubjectClick = { subjectId ->
                    val subject = subjectForPack(subjectId)
                    if (subject != null && homeViewModel.onSubjectSelected(subjectId)) {
                        navController.navigate(Routes.subjectModules(childId, subject))
                        homeViewModel.onOpenFinished()
                    }
                },
                onQuestAction = { action ->
                    when (action) {
                        QuestAction.OpenLesson -> {
                            val quest = (homeState as? PlayroomHomeUiState.Content)?.quest
                            val lessonId = quest?.nextLessonId
                                ?: quest?.targets?.firstOrNull { !it.isCompleted }?.lessonId
                                ?: quest?.targets?.firstOrNull()?.lessonId
                            if (lessonId != null) {
                                navController.navigate(Routes.lessonPlayer(childId, lessonId))
                            } else {
                                val fallback = (homeState as? PlayroomHomeUiState.Content)
                                    ?.quest?.recommendedSubjectId?.let(::subjectForPack)
                                if (fallback != null) navController.navigate(Routes.subjectModules(childId, fallback))
                            }
                        }
                        QuestAction.Continue -> {
                            val target = (homeState as? PlayroomHomeUiState.Content)
                                ?.quest?.recommendedSubjectId
                            val subject = target?.let(::subjectForPack)
                            if (subject != null) {
                                navController.navigate(Routes.subjectModules(childId, subject))
                            }
                        }
                        QuestAction.ChooseSubject -> { /* focus move handled in screen */ }
                        QuestAction.ViewReward -> {
                            navController.navigate(Routes.treatShop(childId))
                        }
                    }
                },
                onHomeClick = { /* Home is the current destination — no push */ },
                onCollectionClick = {
                    navController.navigate(Routes.wildlifeFieldGuide(childId))
                },
                onTreatShopClick = {
                    navController.navigate(Routes.treatShop(childId))
                },
                onVideosClick = {
                    navController.navigate(Routes.videoLibrary(childId))
                },
                onOpenCollection = {
                    navController.navigate(Routes.wildlifeFieldGuide(childId))
                },
                onParentsClick = {
                    navController.navigate(Routes.parentGate(childId))
                },
                onRetry = homeViewModel::retry,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.VIDEO_LIBRARY,
            arguments = listOf(navArgument("childId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val viewModel: VideoLibraryViewModel = hiltViewModel(backStackEntry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            VideoLibraryScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onRetry = viewModel::refresh,
                onDownload = viewModel::download,
                onPlay = viewModel::play,
                onStopPlaying = viewModel::stopPlaying,
                onStartAssessment = viewModel::startAssessment,
                onSelectAssessmentOption = viewModel::selectAssessmentOption,
                onSubmitAssessment = viewModel::submitAssessment,
                onNextAssessment = viewModel::nextAssessment,
                onRestartAssessment = viewModel::restartAssessment,
                onCloseAssessment = viewModel::closeAssessment,
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
            route = Routes.SUBJECT_MODULES,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("subject") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val subject = backStackEntry.arguments?.getString("subject") ?: return@composable
            val viewModel: SubjectModulesViewModel = hiltViewModel(backStackEntry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            SubjectModulesScreen(
                subject = subject,
                state = state,
                onModuleClick = { moduleKey ->
                    navController.navigate(Routes.moduleLessons(childId, subject, moduleKey))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MODULE_LESSONS,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("subject") { type = NavType.StringType },
                navArgument("moduleKey") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val subject = backStackEntry.arguments?.getString("subject") ?: return@composable
            val moduleKey = backStackEntry.arguments?.getString("moduleKey") ?: return@composable
            val viewModel: ModuleLessonsViewModel = hiltViewModel(backStackEntry)
            val state by viewModel.state.collectAsStateWithLifecycle()
            ModuleLessonsScreen(
                moduleTitle = state.moduleTitle.ifEmpty { moduleKey },
                state = state,
                onLessonClick = { lessonId ->
                    navController.navigate(Routes.lessonPlayer(childId, lessonId))
                },
                onBack = { navController.popBackStack() },
                onRetry = viewModel::retry,
            )
        }

        composable(
            route = Routes.LESSON_PLAYER,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: return@composable
            LessonPlayerScreen(
                lessonId = lessonId,
                childId = childId,
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(Routes.CHILD_HOME) { inclusive = true }
                    }
                },
                onViewFieldGuide = { badgeId ->
                    navController.navigate(Routes.wildlifeFieldGuide(childId, badgeId))
                },
                onRewardBreak = { cId, breakId ->
                    navController.navigate(MiniGameRoutes.hub(cId, breakId))
                }
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
