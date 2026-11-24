package com.fitforge.treinos.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Exercício do catálogo (embutido no APK) ou personalizado (criado pelo usuário).
 *
 * [mediaPath]: no catálogo é o caminho dentro de assets ("exercises/x.webp");
 * no personalizado é o nome do arquivo em filesDir/custom_media, ou null sem imagem.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val mediaPath: String?,
    val isCustom: Boolean
)

/** Exercício dentro de um treino. Sem histórico: [weightKg] é sempre a carga atual. */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutId", "exerciseId"], unique = true),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double?,
    val orderIndex: Int
)

data class WorkoutExerciseDetail(
    @Embedded val item: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity
)

data class WorkoutSummary(
    @Embedded val workout: WorkoutEntity,
    val exerciseCount: Int
)
