package com.example.gyme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkoutDao {
    // Ambil latihan berdasarkan hari (misal: ambil latihan hari "Monday")
    @Query("SELECT * FROM workout_table WHERE day = :dayName")
    suspend fun getWorkoutsByDay(dayName: String): List<Workout>

    // Masukkan data latihan (untuk persiapan data awal)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<Workout>)

    // Cek apakah database kosong (biar gak double insert nanti)
    @Query("SELECT COUNT(*) FROM workout_table")
    suspend fun getCount(): Int

    @Update
    suspend fun updateWorkout(workout: Workout)

    // FUNGSI BARU (Opsional): Reset semua checklist (Misal buat besoknya)
    @Query("UPDATE workout_table SET isCompleted = 0")
    suspend fun resetAllWorkouts()

    @Query("UPDATE workout_table SET weight = :newWeight WHERE id = :workoutId")
    suspend fun updateWeight(workoutId: Int, newWeight: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)
}