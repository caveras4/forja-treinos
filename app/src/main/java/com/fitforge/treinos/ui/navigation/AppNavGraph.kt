package com.fitforge.treinos.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitforge.treinos.ui.detail.WorkoutDetailPlaceholderScreen
import com.fitforge.treinos.ui.home.HomeScreen
import com.fitforge.treinos.ui.home.HomeViewModel
import com.fitforge.treinos.ui.theme.ForjaTreinosTheme

object Routes {
    const val HOME = "home"
    const val WORKOUT_DETAIL = "workout_detail/{workoutId}/{workoutName}"

    fun workoutDetail(workoutId: Long, workoutName: String): String {
        return "workout_detail/$workoutId/${Uri.encode(workoutName)}"
    }
}

@Composable
fun AppNavGraph(homeViewModel: HomeViewModel) {
    val navController = rememberNavController()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    ForjaTreinosTheme(
        palette = uiState.selectedPalette,
        themeMode = uiState.themeMode,
        useDynamicColor = uiState.useDynamicColor
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = {
                slideInHorizontally(animationSpec = tween(220)) { it / 6 } + fadeIn(tween(220))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(220)) { it / 6 } + fadeOut(tween(180))
            }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    uiState = uiState,
                    onCreateClick = homeViewModel::openCreateDialog,
                    onEditClick = homeViewModel::openEditDialog,
                    onDeleteClick = homeViewModel::requestDelete,
                    onWorkoutClick = { workout ->
                        navController.navigate(Routes.workoutDetail(workout.id, workout.name))
                    },
                    onSaveWorkout = homeViewModel::saveWorkout,
                    onDismissEditor = homeViewModel::closeEditorDialog,
                    onConfirmDelete = homeViewModel::confirmDelete,
                    onDismissDelete = homeViewModel::dismissDelete,
                    onSelectPalette = homeViewModel::selectPalette,
                    onSelectThemeMode = homeViewModel::setThemeMode
                )
            }

            composable(
                route = Routes.WORKOUT_DETAIL,
                arguments = listOf(
                    navArgument("workoutId") { type = NavType.LongType },
                    navArgument("workoutName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val workoutName = backStackEntry.arguments?.getString("workoutName") ?: "Treino"
                WorkoutDetailPlaceholderScreen(
                    workoutName = workoutName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
