package com.fitforge.treinos.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.domain.DraftItem
import com.fitforge.treinos.domain.MediaValidator
import com.fitforge.treinos.domain.MuscleGroups
import com.fitforge.treinos.domain.WorkoutRules
import com.fitforge.treinos.ui.components.ColorPicker
import com.fitforge.treinos.ui.components.ExerciseMedia
import com.fitforge.treinos.ui.components.Stepper
import com.fitforge.treinos.ui.components.SuccessColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val JSON_MIME_TYPES = arrayOf("application/json", "text/*", "application/octet-stream")
private val IMAGE_MIME_TYPES = arrayOf("image/gif", "image/webp", "image/png", "image/jpeg")

@Composable
fun EditorScreen(viewModel: EditorViewModel, onFinished: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) {
        if (state.finished) onFinished()
    }
    BackHandler(onBack = viewModel::onBack)

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::onImportFile)
    }

    when (state.step) {
        EditorStep.PICKER -> PickerStep(
            state = state,
            onBack = viewModel::onBack,
            onImport = { importLauncher.launch(JSON_MIME_TYPES) },
            onQueryChange = viewModel::onQueryChange,
            onGroupFilter = viewModel::onGroupFilter,
            onToggleExpand = viewModel::onToggleExpand,
            onToggleSelect = viewModel::onToggleSelect,
            onDeleteCustom = viewModel::onRequestDeleteCustom,
            onCreateCustom = viewModel::onOpenCustomForm,
            onConclude = viewModel::onConclude
        )
        EditorStep.FORM -> FormStep(
            state = state,
            onBack = viewModel::onBack,
            onNameChange = viewModel::onNameChange,
            onFocusChange = viewModel::onFocusChange,
            onColorChange = viewModel::onColorChange,
            onSetsChange = viewModel::onSetsChange,
            onRepsChange = viewModel::onRepsChange,
            onWeightChange = viewModel::onWeightChange,
            onRemove = viewModel::onRemove,
            onMove = viewModel::onMove,
            onAddMore = viewModel::onAddMore,
            onSave = viewModel::save
        )
    }

    EditorDialogs(state = state, viewModel = viewModel)
}

