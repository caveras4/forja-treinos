package com.fitforge.treinos.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onCreateClick: () -> Unit,
    onEditClick: (WorkoutEntity) -> Unit,
    onDeleteClick: (WorkoutEntity) -> Unit,
    onWorkoutClick: (WorkoutEntity) -> Unit,
    onSaveWorkout: (name: String, focus: String, colorKey: String) -> Unit,
    onDismissEditor: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onSelectPalette: (AppPalette) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 840.dp
        val isMediumTablet = maxWidth >= 600.dp
        val columns = when {
            maxWidth >= 1024.dp -> 3
            isMediumTablet -> 2
            else -> 1
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val drawerContent: @Composable () -> Unit = {
            AppDrawerContent(
                selectedPalette = uiState.selectedPalette,
                themeMode = uiState.themeMode,
                onSelectPalette = onSelectPalette,
                onSelectThemeMode = onSelectThemeMode,
                onCloseDrawer = {
                    if (!isExpandedTablet) {
                        scope.launch { drawerState.close() }
                    }
                }
            )
        }

        if (isExpandedTablet) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(288.dp)) {
                        drawerContent()
                    }
                }
            ) {
                HomeScaffold(
                    uiState = uiState,
                    columns = columns,
                    showMenuButton = false,
                    onMenuClick = {},
                    onCreateClick = onCreateClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onWorkoutClick = onWorkoutClick
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.widthIn(max = 310.dp)) {
                        drawerContent()
                    }
                }
            ) {
                HomeScaffold(
                    uiState = uiState,
                    columns = columns,
                    showMenuButton = true,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onCreateClick = onCreateClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onWorkoutClick = onWorkoutClick
                )
            }
        }

        if (uiState.isEditorOpen) {
            WorkoutEditorDialog(
                initialWorkout = uiState.editorWorkout,
                defaultPalette = uiState.selectedPalette,
                onDismiss = onDismissEditor,
                onSave = onSaveWorkout
            )
        }

        uiState.deleteCandidate?.let { candidate ->
            AlertDialog(
                onDismissRequest = onDismissDelete,
                title = { Text("Excluir treino?") },
                text = {
                    Text("O treino \"${candidate.name}\" será removido do seu dispositivo.")
                },
                confirmButton = {
                    Button(onClick = onConfirmDelete) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDelete) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

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
    onWorkoutClick: (WorkoutEntity) -> Unit
) {
    var selectedActionWorkoutId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    if (showMenuButton) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = "Abrir menu"
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "Meus Treinos",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Criar novo treino",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.workouts.isEmpty() && !uiState.isLoading) {
            EmptyWorkoutsState(
                onCreateClick = onCreateClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (columns == 1) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 24.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.workouts, key = { it.id }) { workout ->
                    val isSelected = selectedActionWorkoutId == workout.id
                    WorkoutCard(
                        workout = workout,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedActionWorkoutId = null
                            } else {
                                selectedActionWorkoutId = null
                                onWorkoutClick(workout)
                            }
                        },
                        onLongClick = { selectedActionWorkoutId = workout.id },
                        onClearSelection = { selectedActionWorkoutId = null },
                        onEditClick = {
                            selectedActionWorkoutId = null
                            onEditClick(workout)
                        },
                        onDeleteClick = {
                            selectedActionWorkoutId = null
                            onDeleteClick(workout)
                        }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.workouts, key = { it.id }) { workout ->
                    val isSelected = selectedActionWorkoutId == workout.id
                    WorkoutCard(
                        workout = workout,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedActionWorkoutId = null
                            } else {
                                selectedActionWorkoutId = null
                                onWorkoutClick(workout)
                            }
                        },
                        onLongClick = { selectedActionWorkoutId = workout.id },
                        onClearSelection = { selectedActionWorkoutId = null },
                        onEditClick = {
                            selectedActionWorkoutId = null
                            onEditClick(workout)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutCard(
    workout: WorkoutEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClearSelection: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (workout.focus.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workout.focus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Editar ${workout.name}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Excluir ${workout.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancelar seleção",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkoutsState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nenhum treino cadastrado",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Toque no botão + para criar seu primeiro treino.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onCreateClick) {
            Text("Criar treino")
        }
    }
}

@Composable
private fun AppDrawerContent(
    selectedPalette: AppPalette,
    themeMode: ThemeMode,
    onSelectPalette: (AppPalette) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onCloseDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Forja Treinos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        NavigationDrawerItem(
            label = { Text("Meus Treinos") },
            selected = true,
            onClick = onCloseDrawer,
            icon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Aparência",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

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
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectThemeMode(mode) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Paleta de cores",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppPalette.entries.forEach { palette ->
                val swatchColor = when (palette) {
                    AppPalette.EMERALD -> Color(0xFF059669)
                    AppPalette.COBALT -> Color(0xFF2563EB)
                    AppPalette.AMBER -> Color(0xFFD97706)
                    AppPalette.CRIMSON -> Color(0xFFE11D48)
                }
                val isSelected = palette == selectedPalette
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .clickable { onSelectPalette(palette) }
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "${palette.label} selecionado",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Menu completo e área de exercícios serão adicionados nas próximas etapas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun WorkoutEditorDialog(
    initialWorkout: WorkoutEntity?,
    defaultPalette: AppPalette,
    onDismiss: () -> Unit,
    onSave: (name: String, focus: String, colorKey: String) -> Unit
) {
    var name by remember(initialWorkout) { mutableStateOf(initialWorkout?.name ?: "") }
    var focus by remember(initialWorkout) { mutableStateOf(initialWorkout?.focus ?: "") }
    val colorKey = initialWorkout?.colorKey ?: defaultPalette.key
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialWorkout == null) "Novo treino" else "Editar treino")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Nome do treino") },
                    placeholder = { Text("Ex.: Treino D") },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Informe ao menos 2 caracteres.") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = focus,
                    onValueChange = { focus = it },
                    label = { Text("Foco muscular (opcional)") },
                    placeholder = { Text("Ex.: Costas e Bíceps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length < 2) {
                        showError = true
                    } else {
                        onSave(name, focus, colorKey)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
