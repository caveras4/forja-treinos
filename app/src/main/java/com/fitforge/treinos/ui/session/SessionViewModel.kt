package com.fitforge.treinos.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.local.WorkoutExerciseDetail
import com.fitforge.treinos.data.repository.SessionRepository
import com.fitforge.treinos.data.repository.WorkoutRepository
import com.fitforge.treinos.domain.StartDecision
import com.fitforge.treinos.domain.WeightFormat
import com.fitforge.treinos.domain.decideStart
import com.fitforge.treinos.domain.WeightInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tocou em Iniciar com outro treino em andamento. */
data class SessionConflict(val workoutId: Long, val workoutName: String, val doneCount: Int, val total: Int)

data class WeightDialogState(val itemId: Long, val exerciseName: String, val text: String, val error: String? = null)

data class SessionUiState(
    val loading: Boolean = true,
    val workout: WorkoutEntity? = null,
    val items: List<WorkoutExerciseDetail> = emptyList(),
    /** Ids dos exercícios (ex.: "leg-press") marcados como feitos. */
    val doneExerciseIds: Set<String> = emptySet(),
    val expandedId: Long? = null,
    val weightDialog: WeightDialogState? = null,
    val showFinishDialog: Boolean = false,
    val finished: Boolean = false,
    /** Este é o treino em andamento. Antes de tocar em Iniciar, a tela só mostra o treino. */
    val started: Boolean = false,
    val conflict: SessionConflict? = null,
    /** Pedido para abrir outro treino (o que está em andamento). */
    val openWorkoutId: Long? = null
) {
    fun isDone(detail: WorkoutExerciseDetail): Boolean = detail.exercise.id in doneExerciseIds
    val doneCount: Int get() = items.count { isDone(it) }
    val allDone: Boolean get() = items.isNotEmpty() && doneCount == items.size
}

private data class LocalState(
    val expandedId: Long? = null,
    val weightDialog: WeightDialogState? = null,
    val showFinishDialog: Boolean = false,
    val finished: Boolean = false,
    val conflict: SessionConflict? = null,
    val openWorkoutId: Long? = null
)

/**
 * Treino em andamento. O progresso (exercícios feitos) fica no [SessionRepository], então
 * voltar para a Home ou fechar o app não perde nada. A carga alterada substitui a anterior.
 */
class SessionViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val workoutId: Long = checkNotNull(savedStateHandle[ARG_WORKOUT_ID])
    private val local = MutableStateFlow(LocalState())

    init {
        // Abrir a tela NÃO inicia o treino. Se ele já estiver em andamento, limpa marcações
        // de exercícios que saíram do treino desde a última vez.
        viewModelScope.launch {
            if (sessionRepository.current()?.workoutId == workoutId) retainCurrentExercises()
        }
    }

    private suspend fun retainCurrentExercises() {
        val ids = repository.observeExercises(workoutId).first().map { it.exercise.id }
        sessionRepository.retain(workoutId, ids)
    }

    val uiState: StateFlow<SessionUiState> = combine(
        repository.observeWorkout(workoutId),
        repository.observeExercises(workoutId),
        sessionRepository.active,
        local
    ) { workout, items, active, local ->
        val mine = active?.takeIf { it.workoutId == workoutId }
        SessionUiState(
            loading = false,
            workout = workout,
            items = items,
            doneExerciseIds = mine?.doneExerciseIds.orEmpty(),
            expandedId = local.expandedId,
            weightDialog = local.weightDialog,
            showFinishDialog = local.showFinishDialog,
            finished = local.finished,
            started = mine != null,
            conflict = local.conflict,
            openWorkoutId = local.openWorkoutId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    fun onToggleExpand(itemId: Long) = local.update {
        it.copy(expandedId = if (it.expandedId == itemId) null else itemId)
    }

    /** Botão Iniciar. Se outro treino estiver em andamento, pergunta antes de trocar. */
    fun onStartClick() {
        viewModelScope.launch {
            val active = sessionRepository.current()
            when (decideStart(active, workoutId)) {
                StartDecision.StartNew -> sessionRepository.startIfIdle(workoutId)
                StartDecision.Resume -> Unit
                is StartDecision.Conflict -> {
                    val other = repository.observeWorkout(active!!.workoutId).first()
                    if (other == null) {
                        // Sessão de um treino que não existe mais: pode iniciar este direto.
                        sessionRepository.start(workoutId)
                    } else {
                        val ids = repository.observeExercises(other.id).first().map { it.exercise.id }
                        local.update {
                            it.copy(conflict = SessionConflict(other.id, other.name, active.doneCount(ids), ids.size))
                        }
                    }
                }
            }
        }
    }

    fun onDismissConflict() = local.update { it.copy(conflict = null) }

    /** "Continuar treino": abre o treino que já está em andamento. */
    fun onContinueOther() = local.update { it.copy(conflict = null, openWorkoutId = it.conflict?.workoutId) }

    /** "Encerrar e iniciar novo": troca o treino em andamento por este. */
    fun onReplaceSession() {
        local.update { it.copy(conflict = null) }
        viewModelScope.launch { sessionRepository.start(workoutId) }
    }

    fun onOpenedOther() = local.update { it.copy(openWorkoutId = null) }

    fun onToggleDone(detail: WorkoutExerciseDetail) {
        viewModelScope.launch { sessionRepository.toggleDone(workoutId, detail.exercise.id) }
    }

    fun onStepWeight(detail: WorkoutExerciseDetail, delta: Double) {
        viewModelScope.launch {
            repository.updateWeight(detail.item.id, WeightFormat.step(detail.item.weightKg, delta))
        }
    }

    fun onOpenWeightDialog(detail: WorkoutExerciseDetail) = local.update {
        it.copy(
            weightDialog = WeightDialogState(
                itemId = detail.item.id,
                exerciseName = detail.exercise.name,
                text = WeightFormat.toInput(detail.item.weightKg)
            )
        )
    }

    fun onWeightTextChange(text: String) = local.update { state ->
        state.copy(weightDialog = state.weightDialog?.copy(text = text.take(6), error = null))
    }

    fun onConfirmWeight() {
        val dialog = local.value.weightDialog ?: return
        when (val parsed = WeightFormat.parse(dialog.text)) {
            WeightInput.Invalid -> local.update {
                it.copy(weightDialog = dialog.copy(error = "Informe um valor entre 0 e 500 kg."))
            }
            else -> {
                val kg = (parsed as? WeightInput.Value)?.kg?.takeIf { it > 0 }
                local.update { it.copy(weightDialog = null) }
                viewModelScope.launch { repository.updateWeight(dialog.itemId, kg) }
            }
        }
    }

    fun onDismissWeight() = local.update { it.copy(weightDialog = null) }

    /** Desmarca tudo, mas o treino continua em andamento. */
    fun onReset() {
        local.update { it.copy(expandedId = null) }
        viewModelScope.launch { sessionRepository.clearDone(workoutId) }
    }

    /** Botão Finalizar: com tudo feito encerra direto; senão pede confirmação. */
    fun onFinishClick() {
        if (uiState.value.allDone) finish() else local.update { it.copy(showFinishDialog = true) }
    }

    fun onDismissFinish() = local.update { it.copy(showFinishDialog = false) }

    fun finish() {
        local.update { it.copy(showFinishDialog = false) }
        viewModelScope.launch {
            sessionRepository.finishIf(workoutId)
            local.update { it.copy(finished = true) }
        }
    }

    companion object {
        const val ARG_WORKOUT_ID = "workoutId"
        const val WEIGHT_STEP = 1.0
    }
}
