package com.fitforge.treinos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val focus: String = "",
    val colorKey: String = "emerald",
    val orderIndex: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
