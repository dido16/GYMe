package com.example.gyme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RunHistoryDao {
    @Insert
    suspend fun insertRun(run: RunHistory)

    @Delete
    suspend fun deleteRun(run: RunHistory)

    @Query("SELECT * FROM run_history_table ORDER BY dateInMillis DESC")
    suspend fun getAllRuns(): List<RunHistory>
}