package com.fitforge.treinos.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitforge.treinos.data.local.WorkoutExerciseDetail
import com.fitforge.treinos.domain.WeightFormat
import com.fitforge.treinos.ui.components.ExerciseMedia
import com.fitforge.treinos.ui.components.SuccessColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(viewModel: SessionViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Treino excluído enquanto a tela estava aberta.
    LaunchedEffect(state.loading, state.workout) {
        if (!state.loading && state.workout == null) onBack()
    }
    // Treino finalizado: volta para a Home (o progresso já foi apagado).
    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.workout?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (state.items.isNotEmpty()) {
                            Text(
                                "Em andamento · ${state.doneCount} de ${state.items.size} feitos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (state.doneCount > 0) {
                        IconButton(onClick = viewModel::onReset) {
                            Icon(Icons.Rounded.Replay, contentDescription = "Desmarcar todos")
                        }
                    }
                    TextButton(
                        onClick = viewModel::onFinishClick,
                        modifier = Modifier.testTag("finalizar")
                    ) { Text("Finalizar") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("lista_sessao"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.items.isNotEmpty()) {
                item(key = "progresso") {
                    LinearProgressIndicator(
                        progress = { state.doneCount.toFloat() / state.items.size },
                        color = SuccessColors.accent(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (state.allDone) {
                item(key = "concluido") {
                    Surface(
                        color = SuccessColors.container(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = SuccessColors.accent())
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Treino concluído!",
                                style = MaterialTheme.typography.titleMedium,
                                color = SuccessColors.onContainer(),
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = viewModel::finish) { Text("Finalizar") }
                        }
                    }
                }
            }
            if (!state.loading && state.items.isEmpty() && state.workout != null) {
                item {
                    Text(
                        "Este treino não tem exercícios. Edite o treino para adicionar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.items, key = { it.item.id }) { detail ->
                SessionItem(
                    detail = detail,
                    done = state.isDone(detail),
                    expanded = state.expandedId == detail.item.id,
                    onToggleExpand = { viewModel.onToggleExpand(detail.item.id) },
                    onToggleDone = { viewModel.onToggleDone(detail) },
                    onStepWeight = { delta -> viewModel.onStepWeight(detail, delta) },
                    onEditWeight = { viewModel.onOpenWeightDialog(detail) }
                )
            }
        }
    }

    if (state.showFinishDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissFinish,
            title = { Text("Finalizar treino?") },
            text = {
                Text(
                    "Você fez ${state.doneCount} de ${state.items.size} exercícios. " +
                        "Ao finalizar, as marcações de feito são apagadas. As cargas continuam salvas."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::finish, modifier = Modifier.testTag("confirmar_finalizar")) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissFinish) { Text("Continuar treino") } }
        )
    }

    state.weightDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissWeight,
            title = { Text("Carga: ${dialog.exerciseName}") },
            text = {
                OutlinedTextField(
                    value = dialog.text,
                    onValueChange = viewModel::onWeightTextChange,
                    label = { Text("Carga") },
                    placeholder = { Text("Vazio = sem carga") },
                    suffix = { Text("kg") },
                    singleLine = true,
                    isError = dialog.error != null,
                    supportingText = dialog.error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.testTag("campo_carga_dialogo")
                )
            },
            confirmButton = { TextButton(onClick = viewModel::onConfirmWeight) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = viewModel::onDismissWeight) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SessionItem(
    detail: WorkoutExerciseDetail,
    done: Boolean,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleDone: () -> Unit,
    onStepWeight: (Double) -> Unit,
    onEditWeight: () -> Unit
) {
    val item = detail.item
    val container by animateColorAsState(
        if (done) SuccessColors.container() else MaterialTheme.colorScheme.surface,
        label = "sessionItem"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(1.dp, if (done) SuccessColors.accent() else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sessao-${detail.exercise.id}")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Checkbox(
                    checked = done,
                    onCheckedChange = { onToggleDone() },
                    colors = CheckboxDefaults.colors(checkedColor = SuccessColors.accent()),
                    modifier = Modifier.semantics {
                        contentDescription = if (done) "Desmarcar ${detail.exercise.name}" else "Marcar ${detail.exercise.name} como feito"
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        detail.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        color = if (done) SuccessColors.onContainer() else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${item.sets} × ${item.reps} · ${WeightFormat.display(item.weightKg)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    ExerciseMedia(detail.exercise)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        InfoBlock("Séries", item.sets.toString())
                        InfoBlock("Repetições", item.reps.toString())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { onStepWeight(-SessionViewModel.WEIGHT_STEP) },
                            enabled = item.weightKg != null
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Diminuir carga")
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // Largura fixa: os botões −/+ não mudam de lugar quando o texto muda.
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(180.dp)
                                .clickable(onClick = onEditWeight)
                                .padding(8.dp)
                                .testTag("carga-sessao-${detail.exercise.id}")
                        ) {
                            Text("Carga", style = MaterialTheme.typography.labelMedium)
                            Text(
                                WeightFormat.display(item.weightKg),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "toque para digitar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(onClick = { onStepWeight(SessionViewModel.WEIGHT_STEP) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Aumentar carga")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}
