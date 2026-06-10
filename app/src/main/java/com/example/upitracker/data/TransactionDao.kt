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
        val timeToleranceMs = 5 * 60 * 1000L

        // 1. Strict duplicate check (same amount, type, date, description, sender)
        val strictExisting = findDuplicateTransaction(
            amount = transaction.amount,
            type = transaction.type,
            startDate = transaction.date - timeToleranceMs,
            endDate = transaction.date + timeToleranceMs,
            description = transaction.description.trim(),
            senderOrReceiver = transaction.senderOrReceiver.trim()
        )

        if (strictExisting != null) {
            var updated = false
            var toUpdate = strictExisting
            if (strictExisting.balanceAfterTransaction == null && transaction.balanceAfterTransaction != null) {
                toUpdate = toUpdate.copy(balanceAfterTransaction = transaction.balanceAfterTransaction)
                updated = true
            }
            if (strictExisting.bankName == null && transaction.bankName != null) {
                toUpdate = toUpdate.copy(bankName = transaction.bankName)
                updated = true
            }
            if (updated) {
                update(toUpdate)
            }
            return -1L
        }

        // 2. Intelligent duplicate detection & merging (same amount/type within 5 minutes)
        val potentials = findPotentialDuplicates(
            amount = transaction.amount,
            type = transaction.type,
            startDate = transaction.date - timeToleranceMs,
            endDate = transaction.date + timeToleranceMs
        )

        val incomingRef = extractUpiReferenceNumber(transaction.description)

        for (p in potentials) {
            val existingRef = extractUpiReferenceNumber(p.description)
            
            // If reference numbers are both present but differ, they are distinct transactions
            if (incomingRef != null && existingRef != null && incomingRef != existingRef) {
                continue
            }

            val isRefMatch = incomingRef != null && existingRef != null && incomingRef == existingRef
            
            val isBankMatch = p.bankName == transaction.bankName || 
                              (p.bankName == null || p.bankName == "Other Bank") ||
                              (transaction.bankName == null || transaction.bankName == "Other Bank")

            val hasNoRefConflict = incomingRef == null || existingRef == null || incomingRef == existingRef

            if (isRefMatch || (isBankMatch && hasNoRefConflict)) {
                val isBankMergeable = (p.bankName == null || p.bankName == "Other Bank") && 
                                      (transaction.bankName != null && transaction.bankName != "Other Bank")
                // If the existing is generic and incoming is bank-specific, upgrade the existing record
                if (isBankMergeable) {
                    val updatedTxn = p.copy(
                        bankName = transaction.bankName,
                        balanceAfterTransaction = transaction.balanceAfterTransaction ?: p.balanceAfterTransaction,
                        description = transaction.description,
                        senderOrReceiver = transaction.senderOrReceiver
                    )
                    update(updatedTxn)
                } else {
                    // Update balance if incoming has it (or is more accurate/non-zero) and existing doesn't
                    val hasNewBalance = transaction.balanceAfterTransaction != null && 
                                       (p.balanceAfterTransaction == null || p.balanceAfterTransaction == 0.0)
                    if (hasNewBalance) {
                        update(p.copy(
                            balanceAfterTransaction = transaction.balanceAfterTransaction,
                            bankName = if (p.bankName == null || p.bankName == "Other Bank") transaction.bankName else p.bankName
                        ))
                    }
                }
                // It's a duplicate, return -1L
                return -1L
            }
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

    @Query("SELECT * FROM transactions WHERE category = :categoryName AND type = 'DEBIT' AND category != :refundCategory AND date BETWEEN :startDate AND :endDate AND isArchived = 0 AND pendingDeletionTimestamp IS NULL")
    suspend fun getTransactionsForBudgetCheck(
        categoryName: String,
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): List<Transaction>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND category != :refundCategory AND date BETWEEN :startDate AND :endDate AND isArchived = 0 AND pendingDeletionTimestamp IS NULL")
    suspend fun getSpentAmountInRangeSync(
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): Double?

    @Query("SELECT * FROM transactions WHERE senderOrReceiver = :sender AND isArchived = 0 ORDER BY date DESC")
    suspend fun getHistoryForMerchant(sender: String): List<Transaction>

    data class BankBalance(val bankName: String, val latestBalance: Double, val lastUpdated: Long)

    @Query("""
        SELECT t.bankName, t.balanceAfterTransaction AS latestBalance, t.date AS lastUpdated
        FROM transactions t
        WHERE t.bankName IS NOT NULL 
          AND t.bankName != 'Other Bank'
          AND t.balanceAfterTransaction IS NOT NULL
          AND t.isArchived = 0 
          AND t.pendingDeletionTimestamp IS NULL
          AND t.id = (
              SELECT id FROM transactions t2
              WHERE t2.bankName = t.bankName 
                AND t2.balanceAfterTransaction IS NOT NULL 
                AND t2.isArchived = 0 
                AND t2.pendingDeletionTimestamp IS NULL
              ORDER BY t2.date DESC, t2.id DESC
              LIMIT 1
          )
    """)
    fun getLatestBankBalances(): Flow<List<BankBalance>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE amount = :amount
          AND type = :type
          AND date BETWEEN :startDate AND :endDate
          AND isArchived = 0
          AND pendingDeletionTimestamp IS NULL
        """
    )
    suspend fun findPotentialDuplicates(
        amount: Double,
        type: String,
        startDate: Long,
        endDate: Long
    ): List<Transaction>

    @Query("""
        SELECT category FROM transactions 
        WHERE senderOrReceiver = :merchantName 
          AND category IS NOT NULL 
          AND category != ''
        GROUP BY category 
        ORDER BY COUNT(*) DESC 
        LIMIT 3
    """)
    suspend fun getTopCategoriesForMerchant(merchantName: String): List<String>

    @Query("""
        SELECT category FROM transactions 
        WHERE category IS NOT NULL 
          AND category != ''
        GROUP BY category 
        ORDER BY COUNT(*) DESC 
        LIMIT 3
    """)
    suspend fun getGlobalTopCategories(): List<String>
}

private fun extractUpiReferenceNumber(text: String): String? {
    val regex = Regex("""\b(?:upi\s+ref\s*(?:no)?|ref\s*(?:no)?|utr\s*(?:no)?|txn\s*(?:id)?|reference\s*(?:no)?)\s*[:\-\s]*([A-Za-z0-9]{8,16})\b""", RegexOption.IGNORE_CASE)
    val match = regex.find(text)
    if (match != null) {
        return match.groupValues[1].lowercase()
    }
    val digitRegex = Regex("""\b(\d{12})\b""")
    val digitMatch = digitRegex.find(text)
    return digitMatch?.groupValues?.get(1)
}