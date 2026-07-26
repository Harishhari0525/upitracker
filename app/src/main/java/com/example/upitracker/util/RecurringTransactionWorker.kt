package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.BudgetPeriod
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.data.Transaction
import java.util.Calendar
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
                    db.withTransaction {
                        if (rule.createTransactionOnDueDate) {
                            val duplicateWindowMs = TimeUnit.HOURS.toMillis(36)
                            val existingDebit = transactionDao.findSimilarDebitNearDate(
                                amount = rule.amountPaise,
                                startDate = rule.nextDueDate - duplicateWindowMs,
                                endDate = rule.nextDueDate + duplicateWindowMs,
                                dueDate = rule.nextDueDate,
                                description = rule.description.trim(),
                                categoryName = rule.categoryName
                            )
                            if (existingDebit == null) {
                                transactionDao.insert(
                                    Transaction(
                                        amount = rule.amount,
                                        type = "DEBIT",
                                        date = rule.nextDueDate,
                                        description = rule.description,
                                        senderOrReceiver = "Recurring",
                                        category = rule.categoryName
                                    )
                                )
                                Log.d(WORK_NAME, "Created a scheduled transaction")
                            }
                        }

                        recurringRuleDao.update(
                            rule.copy(
                                nextDueDate = firstDueDateAfter(rule, now),
                                lastUpcomingNotificationDueDate = 0L
                            )
                        )
                    }
                }
            }

            // --- ✨ Section 2: Notify for UPCOMING rules (New Logic) ---
            val twoDaysInMillis = TimeUnit.DAYS.toMillis(2)
            val upcomingRules = recurringRuleDao.getUpcomingRules(now, now + twoDaysInMillis)
            if (upcomingRules.isNotEmpty()) {
                Log.d(WORK_NAME, "Found ${upcomingRules.size} upcoming rules to notify about.")
                // Ensure notification channel exists
                NotificationHelper.createNotificationChannels(applicationContext)
                upcomingRules
                    .filter { it.lastUpcomingNotificationDueDate != it.nextDueDate }
                    .forEach { rule ->
                        NotificationHelper.showUpcomingPaymentNotification(applicationContext, rule)
                        recurringRuleDao.update(
                            rule.copy(lastUpcomingNotificationDueDate = rule.nextDueDate)
                        )
                    }
                }

            Log.i(WORK_NAME, "Worker finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error processing recurring transactions.", e)
            Result.retry()
        }
    }

    private fun firstDueDateAfter(rule: RecurringRule, now: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = rule.nextDueDate }
        do {
            when (rule.periodType) {
                BudgetPeriod.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                BudgetPeriod.MONTHLY -> {
                    calendar.add(Calendar.MONTH, 1)
                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        rule.dayOfPeriod.coerceIn(
                            1,
                            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        )
                    )
                }
                BudgetPeriod.YEARLY -> {
                    calendar.add(Calendar.YEAR, 1)
                    calendar.set(
                        Calendar.DAY_OF_YEAR,
                        rule.dayOfPeriod.coerceIn(
                            1,
                            calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                        )
                    )
                }
            }
        } while (calendar.timeInMillis <= now)
        return calendar.timeInMillis
    }
}
