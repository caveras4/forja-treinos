package com.fitforge.treinos.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitforge.treinos.AppContainer
import com.fitforge.treinos.data.files.TextFiles
import com.fitforge.treinos.ui.editor.EditorScreen
import com.fitforge.treinos.ui.editor.EditorViewModel
import com.fitforge.treinos.ui.home.HomeScreen
import com.fitforge.treinos.ui.home.HomeViewModel
import com.fitforge.treinos.ui.session.SessionScreen
import com.fitforge.treinos.ui.session.SessionViewModel

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor?workoutId={workoutId}"
    const val SESSION = "session/{workoutId}"

    fun create() = "editor?workoutId=-1"
    fun edit(workoutId: Long) = "editor?workoutId=$workoutId"
    fun session(workoutId: Long) = "session/$workoutId"
}

@Composable
fun AppNavGraph(
    container: AppContainer,
    textFiles: TextFiles,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(180)) }
    ) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        HomeViewModel(
                            repository = container.workoutRepository,
                            settingsRepository = container.settingsRepository,
                            sessionRepository = container.sessionRepository,
                            backupManager = container.backupManager,
                            textFiles = textFiles,
                            ready = container.ready
                        )
                    }
                }
            )
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = uiState,
                viewModel = homeViewModel,
                onCreateClick = { navController.navigate(Routes.create()) },
                onEditClick = { navController.navigate(Routes.edit(it.id)) },
                onStartClick = { navController.navigate(Routes.session(it.id)) }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument(EditorViewModel.ARG_WORKOUT_ID) {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) {
            val editorViewModel: EditorViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        EditorViewModel(
                            savedStateHandle = createSavedStateHandle(),
                            workoutRepository = container.workoutRepository,
                            exerciseRepository = container.exerciseRepository,
                            mediaStorage = container.mediaStorage,
                            textFiles = textFiles,
                            sessionRepository = container.sessionRepository
                        )
                    }
                }
            )
            EditorScreen(viewModel = editorViewModel, onFinished = { navController.popBackStack() })
        }

        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument(SessionViewModel.ARG_WORKOUT_ID) { type = NavType.LongType })
        ) {
            val sessionViewModel: SessionViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        SessionViewModel(createSavedStateHandle(), container.workoutRepository, container.sessionRepository)
                    }
                }
            )
            SessionScreen(viewModel = sessionViewModel, onBack = { navController.popBackStack() })
        }
    }
}
