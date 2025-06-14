package com.example.upitracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /**
     * Gets all non-archived transactions, ordered by date descending.
     */
    @Query("SELECT * FROM transactions WHERE isArchived = 0 ORDER BY date DESC") // ✨ Filter for non-archived
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * Gets all archived transactions, ordered by date descending.
     */
    @Query("SELECT * FROM transactions WHERE isArchived = 1 ORDER BY date DESC") // ✨ New query for archived
    fun getArchivedTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE amount = :amount AND date = :date AND description = :desc LIMIT 1")
    suspend fun getTransactionByDetails(amount: Double, date: Long, desc: String): Transaction?

    // General insert, will replace if an item with the same primary key (id) exists.
    // Used for both new transactions and can be used if re-importing/updating full transaction details.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    // General update method for a transaction object.
    // Useful for category changes or if you update the whole object including the isArchived flag.
    @Update
    suspend fun update(transaction: Transaction) // ✨ Ensure this @Update method is present

    /**
     * Specifically updates the isArchived status of a transaction.
     */
    @Query("UPDATE transactions SET isArchived = :isArchived WHERE id = :transactionId") // ✨ New method
    suspend fun setArchivedStatus(transactionId: Int, isArchived: Boolean)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll() // This will delete ALL transactions, including archived ones.
    // You might want a separate method to delete only non-archived or only archived.

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

}