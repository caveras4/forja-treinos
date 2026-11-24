package com.fitforge.treinos.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.local.WorkoutSummary
import com.fitforge.treinos.ui.components.SuccessColors
import com.fitforge.treinos.ui.components.WorkoutBadge
import com.fitforge.treinos.ui.components.paletteSwatch
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onCreateClick: () -> Unit,
    onEditClick: (WorkoutEntity) -> Unit,
    onStartClick: (WorkoutEntity) -> Unit
) {
    val exportWorkoutLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
        viewModel::exportWorkoutTo
    )
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
        viewModel::exportBackupTo
    )
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
        viewModel::importBackupFrom
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 840.dp
        val columns = when {
            maxWidth >= 1024.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val closeDrawer: () -> Unit = {
            if (!isExpandedTablet) scope.launch { drawerState.close() }
        }

        val drawerContent: @Composable () -> Unit = {
            AppDrawerContent(
                selectedPalette = uiState.selectedPalette,
                themeMode = uiState.themeMode,
                busy = uiState.busy,
                onSelectPalette = viewModel::selectPalette,
                onSelectThemeMode = viewModel::setThemeMode,
                onExportBackup = {
                    closeDrawer()
                    exportBackupLauncher.launch("forja-backup-${LocalDate.now()}.zip")
                },
                onImportBackup = {
                    closeDrawer()
                    viewModel.requestImportBackup()
                },
                onAbout = {
                    closeDrawer()
                    viewModel.showAbout(true)
                },
                onCloseDrawer = closeDrawer
            )
        }

        val scaffold: @Composable () -> Unit = {
            HomeScaffold(
                uiState = uiState,
                columns = columns,
                showMenuButton = !isExpandedTablet,
                onMenuClick = { scope.launch { drawerState.open() } },
                onCreateClick = onCreateClick,
                onEditClick = onEditClick,
                onDeleteClick = viewModel::requestDelete,
                onExportClick = { workout ->
                    viewModel.startExport(workout)
                    exportWorkoutLauncher.launch("${fileSafe(workout.name)}.json")
                },
                // Só um treino em andamento por vez: a Home pergunta antes de trocar.
                onStartClick = { workout -> if (viewModel.requestStart(workout)) onStartClick(workout) },
                onMessageShown = viewModel::messageShown
            )
        }

        if (isExpandedTablet) {
            PermanentNavigationDrawer(
                drawerContent = { PermanentDrawerSheet(Modifier.width(288.dp)) { drawerContent() } }
            ) { scaffold() }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = { ModalDrawerSheet(Modifier.widthIn(max = 310.dp)) { drawerContent() } }
            ) { scaffold() }
        }
    }

    uiState.startConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConflict,
            title = { Text("Treino em andamento") },
            text = {
                Text(
                    "Você está fazendo \"${conflict.active.workoutName}\" " +
                        "(${conflict.active.doneCount} de ${conflict.active.total} feitos). " +
                        "Só dá para ter um treino em andamento por vez."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissConflict()
                    uiState.workouts.firstOrNull { it.workout.id == conflict.active.workoutId }
                        ?.let { onStartClick(it.workout) }
                }) { Text("Continuar ${conflict.active.workoutName}") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.replaceSession { onStartClick(conflict.target) }
                }) { Text("Encerrar e iniciar ${conflict.target.name}") }
            }
        )
    }

    uiState.deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Excluir treino?") },
            text = { Text("O treino \"${candidate.name}\" será removido do seu dispositivo.") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancelar") } }
        )
    }

    if (uiState.showImportBackupConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissImportBackup,
            title = { Text("Restaurar backup?") },
            text = {
                Text(
                    "Todos os treinos e exercícios personalizados atuais serão substituídos " +
                        "pelos do arquivo. Faça um backup antes, se quiser guardar os atuais."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importBackupLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                }) { Text("Escolher arquivo") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissImportBackup) { Text("Cancelar") } }
        )
    }

    if (uiState.showAbout) {
        AlertDialog(
            onDismissRequest = { viewModel.showAbout(false) },
            title = { Text("Sobre o Forja Treinos") },
            text = {
                Text(
                    "App 100% offline: não usa internet e não tem permissão de rede.\n\n" +
                        "Imagens e lista de exercícios: free-exercise-db " +
                        "(github.com/yuhonas/free-exercise-db), em domínio público (Unlicense). " +
                        "Nomes traduzidos para o português."
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.showAbout(false) }) { Text("OK") } }
        )
    }
}

