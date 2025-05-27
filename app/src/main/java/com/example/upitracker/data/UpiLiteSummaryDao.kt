package com.example.upitracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiLiteSummaryDao {
    @Query("SELECT * FROM upi_lite_summaries ORDER BY date DESC") // ✨ Now sorts chronologically
    fun getAllSummaries(): Flow<List<UpiLiteSummary>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(summary: UpiLiteSummary)

    @Query("DELETE FROM upi_lite_summaries")
    suspend fun deleteAll()

    @Update // ✨ Add this update method ✨
    suspend fun update(summary: UpiLiteSummary)

    // date parameter will now be Long
    @Query("SELECT * FROM upi_lite_summaries WHERE date = :date AND bank = :bank LIMIT 1")
    suspend fun getSummaryByDateAndBank(date: Long, bank: String): UpiLiteSummary?
}