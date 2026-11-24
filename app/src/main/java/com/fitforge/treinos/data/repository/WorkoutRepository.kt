package com.fitforge.treinos.data.repository

import androidx.room.withTransaction
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.local.WorkoutEntity
import com.fitforge.treinos.data.local.WorkoutExerciseDetail
import com.fitforge.treinos.data.local.WorkoutExerciseEntity
import com.fitforge.treinos.data.local.WorkoutSummary
import com.fitforge.treinos.domain.DraftItem
import com.fitforge.treinos.domain.WeightFormat
import com.fitforge.treinos.domain.WeightInput
import com.fitforge.treinos.domain.WorkoutDraft
import com.fitforge.treinos.domain.transfer.WorkoutExerciseJson
import com.fitforge.treinos.domain.transfer.WorkoutImportParser
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeSummaries(): Flow<List<WorkoutSummary>>
    fun observeWorkout(id: Long): Flow<WorkoutEntity?>
    fun observeExercises(workoutId: Long): Flow<List<WorkoutExerciseDetail>>
    suspend fun loadDraft(workoutId: Long): WorkoutDraft?
    suspend fun existingNames(): List<String>

    /** Cria (workoutId == null) ou substitui o treino inteiro numa transação. Devolve o id. */
    suspend fun saveDraft(draft: WorkoutDraft, workoutId: Long?): Long
    suspend fun deleteWorkout(workout: WorkoutEntity)
    suspend fun updateWeight(itemId: Long, weightKg: Double?)
    suspend fun exportJson(workoutId: Long): String?
}

class LocalWorkoutRepository(
    private val database: AppDatabase
) : WorkoutRepository {

    private val workoutDao = database.workoutDao()
    private val itemDao = database.workoutExerciseDao()

    override fun observeSummaries(): Flow<List<WorkoutSummary>> = workoutDao.observeSummaries()

    override fun observeWorkout(id: Long): Flow<WorkoutEntity?> = workoutDao.observeById(id)

    override fun observeExercises(workoutId: Long): Flow<List<WorkoutExerciseDetail>> =
        itemDao.observeForWorkout(workoutId)

    override suspend fun loadDraft(workoutId: Long): WorkoutDraft? {
        val workout = workoutDao.getById(workoutId) ?: return null
        val items = itemDao.getForWorkout(workoutId)
        return WorkoutDraft(
            name = workout.name,
            focus = workout.focus,
            colorKey = workout.colorKey,
            items = items.map {
                DraftItem(
                    exerciseId = it.item.exerciseId,
                    sets = it.item.sets,
                    reps = it.item.reps,
                    weightText = WeightFormat.toInput(it.item.weightKg)
                )
            }
        )
    }

    override suspend fun existingNames(): List<String> = workoutDao.getNames()

    override suspend fun saveDraft(draft: WorkoutDraft, workoutId: Long?): Long =
        database.withTransaction { insertOrReplace(draft, workoutId) }

    /** Precisa ser chamado dentro de uma transação. */
    internal suspend fun insertOrReplace(draft: WorkoutDraft, workoutId: Long?): Long {
        val now = System.currentTimeMillis()
        val existing = workoutId?.let { workoutDao.getById(it) }
        val id = if (existing == null) {
            workoutDao.insert(
                WorkoutEntity(
                    name = draft.name.trim(),
                    focus = draft.focus.trim(),
                    colorKey = draft.colorKey,
                    orderIndex = workoutDao.getNextOrderIndex(),
                    updatedAt = now
                )
            )
        } else {
            workoutDao.update(
                existing.copy(
                    name = draft.name.trim(),
                    focus = draft.focus.trim(),
                    colorKey = draft.colorKey,
                    updatedAt = now
                )
            )
            existing.id
        }
        itemDao.deleteForWorkout(id)
        itemDao.insertAll(
            draft.items.mapIndexed { index, item ->
                WorkoutExerciseEntity(
                    workoutId = id,
                    exerciseId = item.exerciseId,
                    sets = item.sets,
                    reps = item.reps,
                    weightKg = (WeightFormat.parse(item.weightText) as? WeightInput.Value)?.kg?.takeIf { it > 0 },
                    orderIndex = index
                )
            }
        )
        return id
    }

    override suspend fun deleteWorkout(workout: WorkoutEntity) {
        workoutDao.delete(workout)
    }

    override suspend fun updateWeight(itemId: Long, weightKg: Double?) {
        itemDao.updateWeight(itemId, weightKg)
    }

    override suspend fun exportJson(workoutId: Long): String? {
        val workout = workoutDao.getById(workoutId) ?: return null
        val items = itemDao.getForWorkout(workoutId).map {
            WorkoutExerciseJson(
                id = it.item.exerciseId,
                series = it.item.sets,
                repeticoes = it.item.reps,
                cargaKg = it.item.weightKg
            )
        }
        return WorkoutImportParser.export(workout.name, workout.focus, workout.colorKey, items)
    }
}
