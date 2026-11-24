package com.fitforge.treinos.ui.home

import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode

data class HomeUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPalette: AppPalette = AppPalette.EMERALD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val editorWorkout: WorkoutEntity? = null,
    val isEditorOpen: Boolean = false,
    val deleteCandidate: WorkoutEntity? = null
)
