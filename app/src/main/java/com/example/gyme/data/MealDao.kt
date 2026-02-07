package com.example.gyme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: Meal)

    @Insert
    suspend fun insertAll(meals: List<Meal>) // Buat insert rekomendasi sekaligus

    @Delete
    suspend fun deleteMeal(meal: Meal)

    // Ambil makanan hari ini (berdasarkan rentang waktu)
    @Query("SELECT * FROM meal_table WHERE dateInMillis BETWEEN :start AND :end")
    suspend fun getMealsByDate(start: Long, end: Long): List<Meal>

    // Hapus semua makanan hari ini (untuk reset rekomendasi)
    @Query("DELETE FROM meal_table WHERE dateInMillis BETWEEN :start AND :end")
    suspend fun clearMealsByDate(start: Long, end: Long)
}