package com.example.upitracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiLiteSummaryDao {
    @Query("SELECT * FROM upi_lite_summaries ORDER BY id DESC")
    fun getAllSummaries(): Flow<List<UpiLiteSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: UpiLiteSummary)

    @Query("DELETE FROM upi_lite_summaries")
    suspend fun deleteAll()
}
