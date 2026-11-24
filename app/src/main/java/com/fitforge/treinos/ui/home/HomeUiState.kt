package com.fitforge.treinos.ui.home

import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.local.WorkoutSummary
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode

/** Resumo do treino em andamento, para o destaque na Home. */
data class ActiveSessionInfo(
    val workoutId: Long,
    val workoutName: String,
    val doneCount: Int,
    val total: Int
)

/** O usuário tocou num treino enquanto outro está em andamento. */
data class StartConflict(val target: WorkoutEntity, val active: ActiveSessionInfo)

data class HomeUiState(
    val workouts: List<WorkoutSummary> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPalette: AppPalette = AppPalette.EMERALD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val activeSession: ActiveSessionInfo? = null,
    val startConflict: StartConflict? = null,
    val deleteCandidate: WorkoutEntity? = null,
    val showImportBackupConfirm: Boolean = false,
    val showAbout: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null
)
