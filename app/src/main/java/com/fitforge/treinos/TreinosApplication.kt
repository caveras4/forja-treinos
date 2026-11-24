package com.fitforge.treinos

import android.app.Application
import com.fitforge.treinos.data.local.AppDatabase
import com.fitforge.treinos.data.repository.LocalWorkoutRepository
import com.fitforge.treinos.data.repository.WorkoutRepository

class TreinosApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val workoutRepository: WorkoutRepository by lazy {
        LocalWorkoutRepository(database.workoutDao())
    }
}
