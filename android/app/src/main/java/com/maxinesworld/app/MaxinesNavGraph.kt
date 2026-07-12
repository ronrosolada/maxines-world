package com.maxinesworld.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.maxinesworld.featureauth.ParentAuthScreen
import com.maxinesworld.featurechildhome.VillageHomeScreen
import com.maxinesworld.featurelessonplayer.LessonPlayerScreen
import com.maxinesworld.featureparent.ParentDashboardScreen

object Routes {
    const val PARENT_AUTH = "parent_auth"
    const val CHILD_HOME = "child_home/{childId}"
    const val LESSON_PLAYER = "lesson_player/{childId}/{lessonId}"
    const val PARENT_DASHBOARD = "parent_dashboard/{childId}"

    fun childHome(childId: String) = "child_home/$childId"
    fun lessonPlayer(childId: String, lessonId: String) = "lesson_player/$childId/$lessonId"
    fun parentDashboard(childId: String) = "parent_dashboard/$childId"
}

@Composable
fun MaxinesNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.PARENT_AUTH
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
                    // Map subject to its pilot lesson
                    val lessonId = when (subject) {
                        "english" -> "eng-g3-m01-l01"
                        "filipino" -> "fil-g3-m01-l01"
                        "mathematics" -> "math-g3-m01-l01"
                        "science" -> "sci-g3-m01-l01"
                        "philippine-history" -> "hist-g3-m01-l01"
                        else -> "eng-g3-m01-l01"
                    }
                    navController.navigate(Routes.lessonPlayer(childId, lessonId))
                },
                onParentGate = {
                    navController.navigate(Routes.parentDashboard(childId))
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
                onBack = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(Routes.childHome(childId)) {
                        popUpTo(Routes.CHILD_HOME) { inclusive = true }
                    }
                }
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
    }
}
