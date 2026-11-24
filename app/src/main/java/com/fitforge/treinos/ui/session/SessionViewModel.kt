package com.fitforge.treinos.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.local.WorkoutExerciseDetail
import com.fitforge.treinos.data.repository.WorkoutRepository
import com.fitforge.treinos.domain.WeightFormat
import com.fitforge.treinos.domain.WeightInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeightDialogState(val itemId: Long, val exerciseName: String, val text: String, val error: String? = null)

data class SessionUiState(
    val loading: Boolean = true,
    val workout: WorkoutEntity? = null,
    val items: List<WorkoutExerciseDetail> = emptyList(),
    val doneIds: Set<Long> = emptySet(),
    val expandedId: Long? = null,
    val weightDialog: WeightDialogState? = null
) {
    val doneCount: Int get() = items.count { it.item.id in doneIds }
    val allDone: Boolean get() = items.isNotEmpty() && doneCount == items.size
}

private data class LocalState(
    val doneIds: Set<Long> = emptySet(),
    val expandedId: Long? = null,
    val weightDialog: WeightDialogState? = null
)

/**
 * Treino em andamento. "Feito" vale só para esta sessão (sem histórico);
 * a carga alterada substitui a anterior e é salva na hora.
 */
class SessionViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: WorkoutRepository
) : ViewModel() {

    private val workoutId: Long = checkNotNull(savedStateHandle[ARG_WORKOUT_ID])

    // Os itens marcados sobrevivem a rotação de tela e ao processo ser recriado.
    private val local = MutableStateFlow(
        LocalState(doneIds = savedStateHandle.get<LongArray>(KEY_DONE)?.toSet().orEmpty())
    )

    val uiState: StateFlow<SessionUiState> = combine(
        repository.observeWorkout(workoutId),
        repository.observeExercises(workoutId),
        local
    ) { workout, items, local ->
        SessionUiState(
            loading = false,
            workout = workout,
            items = items,
            doneIds = local.doneIds,
            expandedId = local.expandedId,
            weightDialog = local.weightDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    fun onToggleExpand(itemId: Long) = local.update {
        it.copy(expandedId = if (it.expandedId == itemId) null else itemId)
    }

    fun onToggleDone(itemId: Long) {
        local.update {
            val done = if (itemId in it.doneIds) it.doneIds - itemId else it.doneIds + itemId
            it.copy(doneIds = done)
        }
        savedStateHandle[KEY_DONE] = local.value.doneIds.toLongArray()
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

    fun onReset() {
        local.update { it.copy(doneIds = emptySet(), expandedId = null) }
        savedStateHandle[KEY_DONE] = LongArray(0)
    }

    companion object {
        const val ARG_WORKOUT_ID = "workoutId"
        private const val KEY_DONE = "done_ids"
        const val WEIGHT_STEP = 1.0
    }
}
