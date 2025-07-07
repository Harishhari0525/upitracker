package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.data.AppDatabase
import com.example.upitracker.data.BudgetPeriod
import kotlinx.coroutines.flow.first
import java.util.Calendar

class BudgetCheckerWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "BudgetCheckerWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Worker starting: Checking budget statuses...")
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val budgetDao = db.budgetDao()
            val transactionDao = db.transactionDao()

            val activeBudgets = budgetDao.getAllActiveBudgets().first()
            if (activeBudgets.isEmpty()) return Result.success()

            val allNonRefundDebits = transactionDao.getAllTransactions().first()
                .filter { it.type == "DEBIT" && it.category != "Refund" }

            activeBudgets.forEach { budget ->
                val (periodStart, periodEnd) = getCurrentPeriodRange(budget.periodType)

                // Only check if we haven't already notified for the current period
                if (budget.lastNotificationTimestamp < periodStart) {
                    val spentInCurrentPeriod = allNonRefundDebits
                        .filter {
                            it.category.equals(budget.categoryName, true) && it.date in periodStart..periodEnd
                        }
                        .sumOf { it.amount }

                    if (spentInCurrentPeriod > budget.budgetAmount) {
                        Log.d(WORK_NAME, "Budget for '${budget.categoryName}' exceeded. Notifying user.")
                        NotificationHelper.showBudgetExceededNotification(applicationContext, budget, spentInCurrentPeriod)

                        // Update the budget with a new timestamp to prevent re-notifying
                        val updatedBudget = budget.copy(lastNotificationTimestamp = System.currentTimeMillis())
                        budgetDao.update(updatedBudget)
                    }
                }
            }
            Log.i(WORK_NAME, "Budget check finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error checking budgets.", e)
            return Result.failure()
        }
    }

    private fun getCurrentPeriodRange(period: BudgetPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        when (period) {
            BudgetPeriod.WEEKLY -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            BudgetPeriod.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            BudgetPeriod.YEARLY -> calendar.set(Calendar.DAY_OF_YEAR, 1)
        }

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val periodStart = calendar.timeInMillis

        when (period) {
            BudgetPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            BudgetPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            BudgetPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        calendar.add(Calendar.MILLISECOND, -1)
        val periodEnd = calendar.timeInMillis

        return periodStart to periodEnd
    }
}