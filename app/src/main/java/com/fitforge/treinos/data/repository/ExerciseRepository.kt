package com.fitforge.treinos.data.repository

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.local.ExerciseEntity
import com.fitforge.treinos.data.media.CustomMediaStorage
import com.fitforge.treinos.domain.WorkoutRules
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.util.UUID

sealed interface CreateExerciseResult {
    data class Ok(val exercise: ExerciseEntity) : CreateExerciseResult
    data class Invalid(val message: String) : CreateExerciseResult
}

interface ExerciseRepository {
    fun observeAll(): Flow<List<ExerciseEntity>>
    suspend fun allIds(): Set<String>
    suspend fun createCustom(name: String, muscleGroup: String, mediaFileName: String?): CreateExerciseResult
    suspend fun countWorkoutsUsing(exerciseId: String): Int
    suspend fun deleteCustom(exercise: ExerciseEntity)
    suspend fun syncCatalog()
}

@Serializable
private data class CatalogFile(val exercicios: List<CatalogEntry>)

@Serializable
private data class CatalogEntry(val id: String, val nome: String, val grupo: String, val midia: String)

class LocalExerciseRepository(
    private val database: AppDatabase,
    private val assets: AssetManager,
    private val mediaStorage: CustomMediaStorage
) : ExerciseRepository {

    private val exerciseDao = database.exerciseDao()

    override fun observeAll(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    override suspend fun allIds(): Set<String> = exerciseDao.getAllIds().toSet()

    override suspend fun createCustom(
        name: String,
        muscleGroup: String,
        mediaFileName: String?
    ): CreateExerciseResult {
        val trimmed = name.trim()
        WorkoutRules.validateExerciseName(trimmed)?.let { return CreateExerciseResult.Invalid(it) }
        val taken = exerciseDao.getAllNames().any { it.equals(trimmed, ignoreCase = true) }
        if (taken) return CreateExerciseResult.Invalid("Já existe um exercício com esse nome.")
        val entity = ExerciseEntity(
            id = "custom-${UUID.randomUUID()}",
            name = trimmed,
            muscleGroup = muscleGroup,
            mediaPath = mediaFileName,
            isCustom = true
        )
        exerciseDao.insert(entity)
        return CreateExerciseResult.Ok(entity)
    }

    override suspend fun countWorkoutsUsing(exerciseId: String): Int =
        database.workoutExerciseDao().countWorkoutsUsing(exerciseId)

    /** Os itens dos treinos que usam o exercício saem junto (ON DELETE CASCADE). */
    override suspend fun deleteCustom(exercise: ExerciseEntity) {
        if (!exercise.isCustom) return
        exerciseDao.deleteCustom(exercise.id)
        mediaStorage.delete(exercise.mediaPath)
    }

    /** Copia o catálogo de assets/exercises.json para o banco (insere novos, atualiza nomes). */
    override suspend fun syncCatalog() {
        val text = assets.open(CATALOG_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val catalog = catalogJson.decodeFromString(CatalogFile.serializer(), text)
        database.withTransaction {
            exerciseDao.upsertAll(
                catalog.exercicios.map {
                    ExerciseEntity(
                        id = it.id,
                        name = it.nome,
                        muscleGroup = it.grupo,
                        mediaPath = it.midia,
                        isCustom = false
                    )
                }
            )
        }
    }

    companion object {
        const val CATALOG_FILE = "exercises.json"
        private val catalogJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    }
}
