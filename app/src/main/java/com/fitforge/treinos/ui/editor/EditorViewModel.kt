package com.fitforge.treinos.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitforge.treinos.data.files.ReadResult
import com.fitforge.treinos.data.files.TextFiles
import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.data.media.CustomMediaStorage
import com.fitforge.treinos.data.media.MediaImportResult
import com.fitforge.treinos.data.repository.CreateExerciseResult
import com.fitforge.treinos.data.repository.ExerciseRepository
import com.fitforge.treinos.data.repository.WorkoutRepository
import com.fitforge.treinos.domain.DraftErrors
import com.fitforge.treinos.domain.MuscleGroups
import com.fitforge.treinos.domain.WorkoutDraft
import com.fitforge.treinos.domain.transfer.ImportResult
import com.fitforge.treinos.domain.transfer.TransferFormat
import com.fitforge.treinos.domain.transfer.WorkoutImportParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorStep { PICKER, FORM }

data class ImportPrompt(
    val draft: WorkoutDraft,
    val totalInFile: Int,
    val unknownIds: List<String>
)

data class CustomExerciseForm(
    val name: String = "",
    val group: String = MuscleGroups.ORDER.first(),
    val mediaFileName: String? = null,
    val mediaError: String? = null,
    val nameError: String? = null,
    val busy: Boolean = false
)

data class DeletePrompt(val exercise: ExerciseEntity, val usedInWorkouts: Int)

data class EditorUiState(
    val isEditing: Boolean = false,
    val loading: Boolean = true,
    val step: EditorStep = EditorStep.PICKER,
    val exercises: Map<String, ExerciseEntity> = emptyMap(),
    val query: String = "",
    val groupFilter: String? = null,
    val expandedId: String? = null,
    val draft: WorkoutDraft = WorkoutDraft(),
    val focusEditedByUser: Boolean = false,
    val errors: DraftErrors = DraftErrors(),
    val showDiscardDialog: Boolean = false,
    val importPrompt: ImportPrompt? = null,
    val message: String? = null,
    val customForm: CustomExerciseForm? = null,
    val deletePrompt: DeletePrompt? = null,
    val saving: Boolean = false,
    val finished: Boolean = false
) {
    val groups: List<ExerciseGroup> get() = buildGroups(exercises.values, query, groupFilter)
}

/**
 * Criação (workoutId == -1) e edição de treino. Tudo fica num rascunho em memória:
 * só [save] grava no banco.
 */
class EditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val mediaStorage: CustomMediaStorage,
    private val textFiles: TextFiles
) : ViewModel() {

    private val workoutId: Long? = savedStateHandle.get<Long>(ARG_WORKOUT_ID)?.takeIf { it >= 0 }
    private var initialDraft = WorkoutDraft()

    private val _uiState = MutableStateFlow(
        EditorUiState(
            isEditing = workoutId != null,
            step = if (workoutId != null) EditorStep.FORM else EditorStep.PICKER,
            focusEditedByUser = workoutId != null
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepository.observeAll().collect { list ->
                val map = list.associateBy { it.id }
                _uiState.update { state ->
                    state.copy(exercises = map, draft = state.draft.retainExisting(map.keys))
                }
            }
        }
        viewModelScope.launch {
            if (workoutId != null) {
                val draft = workoutRepository.loadDraft(workoutId)
                if (draft == null) {
                    _uiState.update { it.copy(finished = true) }
                    return@launch
                }
                initialDraft = draft
                _uiState.update { it.copy(draft = draft, loading = false) }
            } else {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    val isDirty: Boolean get() = _uiState.value.draft != initialDraft

    // ---------- Lista de exercícios (passo PICKER) ----------

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    fun onGroupFilter(group: String?) = _uiState.update { it.copy(groupFilter = group) }

    /** Tocar no nome expande; tocar de novo recolhe. */
    fun onToggleExpand(exerciseId: String) = _uiState.update {
        it.copy(expandedId = if (it.expandedId == exerciseId) null else exerciseId)
    }

    /** "Adicionar ao treino" / "Remover do treino". */
    fun onToggleSelect(exerciseId: String) = updateDraft { it.toggle(exerciseId) }

    /** Botão Concluir: só funciona com pelo menos um exercício selecionado. */
    fun onConclude() {
        if (_uiState.value.draft.items.isEmpty()) return
        _uiState.update { it.copy(step = EditorStep.FORM, expandedId = null, query = "") }
        refreshSuggestedFocus()
    }

    fun onAddMore() = _uiState.update { it.copy(step = EditorStep.PICKER) }

    // ---------- Formulário (passo FORM) ----------

    fun onNameChange(name: String) = _uiState.update {
        it.copy(draft = it.draft.copy(name = name), errors = it.errors.copy(name = null))
    }

    fun onFocusChange(focus: String) = _uiState.update {
        it.copy(draft = it.draft.copy(focus = focus), focusEditedByUser = true, errors = it.errors.copy(focus = null))
    }

    fun onColorChange(colorKey: String) = updateDraft { it.copy(colorKey = colorKey) }

    fun onSetsChange(exerciseId: String, sets: Int) = updateDraft { it.setSets(exerciseId, sets) }

    fun onRepsChange(exerciseId: String, reps: Int) = updateDraft { it.setReps(exerciseId, reps) }

    fun onWeightChange(exerciseId: String, text: String) {
        updateDraft { it.setWeightText(exerciseId, text) }
        _uiState.update { it.copy(errors = it.errors.copy(items = it.errors.items - exerciseId)) }
    }

    fun onRemove(exerciseId: String) = updateDraft { it.remove(exerciseId) }

    fun onMove(fromId: String, toId: String) = updateDraft { it.move(fromId, toId) }

    fun save() {
        val state = _uiState.value
        if (state.saving) return
        val errors = state.draft.validate()
        if (!errors.isEmpty) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            workoutRepository.saveDraft(state.draft, workoutId)
            initialDraft = state.draft
            _uiState.update { it.copy(saving = false, finished = true) }
        }
    }

    // ---------- Voltar / descartar ----------

    fun onBack() {
        val state = _uiState.value
        when {
            state.step == EditorStep.PICKER && state.isEditing -> _uiState.update { it.copy(step = EditorStep.FORM) }
            state.step == EditorStep.FORM && !state.isEditing -> _uiState.update { it.copy(step = EditorStep.PICKER) }
            state.isEditing && !isDirty -> _uiState.update { it.copy(finished = true) }
            !state.isEditing && state.draft.items.isEmpty() -> _uiState.update { it.copy(finished = true) }
            else -> _uiState.update { it.copy(showDiscardDialog = true) }
        }
    }

    fun onConfirmDiscard() = _uiState.update { it.copy(showDiscardDialog = false, finished = true) }

    fun onDismissDiscard() = _uiState.update { it.copy(showDiscardDialog = false) }

    // ---------- Importar JSON ----------

    fun onImportFile(uri: Uri) {
        viewModelScope.launch {
            val text = when (val read = textFiles.read(uri, TransferFormat.WORKOUT_MAX_BYTES)) {
                is ReadResult.Ok -> read
                is ReadResult.TooLarge -> {
                    showMessage("Arquivo muito grande. O limite é 256 KB.")
                    return@launch
                }
                ReadResult.Failed -> {
                    showMessage("Não foi possível ler o arquivo.")
                    return@launch
                }
            }
            val result = WorkoutImportParser.parse(
                text = text.text,
                byteSize = text.size,
                knownIds = _uiState.value.exercises.keys,
                existingNames = workoutRepository.existingNames()
            )
            when (result) {
                is ImportResult.Error -> showMessage(result.message)
                is ImportResult.Parsed ->
                    if (result.unknownIds.isEmpty()) applyImport(result.draft)
                    else _uiState.update {
                        it.copy(importPrompt = ImportPrompt(result.draft, result.totalInFile, result.unknownIds))
                    }
            }
        }
    }

    fun onConfirmImport() {
        val prompt = _uiState.value.importPrompt ?: return
        _uiState.update { it.copy(importPrompt = null) }
        applyImport(prompt.draft)
    }

    fun onDismissImport() = _uiState.update { it.copy(importPrompt = null) }

    private fun applyImport(draft: WorkoutDraft) {
        _uiState.update {
            it.copy(
                draft = draft,
                focusEditedByUser = draft.focus.isNotBlank(),
                step = EditorStep.FORM,
                errors = DraftErrors()
            )
        }
        refreshSuggestedFocus()
    }

    // ---------- Exercício personalizado ----------

    fun onOpenCustomForm() = _uiState.update {
        it.copy(customForm = CustomExerciseForm(group = it.groupFilter ?: MuscleGroups.ORDER.first(), name = it.query.trim()))
    }

    fun onCustomName(name: String) = updateCustom { it.copy(name = name, nameError = null) }

    fun onCustomGroup(group: String) = updateCustom { it.copy(group = group) }

    fun onCustomMedia(uri: Uri) {
        updateCustom { it.copy(busy = true, mediaError = null) }
        viewModelScope.launch {
            when (val result = mediaStorage.import(uri)) {
                is MediaImportResult.Ok -> {
                    val previous = _uiState.value.customForm?.mediaFileName
                    mediaStorage.delete(previous)
                    updateCustom { it.copy(mediaFileName = result.fileName, busy = false) }
                }
                is MediaImportResult.Rejected -> updateCustom { it.copy(mediaError = result.message, busy = false) }
            }
        }
    }

    fun onCustomRemoveMedia() {
        mediaStorage.delete(_uiState.value.customForm?.mediaFileName)
        updateCustom { it.copy(mediaFileName = null, mediaError = null) }
    }

    fun onCustomSave() {
        val form = _uiState.value.customForm ?: return
        if (form.busy) return
        updateCustom { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = exerciseRepository.createCustom(form.name, form.group, form.mediaFileName)) {
                is CreateExerciseResult.Ok -> _uiState.update {
                    it.copy(
                        customForm = null,
                        exercises = it.exercises + (result.exercise.id to result.exercise),
                        draft = it.draft.toggle(result.exercise.id),
                        expandedId = result.exercise.id,
                        // Mostra só o exercício novo (verde e expandido), para o usuário ver que deu certo.
                        query = result.exercise.name,
                        groupFilter = null
                    )
                }
                is CreateExerciseResult.Invalid -> updateCustom { it.copy(nameError = result.message, busy = false) }
            }
        }
    }

    /** Fechar sem salvar apaga a imagem que já tinha sido copiada. */
    fun onCustomDismiss() {
        mediaStorage.delete(_uiState.value.customForm?.mediaFileName)
        _uiState.update { it.copy(customForm = null) }
    }

    fun onRequestDeleteCustom(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val usage = exerciseRepository.countWorkoutsUsing(exercise.id)
            _uiState.update { it.copy(deletePrompt = DeletePrompt(exercise, usage)) }
        }
    }

    fun onConfirmDeleteCustom() {
        val prompt = _uiState.value.deletePrompt ?: return
        _uiState.update { it.copy(deletePrompt = null, expandedId = null) }
        viewModelScope.launch {
            exerciseRepository.deleteCustom(prompt.exercise)
            updateDraft { it.remove(prompt.exercise.id) }
        }
    }

    fun onDismissDeleteCustom() = _uiState.update { it.copy(deletePrompt = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    override fun onCleared() {
        // Imagem escolhida num formulário que nunca foi salvo.
        mediaStorage.delete(_uiState.value.customForm?.mediaFileName)
    }

    // ---------- Internos ----------

    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }

    private fun updateCustom(transform: (CustomExerciseForm) -> CustomExerciseForm) =
        _uiState.update { state -> state.customForm?.let { state.copy(customForm = transform(it)) } ?: state }

    private fun updateDraft(transform: (WorkoutDraft) -> WorkoutDraft) {
        _uiState.update {
            val draft = transform(it.draft)
            it.copy(
                draft = draft,
                errors = it.errors.copy(exercises = if (draft.items.isEmpty()) it.errors.exercises else null)
            )
        }
        refreshSuggestedFocus()
    }

    /** Enquanto o usuário não digitar o foco, ele acompanha os grupos dos exercícios. */
    private fun refreshSuggestedFocus() {
        _uiState.update { state ->
            if (state.focusEditedByUser) return@update state
            val groups = state.draft.items.mapNotNull { state.exercises[it.exerciseId]?.muscleGroup }
            state.copy(draft = state.draft.copy(focus = MuscleGroups.suggestFocus(groups)))
        }
    }

    companion object {
        const val ARG_WORKOUT_ID = "workoutId"
    }
}
