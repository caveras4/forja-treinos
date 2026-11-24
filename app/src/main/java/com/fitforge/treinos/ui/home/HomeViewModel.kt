package com.fitforge.treinos.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitforge.treinos.data.backup.BackupManager
import com.fitforge.treinos.data.backup.BackupResult
import com.fitforge.treinos.data.files.TextFiles
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.repository.SettingsRepository
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

private data class HomeLocalState(
    val deleteCandidate: WorkoutEntity? = null,
    val showImportBackupConfirm: Boolean = false,
    val showAbout: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val pendingExport: WorkoutEntity? = null
)

class HomeViewModel(
    private val repository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val textFiles: TextFiles,
    ready: StateFlow<Boolean>
) : ViewModel() {

    private val local = MutableStateFlow(HomeLocalState())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSummaries(),
        settingsRepository.settings,
        ready,
        local
    ) { workouts, settings, isReady, local ->
        HomeUiState(
            workouts = workouts,
            isLoading = !isReady,
            selectedPalette = settings.palette,
            themeMode = settings.themeMode,
            deleteCandidate = local.deleteCandidate,
            showImportBackupConfirm = local.showImportBackupConfirm,
            showAbout = local.showAbout,
            busy = local.busy,
            message = local.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun requestDelete(workout: WorkoutEntity) = local.update { it.copy(deleteCandidate = workout) }

    fun dismissDelete() = local.update { it.copy(deleteCandidate = null) }

    fun confirmDelete() {
        val target = local.value.deleteCandidate ?: return
        local.update { it.copy(deleteCandidate = null) }
        viewModelScope.launch { repository.deleteWorkout(target) }
    }

    fun selectPalette(palette: AppPalette) {
        viewModelScope.launch { settingsRepository.setPalette(palette) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    // ---------- Compartilhar um treino (JSON) ----------

    fun startExport(workout: WorkoutEntity) = local.update { it.copy(pendingExport = workout) }

    fun exportWorkoutTo(uri: Uri?) {
        val workout = local.value.pendingExport ?: return
        local.update { it.copy(pendingExport = null) }
        if (uri == null) return
        viewModelScope.launch {
            val json = repository.exportJson(workout.id)
            val ok = json != null && textFiles.write(uri, json)
            showMessage(if (ok) "Treino \"${workout.name}\" exportado." else "Não foi possível exportar o treino.")
        }
    }

    // ---------- Backup completo (.zip) ----------

    fun exportBackupTo(uri: Uri?) {
        if (uri == null) return
        runBusy {
            when (val result = backupManager.export(uri)) {
                is BackupResult.Exported ->
                    "Backup salvo: ${result.workouts} treino(s) e ${result.customExercises} exercício(s) personalizado(s)."
                is BackupResult.Failed -> result.message
                is BackupResult.Imported -> null
            }
        }
    }

    fun requestImportBackup() = local.update { it.copy(showImportBackupConfirm = true) }

    fun dismissImportBackup() = local.update { it.copy(showImportBackupConfirm = false) }

    fun importBackupFrom(uri: Uri?) {
        local.update { it.copy(showImportBackupConfirm = false) }
        if (uri == null) return
        runBusy {
            when (val result = backupManager.import(uri)) {
                is BackupResult.Imported -> buildString {
                    append("Backup restaurado: ${result.workouts} treino(s) e ${result.customExercises} exercício(s) personalizado(s).")
                    if (result.skippedExercises > 0) {
                        append(" ${result.skippedExercises} exercício(s) desconhecido(s) foram ignorados.")
                    }
                }
                is BackupResult.Failed -> result.message
                is BackupResult.Exported -> null
            }
        }
    }

    fun showAbout(show: Boolean) = local.update { it.copy(showAbout = show) }

    fun messageShown() = local.update { it.copy(message = null) }

    private fun showMessage(message: String) = local.update { it.copy(message = message) }

    private fun runBusy(block: suspend () -> String?) {
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = block()
            local.update { it.copy(busy = false, message = message) }
        }
    }
}
