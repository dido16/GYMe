package com.example.gyme.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. Tambahkan RunHistory di entities
@Database(entities = [Workout::class, RunHistory::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    // 2. Tambahkan DAO baru
    abstract fun runHistoryDao(): RunHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database"
                )
                    .fallbackToDestructiveMigration() // Biar kalau versi naik, DB lama direset (aman buat development)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}