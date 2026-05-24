package com.example.upitracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isArchived = 0 AND pendingDeletionTimestamp IS NULL ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isArchived = 1 ORDER BY date DESC")
    fun getArchivedTransactions(): Flow<List<Transaction>>

    @RawQuery(observedEntities = [Transaction::class])
    fun getFilteredTransactions(query: SupportSQLiteQuery): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE amount = :amount AND date = :date AND description = :desc LIMIT 1")
    suspend fun getTransactionByDetails(amount: Double, date: Long, desc: String): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE amount = :amount
          AND type = :type
          AND date BETWEEN :startDate AND :endDate
          AND TRIM(description) = TRIM(:description)
          AND TRIM(senderOrReceiver) = TRIM(:senderOrReceiver)
        LIMIT 1
        """
    )
    suspend fun findDuplicateTransaction(
        amount: Double,
        type: String,
        startDate: Long,
        endDate: Long,
        description: String,
        senderOrReceiver: String
    ): Transaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReturningId(transaction: Transaction): Long

    @androidx.room.Transaction
    suspend fun insertIfNotDuplicate(transaction: Transaction): Long {
        val timeToleranceMs = 2_000L

        val existing = findDuplicateTransaction(
            amount = transaction.amount,
            type = transaction.type,
            startDate = transaction.date - timeToleranceMs,
            endDate = transaction.date + timeToleranceMs,
            description = transaction.description.trim(),
            senderOrReceiver = transaction.senderOrReceiver.trim()
        )

        if (existing != null) {
            return -1L
        }

        return insertReturningId(transaction)
    }

    @Query("SELECT MAX(date) FROM transactions")
    suspend fun getLatestTransactionTimestampIncludingArchived(): Long?


    @Update
    suspend fun update(transaction: Transaction)

    @Query("UPDATE transactions SET isArchived = :isArchived WHERE id = :transactionId")
    suspend fun setArchivedStatus(transactionId: Int, isArchived: Boolean)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Int): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdSync(id: Int): Transaction?

    @Query("DELETE FROM transactions WHERE pendingDeletionTimestamp IS NOT NULL AND pendingDeletionTimestamp < :cutoffTimestamp")
    suspend fun permanentlyDeletePending(cutoffTimestamp: Long)

    @Query("SELECT * FROM transactions WHERE type = 'DEBIT' AND senderOrReceiver = :sender AND isArchived = 0 AND linkedTransactionId IS NULL ORDER BY date DESC")
    suspend fun findPotentialDebitsForRefund(sender: String): List<Transaction>

    @Query("UPDATE transactions SET linkedTransactionId = NULL WHERE id = :transactionId")
    suspend fun unlinkTransaction(transactionId: Int)

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateCategoryName(oldCategory: String, newCategory: String)

    @Query("UPDATE transactions SET category = NULL WHERE category = :categoryName")
    suspend fun clearCategoryForTransactions(categoryName: String)

    @Query("SELECT * FROM transactions WHERE category = :categoryName AND type = 'DEBIT' AND category != :refundCategory AND date BETWEEN :startDate AND :endDate AND isArchived = 0")
    suspend fun getTransactionsForBudgetCheck(
        categoryName: String,
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND category != :refundCategory AND date BETWEEN :startDate AND :endDate AND isArchived = 0")
    suspend fun getSpentAmountInRangeSync(
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): Double?

    @Query("SELECT * FROM transactions WHERE senderOrReceiver = :sender AND isArchived = 0 ORDER BY date DESC")
    suspend fun getHistoryForMerchant(sender: String): List<Transaction>
}