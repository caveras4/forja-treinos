package com.fitforge.treinos

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.fitforge.treinos.data.backup.BackupManager
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.media.CustomMediaStorage
import com.fitforge.treinos.data.repository.ExerciseRepository
import com.fitforge.treinos.data.repository.LocalExerciseRepository
import com.fitforge.treinos.data.repository.LocalWorkoutRepository
import com.fitforge.treinos.data.repository.SettingsRepository
import com.fitforge.treinos.data.repository.WorkoutRepository
import com.fitforge.treinos.domain.DraftItem
import com.fitforge.treinos.domain.WorkoutDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Injeção de dependência manual: cria cada peça uma vez e entrega para os ViewModels. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val settingsRepository = SettingsRepository(appContext.dataStore)
    val mediaStorage = CustomMediaStorage(appContext)

    private val localWorkoutRepository = LocalWorkoutRepository(database)
    val workoutRepository: WorkoutRepository = localWorkoutRepository
    val exerciseRepository: ExerciseRepository =
        LocalExerciseRepository(database, appContext.assets, mediaStorage)
    val backupManager = BackupManager(appContext.contentResolver, database, localWorkoutRepository, mediaStorage)

    private val _ready = MutableStateFlow(false)

    /** Vira true quando o catálogo já está no banco. */
    val ready: StateFlow<Boolean> = _ready

    /** Roda a cada abertura do app: sincroniza o catálogo e cria os exemplos na primeira vez. */
    suspend fun initialize() {
        exerciseRepository.syncCatalog()
        if (!settingsRepository.isSeeded()) {
            if (database.workoutDao().count() == 0) {
                SEED.forEach { workoutRepository.saveDraft(it, workoutId = null) }
            }
            settingsRepository.markSeeded()
        }
        _ready.value = true
    }

    private companion object {
        fun item(id: String, sets: Int, reps: Int) = DraftItem(id, sets, reps)

        val SEED = listOf(
            WorkoutDraft(
                name = "Treino A",
                focus = "Peito, Ombros e Tríceps",
                colorKey = "emerald",
                items = listOf(
                    item("supino-reto-barra", 4, 10),
                    item("supino-inclinado-halteres", 3, 12),
                    item("crucifixo-reto", 3, 12),
                    item("desenvolvimento-halteres", 3, 10),
                    item("elevacao-lateral", 3, 15),
                    item("triceps-corda", 3, 12)
                )
            ),
            WorkoutDraft(
                name = "Treino B",
                focus = "Costas, Bíceps e Abdômen",
                colorKey = "cobalt",
                items = listOf(
                    item("puxada-frontal-aberta", 4, 10),
                    item("remada-baixa", 3, 12),
                    item("remada-unilateral-halter", 3, 10),
                    item("rosca-direta-barra", 3, 10),
                    item("rosca-martelo", 3, 12),
                    item("abdominal-supra", 3, 20)
                )
            ),
            WorkoutDraft(
                name = "Treino C",
                focus = "Pernas e Glúteos",
                colorKey = "amber",
                items = listOf(
                    item("agachamento-livre", 4, 10),
                    item("leg-press", 4, 12),
                    item("cadeira-extensora", 3, 12),
                    item("mesa-flexora", 3, 12),
                    item("elevacao-pelvica", 3, 12),
                    item("panturrilha-em-pe-maquina", 4, 15)
                )
            )
        )
    }
}
