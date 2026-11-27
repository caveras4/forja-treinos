package com.fitforge.treinos.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitforge.treinos.data.backup.BackupManager
import com.fitforge.treinos.data.backup.BackupResult
import com.fitforge.treinos.data.files.TextFiles
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.repository.SessionRepository
import com.fitforge.treinos.data.repository.SettingsRepository
import com.fitforge.treinos.data.repository.WorkoutRepository
import com.fitforge.treinos.domain.ActiveSession
import com.fitforge.treinos.domain.StartDecision
import com.fitforge.treinos.domain.decideStart
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
    val startConflict: StartConflict? = null,
    val showImportBackupConfirm: Boolean = false,
    val showAbout: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val pendingExport: WorkoutEntity? = null
)

class HomeViewModel(
    private val repository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val backupManager: BackupManager,
    private val textFiles: TextFiles,
    ready: StateFlow<Boolean>
) : ViewModel() {

    private val local = MutableStateFlow(HomeLocalState())

    private val data = combine(
        repository.observeSummaries(),
        settingsRepository.settings,
        sessionRepository.active,
        ready
    ) { workouts, settings, active, isReady ->
        val activeSummary = active?.let { s -> workouts.firstOrNull { it.workout.id == s.workoutId } }
        // Sessão de um treino que não existe mais (excluído ou backup restaurado): encerra.
        if (active != null && activeSummary == null && isReady) {
            viewModelScope.launch { sessionRepository.finishIf(active.workoutId) }
        }
        HomeUiState(
            workouts = workouts,
            isLoading = !isReady,
            selectedPalette = settings.palette,
            themeMode = settings.themeMode,
            activeSession = activeSummary?.let {
                ActiveSessionInfo(
                    workoutId = it.workout.id,
                    workoutName = it.workout.name,
                    doneCount = minOf(active.doneExerciseIds.size, it.exerciseCount),
                    total = it.exerciseCount
                )
            }
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(data, local) { base, local ->
        base.copy(
            startConflict = local.startConflict,
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

    // ---------- Treino em andamento ----------

    /**
     * Botão ▶ de um treino. Sem treino em andamento: inicia e chama [onReady]. Se for o próprio
     * treino em andamento: só chama [onReady]. Se outro estiver em andamento: a Home pergunta.
     */
    fun startWorkout(workout: WorkoutEntity, onReady: () -> Unit) {
        val active = uiState.value.activeSession
        when (decideStart(active?.let { ActiveSession(it.workoutId) }, workout.id)) {
            StartDecision.StartNew -> viewModelScope.launch {
                sessionRepository.startIfIdle(workout.id)
                onReady()
            }
            StartDecision.Resume -> onReady()
            is StartDecision.Conflict ->
                local.update { it.copy(startConflict = StartConflict(workout, active!!)) }
        }
    }

    fun dismissConflict() = local.update { it.copy(startConflict = null) }

    /** "Encerrar e iniciar": troca o treino em andamento pelo novo. */
    fun replaceSession(onReady: (Long) -> Unit) {
        val conflict = local.value.startConflict ?: return
        local.update { it.copy(startConflict = null) }
        viewModelScope.launch {
            sessionRepository.start(conflict.target.id)
            onReady(conflict.target.id)
        }
    }

    // ---------- Excluir ----------

    fun requestDelete(workout: WorkoutEntity) = local.update { it.copy(deleteCandidate = workout) }

    fun dismissDelete() = local.update { it.copy(deleteCandidate = null) }

    fun confirmDelete() {
        val target = local.value.deleteCandidate ?: return
        local.update { it.copy(deleteCandidate = null) }
        viewModelScope.launch {
            sessionRepository.finishIf(target.id)
            repository.deleteWorkout(target)
        }
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
                is BackupResult.Imported -> {
                    // Os treinos ganharam ids novos: o treino em andamento não existe mais.
                    sessionRepository.finish()
                    buildString {
                        append("Backup restaurado: ${result.workouts} treino(s) e ${result.customExercises} exercício(s) personalizado(s).")
                        if (result.skippedExercises > 0) {
                            append(" ${result.skippedExercises} exercício(s) desconhecido(s) foram ignorados.")
                        }
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
