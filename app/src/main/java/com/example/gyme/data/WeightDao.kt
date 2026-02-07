package com.example.gyme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(weight: WeightHistory)

    // Ambil semua data diurutkan dari yang terlama ke terbaru (biar grafiknya bener dari kiri ke kanan)
    @Query("SELECT * FROM weight_table ORDER BY dateInMillis ASC")
    suspend fun getAllWeights(): List<WeightHistory>

    // Ambil data terakhir buat update profil otomatis
    @Query("SELECT weightKg FROM weight_table ORDER BY dateInMillis DESC LIMIT 1")
    suspend fun getLatestWeight(): Float?
}