/** "Treino A (2)" → "treino-a-2". */
private fun fileSafe(name: String): String =
    name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "treino" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    uiState: HomeUiState,
    columns: Int,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (WorkoutEntity) -> Unit,
    onDeleteClick: (WorkoutEntity) -> Unit,
    onExportClick: (WorkoutEntity) -> Unit,
    onStartClick: (WorkoutEntity) -> Unit,
    onMessageShown: () -> Unit
) {
    var selectedActionWorkoutId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (showMenuButton) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Abrir menu")
                        }
                    }
                },
                title = {
                    Text("Meus Treinos", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                },
                actions = {
                    if (uiState.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 4.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedActionWorkoutId = null
                    onCreateClick()
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(60.dp)
                    .testTag("fab_criar")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Criar novo treino", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.workouts.isEmpty() -> EmptyWorkoutsState(
                onCreateClick = onCreateClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // Fixa fora da lista: continua visível mesmo com a lista rolada.
            uiState.activeSession?.let { active ->
                ActiveSessionBanner(
                    active = active,
                    onContinue = {
                        selectedActionWorkoutId = null
                        uiState.workouts.firstOrNull { it.workout.id == active.workoutId }
                            ?.let { onStartClick(it.workout) }
                    },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lista_treinos"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // A dica fica sempre na tela (só o texto muda), para a lista não "pular".
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (selectedActionWorkoutId == null) "Toque para iniciar · segure para editar"
                        else "Escolha: editar, exportar ou excluir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.workouts, key = { it.workout.id }) { summary ->
                    val workout = summary.workout
                    val isSelected = selectedActionWorkoutId == workout.id
                    WorkoutCard(
                        summary = summary,
                        isSelected = isSelected,
                        active = uiState.activeSession?.takeIf { it.workoutId == workout.id },
                        onClick = {
                            if (selectedActionWorkoutId != null) selectedActionWorkoutId = null
                            else onStartClick(workout)
                        },
                        onLongClick = { selectedActionWorkoutId = workout.id },
                        onClearSelection = { selectedActionWorkoutId = null },
                        onEditClick = {
                            selectedActionWorkoutId = null
                            onEditClick(workout)
                        },
                        onExportClick = {
                            selectedActionWorkoutId = null
                            onExportClick(workout)
                        },
                        onDeleteClick = {
                            selectedActionWorkoutId = null
                            onDeleteClick(workout)
                        }
                    )
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutCard(
    summary: WorkoutSummary,
    isSelected: Boolean,
    active: ActiveSessionInfo?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClearSelection: () -> Unit,
    onEditClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val workout = summary.workout
    val haptic = LocalHapticFeedback.current

    val inProgress = active != null
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            inProgress -> SuccessColors.container().copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected || inProgress) 2.dp else 1.dp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                inProgress -> SuccessColors.accent()
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("treino-${workout.name}")
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WorkoutBadge(workout.name, workout.colorKey)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = listOfNotNull(
                        workout.focus.takeIf { it.isNotBlank() },
                        when (summary.exerciseCount) {
                            0 -> "sem exercícios"
                            1 -> "1 exercício"
                            else -> "${summary.exerciseCount} exercícios"
                        }
                    ).joinToString(" · ")
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isSelected) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = if (inProgress) "Continuar ${workout.name}" else "Iniciar ${workout.name}",
                        tint = paletteSwatch(workout.colorKey),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            if (active != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = SuccessColors.accent(), shape = RoundedCornerShape(50)) {
                        Text(
                            "EM ANDAMENTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${active.doneCount} de ${active.total} feitos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (active.total == 0) 0f else active.doneCount.toFloat() / active.total },
                    color = SuccessColors.accent(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isSelected) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar ${workout.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Rounded.Share, contentDescription = "Exportar ${workout.name} em JSON", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Excluir ${workout.name}", tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar seleção", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkoutsState(onCreateClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Nenhum treino cadastrado", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Toque no botão + para criar seu primeiro treino.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onCreateClick) { Text("Criar treino") }
    }
}

@Composable
private fun AppDrawerContent(
    selectedPalette: AppPalette,
    themeMode: ThemeMode,
    busy: Boolean,
    onSelectPalette: (AppPalette) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAbout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            "Forja Treinos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(16.dp))
        NavigationDrawerItem(
            label = { Text("Meus Treinos") },
            selected = true,
            onClick = onCloseDrawer,
            icon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) }
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SectionLabel("Aparência")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val selected = mode == themeMode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectThemeMode(mode) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                        }
                        Icon(icon, contentDescription = mode.label, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Paleta de cores")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPalette.entries.forEach { palette ->
                val isSelected = palette == selectedPalette
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(paletteSwatch(palette.key))
                        .clickable { onSelectPalette(palette) }
                ) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = "${palette.label} selecionado", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SectionLabel("Dados")
        NavigationDrawerItem(
            label = { Text("Exportar backup") },
            selected = false,
            onClick = { if (!busy) onExportBackup() },
            icon = { Icon(Icons.Rounded.Download, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("Restaurar backup") },
            selected = false,
            onClick = { if (!busy) onImportBackup() },
            icon = { Icon(Icons.Rounded.Upload, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("Sobre") },
            selected = false,
            onClick = onAbout,
            icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/** Faixa no topo da Home: o treino em andamento, com atalho para continuar. */
@Composable
private fun ActiveSessionBanner(active: ActiveSessionInfo, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = SuccessColors.container(),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onContinue)
            .testTag("faixa_em_andamento")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = SuccessColors.accent())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Treino em andamento",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessColors.onContainer()
                )
                Text(
                    "${active.workoutName} · ${active.doneCount} de ${active.total}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SuccessColors.onContainer(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "Continuar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SuccessColors.accent()
            )
        }
    }
}
