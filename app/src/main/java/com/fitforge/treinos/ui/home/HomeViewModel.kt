package com.fitforge.treinos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.repository.WorkoutRepository
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class UiPrefsState(
    val selectedPalette: AppPalette = AppPalette.EMERALD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val editorWorkout: WorkoutEntity? = null,
    val isEditorOpen: Boolean = false,
    val deleteCandidate: WorkoutEntity? = null
)

class HomeViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val prefsState = MutableStateFlow(UiPrefsState())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeWorkouts(),
        prefsState
    ) { workouts, prefs ->
        HomeUiState(
            workouts = workouts,
            isLoading = false,
            selectedPalette = prefs.selectedPalette,
            themeMode = prefs.themeMode,
            useDynamicColor = prefs.useDynamicColor,
            editorWorkout = prefs.editorWorkout,
            isEditorOpen = prefs.isEditorOpen,
            deleteCandidate = prefs.deleteCandidate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun openCreateDialog() {
        prefsState.update { it.copy(editorWorkout = null, isEditorOpen = true) }
    }

    fun openEditDialog(workout: WorkoutEntity) {
        prefsState.update { it.copy(editorWorkout = workout, isEditorOpen = true) }
    }

    fun closeEditorDialog() {
        prefsState.update { it.copy(editorWorkout = null, isEditorOpen = false) }
    }

    fun saveWorkout(name: String, focus: String, colorKey: String) {
        val trimmedName = name.trim()
        if (trimmedName.length < 2) return

        val editing = prefsState.value.editorWorkout
        viewModelScope.launch {
            if (editing == null) {
                repository.createWorkout(trimmedName, focus, colorKey)
            } else {
                repository.updateWorkout(
                    editing.copy(
                        name = trimmedName,
                        focus = focus.trim(),
                        colorKey = colorKey
                    )
                )
            }
            closeEditorDialog()
        }
    }

    fun requestDelete(workout: WorkoutEntity) {
        prefsState.update { it.copy(deleteCandidate = workout) }
    }

    fun dismissDelete() {
        prefsState.update { it.copy(deleteCandidate = null) }
    }

    fun confirmDelete() {
        val target = prefsState.value.deleteCandidate ?: return
        viewModelScope.launch {
            repository.deleteWorkout(target)
            prefsState.update { it.copy(deleteCandidate = null) }
        }
    }

    fun selectPalette(palette: AppPalette) {
        prefsState.update { it.copy(selectedPalette = palette, useDynamicColor = false) }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefsState.update { it.copy(themeMode = mode) }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        prefsState.update { it.copy(useDynamicColor = enabled) }
    }

    class Factory(
        private val repository: WorkoutRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
