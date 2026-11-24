package com.fitforge.treinos.data.repository

import com.fitforge.treinos.data.local.WorkoutDao
import com.fitforge.treinos.data.local.WorkoutEntity
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkouts(): Flow<List<WorkoutEntity>>
    suspend fun getWorkout(id: Long): WorkoutEntity?
    suspend fun createWorkout(name: String, focus: String, colorKey: String): Long
    suspend fun updateWorkout(workout: WorkoutEntity)
    suspend fun deleteWorkout(workout: WorkoutEntity)
}

class LocalWorkoutRepository(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun observeWorkouts(): Flow<List<WorkoutEntity>> = workoutDao.observeAll()

    override suspend fun getWorkout(id: Long): WorkoutEntity? = workoutDao.getById(id)

    override suspend fun createWorkout(name: String, focus: String, colorKey: String): Long {
        val nextOrder = workoutDao.getNextOrderIndex()
        val entity = WorkoutEntity(
            name = name.trim(),
            focus = focus.trim(),
            colorKey = colorKey,
            orderIndex = nextOrder,
            updatedAt = System.currentTimeMillis()
        )
        return workoutDao.insert(entity)
    }

    override suspend fun updateWorkout(workout: WorkoutEntity) {
        workoutDao.update(
            workout.copy(
                name = workout.name.trim(),
                focus = workout.focus.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteWorkout(workout: WorkoutEntity) {
        workoutDao.delete(workout)
    }
}
