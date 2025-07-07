package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
import java.util.Calendar
import com.example.upitracker.util.NotificationHelper
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "RecurringTransactionWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Worker starting: Checking for due recurring transactions...")
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val recurringRuleDao = db.recurringRuleDao()
            val transactionDao = db.transactionDao()
            val now = System.currentTimeMillis()

            val dueRules = recurringRuleDao.getDueRules(now)
            if (dueRules.isNotEmpty()) {
                Log.d(WORK_NAME, "Found ${dueRules.size} due rules to process.")
                for (rule in dueRules) {
                    val newTransaction = Transaction(
                        amount = rule.amount, type = "DEBIT", date = rule.nextDueDate,
                        description = rule.description, senderOrReceiver = "Recurring", category = rule.categoryName
                    )
                    transactionDao.insert(newTransaction)
                    Log.d(WORK_NAME, "Created transaction for '${rule.description}'.")

                    val calendar = Calendar.getInstance().apply { timeInMillis = rule.nextDueDate }
                    when (rule.periodType) {
                        com.example.upitracker.data.BudgetPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                        com.example.upitracker.data.BudgetPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        com.example.upitracker.data.BudgetPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
                    }
                    val maxDayInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, rule.dayOfPeriod.coerceAtMost(maxDayInMonth))
                    val updatedRule = rule.copy(nextDueDate = calendar.timeInMillis)
                    recurringRuleDao.update(updatedRule)
                }
            }

            // --- ✨ Section 2: Notify for UPCOMING rules (New Logic) ---
            val twoDaysInMillis = TimeUnit.DAYS.toMillis(2)
            val upcomingRules = recurringRuleDao.getUpcomingRules(now, now + twoDaysInMillis)
            if (upcomingRules.isNotEmpty()) {
                Log.d(WORK_NAME, "Found ${upcomingRules.size} upcoming rules to notify about.")
                // Ensure notification channel exists
                NotificationHelper.createNotificationChannels(applicationContext)
                upcomingRules.forEach { rule ->
                    NotificationHelper.showUpcomingPaymentNotification(applicationContext, rule)
                }
            }

            Log.i(WORK_NAME, "Worker finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error processing recurring transactions.", e)
            Result.failure()
        }
    }
}