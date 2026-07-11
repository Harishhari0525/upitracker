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
import com.example.upitracker.util.toMajorUnits
import androidx.paging.PagingSource
import androidx.room.Ignore

@Dao
interface TransactionDao {

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeTransactionCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE isArchived = 0 AND pendingDeletionTimestamp IS NULL ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE isArchived = 0
          AND pendingDeletionTimestamp IS NULL
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:type IS NULL OR type = :type)
        ORDER BY date DESC, id DESC
    """)
    fun getTransactionsForPassbookPaged(startDate: Long?, endDate: Long?, type: String?): PagingSource<Int, Transaction>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0) AS totalDebitPaise,
            COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0) AS totalCreditPaise,
            COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN 1 ELSE 0 END), 0) AS debitCount,
            COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN 1 ELSE 0 END), 0) AS creditCount
        FROM transactions
        WHERE isArchived = 0
          AND pendingDeletionTimestamp IS NULL
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:type IS NULL OR type = :type)
    """)
    fun getPassbookTotals(startDate: Long?, endDate: Long?, type: String?): Flow<TransactionTotals>

    @Query("""
        SELECT COUNT(*) FROM transactions
        WHERE isArchived = 0
          AND pendingDeletionTimestamp IS NULL
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:type IS NULL OR type = :type)
    """)
    fun getPassbookCount(startDate: Long?, endDate: Long?, type: String?): Flow<Int>

    @Query("""
        SELECT * FROM transactions
        WHERE isArchived = 0
          AND pendingDeletionTimestamp IS NULL
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:type IS NULL OR type = :type)
        ORDER BY date DESC, id DESC
    """)
    suspend fun getTransactionsForPassbookSnapshot(startDate: Long?, endDate: Long?, type: String?): List<Transaction>

    @Query("""
        SELECT * FROM transactions
        WHERE isArchived = 0 AND pendingDeletionTimestamp IS NULL
        ORDER BY date DESC, id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getTransactionsPage(limit: Int, offset: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE isArchived = 1 ORDER BY date DESC, id DESC")
    fun getArchivedTransactionsPaged(): PagingSource<Int, Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE isArchived = 1")
    fun getArchivedTransactionCount(): Flow<Int>

    @RawQuery(observedEntities = [Transaction::class])
    fun getFilteredTransactionsPaged(query: SupportSQLiteQuery): PagingSource<Int, Transaction>

    data class TransactionTotals(
        val totalDebitPaise: Long,
        val totalCreditPaise: Long,
        val debitCount: Int,
        val creditCount: Int
    )

    @RawQuery(observedEntities = [Transaction::class])
    fun getFilteredTotals(query: SupportSQLiteQuery): Flow<TransactionTotals>

    @Query("SELECT * FROM transactions WHERE amount = :amount AND date = :date AND description = :desc LIMIT 1")
    suspend fun getTransactionByDetails(amount: Long, date: Long, desc: String): Transaction?

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
        amount: Long,
        type: String,
        startDate: Long,
        endDate: Long,
        description: String,
        senderOrReceiver: String
    ): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE amount = :amount
          AND type = 'DEBIT'
          AND date BETWEEN :startDate AND :endDate
          AND isArchived = 0
          AND pendingDeletionTimestamp IS NULL
          AND senderOrReceiver != 'Recurring'
          AND (
            LOWER(TRIM(category)) = LOWER(TRIM(:categoryName))
            OR LOWER(description) LIKE '%' || LOWER(:description) || '%'
            OR LOWER(:description) LIKE '%' || LOWER(description) || '%'
          )
        ORDER BY ABS(date - :dueDate) ASC
        LIMIT 1
        """
    )
    suspend fun findSimilarDebitNearDate(
        amount: Long,
        startDate: Long,
        endDate: Long,
        dueDate: Long,
        description: String,
        categoryName: String
    ): Transaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReturningId(transaction: Transaction): Long

    @androidx.room.Transaction
    suspend fun insertIfNotDuplicate(transaction: Transaction): Long {
        val strictTimeToleranceMs = 0L
        val potentialDuplicateWindowMs = 5 * 60 * 1000L

        // 1. Strict duplicate check (same amount, type, date, description, sender)
        val strictExisting = findDuplicateTransaction(
            amount = transaction.amountPaise,
            type = transaction.type,
            startDate = transaction.date - strictTimeToleranceMs,
            endDate = transaction.date + strictTimeToleranceMs,
            description = transaction.description.trim(),
            senderOrReceiver = transaction.senderOrReceiver.trim()
        )

        if (strictExisting != null) {
            var updated = false
            var toUpdate = strictExisting
            if (strictExisting.balanceAfterTransaction == null && transaction.balanceAfterTransaction != null) {
                toUpdate = toUpdate.copy(balanceAfterTransactionPaise = transaction.balanceAfterTransactionPaise)
                updated = true
            }
            if (strictExisting.bankName == null && transaction.bankName != null) {
                toUpdate = toUpdate.copy(bankName = transaction.bankName)
                updated = true
            }
            if (isBadPartyName(strictExisting.senderOrReceiver) && !isBadPartyName(transaction.senderOrReceiver)) {
                toUpdate = toUpdate.copy(senderOrReceiver = transaction.senderOrReceiver)
                updated = true
            }
            if (updated) {
                update(toUpdate)
            }
            return -1L
        }

        // 2. Intelligent duplicate detection & merging (same amount/type within 5 minutes)
        val potentials = findPotentialDuplicates(
            amount = transaction.amountPaise,
            type = transaction.type,
            startDate = transaction.date - potentialDuplicateWindowMs,
            endDate = transaction.date + potentialDuplicateWindowMs
        )

        val incomingRef = extractUpiReferenceNumber(transaction.description)

        for (p in potentials) {
            val existingRef = extractUpiReferenceNumber(p.description)
            
            // If reference numbers are both present but differ, they are distinct transactions
            if (incomingRef != null && existingRef != null && incomingRef != existingRef) {
                continue
            }

            val isRefMatch = incomingRef != null && existingRef != null
            
            if (isRefMatch) {
                val isBankMergeable = (p.bankName == null || p.bankName == "Other Bank") && 
                                      (transaction.bankName != null && transaction.bankName != "Other Bank")
                // If the existing is generic and incoming is bank-specific, upgrade the existing record
                if (isBankMergeable) {
                    val updatedTxn = p.copy(
                        bankName = transaction.bankName,
                        balanceAfterTransactionPaise = transaction.balanceAfterTransactionPaise ?: p.balanceAfterTransactionPaise,
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
                            balanceAfterTransactionPaise = transaction.balanceAfterTransactionPaise,
                            bankName = if (p.bankName == null || p.bankName == "Other Bank") transaction.bankName else p.bankName
                        ))
                    } else if (isBadPartyName(p.senderOrReceiver) && !isBadPartyName(transaction.senderOrReceiver)) {
                        update(p.copy(senderOrReceiver = transaction.senderOrReceiver))
                    }
                }
                // It's a duplicate, return -1L
                return -1L
            }

            if (isLikelySameBankNotification(p, transaction)) {
                val updatedTxn = p.copy(
                    bankName = p.bankName ?: transaction.bankName,
                    balanceAfterTransactionPaise = p.balanceAfterTransactionPaise ?: transaction.balanceAfterTransactionPaise,
                    description = chooseRicherText(p.description, transaction.description),
                    senderOrReceiver = chooseBetterParty(p.senderOrReceiver, transaction.senderOrReceiver)
                )
                if (updatedTxn != p) {
                    update(updatedTxn)
                }
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

    @Query("SELECT * FROM transactions WHERE linkedTransactionId = :id LIMIT 1")
    suspend fun getTransactionLinkedTo(id: Int): Transaction?

    @Query("SELECT * FROM transactions WHERE linkedTransactionId = :id LIMIT 1")
    fun getTransactionLinkedToFlow(id: Int): Flow<Transaction?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE category = :category AND type = 'DEBIT' AND isArchived = 0 AND pendingDeletionTimestamp IS NULL")
    suspend fun getCategoryDebitTotalPaise(category: String): Long

    @Query("""
        SELECT category FROM transactions
        WHERE category IS NOT NULL AND category != '' AND isArchived = 0 AND pendingDeletionTimestamp IS NULL
        GROUP BY category ORDER BY COUNT(*) DESC LIMIT :limit
    """)
    fun getMostUsedCategoryNames(limit: Int = 7): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM transactions WHERE senderOrReceiver = :merchant AND category = :category AND isArchived = 0 AND pendingDeletionTimestamp IS NULL")
    suspend fun countCategorizedMerchantTransactions(merchant: String, category: String): Int

    @Query("DELETE FROM transactions WHERE pendingDeletionTimestamp IS NOT NULL AND pendingDeletionTimestamp < :cutoffTimestamp")
    suspend fun permanentlyDeletePending(cutoffTimestamp: Long)

    @Query("SELECT receiptImagePath FROM transactions WHERE pendingDeletionTimestamp IS NOT NULL AND pendingDeletionTimestamp < :cutoffTimestamp AND receiptImagePath IS NOT NULL")
    suspend fun getReceiptPathsPendingDeletion(cutoffTimestamp: Long): List<String>

    @Query("SELECT * FROM transactions WHERE type = 'DEBIT' AND senderOrReceiver = :sender AND isArchived = 0 AND linkedTransactionId IS NULL ORDER BY date DESC")
    suspend fun findPotentialDebitsForRefund(sender: String): List<Transaction>

    @Query("UPDATE transactions SET linkedTransactionId = NULL WHERE id = :transactionId")
    suspend fun unlinkTransaction(transactionId: Int)

    @Query("UPDATE transactions SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateCategoryName(oldCategory: String, newCategory: String)

    @Query("""
        UPDATE transactions
        SET category = :categoryName
        WHERE id != :excludeId
          AND senderOrReceiver = :senderOrReceiver
          AND isArchived = 0
          AND pendingDeletionTimestamp IS NULL
          AND (category IS NULL OR TRIM(category) = '')
    """)
    suspend fun categorizeUncategorizedBySender(
        senderOrReceiver: String,
        categoryName: String,
        excludeId: Int = -1
    ): Int

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
    suspend fun getSpentAmountPaiseInRangeSync(
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): Long?

    suspend fun getSpentAmountInRangeSync(startDate: Long, endDate: Long, refundCategory: String): Double? =
        getSpentAmountPaiseInRangeSync(startDate, endDate, refundCategory)?.toMajorUnits()

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :categoryName AND type = 'DEBIT' AND category != :refundCategory AND date BETWEEN :startDate AND :endDate AND isArchived = 0 AND pendingDeletionTimestamp IS NULL")
    suspend fun getSpentAmountPaiseForCategoryInRangeSync(
        categoryName: String,
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): Long?

    suspend fun getSpentAmountForCategoryInRangeSync(
        categoryName: String,
        startDate: Long,
        endDate: Long,
        refundCategory: String
    ): Double? = getSpentAmountPaiseForCategoryInRangeSync(
        categoryName, startDate, endDate, refundCategory
    )?.toMajorUnits()

    @Query("SELECT * FROM transactions WHERE senderOrReceiver = :sender AND isArchived = 0 ORDER BY date DESC")
    suspend fun getHistoryForMerchant(sender: String): List<Transaction>

    data class BankBalance(val bankName: String, val latestBalancePaise: Long, val lastUpdated: Long) {
        @get:Ignore
        val latestBalance: Double get() = latestBalancePaise.toMajorUnits()
    }

    data class BalanceDrift(
        val bankName: String,
        val transactionId: Int,
        val date: Long,
        val expectedBalancePaise: Long,
        val actualBalancePaise: Long,
        val differencePaise: Long
    )

    @Query("""
        WITH ordered AS (
            SELECT id, bankName, date, amount, type, balanceAfterTransaction,
                   LAG(balanceAfterTransaction) OVER (PARTITION BY bankName ORDER BY date, id) AS previousBalance
            FROM transactions
            WHERE bankName IS NOT NULL AND balanceAfterTransaction IS NOT NULL
              AND isArchived = 0 AND pendingDeletionTimestamp IS NULL
        )
        SELECT bankName, id AS transactionId, date,
               previousBalance + CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END AS expectedBalancePaise,
               balanceAfterTransaction AS actualBalancePaise,
               balanceAfterTransaction - (previousBalance + CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END) AS differencePaise
        FROM ordered
        WHERE previousBalance IS NOT NULL
          AND ABS(balanceAfterTransaction - (previousBalance + CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END)) > 1
        ORDER BY date DESC
        LIMIT 100
    """)
    fun getBalanceDrifts(): Flow<List<BalanceDrift>>

    @Query("""
        SELECT t.bankName, t.balanceAfterTransaction AS latestBalancePaise, t.date AS lastUpdated
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
        amount: Long,
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

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND isArchived = 0 AND pendingDeletionTimestamp IS NULL ORDER BY date DESC")
    suspend fun getTransactionsInRangeSync(startDate: Long, endDate: Long): List<Transaction>
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

private fun isLikelySameBankNotification(existing: Transaction, incoming: Transaction): Boolean {
    if (existing.senderOrReceiver == "Manual Entry" || incoming.senderOrReceiver == "Manual Entry") return false
    if (existing.senderOrReceiver == "Recurring" || incoming.senderOrReceiver == "Recurring") return false

    val closeInTime = kotlin.math.abs(existing.date - incoming.date) <= 10_000L
    if (!closeInTime) return false

    val sameSender = normalizeDuplicateKey(existing.senderOrReceiver) == normalizeDuplicateKey(incoming.senderOrReceiver)
    val sameBank = !existing.bankName.isNullOrBlank() &&
        !incoming.bankName.isNullOrBlank() &&
        existing.bankName.equals(incoming.bankName, ignoreCase = true)

    if (!sameSender && !sameBank) return false

    val existingRef = extractUpiReferenceNumber(existing.description)
    val incomingRef = extractUpiReferenceNumber(incoming.description)
    if (existingRef != null && incomingRef != null && existingRef != incomingRef) return false

    return true
}

private fun normalizeDuplicateKey(value: String): String =
    value.lowercase()
        .replace(Regex("""[^a-z0-9]"""), "")
        .trim()

private fun chooseRicherText(existing: String, incoming: String): String =
    if (incoming.length > existing.length) incoming else existing

private fun chooseBetterParty(existing: String, incoming: String): String =
    when {
        isBadPartyName(existing) && !isBadPartyName(incoming) -> incoming
        !isBadPartyName(existing) -> existing
        else -> chooseRicherText(existing, incoming)
    }

private fun isBadPartyName(value: String): Boolean {
    val normalized = normalizeDuplicateKey(value)
    val lower = value.lowercase()
    return normalized.isBlank() ||
        normalized == "manualentry" ||
        normalized == "recurring" ||
        normalized == "otherbank" ||
        normalized == "bank" ||
        normalized == "dispute" ||
        normalized == "alert" ||
        normalized == "alerts" ||
        normalized == "cmplnt" ||
        normalized == "notice" ||
        normalized == "update" ||
        normalized == "secure" ||
        normalized == "info" ||
        normalized == "icici" ||
        normalized == "icicit" ||
        normalized == "hdfc" ||
        normalized == "hdfcbk" ||
        normalized == "sbibnk" ||
        normalized == "axis" ||
        normalized == "axisbk" ||
        value.length > 60 ||
        lower.contains(" debited") ||
        lower.contains(" credited") ||
        lower.contains(" account") ||
        lower.contains(" balance") ||
        lower.contains(" transaction") ||
        lower.contains(" has been ") ||
        lower.contains(" available ")
}
