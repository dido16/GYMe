package com.example.gyme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_history_table")
data class RunHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dateInMillis: Long,
    val distanceKm: Double,
    val durationInMillis: Long,
    val caloriesBurned: Int
)