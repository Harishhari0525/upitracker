package com.example.upitracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // This query is good for checking duplicates before insertion if MainActivity's importOldUpiSms is the primary import path.
    @Query("SELECT * FROM transactions WHERE amount = :amount AND date = :date AND description = :desc LIMIT 1")
    suspend fun getTransactionByDetails(amount: Double, date: Long, desc: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE) // REPLACE is fine if you intend to update existing on conflict.
    // If duplicates based on a unique constraint (other than id) should be ignored, consider IGNORE.
    // However, your getTransactionByDetails check handles manual duplicate prevention well.
    suspend fun insert(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}