package com.fitforge.treinos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [WorkoutEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "forja_treinos.db"
                )
                    .addCallback(SeedDatabaseCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.workoutDao()
                    val now = System.currentTimeMillis()
                    dao.insertAll(
                        listOf(
                            WorkoutEntity(
                                name = "Treino A",
                                focus = "Peito, Ombros e Tríceps",
                                colorKey = "emerald",
                                orderIndex = 0,
                                updatedAt = now
                            ),
                            WorkoutEntity(
                                name = "Treino B",
                                focus = "Costas, Bíceps e Abdômen",
                                colorKey = "cobalt",
                                orderIndex = 1,
                                updatedAt = now
                            ),
                            WorkoutEntity(
                                name = "Treino C",
                                focus = "Pernas e Glúteos",
                                colorKey = "amber",
                                orderIndex = 2,
                                updatedAt = now
                            )
                        )
                    )
                }
            }
        }
    }
}
