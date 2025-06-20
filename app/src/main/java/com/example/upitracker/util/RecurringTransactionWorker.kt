package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.Transaction
import java.util.Calendar

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

            // 1. Get all rules that are due
            val dueRules = recurringRuleDao.getDueRules(now)
            Log.d(WORK_NAME, "Found ${dueRules.size} due rules to process.")

            for (rule in dueRules) {
                // 2. For each due rule, create a new Transaction
                val newTransaction = Transaction(
                    amount = rule.amount,
                    type = "DEBIT", // Recurring transactions are always debits
                    date = rule.nextDueDate, // Use the due date as the transaction date
                    description = rule.description,
                    senderOrReceiver = "Recurring", // Mark the source
                    category = rule.categoryName
                )
                transactionDao.insert(newTransaction)
                Log.d(WORK_NAME, "Created transaction for '${rule.description}'.")


                // 3. Calculate the NEXT due date and update the rule
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = rule.nextDueDate // Start from the last due date

                when (rule.periodType) {
                    com.example.upitracker.data.BudgetPeriod.MONTHLY -> {
                        calendar.add(Calendar.MONTH, 1)
                    }
                    com.example.upitracker.data.BudgetPeriod.WEEKLY -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    com.example.upitracker.data.BudgetPeriod.YEARLY -> {
                        calendar.add(Calendar.YEAR, 1)
                    }
                }

                val updatedRule = rule.copy(nextDueDate = calendar.timeInMillis)
                recurringRuleDao.update(updatedRule)
                Log.d(WORK_NAME, "Updated rule '${rule.description}' to next due date: ${calendar.time}")
            }

            Log.i(WORK_NAME, "Worker finished successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error processing recurring transactions.", e)
            Result.failure()
        }
    }
}