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
import com.maxinesworld.featurechildhome.VillageHomeScreen
import com.maxinesworld.featurelessonplayer.LessonPlayerScreen
import com.maxinesworld.featureparent.ParentDashboardScreen
import com.maxinesworld.featureparent.ParentGateScreen
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

    fun childHome(childId: String) = "child_home/$childId"
    fun lessonPlayer(childId: String, lessonId: String) = "lesson_player/$childId/$lessonId"
    fun parentDashboard(childId: String) = "parent_dashboard/$childId"
    fun parentGate(childId: String) = "parent_gate/$childId"
}

@Composable
fun MaxinesNavGraph(navController: NavHostController) {
    // Resolve whether setup is complete to decide start destination
    var startDest by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val appContext = navController.context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            com.maxinesworld.app.di.StartupCheckEntryPoint::class.java
        )
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
            VillageHomeScreen(
                onSubjectTap = { subject ->
                    val lessonId = when (subject) {
                        "english" -> "eng-g3-m01-l01"
                        "filipino" -> "fil-g3-m01-l01"
                        "mathematics" -> "math-g3-m01-l01"
                        "science" -> "sci-g3-m01-l01"
                        "makabansa" -> "mkb-g3-m01-l01"
                        "gmrc" -> "gmrc-g3-m01-l01"
                        else -> "eng-g3-m01-l01"
                    }
                    navController.navigate(Routes.lessonPlayer(childId, lessonId))
                },
                onParentGate = {
                    navController.navigate(Routes.parentGate(childId))
                }
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
    }
}