// =====================================================================
// Passo 1: lista de exercícios por grupo muscular
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PickerStep(
    state: EditorUiState,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onQueryChange: (String) -> Unit,
    onGroupFilter: (String?) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onDeleteCustom: (ExerciseEntity) -> Unit,
    onCreateCustom: () -> Unit,
    onConclude: () -> Unit
) {
    val selectedCount = state.draft.items.size
    // Só reagrupa quando o catálogo, a busca ou o filtro mudam, não a cada toque num card.
    val groups = remember(state.exercises, state.query, state.groupFilter) { state.groups }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Adicionar exercícios" else "Novo treino") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (!state.isEditing) {
                        TextButton(onClick = onImport) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importar JSON")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onConclude,
                    enabled = selectedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                        .testTag("concluir")
                ) {
                    Text(
                        when (selectedCount) {
                            0 -> "Selecione um exercício"
                            1 -> "Concluir (1 exercício)"
                            else -> "Concluir ($selectedCount exercícios)"
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar exercício") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("busca")
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.groupFilter == null,
                        onClick = { onGroupFilter(null) },
                        label = { Text("Todos") }
                    )
                }
                items(MuscleGroups.ORDER) { group ->
                    FilterChip(
                        selected = state.groupFilter == group,
                        onClick = { onGroupFilter(if (state.groupFilter == group) null else group) },
                        label = { Text(group) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lista_exercicios"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.loading || state.exercises.isEmpty()) {
                    item { LoadingRow() }
                }
                groups.forEach { group ->
                    stickyHeader(key = "header-${group.name}") {
                        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${group.name} (${group.exercises.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(group.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            selected = state.draft.isSelected(exercise.id),
                            expanded = state.expandedId == exercise.id,
                            onToggleExpand = { onToggleExpand(exercise.id) },
                            onToggleSelect = { onToggleSelect(exercise.id) },
                            onDelete = { onDeleteCustom(exercise) }
                        )
                    }
                }
                if (!state.loading && state.exercises.isNotEmpty() && groups.isEmpty()) {
                    item {
                        Text(
                            "Nenhum exercício encontrado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                item(key = "criar-personalizado") {
                    OutlinedButton(
                        onClick = onCreateCustom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Não encontrou? Criar exercício")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    selected: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val container by animateColorAsState(
        if (selected) SuccessColors.container() else MaterialTheme.colorScheme.surface,
        label = "rowColor"
    )
    val border = if (selected) SuccessColors.accent() else MaterialTheme.colorScheme.outlineVariant
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }

    // Ao expandir, rola a lista até o card inteiro (mídia + botão) ficar visível.
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(320) // espera a animação de expandir terminar
            bringIntoView.bringIntoView()
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .testTag("exercicio-${exercise.id}")
            .semantics { if (selected) contentDescription = "${exercise.name}, adicionado ao treino" }
    ) {
        Column {
            // Tocar no nome expande/recolhe.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        focusManager.clearFocus() // fecha o teclado da busca
                        onToggleExpand()
                    }
                    .testTag("nome-${exercise.id}")
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = SuccessColors.accent(),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) SuccessColors.onContainer() else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (exercise.isCustom) {
                        Text(
                            "Personalizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExerciseMedia(exercise)
                    if (selected) {
                        OutlinedButton(onClick = onToggleSelect, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Remover do treino")
                        }
                    } else {
                        Button(onClick = onToggleSelect, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Adicionar ao treino")
                        }
                    }
                    if (exercise.isCustom) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Excluir exercício personalizado")
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Passo 2: séries, repetições, carga, nome e ordem
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormStep(
    state: EditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onFocusChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onSetsChange: (String, Int) -> Unit,
    onRepsChange: (String, Int) -> Unit,
    onWeightChange: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, String) -> Unit,
    onAddMore: () -> Unit,
    onSave: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onMove(from.key as String, to.key as String)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar treino" else "Configurar treino") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSave,
                    enabled = !state.saving && !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                        .testTag("salvar")
                ) {
                    Text(if (state.isEditing) "Salvar alterações" else "Salvar treino")
                }
            }
        }
    ) { padding ->
        if (state.loading) {
            LoadingRow()
            return@Scaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag("lista_form"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "dados") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.draft.name,
                        onValueChange = onNameChange,
                        label = { Text("Nome do treino") },
                        placeholder = { Text("Ex.: Treino D") },
                        singleLine = true,
                        isError = state.errors.name != null,
                        supportingText = state.errors.name?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("campo_nome")
                    )
                    OutlinedTextField(
                        value = state.draft.focus,
                        onValueChange = onFocusChange,
                        label = { Text("Foco muscular") },
                        singleLine = true,
                        isError = state.errors.focus != null,
                        supportingText = state.errors.focus?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Cor", style = MaterialTheme.typography.labelLarge)
                    ColorPicker(selectedKey = state.draft.colorKey, onSelect = onColorChange)
                }
            }

            item(key = "cabecalho-exercicios") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Exercícios (${state.draft.items.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onAddMore) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Adicionar")
                        }
                    }
                    if (state.draft.items.size > 1) {
                        Text(
                            "Toque e segure ⋮⋮ para mudar a ordem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.errors.exercises?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            items(state.draft.items, key = { it.exerciseId }) { item ->
                ReorderableItem(reorderState, key = item.exerciseId) { isDragging ->
                    val exercise = state.exercises[item.exerciseId]
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = if (isDragging) 8.dp else 1.dp,
                        shadowElevation = if (isDragging) 8.dp else 0.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item-${item.exerciseId}")
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            // A linha do nome é a "alça": toque longo e arraste.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .longPressDraggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                                    .testTag("alca-${item.exerciseId}")
                            ) {
                                Icon(
                                    Icons.Rounded.DragIndicator,
                                    contentDescription = "Segure para mover",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        exercise?.name ?: item.exerciseId,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        exercise?.muscleGroup.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onRemove(item.exerciseId) }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remover ${exercise?.name ?: ""}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            ItemFields(
                                item = item,
                                error = state.errors.items[item.exerciseId],
                                onSetsChange = { onSetsChange(item.exerciseId, it) },
                                onRepsChange = { onRepsChange(item.exerciseId, it) },
                                onWeightChange = { onWeightChange(item.exerciseId, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemFields(
    item: DraftItem,
    error: String?,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Stepper("Séries", item.sets, WorkoutRules.SETS_RANGE, onSetsChange)
        Stepper("Reps", item.reps, WorkoutRules.REPS_RANGE, onRepsChange)
        OutlinedTextField(
            value = item.weightText,
            onValueChange = { onWeightChange(it.take(6)) },
            label = { Text("Carga") },
            placeholder = { Text("—") },
            suffix = { Text("kg") },
            singleLine = true,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier
                .width(100.dp)
                .testTag("carga-${item.exerciseId}")
        )
    }
    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

// =====================================================================
// Diálogos
// =====================================================================

@Composable
private fun EditorDialogs(state: EditorUiState, viewModel: EditorViewModel) {
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDiscard,
            title = { Text("Descartar alterações?") },
            text = { Text("O que você fez neste treino não será salvo.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDiscard) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDiscard) { Text("Continuar editando") }
            }
        )
    }

    state.importPrompt?.let { prompt ->
        val found = prompt.totalInFile - prompt.unknownIds.size
        AlertDialog(
            onDismissRequest = viewModel::onDismissImport,
            title = { Text("Alguns exercícios não existem") },
            text = {
                Text(
                    "$found de ${prompt.totalInFile} exercícios encontrados.\n\n" +
                        "Desconhecidos: ${prompt.unknownIds.joinToString(", ")}\n\n" +
                        "Importar mesmo assim, sem esses exercícios?"
                )
            },
            confirmButton = { TextButton(onClick = viewModel::onConfirmImport) { Text("Importar") } },
            dismissButton = { TextButton(onClick = viewModel::onDismissImport) { Text("Cancelar") } }
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onMessageShown,
            title = { Text("Não foi possível importar") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::onMessageShown) { Text("OK") } }
        )
    }

    state.deletePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteCustom,
            title = { Text("Excluir \"${prompt.exercise.name}\"?") },
            text = {
                Text(
                    when (prompt.usedInWorkouts) {
                        0 -> "O exercício e a imagem dele serão apagados do aparelho."
                        1 -> "Usado em 1 treino. Ele será removido desse treino também."
                        else -> "Usado em ${prompt.usedInWorkouts} treinos. Ele será removido desses treinos também."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmDeleteCustom,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDeleteCustom) { Text("Cancelar") } }
        )
    }

    state.customForm?.let { form ->
        CustomExerciseDialog(
            form = form,
            onNameChange = viewModel::onCustomName,
            onGroupChange = viewModel::onCustomGroup,
            onPickMedia = viewModel::onCustomMedia,
            onRemoveMedia = viewModel::onCustomRemoveMedia,
            onSave = viewModel::onCustomSave,
            onDismiss = viewModel::onCustomDismiss
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseDialog(
    form: CustomExerciseForm,
    onNameChange: (String) -> Unit,
    onGroupChange: (String) -> Unit,
    onPickMedia: (android.net.Uri) -> Unit,
    onRemoveMedia: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPickMedia)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo exercício") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = onNameChange,
                        label = { Text("Nome") },
                        singleLine = true,
                        isError = form.nameError != null,
                        supportingText = form.nameError?.let { { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_nome")
                    )
                }
                item {
                    Text("Grupo muscular", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MuscleGroups.ORDER.forEach { group ->
                            FilterChip(
                                selected = form.group == group,
                                onClick = { onGroupChange(group) },
                                label = { Text(group) }
                            )
                        }
                    }
                }
                item {
                    Text("Imagem ou GIF (opcional)", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "GIF, WebP, PNG ou JPG · até 2 MB · até ${MediaValidator.MAX_DIMENSION} px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (form.mediaFileName != null) {
                        ExerciseMedia(
                            ExerciseEntity(
                                id = "preview",
                                name = form.name.ifBlank { "Pré-visualização" },
                                muscleGroup = form.group,
                                mediaPath = form.mediaFileName,
                                isCustom = true
                            )
                        )
                        TextButton(onClick = onRemoveMedia) { Text("Remover imagem") }
                    } else {
                        OutlinedButton(
                            onClick = { mediaLauncher.launch(IMAGE_MIME_TYPES) },
                            enabled = !form.busy
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (form.busy) "Verificando..." else "Escolher arquivo")
                        }
                    }
                    form.mediaError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !form.busy) { Text("Salvar e adicionar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
