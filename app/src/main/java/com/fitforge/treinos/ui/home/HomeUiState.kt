package com.fitforge.treinos.ui.home

import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.local.WorkoutSummary
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode

data class HomeUiState(
    val workouts: List<WorkoutSummary> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPalette: AppPalette = AppPalette.EMERALD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val deleteCandidate: WorkoutEntity? = null,
    val showImportBackupConfirm: Boolean = false,
    val showAbout: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null
)
