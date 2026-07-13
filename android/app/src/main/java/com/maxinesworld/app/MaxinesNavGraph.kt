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
import com.maxinesworld.featurechildhome.VillageHomeUiState
import com.maxinesworld.featurechildhome.defaultVillageDestinations
import com.maxinesworld.featurechildhome.VillageDestinationUi
import com.maxinesworld.featurechildhome.DestinationAnchor
import com.maxinesworld.featurechildhome.DestinationStatus
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
            VillageHomeScreen(
                state = VillageHomeUiState(destinations = defaultVillageDestinations()),
                onDestinationClick = { subject ->
                    val lessonId = when (subject) {
                        "english" -> "english-g3-m01-d01"
                        "filipino" -> "filipino-g3-m01-d01"
                        "mathematics" -> "mathematics-g3-m01-d01"
                        "science" -> "science-g3-m01-d01"
                        "philippine-history" -> "mkb-g3-m01-l01"
                        "makabansa" -> "mkb-g3-m01-l01"
                        "gmrc" -> "gmrc-g3-m01-l01"
                        else -> "english-g3-m01-d01"
                    }
                    navController.navigate(Routes.lessonPlayer(childId, lessonId))
                },
                onQuestClick = { /* Daily Quest continue */ },
                onProfileClick = { /* Profile */ },
                onAchievementsClick = {
                    navController.navigate(Routes.wildlifeFieldGuide(childId))
                },
                onBackpackClick = { /* Backpack */ },
                onParentsClick = {
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
