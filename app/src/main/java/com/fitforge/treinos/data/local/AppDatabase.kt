package com.fitforge.treinos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkoutEntity::class, ExerciseEntity::class, WorkoutExerciseEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        const val NAME = "forja_treinos.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // O catálogo e os treinos de exemplo não são inseridos aqui: quem faz isso é o
        // AppInitializer, depois que o banco abre. Assim o seed não depende de INSTANCE.
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

/** v1 → v2: adiciona o catálogo de exercícios e os exercícios de cada treino. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercises` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`muscleGroup` TEXT NOT NULL, `mediaPath` TEXT, `isCustom` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `sets` INTEGER NOT NULL, " +
                "`reps` INTEGER NOT NULL, `weightKg` REAL, `orderIndex` INTEGER NOT NULL, " +
                "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_exercises_workoutId_exerciseId` " +
                "ON `workout_exercises` (`workoutId`, `exerciseId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_exercises_exerciseId` ON `workout_exercises` (`exerciseId`)"
        )
    }
}
