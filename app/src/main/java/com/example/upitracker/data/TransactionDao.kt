package com.example.upitracker.data

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /**
     * Gets all non-archived transactions, ordered by date descending.
     */
    @Query("SELECT * FROM transactions WHERE isArchived = 0 AND pendingDeletionTimestamp IS NULL ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * Gets all archived transactions, ordered by date descending.
     */
    @Query("SELECT * FROM transactions WHERE isArchived = 1 ORDER BY date DESC") // ✨ New query for archived
    fun getArchivedTransactions(): Flow<List<Transaction>>

    // ✨ NEW: This is the powerful new function that will handle all filtering and sorting. ✨
    @RawQuery(observedEntities = [Transaction::class])
    fun getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<Transaction>>

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

    @Query("DELETE FROM transactions WHERE pendingDeletionTimestamp IS NOT NULL AND pendingDeletionTimestamp < :cutoffTimestamp")
    suspend fun permanentlyDeletePending(cutoffTimestamp: Long)

    @Query("SELECT * FROM transactions WHERE type = 'DEBIT' AND senderOrReceiver = :sender AND isArchived = 0 AND linkedTransactionId IS NULL ORDER BY date DESC")
    suspend fun findPotentialDebitsForRefund(sender: String): List<Transaction>

    // Unlinks a transaction
    @Query("UPDATE transactions SET linkedTransactionId = NULL WHERE id = :transactionId")
    suspend fun unlinkTransaction(transactionId: Int)

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateCategoryName(oldCategory: String, newCategory: String)

    @Query("UPDATE transactions SET category = NULL WHERE category = :categoryName")
    suspend fun clearCategoryForTransactions(categoryName: String)

    @Query("SELECT * FROM transactions WHERE category = :categoryName AND type = 'DEBIT' AND category != :refundCategory AND date BETWEEN :startDate AND :endDate AND isArchived = 0")
    suspend fun getTransactionsForBudgetCheck(categoryName: String, startDate: Long, endDate: Long, refundCategory: String): List<Transaction>

}