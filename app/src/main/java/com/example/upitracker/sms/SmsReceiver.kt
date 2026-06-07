package com.example.upitracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.Transaction
import com.example.upitracker.util.BankIdentifier
import com.example.upitracker.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // 🛡️ SECURITY FIX: Explicitly validate the system action string to stop spoofing attacks
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.w("SmsReceiver", "Rejected unexpected broadcast action signature: ${intent.action}")
            return
        }

        // ⚡ LIFECYCLE FIX: Keep the background process alive long enough to query SQLite
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Extract raw SMS texts using official Android Telephony API sets
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return@launch

                val fullSmsBody = messages.joinToString(separator = "") { it.displayMessageBody ?: "" }
                val originalSender = messages.firstOrNull()?.originatingAddress ?: "Unknown"

                // 2. Locate the database context safely on disk
                val db = AppDatabase.getDatabase(context)
                val transactionDao = db.transactionDao()
                val categoryRuleDao = db.categorySuggestionRuleDao()


                // 3. Build a temporary transaction object to pass into your core parser pipeline
                val rawTransaction = Transaction(
                    amount = 0.0, // Will be overridden or populated by the parser helper
                    type = "DEBIT",
                    description = fullSmsBody,
                    category = null,
                    date = System.currentTimeMillis(),
                    senderOrReceiver = originalSender,
                    isArchived = false,
                    note = ""
                )

                // 4. Fetch categorization rules active inside the database
                val categoryRules = categoryRuleDao.getAllRules().first()

                // 5. Build structured schema out using your established SmsProcessingService processor
                val processedTransaction = SmsProcessingService.prepareTransactionForInsert(
                    transaction = rawTransaction,
                    rules = categoryRules
                )

                // Confirm if the processed transaction contains valid financial information extracted by your parser
                if (processedTransaction.amount > 0) {

                    // Attach the evaluated bank identifier token mapping
                    val bankName = BankIdentifier.getBankName(originalSender)
                    val finalTransaction = processedTransaction.copy(bankName = bankName)

                    // 6. GATING: Check time-tolerance boundaries to prevent concurrent loop duplicates
                    val insertedId = transactionDao.insertIfNotDuplicate(finalTransaction)

                    if (insertedId != -1L) {
                        Log.i("SmsReceiver", "Successfully processed and written transaction record ID: $insertedId")

                        // 7. Core Budget evaluation check and notification dispatch block
                        val savedTransaction = finalTransaction.copy(id = insertedId.toInt())
                        evaluateBudgetLimitsAndNotify(context, db, savedTransaction)
                    } else {
                        Log.d("SmsReceiver", "Duplicate transaction matching window criteria rejected.")
                    }
                }

            } catch (e: Exception) {
                Log.e("SmsReceiver", "Critical structural error executing background sync thread layers", e)
            } finally {
                // ⚡ Release the process lock, allowing Android to safely return the thread pool
                pendingResult.finish()
            }
        }
    }

    /**
     * Replaces 'checkBudgetAndNotifyLocal' using your exact view model database queries
     */
    private suspend fun evaluateBudgetLimitsAndNotify(context: Context, db: AppDatabase, transaction: Transaction) {
        if (!transaction.type.equals("DEBIT", ignoreCase = true)) return

        val budgetDao = db.budgetDao()
        val transactionDao = db.transactionDao()

        val activeBudgets = budgetDao.getAllActiveBudgets().first()
        val relevantBudget = activeBudgets.find { it.categoryName.equals(transaction.category, ignoreCase = true) }

        if (relevantBudget != null) {
            val calendar = Calendar.getInstance()

            // Generate range boundaries matching active period types
            val (periodStart, periodEnd) = when (relevantBudget.periodType) {
                BudgetPeriod.WEEKLY -> {
                    calendar.firstDayOfWeek = Calendar.MONDAY
                    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    setDayToStart(calendar)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.WEEK_OF_YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
                    start to calendar.timeInMillis
                }
                BudgetPeriod.MONTHLY -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    setDayToStart(calendar)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1)
                    start to calendar.timeInMillis
                }
                BudgetPeriod.YEARLY -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    setDayToStart(calendar)
                    val start = calendar.timeInMillis
                    calendar.add(Calendar.YEAR, 1); calendar.add(Calendar.MILLISECOND, -1)
                    start to calendar.timeInMillis
                }
            }

            // Only proceed if a notification hasn't been fired inside the active time window
            if (relevantBudget.lastNotificationTimestamp < periodStart) {

                val currentPeriodDebits = transactionDao.getTransactionsForBudgetCheck(
                    categoryName = relevantBudget.categoryName,
                    startDate = periodStart,
                    endDate = periodEnd,
                    refundCategory = "Refund"
                )

                val cumulativeSpent = currentPeriodDebits.sumOf { it.amount }

                if (cumulativeSpent > relevantBudget.budgetAmount) {
                    // Trigger the notification banner using your utility layout class context helpers
                    NotificationHelper.showBudgetExceededNotification(
                        context = context,
                        budget = relevantBudget,
                        spentAmount = cumulativeSpent
                    )

                    // Stamp the budget rule to avoid alert spamming loops
                    budgetDao.update(relevantBudget.copy(lastNotificationTimestamp = System.currentTimeMillis()))
                }
            }
        }
    }

    private fun setDayToStart(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
}