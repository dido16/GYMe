package com.example.gyme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_table")
data class WeightHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dateInMillis: Long,
    val weightKg: Float
)