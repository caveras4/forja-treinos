package com.fitforge.treinos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query(
        """
        SELECT w.*, (SELECT COUNT(*) FROM workout_exercises we WHERE we.workoutId = w.id) AS exerciseCount
        FROM workouts w ORDER BY w.orderIndex ASC, w.id ASC
        """
    )
    fun observeSummaries(): Flow<List<WorkoutSummary>>

    @Query("SELECT * FROM workouts ORDER BY orderIndex ASC, id ASC")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT name FROM workouts")
    suspend fun getNames(): List<String>

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM workouts")
    suspend fun getNextOrderIndex(): Int

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()
}

@Dao
interface WorkoutExerciseDao {

    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC, id ASC")
    fun observeForWorkout(workoutId: Long): Flow<List<WorkoutExerciseDetail>>

    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex ASC, id ASC")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutExerciseDetail>

    @Query("SELECT * FROM workout_exercises ORDER BY workoutId ASC, orderIndex ASC")
    suspend fun getAll(): List<WorkoutExerciseEntity>

    @Insert
    suspend fun insertAll(items: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteForWorkout(workoutId: Long)

    @Query("UPDATE workout_exercises SET weightKg = :weightKg WHERE id = :id")
    suspend fun updateWeight(id: Long, weightKg: Double?)

    @Query("SELECT COUNT(DISTINCT workoutId) FROM workout_exercises WHERE exerciseId = :exerciseId")
    suspend fun countWorkoutsUsing(exerciseId: String): Int
}

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT id FROM exercises")
    suspend fun getAllIds(): List<String>

    @Query("SELECT name FROM exercises")
    suspend fun getAllNames(): List<String>

    @Query("SELECT * FROM exercises WHERE isCustom = 1")
    suspend fun getCustom(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises WHERE isCustom = 0")
    suspend fun countCatalog(): Int

    @androidx.room.Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)

    @Insert
    suspend fun insert(exercise: ExerciseEntity)

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: String)

    @Query("DELETE FROM exercises WHERE isCustom = 1")
    suspend fun deleteAllCustom()
}
