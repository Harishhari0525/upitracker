package com.example.upitracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiLiteSummaryDao {
    @Query("SELECT * FROM upi_lite_summaries ORDER BY date DESC")
    fun getAllSummaries(): Flow<List<UpiLiteSummary>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) // <-- Changed from REPLACE
    suspend fun insert(summary: UpiLiteSummary)

    @Query("DELETE FROM upi_lite_summaries")
    suspend fun deleteAll()

    // Add this to check for duplicates
    @Query("SELECT * FROM upi_lite_summaries WHERE date = :date AND bank = :bank LIMIT 1")
    suspend fun getSummaryByDateAndBank(date: String, bank: String): UpiLiteSummary?
}
