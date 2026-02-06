package com.example.gyme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_table")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val day: String,
    val exerciseName: String,
    val muscleGroup: String,
    val sets: String,
    val reps: String,
    val instructions: String,
    val imageUrl: String,
    var isCompleted: Boolean = false,
    var weight: Double = 0.0
)