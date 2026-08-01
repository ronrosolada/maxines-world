package com.maxinesworld.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.maxinesworld.featurechildhome.PlayroomHomeState
import com.maxinesworld.featurelessonplayer.LessonPlayerScreen
import com.maxinesworld.featureparent.ParentDashboardScreen
import com.maxinesworld.featureparent.ParentGateScreen
import com.maxinesworld.featureparent.ParentContentScreen
import com.maxinesworld.featurerewards.WildlifeFieldGuideScreen
import com.maxinesworld.featurerewards.BadgeAwarder
import com.maxinesworld.gamecatcafe.CatCafeDashScreen
import com.maxinesworld.gamepawprintparkour.PawprintParkourScreen
import com.maxinesworld.gamepawprintparkour.ParkourResult
import dagger.hilt.android.EntryPointAccessors

object Routes {
    const val PARENT_AUTH = "parent_auth"
    const val CHILD_HOME = "child_home/{childId}"
    const val LESSON_PLAYER = "lesson_player/{childId}/{lessonId}"
    const val PARENT_DASHBOARD = "parent_dashboard/{childId}"
    const val PARENT_GATE = "parent_gate/{childId}"
    const val PARENT_CONTENT = "parent_content/{childId}"

    fun childHome(childId: String) = "child_home/$childId"
    fun lessonPlayer(childId: String, lessonId: String) = "lesson_player/$childId/$lessonId"
    fun parentDashboard(childId: String) = "parent_dashboard/$childId"
    fun parentGate(childId: String) = "parent_gate/$childId"
    fun parentContent(childId: String) = "parent_content/$childId"
    fun wildlifeFieldGuide(childId: String) = "wildlife_field_guide/$childId"
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

    if (startDest == null) return // Still loading

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
                onDestinationClick = { subject ->
                    val lessonId = lessonIdForSubject(subject)
                    if (lessonId != null) {
                        navController.navigate(Routes.lessonPlayer(childId, lessonId))
                    } else {
                        // Explicit failure state: unknown subject must NOT open an
                        // unrelated lesson. Surface as a visible error, never a redirect.
                        android.util.Log.w("MaxinesNavGraph", "Unsupported subject tapped: '$subject' — refusing navigation")
                    }
                },
                onQuestClick = { },
                onHomeClick = { },
                onProgressClick = { },
                onAvatarsClick = {
                    navController.navigate(Routes.wildlifeFieldGuide(childId))
                },
                onParentsClick = {
                    navController.navigate(Routes.parentGate(childId))
                },
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
                onBack = { navController.popBackStack() },
                onContentManagement = {
                    navController.navigate(Routes.parentContent(childId))
                }
            )
        }

        composable(
            route = Routes.PARENT_CONTENT,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            ParentContentScreen(
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
                onPlayCatCafe = {
                    navController.navigate(MiniGameRoutes.catCafe(childId, breakId))
                },
                onPlayParkour = {
                    navController.navigate(MiniGameRoutes.parkour(childId, breakId))
                },
                onReturnToVillage = {
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = MiniGameRoutes.CAT_CAFE,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            CatCafeDashScreen(
                childId = childId,
                rewardBreakId = breakId,
                onExit = {
                    navController.navigate(MiniGameRoutes.hub(childId, breakId)) {
                        popUpTo(MiniGameRoutes.CAT_CAFE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = MiniGameRoutes.PARKOUR,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("rewardBreakId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val breakId = backStackEntry.arguments?.getString("rewardBreakId") ?: return@composable
            PawprintParkourScreen(
                childId = childId,
                rewardBreakId = breakId,
                onExit = { _: ParkourResult ->
                    navController.navigate(MiniGameRoutes.hub(childId, breakId)) {
                        popUpTo(MiniGameRoutes.PARKOUR) { inclusive = true }
                    }
                }
            )
        }

        // Wildlife Field Guide (badge collection)
        composable(
            route = "wildlife_field_guide/{childId}",
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val badgeAwarder: BadgeAwarder = entryPoint.badgeAwarder()
            WildlifeFieldGuideScreen(
                childId = childId,
                badgeAwarder = badgeAwarder,
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
 * KNOWN GAP (documented, requires product decision): GMRC currently routes to
 * an Araling Panlipunan lesson because no GMRC lesson exists in a playable
 * Month1Lesson format. Tracked as a known issue — do NOT change without
 * first adding real GMRC content.
 */
internal fun lessonIdForSubject(subject: String): String? = when (subject) {
    "english" -> "english-g3-m01-d01"
    "filipino" -> "filipino-g3-m01-d01"
    "mathematics" -> "mathematics-g3-m01-d01"
    "science" -> "science-g3-m01-d01"
    "araling-panlipunan", "philippine-history", "makabansa", "heritage-harbor" ->
        "araling-panlipunan-g3-m01-d01"
    "gmrc" -> "araling-panlipunan-g3-m01-d01"  // KNOWN GAP: no playable GMRC content yet
    else -> null
}
